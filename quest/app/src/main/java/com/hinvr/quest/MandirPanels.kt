package com.hinvr.quest

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

private val Stone = Color(0xF2100B08)
private val Cream = Color(0xFFF3E6D0)
private val CreamMuted = Color(0xFFD9C7A8)
private val Gold = Color(0xFFC9A227)
private val Rounded = RoundedCornerShape(28.dp)

@Composable
fun SankalpPanel() {
    if (!Mandir.inside || !Mandir.sankalpOpen) return
    val live = Mandir.darshan
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(Rounded)
            .background(Stone)
            .border(1.dp, Gold.copy(alpha = 0.55f), Rounded)
            .padding(horizontal = 36.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("SANKALP", color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.6.sp)
        Text(
            Mandir.sankalpName.ifBlank { "Guest" },
            color = Cream,
            fontFamily = FontFamily.Serif,
            fontSize = 34.sp,
        )
        Text("This visit stays in HINVR. Nothing is sent to a temple.", color = CreamMuted, fontSize = 15.sp)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Intention", color = Gold, fontSize = 13.sp, letterSpacing = 1.2.sp, modifier = Modifier.weight(1f))
            Pill(Mandir.sankalpIntention, filled = false) { Mandir.cycleIntention(1) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Darshan window", color = Gold, fontSize = 13.sp, letterSpacing = 1.2.sp, modifier = Modifier.weight(1f))
            Pill(live?.title ?: "Sanctum murti", filled = false) { Mandir.cycleDarshan(1) }
        }
        Spacer(Modifier.weight(1f))
        Text(Mandir.Honesty, color = CreamMuted, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Pill("Begin") { Mandir.beginVisit() }
            Pill("Leave", filled = false, tint = CreamMuted) { Ring.back() }
        }
    }
}

@Composable
fun OfferingHintPanel() {
    if (!Mandir.inside || Mandir.sankalpOpen || Mandir.blessing) return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xE6100B08))
            .border(1.dp, Gold.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                Mandir.hint,
                color = Cream,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            if (Mandir.countsLine.isNotBlank()) {
                Text(Mandir.countsLine, color = Gold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun BlessingPanel() {
    if (!Mandir.inside || !Mandir.blessing) return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(Rounded)
            .background(Stone)
            .border(1.dp, Gold.copy(alpha = 0.6f), Rounded)
            .padding(horizontal = 36.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("ASHIRWAD", color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.6.sp)
        Text("The visit is complete.", color = Cream, fontFamily = FontFamily.Serif, fontSize = 32.sp)
        Text(Mandir.Honesty, color = CreamMuted, fontSize = 16.sp)
        Text(
            "A later release can book real seva. This offering was not performed at a temple.",
            color = CreamMuted,
            fontSize = 15.sp,
        )
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Pill("Leave sanctum") { Ring.back() }
        }
    }
}

@Composable
fun DarshanPanel() {
    if (!Mandir.inside) return
    val card = Mandir.darshan
    val url = card?.liveUrl.orEmpty()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF100B08)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("DARSHAN WINDOW", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.3.sp)
                Text(
                    card?.title ?: "HINVR sanctum",
                    color = Cream,
                    fontFamily = FontFamily.Serif,
                    fontSize = 20.sp,
                    maxLines = 1,
                )
            }
            if (Mandir.liveChoices.size > 1) {
                Pill("Next", filled = false) { Mandir.cycleDarshan(1) }
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF100B08)),
            contentAlignment = Alignment.Center,
        ) {
            if (url.isNotBlank()) {
                LivePlayer(url, spherical = false, Modifier.fillMaxSize())
            } else {
                card?.photo?.let { photo ->
                    Image(
                        painter = painterResource(photo),
                        contentDescription = card.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Text(
                    "The murti is in this sanctum.\nLive darshan is a public stream when one is chosen.",
                    color = Cream,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(20.dp)
                        .background(Color(0xAA100B08), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                )
            }
        }
        Text(
            Mandir.Honesty,
            color = CreamMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
fun FlamePanel() {
    if (!Mandir.inside || !Mandir.diyaLit) return
    DiyaFlame(lit = 1f, size = 90.dp)
}

@Composable
fun SmokePanel() {
    if (!Mandir.inside || !Mandir.agarbattiLit) return
    val drift by rememberInfiniteTransition(label = "smoke").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "drift",
    )
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        for (i in 0..4) {
            val t = ((drift + i / 5f) % 1f)
            val x = w * 0.5f + sin((t + i) * 6f) * w * 0.12f
            val y = h * (1f - t)
            drawCircle(
                color = Color(0x88C9B8A0).copy(alpha = (1f - t) * 0.35f),
                radius = w * (0.08f + t * 0.18f),
                center = Offset(x, y),
            )
        }
    }
}

@Composable
fun TilakOverlay() {
    if (!Mandir.inside || !Mandir.tilakOn) return
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height * 0.22f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xE6B3392B), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = size.minDimension * 0.18f,
                ),
                radius = size.minDimension * 0.18f,
                center = Offset(cx, cy),
            )
            drawCircle(Color(0xFFB3392B), radius = size.minDimension * 0.035f, center = Offset(cx, cy))
        }
    }
}

@Composable
fun MandirCredits() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("SANCTUM CREDITS", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.3.sp)
        Text(
            "Textures: Poly Haven (CC0). Ghanta: Subhashish Panigrahi, CC BY-SA 4.0. Shankh: Dbolton, CC BY 2.5. Courtyard: Gaurav Dhwaj Khadka, CC BY-SA 4.0. Room modeled by HINVR. Nothing is taken from a temple photograph.",
            color = CreamMuted,
            fontSize = 13.sp,
        )
    }
}
