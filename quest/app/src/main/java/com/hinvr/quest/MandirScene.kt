package com.hinvr.quest

import android.net.Uri
import com.meta.spatial.core.Entity
import com.meta.spatial.core.Pose
import com.meta.spatial.core.Quaternion
import com.meta.spatial.core.Vector3
import com.meta.spatial.runtime.Scene
import com.meta.spatial.toolkit.Light
import com.meta.spatial.toolkit.LightType
import com.meta.spatial.toolkit.Mesh
import com.meta.spatial.toolkit.MeshCollision
import com.meta.spatial.toolkit.Transform
import com.meta.spatial.toolkit.Visible
import java.lang.ref.WeakReference

object MandirWorld {
    var room: Entity? = null
    var ceiling: Entity? = null
    var garbhaLight: Entity? = null
    var diyaLight: Entity? = null
    var incenseLight: Entity? = null
    var darshan: Entity? = null
    var sankalp: Entity? = null
    var hint: Entity? = null
    var blessing: Entity? = null
    var courtyard: Entity? = null
    var flame: Entity? = null
    var smoke: Entity? = null
    var tilak: Entity? = null
    val props = HashMap<String, Entity>()
}

object MandirScene {
    private var scene = WeakReference<Scene>(null)
    private var built = false
    private var teleport = 0
    var diyaPose = Pose(MandirPlace.stand + Vector3(-0.14f, 0.06f, 0.02f))
        internal set
    var incensePose = Pose(MandirPlace.incense + Vector3(0f, 0.08f, 0f))
        internal set

    val homes: Map<String, Vector3> = mapOf(
        "ghanta" to MandirPlace.ghanta,
        "diya" to MandirPlace.stand + Vector3(-0.14f, 0.06f, 0.02f),
        "aarti_thali" to MandirPlace.stand + Vector3(0.16f, 0.05f, 0.04f),
        "agarbatti" to MandirPlace.stand + Vector3(0.32f, 0.07f, 0.02f),
        "marigold" to MandirPlace.stand + Vector3(-0.28f, 0.05f, 0.08f),
        "lotus" to MandirPlace.stand + Vector3(0.28f, 0.05f, 0.08f),
        "coconut" to MandirPlace.stand + Vector3(-0.08f, 0.05f, 0.18f),
        "laddoo" to MandirPlace.stand + Vector3(0.08f, 0.05f, 0.18f),
        "kumkum" to MandirPlace.stand + Vector3(-0.22f, 0.05f, -0.04f),
        "coin" to MandirPlace.peti + Vector3(-0.18f, 0.22f, 0.08f),
    )

    fun attach(host: Scene) {
        scene = WeakReference(host)
    }

    fun show() {
        val world = scene.get() ?: return
        if (!built) build(world)
        setVisible(true)
        teleport = 0
        world.setViewOrigin(MandirPlace.spawn.x, MandirPlace.spawn.y, MandirPlace.spawn.z, 0f)
        MandirSound.startVisit()
    }

    fun hide() {
        val world = scene.get() ?: return
        setVisible(false)
        world.setViewOrigin(0f, 0f, 0f, 0f)
    }

    fun nextTeleport() {
        val world = scene.get() ?: return
        teleport = (teleport + 1) % MandirPlace.marks.size
        val mark = MandirPlace.marks[teleport]
        world.setViewOrigin(mark.x, mark.y, mark.z, 0f)
        Mandir.hint = when (teleport) {
            1 -> "Threshold. The garbha griha is ahead."
            2 -> "Pradakshina. Walk the mandap clockwise."
            else -> "The mandap. Offerings are on the stand."
        }
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

    private fun build(world: Scene) {
        built = true
        MandirWorld.room = Entity.create(
            listOf(
                Mesh(Uri.parse("mandir/sanctum.glb"), hittable = MeshCollision.NoCollision),
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
                    intensity = 1.6f,
                    range = 9f,
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
                    Mesh(Uri.parse("mandir/props/$name.glb"), hittable = MeshCollision.NoCollision),
                    Transform(Pose(where)),
                    Visible(true),
                ),
            )
        }
        world.setLightingEnvironment(
            ambientColor = Vector3(0.09f, 0.05f, 0.03f),
            sunColor = Vector3(0.55f, 0.32f, 0.14f),
            sunDirection = -Vector3(0.2f, 1f, 0.15f),
            environmentIntensity = 0.22f,
        )
    }

    private fun setVisible(show: Boolean) {
        listOfNotNull(
            MandirWorld.room,
            MandirWorld.ceiling,
            MandirWorld.garbhaLight,
            MandirWorld.courtyard,
        ).forEach { it.setComponent(Visible(show)) }
        MandirWorld.diyaLight?.setComponent(Visible(show && Mandir.diyaLit))
        MandirWorld.incenseLight?.setComponent(Visible(show && Mandir.agarbattiLit))
        MandirWorld.props.values.forEach { it.setComponent(Visible(show)) }
        if (!show) {
            listOfNotNull(
                MandirWorld.darshan,
                MandirWorld.sankalp,
                MandirWorld.hint,
                MandirWorld.blessing,
                MandirWorld.flame,
                MandirWorld.smoke,
                MandirWorld.tilak,
            ).forEach { it.setComponent(Visible(false)) }
        }
    }
}
