package com.hinvr.app.data

import android.content.Context
import android.util.Log
import com.hinvr.app.R
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One photograph the app ships, as listed in res/raw/photo_credits.json. */
@Serializable
data class PhotoCredit(
    val resource: String = "",
    val title: String = "",
    val author: String = "",
    val license: String = "",
    val licenseUrl: String = "",
    val sourceUrl: String = "",
    val modified: String = "",
)

private val creditJson = Json { ignoreUnknownKeys = true }

fun parsePhotoCredits(raw: String): List<PhotoCredit> {
    val text = raw.trim()
    if (!text.startsWith("[")) return emptyList()
    return runCatching { creditJson.decodeFromString<List<PhotoCredit>>(text) }
        .getOrDefault(emptyList())
        .filter { it.title.isNotBlank() || it.resource.isNotBlank() }
}

fun loadPhotoCredits(context: Context): List<PhotoCredit> = runCatching {
    context.resources.openRawResource(R.raw.photo_credits).bufferedReader().use { it.readText() }
}.onFailure { Log.w("HinvrCredits", "photo_credits.json unreadable", it) }
    .map(::parsePhotoCredits)
    .getOrDefault(emptyList())
