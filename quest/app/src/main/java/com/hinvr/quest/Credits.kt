package com.hinvr.quest

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray

/** One licensed card photograph, as res/raw/photo_credits.json lists it. */
data class PhotoCredit(
    val resource: String,
    val title: String,
    val author: String,
    val license: String,
    val licenseUrl: String,
    val sourceUrl: String,
    val modified: Boolean,
)

object PhotoCredits {
    private const val Tag = "HinvrCredits"

    var all: List<PhotoCredit> = emptyList()
        private set

    fun load(context: Context) {
        all = runCatching {
            context.resources.openRawResource(R.raw.photo_credits).bufferedReader().use { parse(it.readText()) }
        }.onFailure { Log.w(Tag, "photo_credits.json unreadable", it) }.getOrDefault(emptyList())
    }

    fun parse(text: String): List<PhotoCredit> {
        val rows = JSONArray(text)
        return buildList {
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                val author = row.optString("author").trim()
                val license = row.optString("license").trim()
                if (author.isEmpty() && license.isEmpty()) continue
                add(
                    PhotoCredit(
                        resource = row.optString("resource"),
                        title = row.optString("title"),
                        author = author,
                        license = license,
                        licenseUrl = row.optString("licenseUrl"),
                        sourceUrl = row.optString("sourceUrl"),
                        // Either a flag or a short note of what was changed.
                        modified = when (val value = row.opt("modified")) {
                            is Boolean -> value
                            is String -> value.isNotBlank() && !value.equals("false", ignoreCase = true) && value != "no"
                            else -> false
                        },
                    ),
                )
            }
        }
    }
}

/** The sanctum's own sources: textures, models, sounds, and the one photograph in the bake. */
@Composable
fun MandirCredits() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.credits_sanctum_heading), color = Gold, fontSize = 15.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.3.sp)
        Text(stringResource(R.string.credits_sanctum_body), color = CreamMuted, fontSize = 15.sp, lineHeight = 20.sp)
    }
}

/** Legal page. Reachable from pairing and from the profile, so nobody needs an account to read it. */
@Composable
fun CreditsPanel() {
    if (Ring.stage != Stage.Credits) return
    val photos = PhotoCredits.all
    val perPage = 6
    // Page 0 is the sanctum; photo pages follow.
    val pages = 1 + (photos.size + perPage - 1) / perPage
    var page by remember { mutableIntStateOf(0) }
    if (page >= pages) page = 0
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(PanelShape)
            .background(InkGlass)
            .border(1.dp, Gold.copy(alpha = 0.6f), PanelShape)
            .padding(horizontal = 40.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(R.string.credits_heading), color = Gold, fontSize = 15.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.6.sp)
        if (page == 0) {
            MandirCredits()
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.credits_streams), color = CreamMuted, fontSize = 15.sp, lineHeight = 20.sp)
        } else {
            Text(stringResource(R.string.credits_photos_heading), color = Gold, fontSize = 15.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.3.sp)
            photos.drop((page - 1) * perPage).take(perPage).forEach { credit ->
                Column {
                    Text(
                        credit.title.ifBlank { credit.resource },
                        color = Cream,
                        fontSize = 17.sp,
                        maxLines = 1,
                    )
                    Text(
                        stringResource(
                            if (credit.modified) R.string.credits_photo_line_modified else R.string.credits_photo_line,
                            credit.author.ifBlank { stringResource(R.string.credits_unknown_author) },
                            credit.license,
                        ),
                        color = CreamMuted,
                        fontSize = 14.sp,
                        maxLines = 1,
                    )
                    if (credit.sourceUrl.isNotBlank()) {
                        Text(credit.sourceUrl, color = CreamMuted.copy(alpha = 0.7f), fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Pill(stringResource(R.string.action_back)) { Ring.back() }
            if (pages > 1) {
                Pill(stringResource(R.string.action_more), filled = false) { page = (page + 1) % pages }
                Text(stringResource(R.string.credits_page, page + 1, pages), color = CreamMuted, fontSize = 15.sp)
            }
        }
    }
}
