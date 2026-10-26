package com.hinvr.quest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogRulesTest {
    private val now = 1_800_000_000_000L
    private val kashi = RingCard("kashi", "Kashi Vishwanath", "KASHI", 7, liveUrl = "https://www.youtube.com/embed/J0nRdUOQps8")
    private val shirdi = RingCard("shirdi", "Sai Baba", "SHIRDI", 9, liveUrl = "https://www.youtube.com/embed/6Uf3aMujHa8")
    private val bundled = listOf(kashi, shirdi)

    @Test
    fun serverClearedStreamMeansNoStream() {
        val merged = CatalogRules.merge(bundled, listOf(RingCard("kashi", "Kashi Vishwanath", "KASHI", null, liveUrl = "")))
        assertEquals("", merged.single().liveUrl)
        assertTrue(!merged.single().hasStream)
    }

    @Test
    fun unpublishedBundledMandirIsGone() {
        val merged = CatalogRules.merge(bundled, listOf(RingCard("kashi", "Kashi", "KASHI", null)))
        assertEquals(listOf("kashi"), merged.map { it.mandirId })
    }

    @Test
    fun emptyServerCatalogIsEmpty() {
        assertTrue(CatalogRules.merge(bundled, emptyList()).isEmpty())
    }

    @Test
    fun bundledRowLendsOnlyItsPhotoAndBlanks() {
        val remote = RingCard("kashi", "", "", null, liveUrl = "https://www.youtube.com/embed/AAAAAAAAAAA", live = LiveState.Live)
        val merged = CatalogRules.merge(bundled, listOf(remote)).single()
        assertEquals(7, merged.photo)
        assertEquals("Kashi Vishwanath", merged.title)
        assertEquals("https://www.youtube.com/embed/AAAAAAAAAAA", merged.liveUrl)
        assertEquals(LiveState.Live, merged.live)
        assertEquals("Uttar Pradesh", merged.region)
    }

    @Test
    fun newServerMandirHasNoBorrowedPhoto() {
        val merged = CatalogRules.merge(bundled, listOf(RingCard("new-mandir", "New Mandir", "PUNE", null, state = "Maharashtra")))
        assertNull(merged.single().photo)
    }

    @Test
    fun serverOrderWins() {
        val merged = CatalogRules.merge(bundled, listOf(RingCard("shirdi", "Sai Baba", "", null), RingCard("kashi", "Kashi", "", null)))
        assertEquals(listOf("shirdi", "kashi"), merged.map { it.mandirId })
    }

    @Test
    fun liveOnlyWhenTheServerSaysSoRecently() {
        assertEquals(LiveState.Live, CatalogRules.liveState(true, now - 5 * 60_000L, now))
        assertEquals(LiveState.Unknown, CatalogRules.liveState(true, now - 2 * 60 * 60_000L, now))
        assertEquals(LiveState.NotLive, CatalogRules.liveState(false, now, now))
        assertEquals(LiveState.Live, CatalogRules.liveState(true, null, now))
    }

    @Test
    fun parseReadsLiveState() {
        val json = """
            [
              {"id":"kashi","name":"Kashi Vishwanath","city":"Varanasi","place":"Varanasi, Uttar Pradesh",
               "live":true,"live_url":"https://www.youtube.com/embed/J0nRdUOQps8","live_checked_at":"2027-01-15T08:00:00+00:00"},
              {"id":"shirdi","name":"Sai Baba","city":"","place":"Shirdi, Maharashtra","live":false,"live_url":"https://www.youtube.com/embed/6Uf3aMujHa8","live_checked_at":null},
              {"id":"kedarnath","name":"Kedarnath","place":"Kedarnath, Uttarakhand","live":false,"live_url":""},
              {"id":"","name":"skip me"}
            ]
        """.trimIndent()
        val checked = java.time.OffsetDateTime.parse("2027-01-15T08:00:00+00:00").toInstant().toEpochMilli()
        val rows = QuestCatalog.parse(json, checked + 60_000L)
        assertEquals(listOf("kashi", "shirdi", "kedarnath"), rows.map { it.mandirId })
        assertEquals(LiveState.Live, rows[0].live)
        assertEquals("VARANASI", rows[0].place)
        assertEquals("Uttar Pradesh", rows[0].state)
        assertEquals(LiveState.NotLive, rows[1].live)
        assertEquals("SHIRDI", rows[1].place)
        assertEquals(LiveState.NotLive, rows[2].live)
        assertTrue(!rows[2].hasStream)
    }

    @Test
    fun defaultDarshanPrefersAJyotirlinga() {
        val tirupati = RingCard("tirupati", "Sri Venkateswara", "", null, liveUrl = "x", live = LiveState.Live)
        val somnath = RingCard("somnath", "Somnath", "", null, liveUrl = "y", live = LiveState.Live)
        val kashiQuiet = kashi.copy(live = LiveState.NotLive)
        assertEquals("somnath", CatalogRules.defaultDarshan(listOf(tirupati, kashiQuiet, somnath))?.mandirId)
        assertEquals("kashi", CatalogRules.defaultDarshan(listOf(tirupati, kashiQuiet))?.mandirId)
        assertEquals("tirupati", CatalogRules.defaultDarshan(listOf(shirdi, tirupati))?.mandirId)
        assertNull(CatalogRules.defaultDarshan(emptyList()))
    }

    @Test
    fun statesGroupAndCount() {
        val states = CatalogRules.states(BundledMandirs)
        val gujarat = states.single { it.state == "Gujarat" }
        assertEquals(BundledMandirs.count { it.region == "Gujarat" }, gujarat.place.substringBefore(' ').toInt())
        assertEquals(states.map { it.title.lowercase() }.sorted(), states.map { it.title.lowercase() })
    }
}
