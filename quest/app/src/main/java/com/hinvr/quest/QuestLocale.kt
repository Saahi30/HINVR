package com.hinvr.quest

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.core.content.edit
import java.util.Locale

/**
 * English or Hindi in every panel. The choice is applied inside Compose, so switching it
 * redraws the panels in place instead of restarting the VR activity. Until the member picks,
 * the system's (or the per-app setting's) language decides.
 */
object QuestLocale {
    private var prefs: SharedPreferences? = null
    private var system = "en"

    /** "en", "hi", or null to follow the system. */
    var choice by mutableStateOf<String?>(null)
        private set

    val tag: String get() = choice ?: system

    val hindi: Boolean get() = tag == "hi"

    fun attach(context: Context) {
        val store = context.applicationContext.getSharedPreferences("hinvr_quest_prefs", Context.MODE_PRIVATE)
        prefs = store
        system = normalize(context.resources.configuration.locales[0]?.language)
        choice = store.getString(Key, null)?.let(::normalize)
    }

    /** Flips between the two languages and remembers the choice. */
    fun toggle() {
        val next = if (hindi) "en" else "hi"
        choice = next
        prefs?.edit { putString(Key, next) }
    }

    fun normalize(language: String?): String = if (language == "hi") "hi" else "en"

    private const val Key = "language"
}

private val Devanagari = FontFamily(Font(R.font.noto_sans_devanagari))

/**
 * Wraps one panel's content in the chosen language, with a Devanagari face for Hindi.
 * The headset app ships as one APK, not a bundle split by language, so every locale is present.
 */
@SuppressLint("AppBundleLocaleChanges")
@Composable
fun ProvideQuestLanguage(content: @Composable () -> Unit) {
    val tag = QuestLocale.tag
    val base = LocalContext.current
    val root = remember { base }
    val live = LocalConfiguration.current
    val config = remember(tag, live) {
        Configuration(live).apply {
            val locale = Locale.forLanguageTag(tag)
            setLocale(locale)
            setLayoutDirection(locale)
        }
    }
    // Keep the activity in the context chain; only the resources change.
    val localized = remember(config) {
        object : ContextWrapper(root) {
            private val resourcesOverride = root.createConfigurationContext(config).resources
            override fun getResources() = resourcesOverride
        }
    }
    val style = LocalTextStyle.current
    CompositionLocalProvider(
        LocalContext provides localized,
        LocalConfiguration provides config,
        LocalTextStyle provides if (tag == "hi") style.copy(fontFamily = Devanagari) else style,
        content = content,
    )
}
