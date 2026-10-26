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

    /** Not puja steps: a round of the mala, and two festival pujas. */
    Japa("japa"),
    Abhishek("abhishek"),
    Deepmala("deepmala"),
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

/** Whom the visit is offered for. [wire] is the server's key; [blessing] the line on the blessing. */
enum class Dedication(@StringRes val label: Int, val wire: String, @StringRes val blessing: Int) {
    Myself(R.string.for_self, "self", R.string.blessing_for_self),
    Family(R.string.for_family, "family", R.string.blessing_for_family),
    Parents(R.string.for_parents, "parents", R.string.blessing_for_parents),
    Children(R.string.for_children, "children", R.string.blessing_for_children),
    Ancestors(R.string.for_ancestors, "ancestors", R.string.blessing_for_ancestors),
    Friend(R.string.for_friend, "friend", R.string.blessing_for_friend),
    Teacher(R.string.for_teacher, "teacher", R.string.blessing_for_teacher),
    ;

    fun next(delta: Int = 1): Dedication = entries[(ordinal + delta).floorMod(entries.size)]
}

/** One "For" choice: a preset, or a family member by name (sent as family, with their id). */
data class DedicationChoice(val preset: Dedication, val member: FamilyMember? = null)

/** The "For" choices in order: the presets, then the member's family by name. */
object DedicationChoices {
    fun all(family: List<FamilyMember>): List<DedicationChoice> =
        Dedication.entries.map { DedicationChoice(it) } + family.map { DedicationChoice(Dedication.Family, it) }

    fun next(current: DedicationChoice, family: List<FamilyMember>, delta: Int = 1): DedicationChoice {
        val choices = all(family)
        val index = choices.indexOf(current).let { if (it < 0) 0 else it }
        return choices[(index + delta).floorMod(choices.size)]
    }
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
    var dedication by mutableStateOf(Dedication.Myself)
        private set

    /** Set when the visit is for a named family member; [dedication] is then [Dedication.Family]. */
    var familyMember by mutableStateOf<FamilyMember?>(null)
        private set
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

    /** The latest thing that happened, shown on the guide for [EventMs]; 0 when nothing has yet. */
    @get:StringRes
    var event by mutableIntStateOf(0)
        private set

    /** Whether the event line ends with the honesty note. */
    var eventHonest by mutableStateOf(false)
        private set
    var eventAtMs by mutableLongStateOf(0L)
        private set

    /** The prop a hand would take, or ring, if the member pressed now. Its label floats over it. */
    var hover by mutableStateOf<String?>(null)

    /** The aarti thali is in a hand, so its flame shows. */
    var thaliHeld by mutableStateOf(false)

    /** The prop in a hand right now, if any: the first-visit walk-through watches for the diya. */
    var heldProp by mutableStateOf<String?>(null)

    /** The darshan brought forward to a big screen in front of the member, instead of in the garbha. */
    var darshanClose by mutableStateOf(false)

    var diyasToday by mutableIntStateOf(0)

    /** The festival's own puja is done. */
    var festivalDone by mutableStateOf(false)
        private set

    /** Which small diyas on the deepmala tray burn. */
    val deepmalaLit = mutableStateListOf<Int>()

    val done = mutableStateListOf<OfferingKind>()

    val liveChoices: List<RingCard>
        get() = Ring.mandirs.filter { it.hasStream }

    /** Called by Ring when the stage becomes the Mandir. */
    fun enter() {
        resetVisit()
        phase = MandirPhase.Sankalp
        if (sankalpName.isBlank()) sankalpName = QuestAccount.profile?.displayName.orEmpty()
        // Someone removed from the family on the phone since the last visit.
        familyMember?.let { member -> if (family.none { it.id == member.id }) familyMember = null }
        Japa.reset()
        MandirCelebration.stop()
        Festivals.refresh()
        darshan = darshan?.let { current -> liveChoices.firstOrNull { it.mandirId == current.mandirId } }
            ?: CatalogRules.defaultDarshan(liveChoices)
        MandirOfferings.beginVisit()
        MandirOfferings.refreshCounts()
        MandirScene.show()
    }

    /** Called by Ring when the stage leaves the Mandir, whatever caused it. */
    fun exit() {
        phase = MandirPhase.Sankalp
        darshanClose = false
        Tutorial.skip()
        MandirCelebration.stop()
        Japa.active = false
        MandirScene.hide()
        MandirSound.stopVisit()
    }

    fun beginVisit() {
        if (!inside) return
        phase = MandirPhase.Visit
        clearEvent()
        Tutorial.begin()
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
        hover = null
        thaliHeld = false
        heldProp = null
        festivalDone = false
        deepmalaLit.clear()
        done.clear()
        clearEvent()
    }

    /** Shows [line] on the guide for [EventMs], then the guide goes back to the next step. */
    fun setEvent(@StringRes line: Int, honest: Boolean = false) {
        event = line
        eventHonest = honest
        eventAtMs = SystemClock.elapsedRealtime()
    }

    private fun clearEvent() {
        event = 0
        eventHonest = false
        eventAtMs = 0L
    }

    fun cycleDarshan(delta: Int) {
        val rows = liveChoices
        if (rows.isEmpty()) return
        val index = rows.indexOfFirst { it.mandirId == darshan?.mandirId }.let { if (it < 0) 0 else it }
        darshan = rows[(index + delta).floorMod(rows.size)]
    }

    /** The window shows [card]'s stream: the live aarti banner's Watch. */
    fun showDarshan(card: RingCard) {
        darshan = liveChoices.firstOrNull { it.mandirId == card.mandirId } ?: return
    }

    fun cycleDedication(delta: Int = 1) {
        val next = DedicationChoices.next(DedicationChoice(dedication, familyMember), family, delta)
        dedication = next.preset
        familyMember = next.member
    }

    private val family: List<FamilyMember> get() = QuestAccount.profile?.family.orEmpty()

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
        setEvent(line, honest = true)
        MandirOfferings.record(kind)
        if (kind == OfferingKind.Aarti) {
            aartiDone = true
            phase = MandirPhase.Blessing
            MandirCelebration.start()
        }
    }

    /** Today's festival puja is offered. Recorded under its own key, so it counts beside the day's steps. */
    fun markFestival() {
        val festival = Festivals.active ?: return
        if (!inside || festivalDone) return
        festivalDone = true
        setEvent(festival.kind.done, honest = true)
        MandirOfferings.record(festival.kind.records, key = "festival-${festival.kind.wire}")
    }

    fun receivePrasad() {
        if (!inside) return
        phase = MandirPhase.Blessing
        setEvent(R.string.hint_prasad)
    }

    const val EventMs = 3_000L
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
    var darshan = Vector3(0f, 1.55f, -5.68f)
        private set

    /** The centre of the stand's top surface. */
    var stand = Vector3(0f, 0.82f, -0.55f)
        private set

    /** The flame of the lamp that always burns on the stand. */
    var jyoti = Vector3(-0.47f, 0.985f, -0.69f)
        private set

    /** The bell's centre, and the ceiling hook it hangs from (props/ghanta.glb's origin). */
    var ghanta = Vector3(0.7f, 1.6f, -0.25f)
        private set
    var ghantaPivot = Vector3(0.7f, 4.6f, -0.25f)
        private set

    /** The top of the daan peti's slot. */
    var peti = Vector3(0.98f, 0.8f, -0.62f)
        private set

    /** The top of the brass plate on the ledge before the garbha door. */
    var feet = Vector3(0f, 0.455f, -3.15f)
        private set

    /** The sand in the incense holder. */
    var incense = Vector3(0.44f, 0.865f, -0.69f)
        private set

    /** The top of the small table left of the stand: the mala, and a festival's offering. */
    var side = Vector3(-0.98f, 0.8f, -0.62f)
        private set

    /** The deepmala tray's wick tops, relative to the tray's origin. */
    var deepmalaWicks: List<Vector3> = (-1..1).flatMap { z -> (-1..1).map { x -> Vector3(x * 0.085f, 0.046f, z * 0.085f) } }
        private set

    /** Where the room's own flames burn: niche lamps, lamp stands, garbha lamps, the rangoli diya. */
    var lamps: List<Vector3> = listOf(
        Vector3(-3.11f, 1.25f, 0.05f), Vector3(3.11f, 1.25f, 0.05f),
        Vector3(-1.25f, 1.22f, -3.15f), Vector3(1.25f, 1.22f, -3.15f),
        Vector3(-0.8f, 0.89f, -5.0f), Vector3(0.8f, 0.89f, -5.0f),
        Vector3(0f, 0.06f, -1.7f),
    )
        private set

    /** Files under assets/mandir and assets/mandir/props, so a missing one is skipped, not crashed on. */
    private var files: Set<String> = emptySet()

    fun hasAsset(path: String): Boolean = files.isEmpty() || path in files

    /** The mandap at the stand, then the threshold of the garbha griha. A / X moves between them. */
    var marks = listOf(Vector3(0f, 0f, 0.05f), Vector3(0f, 0f, -2.35f))
        private set

    fun load(context: Context) {
        files = runCatching {
            val assets = context.assets
            (assets.list("mandir").orEmpty().map { "mandir/$it" } + assets.list("mandir/props").orEmpty().map { "mandir/props/$it" }).toSet()
        }.getOrDefault(emptySet())
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
        jyoti = read("jyoti", jyoti)
        ghanta = read("ghanta", ghanta)
        ghantaPivot = read("ghanta_pivot", Vector3(ghanta.x, ghantaPivot.y, ghanta.z))
        peti = read("peti", peti)
        feet = read("feet", feet)
        incense = read("incense", incense)
        marks = listOf(
            read("teleport_mandap", marks[0]),
            read("teleport_threshold", marks[1]),
        )
        side = read("side", side)
        fun readList(key: String, fallback: List<Vector3>): List<Vector3> {
            val array = json.optJSONArray(key) ?: return fallback
            val points = (0 until array.length()).mapNotNull { i ->
                val p = array.optJSONArray(i) ?: return@mapNotNull null
                if (p.length() != 3) null else Vector3(p.optDouble(0).toFloat(), p.optDouble(1).toFloat(), p.optDouble(2).toFloat())
            }
            return points.ifEmpty { fallback }
        }
        deepmalaWicks = readList("deepmala_wicks", deepmalaWicks)
        lamps = readList("lamps", lamps)
    }
}
