package com.hinvr.quest

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors
import org.json.JSONObject

object MandirOfferings {
    private val main = Handler(Looper.getMainLooper())
    private val network = Executors.newSingleThreadExecutor()
    private val sent = HashSet<OfferingKind>()

    fun record(kind: OfferingKind) {
        if (!QuestAccount.configured || !QuestAccount.isPaired) return
        if (!sent.add(kind)) return
        network.execute {
            runCatching {
                QuestAccount.call(
                    "record_offering",
                    QuestAccount.deviceArgs()
                        .put("p_kind", kind.label)
                        .put("p_mandir_id", Mandir.darshan?.mandirId.orEmpty())
                        .put("p_sankalp", sankalpText()),
                )
            }
            refreshCounts()
        }
    }

    fun refreshCounts() {
        if (!QuestAccount.configured) return
        network.execute {
            val body = runCatching {
                QuestAccount.call("offering_counts_today", JSONObject())
            }.getOrNull()
            val line = body?.let { parseCounts(it) }.orEmpty()
            main.post { if (line.isNotBlank()) Mandir.countsLine = line }
        }
    }

    private fun parseCounts(raw: String): String {
        val json = runCatching { JSONObject(raw) }.getOrNull() ?: return ""
        val diyas = json.optInt("diya", json.optInt("diyas", 0))
        if (diyas <= 0) return ""
        return "$diyas diyas lit today"
    }

    private fun sankalpText(): String = listOf(
        Mandir.sankalpName,
        Mandir.sankalpGotra,
        Mandir.sankalpIntention,
    ).filter { it.isNotBlank() }.joinToString(" · ")
}
