package com.hinvr.app.ui.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hinvr.app.ui.theme.AntiqueGold
import com.hinvr.app.ui.theme.DiyaAmber
import com.hinvr.app.ui.theme.FlameCore
import com.hinvr.app.ui.theme.Saffron
import kotlin.math.sin

@Composable
fun DiyaFlame(
    lit: Float,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
) {
    val flicker by rememberInfiniteTransition(label = "diya-flicker").animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "flicker",
    )
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
        val flameW = w * 0.16f * (0.85f + 0.15f * sin(flicker * 6f).toFloat())

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
                    colors = listOf(FlameCore, DiyaAmber, Saffron.copy(alpha = 0.85f)),
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
                colors = listOf(AntiqueGold.copy(alpha = 0.95f), Color(0xFF6F5814)),
                startY = bowlTop,
                endY = h * 0.82f,
            ),
        )
        drawOval(
            color = Color(0xFF3A2A10),
            topLeft = Offset(w * 0.24f, bowlTop - h * 0.025f),
            size = androidx.compose.ui.geometry.Size(w * 0.52f, h * 0.06f),
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
