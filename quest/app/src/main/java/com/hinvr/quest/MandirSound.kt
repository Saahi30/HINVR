package com.hinvr.quest

import android.content.Context
import com.meta.spatial.core.Entity
import com.meta.spatial.core.Vector3
import com.meta.spatial.runtime.Scene
import com.meta.spatial.runtime.SceneAudioAsset
import java.lang.ref.WeakReference

/** Spatial one-shots at each offering, and the diya's crackle while it burns. */
object MandirSound {
    private var appContext: Context? = null
    private var scene = WeakReference<Scene>(null)
    private val clips = HashMap<String, SceneAudioAsset>()
    private var crackle: LoopPlayer? = null
    private var visiting = false
    private var diya = false

    fun attach(context: Context, host: Scene) {
        appContext = context.applicationContext
        scene = WeakReference(host)
        listOf(
            "ghanta", "ghanta_peal", "aarti_bell", "shankh",
            "coins", "coconut", "ignite",
        ).forEach { name ->
            if (name in clips) return@forEach
            val asset = runCatching {
                SceneAudioAsset.loadLocalFile("mandir/audio/$name.wav")
            }.getOrNull()
            if (asset != null) clips[name] = asset
        }
    }

    fun startVisit() {
        visiting = true
        diya = false
    }

    fun stopVisit() {
        visiting = false
        diya = false
        crackle?.stop()
    }

    fun setDiya(lit: Boolean) {
        if (!visiting) return
        diya = lit
        if (!lit) {
            crackle?.stop()
            return
        }
        val context = appContext ?: return
        val loop = crackle ?: LoopPlayer(context.assets, "mandir/audio/crackle.wav", crossfadeMs = 700) {
            0.28f * AudioMix.ambienceGain
        }.also { crackle = it }
        if (!AudioMix.held) loop.play()
    }

    fun onHold(held: Boolean) {
        val loop = crackle ?: return
        if (held) loop.pause() else if (visiting && diya) loop.play()
    }

    fun at(name: String, where: Vector3, volume: Float = 1f) {
        val world = scene.get() ?: return
        val clip = clips[name] ?: return
        val gain = volume * AudioMix.level.gain
        if (gain <= 0f || AudioMix.held) return
        runCatching { world.playSound(clip, where, gain) }
    }

    fun on(name: String, entity: Entity, volume: Float = 1f) {
        val world = scene.get() ?: return
        val clip = clips[name] ?: return
        val gain = volume * AudioMix.level.gain
        if (gain <= 0f || AudioMix.held) return
        runCatching { world.playSound(clip, entity, gain) }
            .onFailure { at(name, Vector3(0f, 1.4f, -1f), volume) }
    }

    /** Not placed anywhere: the greeting's bell. */
    fun flat(name: String, volume: Float) {
        val world = scene.get() ?: return
        val clip = clips[name] ?: return
        val gain = volume * AudioMix.level.gain
        if (gain <= 0f || AudioMix.held) return
        runCatching { world.playSound(clip, gain) }
    }

    fun aartiMoment(where: Vector3) {
        at("ghanta_peal", where, 0.95f)
        at("shankh", where, 0.88f)
        at("aarti_bell", where, 0.7f)
    }

    fun release() {
        crackle?.release()
        crackle = null
        visiting = false
    }
}
