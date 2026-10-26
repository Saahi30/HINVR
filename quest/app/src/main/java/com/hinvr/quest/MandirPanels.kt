package com.hinvr.quest

import android.os.SystemClock
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

@Composable
fun SankalpPanel() {
    if (!Mandir.sankalpOpen) return
    val live = Mandir.darshan
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(PanelShape)
            .background(InkGlass)
            .border(1.dp, Gold.copy(alpha = 0.55f), PanelShape)
            .padding(horizontal = 36.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(R.string.sankalp_heading), color = Gold, fontSize = 15.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.6.sp)
        Text(
            Mandir.sankalpName.ifBlank { stringResource(R.string.sankalp_guest) },
            color = Cream,
            fontFamily = FontFamily.Serif,
            fontSize = 34.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(stringResource(R.string.sankalp_private), color = CreamMuted, fontSize = 17.sp)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.sankalp_intention), color = Gold, fontSize = 16.sp, letterSpacing = 1.2.sp, modifier = Modifier.weight(1f))
            Pill(stringResource(Mandir.sankalpIntention.label), filled = false) { Mandir.cycleIntention(1) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.sankalp_window), color = Gold, fontSize = 16.sp, letterSpacing = 1.2.sp, modifier = Modifier.weight(1f))
            Pill(live?.title ?: stringResource(R.string.darshan_murti), filled = false) { Mandir.cycleDarshan(1) }
        }
        Spacer(Modifier.weight(1f))
        Text(stringResource(R.string.honesty), color = CreamMuted, fontSize = 15.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Pill(stringResource(R.string.action_begin)) { Mandir.beginVisit() }
            Pill(stringResource(R.string.action_leave), filled = false, tint = CreamMuted) { Ring.back() }
        }
    }
}

/** The line over the offering stand: what to do next, today's diyas, and the darshan window's source. */
@Composable
fun GuidePanel() {
    if (!Mandir.inside || Mandir.phase != MandirPhase.Visit) return
    val card = Mandir.darshan
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(36.dp))
            .background(InkVeil)
            .border(1.dp, Gold.copy(alpha = 0.4f), RoundedCornerShape(36.dp))
            .padding(horizontal = 28.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(Mandir.hint, Mandir.aartiCircles),
            color = Cream,
            fontSize = 22.sp,
            lineHeight = 27.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            val note = if (Mandir.hintHonest) stringResource(R.string.honesty) else ""
            Text(note, color = CreamMuted, fontSize = 15.sp, maxLines = 1, modifier = Modifier.weight(1f))
            if (Mandir.diyasToday > 0) {
                Text(
                    pluralStringResource(R.plurals.diyas_today, Mandir.diyasToday, Mandir.diyasToday),
                    color = Gold,
                    fontSize = 16.sp,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (card != null && card.hasStream) StreamBadge(card.live, size = 14.sp)
            Text(
                if (card != null && card.hasStream) {
                    stringResource(R.string.darshan_caption, card.title)
                } else {
                    stringResource(R.string.darshan_murti)
                },
                color = CreamMuted,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (Mandir.liveChoices.size > 1) SmallPill(stringResource(R.string.action_next_stream)) { Mandir.cycleDarshan(1) }
            SmallPill(stringResource(R.string.sound_label, stringResource(AudioMix.level.label))) { AudioMix.cycle() }
        }
    }
}

@Composable
private fun SmallPill(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = Gold,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(PillShape)
            .border(1.2.dp, Gold, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
    )
}

@Composable
fun BlessingPanel() {
    if (!Mandir.blessing) return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(PanelShape)
            .background(InkGlass)
            .border(1.dp, Gold.copy(alpha = 0.6f), PanelShape)
            .padding(horizontal = 36.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.blessing_heading), color = Gold, fontSize = 15.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.6.sp)
        Text(stringResource(R.string.blessing_title), color = Cream, fontFamily = FontFamily.Serif, fontSize = 32.sp)
        Text(stringResource(R.string.honesty), color = CreamMuted, fontSize = 17.sp)
        Text(stringResource(R.string.blessing_body), color = CreamMuted, fontSize = 16.sp)
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Pill(stringResource(R.string.action_leave_sanctum)) { Ring.back() }
        }
    }
}

/**
 * The window in the garbha. It is ~5 m away, so it carries only the picture and big type;
 * the source and controls are on the guide over the stand.
 */
@Composable
fun DarshanPanel() {
    if (!Mandir.inside) return
    val card = Mandir.darshan
    val url = card?.liveUrl.orEmpty()
    val active = Mandir.phase != MandirPhase.Sankalp && MandirScene.ready && !AudioMix.held
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(18.dp))
            .background(Ink),
        contentAlignment = Alignment.Center,
    ) {
        if (url.isNotBlank()) {
            YoutubePlayer(url, active, Modifier.fillMaxSize(), textScale = 1.8f)
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
                stringResource(R.string.darshan_no_stream),
                color = Cream,
                fontFamily = FontFamily.Serif,
                fontSize = 44.sp,
                lineHeight = 52.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(24.dp)
                    .background(InkShade, RoundedCornerShape(16.dp))
                    .padding(20.dp),
            )
        }
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

/** A soft warmth at the brow that comes and goes within two seconds. */
@Composable
fun TilakOverlay() {
    val at = Mandir.tilakAtMs
    if (!Mandir.inside || at == 0L) return
    var glow by remember(at) { mutableFloatStateOf(0f) }
    LaunchedEffect(at) {
        while (true) {
            val age = withFrameMillis { SystemClock.elapsedRealtime() - at }
            val t = age / MandirLayout.TilakGlowMs.toFloat()
            if (t >= 1f) {
                glow = 0f
                break
            }
            // Up quickly, then a long ease out.
            glow = if (t < 0.2f) t / 0.2f else 1f - (t - 0.2f) / 0.8f
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val c = Offset(size.width / 2f, size.height * 0.3f)
        val r = size.minDimension * 0.5f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFB3392B).copy(alpha = 0.32f * glow), Color.Transparent),
                center = c,
                radius = r,
            ),
            radius = r,
            center = c,
        )
    }
}

/** Black over the eyes while the member is moved, and while the sanctum is still loading. */
@Composable
fun FadePanel() {
    val level = ViewRig.fade
    if (level <= 0.003f) return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = level)),
        contentAlignment = Alignment.Center,
    ) {
        if (ViewRig.holdBlack && Mandir.inside && !MandirScene.ready) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                DiyaFlame(lit = 1f, size = 60.dp)
                Text(stringResource(R.string.mandir_loading), color = CreamMuted, fontSize = 15.sp)
            }
        }
    }
}

/** Over the state and mandir rings: whether the live state is fresh from the server. */
@Composable
fun CatalogStatusPanel() {
    val text = when {
        Ring.fromServer -> return
        QuestCatalog.status == CatalogStatus.Loading -> R.string.catalog_loading
        QuestCatalog.status == CatalogStatus.Failed || !QuestAccount.configured -> R.string.catalog_offline
        else -> return
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            stringResource(text),
            color = CreamMuted,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .background(InkVeil, PillShape)
                .border(1.dp, Gold.copy(alpha = 0.32f), PillShape)
                .padding(horizontal = 18.dp, vertical = 8.dp),
        )
    }
}

@Composable
fun GoldRule() {
    Box(
        Modifier
            .width(72.dp)
            .height(1.5.dp)
            .background(Brush.horizontalGradient(listOf(Color.Transparent, Gold, Color.Transparent))),
    )
}
