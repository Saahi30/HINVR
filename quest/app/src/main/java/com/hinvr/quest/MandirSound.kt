package com.hinvr.quest

import android.content.Context
import com.meta.spatial.core.Entity
import com.meta.spatial.core.Pose
import com.meta.spatial.core.Vector3
import com.meta.spatial.runtime.Scene
import com.meta.spatial.runtime.SceneAudioAsset
import java.lang.ref.WeakReference
import kotlin.math.atan2
import kotlin.math.sin

/**
 * Spatial one-shots at each offering, the diya's crackle while it burns, and the room's own
 * ambience: a quiet chant from the garbha and the lamp stands crackling, each loudest near it
 * and heard from its side.
 */
object MandirSound {
    /** The member's head, updated each frame while inside; the ambience pans and fades from it. */
    var listener = Pose(Vector3(0f, 1.6f, 0.05f))

    private val beds = ArrayList<LoopPlayer>()
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
        val context = appContext ?: return
        if (beds.isEmpty()) {
            val garbha = MandirPlace.garbha + Vector3(0f, 1.1f, 0f)
            beds += placed(context, "mandir/audio/aarti_chant.wav", garbha, base = 0.30f, reach = 3f, crossfadeMs = 5_000) {
                1f + 1.5f * MandirCelebration.level()
            }
            lampStands().forEach { stand ->
                beds += placed(context, "mandir/audio/crackle.wav", stand, base = 0.18f, reach = 1.2f, crossfadeMs = 700)
            }
        }
        if (!AudioMix.held) beds.forEach { it.play() }
    }

    fun stopVisit() {
        visiting = false
        diya = false
        crackle?.stop()
        beds.forEach { it.stop() }
    }

    /** The two tall lamp stands by the garbha door: the room's lamps away from the walls and the garbha. */
    private fun lampStands(): List<Vector3> =
        MandirPlace.lamps.filter { kotlin.math.abs(it.x) in 0.9f..2f && it.z < -2f && it.z > -4f && it.y > 1f }
            .ifEmpty { listOf(Vector3(-1.25f, 1.22f, -3.15f), Vector3(1.25f, 1.22f, -3.15f)) }

    /** A loop that sounds from [at]: quieter with distance past [reach], panned to its side. */
    private fun placed(
        context: Context,
        path: String,
        at: Vector3,
        base: Float,
        reach: Float,
        crossfadeMs: Int,
        boost: () -> Float = { 1f },
    ) = LoopPlayer(context.assets, path, crossfadeMs, balance = { panTo(at) }) {
        val head = listener.t
        val dx = at.x - head.x
        val dy = at.y - head.y
        val dz = at.z - head.z
        val near = (dx * dx + dy * dy + dz * dz) / (reach * reach)
        base * boost() * AudioMix.ambienceGain / (1f + near)
    }

    /** -1 when [at] is on the member's left, 1 on the right. */
    private fun panTo(at: Vector3): Float {
        val head = listener
        val toward = Math.toDegrees(atan2(at.x - head.t.x, at.z - head.t.z).toDouble()).toFloat()
        val relative = wrapDegrees(toward - yawOf(head))
        // Yaw grows toward the member's left (it's measured from +Z toward +X), so right is negative.
        return -sin(Math.toRadians(relative.toDouble())).toFloat()
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
        if (held) beds.forEach { it.pause() } else if (visiting) beds.forEach { it.play() }
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
        beds.forEach { it.release() }
        beds.clear()
        visiting = false
    }
}
