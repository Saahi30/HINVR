package com.hinvr.quest

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.meta.spatial.core.Vector3
import org.json.JSONObject

/** Symbolic visit inside the generic HINVR sanctum. Labels are the server's allowed kinds. */
enum class OfferingKind(val label: String) {
    Ghanta("ghanta"),
    Diya("diya"),
    Aarti("aarti"),
    Pushp("pushp"),
    Agarbatti("agarbatti"),
    Prasad("prasad"),
    Tilak("tilak"),
    Daan("daan"),
}

/** Sankalp first, then the offerings, then the blessing. One at a time. */
enum class MandirPhase { Sankalp, Visit, Blessing }

/** The wire text stays English so the server's records read the same from every headset. */
enum class Intention(@StringRes val label: Int, val wire: String) {
    Family(R.string.intention_family, "For family"),
    Health(R.string.intention_health, "For health"),
    Peace(R.string.intention_peace, "For peace"),
    Thanks(R.string.intention_thanks, "Thanksgiving"),
    None(R.string.intention_none, "No particular intention"),
}

object Mandir {
    val inside: Boolean get() = Ring.stage == Stage.Mandir

    var phase by mutableStateOf(MandirPhase.Sankalp)
        private set

    val sankalpOpen: Boolean get() = inside && phase == MandirPhase.Sankalp
    val blessing: Boolean get() = inside && phase == MandirPhase.Blessing

    var sankalpName by mutableStateOf("")
    var sankalpGotra by mutableStateOf("")
    var sankalpIntention by mutableStateOf(Intention.Family)
    var darshan by mutableStateOf<RingCard?>(null)
        private set

    var diyaLit by mutableStateOf(false)
    var agarbattiLit by mutableStateOf(false)
    var agarbattiPlanted by mutableStateOf(false)
    var tilakOn by mutableStateOf(false)

    /** The tilak's warm glow is a moment, not a filter over the whole visit. */
    var tilakAtMs by mutableLongStateOf(0L)
        private set
    var aartiCircles by mutableIntStateOf(0)
    var aartiDone by mutableStateOf(false)

    @get:StringRes
    var hint by mutableIntStateOf(R.string.hint_start)

    /** Whether the hint line ends with the honesty note. */
    var hintHonest by mutableStateOf(false)
    var diyasToday by mutableIntStateOf(0)

    val done = mutableStateListOf<OfferingKind>()

    val liveChoices: List<RingCard>
        get() = Ring.mandirs.filter { it.hasStream }

    /** Called by Ring when the stage becomes the Mandir. */
    fun enter() {
        resetVisit()
        phase = MandirPhase.Sankalp
        if (sankalpName.isBlank()) sankalpName = QuestAccount.profile?.displayName.orEmpty()
        darshan = darshan?.let { current -> liveChoices.firstOrNull { it.mandirId == current.mandirId } }
            ?: CatalogRules.defaultDarshan(liveChoices)
        MandirOfferings.beginVisit()
        MandirOfferings.refreshCounts()
        MandirScene.show()
    }

    /** Called by Ring when the stage leaves the Mandir, whatever caused it. */
    fun exit() {
        phase = MandirPhase.Sankalp
        MandirScene.hide()
        MandirSound.stopVisit()
    }

    fun beginVisit() {
        if (!inside) return
        phase = MandirPhase.Visit
        setHint(R.string.hint_start)
    }

    fun catalogChanged() {
        val current = darshan ?: return
        darshan = liveChoices.firstOrNull { it.mandirId == current.mandirId } ?: CatalogRules.defaultDarshan(liveChoices)
    }

    fun resetVisit() {
        diyaLit = false
        agarbattiLit = false
        agarbattiPlanted = false
        tilakOn = false
        tilakAtMs = 0L
        aartiCircles = 0
        aartiDone = false
        done.clear()
        setHint(R.string.hint_start)
    }

    fun setHint(@StringRes line: Int, honest: Boolean = false) {
        hint = line
        hintHonest = honest
    }

    fun cycleDarshan(delta: Int) {
        val rows = liveChoices
        if (rows.isEmpty()) return
        val index = rows.indexOfFirst { it.mandirId == darshan?.mandirId }.let { if (it < 0) 0 else it }
        darshan = rows[(index + delta).floorMod(rows.size)]
    }

    fun cycleIntention(delta: Int) {
        val all = Intention.entries
        sankalpIntention = all[(sankalpIntention.ordinal + delta).floorMod(all.size)]
    }

    fun applyTilak() {
        tilakOn = true
        tilakAtMs = SystemClock.elapsedRealtime()
    }

    fun mark(kind: OfferingKind, @StringRes line: Int) {
        if (!inside) return
        if (kind !in done) done.add(kind)
        setHint(line, honest = true)
        MandirOfferings.record(kind)
        if (kind == OfferingKind.Aarti) {
            aartiDone = true
            phase = MandirPhase.Blessing
        }
    }

    fun receivePrasad() {
        if (!inside) return
        phase = MandirPhase.Blessing
        setHint(R.string.hint_prasad)
    }
}

/**
 * Positions in the sanctum (glTF / headset space), read from assets/mandir/markers.json,
 * which build_sanctum.py writes next to the room. The values below are only a fallback
 * for a missing or broken file and match the shipped one.
 */
object MandirPlace {
    private const val Tag = "HinvrMandir"

    var spawn = Vector3(0f, 0f, 0.05f)
        private set
    var garbha = Vector3(0f, 0f, -5.2f)
        private set
    var darshan = Vector3(0f, 1.45f, -5.68f)
        private set
    var stand = Vector3(0f, 0.78f, -0.85f)
        private set
    var ghanta = Vector3(0.85f, 1.55f, -0.55f)
        private set
    var peti = Vector3(1.55f, 0.16f, -1.35f)
        private set
    var feet = Vector3(0f, 0.82f, -3.85f)
        private set
    var incense = Vector3(0.38f, 0.81f, -0.85f)
        private set
    var marks = listOf(Vector3(0f, 0f, 0.05f), Vector3(0f, 0f, -2.05f), Vector3(1.55f, 0f, -0.4f))
        private set

    fun load(context: Context) {
        val json = runCatching {
            context.assets.open("mandir/markers.json").bufferedReader().use { JSONObject(it.readText()) }
        }.onFailure { Log.w(Tag, "markers.json unreadable; using built-in positions", it) }.getOrNull() ?: return
        fun read(key: String, fallback: Vector3): Vector3 {
            val array = json.optJSONArray(key) ?: return fallback
            if (array.length() != 3) return fallback
            return Vector3(array.optDouble(0).toFloat(), array.optDouble(1).toFloat(), array.optDouble(2).toFloat())
        }
        spawn = read("spawn", spawn)
        garbha = read("garbha", garbha)
        darshan = read("darshan", darshan)
        stand = read("stand", stand)
        ghanta = read("ghanta", ghanta)
        peti = read("peti", peti)
        feet = read("feet", feet)
        incense = read("incense", stand + Vector3(0.38f, 0.03f, 0f))
        marks = listOf(
            read("teleport_mandap", marks[0]),
            read("teleport_threshold", marks[1]),
            read("teleport_pradakshina", marks[2]),
        )
    }
}
