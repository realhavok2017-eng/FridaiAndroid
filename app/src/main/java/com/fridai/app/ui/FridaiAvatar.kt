package com.fridai.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

/**
 * FRIDAI Avatar - 3D Glass Sphere with Wobbly Water Balloon Effect
 * Organic, fluid energy that responds to voice activity
 */
@Composable
fun FridaiAvatar(
    mood: String,
    isListening: Boolean,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier.size(200.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatar")
    val baseColor = getMoodColor(mood)

    // === SLOW SPHERE ROTATION ===
    val sphereRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sphereRotation"
    )

    // === WOBBLE PHASE 1 - Primary organic deformation ===
    val wobble1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when {
                    isSpeaking -> 600
                    isListening -> 1200
                    else -> 3000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "wobble1"
    )

    // === WOBBLE PHASE 2 - Secondary offset wobble ===
    val wobble2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when {
                    isSpeaking -> 800
                    isListening -> 1800
                    else -> 4500
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "wobble2"
    )

    // === WOBBLE PHASE 3 - Tertiary micro wobble ===
    val wobble3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when {
                    isSpeaking -> 400
                    isListening -> 900
                    else -> 2200
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "wobble3"
    )

    // === CORE GLOW - pulses with activity ===
    val coreGlow by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when {
                    isSpeaking -> 200
                    isListening -> 600
                    else -> 2000
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coreGlow"
    )

    // === SOUND WAVE for speaking ===
    val waveProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveProgress"
    )

    // === SURFACE SHIMMER ===
    val shimmerPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when {
                    isSpeaking -> 1500
                    isListening -> 3000
                    else -> 6000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    // === VOICE REACTIVE PULSE - fast outer ring pulse when speaking ===
    val voicePulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when {
                    isSpeaking -> 120  // Very fast reactive pulse
                    isListening -> 300
                    else -> 800
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voicePulse"
    )

    // === OUTER RING WAVE - ripples on outer edge ===
    val outerWave1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isSpeaking) 250 else 600,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "outerWave1"
    )

    val outerWave2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isSpeaking) 180 else 450,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "outerWave2"
    )

    // Wobble intensity based on state
    val wobbleIntensity = when {
        isSpeaking -> 0.08f
        isListening -> 0.05f
        else -> 0.025f
    }

    // Voice reactive intensity for outer effects
    val voiceReactIntensity = when {
        isSpeaking -> 0.15f + voicePulse * 0.1f
        isListening -> 0.08f + voicePulse * 0.04f
        else -> 0.03f
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val baseRadius = size.minDimension / 2 * 0.42f
            val coreRadius = baseRadius * 0.35f

            // === 1. OUTER ATMOSPHERE GLOW ===
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        baseColor.copy(alpha = 0.15f),
                        baseColor.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.8f
                ),
                radius = baseRadius * 1.8f,
                center = center
            )

            // === 1.5. VOICE REACTIVE OUTER RINGS ===
            if (isSpeaking || isListening) {
                drawVoiceReactiveRings(
                    center = center,
                    baseRadius = baseRadius,
                    color = baseColor,
                    wave1 = outerWave1,
                    wave2 = outerWave2,
                    intensity = voiceReactIntensity,
                    isSpeaking = isSpeaking
                )
            }

            // === 2. WOBBLY GLASS SPHERE ===
            drawWobblyGlassSphere(
                center = center,
                baseRadius = baseRadius,
                color = baseColor,
                wobble1 = wobble1,
                wobble2 = wobble2,
                wobble3 = wobble3,
                wobbleIntensity = wobbleIntensity,
                voicePulse = if (isSpeaking) voicePulse else 0f
            )

            // === 3. ROTATING WOBBLY SURFACE BANDS ===
            rotate(sphereRotation, center) {
                drawWobblySurfaceBands(
                    center = center,
                    radius = baseRadius,
                    color = baseColor,
                    shimmerPhase = shimmerPhase,
                    wobble1 = wobble1,
                    wobble2 = wobble2,
                    wobbleIntensity = wobbleIntensity
                )
            }

            // === 4. SOUND WAVES when speaking ===
            if (isSpeaking) {
                drawFluidSoundWaves(
                    center = center,
                    radius = baseRadius,
                    color = baseColor,
                    progress = waveProgress,
                    wobble = wobble1
                )
            }

            // === 5. INNER ENERGY GLOW ===
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        baseColor.copy(alpha = coreGlow * 0.6f),
                        baseColor.copy(alpha = coreGlow * 0.3f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 0.6f
                ),
                radius = baseRadius * 0.55f,
                center = center
            )

            // === 6. BRIGHT INNER CORE ===
            // Core glow halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = coreGlow),
                        baseColor.copy(alpha = coreGlow * 0.8f),
                        baseColor.copy(alpha = coreGlow * 0.3f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = coreRadius * 2f
                ),
                radius = coreRadius * 1.8f,
                center = center
            )

            // Solid bright core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        Color.White.copy(alpha = 0.95f),
                        baseColor.copy(alpha = 0.9f)
                    ),
                    center = Offset(center.x - coreRadius * 0.2f, center.y - coreRadius * 0.2f),
                    radius = coreRadius * 1.2f
                ),
                radius = coreRadius,
                center = center
            )

            // Core specular highlight
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.9f),
                        Color.White.copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                    center = Offset(center.x - coreRadius * 0.3f, center.y - coreRadius * 0.3f),
                    radius = coreRadius * 0.5f
                ),
                radius = coreRadius * 0.4f,
                center = Offset(center.x - coreRadius * 0.25f, center.y - coreRadius * 0.25f)
            )

            // === 7. GLASS REFLECTIONS ===
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    center = Offset(center.x - baseRadius * 0.4f, center.y - baseRadius * 0.4f),
                    radius = baseRadius * 0.4f
                ),
                radius = baseRadius * 0.35f,
                center = Offset(center.x - baseRadius * 0.4f, center.y - baseRadius * 0.4f)
            )
        }
    }
}

/**
 * Draw voice reactive rings on the outer edge - pulses with speech
 */
private fun DrawScope.drawVoiceReactiveRings(
    center: Offset,
    baseRadius: Float,
    color: Color,
    wave1: Float,
    wave2: Float,
    intensity: Float,
    isSpeaking: Boolean
) {
    val numRings = if (isSpeaking) 6 else 3

    for (i in 0 until numRings) {
        val ringPath = Path()
        val segments = 48

        // Each ring has different phase offset
        val phaseOffset = i * 60f

        for (j in 0..segments) {
            val angle = (j.toFloat() / segments) * 2 * PI.toFloat()

            // Create voice-reactive wave pattern on each ring
            val w1 = sin(angle * 4 + (wave1 + phaseOffset) * PI.toFloat() / 180f) * intensity
            val w2 = sin(angle * 6 + (wave2 + phaseOffset * 1.5f) * PI.toFloat() / 180f) * intensity * 0.6f
            val w3 = sin(angle * 8 + wave1 * PI.toFloat() / 180f * 2) * intensity * 0.3f

            // Rings expand outward from sphere
            val ringOffset = 1.05f + i * 0.08f
            val wobbleRadius = baseRadius * ringOffset * (1f + w1 + w2 + w3)

            val x = center.x + cos(angle) * wobbleRadius
            val y = center.y + sin(angle) * wobbleRadius

            if (j == 0) {
                ringPath.moveTo(x, y)
            } else {
                ringPath.lineTo(x, y)
            }
        }
        ringPath.close()

        // Fade rings as they go outward
        val ringAlpha = (0.6f - i * 0.08f).coerceAtLeast(0.1f) * (if (isSpeaking) 1f else 0.5f)

        drawPath(
            path = ringPath,
            color = color.copy(alpha = ringAlpha),
            style = Stroke(width = 2f - i * 0.2f, cap = StrokeCap.Round)
        )
    }
}

/**
 * Draw the wobbly glass sphere outer shell
 */
private fun DrawScope.drawWobblyGlassSphere(
    center: Offset,
    baseRadius: Float,
    color: Color,
    wobble1: Float,
    wobble2: Float,
    wobble3: Float,
    wobbleIntensity: Float,
    voicePulse: Float = 0f
) {
    // Create organic wobble path
    val path = Path()
    val segments = 72

    // Voice pulse adds additional expansion
    val pulseExpand = 1f + voicePulse * 0.05f

    for (i in 0..segments) {
        val angle = (i.toFloat() / segments) * 2 * PI.toFloat()

        // Combine multiple sine waves for organic wobble
        val w1 = sin(angle * 3 + wobble1 * PI.toFloat() / 180f) * wobbleIntensity
        val w2 = sin(angle * 5 + wobble2 * PI.toFloat() / 180f) * wobbleIntensity * 0.5f
        val w3 = sin(angle * 7 + wobble3 * PI.toFloat() / 180f) * wobbleIntensity * 0.3f

        val wobbleRadius = baseRadius * (1f + w1 + w2 + w3) * pulseExpand

        val x = center.x + cos(angle) * wobbleRadius
        val y = center.y + sin(angle) * wobbleRadius

        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()

    // Main sphere body with glass effect - brighter when voice active
    val glassAlpha = 0.1f + voicePulse * 0.1f
    drawPath(
        path = path,
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = glassAlpha),
                color.copy(alpha = 0.25f + voicePulse * 0.1f),
                color.copy(alpha = 0.4f + voicePulse * 0.15f),
                color.copy(alpha = 0.3f)
            ),
            center = Offset(center.x - baseRadius * 0.3f, center.y - baseRadius * 0.3f),
            radius = baseRadius * 1.5f
        )
    )

    // Rim highlight - pulses with voice
    drawPath(
        path = path,
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                Color.Transparent,
                color.copy(alpha = 0.5f + voicePulse * 0.2f),
                color.copy(alpha = 0.8f + voicePulse * 0.2f)
            ),
            center = center,
            radius = baseRadius
        )
    )
}

/**
 * Draw wobbly surface bands that wrap around the sphere
 */
private fun DrawScope.drawWobblySurfaceBands(
    center: Offset,
    radius: Float,
    color: Color,
    shimmerPhase: Float,
    wobble1: Float,
    wobble2: Float,
    wobbleIntensity: Float
) {
    val bandAngles = listOf(-50f, -25f, 0f, 25f, 50f)

    for ((index, baseAngle) in bandAngles.withIndex()) {
        val angle = baseAngle + shimmerPhase * 0.1f
        val bandAlpha = 0.3f + 0.2f * cos((shimmerPhase + index * 45f) * PI.toFloat() / 180f)

        // Add wobble to band position
        val wobbleOffset = sin((wobble1 + index * 30) * PI.toFloat() / 180f) * radius * wobbleIntensity

        val yOffset = radius * sin(angle * PI.toFloat() / 180f) * 0.8f + wobbleOffset
        val bandWidth = radius * 2f * cos(angle * PI.toFloat() / 180f).coerceAtLeast(0.3f)

        // Wobbly band height
        val wobbledHeight = radius * 0.15f * (1f + sin((wobble2 + index * 60) * PI.toFloat() / 180f) * wobbleIntensity * 2)

        drawArc(
            color = color.copy(alpha = bandAlpha),
            startAngle = 160f,
            sweepAngle = 220f,
            useCenter = false,
            topLeft = Offset(center.x - bandWidth / 2, center.y + yOffset - wobbledHeight / 2),
            size = Size(bandWidth, wobbledHeight),
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )
    }

    // Flowing energy chevrons
    for (i in 0..2) {
        val offsetAngle = i * 120f + shimmerPhase
        val wobbleX = sin((wobble1 + i * 45) * PI.toFloat() / 180f) * radius * wobbleIntensity
        val wobbleY = cos((wobble2 + i * 60) * PI.toFloat() / 180f) * radius * wobbleIntensity

        val startX = center.x + cos(offsetAngle * PI / 180f).toFloat() * radius * 0.6f + wobbleX
        val startY = center.y + sin(offsetAngle * PI / 180f).toFloat() * radius * 0.3f + wobbleY

        val path = Path().apply {
            moveTo(startX - radius * 0.3f, startY)
            quadraticBezierTo(
                startX + wobbleX * 0.5f, startY - radius * 0.2f + wobbleY,
                startX + radius * 0.3f, startY
            )
        }

        drawPath(
            path = path,
            color = color.copy(alpha = 0.4f),
            style = Stroke(width = 2f, cap = StrokeCap.Round)
        )
    }
}

/**
 * Draw fluid sound wave ripples when speaking
 */
private fun DrawScope.drawFluidSoundWaves(
    center: Offset,
    radius: Float,
    color: Color,
    progress: Float,
    wobble: Float
) {
    for (i in 0..2) {
        val wavePhase = (progress + i * 0.33f) % 1f
        val waveRadius = radius * (0.4f + wavePhase * 0.5f)
        val waveAlpha = (1f - wavePhase) * 0.5f

        // Add wobble to wave shape
        val wobbleAmount = sin((wobble + i * 40) * PI.toFloat() / 180f) * 0.1f

        // Horizontal wave band with wobble
        val waveHeight = waveRadius * (0.3f + wobbleAmount)
        drawArc(
            color = color.copy(alpha = waveAlpha),
            startAngle = 150f,
            sweepAngle = 240f,
            useCenter = false,
            topLeft = Offset(center.x - waveRadius, center.y - waveHeight / 2),
            size = Size(waveRadius * 2, waveHeight),
            style = Stroke(width = 2f + (1f - wavePhase) * 2f)
        )

        // Vertical ripple component
        val vWobble = cos((wobble + i * 60) * PI.toFloat() / 180f) * 0.1f
        val vWidth = waveRadius * (0.3f + vWobble)
        drawArc(
            color = color.copy(alpha = waveAlpha * 0.5f),
            startAngle = 60f,
            sweepAngle = 240f,
            useCenter = false,
            topLeft = Offset(center.x - vWidth / 2, center.y - waveRadius),
            size = Size(vWidth, waveRadius * 2),
            style = Stroke(width = 1.5f)
        )
    }
}

private fun getMoodColor(mood: String): Color {
    return when (mood.lowercase()) {
        "listening" -> Color(0xFF00FFAA)  // Bright teal-green
        "speaking" -> Color(0xFFFF6B6B)   // Coral red
        "thinking" -> Color(0xFF9966FF)   // Purple
        "working" -> Color(0xFFFFAA33)    // Orange
        "chill", "relaxed" -> Color(0xFF00D9FF)  // Cyan (default)
        "success" -> Color(0xFFFFD700)    // Gold
        "confused" -> Color(0xFFFF66AA)   // Pink
        "sleeping" -> Color(0xFF6666AA)   // Muted purple
        else -> Color(0xFF00D9FF)         // Default cyan
    }
}
