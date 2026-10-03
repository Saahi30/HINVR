package com.hinvr.quest

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.meta.spatial.core.Entity
import com.meta.spatial.core.Vector3
import com.meta.spatial.runtime.Scene
import com.meta.spatial.runtime.SceneAudioAsset
import java.lang.ref.WeakReference

object MandirSound {
    private var appContext: Context? = null
    private var scene = WeakReference<Scene>(null)
    private val clips = HashMap<String, SceneAudioAsset>()
    private var chant: MediaPlayer? = null
    private var crackle: MediaPlayer? = null
    private var visiting = false

    fun attach(context: Context, host: Scene) {
        appContext = context.applicationContext
        scene = WeakReference(host)
        listOf(
            "ghanta", "ghanta_peal", "aarti_bell", "shankh",
            "coins", "coconut", "crackle", "ignite",
        ).forEach { name ->
            if (name in clips) return@forEach
            val asset = runCatching {
                SceneAudioAsset.loadLocalFile("mandir/audio/$name.wav")
            }.getOrNull()
            if (asset != null) clips[name] = asset
        }
    }

    fun startVisit() {
        if (visiting) return
        visiting = true
        chant = loop("aarti_chant", 0.16f)
        chant?.start()
    }

    fun stopVisit() {
        visiting = false
        runCatching { chant?.stop() }
        runCatching { chant?.release() }
        runCatching { crackle?.stop() }
        runCatching { crackle?.release() }
        chant = null
        crackle = null
    }

    fun setDiya(lit: Boolean) {
        if (!visiting) return
        if (lit) {
            if (crackle == null) crackle = loop("crackle", 0.28f)
            if (crackle?.isPlaying != true) runCatching { crackle?.start() }
        } else {
            runCatching { crackle?.pause() }
        }
    }

    fun at(name: String, where: Vector3, volume: Float = 1f) {
        val world = scene.get() ?: return
        val clip = clips[name] ?: return
        runCatching { world.playSound(clip, where, volume) }
    }

    fun on(name: String, entity: Entity, volume: Float = 1f) {
        val world = scene.get() ?: return
        val clip = clips[name] ?: return
        runCatching { world.playSound(clip, entity, volume) }
            .onFailure { at(name, Vector3(0f, 1.4f, -1f), volume) }
    }

    fun aartiMoment(where: Vector3) {
        at("ghanta_peal", where, 0.95f)
        at("shankh", where, 0.88f)
        at("aarti_bell", where, 0.7f)
    }

    private fun loop(name: String, volume: Float): MediaPlayer? {
        val context = appContext ?: return null
        return runCatching {
            val fd = context.assets.openFd("mandir/audio/$name.wav")
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                fd.close()
                isLooping = true
                setVolume(volume, volume)
                prepare()
            }
        }.getOrNull()
    }
}
