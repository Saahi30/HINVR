package com.hinvr.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** desk_requests.summary is capped server-side. Clamp before sending so the insert never fails on length. */
const val DeskSummaryLimit = 4000

/** Kept on the phone so the member can see what the desk has, and what is still waiting. */
private const val KeptRequests = 30

/** After this many network failures the request stops retrying on its own and asks the member. */
const val DeskAutoRetryLimit = 6

enum class DeskRequestState {
    /** Handed to the network right now. */
    Sending,

    /** The server accepted it. */
    Sent,

    /** The network failed. It goes again on resume, on sync, and when the phone is back online. */
    Waiting,

    /** The server refused it, or it kept failing. Only the member's retry sends it again. */
    Failed,
}

@Serializable
data class DeskRequest(
    val id: String,
    val kind: String,
    val summary: String,
    val city: String = "",
    val mandirId: String? = null,
    val createdAt: Long,
    val state: DeskRequestState = DeskRequestState.Sending,
    val attempts: Int = 0,
)

/** How one insert went. Transient failures are retried; rejections are not. */
sealed interface DeskSendResult {
    data object Accepted : DeskSendResult
    data object Transient : DeskSendResult
    data class Rejected(val message: String) : DeskSendResult
}

fun clampDeskSummary(summary: String): String {
    val flat = summary.replace(Regex("\\s*\\n\\s*"), " ").trim()
    return if (flat.length <= DeskSummaryLimit) flat else flat.take(DeskSummaryLimit - 1).trimEnd() + "…"
}

fun newDeskRequest(
    id: String,
    kind: String,
    summary: String,
    city: String,
    mandirId: String?,
    now: Long,
): DeskRequest = DeskRequest(
    id = id,
    kind = kind,
    summary = clampDeskSummary(summary),
    city = city.trim(),
    mandirId = mandirId?.trim()?.ifBlank { null },
    createdAt = now,
    state = DeskRequestState.Sending,
)

/** The queue state after one send attempt finished. */
fun DeskRequest.after(result: DeskSendResult): DeskRequest = when (result) {
    DeskSendResult.Accepted -> copy(state = DeskRequestState.Sent, attempts = attempts + 1)
    DeskSendResult.Transient -> {
        val tries = attempts + 1
        copy(
            state = if (tries >= DeskAutoRetryLimit) DeskRequestState.Failed else DeskRequestState.Waiting,
            attempts = tries,
        )
    }
    is DeskSendResult.Rejected -> copy(state = DeskRequestState.Failed, attempts = attempts + 1)
}

/** The member tapped retry, or the queue is flushing. Sent requests never go twice. */
fun DeskRequest.retrying(manual: Boolean): DeskRequest? = when (state) {
    DeskRequestState.Sent -> null
    DeskRequestState.Sending -> null
    DeskRequestState.Waiting -> copy(state = DeskRequestState.Sending)
    DeskRequestState.Failed -> if (manual) copy(state = DeskRequestState.Sending, attempts = 0) else null
}

/** A request left in [DeskRequestState.Sending] when the app died never finished; treat it as waiting. */
fun DeskRequest.recovered(): DeskRequest =
    if (state == DeskRequestState.Sending) copy(state = DeskRequestState.Waiting) else this

fun List<DeskRequest>.upsert(request: DeskRequest): List<DeskRequest> {
    val replaced = map { if (it.id == request.id) request else it }
    val merged = if (replaced.any { it.id == request.id }) replaced else replaced + request
    // Never trim away something the desk has not received.
    val sent = merged.filter { it.state == DeskRequestState.Sent }
    val overflow = (merged.size - KeptRequests).coerceAtLeast(0)
    val drop = sent.sortedBy { it.createdAt }.take(overflow).mapTo(mutableSetOf()) { it.id }
    return merged.filterNot { it.id in drop }
}

private val deskJson = Json { ignoreUnknownKeys = true }

fun encodeDeskRequests(rows: List<DeskRequest>): String = deskJson.encodeToString(rows)

fun decodeDeskRequests(raw: String): List<DeskRequest> {
    val text = raw.trim()
    if (!text.startsWith("[")) return emptyList()
    return runCatching { deskJson.decodeFromString<List<DeskRequest>>(text) }.getOrDefault(emptyList())
}
