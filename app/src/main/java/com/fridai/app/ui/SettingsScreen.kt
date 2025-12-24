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
    val backendConnected: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: FridaiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        checkBackendConnection()
        loadVoices()
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

                // About Section
                item {
                    SettingsSection(title = "About") {
                        AboutCard()
                    }
                }

                // Assistant Settings
                item {
                    SettingsSection(title = "System") {
                        Column {
                            // Assistant Settings Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { /* Open system assistant settings */ }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = Color(0xFF00D9FF),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Assistant Settings", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                    Text("Set FRIDAI as default", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                }
                                Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(alpha = 0.3f))
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Notifications Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { /* Open notification settings */ }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = Color(0xFF00D9FF),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Notifications", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                    Text("Push notification settings", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                }
                                Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(alpha = 0.3f))
                            }
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

