package com.hinvr.app.i18n

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.hinvr.app.R
import com.hinvr.app.navigation.Destinations
import com.hinvr.app.ui.catalog.HinvrCatalog
import com.hinvr.app.ui.catalog.Mandir
import com.hinvr.app.ui.catalog.ServiceTile

/**
 * Hindi copy for bundled temples lives in values-hi as mandir_<id>_<field>. It is a
 * translation of the bundled English, so it is used only while the desk's text
 * still matches that English. Text the desk has written itself is shown as written
 * (English when there is no Hindi), never replaced with generic filler.
 */
fun Mandir.localized(context: Context): Mandir {
    val hindi = context.resources.configuration.locales[0]?.language == "hi"
    if (!hindi) return this
    val bundled = HinvrCatalog.bundledMandirs.find { it.id == id } ?: return this
    fun field(suffix: String, current: String, original: String): String {
        if (current != original) return current
        val key = "mandir_${id.replace('-', '_')}_$suffix"
        val res = context.resources.getIdentifier(key, "string", context.packageName)
        return if (res != 0) context.getString(res) else current
    }
    return copy(
        deity = field("deity", deity, bundled.deity),
        summary = field("summary", summary, bundled.summary),
        history = field("history", history, bundled.history),
        significance = field("significance", significance, bundled.significance),
        architecture = field("architecture", architecture, bundled.architecture),
        dressCode = field("dress", dressCode, bundled.dressCode),
        bestTime = field("best", bestTime, bundled.bestTime),
        visitorNotes = field("notes", visitorNotes, bundled.visitorNotes),
        facilities = field("facilities", facilities, bundled.facilities),
        timings = field("timings", timings, bundled.timings),
    )
}

@Composable
fun List<Mandir>.localized(): List<Mandir> {
    val context = LocalContext.current
    return remember(this, context) { map { it.localized(context) } }
}

/**
 * The six service tiles always use the app's own translated copy, keyed by route.
 * The CMS seed still carries retired wording, so its title text is not shown.
 */
@Composable
fun ServiceTile.localized(): ServiceTile {
    val (titleRes, benefitRes) = when (route) {
        Destinations.Live -> R.string.service_live_title to R.string.service_live_benefit
        Destinations.Vr -> R.string.service_vr_title to R.string.service_vr_benefit
        Destinations.Pass -> R.string.service_pass_title to R.string.service_pass_benefit
        Destinations.Pooja -> R.string.service_pooja_title to R.string.service_pooja_benefit
        Destinations.Concierge -> R.string.service_concierge_title to R.string.service_concierge_benefit
        Destinations.Yatra -> R.string.service_yatra_title to R.string.service_yatra_benefit
        else -> return this
    }
    return copy(title = stringResource(titleRes), benefit = stringResource(benefitRes))
}
