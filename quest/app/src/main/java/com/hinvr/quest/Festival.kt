package com.hinvr.quest

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.concurrent.Executors
import org.json.JSONObject

/**
 * The extra puja a festival adds before aarti, as the server names it. [prop] waits on the
 * side table; [records] is the offering kind the server keeps for it.
 */
enum class FestivalKind(
    val wire: String,
    val prop: String,
    val records: OfferingKind,
    @StringRes val step: Int,
    @StringRes val next: Int,
    @StringRes val done: Int,
) {
    Deepmala("deepmala", "deepmala", OfferingKind.Deepmala, R.string.step_deepmala, R.string.next_deepmala, R.string.offer_deepmala),
    Abhishek("abhishek", "lota", OfferingKind.Abhishek, R.string.step_abhishek, R.string.next_abhishek, R.string.offer_abhishek),
    Modak("modak", "modak", OfferingKind.Prasad, R.string.step_modak, R.string.next_modak, R.string.offer_modak),
    Hibiscus("hibiscus", "hibiscus", OfferingKind.Pushp, R.string.step_hibiscus, R.string.next_hibiscus, R.string.offer_hibiscus),
    ;

    companion object {
        fun from(wire: String?): FestivalKind? = entries.firstOrNull { it.wire == wire?.trim()?.lowercase() }
    }
}

data class Festival(val id: String, val kind: FestivalKind, val name: String, val nameHi: String) {
    /** The festival's name in the headset's language. */
    fun title(hindi: Boolean): String = if (hindi && nameHi.isNotBlank()) nameHi else name

    companion object {
        /** The RPC's jsonb: an object, or null when no festival is on. Unknown kinds are ignored. */
        fun parse(raw: String?): Festival? {
            val text = raw?.trim().orEmpty()
            if (text.isEmpty() || text == "null") return null
            val json = runCatching { JSONObject(text) }.getOrNull() ?: return null
            val kind = FestivalKind.from(json.optString("kind")) ?: return null
            val name = json.optString("name").takeIf { it.isNotBlank() && it != "null" } ?: return null
            return Festival(
                id = json.optString("id").ifBlank { kind.wire },
                kind = kind,
                name = name,
                nameHi = json.optString("name_hi").takeIf { it != "null" }.orEmpty(),
            )
        }
    }
}

/** Today's festival, if the server has one. Fetched each time the member enters the sanctum. */
object Festivals {
    private const val Tag = "HinvrFestival"

    private val main = Handler(Looper.getMainLooper())
    private val network = Executors.newSingleThreadExecutor()

    var active by mutableStateOf<Festival?>(null)
        private set

    /** Debug builds can try a festival without the server (intent extra "festival"). */
    var override: FestivalKind? = null

    fun refresh() {
        override?.let { kind ->
            active = Festival("debug-${kind.wire}", kind, kind.name, kind.name)
            return
        }
        if (!QuestAccount.configured) return
        network.execute {
            val festival = runCatching { Festival.parse(QuestAccount.call("active_festival", JSONObject())) }
                .onFailure { Log.w(Tag, "active_festival unavailable", it) }
                .getOrNull()
            main.post {
                active = festival
                MandirScene.festivalChanged()
            }
        }
    }
}
