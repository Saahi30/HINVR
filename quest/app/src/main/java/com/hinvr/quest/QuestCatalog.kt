package com.hinvr.quest

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.net.HttpURLConnection
import java.net.URI
import java.time.OffsetDateTime
import java.util.concurrent.Executors
import org.json.JSONArray

enum class CatalogStatus { Idle, Loading, Fresh, Failed }

/** Published mandirs and their live state, as the server's live check last saw them. */
object QuestCatalog {
    private const val Tag = "HinvrCatalog"
    private const val StaleAfterMs = 5 * 60_000L

    private val main = Handler(Looper.getMainLooper())
    private val network = Executors.newSingleThreadExecutor()
    private var loading = false
    private var fetchedAtMs = 0L

    var status by mutableStateOf(CatalogStatus.Idle)
        private set

    fun refresh() {
        if (!QuestAccount.configured || loading) return
        loading = true
        if (!Ring.fromServer) status = CatalogStatus.Loading
        network.execute {
            val result = runCatching { fetch() }
            result.exceptionOrNull()?.let { Log.w(Tag, "Catalog fetch failed", it) }
            main.post {
                loading = false
                val rows = result.getOrNull()
                if (rows == null) {
                    status = CatalogStatus.Failed
                    return@post
                }
                fetchedAtMs = SystemClock.elapsedRealtime()
                status = CatalogStatus.Fresh
                Ring.replaceFromCloud(rows)
            }
        }
    }

    /** Live state changes every few minutes; screens that show it call this each frame. */
    fun refreshIfStale() {
        if (loading || !QuestAccount.configured) return
        val age = SystemClock.elapsedRealtime() - fetchedAtMs
        if (fetchedAtMs == 0L && status == CatalogStatus.Failed && age < 60_000L) return
        if (fetchedAtMs == 0L || age > StaleAfterMs) refresh()
    }

    /** Off once the server has said it has no aarti_times column, so later fetches don't ask again. */
    private var askAartiTimes = true

    /** Null when the server couldn't be read. An empty list means nothing is published. */
    private fun fetch(): List<RingCard>? {
        if (askAartiTimes) {
            val rows = fetch(withAartiTimes = true)
            if (rows != null) return rows
            if (!refusedColumn) return null
            // An older schema: ask again without the column.
            askAartiTimes = false
        }
        return fetch(withAartiTimes = false)
    }

    private var refusedColumn = false

    private fun fetch(withAartiTimes: Boolean): List<RingCard>? {
        refusedColumn = false
        val endpoint = buildString {
            append(BuildConfig.SUPABASE_URL.trimEnd('/'))
            append("/rest/v1/mandirs?published=eq.true")
            append("&select=id,name,city,place,scene,live,live_url,live_checked_at")
            if (withAartiTimes) append(",aarti_times")
            append("&order=sort_order.asc")
        }
        val connection = (URI(endpoint).toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_PUBLISHABLE_KEY}")
            setRequestProperty("Accept", "application/json")
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                Log.w(Tag, "Catalog HTTP $code")
                refusedColumn = withAartiTimes && code == 400
                return null
            }
            val text = connection.inputStream.bufferedReader().readText()
            return parse(text, System.currentTimeMillis())
        } finally {
            connection.disconnect()
        }
    }

    fun parse(text: String, nowMs: Long): List<RingCard> {
        val rows = JSONArray(text)
        return buildList {
            for (i in 0 until rows.length()) {
                val row = rows.getJSONObject(i)
                val id = row.optString("id")
                if (id.isBlank()) continue
                val place = row.optString("place")
                val city = row.optString("city").ifBlank { place.substringBefore(',').trim() }
                val checkedAt = row.optString("live_checked_at").takeIf { it.isNotBlank() && it != "null" }
                    ?.let { runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull() }
                val liveUrl = row.optString("live_url").takeIf { it != "null" }.orEmpty()
                add(
                    RingCard(
                        mandirId = id,
                        title = row.optString("name"),
                        place = city.uppercase(),
                        photo = null,
                        liveUrl = liveUrl,
                        live = if (liveUrl.isBlank()) {
                            LiveState.NotLive
                        } else {
                            CatalogRules.liveState(row.optBoolean("live", false), checkedAt, nowMs)
                        },
                        state = place.substringAfterLast(',', missingDelimiterValue = "").trim(),
                        aartiTimes = row.optJSONArray("aarti_times")?.let { times ->
                            AartiSync.parseTimes(List(times.length()) { times.optString(it) })
                        }.orEmpty(),
                    ),
                )
            }
        }
    }
}
