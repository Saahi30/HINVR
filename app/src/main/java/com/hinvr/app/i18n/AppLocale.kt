package com.hinvr.app.i18n

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.hinvr.app.ui.theme.applyHinvrType
import java.util.Locale

val LocalAppLanguage = staticCompositionLocalOf { "en" }

object AppLocale {
    private const val PREFS = "hinvr_locale"
    private const val KEY = "language_tag"

    fun normalize(tag: String?): String = if (tag == "hi") "hi" else "en"

    fun read(context: Context): String =
        normalize(
            context.applicationContext
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY, "en"),
        )

    fun persist(context: Context, tag: String) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, normalize(tag))
            .apply()
    }

    /**
     * Process-wide side effects of a language choice: default locale for date
     * formatting and the Devanagari type scale. Called when the language changes,
     * never from composition.
     */
    fun applyProcessDefaults(tag: String) {
        val normalized = normalize(tag)
        Locale.setDefault(Locale.forLanguageTag(normalized))
        applyHinvrType(normalized == "hi")
    }
}

@Composable
fun ProvideAppLanguage(languageTag: String, content: @Composable () -> Unit) {
    val tag = AppLocale.normalize(languageTag)
    val base = LocalContext.current
    val root = remember { base }
    val locale = remember(tag) { Locale.forLanguageTag(tag) }
    // The activity handles rotation itself, so the live configuration changes under
    // us. Derive from it every time; only the locale is overridden.
    val live = LocalConfiguration.current
    val config = remember(tag, live) {
        Configuration(live).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
    }
    // Keep the Activity in the context chain. createConfigurationContext() on newer
    // Android returns a detached context, which drops the activity-result owner.
    val localized = remember(config) {
        object : ContextWrapper(root) {
            private val resourcesOverride = root.createConfigurationContext(config).resources
            override fun getResources() = resourcesOverride
        }
    }
    CompositionLocalProvider(
        LocalAppLanguage provides tag,
        LocalContext provides localized,
        LocalConfiguration provides config,
        content = content,
    )
}
