package com.hinvr.quest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class NavTest {
    private val tour = HomeChoice("Badrinath Temple", "BADRINATH", 0, url = "https://www.youtube.com/embed/VDNIuBQBSmk")
    private val stream = StreamSource("https://www.youtube.com/embed/J0nRdUOQps8", "Kashi", spherical = false, live = LiveState.NotLive)

    @Test
    fun unpairedMenuIsPairing() {
        assertEquals(Stage.Pair, Nav.menu(NavState(), paired = false).stage)
        assertEquals(Stage.Menu, Nav.menu(NavState(), paired = true).stage)
    }

    @Test
    fun mandirNeedsPairing() {
        assertEquals(Stage.Pair, Nav.mandir(NavState(stage = Stage.Menu), paired = false).stage)
    }

    @Test
    fun mandirFromATourDropsTheTour() {
        val inTour = Nav.sphere(Nav.openTour(NavState(stage = Stage.Menu)), tour)
        assertTrue(inTour.overlay is Overlay.Sphere)
        val mandir = Nav.mandir(inTour, paired = true)
        assertEquals(Stage.Mandir, mandir.stage)
        assertEquals(Overlay.None, mandir.overlay)
    }

    @Test
    fun mandirFromAStreamDropsTheStream() {
        val live = NavState(stage = Stage.Live, stateName = "Uttar Pradesh")
        val watching = Nav.watch(live, stream)
        assertTrue(watching.overlay is Overlay.Stream)
        assertEquals(Overlay.None, Nav.mandir(watching, paired = true).overlay)
    }

    @Test
    fun backFromMandirLeavesIt() {
        val back = Nav.back(NavState(stage = Stage.Mandir), paired = true)
        assertEquals(Stage.Menu, back.stage)
    }

    @Test
    fun backFromStreamFadesThenFinishes() {
        val watching = Nav.watch(NavState(stage = Stage.Live), stream)
        val leaving = Nav.back(watching, paired = true)
        assertEquals(true, (leaving.overlay as Overlay.Stream).leaving)
        assertSame(leaving, Nav.back(leaving, paired = true))
        assertEquals(Overlay.None, Nav.finishLeave(leaving).overlay)
        assertEquals(Stage.Live, Nav.finishLeave(leaving).stage)
    }

    @Test
    fun noStreamOverTheMandirOrSplash() {
        val mandir = NavState(stage = Stage.Mandir)
        assertSame(mandir, Nav.watch(mandir, stream))
        val splash = NavState()
        assertSame(splash, Nav.watch(splash, stream))
    }

    @Test
    fun aFailedTourNeverBecomesComplete() {
        val failed = Nav.sphereStatus(Nav.sphere(NavState(stage = Stage.Tour), tour), SphereStatus.Failed)
        val after = Nav.sphereStatus(failed, SphereStatus.Ended)
        assertEquals(SphereStatus.Failed, (after.overlay as Overlay.Sphere).status)
    }

    @Test
    fun backFromLiveReturnsToItsState() {
        val live = Nav.openState(NavState(stage = Stage.States, index = 3), "Gujarat")
        val back = Nav.back(live, paired = true, parentIndex = 3)
        assertEquals(Stage.States, back.stage)
        assertEquals(3, back.index)
    }

    @Test
    fun creditsOpenWithoutPairingAndReturn() {
        val credits = Nav.credits(NavState(stage = Stage.Pair))
        assertEquals(Stage.Credits, credits.stage)
        assertEquals(Stage.Pair, Nav.back(credits, paired = false).stage)
        val fromProfile = Nav.credits(NavState(stage = Stage.Profile))
        assertEquals(Stage.Profile, Nav.back(fromProfile, paired = true).stage)
    }

    @Test
    fun greetingWaitsForWhateverIsPlaying() {
        val watching = Nav.watch(NavState(stage = Stage.Live), stream)
        assertSame(watching, Nav.greet(watching))
        assertEquals(Stage.Splash, Nav.greet(NavState(stage = Stage.Mandir)).stage)
    }

    @Test
    fun handoffComesBackToTheTourList() {
        val away = Nav.handoff(NavState(stage = Stage.Tour, index = 2))
        assertEquals(Overlay.Handoff, away.overlay)
        val back = Nav.returnFromHandoff(away)
        assertEquals(Overlay.None, back.overlay)
        assertEquals(2, back.index)
    }

    @Test
    fun unlinkingStopsEverything() {
        val watching = Nav.watch(NavState(stage = Stage.Live), stream)
        val pair = Nav.requirePairing(watching)
        assertEquals(Stage.Pair, pair.stage)
        assertEquals(Overlay.None, pair.overlay)
    }
}
