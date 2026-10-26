package com.hinvr.quest

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** The mantras the japa panel offers. Text only: nothing is chanted for the member. */
enum class Mantra(@StringRes val label: Int) {
    Shiva(R.string.mantra_shiva),
    Gayatri(R.string.mantra_gayatri),
    HareKrishna(R.string.mantra_hare_krishna),
    Om(R.string.mantra_om),
}

/** One mala: 108 beads, felt in quarters of 27, then the guru bead and round again. */
class JapaCounter(val beads: Int = 108, val quarter: Int = 27) {
    enum class Bead { Bead, Quarter, Round }

    var count = 0
        private set
    var rounds = 0
        private set

    fun advance(): Bead {
        count++
        if (count >= beads) {
            count = 0
            rounds++
            return Bead.Round
        }
        return if (count % quarter == 0) Bead.Quarter else Bead.Bead
    }

    fun reset() {
        count = 0
        rounds = 0
    }
}

/** Japa in this visit: optional, never one of the puja steps, and it never holds back the blessing. */
object Japa {
    private val counter = JapaCounter()

    var mantra by mutableStateOf(Mantra.Shiva)
        private set

    /** The mala is in the member's hand, counting. */
    var active by mutableStateOf(false)

    var beads by mutableIntStateOf(0)
        private set
    var rounds by mutableIntStateOf(0)
        private set

    val total: Int get() = counter.beads

    fun cycleMantra() {
        mantra = Mantra.entries[(mantra.ordinal + 1) % Mantra.entries.size]
    }

    /** One bead under the thumb. A completed round is recorded once. */
    fun advance(): JapaCounter.Bead {
        val bead = counter.advance()
        beads = counter.count
        rounds = counter.rounds
        if (bead == JapaCounter.Bead.Round) MandirOfferings.record(OfferingKind.Japa, key = "japa-${counter.rounds}")
        return bead
    }

    fun reset() {
        counter.reset()
        beads = 0
        rounds = 0
        active = false
    }
}
