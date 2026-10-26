package com.hinvr.quest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfferingRulesTest {
    private val stand = Vec(0f, 0.82f, -0.55f)
    private val jyoti = Vec(-0.47f, 0.985f, -0.69f)
    private val feet = Vec(0f, 0.455f, -3.15f)
    private val garbha = Vec(0f, 0f, -5.2f)
    private val peti = Vec(0.98f, 0.8f, -0.62f)

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
    fun diyaWickAtTheJyotiFlameCatches() {
        assertTrue(OfferingRules.touchesFlame(Vec(-0.44f, 0.99f, -0.66f), jyoti))
    }

    @Test
    fun diyaHeldBesideTheJyotiDoesNotCatch() {
        assertFalse(OfferingRules.touchesFlame(Vec(-0.25f, 0.99f, -0.6f), jyoti))
    }

    @Test
    fun propLetGoJustOverTheStandIsSetDownThere() {
        assertTrue(OfferingRules.overStand(Vec(0.3f, 0.95f, -0.5f), stand))
        assertFalse(OfferingRules.overStand(Vec(0.8f, 0.95f, -0.5f), stand))
        assertFalse(OfferingRules.overStand(Vec(0f, 1.4f, -0.5f), stand))
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
        assertTrue(OfferingRules.offeredToGarbha(g, Vec(0.1f, 0.55f, -3.1f), Vec(0f, 0f, 0f), feet, garbha))
    }

    @Test
    fun coinLetGoOverThePetiAtStandingHeightDropsIn() {
        val start = Vec(0.4f, 0.83f, -0.45f)
        val release = Vec(1.0f, 1.1f, -0.55f)
        val carried = carry(start, release)
        assertTrue(OfferingRules.intoPeti(carried, release, Vec(0f, 0f, 0f), peti))
    }

    @Test
    fun coinNotCarriedDoesNotCount() {
        val at = Vec(0.98f, 0.9f, -0.62f)
        assertFalse(OfferingRules.intoPeti(carry(at, at), at, Vec(0f, 0f, 0f), peti))
    }

    @Test
    fun coinReleasedAwayFromThePetiDoesNotCount() {
        val start = Vec(0.4f, 0.83f, -0.45f)
        val release = Vec(-0.2f, 1.1f, -0.2f)
        assertFalse(OfferingRules.intoPeti(carry(start, release), release, Vec(0f, 0f, 0f), peti))
    }

    @Test
    fun velocityIsMeasuredOverTheLastTenthOfASecond() {
        val g = Gesture(Vec(0f, 0f, 0f), 0L)
        for (i in 1..10) g.add(Vec(0f, 0f, -0.02f * i), i * 20_000_000L)
        assertEquals(-1f, g.velocity().z, 0.01f)
    }

    @Test
    fun rayThroughAPropAimsAtIt() {
        val head = Vec(0.1f, 1.3f, 0.1f)
        val diya = Vec(-0.2f, 0.86f, -0.45f)
        val dir = (diya - head) * (1f / (diya - head).length())
        val dead = OfferingRules.rayAim(head, dir, diya)
        assertEquals(0f, dead ?: 1f, 0.01f)
    }

    @Test
    fun rayPassingWideOfAPropMisses() {
        val origin = Vec(0f, 1.2f, 0f)
        val dir = Vec(0f, 0f, -1f)
        assertNull(OfferingRules.rayAim(origin, dir, Vec(0.2f, 1.2f, -1f)))
        assertNull(OfferingRules.rayAim(origin, dir, Vec(0f, 1.2f, 1f)))
        assertNull(OfferingRules.rayAim(origin, dir, Vec(0f, 1.2f, -3.5f)))
    }

    @Test
    fun guideSuggestsTheFirstStepNotDone() {
        assertEquals(OfferingKind.Diya, OfferingRules.nextStep(emptyList()))
        assertEquals(OfferingKind.Pushp, OfferingRules.nextStep(listOf(OfferingKind.Diya, OfferingKind.Agarbatti, OfferingKind.Ghanta)))
        assertEquals(OfferingKind.Aarti, OfferingRules.nextStep(OfferingRules.PujaOrder.dropLast(1)))
        assertNull(OfferingRules.nextStep(OfferingRules.PujaOrder))
    }

    @Test
    fun sevenCirclesOfTheThaliCountSeven() {
        // The first part of a turn finds the centre, so seven and a half turns count seven.
        assertEquals(7, circlesFor(turns = 7.5f))
        assertEquals(6, circlesFor(turns = 6.5f))
    }

    /** [turns] circles at a turn a second, 12 cm across, somewhere in front of the member. */
    private fun circlesFor(turns: Float): Int {
        val counter = AartiCircles()
        val dt = 1f / 72f
        var count = 0
        for (i in 0..(turns / dt).toInt()) {
            val a = i * dt * 2f * Math.PI.toFloat()
            count = counter.add(0.1f + 0.12f * kotlin.math.cos(a), 1.2f + 0.12f * kotlin.math.sin(a), dt)
        }
        return count
    }

    @Test
    fun jitterIsNotAnAarti() {
        val counter = AartiCircles()
        val random = java.util.Random(7)
        var count = 0
        repeat(2_000) {
            count = counter.add(0.1f + random.nextFloat() * 0.03f, 1.2f + random.nextFloat() * 0.03f, 1f / 72f)
        }
        assertEquals(0, count)
    }

    @Test
    fun aLineBackAndForthIsNotAnAarti() {
        val counter = AartiCircles()
        val dt = 1f / 72f
        var count = 0
        for (i in 0..(10f / dt).toInt()) {
            count = counter.add(0.25f * kotlin.math.sin(i * dt * 2f * Math.PI.toFloat()), 1.2f, dt)
        }
        assertEquals(0, count)
    }

    @Test
    fun aRungBellSwingsNearItsAmplitudeAndSettles() {
        val swing = BellSwing(length = 3.0f)
        swing.kick(1f, 0f, amplitudeDeg = 6f)
        var peak = 0f
        repeat(72 * 3) {
            swing.step(1f / 72f)
            peak = maxOf(peak, swing.angleDeg)
        }
        assertTrue("peak $peak", peak in 4f..6.5f)
        repeat(72 * 30) { swing.step(1f / 72f) }
        assertTrue(swing.angleDeg < 0.2f)
    }

    @Test
    fun ringingAgainNeverSwingsPastTheCap() {
        val swing = BellSwing(length = 3.0f)
        repeat(10) { swing.kick(0f, -1f) }
        var peak = 0f
        repeat(72 * 3) {
            swing.step(1f / 72f)
            peak = maxOf(peak, swing.angleDeg)
        }
        assertTrue("peak $peak", peak <= BellSwing.MaxDeg + 0.01f)
    }

    @Test
    fun sankalpIsClampedToTheServerLimit() {
        assertEquals("short", OfferingRules.clampSankalp("short"))
        assertEquals(500, OfferingRules.clampSankalp("a".repeat(800)).length)
        val emoji = "a".repeat(499) + "🙏"
        assertEquals(499, OfferingRules.clampSankalp(emoji).length)
    }
}
