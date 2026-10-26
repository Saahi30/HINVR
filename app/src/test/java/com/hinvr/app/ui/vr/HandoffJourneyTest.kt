package com.hinvr.app.ui.vr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HandoffJourneyTest {
    @Test
    fun barMovesQuicklyThenWaitsForTheHeadset() {
        val atEight = handoffProgress(8f, opened = false)
        assertTrue("about three quarters at 8 s: $atEight", atEight in 0.65f..0.8f)
        assertTrue(handoffProgress(600f, opened = false) < 0.93f)
    }

    @Test
    fun barFillsWhenOpened() {
        assertEquals(1f, handoffProgress(2f, opened = true), 0f)
    }
}
