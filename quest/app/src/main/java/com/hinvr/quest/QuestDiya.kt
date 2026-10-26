package com.hinvr.quest

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/** A flame's unsteadiness: three slow waves, so no two flames (by [phase]) move together. */
object Flicker {
    fun at(seconds: Float, phase: Float): Float =
        1f + 0.05f * sin(seconds * 9.7f + phase) + 0.035f * sin(seconds * 15.3f + phase * 2.1f) +
            0.025f * sin(seconds * 4.1f + phase * 3.7f)
}

/**
 * One flame's flicker, updated [perSecond] times a second: each update redraws its panel, so
 * soft glows ask for fewer. Read it in a draw block so only drawing repeats.
 */
@Composable
fun rememberFlicker(seed: Int, perSecond: Int = 30): State<Float> {
    val phase = remember(seed) { java.util.Random(seed * 7_919L + 13L).nextFloat() * 6.283f }
    val value = remember { mutableFloatStateOf(1f) }
    LaunchedEffect(seed, perSecond) {
        while (true) {
            value.floatValue = Flicker.at(System.nanoTime() / 1_000_000_000f, phase)
            delay(1_000L / perSecond)
        }
    }
    return value
}

@Composable
fun DiyaFlame(
    lit: Float,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
) {
    val seed = remember { kotlin.random.Random.nextInt(1, 100_000) }
    val flicker by rememberFlicker(seed)
    val glowPulse by rememberInfiniteTransition(label = "diya-glow").animateFloat(
        initialValue = 0.35f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow",
    )

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val litClamped = lit.coerceIn(0f, 1f)
        val flameH = h * 0.42f * litClamped * flicker
        val flameW = w * 0.16f * (0.85f + 0.15f * sin(flicker * 6f))

        if (litClamped > 0.02f) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        DiyaAmber.copy(alpha = glowPulse * litClamped),
                        Color.Transparent,
                    ),
                    center = Offset(cx, h * 0.38f),
                    radius = w * 0.55f,
                ),
                radius = w * 0.55f,
                center = Offset(cx, h * 0.42f),
            )

            val flame = Path().apply {
                moveTo(cx, h * 0.48f - flameH)
                cubicTo(
                    cx + flameW, h * 0.48f - flameH * 0.55f,
                    cx + flameW * 1.05f, h * 0.48f - flameH * 0.12f,
                    cx, h * 0.50f,
                )
                cubicTo(
                    cx - flameW * 1.05f, h * 0.48f - flameH * 0.12f,
                    cx - flameW, h * 0.48f - flameH * 0.55f,
                    cx, h * 0.48f - flameH,
                )
                close()
            }
            drawPath(
                path = flame,
                brush = Brush.verticalGradient(
                    colors = listOf(FlameCore, DiyaAmber, Ember.copy(alpha = 0.85f)),
                    startY = h * 0.48f - flameH,
                    endY = h * 0.50f,
                ),
            )
            val inner = Path().apply {
                val innerH = flameH * 0.55f
                val innerW = flameW * 0.42f
                moveTo(cx, h * 0.49f - innerH)
                cubicTo(
                    cx + innerW, h * 0.49f - innerH * 0.5f,
                    cx + innerW, h * 0.50f - innerH * 0.1f,
                    cx, h * 0.50f,
                )
                cubicTo(
                    cx - innerW, h * 0.50f - innerH * 0.1f,
                    cx - innerW, h * 0.49f - innerH * 0.5f,
                    cx, h * 0.49f - innerH,
                )
                close()
            }
            drawPath(inner, FlameCore.copy(alpha = 0.9f * litClamped), style = Fill)
        }

        val bowlTop = h * 0.52f
        val bowl = Path().apply {
            moveTo(w * 0.18f, bowlTop)
            quadraticTo(cx, bowlTop - h * 0.03f, w * 0.82f, bowlTop)
            quadraticTo(w * 0.78f, h * 0.78f, cx, h * 0.82f)
            quadraticTo(w * 0.22f, h * 0.78f, w * 0.18f, bowlTop)
            close()
        }
        drawPath(
            bowl,
            Brush.verticalGradient(
                colors = listOf(Gold.copy(alpha = 0.95f), Color(0xFF6F5814)),
                startY = bowlTop,
                endY = h * 0.82f,
            ),
        )
        drawOval(
            color = Color(0xFF3A2A10),
            topLeft = Offset(w * 0.24f, bowlTop - h * 0.025f),
            size = Size(w * 0.52f, h * 0.06f),
        )
        if (litClamped > 0.1f) {
            drawCircle(
                color = Color(0xFFE8D9A0),
                radius = w * 0.012f,
                center = Offset(cx, bowlTop + h * 0.01f),
            )
        }
    }
}

/**
 * Just the flame on a real wick, filling its panel: the base sits at [WickFlameBase] of the
 * height, the flame about 3.5 cm tall on its 9 cm panel, the glow no wider than the flame's warmth.
 */
@Composable
fun WickFlame(modifier: Modifier = Modifier) {
    val seed = remember { kotlin.random.Random.nextInt(1, 100_000) }
    val flicker by rememberFlicker(seed)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val base = h * WickFlameBase
        val flameH = h * 0.40f * flicker
        val flameW = w * 0.17f * (0.9f + 0.1f * sin(flicker * 6f))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(DiyaAmber.copy(alpha = 0.32f * flicker), Color.Transparent),
                center = Offset(cx, base - flameH * 0.45f),
                radius = w * 0.5f,
            ),
            radius = w * 0.5f,
            center = Offset(cx, base - flameH * 0.45f),
        )
        val flame = Path().apply {
            moveTo(cx, base - flameH)
            cubicTo(cx + flameW, base - flameH * 0.55f, cx + flameW * 1.05f, base - flameH * 0.1f, cx, base)
            cubicTo(cx - flameW * 1.05f, base - flameH * 0.1f, cx - flameW, base - flameH * 0.55f, cx, base - flameH)
            close()
        }
        drawPath(
            flame,
            Brush.verticalGradient(
                colors = listOf(FlameCore, DiyaAmber, Ember.copy(alpha = 0.85f)),
                startY = base - flameH,
                endY = base,
            ),
        )
        val innerH = flameH * 0.5f
        val innerW = flameW * 0.42f
        val inner = Path().apply {
            moveTo(cx, base - innerH)
            cubicTo(cx + innerW, base - innerH * 0.5f, cx + innerW, base - innerH * 0.1f, cx, base)
            cubicTo(cx - innerW, base - innerH * 0.1f, cx - innerW, base - innerH * 0.5f, cx, base - innerH)
            close()
        }
        drawPath(inner, FlameCore.copy(alpha = 0.9f), style = Fill)
    }
}

/** Where a [WickFlame]'s base sits, as a fraction of its panel's height from the top. */
const val WickFlameBase = 0.88f
