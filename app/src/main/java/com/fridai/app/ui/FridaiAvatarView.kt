package com.fridai.app.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.sin

/**
 * FridaiAvatarView - Traditional Android View version of the FRIDAI avatar
 * For use in overlay service where Compose isn't available
 */
class FridaiAvatarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class State {
        LISTENING, THINKING, SPEAKING, IDLE
    }

    var state: State = State.LISTENING
        set(value) {
            field = value
            updateColors()
        }

    private var wobblePhase1 = 0f
    private var wobblePhase2 = 0f
    private var rotationPhase = 0f
    private var pulsePhase = 0f

    private var baseColor = Color.parseColor("#00D9FF")
    private val paintGlow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintCore = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintRing = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintFacet = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintHighlight = Paint(Paint.ANTI_ALIAS_FLAG)

    private var animator1: ValueAnimator? = null
    private var animator2: ValueAnimator? = null
    private var animator3: ValueAnimator? = null
    private var animator4: ValueAnimator? = null

    init {
        updateColors()
        startAnimations()
    }

    private fun updateColors() {
        baseColor = when (state) {
            State.LISTENING -> Color.parseColor("#00FFAA")
            State.THINKING -> Color.parseColor("#FFD700")
            State.SPEAKING -> Color.parseColor("#FF6B6B")
            State.IDLE -> Color.parseColor("#00D9FF")
        }
    }

    private fun startAnimations() {
        // Wobble animation 1
        animator1 = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = when (state) {
                State.SPEAKING -> 600
                State.LISTENING -> 1200
                else -> 3000
            }
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                wobblePhase1 = it.animatedValue as Float
                invalidate()
            }
            start()
        }

        // Wobble animation 2
        animator2 = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = when (state) {
                State.SPEAKING -> 800
                State.LISTENING -> 1800
                else -> 4500
            }
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                wobblePhase2 = it.animatedValue as Float
            }
            start()
        }

        // Rotation
        animator3 = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 20000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                rotationPhase = it.animatedValue as Float
            }
            start()
        }

        // Pulse
        animator4 = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = when (state) {
                State.SPEAKING -> 200
                State.LISTENING -> 600
                else -> 2000
            }
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener {
                pulsePhase = it.animatedValue as Float
            }
            start()
        }
    }

    fun stopAnimations() {
        animator1?.cancel()
        animator2?.cancel()
        animator3?.cancel()
        animator4?.cancel()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimations()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val baseRadius = minOf(width, height) / 2f * 0.42f
        val coreRadius = baseRadius * 0.35f

        // 1. Outer glow
        paintGlow.shader = RadialGradient(
            centerX, centerY, baseRadius * 1.8f,
            intArrayOf(
                adjustAlpha(baseColor, 0.15f),
                adjustAlpha(baseColor, 0.05f),
                Color.TRANSPARENT
            ),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(centerX, centerY, baseRadius * 1.8f, paintGlow)

        // 2. Main sphere with wobble
        val wobbleIntensity = when (state) {
            State.SPEAKING -> 0.15f
            State.LISTENING -> 0.08f
            else -> 0.025f
        }
        drawWobblySphere(canvas, centerX, centerY, baseRadius, wobbleIntensity)

        // 3. Internal facets
        drawFacets(canvas, centerX, centerY, baseRadius)

        // 4. Inner energy glow
        val glowAlpha = 0.3f + pulsePhase * 0.3f
        paintGlow.shader = RadialGradient(
            centerX, centerY, baseRadius * 0.6f,
            intArrayOf(
                adjustAlpha(baseColor, glowAlpha),
                adjustAlpha(baseColor, glowAlpha * 0.5f),
                Color.TRANSPARENT
            ),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(centerX, centerY, baseRadius * 0.55f, paintGlow)

        // 5. Bright core
        val coreScale = 1f + pulsePhase * 0.1f
        paintCore.shader = RadialGradient(
            centerX - coreRadius * 0.2f, centerY - coreRadius * 0.2f,
            coreRadius * 1.2f * coreScale,
            intArrayOf(
                Color.WHITE,
                adjustAlpha(Color.WHITE, 0.95f),
                adjustAlpha(baseColor, 0.9f)
            ),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(centerX, centerY, coreRadius * coreScale, paintCore)

        // 6. Core highlight
        paintHighlight.shader = RadialGradient(
            centerX - coreRadius * 0.3f, centerY - coreRadius * 0.3f,
            coreRadius * 0.5f,
            intArrayOf(
                adjustAlpha(Color.WHITE, 0.9f),
                adjustAlpha(Color.WHITE, 0.3f),
                Color.TRANSPARENT
            ),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(
            centerX - coreRadius * 0.25f,
            centerY - coreRadius * 0.25f,
            coreRadius * 0.4f,
            paintHighlight
        )

        // 7. Glass reflection
        paintHighlight.shader = RadialGradient(
            centerX - baseRadius * 0.4f, centerY - baseRadius * 0.4f,
            baseRadius * 0.4f,
            intArrayOf(
                adjustAlpha(Color.WHITE, 0.25f),
                adjustAlpha(Color.WHITE, 0.1f),
                Color.TRANSPARENT
            ),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(
            centerX - baseRadius * 0.4f,
            centerY - baseRadius * 0.4f,
            baseRadius * 0.35f,
            paintHighlight
        )
    }

    private fun drawWobblySphere(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        baseRadius: Float,
        wobbleIntensity: Float
    ) {
        val path = Path()
        val segments = 72

        for (i in 0..segments) {
            val angle = (i.toFloat() / segments) * 2 * Math.PI.toFloat()

            val w1 = sin(angle * 3 + wobblePhase1 * Math.PI.toFloat() / 180f) * wobbleIntensity
            val w2 = sin(angle * 5 + wobblePhase2 * Math.PI.toFloat() / 180f) * wobbleIntensity * 0.5f

            val wobbleRadius = baseRadius * (1f + w1 + w2)

            val x = centerX + cos(angle) * wobbleRadius
            val y = centerY + sin(angle) * wobbleRadius

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()

        // Sphere fill
        paintGlow.shader = RadialGradient(
            centerX - baseRadius * 0.3f, centerY - baseRadius * 0.3f,
            baseRadius * 1.5f,
            intArrayOf(
                adjustAlpha(baseColor, 0.1f),
                adjustAlpha(baseColor, 0.25f),
                adjustAlpha(baseColor, 0.4f),
                adjustAlpha(baseColor, 0.3f)
            ),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(path, paintGlow)

        // Rim highlight
        paintRing.shader = RadialGradient(
            centerX, centerY, baseRadius,
            intArrayOf(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                adjustAlpha(baseColor, 0.5f),
                adjustAlpha(baseColor, 0.8f)
            ),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(path, paintRing)
    }

    private fun drawFacets(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {
        val layers = listOf(
            Triple(0.85f, 8, 0f),
            Triple(0.7f, 6, 30f),
            Triple(0.55f, 5, 15f)
        )

        paintFacet.style = Paint.Style.STROKE
        paintFacet.strokeWidth = 1.5f

        for ((layerRadius, numFacets, angleOffset) in layers) {
            val layerR = radius * layerRadius

            for (i in 0 until numFacets) {
                val baseAngle = (i.toFloat() / numFacets) * 360f + angleOffset + rotationPhase
                val wobbleOffset = sin((wobblePhase1 + i * 40) * Math.PI.toFloat() / 180f) * 5f

                val angle1 = (baseAngle + wobbleOffset) * Math.PI.toFloat() / 180f
                val angle2 = (baseAngle + 360f / numFacets * 0.4f + wobbleOffset) * Math.PI.toFloat() / 180f
                val angle3 = (baseAngle + 360f / numFacets * 0.6f) * Math.PI.toFloat() / 180f

                val v1x = centerX + cos(angle1) * layerR * 0.3f
                val v1y = centerY + sin(angle1) * layerR * 0.3f
                val v2x = centerX + cos(angle2) * layerR
                val v2y = centerY + sin(angle2) * layerR
                val v3x = centerX + cos(angle3) * layerR * 0.8f
                val v3y = centerY + sin(angle3) * layerR * 0.8f

                val facetAlpha = 0.15f + sin((rotationPhase + i * 45) * Math.PI.toFloat() / 180f) * 0.1f

                val path = Path().apply {
                    moveTo(v1x, v1y)
                    lineTo(v2x, v2y)
                    lineTo(v3x, v3y)
                    close()
                }

                paintFacet.color = adjustAlpha(baseColor, facetAlpha)
                canvas.drawPath(path, paintFacet)
            }
        }
    }

    private fun adjustAlpha(color: Int, alpha: Float): Int {
        val a = (alpha.coerceIn(0f, 1f) * 255).toInt()
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
    }
}
