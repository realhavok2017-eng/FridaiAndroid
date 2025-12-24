package com.fridai.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FridaiApplication : Application() {

    companion object {
        const val LISTENING_CHANNEL_ID = "fridai_listening"
        const val NOTIFICATION_CHANNEL_ID = "fridai_notifications"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Channel for always-listening foreground service
            val listeningChannel = NotificationChannel(
                LISTENING_CHANNEL_ID,
                "FRIDAI Listening",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when FRIDAI is listening for wake word"
                setShowBadge(false)
            }

            // Channel for general notifications
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "FRIDAI Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications from FRIDAI"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(listeningChannel)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
}
