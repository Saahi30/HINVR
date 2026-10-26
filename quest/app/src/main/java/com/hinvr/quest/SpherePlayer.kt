package com.hinvr.quest

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import android.view.Surface
import java.io.File

/** Plays a downloaded 360 tour onto the sphere panel's surface. */
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
        val file = fileFor(tour)
        if (file == null) {
            Ring.sphereStatus(SphereStatus.Failed)
            return false
        }
        stop()
        pending = file
        if (surface != null) start(file)
        return true
    }

    fun replay() {
        val current = player
        if (current == null) {
            Ring.sphere?.tour?.let { play(it) }
            return
        }
        runCatching {
            current.seekTo(0)
            current.start()
            Ring.sphereStatus(SphereStatus.Playing)
        }.onFailure { fail("replay", it) }
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
                setOnPreparedListener {
                    it.start()
                    Ring.sphereStatus(SphereStatus.Playing)
                }
                setOnCompletionListener { Ring.sphereStatus(SphereStatus.Ended) }
                setOnErrorListener { _, what, extra ->
                    fail("playback $what/$extra", null)
                    true
                }
                prepareAsync()
            }
        }.onFailure { fail("open ${file.name}", it) }.getOrNull()
    }

    /** A broken file or decoder is a failure, never "darshan complete". */
    private fun fail(what: String, error: Throwable?) {
        Log.w("HinvrSphere", "Tour failed: $what", error)
        runCatching { player?.release() }
        player = null
        Ring.sphereStatus(SphereStatus.Failed)
    }
}
