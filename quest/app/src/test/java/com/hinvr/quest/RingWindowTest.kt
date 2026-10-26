package com.hinvr.quest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RingWindowTest {
    @Test
    fun smallDeckShowsEveryCard() {
        assertEquals((0 until 5).toSet(), RingWindow.wanted(5, 16, facingDeg = 120f))
        assertTrue(RingWindow.wanted(0, 16, 0f).isEmpty())
    }

    @Test
    fun bigDeckShowsSixteenAroundTheFacing() {
        val wanted = RingWindow.wanted(30, 16, facingDeg = 0f)
        assertEquals(16, wanted.size)
        assertTrue(0 in wanted)
        assertTrue(29 in wanted && 22 in wanted && 7 in wanted)
        assertFalse(15 in wanted)
    }

    @Test
    fun windowWrapsWhenFacingBehind() {
        val wanted = RingWindow.wanted(30, 16, facingDeg = 180f)
        assertTrue(15 in wanted)
        assertFalse(0 in wanted)
    }

    @Test
    fun assignKeepsCardsOnTheirPanels() {
        val shown = MutableList(4) { -1 }
        assertTrue(RingWindow.assign(shown, setOf(0, 1, 2)))
        val before = shown.toList()
        assertFalse(RingWindow.assign(shown, setOf(0, 1, 2)))
        assertEquals(before, shown)
        assertTrue(RingWindow.assign(shown, setOf(1, 2, 3)))
        assertEquals(before.indexOf(1), shown.indexOf(1))
        assertEquals(before.indexOf(2), shown.indexOf(2))
        assertFalse(0 in shown)
        assertTrue(3 in shown)
    }

    @Test
    fun assignClearsPanelsNoLongerWanted() {
        val shown = mutableListOf(4, 5, 6, 7)
        assertTrue(RingWindow.assign(shown, setOf(5)))
        assertEquals(listOf(-1, 5, -1, -1), shown)
    }

    @Test
    fun arcCardsAreCentred() {
        assertEquals(-38f, RingLayout.cardYaw(0, 3), 1e-4f)
        assertEquals(0f, RingLayout.cardYaw(1, 3), 1e-4f)
        assertEquals(90f, RingLayout.cardYaw(3, 12), 1e-4f)
    }
}
