package com.hinvr.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class MemberPlace(
    val label: String = "",
    val address: String = "",
)

private val placeJson = Json { ignoreUnknownKeys = true }

fun encodePlaces(places: List<MemberPlace>): String = placeJson.encodeToString(places)

fun decodePlaces(raw: String): List<MemberPlace> {
    val text = raw.trim()
    if (text.isEmpty()) return emptyList()
    if (!text.startsWith("[")) return listOf(MemberPlace(label = "Home", address = text))
    return runCatching { placeJson.decodeFromString<List<MemberPlace>>(text) }.getOrDefault(emptyList())
}

/** Completed places only. Null when a row has a name without an address, or the reverse. */
fun List<MemberPlace>.readyPlaces(): List<MemberPlace>? {
    val filled = map { MemberPlace(label = it.label.trim(), address = it.address.trim()) }
        .filter { it.label.isNotEmpty() || it.address.isNotEmpty() }
    if (filled.isEmpty() || filled.any { it.label.isBlank() || it.address.isBlank() }) return null
    return filled
}
