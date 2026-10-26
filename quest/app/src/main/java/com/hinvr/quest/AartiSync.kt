package com.hinvr.quest

import java.time.Instant
import java.time.ZoneOffset

/**
 * Whether a temple's official stream is live during one of its aartis. Times come from the
 * server as "HH:MM" in India time, whatever zone the headset is in.
 */
object AartiSync {
    /** An aarti counts as on for this long after it starts. */
    const val WindowMinutes = 40

    private const val Day = 24 * 60
    private val India = ZoneOffset.ofHoursMinutes(5, 30)

    /** "06:30", "6:30" or "18:00" as minutes after midnight; anything else is dropped. */
    fun parseTimes(values: List<String>): List<Int> = values.mapNotNull { raw ->
        val parts = raw.trim().split(':')
        if (parts.size != 2) return@mapNotNull null
        val hours = parts[0].toIntOrNull() ?: return@mapNotNull null
        val minutes = parts[1].take(2).toIntOrNull() ?: return@mapNotNull null
        if (hours !in 0..23 || minutes !in 0..59) null else hours * 60 + minutes
    }.distinct().sorted()

    /** Minutes after midnight in India at [epochMs]. */
    fun minutesInIndia(epochMs: Long): Int {
        val time = Instant.ofEpochMilli(epochMs).atOffset(India)
        return time.hour * 60 + time.minute
    }

    /** Within [WindowMinutes] after [start], across midnight too. */
    fun inWindow(nowMinutes: Int, start: Int): Boolean = Math.floorMod(nowMinutes - start, Day) < WindowMinutes

    /** The first live mandir with an aarti on now, or null. */
    fun liveNow(mandirs: List<RingCard>, epochMs: Long): RingCard? {
        val now = minutesInIndia(epochMs)
        return mandirs.firstOrNull { card ->
            card.hasStream && card.live == LiveState.Live && card.aartiTimes.any { inWindow(now, it) }
        }
    }
}
