package com.hinvr.app.data

import java.time.Instant
import java.time.LocalTime
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AartiScheduleTest {
    private fun ist(text: String): Instant = ZonedDateTime.parse("${text}+05:30[Asia/Kolkata]").toInstant()

    @Test
    fun parsesOnlyWellFormedTimesSortedOnce() {
        assertEquals(
            listOf(LocalTime.of(5, 0), LocalTime.of(19, 0)),
            AartiSchedule.parse(listOf("19:00", "05:00", "19:00", "7pm", "24:00", " 05:00 ")),
        )
    }

    @Test
    fun nextIsTodaysAartiWhenItsReminderIsAhead() {
        val next = AartiSchedule.next(listOf(LocalTime.of(19, 0)), 15, ist("2026-10-27T18:00:00"))!!
        assertEquals(ist("2026-10-27T19:00:00"), next.start)
        assertEquals(ist("2026-10-27T18:45:00"), next.remindAt)
    }

    @Test
    fun aReminderAlreadyPassedMovesToTheNextAarti() {
        // 18:50: the 19:00 reminder (18:45) has gone; next is tomorrow's 05:00.
        val next = AartiSchedule.next(listOf(LocalTime.of(5, 0), LocalTime.of(19, 0)), 15, ist("2026-10-27T18:50:00"))!!
        assertEquals(ist("2026-10-28T05:00:00"), next.start)
    }

    @Test
    fun leadCanCrossMidnight() {
        // A 00:10 aarti with a 30 minute lead is reminded at 23:40 the evening before.
        val next = AartiSchedule.next(listOf(LocalTime.of(0, 10)), 30, ist("2026-10-27T23:00:00"))!!
        assertEquals(ist("2026-10-28T00:10:00"), next.start)
        assertEquals(ist("2026-10-27T23:40:00"), next.remindAt)
    }

    @Test
    fun worksFromAPhoneOutsideIndia() {
        // 13:00 in London (UTC) is 18:30 in India: the 19:00 aarti, reminded at 18:50 India time.
        val now = Instant.parse("2026-10-27T13:00:00Z")
        val next = AartiSchedule.next(listOf(LocalTime.of(19, 0)), 10, now)!!
        assertEquals(Instant.parse("2026-10-27T13:30:00Z"), next.start)
        assertEquals(Instant.parse("2026-10-27T13:20:00Z"), next.remindAt)
    }

    @Test
    fun noTimesNoReminder() {
        assertNull(AartiSchedule.next(emptyList(), 15, Instant.now()))
    }

    @Test
    fun minutesRoundToTheNearestAndNeverGoNegative() {
        val start = ist("2026-10-27T19:00:00")
        assertEquals(15, AartiSchedule.minutesUntil(start, ist("2026-10-27T18:45:10")))
        assertEquals(0, AartiSchedule.minutesUntil(start, ist("2026-10-27T19:01:00")))
    }
}
