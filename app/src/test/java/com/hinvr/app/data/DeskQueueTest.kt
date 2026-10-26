package com.hinvr.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeskQueueTest {

    private fun request(state: DeskRequestState = DeskRequestState.Sending, attempts: Int = 0) =
        newDeskRequest("r1", "VISIT", "Kashi · 2026-11-03", "Pune", "kashi", now = 10L)
            .copy(state = state, attempts = attempts)

    @Test
    fun newRequestStartsSending() {
        val r = request()
        assertEquals(DeskRequestState.Sending, r.state)
        assertEquals("kashi", r.mandirId)
    }

    @Test
    fun acceptedBecomesSent() {
        assertEquals(DeskRequestState.Sent, request().after(DeskSendResult.Accepted).state)
    }

    @Test
    fun networkFailureWaitsThenGivesUp() {
        val once = request().after(DeskSendResult.Transient)
        assertEquals(DeskRequestState.Waiting, once.state)
        assertEquals(1, once.attempts)
        val last = request(attempts = DeskAutoRetryLimit - 1).after(DeskSendResult.Transient)
        assertEquals(DeskRequestState.Failed, last.state)
    }

    @Test
    fun rejectionFailsAtOnce() {
        assertEquals(DeskRequestState.Failed, request().after(DeskSendResult.Rejected("no")).state)
    }

    @Test
    fun retryRules() {
        assertNull(request(DeskRequestState.Sent).retrying(manual = true))
        assertEquals(DeskRequestState.Sending, request(DeskRequestState.Waiting).retrying(manual = false)?.state)
        assertNull(request(DeskRequestState.Failed).retrying(manual = false))
        val manual = request(DeskRequestState.Failed, attempts = 6).retrying(manual = true)
        assertEquals(DeskRequestState.Sending, manual?.state)
        assertEquals(0, manual?.attempts)
    }

    @Test
    fun interruptedSendIsRecoveredAsWaiting() {
        assertEquals(DeskRequestState.Waiting, request(DeskRequestState.Sending).recovered().state)
        assertEquals(DeskRequestState.Sent, request(DeskRequestState.Sent).recovered().state)
    }

    @Test
    fun summaryIsFlattenedAndCapped() {
        assertEquals("a b", clampDeskSummary("a\n  b"))
        val long = clampDeskSummary("x".repeat(DeskSummaryLimit + 50))
        assertEquals(DeskSummaryLimit, long.length)
        assertTrue(long.endsWith("…"))
    }

    @Test
    fun trimmingNeverDropsUnsentRequests() {
        var rows = emptyList<DeskRequest>()
        rows = rows.upsert(request(DeskRequestState.Waiting).copy(id = "keep", createdAt = 0))
        repeat(40) { i ->
            rows = rows.upsert(request(DeskRequestState.Sent).copy(id = "s$i", createdAt = 100L + i))
        }
        assertTrue(rows.any { it.id == "keep" })
        assertTrue(rows.size <= 30)
    }

    @Test
    fun queueRoundTrips() {
        val rows = listOf(request(DeskRequestState.Waiting, attempts = 2))
        assertEquals(rows, decodeDeskRequests(encodeDeskRequests(rows)))
        assertEquals(emptyList<DeskRequest>(), decodeDeskRequests("not json"))
    }
}
