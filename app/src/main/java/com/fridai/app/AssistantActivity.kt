package com.fridai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fridai.app.ui.FridaiAvatar
import com.fridai.app.ui.ConversationViewModel
import com.fridai.app.ui.theme.FridaiTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * AssistantActivity - Quick overlay triggered by system gestures
 * (long-press home, swipe from corner, etc.)
 *
 * FRIDAI is the star - full screen presence
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

    // Animated background
    val infiniteTransition = rememberInfiniteTransition(label = "overlayBg")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // ====== BACKGROUND WITH BLUR EFFECT ======
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Dark overlay
            drawRect(Color(0xFF050510).copy(alpha = 0.92f))

            // Central glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00D9FF).copy(alpha = glowAlpha),
                        Color(0xFF6C63FF).copy(alpha = glowAlpha * 0.6f),
                        Color(0xFF9966FF).copy(alpha = glowAlpha * 0.3f),
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2, size.height * 0.45f),
                    radius = size.width * 1.0f
                ),
                radius = size.width * 1.0f,
                center = Offset(size.width / 2, size.height * 0.45f)
            )
        }

        // ====== MAIN CONTENT ======
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Close button row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1A2E).copy(alpha = 0.6f))
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF00D9FF).copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.15f))

            // ====== FRIDAI AVATAR - THE STAR ======
            FridaiAvatar(
                mood = uiState.currentMood,
                isListening = uiState.isListening,
                isSpeaking = uiState.isSpeaking,
                audioLevel = uiState.audioLevel,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .aspectRatio(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { viewModel.toggleListening() }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Status text with glow effect
            StatusTextWithGlow(
                isListening = uiState.isListening,
                isSpeaking = uiState.isSpeaking,
                isThinking = uiState.isThinking,
                error = uiState.error
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Last transcription (what user said)
            if (uiState.lastTranscription.isNotEmpty()) {
                TranscriptionCard(
                    label = "You said:",
                    text = uiState.lastTranscription,
                    color = Color(0xFF00D9FF)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Last response (what FRIDAI said)
            if (uiState.lastResponse.isNotEmpty()) {
                TranscriptionCard(
                    label = "FRIDAI:",
                    text = uiState.lastResponse,
                    color = Color(0xFF9966FF)
                )
            }

            Spacer(modifier = Modifier.weight(0.2f))
        }
    }
}

@Composable
fun StatusTextWithGlow(
    isListening: Boolean,
    isSpeaking: Boolean,
    isThinking: Boolean,
    error: String?
) {
    val infiniteTransition = rememberInfiniteTransition(label = "statusPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val (text, color) = when {
        error != null -> error to Color(0xFFFF6B6B)
        isListening -> "Listening..." to Color(0xFF00FF88)
        isSpeaking -> "Speaking..." to Color(0xFFFF6B6B)
        isThinking -> "Thinking..." to Color(0xFF9966FF)
        else -> "Tap the orb to speak" to Color(0xFF00D9FF)
    }

    val isActive = isListening || isSpeaking || isThinking
    val animatedAlpha = if (isActive) pulseAlpha else 0.9f

    Text(
        text = text,
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
        color = color.copy(alpha = animatedAlpha),
        textAlign = TextAlign.Center
    )
}

@Composable
fun TranscriptionCard(
    label: String,
    text: String,
    color: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A2E).copy(alpha = 0.6f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = color.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.9f),
            lineHeight = 22.sp
        )
    }
}

private val EaseInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
