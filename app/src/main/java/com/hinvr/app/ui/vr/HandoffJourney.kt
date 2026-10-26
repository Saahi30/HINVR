package com.hinvr.app.ui.vr

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hinvr.app.R
import com.hinvr.app.data.HandoffPhase
import com.hinvr.app.ui.theme.DiyaAmber
import com.hinvr.app.ui.theme.FlameCore
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography
import kotlin.math.exp
import kotlinx.coroutines.delay

/** How full the wait bar is [seconds] into waiting: about three quarters at 8 s, never full until it opens. */
fun handoffProgress(seconds: Float, opened: Boolean): Float =
    if (opened) 1f else 0.92f * (1f - exp(-seconds / 5.5f))

/** After this long without the headset, say what usually helps. */
const val HandoffSlowHintSeconds = 25f

/**
 * What the member watches while the headset picks the request up: a light carried from the
 * phone to the headset, the three steps ticking off, and a bar that keeps moving.
 */
@Composable
fun HandoffJourney(phase: HandoffPhase, title: String, modifier: Modifier = Modifier) {
    val colors = HinvrTheme.colors
    val opened = phase is HandoffPhase.Opened
    val asking = phase == HandoffPhase.Asking
    // Seconds since the request reached the server, ticking while the sheet waits.
    var waitingSince by remember { mutableLongStateOf(0L) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(phase) {
        if (phase == HandoffPhase.Waiting && waitingSince == 0L) waitingSince = System.currentTimeMillis()
        while (phase == HandoffPhase.Waiting) {
            nowMs = System.currentTimeMillis()
            delay(100)
        }
    }
    val seconds = if (waitingSince == 0L) 0f else (nowMs - waitingSince) / 1000f
    val progress by animateFloatAsState(
        targetValue = if (asking) 0.04f else handoffProgress(seconds, opened),
        animationSpec = tween(if (opened) 600 else 220, easing = FastOutSlowInEasing),
        label = "handoff-progress",
    )
    val travel by rememberInfiniteTransition(label = "handoff-travel").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1700, easing = LinearEasing)),
        label = "travel",
    )
    val arrived by animateFloatAsState(if (opened) 1f else 0f, tween(700, easing = FastOutSlowInEasing), label = "arrived")

    Column(modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(92.dp),
        ) {
            val phoneAt = Offset(size.width * 0.14f, size.height * 0.62f)
            val headsetAt = Offset(size.width * 0.86f, size.height * 0.62f)
            val lift = size.height * 0.55f
            drawArcPath(phoneAt, headsetAt, lift, colors.goldDim.copy(alpha = 0.55f))
            drawPhone(phoneAt, colors.gold)
            drawHeadset(headsetAt, colors.gold, glow = arrived)
            if (!opened) {
                // The light and its trail, carried along the arc again and again.
                for (k in 0 until 6) {
                    val t = travel - k * 0.045f
                    if (t < 0f) continue
                    drawLight(pointOnArc(phoneAt, headsetAt, lift, t), (1f - k / 6f) * if (asking) 0.45f else 1f, k == 0)
                }
            } else {
                drawLight(pointOnArc(phoneAt, headsetAt, lift, 1f) - Offset(0f, size.height * 0.36f * arrived), arrived, true)
            }
        }
        Spacer(Modifier.height(14.dp))
        Step(
            state = if (asking) StepState.Active else StepState.Done,
            text = stringResource(R.string.handoff_step_sent),
        )
        Step(
            state = when {
                opened -> StepState.Done
                asking -> StepState.Pending
                else -> StepState.Active
            },
            text = stringResource(R.string.handoff_step_waiting),
            detail = stringResource(R.string.handoff_step_waiting_detail),
        )
        Step(
            state = if (opened) StepState.Done else StepState.Pending,
            text = stringResource(R.string.handoff_step_opening, title),
        )
        Spacer(Modifier.height(12.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(6.dp),
        ) {
            val r = CornerRadius(size.height / 2f)
            drawRoundRect(colors.goldDim.copy(alpha = 0.25f), cornerRadius = r)
            drawRoundRect(
                Brush.horizontalGradient(listOf(colors.saffron, colors.gold)),
                size = Size(size.width * progress, size.height),
                cornerRadius = r,
            )
        }
        AnimatedVisibility(visible = !opened && seconds > HandoffSlowHintSeconds, enter = fadeIn()) {
            Text(
                stringResource(R.string.handoff_slow_hint),
                style = HinvrTypography.bodyMedium,
                color = colors.inkMuted,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

private enum class StepState { Done, Active, Pending }

@Composable
private fun Step(state: StepState, text: String, detail: String? = null) {
    val colors = HinvrTheme.colors
    val pulse by rememberInfiniteTransition(label = "step-pulse").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    Row(
        Modifier.padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(18.dp)) {
                val r = size.minDimension / 2f
                when (state) {
                    StepState.Done -> {
                        drawCircle(colors.gold, r)
                        val check = Path().apply {
                            moveTo(size.width * 0.28f, size.height * 0.52f)
                            lineTo(size.width * 0.44f, size.height * 0.68f)
                            lineTo(size.width * 0.74f, size.height * 0.36f)
                        }
                        drawPath(check, colors.ivory, style = Stroke(width = r * 0.28f, cap = StrokeCap.Round))
                    }
                    StepState.Active -> {
                        drawCircle(colors.gold.copy(alpha = 0.25f * pulse), r)
                        drawCircle(colors.gold, r * 0.42f * (0.8f + 0.2f * pulse))
                    }
                    StepState.Pending -> drawCircle(colors.goldDim.copy(alpha = 0.6f), r * 0.9f, style = Stroke(width = r * 0.16f))
                }
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                text,
                style = HinvrTypography.bodyLarge,
                color = if (state == StepState.Pending) colors.inkMuted else colors.ink,
            )
            if (detail != null && state == StepState.Active) {
                Text(detail, style = HinvrTypography.bodyMedium, color = colors.inkMuted)
            }
        }
    }
}

private fun pointOnArc(from: Offset, to: Offset, lift: Float, t: Float): Offset {
    val c = Offset((from.x + to.x) / 2f, minOf(from.y, to.y) - lift)
    val u = 1f - t
    return Offset(u * u * from.x + 2 * u * t * c.x + t * t * to.x, u * u * from.y + 2 * u * t * c.y + t * t * to.y)
}

private fun DrawScope.drawArcPath(from: Offset, to: Offset, lift: Float, color: Color) {
    val c = Offset((from.x + to.x) / 2f, minOf(from.y, to.y) - lift)
    // Dotted, so it reads as a path the light takes rather than a line.
    for (i in 1 until 24) {
        val p = run {
            val t = i / 24f
            val u = 1f - t
            Offset(u * u * from.x + 2 * u * t * c.x + t * t * to.x, u * u * from.y + 2 * u * t * c.y + t * t * to.y)
        }
        drawCircle(color, radius = 1.6.dp.toPx(), center = p)
    }
}

private fun DrawScope.drawLight(at: Offset, strength: Float, head: Boolean) {
    val r = (if (head) 9.dp else 5.dp).toPx()
    drawCircle(
        Brush.radialGradient(listOf(DiyaAmber.copy(alpha = 0.55f * strength), Color.Transparent), center = at, radius = r * 2.2f),
        radius = r * 2.2f,
        center = at,
    )
    drawCircle(FlameCore.copy(alpha = strength), radius = r * 0.45f, center = at)
}

private fun DrawScope.drawPhone(at: Offset, color: Color) {
    val w = 22.dp.toPx()
    val h = 38.dp.toPx()
    drawRoundRect(
        color,
        topLeft = Offset(at.x - w / 2f, at.y - h / 2f),
        size = Size(w, h),
        cornerRadius = CornerRadius(5.dp.toPx()),
        style = Stroke(width = 2.dp.toPx()),
    )
    drawLine(color, Offset(at.x - w * 0.18f, at.y + h * 0.38f), Offset(at.x + w * 0.18f, at.y + h * 0.38f), 2.dp.toPx(), StrokeCap.Round)
}

private fun DrawScope.drawHeadset(at: Offset, color: Color, glow: Float) {
    val w = 52.dp.toPx()
    val h = 26.dp.toPx()
    if (glow > 0f) {
        drawCircle(
            Brush.radialGradient(listOf(DiyaAmber.copy(alpha = 0.45f * glow), Color.Transparent), center = at, radius = w),
            radius = w,
            center = at,
        )
    }
    drawRoundRect(
        color,
        topLeft = Offset(at.x - w / 2f, at.y - h / 2f),
        size = Size(w, h),
        cornerRadius = CornerRadius(h * 0.45f),
        style = Stroke(width = 2.dp.toPx()),
    )
    // The two lenses, lit once the headset has it.
    for (s in listOf(-1f, 1f)) {
        val lens = Offset(at.x + s * w * 0.22f, at.y)
        drawCircle(FlameCore.copy(alpha = 0.9f * glow), radius = h * 0.22f, center = lens)
        drawCircle(color, radius = h * 0.22f, center = lens, style = Stroke(width = 1.5.dp.toPx()))
    }
    drawLine(color, Offset(at.x - w / 2f, at.y - h * 0.1f), Offset(at.x - w * 0.62f, at.y - h * 0.32f), 2.dp.toPx(), StrokeCap.Round)
    drawLine(color, Offset(at.x + w / 2f, at.y - h * 0.1f), Offset(at.x + w * 0.62f, at.y - h * 0.32f), 2.dp.toPx(), StrokeCap.Round)
}
