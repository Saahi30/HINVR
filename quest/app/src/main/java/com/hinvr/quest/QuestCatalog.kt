package com.hinvr.quest

import android.os.Handler
import android.os.Looper
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.Executors
import org.json.JSONArray

/** Published mandirs, grouped later by state on the ring. */
object QuestCatalog {
    private val main = Handler(Looper.getMainLooper())
    private val network = Executors.newSingleThreadExecutor()
    private var loading = false

    fun refresh() {
        if (!QuestAccount.configured || loading) return
        loading = true
        network.execute {
            val remote = runCatching { fetch() }.getOrNull()
            main.post {
                loading = false
                if (!remote.isNullOrEmpty()) Ring.replaceFromCloud(remote)
            }
        }
    }

    private fun fetch(): List<RingCard> {
        val endpoint = buildString {
            append(BuildConfig.SUPABASE_URL.trimEnd('/'))
            append("/rest/v1/mandirs?published=eq.true")
            append("&select=id,name,city,place,scene,live_url")
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
            val text = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.readText()
                .orEmpty()
            if (code !in 200..299 || text.isBlank()) return emptyList()
            val rows = JSONArray(text)
            return buildList {
                for (i in 0 until rows.length()) {
                    val row = rows.getJSONObject(i)
                    val id = row.optString("id")
                    if (id.isBlank()) continue
                    val place = row.optString("place")
                    val city = row.optString("city").ifBlank { place.substringBefore(',').trim() }
                    add(
                        RingCard(
                            panelId = 0,
                            mandirId = id,
                            title = row.optString("name"),
                            place = city.uppercase(),
                            photo = photoFor(row.optString("scene")),
                            liveUrl = row.optString("live_url"),
                            state = place.substringAfterLast(',', missingDelimiterValue = "").trim(),
                        ),
                    )
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun photoFor(scene: String): Int? = when (scene.lowercase()) {
        "tirupati" -> R.drawable.temple_tirupati
        "kashi" -> R.drawable.temple_kashi
        "shirdi" -> R.drawable.temple_shirdi
        "kedarnath" -> R.drawable.temple_kedarnath
        "somnath" -> R.drawable.temple_somnath
        else -> null
    }
}
