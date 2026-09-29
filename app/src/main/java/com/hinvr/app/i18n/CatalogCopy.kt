package com.hinvr.app.i18n

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.hinvr.app.R
import com.hinvr.app.navigation.Destinations
import com.hinvr.app.ui.catalog.Mandir
import com.hinvr.app.ui.catalog.ServiceTile

fun Mandir.localized(context: Context): Mandir {
    val updated = localizeStamp(context, updatedLabel)
    val hindi = context.resources.configuration.locales[0]?.language == "hi"
    if (!hindi) return copy(updatedLabel = updated)
    fun named(suffix: String): String? {
        val key = "mandir_${id.replace('-', '_')}_$suffix"
        val res = context.resources.getIdentifier(key, "string", context.packageName)
        return if (res != 0) context.getString(res) else null
    }
    return copy(
        deity = named("deity") ?: deity,
        summary = named("summary") ?: summary.takeIf { it.isNotBlank() }?.let {
            context.getString(R.string.mandir_generic_summary, name, place)
        }.orEmpty(),
        history = named("history") ?: history,
        significance = named("significance") ?: significance,
        architecture = named("architecture") ?: architecture,
        dressCode = named("dress") ?: dressCode.takeIf { it.isNotBlank() }?.let {
            context.getString(R.string.mandir_generic_dress)
        }.orEmpty(),
        bestTime = named("best") ?: bestTime.takeIf { it.isNotBlank() }?.let {
            nextAarti?.let { aarti -> context.getString(R.string.mandir_generic_best_aarti, aarti) }
                ?: context.getString(R.string.mandir_generic_best)
        }.orEmpty(),
        visitorNotes = named("notes") ?: visitorNotes.takeIf { it.isNotBlank() }?.let {
            context.getString(R.string.mandir_generic_notes)
        }.orEmpty(),
        facilities = named("facilities") ?: facilities,
        timings = named("timings") ?: timings,
        updatedLabel = updated,
    )
}

fun localizeStamp(context: Context, label: String): String {
    val minutes = Regex("""Updated (\d+) min ago""").find(label)?.groupValues?.getOrNull(1)
    if (minutes != null) return context.getString(R.string.updated_min_ago, minutes)
    if (label.contains("just now", ignoreCase = true)) return context.getString(R.string.updated_just_now)
    return label
}

@Composable
fun List<Mandir>.localized(): List<Mandir> {
    val context = LocalContext.current
    return map { it.localized(context) }
}

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
