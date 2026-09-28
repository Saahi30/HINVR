package com.hinvr.quest

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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

private val Gold = Color(0xFFC9A227)
private val GoldLight = Color(0xFFF1D27A)
private val Cream = Color(0xFFF3E6D0)
private val Saffron = Color(0xFFE07A2E)
private val Marigold = Color(0xFFF2A516)
private val DiyaAmber = Color(0xFFE8A317)
private val SealShape = RoundedCornerShape(percent = 50)
private val GoldKnockout = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0.30f, 0.52f, 0.18f, 0f, -0.04f,
        ),
    ),
)

const val OpeningLengthMs = 5200L

private data class Greeting(val hindi: String, val english: String)

private fun greetingForNow(): Greeting {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 4..10 -> Greeting("सुप्रभात", "Good morning. Your darshan awaits.")
        in 11..15 -> Greeting("नमस्ते", "Welcome. Your darshan awaits.")
        in 16..19 -> Greeting("शुभ संध्या", "Good evening. The aarti lamps are lit.")
        else -> Greeting("शुभ रात्रि", "The sanctum is quiet. Your darshan awaits.")
    }
}

@Composable
fun SanctumOpening() {
    key(Ring.greeting) { OpeningSequence() }
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

    LaunchedEffect(Unit) {
        showHalo = true
        delay(250)
        showSeal = true
        delay(550)
        lightDiya = true
        Ring.markDiyaLit()
        delay(650)
        showGreeting = true
        delay(650)
        showMark = true
        delay(OpeningLengthMs - 2100 - 600)
        leaving = true
    }

    val haloAlpha by animateFloatAsState(if (showHalo) 1f else 0f, tween(1400), label = "halo")
    val sealAlpha by animateFloatAsState(
        targetValue = if (showSeal) 1f else 0f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "seal-fade",
    )
    val sealScale by animateFloatAsState(
        targetValue = if (showSeal) 1f else 0.86f,
        animationSpec = tween(1100, easing = LinearOutSlowInEasing),
        label = "seal-scale",
    )
    val flame by animateFloatAsState(
        targetValue = if (lightDiya) 1f else 0.04f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "flame",
    )
    val greetAlpha by animateFloatAsState(if (showGreeting) 1f else 0f, tween(900), label = "greet")
    val greetRise by animateFloatAsState(
        targetValue = if (showGreeting) 0f else 18f,
        animationSpec = tween(1000, easing = LinearOutSlowInEasing),
        label = "greet-rise",
    )
    val markAlpha by animateFloatAsState(if (showMark) 1f else 0f, tween(800), label = "mark")
    val exit by animateFloatAsState(
        targetValue = if (leaving) 0f else 1f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
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
            }
            .clickable { Ring.openMenu() },
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
                GaneshaSeal(
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
                    greeting.hindi,
                    color = Cream,
                    fontFamily = FontFamily.Serif,
                    fontSize = 54.sp,
                    textAlign = TextAlign.Center,
                )
                Text(
                    greeting.english,
                    color = Cream.copy(alpha = 0.78f),
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Spacer(Modifier.height(22.dp))
            Text(
                text = "HINVR",
                color = Gold,
                fontFamily = FontFamily.Serif,
                fontSize = 30.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(markAlpha),
            )
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .width(72.dp)
                    .height(1.5.dp)
                    .alpha(markAlpha)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Gold, Color.Transparent),
                        ),
                    ),
            )
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
        List(26) {
            Petal(
                x = random.nextFloat(),
                speed = 0.55f + random.nextFloat() * 0.6f,
                phase = random.nextFloat(),
                size = 7f + random.nextFloat() * 9f,
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

@Composable
private fun GaneshaSeal(modifier: Modifier = Modifier) {
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
        Image(
            painter = painterResource(R.drawable.ganesha_stencil),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 28.dp),
            contentScale = ContentScale.Fit,
            colorFilter = GoldKnockout,
        )
    }
}

@Composable
fun HallBackdrop() {
    Image(
        painter = painterResource(R.drawable.sanctum_hall),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
    )
}
