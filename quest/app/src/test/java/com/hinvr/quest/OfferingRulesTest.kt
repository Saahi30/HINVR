package com.hinvr.quest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfferingRulesTest {
    private val stand = Vec(0f, 0.78f, -0.85f)
    private val feet = Vec(0f, 0.82f, -3.85f)
    private val garbha = Vec(0f, 0f, -5.2f)
    private val peti = Vec(1.55f, 0.16f, -1.35f)

    /** Moves from [from] to [to] in [steps] frames of [frameMs]. */
    private fun carry(from: Vec, to: Vec, steps: Int = 10, frameMs: Long = 16, via: Vec? = null): Gesture {
        val gesture = Gesture(from, 0L)
        for (i in 1..steps) {
            val t = i / steps.toFloat()
            val mid = via
            val at = if (mid != null && t <= 0.5f) {
                lerp(from, mid, t * 2f)
            } else if (mid != null) {
                lerp(mid, to, (t - 0.5f) * 2f)
            } else {
                lerp(from, to, t)
            }
            gesture.add(at, i * frameMs * 1_000_000L)
        }
        return gesture
    }

    private fun lerp(a: Vec, b: Vec, t: Float) = Vec(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t)

    @Test
    fun diyaPickedUpAndLetGoInPlaceIsNotLit() {
        val home = Vec(-0.14f, 0.84f, -0.83f)
        val still = carry(home, home)
        assertFalse(OfferingRules.diyaSetDown(still, home, stand))
    }

    @Test
    fun diyaRaisedAndSetDownIsLit() {
        val home = Vec(-0.14f, 0.84f, -0.83f)
        val lifted = carry(home, home, via = Vec(-0.14f, 1.02f, -0.8f))
        assertTrue(OfferingRules.diyaSetDown(lifted, home, stand))
    }

    @Test
    fun flowersTossedTowardTheGarbhaFromTheMandapCount() {
        val start = Vec(-0.28f, 0.83f, -0.77f)
        val release = Vec(-0.2f, 1.2f, -0.95f)
        val toss = carry(start, release, steps = 8)
        val velocity = Vec(0f, 0.1f, -1.4f)
        assertTrue(OfferingRules.offeredToGarbha(toss, release, velocity, feet, garbha))
    }

    @Test
    fun flowersDroppedAtTheStandDoNotCount() {
        val start = Vec(-0.28f, 0.83f, -0.77f)
        val nudge = carry(start, Vec(-0.25f, 0.85f, -0.8f))
        assertFalse(OfferingRules.offeredToGarbha(nudge, Vec(-0.25f, 0.85f, -0.8f), nudge.velocity(), feet, garbha))
    }

    @Test
    fun flowersThrownSidewaysDoNotCount() {
        val start = Vec(-0.28f, 0.83f, -0.77f)
        val release = Vec(0.3f, 1.0f, -0.8f)
        val toss = carry(start, release)
        assertFalse(OfferingRules.offeredToGarbha(toss, release, Vec(1.5f, 0f, 0f), feet, garbha))
    }

    @Test
    fun flowersPlacedAtTheFeetAlwaysCount() {
        val g = carry(feet, feet)
        assertTrue(OfferingRules.offeredToGarbha(g, Vec(0.1f, 0.85f, -3.8f), Vec(0f, 0f, 0f), feet, garbha))
    }

    @Test
    fun coinLetGoOverThePetiAtStandingHeightDropsIn() {
        val start = Vec(0.22f, 0.83f, -0.91f)
        val release = Vec(1.4f, 1.1f, -1.1f)
        val carried = carry(start, release)
        assertTrue(OfferingRules.intoPeti(carried, release, Vec(0f, 0f, 0f), peti))
    }

    @Test
    fun coinNotCarriedDoesNotCount() {
        val at = Vec(1.5f, 0.6f, -1.3f)
        assertFalse(OfferingRules.intoPeti(carry(at, at), at, Vec(0f, 0f, 0f), peti))
    }

    @Test
    fun coinReleasedAwayFromThePetiDoesNotCount() {
        val start = Vec(0.22f, 0.83f, -0.91f)
        val release = Vec(0f, 1.1f, -0.5f)
        assertFalse(OfferingRules.intoPeti(carry(start, release), release, Vec(0f, 0f, 0f), peti))
    }

    @Test
    fun velocityIsMeasuredOverTheLastTenthOfASecond() {
        val g = Gesture(Vec(0f, 0f, 0f), 0L)
        for (i in 1..10) g.add(Vec(0f, 0f, -0.02f * i), i * 20_000_000L)
        assertEquals(-1f, g.velocity().z, 0.01f)
    }

    @Test
    fun sankalpIsClampedToTheServerLimit() {
        assertEquals("short", OfferingRules.clampSankalp("short"))
        assertEquals(500, OfferingRules.clampSankalp("a".repeat(800)).length)
        val emoji = "a".repeat(499) + "🙏"
        assertEquals(499, OfferingRules.clampSankalp(emoji).length)
    }
}
