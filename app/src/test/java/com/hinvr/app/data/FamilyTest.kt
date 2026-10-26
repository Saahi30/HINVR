package com.hinvr.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyTest {
    private fun member(id: String, at: String) = FamilyMemberRow(id, id, "mother", at)

    @Test
    fun serverLimitsWinOverDefaults() {
        assertEquals(3, Family.limitFor(MembershipTier.Gold, mapOf("Gold" to 3)))
        assertEquals(2, Family.limitFor(MembershipTier.Gold, null))
        assertEquals(0, Family.limitFor(MembershipTier.Darshan, emptyMap()))
        assertEquals(4, Family.limitFor(MembershipTier.Nri, mapOf("Gold" to 3)))
    }

    @Test
    fun canAddUntilTheLimit() {
        assertTrue(Family.canAdd(1, 2))
        assertFalse(Family.canAdd(2, 2))
        assertFalse(Family.canAdd(0, 0))
    }

    @Test
    fun coveredAreTheFirstAddedAfterADowngrade() {
        val list = listOf(member("c", "2026-03-01"), member("a", "2026-01-01"), member("b", "2026-02-01"))
        assertEquals(listOf("a", "b"), Family.covered(list, 2).map { it.id })
        assertEquals(emptyList<String>(), Family.covered(list, 0).map { it.id })
    }

    @Test
    fun relationsReadTheWireAndFallBackToFamily() {
        assertEquals(Relation.Grandmother, Relation.of("grandmother"))
        assertEquals(Relation.Other, Relation.of("cousin"))
    }

    @Test
    fun namesAreTrimmedAndCapped() {
        assertEquals("Amma Devi", Family.cleanName("  Amma   Devi "))
        assertEquals(Family.NameMax, Family.cleanName("x".repeat(80)).length)
    }
}
