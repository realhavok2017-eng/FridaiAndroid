package com.fridai.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * FRIDAI Avatar - Animated orb that displays mood states
 */
@Composable
fun FridaiAvatar(
    mood: String,
    isListening: Boolean,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier.size(150.dp)
) {
    // Mood colors
    val moodColor = getMoodColor(mood)
    val animatedColor by animateColorAsState(
        targetValue = moodColor,
        animationSpec = tween(500),
        label = "moodColor"
    )

    // Pulse animation for speaking
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isSpeaking) 1.15f else if (isListening) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isSpeaking) 300 else 800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Glow animation
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // Rotation for thinking
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (mood == "thinking") 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val baseRadius = (size.minDimension / 2) * 0.6f
            val scaledRadius = baseRadius * pulseScale

            // Outer glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedColor.copy(alpha = glowAlpha * 0.5f),
                        animatedColor.copy(alpha = 0f)
                    ),
                    center = center,
                    radius = scaledRadius * 1.8f
                ),
                radius = scaledRadius * 1.8f,
                center = center
            )

            // Middle glow ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedColor.copy(alpha = glowAlpha),
                        animatedColor.copy(alpha = 0.2f)
                    ),
                    center = center,
                    radius = scaledRadius * 1.3f
                ),
                radius = scaledRadius * 1.3f,
                center = center
            )

            // Main orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        animatedColor,
                        animatedColor.copy(alpha = 0.8f)
                    ),
                    center = Offset(center.x - scaledRadius * 0.3f, center.y - scaledRadius * 0.3f),
                    radius = scaledRadius
                ),
                radius = scaledRadius,
                center = center
            )

            // Inner core
            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = scaledRadius * 0.3f,
                center = Offset(center.x - scaledRadius * 0.2f, center.y - scaledRadius * 0.2f)
            )

            // Particle effects for listening
            if (isListening) {
                drawListeningParticles(center, scaledRadius, animatedColor, glowAlpha)
            }
        }
    }
}

private fun DrawScope.drawListeningParticles(
    center: Offset,
    radius: Float,
    color: Color,
    alpha: Float
) {
    val particleCount = 8
    val time = System.currentTimeMillis() / 1000f

    for (i in 0 until particleCount) {
        val angle = (i * 360f / particleCount + time * 60) * (Math.PI / 180f).toFloat()
        val distance = radius * 1.4f + sin(time * 3 + i) * radius * 0.2f
        val particleX = center.x + cos(angle) * distance
        val particleY = center.y + sin(angle) * distance

        drawCircle(
            color = color.copy(alpha = alpha * 0.7f),
            radius = 4.dp.toPx(),
            center = Offset(particleX, particleY)
        )
    }
}

private fun getMoodColor(mood: String): Color {
    return when (mood.lowercase()) {
        "chill" -> Color(0xFF00D9FF)
        "listening" -> Color(0xFF00FF88)
        "thinking" -> Color(0xFF9966FF)
        "speaking" -> Color(0xFFFF6666)
        "working" -> Color(0xFFFFAA33)
        "success" -> Color(0xFFFFD700)
        "sleeping" -> Color(0xFF6666AA)
        "preparing" -> Color(0xFFCC99FF)
        "attentive" -> Color(0xFF00E6CC)
        "searching" -> Color(0xFFFF9966)
        "confused" -> Color(0xFFFF66CC)
        "excited" -> Color(0xFFFF6699)
        else -> Color(0xFF00D9FF)
    }
}

private val EaseInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
