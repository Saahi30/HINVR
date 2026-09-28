package com.hinvr.quest

import android.content.Context
import android.media.MediaPlayer
import android.view.Surface
import java.io.File

object SpherePlayer {
    private var appContext: Context? = null
    private var surface: Surface? = null
    private var player: MediaPlayer? = null
    private var pending: File? = null
    private var pausedByApp = false

    fun attach(context: Context) {
        appContext = context.applicationContext
    }

    fun attachSurface(target: Surface) {
        surface = target
        pending?.let { start(it) }
    }

    fun fileFor(tour: HomeChoice): File? {
        val context = appContext ?: return null
        if (tour.localFile.isBlank()) return null
        val file = File(context.getExternalFilesDir(null), "Tours/${tour.localFile}")
        return file.takeIf { it.isFile && it.length() > 0 }
    }

    fun play(tour: HomeChoice): Boolean {
        val file = fileFor(tour) ?: return false
        stop()
        pending = file
        if (surface != null) start(file)
        return true
    }

    fun replay() {
        val current = player ?: return
        runCatching {
            current.seekTo(0)
            current.start()
        }
    }

    fun pauseForApp() {
        val current = player ?: return
        if (runCatching { current.isPlaying }.getOrDefault(false)) {
            runCatching { current.pause() }
            pausedByApp = true
        }
    }

    fun resumeForApp() {
        if (!pausedByApp) return
        pausedByApp = false
        runCatching { player?.start() }
    }

    fun stop() {
        pending = null
        pausedByApp = false
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
    }

    private fun start(file: File) {
        val target = surface ?: return
        pending = null
        player = runCatching {
            MediaPlayer().apply {
                setSurface(target)
                setDataSource(file.path)
                setOnPreparedListener { it.start() }
                setOnCompletionListener { Ring.markTourEnded() }
                setOnErrorListener { _, _, _ ->
                    Ring.markTourEnded()
                    true
                }
                prepareAsync()
            }
        }.getOrNull()
        if (player == null) Ring.markTourEnded()
    }
}
