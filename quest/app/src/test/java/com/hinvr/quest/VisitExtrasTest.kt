package com.hinvr.quest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VisitExtrasTest {
    // --- First-visit walk-through ---

    @Test
    fun walkThroughFollowsTheDiya() {
        assertEquals(TutorialStep.Light, TutorialRules.next(TutorialStep.Take, holdingDiya = true, diyaLit = false, inStepMs = 1_000))
        assertEquals(TutorialStep.Done, TutorialRules.next(TutorialStep.Light, holdingDiya = true, diyaLit = true, inStepMs = 1_000))
        assertEquals(TutorialStep.Off, TutorialRules.next(TutorialStep.Done, holdingDiya = false, diyaLit = true, inStepMs = TutorialRules.DoneMs + 1))
    }

    @Test
    fun puttingTheDiyaDownUnlitGoesBackAStep() {
        assertEquals(TutorialStep.Take, TutorialRules.next(TutorialStep.Light, holdingDiya = false, diyaLit = false, inStepMs = 500))
    }

    @Test
    fun walkThroughEndsQuietlyWhenLeftAlone() {
        assertEquals(TutorialStep.Off, TutorialRules.next(TutorialStep.Take, false, false, TutorialRules.StepTimeoutMs + 1))
        assertEquals(TutorialStep.Take, TutorialRules.next(TutorialStep.Take, false, false, 10_000))
        assertEquals(TutorialStep.Off, TutorialRules.next(TutorialStep.Off, true, true, 0))
    }

    @Test
    fun aDiyaLitAnotherWaySkipsToTheEnd() {
        assertEquals(TutorialStep.Done, TutorialRules.next(TutorialStep.Take, holdingDiya = false, diyaLit = true, inStepMs = 0))
    }

    // --- Cast tip ---

    @Test
    fun castTipWaitsForTwentySecondsOfDarshan() {
        assertFalse(CastTipRules.shouldShow(1, -1, 0, false, 19_999))
        assertTrue(CastTipRules.shouldShow(1, -1, 0, false, CastTipRules.MinWatchMs))
    }

    @Test
    fun castTipIsOncePerSessionAndEveryFewSessions() {
        assertFalse(CastTipRules.shouldShow(5, -1, 0, shownThisSession = true, watchedMs = 60_000))
        assertFalse(CastTipRules.shouldShow(5, 3, 1, false, 60_000))
        assertTrue(CastTipRules.shouldShow(6, 3, 1, false, 60_000))
        assertFalse(CastTipRules.shouldShow(40, 3, CastTipRules.MaxTimes, false, 60_000))
    }

    // --- Crash reports ---

    @Test
    fun crashTraceIsTrimmedAndKeepsItsCause() {
        val deep = RuntimeException("outer", IllegalStateException("the real reason"))
        val report = CrashFormat.of(deep, "0.1.0", 1_700_000_000_000L)
        assertEquals("java.lang.RuntimeException", report.kind)
        assertTrue(report.trace.contains("the real reason"))
        assertTrue(report.trace.length <= CrashFormat.TraceLimit)
    }

    @Test
    fun longTextIsCutOnALineAndMarked() {
        val text = (1..2_000).joinToString("\n") { "\tat com.hinvr.quest.Frame$it(Frame.kt:$it)" }
        val cut = CrashFormat.trim(text, CrashFormat.TraceLimit)
        assertTrue(cut.length <= CrashFormat.TraceLimit)
        assertTrue(cut.endsWith("\n…"))
        assertEquals("short", CrashFormat.trim("short", 10))
    }

    @Test
    fun crashFileRoundTrips() {
        val report = CrashReport("0.1.0", "java.lang.NullPointerException", "boom", "trace", 42L)
        assertEquals(report, CrashFormat.fromJson(CrashFormat.toJson(report)))
        assertEquals(null, CrashFormat.fromJson("not json"))
    }

    // --- Family in the sankalp ---

    private val amma = FamilyMember("a1", "Amma", "mother")
    private val ravi = FamilyMember("r2", "Ravi", "son")

    @Test
    fun forCyclesThePresetsThenTheFamilyByName() {
        val all = DedicationChoices.all(listOf(amma, ravi))
        assertEquals(Dedication.entries.size + 2, all.size)
        val last = DedicationChoice(Dedication.Teacher)
        val next = DedicationChoices.next(last, listOf(amma, ravi))
        assertEquals(amma, next.member)
        assertEquals(Dedication.Family, next.preset)
        assertEquals(DedicationChoice(Dedication.Myself), DedicationChoices.next(DedicationChoice(Dedication.Family, ravi), listOf(amma, ravi)))
    }

    @Test
    fun withoutFamilyTheChoicesAreUnchanged() {
        assertEquals(Dedication.entries.map { DedicationChoice(it) }, DedicationChoices.all(emptyList()))
        // Someone removed from the family: the cycle starts again from the presets.
        assertNotNull(DedicationChoices.next(DedicationChoice(Dedication.Family, amma), emptyList()))
    }
}
