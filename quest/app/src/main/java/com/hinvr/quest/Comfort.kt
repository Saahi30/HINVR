package com.hinvr.quest

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

/** Seated or standing in the sanctum. Remembered across visits. */
object Comfort {
    /** A seated member's eyes are lower: the room comes up this much, and the mark this much nearer the stand. */
    const val SeatedLift = 0.30f
    const val SeatedStep = 0.12f

    /** Each push of the thumbstick turns the member this far, behind a fade. */
    const val SnapDegrees = 30f

    private var prefs: SharedPreferences? = null

    var seated by mutableStateOf(false)
        private set

    fun attach(context: Context) {
        val store = context.applicationContext.getSharedPreferences("hinvr_quest_prefs", Context.MODE_PRIVATE)
        prefs = store
        seated = store.getBoolean("seated", false)
    }

    fun toggleSeated() {
        seated = !seated
        prefs?.edit { putBoolean("seated", seated) }
        MandirScene.applyComfort()
    }
}
