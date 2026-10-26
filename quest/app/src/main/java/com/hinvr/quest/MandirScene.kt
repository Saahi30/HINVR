package com.hinvr.quest

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import com.meta.spatial.core.Entity
import com.meta.spatial.core.Pose
import com.meta.spatial.core.SystemManager
import com.meta.spatial.core.Vector3
import com.meta.spatial.runtime.Scene
import com.meta.spatial.toolkit.Light
import com.meta.spatial.toolkit.LightType
import com.meta.spatial.toolkit.Mesh
import com.meta.spatial.toolkit.MeshCollision
import com.meta.spatial.toolkit.SceneObjectSystem
import com.meta.spatial.toolkit.Transform
import com.meta.spatial.toolkit.Visible
import java.lang.ref.WeakReference
import kotlin.math.atan2

object MandirWorld {
    var room: Entity? = null
    var ceiling: Entity? = null
    var garbhaLight: Entity? = null
    var diyaLight: Entity? = null
    var incenseLight: Entity? = null
    var darshan: Panel? = null
    var sankalp: Panel? = null
    var guide: Panel? = null
    var blessing: Panel? = null
    var flame: Panel? = null
    var smoke: Panel? = null
    var tilak: Panel? = null
    val props = HashMap<String, Entity>()

    val panels: List<Panel> by lazy { listOfNotNull(darshan, sankalp, guide, blessing, flame, smoke, tilak) }
}

object MandirScene {
    private const val Tag = "HinvrMandir"

    /** The room is ~7 MB of textures; past this we stop waiting and show what has loaded. */
    private const val LoadTimeoutMs = 10_000L

    private val main = Handler(Looper.getMainLooper())
    private var scene = WeakReference<Scene>(null)
    private var systems: SystemManager? = null
    private var built = false
    private var teleport = 0
    private var loadStartedAt = 0L
    private var watchingLoad = false

    /** False until the room's mesh has finished loading. */
    var ready by mutableStateOf(false)
        private set

    var diyaPose = Pose(Vector3(0f))
        internal set
    var incensePose = Pose(Vector3(0f))
        internal set

    /** Where each prop rests. Built from markers.json once it has been read. */
    var homes: Map<String, Vector3> = emptyMap()
        private set

    fun attach(host: Scene, systemManager: SystemManager) {
        scene = WeakReference(host)
        systems = systemManager
        val stand = MandirPlace.stand
        homes = mapOf(
            "ghanta" to MandirPlace.ghanta,
            "diya" to stand + Vector3(-0.14f, 0.06f, 0.02f),
            "aarti_thali" to stand + Vector3(0.16f, 0.05f, 0.04f),
            "agarbatti" to stand + Vector3(0.32f, 0.07f, 0.02f),
            "marigold" to stand + Vector3(-0.28f, 0.05f, 0.08f),
            "lotus" to stand + Vector3(0.28f, 0.05f, 0.08f),
            "coconut" to stand + Vector3(-0.08f, 0.05f, 0.18f),
            "laddoo" to stand + Vector3(0.08f, 0.05f, 0.18f),
            "kumkum" to stand + Vector3(-0.22f, 0.05f, -0.04f),
            // On the stand, where a seated member can reach it, not at the peti on the floor.
            "coin" to stand + Vector3(0.22f, 0.05f, -0.06f),
        )
        diyaPose = Pose(homes.getValue("diya"))
        incensePose = Pose(MandirPlace.incense + Vector3(0f, 0.08f, 0f))
    }

    fun show() {
        if (scene.get() == null) return
        if (!built) build()
        setVisible(true)
        teleport = 0
        // Stay behind the fade until the room is there, rather than standing in an empty void.
        ViewRig.holdBlack = !ready
        ViewRig.placeHead(MandirPlace.marks[0], facingGarbha(MandirPlace.marks[0]))
        MandirSound.startVisit()
    }

    fun hide() {
        setVisible(false)
        ViewRig.holdBlack = false
        ViewRig.reset(faded = false)
        ViewRig.fadeFromBlack()
    }

    /** Called each frame while inside, until the room has loaded. */
    fun watchLoad() {
        if (ready) return
        val room = MandirWorld.room ?: return
        if (SystemClock.elapsedRealtime() - loadStartedAt > LoadTimeoutMs) {
            Log.w(Tag, "Room still loading after ${LoadTimeoutMs}ms; showing it anyway")
            markReady()
            return
        }
        if (watchingLoad) return
        val future = runCatching { systems?.findSystem<SceneObjectSystem>()?.getSceneObject(room) }.getOrNull() ?: return
        watchingLoad = true
        future.whenComplete { _, error ->
            if (error != null) Log.w(Tag, "Room failed to load", error)
            main.post { markReady() }
        }
    }

    private fun markReady() {
        if (ready) return
        ready = true
        ViewRig.holdBlack = false
    }

    fun nextTeleport() {
        if (ViewRig.busy) return
        teleport = (teleport + 1) % MandirPlace.marks.size
        val mark = MandirPlace.marks[teleport]
        ViewRig.placeHead(mark, facingGarbha(mark))
        Mandir.setHint(teleportHint(teleport))
    }

    /** The member's view origin moved under them (system recentre). Put them back on their mark. */
    fun recentre() {
        val mark = MandirPlace.marks[teleport]
        ViewRig.placeHead(mark, facingGarbha(mark))
    }

    @StringRes
    private fun teleportHint(mark: Int): Int = when (mark) {
        1 -> R.string.hint_threshold
        2 -> R.string.hint_pradakshina
        else -> R.string.hint_mandap
    }

    private fun facingGarbha(from: Vector3): Float {
        val garbha = MandirPlace.garbha
        return Math.toDegrees(atan2(garbha.x - from.x, garbha.z - from.z).toDouble()).toFloat()
    }

    fun setDiyaLight(on: Boolean, where: Vector3) {
        MandirWorld.diyaLight?.let { light ->
            light.setComponent(Visible(on))
            light.setComponent(Transform(Pose(where + Vector3(0f, 0.08f, 0f))))
        }
        MandirSound.setDiya(on)
    }

    fun setIncenseLight(on: Boolean, where: Vector3) {
        MandirWorld.incenseLight?.let { light ->
            light.setComponent(Visible(on))
            light.setComponent(Transform(Pose(where + Vector3(0f, 0.12f, 0f))))
        }
    }

    private fun build() {
        built = true
        loadStartedAt = SystemClock.elapsedRealtime()
        MandirWorld.room = Entity.create(
            listOf(
                Mesh("mandir/sanctum.glb".toUri(), hittable = MeshCollision.NoCollision),
                Transform(Pose(Vector3(0f, 0f, 0f))),
                Visible(true),
            ),
        )
        MandirWorld.ceiling = Entity.create(
            listOf(
                Transform(Pose(Vector3(0f, 3.4f, -0.4f))),
                Light(
                    type = LightType.POINT,
                    color = Vector3(1f, 0.72f, 0.42f),
                    intensity = 4.5f,
                    range = 12f,
                ),
            ),
        )
        MandirWorld.garbhaLight = Entity.create(
            listOf(
                Transform(Pose(MandirPlace.darshan + Vector3(0f, 0.35f, 0.4f))),
                Light(
                    type = LightType.SPOT,
                    color = Vector3(1f, 0.78f, 0.48f),
                    intensity = 3.2f,
                    range = 4.5f,
                ),
            ),
        )
        MandirWorld.diyaLight = Entity.create(
            listOf(
                Transform(Pose(MandirPlace.stand)),
                Light(
                    type = LightType.POINT,
                    color = Vector3(1f, 0.55f, 0.22f),
                    intensity = 2.4f,
                    range = 3.2f,
                ),
                Visible(false),
            ),
        )
        MandirWorld.incenseLight = Entity.create(
            listOf(
                Transform(Pose(MandirPlace.incense)),
                Light(
                    type = LightType.POINT,
                    color = Vector3(0.9f, 0.45f, 0.18f),
                    intensity = 0.7f,
                    range = 1.4f,
                ),
                Visible(false),
            ),
        )
        homes.forEach { (name, where) ->
            MandirWorld.props[name] = Entity.create(
                listOf(
                    Mesh("mandir/props/$name.glb".toUri(), hittable = MeshCollision.NoCollision),
                    Transform(Pose(where)),
                    Visible(true),
                ),
            )
        }
    }

    private fun setVisible(show: Boolean) {
        listOfNotNull(
            MandirWorld.room,
            MandirWorld.ceiling,
            MandirWorld.garbhaLight,
        ).forEach { it.setComponent(Visible(show)) }
        MandirWorld.diyaLight?.setComponent(Visible(show && Mandir.diyaLit))
        MandirWorld.incenseLight?.setComponent(Visible(show && Mandir.agarbattiLit))
        MandirWorld.props.values.forEach { it.setComponent(Visible(show)) }
    }
}
