package com.hinvr.quest

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.Executors
import org.json.JSONObject

/** Sends each offering of a visit once, and keeps trying for a while if the network drops. */
object MandirOfferings {
    private const val Tag = "HinvrOfferings"

    private val RetryDelaysMs = longArrayOf(5_000L, 20_000L, 60_000L)

    private val main = Handler(Looper.getMainLooper())
    private val network = Executors.newSingleThreadExecutor()
    private val sent = HashSet<OfferingKind>()
    private var visit = 0

    /** A new visit may offer everything again. Retries from the last visit are dropped. */
    fun beginVisit() {
        visit++
        sent.clear()
    }

    fun record(kind: OfferingKind) {
        if (!QuestAccount.configured || !QuestAccount.isPaired) return
        if (kind.label !in AllowedKinds) return
        if (!sent.add(kind)) return
        send(kind, sankalpText(), Mandir.darshan?.mandirId.orEmpty(), visit, attempt = 0)
    }

    private fun send(kind: OfferingKind, sankalp: String, mandirId: String, visitId: Int, attempt: Int) {
        network.execute {
            val result = runCatching {
                QuestAccount.call(
                    "record_offering",
                    QuestAccount.deviceArgs()
                        .put("p_kind", kind.label)
                        .put("p_mandir_id", mandirId)
                        .put("p_sankalp", sankalp),
                )
            }
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
                        if (visitId == visit) send(kind, sankalp, mandirId, visitId, attempt + 1)
                    }, RetryDelaysMs[attempt])
                } else {
                    sent.remove(kind)
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

    val AllowedKinds = setOf("ghanta", "diya", "aarti", "pushp", "agarbatti", "prasad", "tilak", "daan")
}
