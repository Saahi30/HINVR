package com.hinvr.quest

import android.content.Context
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.concurrent.Executors
import org.json.JSONObject

/** A request from the member's phone: what to open, and for which mandir. */
data class HandoffRequest(val id: String, val target: String, val mandirId: String?)

/** What the headset does with a request, decided without touching any state. */
sealed interface HandoffAction {
    data class Live(val card: RingCard) : HandoffAction
    data class Tour(val tour: HomeChoice) : HandoffAction
    data class Sanctum(val darshanId: String?) : HandoffAction

    /** Nothing here can be opened for it; say so and stay put. */
    data class Unavailable(val title: String) : HandoffAction
}

object HandoffRules {
    /** The phone's request in terms of what this headset has: its catalog and its 360 walks. */
    fun action(request: HandoffRequest, mandirs: List<RingCard>, tours: List<HomeChoice>): HandoffAction {
        val card = mandirs.firstOrNull { it.mandirId != null && it.mandirId == request.mandirId }
        val tour = tours.firstOrNull { it.mandirId != null && it.mandirId == request.mandirId }
        val streams = card?.hasStream == true
        val title = card?.title ?: tour?.title.orEmpty()
        return when (request.target) {
            "sanctum" -> HandoffAction.Sanctum(card?.mandirId?.takeIf { streams })
            "tour" -> when {
                tour != null -> HandoffAction.Tour(tour)
                streams -> HandoffAction.Live(card)
                else -> HandoffAction.Unavailable(title)
            }
            "live" -> when {
                streams -> HandoffAction.Live(card)
                tour != null -> HandoffAction.Tour(tour)
                else -> HandoffAction.Unavailable(title)
            }
            else -> HandoffAction.Unavailable(title)
        }
    }

    /** Never during the greeting or while pairing; anywhere else the member asked for it. */
    fun canApply(stage: Stage): Boolean = stage != Stage.Splash && stage != Stage.Pair

    /** The server's answer: a request, or JSON null when there is none. */
    fun parse(raw: String): HandoffRequest? {
        val text = raw.trim()
        if (text.isEmpty() || text == "null") return null
        val json = runCatching { JSONObject(text) }.getOrNull() ?: return null
        val id = json.optString("id")
        val target = json.optString("target")
        if (id.isBlank() || target.isBlank()) return null
        val mandir = json.optString("mandir_id").takeIf { it.isNotBlank() && it != "null" }
        return HandoffRequest(id, target, mandir)
    }
}

/**
 * "View in VR" from the phone. The headset has no push, so while HINVR is open and paired it
 * asks the server every few seconds whether the phone has sent something, and opens it.
 */
object Handoff {
    private const val Tag = "HinvrHandoff"
    // Every open headset asks this often, so it stays gentle on the server.
    private const val PollMs = 8_000L
    private const val BackoffMs = 15_000L
    const val NoticeMs = 5_000L

    private val main = Handler(Looper.getMainLooper())
    private val network = Executors.newSingleThreadExecutor()
    private var inFlight = false
    private var nextAtMs = 0L
    private var lastTickMs = 0L

    /** The server has no handoff function: stop asking for this session. */
    private var unsupported = false
    private var pending: HandoffRequest? = null

    /** A line saying what the phone opened, shown for a few seconds. */
    @get:StringRes
    var notice by mutableStateOf(0)
        private set
    var noticeArg by mutableStateOf("")
        private set
    var noticeAtMs by mutableLongStateOf(0L)
        private set

    val showingNotice: Boolean
        get() = notice != 0 && SystemClock.elapsedRealtime() - noticeAtMs < NoticeMs

    /** Called every frame. Frames stop while the app is paused, and so does the asking. */
    fun tick() {
        val now = SystemClock.elapsedRealtime()
        // Back from a pause: ask at once rather than waiting out the old timer.
        if (lastTickMs != 0L && now - lastTickMs > 2_000L) nextAtMs = 0L
        lastTickMs = now
        pending?.let { request ->
            if (HandoffRules.canApply(Ring.stage)) {
                pending = null
                apply(request)
            }
        }
        if (unsupported || inFlight || now < nextAtMs) return
        if (!QuestAccount.configured || !QuestAccount.isPaired) return
        inFlight = true
        val args = QuestAccount.deviceArgs()
        val battery = HeadsetBattery.addTo(args, now)
        network.execute {
            val result = runCatching { QuestAccount.call("vr_claim_handoff", args) }
            main.post {
                inFlight = false
                val error = result.exceptionOrNull()
                if (error == null) {
                    if (battery) HeadsetBattery.sent(args, now)
                    nextAtMs = SystemClock.elapsedRealtime() + PollMs
                    HandoffRules.parse(result.getOrDefault(""))?.let { pending = it }
                    return@post
                }
                if (error is QuestAccount.ServerRefused && error.status == 404) {
                    // An older server knows the handoff but not the battery: keep the handoff.
                    if (battery) HeadsetBattery.unsupported = true else unsupported = true
                    return@post
                }
                Log.w(Tag, "vr_claim_handoff failed", error)
                nextAtMs = SystemClock.elapsedRealtime() + BackoffMs
            }
        }
    }

    private fun apply(request: HandoffRequest) {
        when (val action = HandoffRules.action(request, Ring.mandirs, TourChoices)) {
            is HandoffAction.Live -> {
                Ring.openLiveFrom(action.card)
                say(R.string.handoff_opening, action.card.title)
            }
            is HandoffAction.Tour -> {
                Ring.openTourFrom(action.tour)
                say(R.string.handoff_opening, action.tour.title)
            }
            is HandoffAction.Sanctum -> {
                Ring.openSanctumFrom(action.darshanId)
                say(R.string.handoff_sanctum, "")
            }
            is HandoffAction.Unavailable -> say(R.string.handoff_unavailable, action.title)
        }
    }

    private fun say(@StringRes line: Int, arg: String) {
        notice = line
        noticeArg = arg
        noticeAtMs = SystemClock.elapsedRealtime()
    }
}

/** "Opening Kashi Vishwanath from your phone" — a small pill, a few seconds. */
@Composable
fun HandoffNotice() {
    if (!Handoff.showingNotice) return
    val text = if (Handoff.noticeArg.isBlank()) {
        stringResource(Handoff.notice)
    } else {
        stringResource(Handoff.notice, Handoff.noticeArg)
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text,
            color = Cream,
            fontSize = 17.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .background(InkVeil, PillShape)
                .border(1.dp, Gold.copy(alpha = 0.45f), PillShape)
                .padding(horizontal = 20.dp, vertical = 9.dp),
        )
    }
}

/** When the battery is worth reporting again: first time, charging changed, 5 points moved, or 10 minutes on. */
object BatteryReport {
    const val StepPct = 5
    const val EveryMs = 10 * 60_000L

    fun worthSending(pct: Int, charging: Boolean, lastPct: Int?, lastCharging: Boolean?, sinceMs: Long): Boolean =
        lastPct == null || charging != lastCharging || kotlin.math.abs(pct - lastPct) >= StepPct || sinceMs >= EveryMs
}

/** The headset's own battery, sent with the handoff check only when it has changed (see [BatteryReport]). */
object HeadsetBattery {
    private var manager: BatteryManager? = null
    private var lastPct: Int? = null
    private var lastCharging: Boolean? = null
    private var lastAtMs = 0L

    /** The server doesn't take a battery yet: stop offering one this session. */
    var unsupported = false

    fun attach(context: Context) {
        manager = context.applicationContext.getSystemService(BatteryManager::class.java)
    }

    /** Puts p_battery / p_charging into [args] when worth it; true if it did. */
    fun addTo(args: JSONObject, now: Long): Boolean {
        if (unsupported) return false
        val bm = manager ?: return false
        val pct = runCatching { bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) }.getOrNull()
            ?.takeIf { it in 0..100 } ?: return false
        val charging = bm.isCharging
        if (!BatteryReport.worthSending(pct, charging, lastPct, lastCharging, now - lastAtMs)) return false
        args.put("p_battery", pct).put("p_charging", charging)
        return true
    }

    /** The server took the reading in [args]. */
    fun sent(args: JSONObject, now: Long) {
        lastPct = args.optInt("p_battery")
        lastCharging = args.optBoolean("p_charging")
        lastAtMs = now
    }
}
