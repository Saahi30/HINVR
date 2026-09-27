package com.hinvr.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class PassCheckIn(
    val id: String = "",
    val place: String = "",
    val note: String = "",
    val createdAt: String = "",
)

private val checkInJson = Json { ignoreUnknownKeys = true }

fun encodeCheckIns(rows: List<PassCheckIn>): String = checkInJson.encodeToString(rows)

fun decodeCheckIns(raw: String): List<PassCheckIn> {
    val text = raw.trim()
    if (!text.startsWith("[")) return emptyList()
    return runCatching { checkInJson.decodeFromString<List<PassCheckIn>>(text) }.getOrDefault(emptyList())
}
