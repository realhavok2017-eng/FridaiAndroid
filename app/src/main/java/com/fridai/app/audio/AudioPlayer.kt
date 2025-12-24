package com.fridai.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * AudioPlayer - Plays TTS audio from backend
 */
@Singleton
class AudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

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
                    _isPlaying.value = false
                    tempFile.delete()
                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }

                setOnErrorListener { _, what, extra ->
                    android.util.Log.e("FRIDAI", "AudioPlayer: Error what=$what extra=$extra")
                    _isPlaying.value = false
                    tempFile.delete()
                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                    true
                }

                _isPlaying.value = true
                android.util.Log.d("FRIDAI", "AudioPlayer: Starting playback")
                start()
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
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        _isPlaying.value = false
    }
}
