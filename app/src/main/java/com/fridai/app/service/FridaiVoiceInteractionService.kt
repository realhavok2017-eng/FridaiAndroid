package com.fridai.app.service

import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import com.fridai.app.AssistantActivity

/**
 * FridaiVoiceInteractionService - THE KEY to replacing Google Assistant
 *
 * This service is registered in AndroidManifest with BIND_VOICE_INTERACTION permission.
 * When user sets FRIDAI as their Digital Assistant:
 * - Long-press home button → triggers this service
 * - Swipe from corner → triggers this service
 * - "Ok Google" (if configured) → triggers this service
 */
class FridaiVoiceInteractionService : VoiceInteractionService() {

    override fun onReady() {
        super.onReady()
        // Service is ready to handle voice interactions
    }
}

/**
 * Session service that creates voice interaction sessions
 */
class FridaiVoiceInteractionSessionService : VoiceInteractionSessionService() {

    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        return FridaiVoiceInteractionSession(this)
    }
}

/**
 * The actual session that handles voice interaction
 */
class FridaiVoiceInteractionSession(
    private val context: android.content.Context
) : VoiceInteractionSession(context) {

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)

        // Launch the AssistantActivity overlay
        val intent = Intent(context, AssistantActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(intent)

        // Hide the system voice interaction UI (we're using our own)
        hide()
    }

    override fun onHandleAssist(
        data: Bundle?,
        structure: android.app.assist.AssistStructure?,
        content: android.app.assist.AssistContent?
    ) {
        // Handle assist data if needed
        // For now, we just launch our activity
    }
}
