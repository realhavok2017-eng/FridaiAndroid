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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.fridai.app.R
import com.fridai.app.ui.FridaiAvatar
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
 * Lifecycle owner for ComposeView in a Service
 */
private class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    fun onCreate() {
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun onStart() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    fun onResume() {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }
}

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

    // Lifecycle for Compose
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    // UI components
    private var avatarContainer: FrameLayout? = null
    private var statusText: TextView? = null
    private var transcriptText: TextView? = null
    private var composeView: ComposeView? = null

    // Compose state
    private val avatarMood = mutableStateOf("listening")
    private val avatarIsListening = mutableStateOf(true)
    private val avatarIsSpeaking = mutableStateOf(false)

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
        // Create lifecycle owner FIRST
        lifecycleOwner = OverlayLifecycleOwner()
        lifecycleOwner?.onCreate()

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

        // Set lifecycle on root view BEFORE adding to window
        overlayView?.setViewTreeLifecycleOwner(lifecycleOwner)
        overlayView?.setViewTreeSavedStateRegistryOwner(lifecycleOwner)

        windowManager?.addView(overlayView, params)

        // Start lifecycle AFTER adding to window
        lifecycleOwner?.onStart()
        lifecycleOwner?.onResume()

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
        // Create ComposeView with full FridaiAvatar
        // Lifecycle is already set on root view in createOverlayView
        composeView = ComposeView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

            setContent {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    FridaiAvatar(
                        mood = avatarMood.value,
                        isListening = avatarIsListening.value,
                        isSpeaking = avatarIsSpeaking.value,
                        audioLevel = 0f,
                        modifier = Modifier.size(180.dp)
                    )
                }
            }
        }
        container.addView(composeView)
    }

    private fun setState(state: AssistantState) {
        currentState = state
        handler.post {
            // Update compose avatar state
            when (state) {
                AssistantState.LISTENING -> {
                    avatarMood.value = "listening"
                    avatarIsListening.value = true
                    avatarIsSpeaking.value = false
                    statusText?.text = "Listening..."
                    statusText?.setTextColor(Color.parseColor("#00D9FF"))
                }
                AssistantState.THINKING -> {
                    avatarMood.value = "thinking"
                    avatarIsListening.value = false
                    avatarIsSpeaking.value = false
                    statusText?.text = "Thinking..."
                    statusText?.setTextColor(Color.parseColor("#FFD700"))
                }
                AssistantState.SPEAKING -> {
                    avatarMood.value = "speaking"
                    avatarIsListening.value = false
                    avatarIsSpeaking.value = true
                    statusText?.text = "Speaking..."
                    statusText?.setTextColor(Color.parseColor("#00FF88"))
                }
                AssistantState.IDLE -> {
                    avatarMood.value = "chill"
                    avatarIsListening.value = false
                    avatarIsSpeaking.value = false
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
                // Use ShortArray like the main app for proper PCM handling
                val buffer = ShortArray(bufferSize / 2)
                var silenceStart = 0L
                var hasVoice = false
                val startTime = System.currentTimeMillis()
                val silenceThreshold = 1000
                val silenceDurationMs = 1500L
                val maxRecordingMs = 10000L

                android.util.Log.d("FRIDAI", "Overlay: Recording started, bufferSize=$bufferSize")

                var logCounter = 0
                val minRecordingMs = 2000L  // Record at least 2 seconds before allowing silence stop

                while (isRecording) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        // Calculate audio level
                        val level = buffer.take(read).map { abs(it.toInt()) }.average().toFloat()

                        // Log every ~500ms (every 8 reads at 16kHz)
                        logCounter++
                        if (logCounter % 8 == 0) {
                            android.util.Log.d("FRIDAI", "Overlay: level=$level, hasVoice=$hasVoice, dataSize=${audioData.size()}")
                        }

                        // Convert shorts to bytes (little-endian) like the main app
                        for (i in 0 until read) {
                            audioData.write(buffer[i].toInt() and 0xFF)
                            audioData.write((buffer[i].toInt() shr 8) and 0xFF)
                        }

                        // Voice activity detection
                        if (level > silenceThreshold) {
                            if (!hasVoice) {
                                android.util.Log.d("FRIDAI", "Overlay: Voice DETECTED at level $level")
                            }
                            hasVoice = true
                            silenceStart = 0L
                        } else if (hasVoice && (System.currentTimeMillis() - startTime > minRecordingMs)) {
                            // Only check silence after minimum recording time
                            if (silenceStart == 0L) {
                                silenceStart = System.currentTimeMillis()
                                android.util.Log.d("FRIDAI", "Overlay: Silence started after voice")
                            } else if (System.currentTimeMillis() - silenceStart > silenceDurationMs) {
                                android.util.Log.d("FRIDAI", "Overlay: Silence detected after voice, stopping")
                                break
                            }
                        }

                        // Max recording time
                        if (System.currentTimeMillis() - startTime > maxRecordingMs) {
                            android.util.Log.d("FRIDAI", "Overlay: Max recording time reached")
                            break
                        }
                    }
                }

                stopRecording()

                android.util.Log.d("FRIDAI", "Overlay: Recording stopped. hasVoice=$hasVoice, dataSize=${audioData.size()}")

                if (hasVoice && audioData.size() > 0) {
                    processAudio(audioData.toByteArray())
                } else {
                    handler.post {
                        transcriptText?.text = "No speech detected"
                    }
                    delay(1500)
                    hideOverlay()
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
        val wavBytes = createWavBytes(pcmData)
        FileOutputStream(wavFile).use { fos ->
            fos.write(wavBytes)
        }
        android.util.Log.d("FRIDAI", "Overlay: Created WAV file with ${wavBytes.size} bytes")
        return wavFile
    }

    private fun createWavBytes(pcmData: ByteArray): ByteArray {
        val sampleRate = 16000
        val totalDataLen = pcmData.size + 36
        val byteRate = sampleRate * 1 * 16 / 8  // mono, 16-bit

        val header = ByteArray(44)

        // RIFF header
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()

        // fmt chunk
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16  // chunk size
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1  // PCM
        header[21] = 0
        header[22] = 1  // mono
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = 2  // block align
        header[33] = 0
        header[34] = 16  // bits per sample
        header[35] = 0

        // data chunk
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (pcmData.size and 0xff).toByte()
        header[41] = ((pcmData.size shr 8) and 0xff).toByte()
        header[42] = ((pcmData.size shr 16) and 0xff).toByte()
        header[43] = ((pcmData.size shr 24) and 0xff).toByte()

        return header + pcmData
    }

    private suspend fun transcribeAudio(wavFile: File): String {
        val wavBytes = wavFile.readBytes()
        android.util.Log.d("FRIDAI", "Overlay: Sending WAV as base64, size=${wavBytes.size} bytes")

        // Encode audio as base64 - this is what the backend expects
        val base64Audio = Base64.encodeToString(wavBytes, Base64.NO_WRAP)

        val json = JSONObject().apply {
            put("audio", base64Audio)
        }

        val request = Request.Builder()
            .url("$baseUrl/transcribe")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: "{}"
        android.util.Log.d("FRIDAI", "Overlay: Transcribe response code=${response.code}, body=${body.take(200)}")
        val responseJson = JSONObject(body)
        return responseJson.optString("text", "")
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

        // Must run on main thread for animations
        handler.post {
            stopRecording()
            mediaPlayer?.release()
            mediaPlayer = null
            pulseAnimator?.cancel()

            // Cleanup lifecycle
            lifecycleOwner?.onDestroy()
            lifecycleOwner = null

            overlayView?.animate()
                ?.alpha(0f)
                ?.translationY(200f)
                ?.setDuration(200)
                ?.withEndAction {
                    try {
                        windowManager?.removeView(overlayView)
                    } catch (e: Exception) { }
                    overlayView = null
                    composeView = null
                    isShowing = false
                    stopSelf()
                }
                ?.start()
        }
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
