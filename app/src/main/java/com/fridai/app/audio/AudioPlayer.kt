package com.fridai.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.audiofx.Visualizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.math.abs

/**
 * AudioPlayer - Plays TTS audio from backend with audio level visualization
 */
@Singleton
class AudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null
    private var visualizer: Visualizer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel

    /**
     * Play audio from byte array (MP3 from backend)
     */
    suspend fun playAudio(audioData: ByteArray) = suspendCancellableCoroutine { continuation ->
        try {
            android.util.Log.d("FRIDAI", "AudioPlayer: Received ${audioData.size} bytes")

            // Write to temp file
            val tempFile = File(context.cacheDir, "tts_audio.mp3")
            FileOutputStream(tempFile).use { it.write(audioData) }
            android.util.Log.d("FRIDAI", "AudioPlayer: Wrote to temp file ${tempFile.absolutePath}")

            // Release previous player
            mediaPlayer?.release()

            // Create new player
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)  // Changed from ASSISTANT to MEDIA for better compatibility
                        .build()
                )

                setDataSource(tempFile.absolutePath)
                android.util.Log.d("FRIDAI", "AudioPlayer: Set data source")

                prepare()
                android.util.Log.d("FRIDAI", "AudioPlayer: Prepared, duration=${duration}ms")

                setOnCompletionListener {
                    android.util.Log.d("FRIDAI", "AudioPlayer: Playback completed")
                    releaseVisualizer()
                    _isPlaying.value = false
                    _audioLevel.value = 0f
                    tempFile.delete()
                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }

                setOnErrorListener { _, what, extra ->
                    android.util.Log.e("FRIDAI", "AudioPlayer: Error what=$what extra=$extra")
                    releaseVisualizer()
                    _isPlaying.value = false
                    _audioLevel.value = 0f
                    tempFile.delete()
                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                    true
                }

                _isPlaying.value = true
                android.util.Log.d("FRIDAI", "AudioPlayer: Starting playback")
                start()

                // Attach visualizer to capture audio levels for avatar animation
                try {
                    setupVisualizer(audioSessionId)
                } catch (e: Exception) {
                    android.util.Log.e("FRIDAI", "AudioPlayer: Visualizer setup failed: ${e.message}")
                }
            }

            continuation.invokeOnCancellation {
                android.util.Log.d("FRIDAI", "AudioPlayer: Cancelled")
                stop()
            }
        } catch (e: Exception) {
            android.util.Log.e("FRIDAI", "AudioPlayer: Exception ${e.message}", e)
            _isPlaying.value = false
            if (continuation.isActive) {
                continuation.resume(Unit)
            }
        }
    }

    /**
     * Stop playback
     */
    fun stop() {
        releaseVisualizer()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        _isPlaying.value = false
        _audioLevel.value = 0f
    }

    /**
     * Setup visualizer to capture audio levels for avatar animation
     */
    private fun setupVisualizer(audioSessionId: Int) {
        releaseVisualizer()

        visualizer = Visualizer(audioSessionId).apply {
            captureSize = Visualizer.getCaptureSizeRange()[0]  // Minimum size for performance

            setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(
                    visualizer: Visualizer?,
                    waveform: ByteArray?,
                    samplingRate: Int
                ) {
                    waveform?.let { data ->
                        // Calculate RMS amplitude from waveform
                        var sum = 0L
                        for (byte in data) {
                            val sample = (byte.toInt() and 0xFF) - 128
                            sum += sample * sample
                        }
                        val rms = kotlin.math.sqrt(sum.toDouble() / data.size).toFloat()
                        // Normalize to 0-1 range (128 is max amplitude for unsigned byte centered at 128)
                        val normalized = (rms / 64f).coerceIn(0f, 1f)
                        _audioLevel.value = normalized
                    }
                }

                override fun onFftDataCapture(
                    visualizer: Visualizer?,
                    fft: ByteArray?,
                    samplingRate: Int
                ) {
                    // Not using FFT data
                }
            }, Visualizer.getMaxCaptureRate() / 2, true, false)

            enabled = true
        }
        android.util.Log.d("FRIDAI", "AudioPlayer: Visualizer attached")
    }

    /**
     * Release visualizer resources
     */
    private fun releaseVisualizer() {
        visualizer?.enabled = false
        visualizer?.release()
        visualizer = null
    }
}
