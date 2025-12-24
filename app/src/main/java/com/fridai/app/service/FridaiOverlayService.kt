package com.fridai.app.service

import android.animation.ValueAnimator
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Base64
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.fridai.app.R
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.sin

/**
 * FridaiOverlayService - Shows a floating overlay when wake word is detected
 *
 * This creates a Google Assistant-like popup that floats over other apps.
 */
class FridaiOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isShowing = false
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())

    // UI components
    private var avatarContainer: FrameLayout? = null
    private var statusText: TextView? = null
    private var transcriptText: TextView? = null

    // Audio
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var mediaPlayer: MediaPlayer? = null

    // Animation
    private var pulseAnimator: ValueAnimator? = null
    private var glowAnimator: ValueAnimator? = null

    // State
    private var currentState = AssistantState.LISTENING

    // Network
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private val baseUrl = "https://fridai.fridai.me"

    enum class AssistantState {
        LISTENING, THINKING, SPEAKING, IDLE
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> showOverlay()
            ACTION_HIDE -> hideOverlay()
            else -> showOverlay()
        }
        return START_NOT_STICKY
    }

    private fun showOverlay() {
        if (isShowing) return
        if (!Settings.canDrawOverlays(this)) {
            android.util.Log.e("FRIDAI", "Overlay permission not granted")
            stopSelf()
            return
        }

        isShowing = true
        createOverlayView()

        // Vibrate to indicate activation
        vibrate()

        // Start listening
        setState(AssistantState.LISTENING)
        startRecording()
    }

    private fun createOverlayView() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 100 // Offset from bottom
        }

        overlayView = createCustomView()
        windowManager?.addView(overlayView, params)

        // Animate in
        overlayView?.alpha = 0f
        overlayView?.translationY = 200f
        overlayView?.animate()
            ?.alpha(1f)
            ?.translationY(0f)
            ?.setDuration(300)
            ?.setInterpolator(AccelerateDecelerateInterpolator())
            ?.start()
    }

    private fun createCustomView(): View {
        val container = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(32, 32, 32, 32)
        }

        // Main card background
        val card = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(24, 0, 24, 0)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 48f
                setColor(Color.parseColor("#1A1A2E"))
                setStroke(2, Color.parseColor("#00D9FF"))
            }
            setPadding(48, 48, 48, 48)
            elevation = 24f

            // Close on tap outside avatar
            setOnClickListener { hideOverlay() }
        }

        // Vertical layout inside card
        val innerLayout = android.widget.LinearLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // Avatar container
        avatarContainer = FrameLayout(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(200, 200).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
        }
        createAvatarView(avatarContainer!!)
        innerLayout.addView(avatarContainer)

        // Status text
        statusText = TextView(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = 24
            }
            text = "Listening..."
            setTextColor(Color.parseColor("#00D9FF"))
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        innerLayout.addView(statusText)

        // Transcript text
        transcriptText = TextView(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = 16
            }
            text = ""
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER
            maxLines = 3
        }
        innerLayout.addView(transcriptText)

        card.addView(innerLayout)
        container.addView(card)

        return container
    }

    private fun createAvatarView(container: FrameLayout) {
        // Outer glow
        val glowView = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(200, 200)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.TRANSPARENT)
                setStroke(4, Color.parseColor("#00D9FF"))
            }
            alpha = 0.5f
        }
        container.addView(glowView)

        // Core orb
        val coreView = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(160, 160).apply {
                gravity = Gravity.CENTER
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                gradientType = GradientDrawable.RADIAL_GRADIENT
                gradientRadius = 80f
                colors = intArrayOf(
                    Color.parseColor("#00D9FF"),
                    Color.parseColor("#6C63FF")
                )
            }
        }
        container.addView(coreView)

        // Center letter
        val letterView = TextView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
            text = "F"
            setTextColor(Color.WHITE)
            textSize = 48f
            typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
        }
        container.addView(letterView)

        // Start pulse animation
        startPulseAnimation(coreView, glowView)
    }

    private fun startPulseAnimation(coreView: View, glowView: View) {
        pulseAnimator?.cancel()
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val value = animator.animatedValue as Float
                val scale = 1f + (value * 0.1f)
                coreView.scaleX = scale
                coreView.scaleY = scale
                glowView.alpha = 0.3f + (value * 0.4f)
            }
            start()
        }
    }

    private fun setState(state: AssistantState) {
        currentState = state
        handler.post {
            when (state) {
                AssistantState.LISTENING -> {
                    statusText?.text = "Listening..."
                    statusText?.setTextColor(Color.parseColor("#00D9FF"))
                }
                AssistantState.THINKING -> {
                    statusText?.text = "Thinking..."
                    statusText?.setTextColor(Color.parseColor("#FFD700"))
                }
                AssistantState.SPEAKING -> {
                    statusText?.text = "Speaking..."
                    statusText?.setTextColor(Color.parseColor("#00FF88"))
                }
                AssistantState.IDLE -> {
                    statusText?.text = "Tap to dismiss"
                    statusText?.setTextColor(Color.parseColor("#888888"))
                }
            }
        }
    }

    private fun startRecording() {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val sampleRate = 16000
                val channelConfig = AudioFormat.CHANNEL_IN_MONO
                val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    android.util.Log.e("FRIDAI", "AudioRecord failed to initialize")
                    return@launch
                }

                audioRecord?.startRecording()
                isRecording = true

                val audioData = ByteArrayOutputStream()
                val buffer = ByteArray(bufferSize)
                var silenceCounter = 0
                val silenceThreshold = 500 // Adjust based on environment
                val maxSilenceFrames = 30 // ~2 seconds of silence to stop

                android.util.Log.d("FRIDAI", "Overlay: Recording started")

                while (isRecording && silenceCounter < maxSilenceFrames) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        audioData.write(buffer, 0, read)

                        // Check for silence (voice activity detection)
                        var sum = 0
                        for (i in 0 until read step 2) {
                            val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
                            sum += abs(sample)
                        }
                        val avg = sum / (read / 2)

                        if (avg < silenceThreshold) {
                            silenceCounter++
                        } else {
                            silenceCounter = 0
                        }
                    }
                }

                stopRecording()

                if (audioData.size() > 0) {
                    processAudio(audioData.toByteArray())
                }

            } catch (e: Exception) {
                android.util.Log.e("FRIDAI", "Recording error: ${e.message}")
            }
        }
    }

    private fun stopRecording() {
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            android.util.Log.e("FRIDAI", "Error stopping recording: ${e.message}")
        }
    }

    private fun processAudio(audioData: ByteArray) {
        setState(AssistantState.THINKING)

        serviceScope.launch(Dispatchers.IO) {
            try {
                // Create WAV file
                val wavFile = createWavFile(audioData)

                // Transcribe
                val transcript = transcribeAudio(wavFile)
                android.util.Log.d("FRIDAI", "Transcript: $transcript")

                handler.post {
                    transcriptText?.text = "\"$transcript\""
                }

                if (transcript.isNotBlank()) {
                    // Get response from Claude
                    val response = chat(transcript)
                    android.util.Log.d("FRIDAI", "Response: $response")

                    // Speak the response
                    setState(AssistantState.SPEAKING)
                    speakResponse(response)
                } else {
                    handler.post {
                        transcriptText?.text = "Didn't catch that"
                    }
                    delay(1500)
                    hideOverlay()
                }

                wavFile.delete()

            } catch (e: Exception) {
                android.util.Log.e("FRIDAI", "Process error: ${e.message}")
                handler.post {
                    transcriptText?.text = "Error: ${e.message}"
                }
                delay(2000)
                hideOverlay()
            }
        }
    }

    private fun createWavFile(pcmData: ByteArray): File {
        val wavFile = File(cacheDir, "recording.wav")
        FileOutputStream(wavFile).use { fos ->
            val totalDataLen = pcmData.size + 36
            val channels = 1
            val sampleRate = 16000
            val bitsPerSample = 16
            val byteRate = sampleRate * channels * bitsPerSample / 8

            // WAV header
            fos.write("RIFF".toByteArray())
            fos.write(intToByteArray(totalDataLen))
            fos.write("WAVE".toByteArray())
            fos.write("fmt ".toByteArray())
            fos.write(intToByteArray(16)) // Subchunk1Size
            fos.write(shortToByteArray(1)) // AudioFormat (PCM)
            fos.write(shortToByteArray(channels.toShort()))
            fos.write(intToByteArray(sampleRate))
            fos.write(intToByteArray(byteRate))
            fos.write(shortToByteArray((channels * bitsPerSample / 8).toShort()))
            fos.write(shortToByteArray(bitsPerSample.toShort()))
            fos.write("data".toByteArray())
            fos.write(intToByteArray(pcmData.size))
            fos.write(pcmData)
        }
        return wavFile
    }

    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            value.toByte(),
            (value shr 8).toByte(),
            (value shr 16).toByte(),
            (value shr 24).toByte()
        )
    }

    private fun shortToByteArray(value: Short): ByteArray {
        return byteArrayOf(
            value.toByte(),
            (value.toInt() shr 8).toByte()
        )
    }

    private suspend fun transcribeAudio(wavFile: File): String {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "audio",
                "recording.wav",
                wavFile.readBytes().toRequestBody("audio/wav".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url("$baseUrl/transcribe")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: "{}"
        val json = JSONObject(body)
        return json.optString("text", "")
    }

    private suspend fun chat(message: String): String {
        val json = JSONObject().apply {
            put("message", message)
        }

        val request = Request.Builder()
            .url("$baseUrl/chat")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: "{}"
        val responseJson = JSONObject(body)
        return responseJson.optString("response", "I couldn't process that request.")
    }

    private suspend fun speakResponse(text: String) {
        try {
            val json = JSONObject().apply {
                put("text", text)
            }

            val request = Request.Builder()
                .url("$baseUrl/speak")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            val responseJson = JSONObject(body)
            val audioBase64 = responseJson.optString("audio", "")

            if (audioBase64.isNotBlank()) {
                val audioBytes = Base64.decode(audioBase64, Base64.DEFAULT)
                playAudio(audioBytes)
            }
        } catch (e: Exception) {
            android.util.Log.e("FRIDAI", "Speak error: ${e.message}")
        }
    }

    private suspend fun playAudio(audioData: ByteArray) {
        withContext(Dispatchers.Main) {
            try {
                val tempFile = File(cacheDir, "response.mp3")
                FileOutputStream(tempFile).use { it.write(audioData) }

                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(tempFile.absolutePath)
                    prepare()
                    setOnCompletionListener {
                        tempFile.delete()
                        setState(AssistantState.IDLE)
                        // Auto-hide after speaking
                        handler.postDelayed({ hideOverlay() }, 1500)
                    }
                    start()
                }
            } catch (e: Exception) {
                android.util.Log.e("FRIDAI", "Playback error: ${e.message}")
                hideOverlay()
            }
        }
    }

    private fun vibrate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            }
        } catch (e: Exception) {
            // Ignore vibration errors
        }
    }

    private fun hideOverlay() {
        if (!isShowing) return

        stopRecording()
        mediaPlayer?.release()
        mediaPlayer = null
        pulseAnimator?.cancel()

        overlayView?.animate()
            ?.alpha(0f)
            ?.translationY(200f)
            ?.setDuration(200)
            ?.withEndAction {
                try {
                    windowManager?.removeView(overlayView)
                } catch (e: Exception) { }
                overlayView = null
                isShowing = false
                stopSelf()
            }
            ?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        stopRecording()
        mediaPlayer?.release()
        pulseAnimator?.cancel()
        try {
            if (overlayView != null) {
                windowManager?.removeView(overlayView)
            }
        } catch (e: Exception) { }
    }

    companion object {
        const val ACTION_SHOW = "com.fridai.app.SHOW_OVERLAY"
        const val ACTION_HIDE = "com.fridai.app.HIDE_OVERLAY"

        fun show(context: Context) {
            val intent = Intent(context, FridaiOverlayService::class.java).apply {
                action = ACTION_SHOW
            }
            context.startService(intent)
        }

        fun hide(context: Context) {
            val intent = Intent(context, FridaiOverlayService::class.java).apply {
                action = ACTION_HIDE
            }
            context.startService(intent)
        }
    }
}
