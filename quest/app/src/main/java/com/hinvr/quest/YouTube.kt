package com.hinvr.quest

/** What a catalog link points at on YouTube: one video, or a channel's current live stream. */
sealed interface YoutubeRef {
    data class Video(val id: String) : YoutubeRef
    data class Channel(val id: String) : YoutubeRef
}

object YouTube {
    private val videoPattern =
        Regex("""(?:youtube(?:-nocookie)?\.com/(?:embed/(?!live_stream)|live/|shorts/|watch\?(?:.*?&)?v=)|youtu\.be/)([\w-]{11})(?![\w-])""")
    private val channelPattern = Regex("""[?&]channel=(UC[\w-]{22})(?![\w-])""")

    /** Null when the link is blank or not a YouTube video or live-channel link. */
    fun parse(raw: String): YoutubeRef? {
        val url = raw.trim()
        if (url.isEmpty()) return null
        videoPattern.find(url)?.let { return YoutubeRef.Video(it.groupValues[1]) }
        if (url.contains("live_stream")) {
            channelPattern.find(url)?.let { return YoutubeRef.Channel(it.groupValues[1]) }
        }
        return null
    }

    fun videoId(raw: String): String? = (parse(raw) as? YoutubeRef.Video)?.id
}
