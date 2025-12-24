package com.fridai.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.fridai.app.FridaiApplication
import com.fridai.app.MainActivity
import com.fridai.app.R
import com.fridai.app.audio.WakeWordDetector
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * AlwaysListeningService - Background service for "Hey Friday" wake word detection
 *
 * Runs as a foreground service with a persistent notification.
 * Continuously listens for the wake word and launches AssistantActivity when detected.
 */
@AndroidEntryPoint
class AlwaysListeningService : Service() {

    @Inject
    lateinit var wakeWordDetector: WakeWordDetector

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isListening = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isListening) {
            startListening()
        }
        return START_STICKY
    }

    private fun startListening() {
        isListening = true
        serviceScope.launch {
            wakeWordDetector.startListening { detected ->
                if (detected) {
                    onWakeWordDetected()
                }
            }
        }
    }

    private fun onWakeWordDetected() {
        // Launch AssistantActivity
        val intent = Intent(this, com.fridai.app.AssistantActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, FridaiApplication.LISTENING_CHANNEL_ID)
            .setContentTitle(getString(R.string.listening_notification_title))
            .setContentText(getString(R.string.listening_notification_text))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isListening = false
        serviceScope.cancel()
        wakeWordDetector.stopListening()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
