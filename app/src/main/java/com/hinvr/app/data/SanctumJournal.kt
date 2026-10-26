package com.hinvr.app.data

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

/** One offering made in the HINVR sanctum on a Quest. Symbolic; nothing reaches a temple. */
data class SanctumOffering(
    val kind: String,
    val at: Instant,
    val deviceId: String,
    val sankalp: String? = null,
    val dedicatedTo: String? = null,
    val mandirId: String? = null,
    /** Who in the member's family it was for, by name, when it was for one of them. */
    val forName: String? = null,
)

/** Offerings from one headset with no gap longer than [SanctumJournal.VisitGap] between them. */
data class SanctumVisit(
    val deviceId: String,
    val start: Instant,
    val end: Instant,
    /** In the order they were first made, each once. */
    val kinds: List<String>,
    val counts: Map<String, Int>,
    val sankalp: String?,
    val dedicatedTo: String?,
    val forName: String? = null,
)

data class SanctumMonth(val visits: Int, val diyas: Int, val japaRounds: Int)

object SanctumJournal {
    val VisitGap: Duration = Duration.ofMinutes(45)

    /** The member's day is the Indian day, the same day the server counts in. */
    val Zone: ZoneId = ZoneId.of("Asia/Kolkata")

    /** What a headset can dedicate an offering to; anything else is shown as no dedication. */
    val Dedications = listOf("self", "family", "parents", "children", "ancestors", "friend", "teacher")

    fun fromRows(rows: List<OfferingRow>, familyNames: Map<String, String> = emptyMap()): List<SanctumOffering> =
        rows.mapNotNull { row ->
        val at = runCatching { OffsetDateTime.parse(row.createdAt).toInstant() }
            .recoverCatching { Instant.parse(row.createdAt) }
            .getOrNull() ?: return@mapNotNull null
        SanctumOffering(
            kind = row.kind,
            at = at,
            deviceId = row.deviceId,
            sankalp = row.sankalp?.trim()?.ifBlank { null },
            dedicatedTo = row.dedicatedTo?.takeIf { it in Dedications },
            mandirId = row.mandirId,
            forName = row.familyMemberId?.let { familyNames[it] },
        )
    }

    /** Newest visit first. */
    fun visits(offerings: List<SanctumOffering>): List<SanctumVisit> {
        val out = ArrayList<SanctumVisit>()
        offerings.groupBy { it.deviceId }.forEach { (device, rows) ->
            var current = ArrayList<SanctumOffering>()
            for (offering in rows.sortedBy { it.at }) {
                val last = current.lastOrNull()
                if (last != null && Duration.between(last.at, offering.at) > VisitGap) {
                    out += visitOf(device, current)
                    current = ArrayList()
                }
                current += offering
            }
            if (current.isNotEmpty()) out += visitOf(device, current)
        }
        return out.sortedByDescending { it.start }
    }

    private fun visitOf(device: String, rows: List<SanctumOffering>): SanctumVisit = SanctumVisit(
        deviceId = device,
        start = rows.first().at,
        end = rows.last().at,
        kinds = rows.map { it.kind }.distinct(),
        counts = rows.groupingBy { it.kind }.eachCount(),
        sankalp = rows.lastOrNull { it.sankalp != null }?.sankalp,
        dedicatedTo = rows.lastOrNull { it.dedicatedTo != null }?.dedicatedTo,
        forName = rows.lastOrNull { it.forName != null }?.forName,
    )

    /** Visits grouped by the day they began, newest day first. */
    fun byDay(visits: List<SanctumVisit>, zone: ZoneId = Zone): List<Pair<LocalDate, List<SanctumVisit>>> =
        visits.groupBy { it.start.atZone(zone).toLocalDate() }
            .toList()
            .sortedByDescending { it.first }

    /**
     * Days in a row with a visit, ending today or yesterday (a day not yet visited doesn't
     * break it). Zero when the last visit is older.
     */
    fun streak(visits: List<SanctumVisit>, today: LocalDate, zone: ZoneId = Zone): Int {
        val days = visits.map { it.start.atZone(zone).toLocalDate() }.toSet()
        var day = if (today in days) today else today.minusDays(1)
        var count = 0
        while (day in days) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    fun month(offerings: List<SanctumOffering>, visits: List<SanctumVisit>, today: LocalDate, zone: ZoneId = Zone): SanctumMonth {
        fun inMonth(at: Instant): Boolean {
            val date = at.atZone(zone).toLocalDate()
            return date.year == today.year && date.month == today.month
        }
        val mine = offerings.filter { inMonth(it.at) }
        return SanctumMonth(
            visits = visits.count { inMonth(it.start) },
            diyas = mine.count { it.kind == "diya" },
            japaRounds = mine.count { it.kind == "japa" },
        )
    }
}
