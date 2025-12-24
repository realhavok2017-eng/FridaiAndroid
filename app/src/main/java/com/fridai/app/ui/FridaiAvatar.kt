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
    audioLevel: Float = 0f,  // Real-time audio level (0-1) for voice reactivity
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

    // Wobble intensity based on state AND actual audio level
    val wobbleIntensity = when {
        isSpeaking -> 0.08f + audioLevel * 0.1f
        isListening -> 0.04f + audioLevel * 0.15f  // React strongly to voice input
        else -> 0.025f
    }

    // Voice reactive intensity - driven by actual audio
    val voiceReactIntensity = when {
        isSpeaking -> 0.15f + voicePulse * 0.1f + audioLevel * 0.2f
        isListening -> 0.08f + audioLevel * 0.3f  // Strong reaction to user's voice
        else -> 0.03f
    }

    // Audio-driven boost for facets - STRONG response
    val audioBoost = audioLevel * 3f  // Amplified for dramatic visual effect

    // Capture audioLevel for Canvas scope
    val currentAudioLevel = audioLevel

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

            // === 2. WOBBLY GLASS SPHERE with voice waveform ===
            drawWobblyGlassSphere(
                center = center,
                baseRadius = baseRadius,
                color = baseColor,
                wobble1 = wobble1,
                wobble2 = wobble2,
                wobble3 = wobble3,
                wobbleIntensity = wobbleIntensity,
                voicePulse = if (isSpeaking) voicePulse else 0f,
                audioLevel = currentAudioLevel  // Real-time voice response
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

            // === 4. INTERNAL GEOMETRIC FACETS - crystalline structure inside ===
            drawInternalFacets(
                center = center,
                radius = baseRadius,
                color = baseColor,
                rotation = sphereRotation,
                wobble1 = wobble1,
                wobble2 = wobble2,
                isSpeaking = isSpeaking,
                isListening = isListening,
                audioBoost = audioBoost
            )

            // === 5. INNER ENERGY GLOW - pulses with voice ===
            val voiceGlow = currentAudioLevel * 0.4f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        baseColor.copy(alpha = ((coreGlow + voiceGlow) * 0.6f).coerceIn(0f, 1f)),
                        baseColor.copy(alpha = ((coreGlow + voiceGlow) * 0.3f).coerceIn(0f, 1f)),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * (0.6f + currentAudioLevel * 0.1f)
                ),
                radius = baseRadius * (0.55f + currentAudioLevel * 0.08f),
                center = center
            )

            // === 6. BRIGHT INNER CORE - expands with voice ===
            val coreExpand = 1f + currentAudioLevel * 0.15f
            // Core glow halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = (coreGlow + currentAudioLevel * 0.3f).coerceIn(0f, 1f)),
                        baseColor.copy(alpha = ((coreGlow + currentAudioLevel * 0.2f) * 0.8f).coerceIn(0f, 1f)),
                        baseColor.copy(alpha = (coreGlow * 0.3f).coerceIn(0f, 1f)),
                        Color.Transparent
                    ),
                    center = center,
                    radius = coreRadius * 2f * coreExpand
                ),
                radius = coreRadius * 1.8f * coreExpand,
                center = center
            )

            // Solid bright core - pulses with voice
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        Color.White.copy(alpha = 0.95f),
                        baseColor.copy(alpha = 0.9f)
                    ),
                    center = Offset(center.x - coreRadius * 0.2f, center.y - coreRadius * 0.2f),
                    radius = coreRadius * 1.2f * coreExpand
                ),
                radius = coreRadius * coreExpand,
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
 * Draw internal geometric facets - crystalline structure visible inside the sphere
 * Creates that 3D glass ball with rotating geometric shapes inside
 */
private fun DrawScope.drawInternalFacets(
    center: Offset,
    radius: Float,
    color: Color,
    rotation: Float,
    wobble1: Float,
    wobble2: Float,
    isSpeaking: Boolean,
    isListening: Boolean,
    audioBoost: Float = 0f
) {
    // Multiple layers of facets at different depths
    val layers = listOf(
        Triple(0.85f, 8, 0f),      // Outer layer - 8 facets
        Triple(0.7f, 6, 30f),      // Middle layer - 6 facets, offset
        Triple(0.55f, 5, 15f)      // Inner layer - 5 facets, offset
    )

    for ((layerRadius, numFacets, angleOffset) in layers) {
        val layerR = radius * layerRadius

        for (i in 0 until numFacets) {
            val baseAngle = (i.toFloat() / numFacets) * 360f + angleOffset + rotation

            // Add wobble to facet position - driven by actual audio level
            val audioWobble = audioBoost * 15f  // Audio drives extra movement
            val wobbleOffset = if (isSpeaking) {
                sin((wobble1 + i * 40) * PI.toFloat() / 180f) * (8f + audioWobble) +
                cos((wobble2 + i * 60) * PI.toFloat() / 180f) * (5f + audioWobble * 0.5f)
            } else if (isListening) {
                sin((wobble1 + i * 40) * PI.toFloat() / 180f) * (4f + audioWobble) +
                audioWobble * cos((wobble2 + i * 30) * PI.toFloat() / 180f)  // React to voice
            } else {
                sin((wobble1 + i * 40) * PI.toFloat() / 180f) * 2f
            }

            val angle1 = (baseAngle + wobbleOffset) * PI.toFloat() / 180f
            val angle2 = (baseAngle + 360f / numFacets * 0.4f + wobbleOffset) * PI.toFloat() / 180f
            val angle3 = (baseAngle + 360f / numFacets * 0.6f + wobbleOffset * 0.5f) * PI.toFloat() / 180f

            // Facet vertices - triangular panels
            val v1x = center.x + cos(angle1) * layerR * 0.3f
            val v1y = center.y + sin(angle1) * layerR * 0.3f
            val v2x = center.x + cos(angle2) * layerR
            val v2y = center.y + sin(angle2) * layerR
            val v3x = center.x + cos(angle3) * layerR * 0.8f
            val v3y = center.y + sin(angle3) * layerR * 0.8f

            // Facet alpha varies with rotation, activity, AND audio level
            val facetAlpha = (0.15f +
                sin((rotation + i * 45) * PI.toFloat() / 180f) * 0.1f +
                (if (isSpeaking) 0.15f else if (isListening) 0.08f else 0f) +
                audioBoost * 0.25f  // Brighten with voice
            ).coerceIn(0.05f, 0.5f)

            // Draw facet as a filled triangle
            val facetPath = Path().apply {
                moveTo(v1x, v1y)
                lineTo(v2x, v2y)
                lineTo(v3x, v3y)
                close()
            }

            // Fill with gradient
            drawPath(
                path = facetPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        color.copy(alpha = (facetAlpha * 1.2f).coerceIn(0f, 1f)),
                        color.copy(alpha = (facetAlpha * 0.5f).coerceIn(0f, 1f))
                    ),
                    start = Offset(v1x, v1y),
                    end = Offset(v2x, v2y)
                )
            )

            // Draw facet edges for that crystalline look
            drawPath(
                path = facetPath,
                color = color.copy(alpha = (facetAlpha * 1.5f).coerceIn(0f, 1f)),
                style = Stroke(width = 1.5f)
            )
        }
    }

    // Add some connecting lines between layers for depth
    val numConnectors = if (isSpeaking) 12 else 8
    for (i in 0 until numConnectors) {
        val angle = (i.toFloat() / numConnectors) * 360f + rotation * 0.5f
        val wobble = sin((wobble1 + i * 30) * PI.toFloat() / 180f) * (if (isSpeaking) 10f else 5f)
        val angleRad = (angle + wobble) * PI.toFloat() / 180f

        val innerR = radius * 0.35f
        val outerR = radius * 0.8f

        val x1 = center.x + cos(angleRad) * innerR
        val y1 = center.y + sin(angleRad) * innerR
        val x2 = center.x + cos(angleRad) * outerR
        val y2 = center.y + sin(angleRad) * outerR

        val lineAlpha = 0.1f + sin((rotation * 2 + i * 30) * PI.toFloat() / 180f) * 0.08f

        drawLine(
            color = color.copy(alpha = lineAlpha),
            start = Offset(x1, y1),
            end = Offset(x2, y2),
            strokeWidth = 1f
        )
    }
}

/**
 * Draw the wobbly glass sphere outer shell with real voice waveform response
 */
private fun DrawScope.drawWobblyGlassSphere(
    center: Offset,
    baseRadius: Float,
    color: Color,
    wobble1: Float,
    wobble2: Float,
    wobble3: Float,
    wobbleIntensity: Float,
    voicePulse: Float = 0f,
    audioLevel: Float = 0f
) {
    // Create organic wobble path with VOICE REACTIVE vertices
    val path = Path()
    val segments = 72

    // Voice creates direct surface deformation
    val voiceDeform = audioLevel * 0.25f  // Strong voice response

    for (i in 0..segments) {
        val angle = (i.toFloat() / segments) * 2 * PI.toFloat()

        // Base organic wobble
        val w1 = sin(angle * 3 + wobble1 * PI.toFloat() / 180f) * wobbleIntensity
        val w2 = sin(angle * 5 + wobble2 * PI.toFloat() / 180f) * wobbleIntensity * 0.5f
        val w3 = sin(angle * 7 + wobble3 * PI.toFloat() / 180f) * wobbleIntensity * 0.3f

        // VOICE WAVEFORM - creates ripples across surface
        // Different vertices respond to voice at different phases
        val voicePhase = angle * 8 + wobble1 * PI.toFloat() / 90f
        val voiceRipple = sin(voicePhase) * voiceDeform
        val voiceRipple2 = cos(angle * 12 + wobble2 * PI.toFloat() / 60f) * voiceDeform * 0.6f
        val voiceRipple3 = sin(angle * 16 + wobble3 * PI.toFloat() / 45f) * voiceDeform * 0.4f

        // Combine all deformations
        val totalDeform = w1 + w2 + w3 + voiceRipple + voiceRipple2 + voiceRipple3
        val wobbleRadius = baseRadius * (1f + totalDeform)

        val x = center.x + cos(angle) * wobbleRadius
        val y = center.y + sin(angle) * wobbleRadius

        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()

    // Main sphere body - brightens with voice
    val voiceBrightness = audioLevel * 0.2f
    drawPath(
        path = path,
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = (0.1f + voiceBrightness).coerceIn(0f, 1f)),
                color.copy(alpha = (0.25f + voiceBrightness).coerceIn(0f, 1f)),
                color.copy(alpha = (0.4f + voiceBrightness).coerceIn(0f, 1f)),
                color.copy(alpha = 0.3f)
            ),
            center = Offset(center.x - baseRadius * 0.3f, center.y - baseRadius * 0.3f),
            radius = baseRadius * 1.5f
        )
    )

    // Rim highlight - reacts to voice
    drawPath(
        path = path,
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                Color.Transparent,
                color.copy(alpha = (0.5f + audioLevel * 0.3f).coerceIn(0f, 1f)),
                color.copy(alpha = (0.8f + audioLevel * 0.2f).coerceIn(0f, 1f))
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
