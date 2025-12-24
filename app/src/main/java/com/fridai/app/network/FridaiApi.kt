package com.fridai.app.network

import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API interface for FRIDAI backend
 * Base URL: https://fridai.fridai.me
 */
interface FridaiApi {

    // ===== CORE ENDPOINTS =====

    @POST("chat")
    suspend fun chat(@Body request: ChatRequest): Response<ChatResponse>

    @POST("speak")
    suspend fun speak(@Body request: SpeakRequest): Response<SpeakResponse>

    @POST("transcribe")
    suspend fun transcribe(@Body request: TranscribeRequest): Response<TranscribeResponse>

    @GET("health")
    suspend fun healthCheck(): Response<HealthResponse>

    // ===== EMOTION ENDPOINTS =====

    @GET("emotion/state")
    suspend fun getEmotionState(): Response<EmotionStateResponse>

    // ===== FCM REGISTRATION =====

    @POST("fcm_subscribe")
    suspend fun registerFcmToken(@Body request: FcmTokenRequest): Response<Unit>

    // ===== VOICE ENROLLMENT =====

    @GET("voice/status")
    suspend fun getVoiceStatus(): Response<VoiceStatusResponse>

    @POST("voice/enroll/start")
    suspend fun startVoiceEnrollment(): Response<EnrollmentResponse>

    @POST("voice/enroll/complete")
    suspend fun completeVoiceEnrollment(): Response<EnrollmentResponse>

    // ===== SETTINGS =====

    @GET("voices")
    suspend fun getVoices(): Response<VoicesResponse>

    @POST("set_voice")
    suspend fun setVoice(@Body request: SetVoiceRequest): Response<Unit>
}

// ===== REQUEST MODELS =====

data class ChatRequest(
    val message: String
)

data class SpeakRequest(
    val text: String
)

data class TranscribeRequest(
    val audio: String  // Base64 encoded audio
)

data class FcmTokenRequest(
    val token: String
)

data class SetVoiceRequest(
    val voice_id: String
)

// ===== RESPONSE MODELS =====

data class ChatResponse(
    val response: String,
    val tool_results: List<ToolResult>? = null,
    val spatial_actions: List<SpatialAction>? = null,
    val spatial_state: Any? = null  // Can be String or Object
)

data class ToolResult(
    val tool: String,
    val input: Map<String, Any>? = null,
    val result: String? = null
)

data class SpatialAction(
    val action: String,
    val gesture: String? = null,
    val x: Int? = null,
    val y: Int? = null
)

data class SpeakResponse(
    val audio: String  // Base64 encoded audio
)

data class TranscribeResponse(
    val text: String,
    val speaker: SpeakerInfo? = null
)

data class SpeakerInfo(
    val is_boss: Boolean,
    val confidence: Double,
    val last_verified: String? = null
)

data class HealthResponse(
    val status: String,
    val message: String? = null
)

data class EmotionStateResponse(
    val current_emotion: String,
    val intensity: Int,
    val last_emotion: String? = null,
    val description: String? = null
)

data class VoiceStatusResponse(
    val boss_enrolled: Boolean,
    val enrollment_active: Boolean,
    val similarity_threshold: Double
)

data class EnrollmentResponse(
    val success: Boolean,
    val message: String? = null,
    val samples_needed: Int? = null,
    val samples_collected: Int? = null
)

data class VoicesResponse(
    val voices: List<Voice>
)

data class Voice(
    val id: String,
    val name: String
)
