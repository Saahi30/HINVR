package com.hinvr.app.data

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VrHandoffTest {
    private val now = Instant.parse("2026-10-27T10:00:00Z")
    private val later = now.plusSeconds(300)

    @Test
    fun tourBeatsStreamAndNothingMeansNoButton() {
        assertEquals(VrTarget.Tour, VrHandoffs.targetFor(hasTour = true, hasStream = true))
        assertEquals(VrTarget.Live, VrHandoffs.targetFor(hasTour = false, hasStream = true))
        assertNull(VrHandoffs.targetFor(hasTour = false, hasStream = false))
    }

    @Test
    fun claimedShowsTheHeadset() {
        val phase = VrHandoffs.phaseFor(VrHandoffStatus(claimedAt = "2026-10-27T10:00:05Z", deviceName = "Quest 3"), later, now)
        assertEquals(HandoffPhase.Opened("Quest 3"), phase)
        assertTrue(VrHandoffs.settled(phase))
    }

    @Test
    fun unclaimedWaitsUntilTheDeadline() {
        assertEquals(HandoffPhase.Waiting, VrHandoffs.phaseFor(VrHandoffStatus(), later, now))
        assertEquals(HandoffPhase.Waiting, VrHandoffs.phaseFor(null, later, now))
        assertEquals(HandoffPhase.Expired, VrHandoffs.phaseFor(null, now, now))
        assertEquals(HandoffPhase.Expired, VrHandoffs.phaseFor(VrHandoffStatus(expired = true), later, now))
        assertFalse(VrHandoffs.settled(HandoffPhase.Waiting))
    }

    @Test
    fun readsServerTimestamps() {
        assertEquals(Instant.parse("2026-10-27T10:10:00Z"), VrHandoffs.parseInstant("2026-10-27T10:10:00+00:00"))
        assertNull(VrHandoffs.parseInstant("soon"))
    }
}
