package com.hinvr.quest

import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatTest {
    @Test
    fun validUntilReadsTheNewSqlDate() {
        assertEquals("30 Apr 2027", MemberFormat.validUntil("2027-04-30"))
        assertEquals("30 Apr 2027", MemberFormat.validUntil("2027-04-30T00:00:00+05:30"))
    }

    @Test
    fun validUntilKeepsOldFreeText() {
        assertEquals("March 2027", MemberFormat.validUntil("March 2027"))
        assertEquals("—", MemberFormat.validUntil(""))
        assertEquals("—", MemberFormat.validUntil("null"))
    }

    @Test
    fun pairedSince() {
        assertEquals("1 Oct 2026", MemberFormat.shortDate("2026-10-01T10:00:00+00:00", ZoneId.of("UTC")))
        assertEquals("—", MemberFormat.shortDate("", ZoneId.of("UTC")))
    }

    @Test
    fun photoCreditsParse() {
        val rows = PhotoCredits.parse(
            """
            [
              {"resource":"temple_kashi","title":"Kashi Vishwanath","author":"A. Person","license":"CC BY-SA 4.0",
               "licenseUrl":"https://creativecommons.org/licenses/by-sa/4.0/","sourceUrl":"https://commons.wikimedia.org/x","modified":"cropped"},
              {"resource":"temple_somnath","title":"Somnath","author":"B","license":"CC0","modified":false},
              {"resource":"blank"}
            ]
            """.trimIndent(),
        )
        assertEquals(2, rows.size)
        assertTrue(rows[0].modified)
        assertEquals(false, rows[1].modified)
        assertEquals("CC0", rows[1].license)
    }

    @Test
    fun allowedOfferingKindsMatchTheServer() {
        OfferingKind.entries.forEach { assertTrue(it.label in MandirOfferings.AllowedKinds) }
    }
}
