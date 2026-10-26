package com.hinvr.quest

import android.content.Context

enum class Ambience(val volume: Float) {
    Off(0f),
    Hall(0.10f),
    Mandir(0.16f),
}

/**
 * The aarti chant under the hall and the sanctum, and the bell that greets the member.
 * Both are credited CC0 recordings from assets/mandir/audio (see ASSET_CREDITS.md).
 */
object SanctumSound {
    private var appContext: Context? = null
    private var chant: LoopPlayer? = null
    private var mode = Ambience.Off

    fun attach(context: Context) {
        appContext = context.applicationContext
    }

    /** Cheap to call every frame: it only acts when the wanted ambience changes. */
    fun sync(wanted: Ambience) {
        if (wanted == mode) return
        mode = wanted
        if (wanted == Ambience.Off) {
            chant?.stop()
            return
        }
        val context = appContext ?: return
        val loop = chant ?: LoopPlayer(context.assets, "mandir/audio/aarti_chant.wav", crossfadeMs = 5_000) {
            mode.volume * AudioMix.ambienceGain
        }.also { chant = it }
        if (!AudioMix.held) loop.play()
    }

    /** The diya catches at the end of the greeting. */
    fun welcome() {
        MandirSound.flat("aarti_bell", 0.7f)
    }

    fun onHold(held: Boolean) {
        val loop = chant ?: return
        if (held) loop.pause() else if (mode != Ambience.Off) loop.play()
    }

    fun release() {
        chant?.release()
        chant = null
        mode = Ambience.Off
        appContext = null
    }
}
