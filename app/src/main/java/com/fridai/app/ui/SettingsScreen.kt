package com.fridai.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fridai.app.network.Voice
import com.fridai.app.repository.FridaiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val voices: List<Voice> = emptyList(),
    val selectedVoiceId: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val backendConnected: Boolean = false,
    val wakeWordEnabled: Boolean = false,
    val overlayPermissionGranted: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: FridaiRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("fridai_settings", android.content.Context.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // Load persisted wake word state
        val wakeWordEnabled = prefs.getBoolean("wake_word_enabled", false)
        val overlayGranted = android.provider.Settings.canDrawOverlays(context)
        _uiState.value = _uiState.value.copy(
            wakeWordEnabled = wakeWordEnabled,
            overlayPermissionGranted = overlayGranted
        )

        // If wake word should be enabled, make sure the service is running
        val micPermissionGranted = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (wakeWordEnabled && overlayGranted && micPermissionGranted) {
            val intent = android.content.Intent(context, com.fridai.app.service.AlwaysListeningService::class.java)
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                android.util.Log.e("FRIDAI", "Failed to restart wake word service: ${e.message}")
            }
        }

        checkBackendConnection()
        loadVoices()
    }

    fun checkOverlayPermission(): Boolean {
        val granted = android.provider.Settings.canDrawOverlays(context)
        _uiState.value = _uiState.value.copy(overlayPermissionGranted = granted)
        return granted
    }

    private fun checkBackendConnection() {
        viewModelScope.launch {
            repository.healthCheck().onSuccess {
                _uiState.value = _uiState.value.copy(backendConnected = it)
            }.onFailure {
                _uiState.value = _uiState.value.copy(backendConnected = false)
            }
        }
    }

    private fun loadVoices() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.getVoices().onSuccess { voices ->
                _uiState.value = _uiState.value.copy(
                    voices = voices,
                    isLoading = false
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }

    fun selectVoice(voiceId: String) {
        _uiState.value = _uiState.value.copy(selectedVoiceId = voiceId)
        // TODO: Call API to set voice
    }

    fun toggleWakeWord() {
        val newState = !_uiState.value.wakeWordEnabled
        _uiState.value = _uiState.value.copy(wakeWordEnabled = newState)

        // Persist the state
        prefs.edit().putBoolean("wake_word_enabled", newState).apply()

        val intent = android.content.Intent(context, com.fridai.app.service.AlwaysListeningService::class.java)
        if (newState) {
            // Start listening service
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            android.util.Log.d("FRIDAI", "Wake word service STARTED")
        } else {
            // Stop listening service
            context.stopService(intent)
            android.util.Log.d("FRIDAI", "Wake word service STOPPED")
        }
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050510))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            SettingsHeader(onBack = onBack)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Connection Status
                item {
                    SettingsSection(title = "Connection Status") {
                        ConnectionStatusCard(isConnected = uiState.backendConnected)
                    }
                }

                // Voice Selection
                item {
                    SettingsSection(title = "Voice") {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(16.dp),
                                color = Color(0xFF00D9FF)
                            )
                        } else if (uiState.voices.isEmpty()) {
                            Text(
                                text = "No voices available",
                                color = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.padding(16.dp)
                            )
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                uiState.voices.forEach { voice ->
                                    VoiceOption(
                                        voice = voice,
                                        isSelected = voice.id == uiState.selectedVoiceId,
                                        onClick = { viewModel.selectVoice(voice.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Wake Word Section
                item {
                    val localContext = androidx.compose.ui.platform.LocalContext.current
                    SettingsSection(title = "Wake Word") {
                        WakeWordCard(
                            isEnabled = uiState.wakeWordEnabled,
                            hasOverlayPermission = uiState.overlayPermissionGranted,
                            onToggle = {
                                if (!viewModel.checkOverlayPermission()) {
                                    // Open overlay permission settings
                                    val intent = android.content.Intent(
                                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        android.net.Uri.parse("package:${localContext.packageName}")
                                    )
                                    localContext.startActivity(intent)
                                } else {
                                    viewModel.toggleWakeWord()
                                }
                            },
                            onRequestOverlayPermission = {
                                val intent = android.content.Intent(
                                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    android.net.Uri.parse("package:${localContext.packageName}")
                                )
                                localContext.startActivity(intent)
                            }
                        )
                    }
                }

                // About Section
                item {
                    SettingsSection(title = "About") {
                        AboutCard()
                    }
                }

                // Permissions Section
                item {
                    val localContext = androidx.compose.ui.platform.LocalContext.current
                    SettingsSection(title = "Permissions") {
                        Column {
                            // Microphone Permission
                            SettingsRow(
                                icon = Icons.Default.Mic,
                                title = "Microphone",
                                subtitle = "Required for voice commands",
                                onClick = {
                                    val intent = android.content.Intent(
                                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        android.net.Uri.parse("package:${localContext.packageName}")
                                    )
                                    localContext.startActivity(intent)
                                }
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = Color.White.copy(alpha = 0.1f)
                            )

                            // Overlay Permission
                            SettingsRow(
                                icon = Icons.Default.Layers,
                                title = "Display Over Apps",
                                subtitle = if (uiState.overlayPermissionGranted) "Granted" else "Required for overlay",
                                statusColor = if (uiState.overlayPermissionGranted) Color(0xFF00FF88) else Color(0xFFFFD700),
                                onClick = {
                                    val intent = android.content.Intent(
                                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        android.net.Uri.parse("package:${localContext.packageName}")
                                    )
                                    localContext.startActivity(intent)
                                }
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = Color.White.copy(alpha = 0.1f)
                            )

                            // Battery Optimization
                            SettingsRow(
                                icon = Icons.Default.BatteryChargingFull,
                                title = "Battery Optimization",
                                subtitle = "Disable for reliable wake word",
                                onClick = {
                                    try {
                                        val intent = android.content.Intent(
                                            android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                            android.net.Uri.parse("package:${localContext.packageName}")
                                        )
                                        localContext.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Fallback to battery settings
                                        val intent = android.content.Intent(
                                            android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                                        )
                                        localContext.startActivity(intent)
                                    }
                                }
                            )
                        }
                    }
                }

                // System Settings Section
                item {
                    val localContext = androidx.compose.ui.platform.LocalContext.current
                    SettingsSection(title = "System") {
                        Column {
                            // Default Assistant
                            SettingsRow(
                                icon = Icons.Default.Assistant,
                                title = "Default Assistant",
                                subtitle = "Set FRIDAI as your assistant",
                                onClick = {
                                    try {
                                        // Try to open assistant settings directly
                                        val intent = android.content.Intent(android.provider.Settings.ACTION_VOICE_INPUT_SETTINGS)
                                        localContext.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Fallback to app settings
                                        val intent = android.content.Intent(
                                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            android.net.Uri.parse("package:${localContext.packageName}")
                                        )
                                        localContext.startActivity(intent)
                                    }
                                }
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = Color.White.copy(alpha = 0.1f)
                            )

                            // Notifications
                            SettingsRow(
                                icon = Icons.Default.Notifications,
                                title = "Notifications",
                                subtitle = "Manage notification settings",
                                onClick = {
                                    val intent = android.content.Intent().apply {
                                        action = android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                        putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, localContext.packageName)
                                    }
                                    localContext.startActivity(intent)
                                }
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = Color.White.copy(alpha = 0.1f)
                            )

                            // App Info
                            SettingsRow(
                                icon = Icons.Default.Info,
                                title = "App Info",
                                subtitle = "Storage, permissions, and more",
                                onClick = {
                                    val intent = android.content.Intent(
                                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        android.net.Uri.parse("package:${localContext.packageName}")
                                    )
                                    localContext.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A1A2E).copy(alpha = 0.6f))
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFF00D9FF)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = "Settings",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00D9FF)
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF00D9FF).copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1A1A2E).copy(alpha = 0.6f))
                .border(1.dp, Color(0xFF00D9FF).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            content()
        }
    }
}

@Composable
fun ConnectionStatusCard(isConnected: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(if (isConnected) Color(0xFF00FF88) else Color(0xFFFF6B6B))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = if (isConnected) "Connected to FRIDAI Backend" else "Disconnected",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Text(
                text = "fridai.fridai.me",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun VoiceOption(
    voice: Voice,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) Color(0xFF00D9FF).copy(alpha = 0.15f)
                else Color.Transparent
            )
            .border(
                1.dp,
                if (isSelected) Color(0xFF00D9FF).copy(alpha = 0.5f)
                else Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(0xFF00D9FF),
                unselectedColor = Color.White.copy(alpha = 0.5f)
            )
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = voice.name,
            fontSize = 16.sp,
            color = Color.White
        )
    }
}

@Composable
fun AboutCard() {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00D9FF),
                                Color(0xFF6C63FF)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "F",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "F.R.I.D.A.I.",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Female Responsive Intelligent Digital AI Interface",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Version 1.0.0",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    statusColor: Color? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF00D9FF),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = statusColor ?: Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.3f)
        )
    }
}

@Composable
fun WakeWordCard(
    isEnabled: Boolean,
    hasOverlayPermission: Boolean = true,
    onToggle: () -> Unit,
    onRequestOverlayPermission: () -> Unit = {}
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hey Friday",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = when {
                        !hasOverlayPermission -> "Overlay permission required"
                        isEnabled -> "Listening in background"
                        else -> "Tap to enable"
                    },
                    fontSize = 12.sp,
                    color = when {
                        !hasOverlayPermission -> Color(0xFFFFD700)
                        isEnabled -> Color(0xFF00FF88)
                        else -> Color.White.copy(alpha = 0.5f)
                    }
                )
            }

            if (!hasOverlayPermission) {
                Button(
                    onClick = onRequestOverlayPermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00D9FF)
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Grant", color = Color.White, fontSize = 14.sp)
                }
            } else {
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF00D9FF),
                        checkedTrackColor = Color(0xFF00D9FF).copy(alpha = 0.5f),
                        uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (hasOverlayPermission)
                "Say \"Hey Friday\" to activate FRIDAI hands-free from anywhere on your phone."
            else
                "FRIDAI needs permission to display over other apps for the overlay assistant.",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.4f),
            lineHeight = 16.sp
        )
    }
}

