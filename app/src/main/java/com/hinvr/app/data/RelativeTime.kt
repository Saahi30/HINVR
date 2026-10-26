package com.hinvr.app.data

/** How long ago something happened, bucketed the way the app words it. */
sealed interface Ago {
    data object JustNow : Ago
    data class Minutes(val count: Long) : Ago
    data class Hours(val count: Long) : Ago

    /** Older than a day: show the date itself. */
    data class OnDate(val epochMs: Long) : Ago
}

fun agoOf(epochMs: Long, now: Long): Ago {
    val elapsed = (now - epochMs).coerceAtLeast(0L)
    val minutes = elapsed / 60_000L
    return when {
        minutes < 1 -> Ago.JustNow
        minutes < 60 -> Ago.Minutes(minutes)
        minutes < 24 * 60 -> Ago.Hours(minutes / 60)
        else -> Ago.OnDate(epochMs)
    }
}
