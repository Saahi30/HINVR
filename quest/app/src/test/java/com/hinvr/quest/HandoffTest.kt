package com.hinvr.quest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HandoffTest {
    private val kashi = RingCard("kashi", "Kashi Vishwanath", "VARANASI", null, liveUrl = "https://www.youtube.com/embed/x")
    private val shirdi = RingCard("shirdi", "Shirdi Sai Baba", "SHIRDI", null)
    private val badrinath = RingCard("badrinath", "Badrinath", "BADRINATH", null)
    private val mandirs = listOf(kashi, shirdi, badrinath)
    private val walk = HomeChoice("Badrinath Temple", "BADRINATH", 0, url = "u", mandirId = "badrinath")
    private val tours = listOf(walk)

    private fun ask(target: String, mandir: String?) = HandoffRules.action(HandoffRequest("1", target, mandir), mandirs, tours)

    @Test
    fun liveOpensTheStreamOrFallsBackToTheWalk() {
        assertEquals(HandoffAction.Live(kashi), ask("live", "kashi"))
        assertEquals(HandoffAction.Tour(walk), ask("live", "badrinath"))
        assertEquals(HandoffAction.Unavailable("Shirdi Sai Baba"), ask("live", "shirdi"))
    }

    @Test
    fun tourOpensTheWalkOrFallsBackToTheStream() {
        assertEquals(HandoffAction.Tour(walk), ask("tour", "badrinath"))
        assertEquals(HandoffAction.Live(kashi), ask("tour", "kashi"))
        assertEquals(HandoffAction.Unavailable(""), ask("tour", "nowhere"))
    }

    @Test
    fun sanctumTakesTheDarshanOnlyWhenItStreams() {
        assertEquals(HandoffAction.Sanctum("kashi"), ask("sanctum", "kashi"))
        assertEquals(HandoffAction.Sanctum(null), ask("sanctum", "shirdi"))
        assertEquals(HandoffAction.Sanctum(null), ask("sanctum", null))
    }

    @Test
    fun waitsOutTheGreetingAndPairing() {
        assertFalse(HandoffRules.canApply(Stage.Splash))
        assertFalse(HandoffRules.canApply(Stage.Pair))
        assertTrue(HandoffRules.canApply(Stage.Menu))
        assertTrue(HandoffRules.canApply(Stage.Mandir))
    }

    @Test
    fun parsesTheServerAnswer() {
        assertNull(HandoffRules.parse("null"))
        assertNull(HandoffRules.parse(""))
        assertEquals(
            HandoffRequest("a1", "live", "kashi"),
            HandoffRules.parse("""{"id":"a1","target":"live","mandir_id":"kashi"}"""),
        )
        assertEquals(HandoffRequest("a2", "sanctum", null), HandoffRules.parse("""{"id":"a2","target":"sanctum","mandir_id":null}"""))
    }
}
