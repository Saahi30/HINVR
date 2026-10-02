package com.hinvr.quest

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.sin

private val Marigold = Color(0xFFF4C430)
private val Saffron = Color(0xFFF0B429)
private val Gold = Color(0xFFE8C547)
private val Cream = Color(0xFFF3E6D0)
private val Pink = Color(0xFFE88AA8)
private val Lotus = Color(0xFFF0A0B8)
private val Rose = Color(0xFFE07A92)

private enum class BloomKind { Petal, Marigold, Lotus }

private data class Bloom(
    val x: Float,
    val speed: Float,
    val phase: Float,
    val scale: Float,
    val tint: Color,
    val kind: BloomKind,
    val spin: Float,
)

private data class MoteSpec(
    val x: Float,
    val y: Float,
    val speed: Float,
    val phase: Float,
    val scale: Float,
)

@Composable
fun SanctumAir(slot: Int) {
    val blessing = Ring.stage == Stage.Pair && QuestAccount.step == PairStep.Welcome
    val claiming = Ring.stage == Stage.Pair && QuestAccount.step == PairStep.Claiming
    val swell by animateFloatAsState(
        targetValue = when {
            blessing -> 1f
            claiming -> 0.78f
            Ring.stage == Stage.Splash -> 0.72f
            else -> 0.64f
        },
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "air-swell",
    )
    val blooms = remember(slot) { bloomField(slot) }
    val motes = remember(slot) { moteField(slot) }
    val time by rememberInfiniteTransition(label = "sanctum-air-$slot").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16_000, easing = LinearEasing)),
        label = "air-time",
    )
    Canvas(Modifier.fillMaxSize()) {
        val unit = size.minDimension
        motes.forEach { mote ->
            val t = (time * mote.speed + mote.phase) % 1f
            val y = size.height * ((mote.y + t * 0.35f) % 1.15f)
            val sway = sin((t * 5f + mote.phase * 8f) * 2.0 * PI).toFloat() * unit * 0.02f
            drawCircle(
                color = Gold.copy(alpha = 0.22f * swell * edgeFade(t)),
                radius = unit * mote.scale,
                center = Offset(mote.x * size.width + sway, y),
            )
        }
        blooms.forEach { bloom ->
            val t = (time * bloom.speed * (0.85f + 0.25f * swell) + bloom.phase) % 1f
            val y = -unit * 0.08f + t * (size.height + unit * 0.16f)
            val sway = sin((t * 5.6f + bloom.phase * 9f) * 2.0 * PI).toFloat() * unit * 0.035f
            val at = Offset(bloom.x * size.width + sway, y)
            val fade = edgeFade(t) * swell
            val spin = t * bloom.spin + bloom.phase * 220f
            val s = unit * bloom.scale
            when (bloom.kind) {
                BloomKind.Petal -> drawPetal(at, s, s * 0.58f, spin, bloom.tint.copy(alpha = 0.82f * fade))
                BloomKind.Marigold -> drawMarigold(at, s, spin, bloom.tint.copy(alpha = 0.88f * fade))
                BloomKind.Lotus -> drawLotus(at, s, spin, bloom.tint, fade)
            }
        }
    }
}

private fun bloomField(slot: Int): List<Bloom> {
    val random = java.util.Random(108L * (slot + 1))
    val yellow = listOf(Marigold, Saffron, Gold, Marigold)
    val pink = listOf(Lotus, Pink, Rose, Lotus)
    return List(62) {
        val wantPink = random.nextBoolean()
        val kind = when {
            wantPink && random.nextFloat() < 0.55f -> BloomKind.Lotus
            !wantPink && random.nextFloat() < 0.55f -> BloomKind.Marigold
            else -> BloomKind.Petal
        }
        val scale = when (kind) {
            BloomKind.Petal -> 0.012f + random.nextFloat() * 0.012f
            BloomKind.Marigold -> 0.018f + random.nextFloat() * 0.014f
            BloomKind.Lotus -> 0.020f + random.nextFloat() * 0.012f
        }
        Bloom(
            x = random.nextFloat(),
            speed = 0.42f + random.nextFloat() * 0.7f,
            phase = random.nextFloat(),
            scale = scale,
            tint = if (wantPink) pink[random.nextInt(pink.size)] else yellow[random.nextInt(yellow.size)],
            kind = kind,
            spin = 160f + random.nextFloat() * 280f,
        )
    }
}

private fun moteField(slot: Int): List<MoteSpec> {
    val random = java.util.Random(63L * (slot + 4))
    return List(16) {
        MoteSpec(
            x = random.nextFloat(),
            y = random.nextFloat(),
            speed = 0.22f + random.nextFloat() * 0.35f,
            phase = random.nextFloat(),
            scale = 0.0025f + random.nextFloat() * 0.003f,
        )
    }
}

private fun edgeFade(t: Float): Float = when {
    t < 0.08f -> t / 0.08f
    t > 0.9f -> (1f - t) / 0.1f
    else -> 1f
}.coerceIn(0f, 1f)

private fun DrawScope.drawPetal(center: Offset, width: Float, height: Float, rotation: Float, color: Color) {
    rotate(rotation, center) {
        drawOval(color, Offset(center.x - width / 2f, center.y - height / 2f), Size(width, height))
        drawOval(
            Cream.copy(alpha = color.alpha * 0.22f),
            Offset(center.x - width * 0.16f, center.y - height * 0.28f),
            Size(width * 0.32f, height * 0.36f),
        )
    }
}

private fun DrawScope.drawMarigold(center: Offset, size: Float, rotation: Float, color: Color) {
    rotate(rotation, center) {
        for (i in 0 until 12) {
            rotate(i * 30f, center) {
                drawOval(
                    color,
                    Offset(center.x - size * 0.16f, center.y - size * 0.72f),
                    Size(size * 0.32f, size * 0.58f),
                )
            }
        }
        drawCircle(Gold.copy(alpha = color.alpha), radius = size * 0.18f, center = center)
    }
}

private fun DrawScope.drawLotus(center: Offset, size: Float, rotation: Float, tint: Color, fade: Float) {
    rotate(rotation, center) {
        for (i in 0 until 6) {
            rotate(i * 60f, center) {
                drawOval(
                    tint.copy(alpha = 0.84f * fade),
                    Offset(center.x - size * 0.16f, center.y - size * 0.78f),
                    Size(size * 0.32f, size * 0.7f),
                )
            }
        }
        drawCircle(Gold.copy(alpha = 0.7f * fade), radius = size * 0.14f, center = center)
    }
}
