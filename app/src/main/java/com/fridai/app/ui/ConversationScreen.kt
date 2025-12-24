package com.fridai.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Main Conversation Screen - FRIDAI is the STAR
 * Avatar dominates the screen, transcript is collapsable
 */
@Composable
fun ConversationScreen(
    viewModel: ConversationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Transcript expanded state
    var isTranscriptExpanded by remember { mutableStateOf(false) }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Animated background glow
    val infiniteTransition = rememberInfiniteTransition(label = "bgGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // ====== ANIMATED BACKGROUND ======
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Base dark background
            drawRect(Color(0xFF050510))

            // Central glow behind avatar
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00D9FF).copy(alpha = glowAlpha * 1.5f),
                        Color(0xFF6C63FF).copy(alpha = glowAlpha * 0.8f),
                        Color(0xFF9966FF).copy(alpha = glowAlpha * 0.3f),
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2, size.height * 0.4f),
                    radius = size.width * 1.2f
                ),
                radius = size.width * 1.2f,
                center = Offset(size.width / 2, size.height * 0.4f)
            )

            // Subtle ambient particles effect
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00FF88).copy(alpha = glowAlpha * 0.2f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.2f, size.height * 0.3f),
                    radius = size.width * 0.3f
                ),
                radius = size.width * 0.3f,
                center = Offset(size.width * 0.2f, size.height * 0.3f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFF6B6B).copy(alpha = glowAlpha * 0.15f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.85f, size.height * 0.5f),
                    radius = size.width * 0.25f
                ),
                radius = size.width * 0.25f,
                center = Offset(size.width * 0.85f, size.height * 0.5f)
            )
        }

        // ====== MAIN CONTENT ======
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Minimal header
            MinimalHeader()

            // ====== FRIDAI - THE STAR ======
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Spacer(modifier = Modifier.weight(0.15f))

                    // THE AVATAR - Full glory, no clipping
                    FridaiAvatar(
                        mood = uiState.currentMood,
                        isListening = uiState.isListening,
                        isSpeaking = uiState.isSpeaking,
                        audioLevel = uiState.audioLevel,
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .aspectRatio(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { viewModel.toggleListening() }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Status text with glow
                    AnimatedStatusText(
                        isListening = uiState.isListening,
                        isSpeaking = uiState.isSpeaking,
                        isThinking = uiState.isThinking,
                        error = uiState.error
                    )

                    // Quick response preview (last response, tappable to expand)
                    if (uiState.lastResponse.isNotEmpty() && !isTranscriptExpanded) {
                        Spacer(modifier = Modifier.height(16.dp))
                        QuickResponsePreview(
                            response = uiState.lastResponse,
                            onClick = { isTranscriptExpanded = true }
                        )
                    }

                    Spacer(modifier = Modifier.weight(0.2f))

                    // Mic button area
                    GlowingMicButton(
                        isListening = uiState.isListening,
                        onClick = { viewModel.toggleListening() }
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // ====== COLLAPSABLE TRANSCRIPT PANEL ======
        AnimatedVisibility(
            visible = isTranscriptExpanded,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            TranscriptPanel(
                messages = uiState.messages,
                listState = listState,
                onCollapse = { isTranscriptExpanded = false }
            )
        }

        // ====== EXPAND TRANSCRIPT BUTTON ======
        if (!isTranscriptExpanded && uiState.messages.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                TranscriptExpandButton(
                    messageCount = uiState.messages.size,
                    onClick = { isTranscriptExpanded = true }
                )
            }
        }
    }
}

@Composable
fun MinimalHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "F.R.I.D.A.I.",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00D9FF).copy(alpha = 0.9f)
        )

        IconButton(
            onClick = { /* Open settings */ },
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A1A2E).copy(alpha = 0.4f))
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color(0xFF00D9FF).copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun AnimatedStatusText(
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

    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    val (text, color) = when {
        error != null -> error to Color(0xFFFF6B6B)
        isListening -> "Listening..." to Color(0xFF00FF88)
        isSpeaking -> "Speaking..." to Color(0xFFFF6B6B)
        isThinking -> "Thinking..." to Color(0xFF9966FF)
        else -> "Tap to speak" to Color(0xFF00D9FF)
    }

    val isActive = isListening || isSpeaking || isThinking
    val animatedAlpha = if (isActive) pulseAlpha else 0.9f

    Box(contentAlignment = Alignment.Center) {
        // Glow behind text when active
        if (isActive) {
            Text(
                text = text,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = color.copy(alpha = 0.3f),
                textAlign = TextAlign.Center,
                modifier = Modifier.scale(glowScale)
            )
        }

        Text(
            text = text,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = color.copy(alpha = animatedAlpha),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun QuickResponsePreview(
    response: String,
    onClick: () -> Unit
) {
    val previewText = if (response.length > 100) {
        response.take(100) + "..."
    } else response

    Box(
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A2E).copy(alpha = 0.5f))
            .border(1.dp, Color(0xFF6C63FF).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = previewText,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Default.ExpandMore,
                contentDescription = "Expand",
                tint = Color(0xFF00D9FF).copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun GlowingMicButton(
    isListening: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "micGlow")

    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.4f else 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening) 400 else 2500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    val buttonColor by animateColorAsState(
        targetValue = if (isListening) Color(0xFF00FF88) else Color(0xFF00D9FF),
        animationSpec = tween(300),
        label = "buttonColor"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Multi-layer glow
        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(glowScale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            buttonColor.copy(alpha = 0.3f),
                            buttonColor.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Inner glow
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(1f + (glowScale - 1f) * 0.5f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            buttonColor.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Button
        FloatingActionButton(
            onClick = onClick,
            containerColor = buttonColor,
            modifier = Modifier
                .size(68.dp)
                .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isListening) "Stop" else "Speak",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun TranscriptExpandButton(
    messageCount: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1A1A2E).copy(alpha = 0.8f))
            .border(1.dp, Color(0xFF00D9FF).copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.ChatBubbleOutline,
            contentDescription = null,
            tint = Color(0xFF00D9FF),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$messageCount messages",
            fontSize = 14.sp,
            color = Color(0xFF00D9FF)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            Icons.Default.ExpandLess,
            contentDescription = "Expand",
            tint = Color(0xFF00D9FF).copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun TranscriptPanel(
    messages: List<ChatMessage>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onCollapse: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.6f)
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E).copy(alpha = 0.98f),
                        Color(0xFF0A0A1A)
                    )
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF00D9FF).copy(alpha = 0.4f),
                        Color(0xFF6C63FF).copy(alpha = 0.2f),
                        Color.Transparent
                    )
                ),
                RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            )
    ) {
        Column {
            // Header with drag handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCollapse)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Drag handle
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF00D9FF).copy(alpha = 0.5f))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Conversation",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF00D9FF).copy(alpha = 0.8f)
                    )
                }
            }

            // Messages
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(message)
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.isUser

    val bubbleGradient = if (isUser) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF00D9FF).copy(alpha = 0.15f),
                Color(0xFF6C63FF).copy(alpha = 0.08f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF2A2A3E).copy(alpha = 0.9f),
                Color(0xFF1A1A2E).copy(alpha = 0.9f)
            )
        )
    }

    val borderColor = if (isUser) {
        Color(0xFF00D9FF).copy(alpha = 0.25f)
    } else {
        Color(0xFF9966FF).copy(alpha = 0.2f)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Sender label
        Text(
            text = if (isUser) "You" else "FRIDAI",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (isUser) Color(0xFF00D9FF).copy(alpha = 0.7f) else Color(0xFF9966FF).copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )

        // Message bubble
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = if (isUser) 18.dp else 4.dp,
                        topEnd = if (isUser) 4.dp else 18.dp,
                        bottomStart = 18.dp,
                        bottomEnd = 18.dp
                    )
                )
                .background(bubbleGradient)
                .border(
                    1.dp,
                    borderColor,
                    RoundedCornerShape(
                        topStart = if (isUser) 18.dp else 4.dp,
                        topEnd = if (isUser) 4.dp else 18.dp,
                        bottomStart = 18.dp,
                        bottomEnd = 18.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.text,
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }
    }
}

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

private val EaseInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
