package com.fridai.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.fridai.app.service.AlwaysListeningService
import com.fridai.app.ui.ConversationScreen
import com.fridai.app.ui.SettingsScreen
import com.fridai.app.ui.theme.FridaiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            startListeningService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FridaiTheme {
                var showSetupDialog by remember { mutableStateOf(!hasRequiredPermissions()) }
                var showSettings by remember { mutableStateOf(false) }

                if (showSetupDialog) {
                    SetupDialog(
                        onRequestPermissions = {
                            requestPermissions()
                            showSetupDialog = false
                        },
                        onOpenAssistantSettings = {
                            openAssistantSettings()
                        }
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (showSettings) {
                        SettingsScreen(
                            onBack = { showSettings = false }
                        )
                    } else {
                        ConversationScreen(
                            onOpenSettings = { showSettings = true }
                        )
                    }
                }
            }
        }

        // Restore wake word service if it was previously enabled
        restoreWakeWordService()
    }

    private fun restoreWakeWordService() {
        val prefs = getSharedPreferences("fridai_settings", MODE_PRIVATE)
        val wakeWordEnabled = prefs.getBoolean("wake_word_enabled", false)
        val overlayGranted = Settings.canDrawOverlays(this)
        val micPermissionGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (wakeWordEnabled && overlayGranted && micPermissionGranted) {
            android.util.Log.d("FRIDAI", "Restoring wake word service from MainActivity")
            startListeningService()
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val audioPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        return audioPermission && notificationPermission
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissionLauncher.launch(permissions.toTypedArray())
    }

    private fun startListeningService() {
        val intent = Intent(this, AlwaysListeningService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun openAssistantSettings() {
        try {
            val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general settings
            val intent = Intent(Settings.ACTION_SETTINGS)
            startActivity(intent)
        }
    }
}

@Composable
fun SetupDialog(
    onRequestPermissions: () -> Unit,
    onOpenAssistantSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text("Set Up FRIDAI") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("To use FRIDAI as your assistant:")
                Text("1. Grant microphone permission")
                Text("2. Set FRIDAI as your default Digital Assistant")

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onOpenAssistantSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Assistant Settings")
                }
            }
        },
        confirmButton = {
            Button(onClick = onRequestPermissions) {
                Text("Grant Permissions")
            }
        }
    )
}
