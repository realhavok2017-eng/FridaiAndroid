package com.fridai.app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.fridai.app.service.AlwaysListeningService

/**
 * BootReceiver - Restarts the wake word service after device reboot
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {

            val prefs = context.getSharedPreferences("fridai_settings", Context.MODE_PRIVATE)
            val wakeWordEnabled = prefs.getBoolean("wake_word_enabled", false)
            val overlayGranted = Settings.canDrawOverlays(context)
            val micPermissionGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (wakeWordEnabled && overlayGranted && micPermissionGranted) {
                android.util.Log.d("FRIDAI", "Boot receiver: Starting wake word service")
                val serviceIntent = Intent(context, AlwaysListeningService::class.java)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FRIDAI", "Boot receiver: Failed to start service: ${e.message}")
                }
            }
        }
    }
}
