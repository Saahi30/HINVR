package com.hinvr.quest

import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SanctumFeaturesTest {
    // --- Japa ---

    @Test
    fun japaFeelsEachQuarterAndCompletesARoundAt108() {
        val japa = JapaCounter()
        val beads = List(108) { japa.advance() }
        assertEquals(JapaCounter.Bead.Bead, beads[0])
        assertEquals(JapaCounter.Bead.Quarter, beads[26])
        assertEquals(JapaCounter.Bead.Quarter, beads[80])
        assertEquals(3, beads.count { it == JapaCounter.Bead.Quarter })
        assertEquals(JapaCounter.Bead.Round, beads[107])
        assertEquals(0, japa.count)
        assertEquals(1, japa.rounds)
    }

    @Test
    fun japaKeepsCountingIntoASecondRound() {
        val japa = JapaCounter()
        repeat(108 + 10) { japa.advance() }
        assertEquals(10, japa.count)
        assertEquals(1, japa.rounds)
        japa.reset()
        assertEquals(0, japa.count)
        assertEquals(0, japa.rounds)
    }

    // --- Live aarti ---

    private fun ist(hour: Int, minute: Int): Long =
        OffsetDateTime.of(2026, 10, 27, hour, minute, 0, 0, ZoneOffset.ofHoursMinutes(5, 30)).toInstant().toEpochMilli()

    private fun card(times: List<String>, live: LiveState = LiveState.Live, url: String = "https://youtube.com/live/x") =
        RingCard("kashi", "Kashi Vishwanath", "VARANASI", null, liveUrl = url, live = live, aartiTimes = AartiSync.parseTimes(times))

    @Test
    fun aartiTimesParseLooselyAndDropNonsense() {
        assertEquals(listOf(6 * 60 + 30, 18 * 60), AartiSync.parseTimes(listOf("18:00", " 6:30", "25:00", "noon", "06:30")))
    }

    @Test
    fun indiaTimeIsUsedWhateverTheHeadsetZone() {
        val noonUtc = OffsetDateTime.of(2026, 10, 27, 12, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals(17 * 60 + 30, AartiSync.minutesInIndia(noonUtc))
    }

    @Test
    fun theAartiWindowOpensAtTheStartAndClosesFortyMinutesLater() {
        val start = 19 * 60
        assertFalse(AartiSync.inWindow(start - 1, start))
        assertTrue(AartiSync.inWindow(start, start))
        assertTrue(AartiSync.inWindow(start + 39, start))
        assertFalse(AartiSync.inWindow(start + 40, start))
    }

    @Test
    fun aLateAartiRunsPastMidnight() {
        val start = 23 * 60 + 50
        assertTrue(AartiSync.inWindow(5, start))
        assertTrue(AartiSync.inWindow(29, start))
        assertFalse(AartiSync.inWindow(30, start))
    }

    @Test
    fun onlyALiveOfficialStreamDuringAnAartiCounts() {
        val during = ist(19, 10)
        assertEquals("kashi", AartiSync.liveNow(listOf(card(listOf("19:00"))), during)?.mandirId)
        assertNull(AartiSync.liveNow(listOf(card(listOf("19:00"))), ist(20, 0)))
        assertNull(AartiSync.liveNow(listOf(card(listOf("19:00"), live = LiveState.NotLive)), during))
        assertNull(AartiSync.liveNow(listOf(card(listOf("19:00"), url = "")), during))
        assertNull(AartiSync.liveNow(listOf(card(emptyList())), during))
    }

    // --- Festivals ---

    @Test
    fun aFestivalParsesFromTheServer() {
        val festival = Festival.parse("""{"id":"diwali-2026","kind":"deepmala","name":"Diwali","name_hi":"दीपावली"}""")
        assertEquals(FestivalKind.Deepmala, festival?.kind)
        assertEquals("दीपावली", festival?.title(hindi = true))
        assertEquals("Diwali", festival?.title(hindi = false))
    }

    @Test
    fun noFestivalOrAnUnknownOneIsNone() {
        assertNull(Festival.parse("null"))
        assertNull(Festival.parse(""))
        assertNull(Festival.parse("""{"id":"x","kind":"rangoli","name":"Something"}"""))
        assertNull(Festival.parse("""{"id":"x","kind":"modak"}"""))
    }

    @Test
    fun aMissingHindiNameFallsBackToEnglish() {
        val festival = Festival.parse("""{"id":"g","kind":"modak","name":"Ganesh Chaturthi","name_hi":null}""")
        assertEquals("Ganesh Chaturthi", festival?.title(hindi = true))
    }

    @Test
    fun eachFestivalRecordsAKindTheServerAllows() {
        FestivalKind.entries.forEach { kind ->
            // The server's list (supabase record_offering), spelled out so the test needs no Android.
            assertTrue(kind.records.label in setOf("ghanta", "diya", "aarti", "pushp", "agarbatti", "prasad", "tilak", "daan", "japa", "abhishek", "deepmala"))
            assertEquals(kind, FestivalKind.from(kind.wire.uppercase()))
        }
    }

    @Test
    fun theFestivalStepComesJustBeforeAarti() {
        val steps = OfferingRules.steps(festival = true)
        assertEquals(9, steps.size)
        assertEquals(OfferingRules.Step.Festival, steps[7])
        assertEquals(OfferingRules.Step.Offering(OfferingKind.Aarti), steps.last())
        assertEquals(8, OfferingRules.steps(festival = false).size)
    }

    @Test
    fun theGuideSuggestsTheFestivalOnceTheSevenAreDone() {
        val seven = OfferingRules.PujaOrder.dropLast(1)
        assertEquals(OfferingRules.Step.Festival, OfferingRules.nextStep(seven, festival = true, festivalDone = false))
        assertEquals(
            OfferingRules.Step.Offering(OfferingKind.Aarti),
            OfferingRules.nextStep(seven, festival = true, festivalDone = true),
        )
        assertEquals(
            OfferingRules.Step.Offering(OfferingKind.Diya),
            OfferingRules.nextStep(emptyList(), festival = true, festivalDone = true),
        )
        assertNull(OfferingRules.nextStep(OfferingRules.PujaOrder, festival = true, festivalDone = true))
    }

    // --- Abhishek ---

    @Test
    fun aLotaTippedTowardTheGarbhaPours() {
        val toGarbha = Vec(0f, -1f, -3f)
        // Tipped 80° forward, toward -Z.
        val tipped = Vec(0f, 0.17f, -0.98f)
        assertTrue(OfferingRules.pours(tipped, toGarbha, atThreshold = false))
        // Upright, or tipped sideways away from the garbha, does not.
        assertFalse(OfferingRules.pours(Vec(0f, 1f, 0f), toGarbha, atThreshold = true))
        assertFalse(OfferingRules.pours(Vec(0.98f, 0.17f, 0f), toGarbha, atThreshold = false))
        // At the threshold any good tip pours: the lingam is right ahead.
        assertTrue(OfferingRules.pours(Vec(0.98f, 0.17f, 0f), toGarbha, atThreshold = true))
    }

    // --- Dedication ---

    @Test
    fun dedicationsSendTheServersKeysAndCycleRound() {
        assertEquals(
            listOf("self", "family", "parents", "children", "ancestors", "friend", "teacher"),
            Dedication.entries.map { it.wire },
        )
        assertEquals(Dedication.Family, Dedication.Myself.next())
        assertEquals(Dedication.Myself, Dedication.Teacher.next())
        assertEquals(Dedication.Teacher, Dedication.Myself.next(-1))
    }

    // --- Day and the aarti moment ---

    @Test
    fun theRoomIsInDaylightFromHalfPastFiveToHalfPastFive() {
        assertFalse(isDaytime(LocalTime.of(5, 29)))
        assertTrue(isDaytime(LocalTime.of(5, 30)))
        assertTrue(isDaytime(LocalTime.of(17, 29)))
        assertFalse(isDaytime(LocalTime.of(17, 30)))
        assertFalse(isDaytime(LocalTime.of(0, 0)))
    }

    @Test
    fun theAartiMomentRisesHoldsAndFades() {
        assertEquals(0f, celebrationLevel(-1L), 1e-6f)
        assertEquals(0.5f, celebrationLevel(500L), 1e-6f)
        assertEquals(1f, celebrationLevel(3_000L), 1e-6f)
        assertEquals(0.5f, celebrationLevel(5_000L), 1e-6f)
        assertEquals(0f, celebrationLevel(6_000L), 1e-6f)
    }

    @Test
    fun flamesFlickerGentlyAndOutOfStep() {
        val samples = (0 until 200).map { Flicker.at(it / 60f, 1.3f) }
        assertTrue(samples.all { it in 0.88f..1.12f })
        assertTrue(Flicker.at(1f, 0.2f) != Flicker.at(1f, 2.9f))
    }
}
