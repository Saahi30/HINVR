package com.hinvr.app.i18n

import android.content.Context
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
}

@Composable
fun ProvideAppLanguage(languageTag: String, content: @Composable () -> Unit) {
    val tag = AppLocale.normalize(languageTag)
    val base = LocalContext.current
    val root = remember { base }
    val locale = remember(tag) { Locale.forLanguageTag(tag) }
    Locale.setDefault(locale)
    applyHinvrType(tag == "hi")
    val config = remember(tag) {
        Configuration(root.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
    }
    val localized = remember(tag) { root.createConfigurationContext(config) }
    CompositionLocalProvider(
        LocalAppLanguage provides tag,
        LocalContext provides localized,
        LocalConfiguration provides config,
        content = content,
    )
}
