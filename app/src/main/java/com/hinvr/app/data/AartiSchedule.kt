package com.hinvr.app.data

import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * When to remind a member before a mandir's aarti. Times arrive as 'HH:MM' in India time
 * (mandirs.aarti_times); everything here works in instants, so the phone's own time zone
 * only matters when the reminder is shown.
 */
object AartiSchedule {
    val India: ZoneId = ZoneId.of("Asia/Kolkata")
    val Leads = listOf(10, 15, 30)
    const val DefaultLead = 15

    /** A reminder that fires this long after its aarti has started is not worth showing. */
    val LateBy: Duration = Duration.ofMinutes(2)

    private val Hhmm = Regex("""^([01]\d|2[0-3]):([0-5]\d)$""")

    /** The well-formed times, sorted, without repeats. */
    fun parse(times: List<String>): List<LocalTime> =
        times.mapNotNull { raw ->
            Hhmm.matchEntire(raw.trim())?.let { LocalTime.of(it.groupValues[1].toInt(), it.groupValues[2].toInt()) }
        }.distinct().sorted()

    /** The next aarti whose reminder ([leadMinutes] before it) is still ahead of [now]. */
    fun next(times: List<LocalTime>, leadMinutes: Int, now: Instant): Upcoming? {
        if (times.isEmpty()) return null
        val today = now.atZone(India).toLocalDate()
        val lead = Duration.ofMinutes(leadMinutes.toLong())
        return (0L..2L).asSequence()
            .flatMap { day -> times.asSequence().map { today.plusDays(day).atTime(it).atZone(India).toInstant() } }
            .map { start -> Upcoming(start, start.minus(lead)) }
            .filter { it.remindAt.isAfter(now) }
            .minByOrNull { it.remindAt }
    }

    /** Minutes from [now] until [start], to the nearest minute, never below zero. */
    fun minutesUntil(start: Instant, now: Instant): Int =
        ((Duration.between(now, start).seconds + 30) / 60).coerceAtLeast(0).toInt()

    data class Upcoming(val start: Instant, val remindAt: Instant)
}
