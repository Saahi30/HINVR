package com.hinvr.quest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YouTubeTest {
    @Test
    fun embedLinkGivesVideo() {
        assertEquals(YoutubeRef.Video("J0nRdUOQps8"), YouTube.parse("https://www.youtube.com/embed/J0nRdUOQps8"))
    }

    @Test
    fun watchAndShortLinksGiveVideo() {
        assertEquals("dQw4w9WgXcQ", YouTube.videoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=10"))
        assertEquals("dQw4w9WgXcQ", YouTube.videoId("https://www.youtube.com/watch?feature=share&v=dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", YouTube.videoId("https://youtu.be/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", YouTube.videoId("https://www.youtube.com/live/dQw4w9WgXcQ?si=x"))
        assertEquals("dQw4w9WgXcQ", YouTube.videoId("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ?autoplay=1"))
    }

    @Test
    fun liveStreamChannelLinkGivesChannel() {
        assertEquals(
            YoutubeRef.Channel("UCTboTRX74UydvU_cBdm_cCQ"),
            YouTube.parse("https://www.youtube.com/embed/live_stream?channel=UCTboTRX74UydvU_cBdm_cCQ"),
        )
        assertNull(YouTube.videoId("https://www.youtube.com/embed/live_stream?channel=UCTboTRX74UydvU_cBdm_cCQ"))
    }

    @Test
    fun junkGivesNothing() {
        assertNull(YouTube.parse(""))
        assertNull(YouTube.parse("   "))
        assertNull(YouTube.parse("https://example.com/embed/J0nRdUOQps8"))
        assertNull(YouTube.parse("https://www.youtube.com/embed/short"))
        assertNull(YouTube.parse("https://www.youtube.com/embed/J0nRdUOQps8extra"))
    }
}
