package com.hinvr.quest

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit

/** When the "watch together" tip may show: not at once, not every time, not forever. */
object CastTipRules {
    /** Watching this long first, so it never covers the start of a darshan. */
    const val MinWatchMs = 20_000L

    /** Shown at most once in this many sessions. */
    const val EverySessions = 3

    /** After this many showings the member knows. */
    const val MaxTimes = 6

    /** Up this long unless dismissed. */
    const val ShowForMs = 18_000L

    fun shouldShow(session: Int, lastShownSession: Int, timesShown: Int, shownThisSession: Boolean, watchedMs: Long): Boolean =
        !shownThisSession && timesShown < MaxTimes && watchedMs >= MinWatchMs &&
            (lastShownSession < 0 || session - lastShownSession >= EverySessions)
}

/** Below a live darshan: the headset can cast to a phone or TV so the family can watch too. */
object CastTip {
    private var prefs: SharedPreferences? = null
    private var session = 0
    private var shownThisSession = false
    private var watchedMs = 0L
    private var shownForMs = 0L

    var showing by mutableStateOf(false)
        private set

    /** Once per app start: that start is a session. */
    fun attach(context: Context) {
        val store = context.applicationContext.getSharedPreferences("hinvr_quest_prefs", Context.MODE_PRIVATE)
        prefs = store
        session = store.getInt("cast_sessions", 0) + 1
        store.edit { putInt("cast_sessions", session) }
    }

    /** Every frame, with whether a live darshan is playing in front of the member. */
    fun tick(dtMs: Long, watching: Boolean) {
        if (!watching) {
            watchedMs = 0L
            showing = false
            return
        }
        watchedMs += dtMs
        if (showing) {
            shownForMs += dtMs
            if (shownForMs >= CastTipRules.ShowForMs) showing = false
            return
        }
        val store = prefs ?: return
        val last = store.getInt("cast_last_session", -1)
        val times = store.getInt("cast_times", 0)
        if (!CastTipRules.shouldShow(session, last, times, shownThisSession, watchedMs)) return
        shownThisSession = true
        shownForMs = 0L
        showing = true
        store.edit {
            putInt("cast_last_session", session)
            putInt("cast_times", times + 1)
        }
    }

    fun dismiss() {
        showing = false
    }
}

@Composable
fun CastTipPanel() {
    if (!CastTip.showing) return
    val shape = RoundedCornerShape(26.dp)
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(InkVeil)
            .border(1.dp, Gold.copy(alpha = 0.45f), shape)
            .clickable { CastTip.dismiss() }
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CastAnimation(
            Modifier
                .width(118.dp)
                .fillMaxHeight(),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.cast_title), color = Gold, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.cast_body), color = Cream, fontSize = 13.sp, lineHeight = 16.sp, maxLines = 3)
        }
        Text(
            stringResource(R.string.cast_got_it),
            color = Gold,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(PillShape)
                .border(1.2.dp, Gold, PillShape)
                .clickable { CastTip.dismiss() }
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}

/** The headset sends a light along an arc to a TV, which lights up and shows a small flame. */
@Composable
private fun CastAnimation(modifier: Modifier) {
    val t by rememberInfiniteTransition(label = "cast").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_000, easing = LinearEasing)),
        label = "cast-t",
    )
    Canvas(modifier) {
        val from = Offset(size.width * 0.17f, size.height * 0.66f)
        val to = Offset(size.width * 0.80f, size.height * 0.62f)
        val lift = size.height * 0.5f
        // The light travels for the first two thirds of the loop; the TV stays lit for the rest.
        val travel = (t / 0.66f).coerceAtMost(1f)
        val lit = ((t - 0.6f) / 0.15f).coerceIn(0f, 1f) * (1f - ((t - 0.92f) / 0.08f).coerceIn(0f, 1f))
        for (i in 1 until 14) drawCircle(Gold.copy(alpha = 0.35f), radius = 1.2.dp.toPx(), center = arc(from, to, lift, i / 14f))
        drawHeadset(from)
        drawTv(to, lit)
        if (t < 0.7f) {
            for (k in 0 until 4) {
                val at = travel - k * 0.05f
                if (at < 0f) continue
                val p = arc(from, to, lift, at)
                val strength = 1f - k / 4f
                drawCircle(
                    Brush.radialGradient(listOf(DiyaAmber.copy(alpha = 0.5f * strength), Color.Transparent), center = p, radius = 9.dp.toPx()),
                    radius = 9.dp.toPx(),
                    center = p,
                )
                drawCircle(FlameCore.copy(alpha = strength), radius = 2.2.dp.toPx(), center = p)
            }
        }
    }
}

private fun arc(from: Offset, to: Offset, lift: Float, t: Float): Offset {
    val c = Offset((from.x + to.x) / 2f, minOf(from.y, to.y) - lift)
    val u = 1f - t
    return Offset(u * u * from.x + 2 * u * t * c.x + t * t * to.x, u * u * from.y + 2 * u * t * c.y + t * t * to.y)
}

private fun DrawScope.drawHeadset(at: Offset) {
    val w = 30.dp.toPx()
    val h = 15.dp.toPx()
    val stroke = Stroke(width = 1.6.dp.toPx())
    drawRoundRect(Gold, Offset(at.x - w / 2f, at.y - h / 2f), Size(w, h), CornerRadius(h * 0.45f), style = stroke)
    for (s in listOf(-1f, 1f)) drawCircle(Gold, radius = h * 0.2f, center = Offset(at.x + s * w * 0.22f, at.y), style = stroke)
}

private fun DrawScope.drawTv(at: Offset, lit: Float) {
    val w = 40.dp.toPx()
    val h = 26.dp.toPx()
    val top = Offset(at.x - w / 2f, at.y - h / 2f)
    if (lit > 0f) {
        drawCircle(
            Brush.radialGradient(listOf(DiyaAmber.copy(alpha = 0.4f * lit), Color.Transparent), center = at, radius = w * 0.9f),
            radius = w * 0.9f,
            center = at,
        )
        drawRoundRect(Ink.copy(alpha = 0.6f), top, Size(w, h), CornerRadius(3.dp.toPx()))
        drawRoundRect(DiyaAmber.copy(alpha = 0.22f * lit), top, Size(w, h), CornerRadius(3.dp.toPx()))
        // A small flame on the screen: the darshan, now on the TV.
        val fh = h * 0.46f * lit
        val fw = w * 0.09f
        val base = at.y + h * 0.22f
        val flame = Path().apply {
            moveTo(at.x, base - fh)
            cubicTo(at.x + fw, base - fh * 0.5f, at.x + fw, base - fh * 0.1f, at.x, base)
            cubicTo(at.x - fw, base - fh * 0.1f, at.x - fw, base - fh * 0.5f, at.x, base - fh)
            close()
        }
        drawPath(flame, Brush.verticalGradient(listOf(FlameCore, DiyaAmber, Ember), startY = base - fh, endY = base))
    }
    drawRoundRect(Gold, top, Size(w, h), CornerRadius(3.dp.toPx()), style = Stroke(width = 1.6.dp.toPx()))
    drawLine(Gold, Offset(at.x - w * 0.2f, at.y + h / 2f + 4.dp.toPx()), Offset(at.x + w * 0.2f, at.y + h / 2f + 4.dp.toPx()), 1.6.dp.toPx(), StrokeCap.Round)
}
