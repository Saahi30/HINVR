package com.hinvr.quest

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.Executors
import org.json.JSONObject

/**
 * Sends each offering of a visit once, and keeps trying for a while if the network drops.
 * Each record carries whom the visit is for; a server without that field gets it without.
 */
object MandirOfferings {
    private const val Tag = "HinvrOfferings"

    private val RetryDelaysMs = longArrayOf(5_000L, 20_000L, 60_000L)

    private val main = Handler(Looper.getMainLooper())
    private val network = Executors.newSingleThreadExecutor()
    private val sent = HashSet<String>()
    private var visit = 0

    /** Off once the server has refused p_dedicated_to, so the rest of the session doesn't try it. */
    private var sendDedication = true

    /** Likewise for p_family_member: an older server keeps the dedication and loses only the name. */
    private var sendFamily = true

    /** A new visit may offer everything again. Retries from the last visit are dropped. */
    fun beginVisit() {
        visit++
        sent.clear()
    }

    /** Once per [key] a visit: a kind by default; each japa round and the festival puja have their own. */
    fun record(kind: OfferingKind, key: String = kind.label) {
        if (!QuestAccount.configured || !QuestAccount.isPaired) return
        if (kind.label !in AllowedKinds) return
        if (!sent.add(key)) return
        val offer = Offer(
            kind, key, sankalpText(), Mandir.darshan?.mandirId.orEmpty(), Mandir.dedication.wire, Mandir.familyMember?.id.orEmpty(),
        )
        send(offer, visit, attempt = 0)
    }

    private class Offer(
        val kind: OfferingKind,
        val key: String,
        val sankalp: String,
        val mandirId: String,
        val dedication: String,
        val familyMember: String,
    )

    private fun send(offer: Offer, visitId: Int, attempt: Int) {
        val kind = offer.kind
        network.execute {
            fun post(withDedication: Boolean, withFamily: Boolean) = QuestAccount.call(
                "record_offering",
                QuestAccount.deviceArgs()
                    .put("p_kind", kind.label)
                    .put("p_mandir_id", offer.mandirId)
                    .put("p_sankalp", offer.sankalp)
                    .apply { if (withDedication) put("p_dedicated_to", offer.dedication) }
                    .apply { if (withDedication && withFamily) put("p_family_member", offer.familyMember) },
            )
            // Drop what an older record_offering doesn't know, newest field first.
            fun postAsAccepted(): String {
                var family = sendFamily && offer.familyMember.isNotEmpty()
                while (true) {
                    try {
                        return post(sendDedication, family)
                    } catch (refused: QuestAccount.ServerRefused) {
                        // PostgREST can't find a signature with that parameter.
                        val unknown = refused.status == 404
                        when {
                            family && (unknown || "family" in refused.serverMessage) -> {
                                sendFamily = false
                                family = false
                            }
                            sendDedication && (unknown || "dedicated" in refused.serverMessage) -> sendDedication = false
                            else -> throw refused
                        }
                    }
                }
            }
            val result = runCatching { postAsAccepted() }
            val error = result.exceptionOrNull()
            main.post {
                if (error == null) {
                    refreshCounts()
                    return@post
                }
                Log.w(Tag, "record_offering ${kind.label} failed (attempt ${attempt + 1})", error)
                if (visitId != visit) return@post
                // Only a dropped connection is worth repeating; the server refusing it won't change.
                if (error is QuestAccount.NetworkFailure && attempt < RetryDelaysMs.size) {
                    main.postDelayed({
                        if (visitId == visit) send(offer, visitId, attempt + 1)
                    }, RetryDelaysMs[attempt])
                } else {
                    sent.remove(offer.key)
                }
            }
        }
    }

    fun refreshCounts() {
        if (!QuestAccount.configured) return
        network.execute {
            val body = runCatching {
                QuestAccount.call("offering_counts_today", JSONObject())
            }.getOrNull()
            val diyas = body?.let { parseDiyas(it) } ?: 0
            main.post { Mandir.diyasToday = diyas }
        }
    }

    private fun parseDiyas(raw: String): Int {
        val json = runCatching { JSONObject(raw) }.getOrNull() ?: return 0
        return json.optInt("diya", json.optInt("diyas", 0))
    }

    private fun sankalpText(): String = OfferingRules.clampSankalp(
        listOf(Mandir.sankalpName.trim(), Mandir.sankalpGotra.trim(), Mandir.sankalpIntention.wire)
            .filter { it.isNotBlank() }
            .joinToString(" · "),
    )

    val AllowedKinds = setOf(
        "ghanta", "diya", "aarti", "pushp", "agarbatti", "prasad", "tilak", "daan",
        "japa", "abhishek", "deepmala",
    )
}
