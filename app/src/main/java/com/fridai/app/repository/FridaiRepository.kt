package com.fridai.app.repository

import android.util.Base64
import com.fridai.app.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for all FRIDAI API interactions
 */
@Singleton
class FridaiRepository @Inject constructor(
    private val api: FridaiApi
) {
    /**
     * Send a chat message and get response
     */
    suspend fun chat(message: String): Result<ChatResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.chat(ChatRequest(message))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Chat failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Convert text to speech (returns base64 audio)
     */
    suspend fun speak(text: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val response = api.speak(SpeakRequest(text))
            if (response.isSuccessful && response.body() != null) {
                val base64Audio = response.body()!!.audio
                val audioBytes = Base64.decode(base64Audio, Base64.DEFAULT)
                Result.success(audioBytes)
            } else {
                Result.failure(Exception("Speak failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Transcribe audio to text
     */
    suspend fun transcribe(audioBytes: ByteArray): Result<TranscribeResponse> = withContext(Dispatchers.IO) {
        try {
            val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
            val response = api.transcribe(TranscribeRequest(base64Audio))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Transcribe failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get current emotion state
     */
    suspend fun getEmotionState(): Result<EmotionStateResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.getEmotionState()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Get emotion failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Register FCM token with backend
     */
    suspend fun registerFcmToken(token: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.registerFcmToken(FcmTokenRequest(token))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Register FCM failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if backend is healthy
     */
    suspend fun healthCheck(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = api.healthCheck()
            Result.success(response.isSuccessful && response.body()?.status == "ok")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get available voices
     */
    suspend fun getVoices(): Result<List<Voice>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getVoices()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.voices)
            } else {
                Result.failure(Exception("Get voices failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
