package com.hinvr.quest

import android.os.SystemClock
import androidx.annotation.StringRes
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
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
import kotlinx.coroutines.delay
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
        Festivals.active?.let { festival ->
            Text(
                stringResource(R.string.festival_today, festival.title(QuestLocale.hindi), stringResource(festival.kind.step)),
                color = Saffron,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        LiveAartiBanner()
        Spacer(Modifier.height(2.dp))
        SankalpChoice(stringResource(R.string.sankalp_intention), stringResource(Mandir.sankalpIntention.label)) { Mandir.cycleIntention(1) }
        SankalpChoice(
            stringResource(R.string.sankalp_for),
            Mandir.familyMember?.name ?: stringResource(Mandir.dedication.label),
        ) { Mandir.cycleDedication() }
        SankalpChoice(stringResource(R.string.sankalp_window), live?.title ?: stringResource(R.string.darshan_murti)) { Mandir.cycleDarshan(1) }
        Spacer(Modifier.weight(1f))
        Text(stringResource(R.string.honesty), color = CreamMuted, fontSize = 15.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Pill(stringResource(R.string.action_begin)) { Mandir.beginVisit() }
            Pill(stringResource(R.string.action_leave), filled = false, tint = CreamMuted) { Ring.back() }
            Spacer(Modifier.weight(1f))
            SmallPill(stringResource(if (Tutorial.replay) R.string.tutorial_again_on else R.string.tutorial_again)) { Tutorial.toggleReplay() }
            SmallPill(stringResource(if (Comfort.seated) R.string.comfort_seated else R.string.comfort_standing)) { Comfort.toggleSeated() }
            SmallPill(stringResource(R.string.language_switch)) { QuestLocale.toggle() }
        }
    }
}

/** A label and the pill that cycles its value. */
@Composable
private fun SankalpChoice(label: String, value: String, onClick: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Gold, fontSize = 16.sp, letterSpacing = 1.2.sp, modifier = Modifier.weight(1f))
        Pill(value, filled = false, onClick = onClick)
    }
}

/**
 * "Aarti is live now at …", when a temple with an official stream is live inside one of its
 * aarti times, with Watch to put it in the darshan window. Checked every half minute.
 */
@Composable
private fun LiveAartiBanner() {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            now = System.currentTimeMillis()
        }
    }
    val card = AartiSync.liveNow(Ring.mandirs, now) ?: return
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        StreamBadge(card.live, size = 14.sp)
        Text(
            stringResource(R.string.aarti_live_now, card.title),
            color = Cream,
            fontSize = 17.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (Mandir.darshan?.mandirId != card.mandirId) SmallPill(stringResource(R.string.action_watch)) { Mandir.showDarshan(card) }
    }
}

/**
 * The guide behind the offering stand: the next step (or what just happened), every step
 * with the ones done filled in, today's diyas, and the darshan window's source.
 */
@Composable
fun GuidePanel() {
    if (!Mandir.inside || Mandir.phase != MandirPhase.Visit) return
    val card = Mandir.darshan
    val at = Mandir.eventAtMs
    var fresh by remember(at) { mutableStateOf(at != 0L && SystemClock.elapsedRealtime() - at < Mandir.EventMs) }
    LaunchedEffect(at) {
        if (!fresh) return@LaunchedEffect
        delay(Mandir.EventMs - (SystemClock.elapsedRealtime() - at))
        fresh = false
    }
    val festival = Festivals.active
    val steps = OfferingRules.steps(festival != null)
    val next = OfferingRules.nextStep(Mandir.done, festival != null, Mandir.festivalDone)
        ?: OfferingRules.Step.Offering(OfferingKind.Aarti)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(36.dp))
            .background(InkVeil)
            .border(1.dp, Gold.copy(alpha = 0.4f), RoundedCornerShape(36.dp))
            .padding(horizontal = 28.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val tutorialLine = when (Tutorial.step) {
            TutorialStep.Take -> R.string.tutorial_take
            TutorialStep.Light -> R.string.tutorial_light
            TutorialStep.Done -> R.string.tutorial_done
            TutorialStep.Off -> 0
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                when {
                    // The first visit's walk-through speaks first; offerings still work throughout.
                    tutorialLine != 0 -> stringResource(tutorialLine)
                    fresh && Mandir.event != 0 -> stringResource(Mandir.event, Mandir.aartiCircles)
                    else -> stringResource(R.string.guide_next, stringResource(nextLine(next, festival)))
                },
                color = if (tutorialLine != 0) GoldLight else Cream,
                fontSize = 22.sp,
                lineHeight = 27.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (Tutorial.step == TutorialStep.Take || Tutorial.step == TutorialStep.Light) {
                SmallPill(stringResource(R.string.tutorial_skip)) { Tutorial.skip() }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            steps.forEach { step ->
                val done = when (step) {
                    is OfferingRules.Step.Offering -> step.kind in Mandir.done
                    OfferingRules.Step.Festival -> Mandir.festivalDone
                }
                StepChip(stringResource(stepLabel(step, festival)), done = done, next = step == next, Modifier.weight(1f))
            }
        }
        if (festival != null) {
            Text(
                stringResource(R.string.festival_today, festival.title(QuestLocale.hindi), stringResource(festival.kind.step)),
                color = Saffron,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        LiveAartiBanner()
        Row(verticalAlignment = Alignment.CenterVertically) {
            val note = if (fresh && Mandir.eventHonest) stringResource(R.string.honesty) else ""
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
            Text(
                stringResource(if (MandirScene.mark == 0) R.string.step_closer else R.string.step_back),
                color = CreamMuted,
                fontSize = 15.sp,
                maxLines = 1,
            )
            if (Mandir.liveChoices.size > 1) SmallPill(stringResource(R.string.action_next_stream)) { Mandir.cycleDarshan(1) }
            SmallPill(stringResource(if (Mandir.darshanClose) R.string.action_darshan_back else R.string.action_darshan_close)) {
                Mandir.darshanClose = !Mandir.darshanClose
            }
            SmallPill(stringResource(R.string.sound_label, stringResource(AudioMix.level.label))) { AudioMix.cycle() }
        }
    }
}

/** One step of the visit: filled once it is done, outlined in gold when it is next. */
@Composable
private fun StepChip(label: String, done: Boolean, next: Boolean, modifier: Modifier) {
    Text(
        label,
        color = if (done) Ink else if (next) Gold else CreamMuted,
        fontSize = 14.sp,
        fontWeight = if (done || next) FontWeight.SemiBold else FontWeight.Normal,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(PillShape)
            .background(if (done) Gold else Color.Transparent)
            .border(1.dp, if (done || next) Gold else Gold.copy(alpha = 0.3f), PillShape)
            .padding(vertical = 5.dp),
    )
}

@StringRes
private fun stepLabel(step: OfferingRules.Step, festival: Festival?): Int = when (step) {
    OfferingRules.Step.Festival -> festival?.kind?.step ?: R.string.step_festival
    is OfferingRules.Step.Offering -> when (step.kind) {
        OfferingKind.Diya -> R.string.step_diya
        OfferingKind.Agarbatti -> R.string.step_agarbatti
        OfferingKind.Pushp -> R.string.step_pushp
        OfferingKind.Prasad -> R.string.step_prasad
        OfferingKind.Ghanta -> R.string.step_ghanta
        OfferingKind.Tilak -> R.string.step_tilak
        OfferingKind.Daan -> R.string.step_daan
        OfferingKind.Aarti -> R.string.step_aarti
        OfferingKind.Japa -> R.string.step_japa
        OfferingKind.Abhishek -> R.string.step_abhishek
        OfferingKind.Deepmala -> R.string.step_deepmala
    }
}

@StringRes
private fun nextLine(step: OfferingRules.Step, festival: Festival?): Int = when (step) {
    OfferingRules.Step.Festival -> festival?.kind?.next ?: R.string.next_aarti
    is OfferingRules.Step.Offering -> when (step.kind) {
        OfferingKind.Diya -> R.string.next_diya
        OfferingKind.Agarbatti -> R.string.next_agarbatti
        OfferingKind.Pushp -> R.string.next_pushp
        OfferingKind.Prasad -> R.string.next_prasad
        OfferingKind.Ghanta -> R.string.next_ghanta
        OfferingKind.Tilak -> R.string.next_tilak
        OfferingKind.Daan -> R.string.next_daan
        OfferingKind.Aarti, OfferingKind.Japa -> R.string.next_aarti
        OfferingKind.Abhishek -> R.string.next_abhishek
        OfferingKind.Deepmala -> R.string.next_deepmala
    }
}

/** Over the prop a hand would take: its name and what to do with it. */
@Composable
fun PropLabel() {
    val name = Mandir.hover
    if (!Mandir.inside || name == null) return
    val (title, verb) = propCopy(name) ?: return
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            stringResource(R.string.prop_label, stringResource(title), stringResource(verb)),
            color = Cream,
            fontSize = 17.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .background(InkVeil, PillShape)
                .border(1.dp, Gold.copy(alpha = 0.5f), PillShape)
                .padding(horizontal = 16.dp, vertical = 7.dp),
        )
    }
}

/** A prop's name and its verb, which follows what has already happened to it. */
private fun propCopy(name: String): Pair<Int, Int>? = when (name) {
    "diya" -> R.string.prop_diya to if (Mandir.diyaLit) R.string.verb_diya_lit else R.string.verb_diya
    "agarbatti" -> R.string.prop_agarbatti to if (Mandir.agarbattiLit) R.string.verb_agarbatti_lit else R.string.verb_agarbatti
    "aarti_thali" -> R.string.prop_thali to R.string.verb_thali
    "marigold" -> R.string.prop_marigold to R.string.verb_flower
    "lotus" -> R.string.prop_lotus to R.string.verb_flower
    "coconut", "laddoo" -> (if (name == "coconut") R.string.prop_coconut else R.string.prop_laddoo) to
        if (OfferingKind.Prasad in Mandir.done) R.string.verb_prasad else R.string.verb_naivedya
    "kumkum" -> R.string.prop_kumkum to R.string.verb_kumkum
    "coin" -> R.string.prop_coin to R.string.verb_coin
    OfferingSystem.Ghanta -> R.string.prop_ghanta to R.string.verb_ghanta
    OfferingSystem.Mala -> R.string.prop_mala to if (Japa.active) R.string.verb_mala_counting else R.string.verb_mala
    "lota" -> R.string.prop_lota to R.string.verb_lota
    "modak" -> R.string.prop_modak to R.string.verb_naivedya
    "hibiscus" -> R.string.prop_hibiscus to R.string.verb_flower
    else -> null
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
        Text(
            Mandir.familyMember?.let { stringResource(R.string.blessing_for_member, it.name) } ?: stringResource(Mandir.dedication.blessing),
            color = GoldLight,
            fontSize = 18.sp,
        )
        if (Japa.rounds > 0) {
            Text(pluralStringResource(R.plurals.japa_rounds, Japa.rounds, Japa.rounds), color = CreamMuted, fontSize = 16.sp)
        }
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

/** Over the side table: the mantra, the bead under the thumb, and how to count. Text only. */
@Composable
fun JapaPanel() {
    if (!Mandir.inside || Mandir.phase != MandirPhase.Visit) return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(26.dp))
            .background(InkVeil)
            .border(1.dp, Gold.copy(alpha = 0.4f), RoundedCornerShape(26.dp))
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stringResource(R.string.japa_heading), color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.4.sp)
        Text(stringResource(Japa.mantra.label), color = Cream, fontFamily = FontFamily.Serif, fontSize = 24.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.japa_count, Japa.beads, Japa.total), color = GoldLight, fontSize = 18.sp)
            if (Japa.rounds > 0) {
                Text(pluralStringResource(R.plurals.japa_rounds, Japa.rounds, Japa.rounds), color = CreamMuted, fontSize = 15.sp, modifier = Modifier.weight(1f))
            } else {
                Spacer(Modifier.weight(1f))
            }
            SmallPill(stringResource(R.string.action_mantra)) { Japa.cycleMantra() }
        }
        Text(
            stringResource(if (Japa.active) R.string.japa_hint_counting else R.string.japa_hint_take),
            color = CreamMuted,
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A soft, breathing glow over one of the room's own flames. */
@Composable
fun HaloPanel(index: Int) {
    if (!Mandir.inside || !MandirScene.ready || index >= MandirPlace.lamps.size) return
    val flicker by rememberFlicker(seed = index + 1, perSecond = 12)
    Canvas(Modifier.fillMaxSize()) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension / 2f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(DiyaAmber.copy(alpha = 0.30f * flicker), DiyaAmber.copy(alpha = 0.08f * flicker), Color.Transparent),
                center = c,
                radius = r,
            ),
            radius = r,
            center = c,
        )
    }
}

@Composable
fun FlamePanel() {
    if (!Mandir.inside || !Mandir.diyaLit) return
    WickFlame(Modifier.fillMaxSize())
}

/** The lamp on the stand that is always burning: where the diya and agarbatti are lit. */
@Composable
fun JyotiFlamePanel() {
    if (!Mandir.inside || !MandirScene.ready) return
    WickFlame(Modifier.fillMaxSize())
}

/** The camphor on the aarti thali, burning while it is in a hand. */
@Composable
fun ThaliFlamePanel() {
    if (!Mandir.inside || !Mandir.thaliHeld) return
    WickFlame(Modifier.fillMaxSize())
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
