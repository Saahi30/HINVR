package com.hinvr.quest

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.meta.spatial.core.Vector3

/** Symbolic visit inside the generic HINVR sanctum. */
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

object Mandir {
    const val Honesty = "Symbolic offering in HINVR. Not performed at a temple."

    val intentions = listOf(
        "For family",
        "For health",
        "For peace",
        "Thanksgiving",
        "No particular intention",
    )

    var inside by mutableStateOf(false)
        private set

    var sankalpOpen by mutableStateOf(true)
    var sankalpName by mutableStateOf("")
    var sankalpGotra by mutableStateOf("")
    var sankalpIntention by mutableStateOf(intentions.first())
    var darshan by mutableStateOf<RingCard?>(null)

    var diyaLit by mutableStateOf(false)
    var agarbattiLit by mutableStateOf(false)
    var agarbattiPlanted by mutableStateOf(false)
    var tilakOn by mutableStateOf(false)
    var aartiCircles by mutableIntStateOf(0)
    var aartiDone by mutableStateOf(false)
    var blessing by mutableStateOf(false)
    var hint by mutableStateOf("Light the diya. Ring the ghanta. A steps you closer.")
    var countsLine by mutableStateOf("")

    val done = mutableStateListOf<OfferingKind>()

    val liveChoices: List<RingCard>
        get() = Ring.mandirs.filter { it.liveUrl.isNotBlank() }

    fun enter() {
        if (inside) return
        inside = true
        resetVisit()
        sankalpOpen = true
        if (sankalpName.isBlank()) sankalpName = QuestAccount.profile?.displayName.orEmpty()
        if (darshan == null) darshan = liveChoices.firstOrNull() ?: Ring.mandirs.firstOrNull()
        MandirOfferings.refreshCounts()
    }

    fun beginVisit() {
        sankalpOpen = false
        hint = "Light the diya. Ring the ghanta. A steps you closer."
    }

    fun leave() {
        if (!inside) return
        inside = false
        blessing = false
        sankalpOpen = true
        MandirScene.hide()
        MandirSound.stopVisit()
    }

    fun resetVisit() {
        diyaLit = false
        agarbattiLit = false
        agarbattiPlanted = false
        tilakOn = false
        aartiCircles = 0
        aartiDone = false
        blessing = false
        done.clear()
        hint = "Light the diya. Ring the ghanta. A steps you closer."
    }

    fun cycleDarshan(delta: Int) {
        val rows = liveChoices
        if (rows.isEmpty()) return
        val index = rows.indexOfFirst { it.mandirId == darshan?.mandirId }.let { if (it < 0) 0 else it }
        darshan = rows[(index + delta).floorMod(rows.size)]
    }

    fun cycleIntention(delta: Int) {
        val index = intentions.indexOf(sankalpIntention).let { if (it < 0) 0 else it }
        sankalpIntention = intentions[(index + delta).floorMod(intentions.size)]
    }

    fun mark(kind: OfferingKind, line: String) {
        if (kind !in done) done.add(kind)
        hint = line
        MandirOfferings.record(kind)
        if (kind == OfferingKind.Aarti) {
            aartiDone = true
            blessing = true
        }
    }

    fun receivePrasad() {
        blessing = true
        hint = "Take the blessing. Prasad is received as you leave."
    }
}

/** Positions from markers.json (glTF / headset space). */
object MandirPlace {
    // Just in front of the offering stand, so a seated guest can reach the props.
    val spawn = Vector3(0f, 0f, 0.05f)
    val garbha = Vector3(0f, 0f, -5.2f)
    val darshan = Vector3(0f, 1.45f, -5.68f)
    val stand = Vector3(0f, 0.78f, -0.85f)
    val ghanta = Vector3(0.85f, 1.55f, -0.55f)
    val peti = Vector3(1.55f, 0.16f, -1.35f)
    val feet = Vector3(0f, 0.82f, -3.85f)
    val incense = Vector3(0.38f, 0.81f, -0.85f)
    val teleportMandap = Vector3(0f, 0f, 0.05f)
    val teleportThreshold = Vector3(0f, 0f, -2.05f)
    val teleportPradakshina = Vector3(1.55f, 0f, -0.4f)

    val marks = listOf(teleportMandap, teleportThreshold, teleportPradakshina)
}

private fun Int.floorMod(size: Int): Int = ((this % size) + size) % size
