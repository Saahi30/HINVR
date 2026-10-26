package com.hinvr.quest

import androidx.annotation.StringRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

private val SealShape = RoundedCornerShape(percent = 50)
private data class Greeting(@StringRes val hindi: Int, @StringRes val english: Int)

private fun greetingForNow(): Greeting {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 4..10 -> Greeting(R.string.greet_morning_hi, R.string.greet_morning_en)
        in 11..15 -> Greeting(R.string.greet_day_hi, R.string.greet_day_en)
        in 16..19 -> Greeting(R.string.greet_evening_hi, R.string.greet_evening_en)
        else -> Greeting(R.string.greet_night_hi, R.string.greet_night_en)
    }
}

@Composable
fun SanctumOpening() {
    if (Ring.stage != Stage.Splash || !Ring.splashShown) return
    key(Ring.greeting, Ring.splashShown) { OpeningSequence() }
}

@Composable
private fun OpeningSequence() {
    val greeting = remember { greetingForNow() }
    var showHalo by remember { mutableStateOf(false) }
    var showSeal by remember { mutableStateOf(false) }
    var lightDiya by remember { mutableStateOf(false) }
    var showGreeting by remember { mutableStateOf(false) }
    var showMark by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }

    // Visual only. The input system owns the clock that ends the greeting (OpeningLengthMs),
    // so there is exactly one timer, and a trigger can skip it.
    LaunchedEffect(Unit) {
        showHalo = true
        delay(150)
        showSeal = true
        delay(250)
        lightDiya = true
        Ring.markDiyaLit()
        delay(300)
        showGreeting = true
        delay(300)
        showMark = true
        delay(OpeningLengthMs - 1000 - 600)
        leaving = true
    }

    val haloAlpha by animateFloatAsState(if (showHalo) 1f else 0f, tween(700), label = "halo")
    val sealAlpha by animateFloatAsState(
        targetValue = if (showSeal) 1f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "seal-fade",
    )
    val sealScale by animateFloatAsState(
        targetValue = if (showSeal) 1f else 0.86f,
        animationSpec = tween(700, easing = LinearOutSlowInEasing),
        label = "seal-scale",
    )
    val flame by animateFloatAsState(
        targetValue = if (lightDiya) 1f else 0.04f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "flame",
    )
    val greetAlpha by animateFloatAsState(if (showGreeting) 1f else 0f, tween(500), label = "greet")
    val greetRise by animateFloatAsState(
        targetValue = if (showGreeting) 0f else 18f,
        animationSpec = tween(600, easing = LinearOutSlowInEasing),
        label = "greet-rise",
    )
    val markAlpha by animateFloatAsState(if (showMark) 1f else 0f, tween(500), label = "mark")
    val exit by animateFloatAsState(
        targetValue = if (leaving) 0f else 1f,
        animationSpec = tween(550, easing = FastOutSlowInEasing),
        label = "exit",
    )
    SideEffect { Ring.publishFlame(flame) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = exit
                scaleX = 1f + 0.05f * (1f - exit)
                scaleY = 1f + 0.05f * (1f - exit)
            },
        contentAlignment = Alignment.Center,
    ) {
        MarigoldShower(Modifier.fillMaxSize().alpha(haloAlpha * 0.9f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Mandala(
                    glow = flame,
                    modifier = Modifier
                        .size(420.dp)
                        .alpha(haloAlpha),
                )
                HinvrSeal(
                    modifier = Modifier.graphicsLayer {
                        alpha = sealAlpha
                        scaleX = sealScale
                        scaleY = sealScale
                        transformOrigin = TransformOrigin.Center
                    },
                )
            }
            DiyaFlame(lit = flame, size = 88.dp, modifier = Modifier.padding(top = 0.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = greetAlpha
                    translationY = greetRise
                },
            ) {
                Text(
                    stringResource(greeting.hindi),
                    color = Cream,
                    fontFamily = FontFamily.Serif,
                    fontSize = 54.sp,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(greeting.english),
                    color = Cream.copy(alpha = 0.78f),
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Spacer(Modifier.height(22.dp))
            Text(
                text = stringResource(R.string.app_name),
                color = Gold,
                fontFamily = FontFamily.Serif,
                fontSize = 30.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(markAlpha),
            )
            Spacer(Modifier.height(8.dp))
            Box(Modifier.alpha(markAlpha)) { GoldRule() }
        }
    }
}

@Composable
private fun Mandala(glow: Float, modifier: Modifier = Modifier) {
    val spin by rememberInfiniteTransition(label = "mandala").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(60_000, easing = LinearEasing)),
        label = "spin",
    )
    val breathe by rememberInfiniteTransition(label = "mandala-breathe").animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe",
    )
    Canvas(modifier) {
        val c = center
        val r = size.minDimension / 2f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    DiyaAmber.copy(alpha = 0.34f * glow * breathe),
                    Saffron.copy(alpha = 0.12f * glow),
                    Color.Transparent,
                ),
                center = c,
                radius = r,
            ),
            radius = r,
            center = c,
        )
        rotate(spin, c) {
            val petals = 16
            for (i in 0 until petals) {
                val a = (i * 2.0 * PI / petals).toFloat()
                val tip = Offset(c.x + cos(a) * r * 0.9f, c.y + sin(a) * r * 0.9f)
                val base = Offset(c.x + cos(a) * r * 0.72f, c.y + sin(a) * r * 0.72f)
                rotate(Math.toDegrees(a.toDouble()).toFloat() + 90f, Offset((tip.x + base.x) / 2f, (tip.y + base.y) / 2f)) {
                    drawOval(
                        color = Gold.copy(alpha = 0.16f * glow),
                        topLeft = Offset((tip.x + base.x) / 2f - r * 0.035f, (tip.y + base.y) / 2f - r * 0.09f),
                        size = Size(r * 0.07f, r * 0.18f),
                    )
                    drawOval(
                        color = GoldLight.copy(alpha = 0.72f),
                        topLeft = Offset((tip.x + base.x) / 2f - r * 0.035f, (tip.y + base.y) / 2f - r * 0.09f),
                        size = Size(r * 0.07f, r * 0.18f),
                        style = Stroke(width = 2.2f),
                    )
                }
            }
            drawCircle(Gold.copy(alpha = 0.45f), radius = r * 0.70f, center = c, style = Stroke(width = 1.4f))
        }
        rotate(-spin * 1.6f, c) {
            val dots = 48
            for (i in 0 until dots) {
                val a = (i * 2.0 * PI / dots).toFloat()
                drawCircle(
                    color = GoldLight.copy(alpha = if (i % 4 == 0) 0.75f else 0.35f),
                    radius = if (i % 4 == 0) 3.2f else 1.8f,
                    center = Offset(c.x + cos(a) * r * 0.97f, c.y + sin(a) * r * 0.97f),
                )
            }
        }
        drawCircle(Gold.copy(alpha = 0.25f), radius = r * 0.62f, center = c, style = Stroke(width = 1f))
    }
}

private data class Petal(val x: Float, val speed: Float, val phase: Float, val size: Float, val tint: Color)

@Composable
private fun MarigoldShower(modifier: Modifier = Modifier) {
    val petals = remember {
        val random = java.util.Random(108)
        List(40) {
            Petal(
                x = random.nextFloat(),
                speed = 0.55f + random.nextFloat() * 0.6f,
                phase = random.nextFloat(),
                size = 10f + random.nextFloat() * 12f,
                tint = if (random.nextBoolean()) Marigold else Saffron,
            )
        }
    }
    val time by rememberInfiniteTransition(label = "petals").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "fall",
    )
    Canvas(modifier) {
        petals.forEach { p ->
            val t = (time * p.speed + p.phase) % 1f
            val y = -40f + t * (size.height + 80f)
            val sway = sin((t * 6f + p.phase * 10f).toDouble()).toFloat() * 26f
            val x = p.x * size.width + sway
            val fade = when {
                t < 0.12f -> t / 0.12f
                t > 0.85f -> (1f - t) / 0.15f
                else -> 1f
            }
            rotate(t * 540f + p.phase * 360f, Offset(x, y)) {
                drawOval(
                    color = p.tint.copy(alpha = 0.85f * fade),
                    topLeft = Offset(x - p.size / 2f, y - p.size * 0.35f),
                    size = Size(p.size, p.size * 0.7f),
                )
            }
        }
    }
}

/** The HINVR seal: a lamp over a lotus in a gold-edged oval, drawn here. No deity is drawn. */
@Composable
private fun HinvrSeal(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 176.dp, height = 256.dp)
            .clip(SealShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2C1C12),
                        Color(0xFF120C08),
                        Color(0xFF0A0706),
                    ),
                ),
            )
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GoldLight.copy(alpha = 0.75f),
                        Gold.copy(alpha = 0.15f),
                        Gold.copy(alpha = 0.5f),
                    ),
                ),
                shape = SealShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 34.dp),
        ) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            // Arch of the niche.
            drawArc(
                color = Gold.copy(alpha = 0.55f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(w * 0.12f, h * 0.06f),
                size = Size(w * 0.76f, w * 0.76f),
                style = Stroke(width = 2f),
            )
            // The lamp's flame.
            val flameTop = h * 0.22f
            val flame = Path().apply {
                moveTo(cx, flameTop)
                cubicTo(cx + w * 0.10f, flameTop + h * 0.08f, cx + w * 0.08f, flameTop + h * 0.16f, cx, flameTop + h * 0.19f)
                cubicTo(cx - w * 0.08f, flameTop + h * 0.16f, cx - w * 0.10f, flameTop + h * 0.08f, cx, flameTop)
                close()
            }
            drawPath(flame, Brush.verticalGradient(listOf(FlameCore, DiyaAmber), startY = flameTop, endY = flameTop + h * 0.19f))
            // The lamp's bowl.
            val bowlY = h * 0.43f
            drawArc(
                color = Gold,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(cx - w * 0.18f, bowlY - w * 0.09f),
                size = Size(w * 0.36f, w * 0.18f),
            )
            // Lotus: seven petals fanned under the lamp.
            val base = Offset(cx, h * 0.82f)
            for (i in -3..3) {
                rotate(i * 22f, base) {
                    val petal = Path().apply {
                        moveTo(base.x, base.y)
                        cubicTo(base.x + w * 0.11f, base.y - h * 0.12f, base.x + w * 0.05f, base.y - h * 0.26f, base.x, base.y - h * 0.30f)
                        cubicTo(base.x - w * 0.05f, base.y - h * 0.26f, base.x - w * 0.11f, base.y - h * 0.12f, base.x, base.y)
                        close()
                    }
                    drawPath(petal, Gold.copy(alpha = 0.55f + 0.13f * (3 - abs(i))))
                    drawPath(petal, GoldLight.copy(alpha = 0.6f), style = Stroke(width = 1.4f))
                }
            }
            drawLine(Gold.copy(alpha = 0.7f), Offset(w * 0.18f, h * 0.86f), Offset(w * 0.82f, h * 0.86f), strokeWidth = 2f)
        }
    }
}
