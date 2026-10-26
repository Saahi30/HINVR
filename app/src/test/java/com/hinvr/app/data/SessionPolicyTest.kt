package com.hinvr.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionPolicyTest {

    private val user = RemoteUser(id = "u1", phone = "", email = "a@b.c", displayName = "Asha", profile = null)

    @Test
    fun offlineKeepsEverything() {
        assertEquals(SyncAction.KeepAll, syncAction(RemoteSync.Offline, locallySignedIn = true))
    }

    @Test
    fun profileReadFailureKeepsTierAndPass() {
        val partial = RemoteSync.SessionOnly(id = "u1", email = "a@b.c", phone = "")
        assertEquals(SyncAction.ApplyIdentity, syncAction(partial, locallySignedIn = true))
    }

    @Test
    fun freshProfileIsApplied() {
        assertEquals(SyncAction.ApplyProfile, syncAction(RemoteSync.Fresh(user), locallySignedIn = true))
    }

    @Test
    fun onlyARejectedSessionSignsOut() {
        assertEquals(SyncAction.ClearSession, syncAction(RemoteSync.SignedOut, locallySignedIn = true))
        assertEquals(SyncAction.KeepAll, syncAction(RemoteSync.SignedOut, locallySignedIn = false))
    }

    @Test
    fun passSurvivesSameCredential() {
        assertTrue(passSurvivesProfile("Gold", "HNV-123", "Gold", "HNV-123"))
        assertTrue(passSurvivesProfile("Platinum", " HNV-9 ", "platinum", "HNV-9"))
    }

    @Test
    fun passDropsWhenTierOrIdChanges() {
        assertFalse(passSurvivesProfile("Gold", "HNV-123", "Platinum", "HNV-123"))
        assertFalse(passSurvivesProfile("Gold", "HNV-123", "Gold", "HNV-456"))
        assertFalse(passSurvivesProfile("Gold", "HNV-123", "Darshan", "HNV-123"))
        assertFalse(passSurvivesProfile("Gold", "HNV-123", "", "HNV-123"))
    }

    @Test
    fun unknownTierIsNone() {
        assertEquals(MembershipTier.None, tierOf(null))
        assertEquals(MembershipTier.None, tierOf("Diamond"))
        assertEquals(MembershipTier.Nri, tierOf("NRI"))
    }

    @Test
    fun reissueWhenMissingExpiredOrUnderThreeHours() {
        val now = 1_000_000_000L
        val hour = 60 * 60 * 1000L
        assertTrue(passNeedsReissue("", 0, now))
        assertTrue(passNeedsReissue("bogus", now + 6 * hour, now))
        assertTrue(passNeedsReissue("HNV1.x", now - 1, now))
        assertTrue(passNeedsReissue("HNV1.x", now + 2 * hour, now))
        assertFalse(passNeedsReissue("HNV1.x", now + 5 * hour, now))
        assertFalse(passNeedsReissue("HNV1.x", now + 3 * hour, now))
    }

    @Test
    fun aCodeNearExpiryIsStillUsableOffline() {
        val now = 5_000L
        assertTrue(passIsUsable("HNV1.x", now + 60_000, now))
        assertFalse(passIsUsable("HNV1.x", now, now))
    }
}
