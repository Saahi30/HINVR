package com.hinvr.app.data

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormattingTest {

    @Test
    fun isoDateBecomesMonthAndYear() {
        assertEquals("Apr 2027", formatValidUntil("2027-04-30", Locale.ENGLISH))
        assertEquals("Apr 2027", formatValidUntil("2027-04-30T00:00:00+00:00", Locale.ENGLISH))
    }

    @Test
    fun hindiMonthIsLocalised() {
        val hindi = formatValidUntil("2027-04-30", Locale.forLanguageTag("hi"))
        assertTrue(hindi.contains("2027"))
        assertNotEquals("Apr 2027", hindi)
    }

    @Test
    fun oldFreeTextLabelShowsAsIs() {
        assertEquals("Apr 2027", formatValidUntil("Apr 2027", Locale.ENGLISH))
        assertEquals("Lifetime", formatValidUntil(" Lifetime ", Locale.ENGLISH))
        assertEquals("", formatValidUntil("", Locale.ENGLISH))
    }

    @Test
    fun agoBuckets() {
        val now = 10_000_000_000L
        assertEquals(Ago.JustNow, agoOf(now - 20_000, now))
        assertEquals(Ago.Minutes(12), agoOf(now - 12 * 60_000, now))
        assertEquals(Ago.Hours(3), agoOf(now - 3 * 3_600_000 - 5, now))
        assertEquals(Ago.OnDate(now - 30 * 3_600_000L), agoOf(now - 30 * 3_600_000L, now))
        assertEquals(Ago.JustNow, agoOf(now + 60_000, now))
    }

    @Test
    fun clearingEveryPlaceIsAllowedButHalfRowsAreNot() {
        assertEquals(emptyList<MemberPlace>(), listOf(MemberPlace("", " ")).placesToSave())
        assertEquals(null, listOf(MemberPlace("Home", "")).placesToSave())
        assertEquals(listOf(MemberPlace("Home", "1 Road")), listOf(MemberPlace(" Home", "1 Road ")).placesToSave())
    }

    @Test
    fun photoCreditsParseAndIgnoreEmpty() {
        assertEquals(emptyList<PhotoCredit>(), parsePhotoCredits("[]"))
        val parsed = parsePhotoCredits(
            """[{"resource":"temple_kashi","title":"Kashi","author":"A","license":"CC BY 4.0",
                "licenseUrl":"https://l","sourceUrl":"https://s","modified":"cropped","extra":1}]""",
        )
        assertEquals("Kashi", parsed.single().title)
    }
}
