package com.fridai.app.audio

import android.content.Context
import ai.picovoice.porcupine.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WakeWordDetector - Listens for wake word using Picovoice Porcupine
 *
 * Custom wake word: "Hey Friday" trained on Picovoice Console
 */
@Singleton
class WakeWordDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var porcupineManager: PorcupineManager? = null
    private var isListening = false
    private var onDetectionCallback: ((Boolean) -> Unit)? = null

    companion object {
        // Picovoice access key
        private const val ACCESS_KEY = "nLgej7cBIwR0+CK3IGR9dv3vf5Be2DemX1GUCp2whmD7r8Nx6TPQGA=="

        // Custom wake word - "Hey Friday" trained on Picovoice Console
        private const val WAKE_WORD_ASSET = "Hey-Friday_en_android_v4_0_0.ppn"
    }

    /**
     * Copy asset file to internal storage and return path
     */
    private fun copyAssetToInternalStorage(assetName: String): String {
        val outFile = File(context.filesDir, assetName)
        if (!outFile.exists()) {
            context.assets.open(assetName).use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
            android.util.Log.d("FRIDAI", "Copied $assetName to ${outFile.absolutePath}")
        }
        return outFile.absolutePath
    }

    /**
     * Start continuous listening for wake word
     */
    suspend fun startListening(onWakeWordDetected: (Boolean) -> Unit) {
        if (isListening) return

        onDetectionCallback = onWakeWordDetected

        withContext(Dispatchers.Main) {
            try {
                // Copy .ppn from assets to internal storage
                val keywordPath = copyAssetToInternalStorage(WAKE_WORD_ASSET)
                android.util.Log.d("FRIDAI", "WakeWordDetector: Using keyword file: $keywordPath")

                // Use custom "Hey Friday" wake word
                porcupineManager = PorcupineManager.Builder()
                    .setAccessKey(ACCESS_KEY)
                    .setKeywordPath(keywordPath)
                    .setSensitivity(0.7f)  // Slightly higher sensitivity
                    .build(context) { keywordIndex ->
                        // Wake word detected!
                        android.util.Log.d("FRIDAI", "🎉 HEY FRIDAY detected!")
                        onDetectionCallback?.invoke(true)
                    }

                porcupineManager?.start()
                isListening = true
                android.util.Log.d("FRIDAI", "WakeWordDetector: Listening for 'Hey Friday'...")

            } catch (e: PorcupineException) {
                android.util.Log.e("FRIDAI", "WakeWordDetector: Porcupine error: ${e.message}")
                e.printStackTrace()
                isListening = false
            } catch (e: Exception) {
                android.util.Log.e("FRIDAI", "WakeWordDetector: Error: ${e.message}")
                e.printStackTrace()
                isListening = false
            }
        }
    }

    /**
     * Stop listening
     */
    fun stopListening() {
        isListening = false
        try {
            porcupineManager?.stop()
            porcupineManager?.delete()
            porcupineManager = null
        } catch (e: Exception) {
            android.util.Log.e("FRIDAI", "WakeWordDetector: Error stopping: ${e.message}")
        }
    }

    /**
     * Check if currently listening
     */
    fun isActive(): Boolean = isListening

    /**
     * Check if text contains wake word (for transcript verification)
     */
    fun containsWakeWord(text: String): Boolean {
        val lowerText = text.lowercase().trim()
        return listOf("hey friday", "friday", "hey fridai", "fridai", "jarvis", "hey jarvis")
            .any { lowerText.contains(it) }
    }
}
