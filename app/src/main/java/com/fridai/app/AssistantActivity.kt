package com.fridai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fridai.app.ui.FridaiAvatar
import com.fridai.app.ui.ConversationViewModel
import com.fridai.app.ui.theme.FridaiTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * AssistantActivity - Launched when user triggers the assistant
 * (long-press home, swipe from corner, etc.)
 *
 * This is the quick overlay that appears over other apps.
 */
@AndroidEntryPoint
class AssistantActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FridaiTheme {
                AssistantOverlay(
                    onDismiss = { finish() }
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Close when user leaves
        finish()
    }
}

@Composable
fun AssistantOverlay(
    onDismiss: () -> Unit,
    viewModel: ConversationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Don't auto-start - wait for user to tap the avatar
    // LaunchedEffect(Unit) {
    //     viewModel.startListening()
    // }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Main assistant card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A2E)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.Gray)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // FRIDAI Avatar - tap to start listening
                FridaiAvatar(
                    mood = uiState.currentMood,
                    isListening = uiState.isListening,
                    isSpeaking = uiState.isSpeaking,
                    modifier = Modifier
                        .size(150.dp)
                        .clickable { viewModel.toggleListening() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Status text
                Text(
                    text = when {
                        uiState.isListening -> "Listening..."
                        uiState.isSpeaking -> "Speaking..."
                        uiState.isThinking -> "Thinking..."
                        uiState.error != null -> uiState.error!!
                        else -> "Tap the orb to speak"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (uiState.error != null) Color.Red else Color(0xFF00D9FF)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Last response or transcription
                if (uiState.lastTranscription.isNotEmpty()) {
                    Text(
                        text = "You: ${uiState.lastTranscription}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                if (uiState.lastResponse.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.lastResponse,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Close button
                TextButton(onClick = onDismiss) {
                    Text("Close", color = Color(0xFF00D9FF))
                }
            }
        }
    }
}
