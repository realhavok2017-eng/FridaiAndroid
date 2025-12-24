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
            // Write to temp file
            val tempFile = File(context.cacheDir, "tts_audio.mp3")
            FileOutputStream(tempFile).use { it.write(audioData) }

            // Release previous player
            mediaPlayer?.release()

            // Create new player
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .build()
                )

                setDataSource(tempFile.absolutePath)
                prepare()

                setOnCompletionListener {
                    _isPlaying.value = false
                    tempFile.delete()
                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }

                setOnErrorListener { _, _, _ ->
                    _isPlaying.value = false
                    tempFile.delete()
                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                    true
                }

                _isPlaying.value = true
                start()
            }

            continuation.invokeOnCancellation {
                stop()
            }
        } catch (e: Exception) {
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
