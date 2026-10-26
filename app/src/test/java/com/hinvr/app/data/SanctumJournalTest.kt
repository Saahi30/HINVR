package com.hinvr.app.data

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SanctumJournalTest {

    // 2026-10-27 09:00 in India is 03:30 UTC.
    private val morning = Instant.parse("2026-10-27T03:30:00Z")

    private fun offering(kind: String, minutes: Long, device: String = "quest-a", dedicatedTo: String? = null, base: Instant = morning) =
        SanctumOffering(kind = kind, at = base.plusSeconds(minutes * 60), deviceId = device, dedicatedTo = dedicatedTo)

    @Test
    fun offeringsCloseTogetherAreOneVisit() {
        val visits = SanctumJournal.visits(
            listOf(offering("diya", 0), offering("ghanta", 5), offering("diya", 40), offering("aarti", 80)),
        )
        assertEquals(1, visits.size)
        assertEquals(listOf("diya", "ghanta", "aarti"), visits[0].kinds)
        assertEquals(2, visits[0].counts["diya"])
    }

    @Test
    fun aLongGapStartsANewVisitNewestFirst() {
        val visits = SanctumJournal.visits(listOf(offering("diya", 0), offering("pushp", 46)))
        assertEquals(2, visits.size)
        assertEquals("pushp", visits[0].kinds.single())
    }

    @Test
    fun twoHeadsetsAreTwoVisits() {
        val visits = SanctumJournal.visits(listOf(offering("diya", 0), offering("diya", 1, device = "quest-b")))
        assertEquals(2, visits.size)
    }

    @Test
    fun theLastDedicationOfAVisitIsKept() {
        val visit = SanctumJournal.visits(
            listOf(offering("diya", 0, dedicatedTo = "family"), offering("aarti", 3, dedicatedTo = "parents"), offering("ghanta", 4)),
        ).single()
        assertEquals("parents", visit.dedicatedTo)
    }

    @Test
    fun unknownDedicationsAreDropped() {
        val rows = listOf(
            OfferingRow(kind = "diya", deviceId = "q", dedicatedTo = "neighbour", createdAt = "2026-10-27T03:30:00+00:00"),
            OfferingRow(kind = "diya", deviceId = "q", dedicatedTo = "teacher", createdAt = "2026-10-27T03:31:00Z"),
            OfferingRow(kind = "diya", deviceId = "q", createdAt = "not a time"),
        )
        val offerings = SanctumJournal.fromRows(rows)
        assertEquals(2, offerings.size)
        assertNull(offerings[0].dedicatedTo)
        assertEquals("teacher", offerings[1].dedicatedTo)
    }

    @Test
    fun streakCountsDaysInARowEndingTodayOrYesterday() {
        val day = 24 * 60L
        val visits = SanctumJournal.visits(
            listOf(offering("diya", 0), offering("diya", -day), offering("diya", -2 * day), offering("diya", -4 * day)),
        )
        val today = LocalDate.of(2026, 10, 27)
        assertEquals(3, SanctumJournal.streak(visits, today))
        // Not visited yet today: yesterday's run still stands.
        assertEquals(3, SanctumJournal.streak(visits, today.plusDays(1)))
        assertEquals(0, SanctumJournal.streak(visits, today.plusDays(2)))
    }

    @Test
    fun aLateEveningVisitCountsOnTheIndianDay() {
        // 23:50 in India on the 26th is 18:20 UTC: still the 26th, so today (27th) has no visit.
        val evening = SanctumOffering("diya", Instant.parse("2026-10-26T18:20:00Z"), "q")
        val visits = SanctumJournal.visits(listOf(evening))
        assertEquals(LocalDate.of(2026, 10, 26), SanctumJournal.byDay(visits).single().first)
    }

    @Test
    fun monthCountsOnlyThisMonth() {
        val lastMonth = Instant.parse("2026-09-30T10:00:00Z")
        val offerings = listOf(
            offering("diya", 0),
            offering("japa", 2),
            offering("japa", 3),
            offering("diya", 0, base = lastMonth),
        )
        val month = SanctumJournal.month(offerings, SanctumJournal.visits(offerings), LocalDate.of(2026, 10, 27))
        assertEquals(SanctumMonth(visits = 1, diyas = 1, japaRounds = 2), month)
    }
}
