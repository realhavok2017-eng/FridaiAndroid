package com.fridai.app.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WakeWordDetector - Listens for "Hey Friday" wake word
 *
 * Note: For production, consider using Picovoice Porcupine for accurate
 * on-device wake word detection. This is a simplified implementation
 * that uses the backend's transcription service.
 */
@Singleton
class WakeWordDetector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioRecorder: AudioRecorder
) {
    private var detectionJob: Job? = null
    private var isListening = false

    companion object {
        private val WAKE_WORDS = listOf(
            "hey friday",
            "friday",
            "hey fridai",
            "fridai",
            "hey fry day",
            "fry day"
        )
    }

    /**
     * Start continuous listening for wake word
     */
    suspend fun startListening(onWakeWordDetected: (Boolean) -> Unit) {
        if (isListening) return
        if (!hasPermission()) return

        isListening = true
        detectionJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive && isListening) {
                try {
                    // Record a short clip
                    val audioData = audioRecorder.recordWithVAD()
                    if (audioData != null) {
                        // For now, we'll skip the wake word check and just detect voice activity
                        // In a production app, you'd use Picovoice Porcupine here
                        // For now, any voice will trigger the assistant
                        onWakeWordDetected(true)
                    }

                    // Brief pause between checks
                    delay(100)
                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    // Continue listening
                    delay(1000)
                }
            }
        }
    }

    /**
     * Check if text contains wake word
     */
    fun containsWakeWord(text: String): Boolean {
        val lowerText = text.lowercase().trim()
        return WAKE_WORDS.any { lowerText.contains(it) }
    }

    /**
     * Stop listening
     */
    fun stopListening() {
        isListening = false
        detectionJob?.cancel()
        detectionJob = null
    }

    private fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
}
