package com.hinvr.quest

import android.content.Context
import android.media.MediaPlayer

object SanctumSound {
    private var appContext: Context? = null
    private var bell: MediaPlayer? = null
    private var loop: MediaPlayer? = null
    private var bellStarted = false
    private var loopMissing = false
    private var playing = false
    private var held = false

    fun attach(context: Context) {
        appContext = context.applicationContext
    }

    fun onFrame(stage: Stage, watching: Boolean, diyaLit: Boolean) {
        val context = appContext ?: return
        if (!diyaLit) bellStarted = false
        if (diyaLit && !bellStarted) {
            bellStarted = true
            runCatching { bell?.release() }
            bell = create(context, "temple_bell", looping = false)
            bell?.start()
        }
        val bellBusy = runCatching { bell?.isPlaying == true }.getOrDefault(false)
        val wantLoop = stage != Stage.Splash && !watching && !held && !bellBusy
        if (!wantLoop) {
            if (playing) {
                runCatching { loop?.pause() }
                playing = false
            }
            return
        }
        if (loop == null && !loopMissing) {
            loop = create(context, "sanctum_loop", looping = true)
            if (loop == null) loopMissing = true
        }
        if (loop != null && !playing) {
            runCatching { loop?.start() }
            playing = true
        }
    }

    fun hold() {
        held = true
        if (playing) {
            runCatching { loop?.pause() }
            playing = false
        }
    }

    fun releaseHold() {
        held = false
    }

    fun release() {
        runCatching { bell?.release() }
        runCatching { loop?.release() }
        bell = null
        loop = null
        playing = false
        appContext = null
    }

    private fun create(context: Context, name: String, looping: Boolean): MediaPlayer? {
        val id = context.resources.getIdentifier(name, "raw", context.packageName)
        if (id == 0) return null
        return runCatching {
            MediaPlayer.create(context, id)?.apply {
                isLooping = looping
                val level = if (looping) 0.22f else 0.85f
                setVolume(level, level)
            }
        }.getOrNull()
    }
}
