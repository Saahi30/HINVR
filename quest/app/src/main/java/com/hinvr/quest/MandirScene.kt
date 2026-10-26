package com.hinvr.quest

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import com.meta.spatial.core.Color4
import com.meta.spatial.core.Entity
import com.meta.spatial.core.Pose
import com.meta.spatial.core.Quaternion
import com.meta.spatial.core.SystemManager
import com.meta.spatial.core.Vector3
import com.meta.spatial.runtime.Scene
import com.meta.spatial.toolkit.Light
import com.meta.spatial.toolkit.LightType
import com.meta.spatial.toolkit.Material
import com.meta.spatial.toolkit.Mesh
import com.meta.spatial.toolkit.MeshCollision
import com.meta.spatial.toolkit.Scale
import com.meta.spatial.toolkit.SceneObjectSystem
import com.meta.spatial.toolkit.Sphere
import com.meta.spatial.toolkit.Transform
import com.meta.spatial.toolkit.Visible
import java.lang.ref.WeakReference
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object MandirWorld {
    var room: Entity? = null
    var ceiling: Entity? = null
    var garbhaLight: Entity? = null
    var diyaLight: Entity? = null
    var incenseLight: Entity? = null
    var jyotiLight: Entity? = null
    var darshan: Panel? = null
    var sankalp: Panel? = null
    var guide: Panel? = null
    var blessing: Panel? = null
    var flame: Panel? = null
    var smoke: Panel? = null
    var tilak: Panel? = null
    var jyoti: Panel? = null
    var thaliFlame: Panel? = null
    var label: Panel? = null
    var japa: Panel? = null

    /** The first visit's walk-through: a ring around what to touch next. */
    var tutorial: Panel? = null
    val halos: Array<Panel?> = arrayOfNulls(HaloCount)
    val props = HashMap<String, Entity>()

    /** The deepmala tray (not something to pick up) and the flame over each of its diyas. */
    var deepmala: Entity? = null
    val deepmalaFlames = ArrayList<Entity>()

    /** Water drops for the abhishek, reused for every pour. */
    val drops = ArrayList<Entity>()

    val panels: List<Panel> by lazy {
        listOfNotNull(darshan, sankalp, guide, blessing, flame, smoke, tilak, jyoti, thaliFlame, label, japa, tutorial) +
            halos.filterNotNull()
    }

    /** Glows over the room's own flames; markers.json lists seven. */
    const val HaloCount = 8
}

object MandirScene {
    private const val Tag = "HinvrMandir"

    /** The room is ~7 MB of textures; past this we stop waiting and show what has loaded. */
    private const val LoadTimeoutMs = 10_000L

    private val main = Handler(Looper.getMainLooper())
    private var scene = WeakReference<Scene>(null)
    private var systems: SystemManager? = null
    private var built = false
    private var loadStartedAt = 0L
    private var watchingLoad = false

    /** False until the room's mesh has finished loading. */
    var ready by mutableStateOf(false)
        private set

    /** Which mark the member stands on: 0 at the stand, 1 at the threshold. */
    var mark by mutableIntStateOf(0)
        private set

    /** Snap turns since arriving on the mark, in degrees. */
    private var turn = 0f

    /** The room in daylight (05:30 to 17:30 local) or by lamplight; picked as the member enters. */
    var daytime by mutableStateOf(false)
        private set
    private var loadedRoom = ""

    /** The ceiling light as last written, so the celebration only rewrites it when it changes. */
    private var ceilingWarmth = -1f

    /** Where the flames and smoke are drawn: the diya's wick, the agarbatti's tip, the held thali's camphor. */
    var diyaWick = Vector3(0f)
        internal set
    var incenseTip = Vector3(0f)
        internal set
    var thaliFlame = Vector3(0f)
        internal set

    /** Where each prop rests. Built from markers.json once it has been read. The ghanta's is its hook. */
    var homes: Map<String, Vector3> = emptyMap()
        private set

    /** Where today's festival offering waits on the side table, and the mala's place beside it. */
    val festivalHome: Vector3 get() = MandirPlace.side + Vector3(-0.07f, 0.003f, 0f)
    private val malaHome: Vector3 get() = MandirPlace.side + Vector3(0.17f, 0.003f, 0.08f)

    /** The bell hangs this far below its hook. */
    val ghantaDrop: Float get() = (MandirPlace.ghantaPivot.y - MandirPlace.ghanta.y).coerceAtLeast(0.3f)

    fun attach(host: Scene, systemManager: SystemManager) {
        scene = WeakReference(host)
        systems = systemManager
        // Props sit on their base, so a home is on the stand's top. The front row is nearest the member.
        val top = MandirPlace.stand + Vector3(0f, 0.003f, 0f)
        homes = mapOf(
            "ghanta" to MandirPlace.ghantaPivot,
            "marigold" to top + Vector3(-0.40f, 0f, 0.10f),
            "diya" to top + Vector3(-0.20f, 0f, 0.10f),
            "aarti_thali" to top + Vector3(0.03f, 0f, 0.10f),
            "kumkum" to top + Vector3(0.25f, 0f, 0.10f),
            "coin" to top + Vector3(0.40f, 0f, 0.10f),
            "lotus" to top + Vector3(-0.30f, 0f, -0.11f),
            "coconut" to top + Vector3(-0.10f, 0f, -0.11f),
            "laddoo" to top + Vector3(0.08f, 0f, -0.11f),
            "agarbatti" to top + Vector3(0.26f, 0f, -0.11f),
            "mala" to malaHome,
        )
        resetFlames()
    }

    /** The festival changed: its offering comes or goes from the side table. */
    fun festivalChanged() {
        val kind = Festivals.active?.kind
        val wanted = kind?.prop?.takeIf { it != "deepmala" }
        // Drop whatever festival prop is out, keep the everyday ones.
        festivalProps.filter { it != wanted }.forEach { name ->
            MandirWorld.props.remove(name)?.destroy()
        }
        homes = homes.filterKeys { it !in festivalProps } + (wanted?.let { mapOf(it to festivalHome) } ?: emptyMap())
        if (!built) return
        if (wanted != null && wanted !in MandirWorld.props) createProp(wanted, festivalHome)
        buildDeepmala(kind == FestivalKind.Deepmala)
        setVisible(Mandir.inside)
    }

    /** The tray's wick tops in the room. */
    fun deepmalaWicks(): List<Vector3> = MandirPlace.deepmalaWicks.map { festivalHome + it }

    /** Flames and smoke back over the props' homes. */
    fun resetFlames() {
        diyaWick = (homes["diya"] ?: MandirPlace.stand) + Vector3(0f, OfferingRules.WickHeight, 0f)
        incenseTip = (homes["agarbatti"] ?: MandirPlace.stand) + Vector3(0f, OfferingRules.IncenseTipHeight, 0f)
        thaliFlame = (homes["aarti_thali"] ?: MandirPlace.stand) + Vector3(0f, OfferingRules.ThaliFlameHeight, 0f)
    }

    fun show() {
        if (scene.get() == null) return
        daytime = isDaytime(LocalTime.now())
        if (!built) build() else loadRoom()
        festivalChanged()
        setVisible(true)
        mark = 0
        turn = 0f
        // Stay behind the fade until the room is there, rather than standing in an empty void.
        ViewRig.holdBlack = !ready
        ViewRig.setLift(if (Comfort.seated) Comfort.SeatedLift else 0f)
        ViewRig.placeHead(standingAt(0), facing(0))
        MandirSound.startVisit()
    }

    fun hide() {
        setVisible(false)
        ViewRig.setLift(0f)
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

    /** A / X: step up to the threshold, or back to the stand. */
    fun nextTeleport() {
        if (ViewRig.busy) return
        mark = (mark + 1) % MandirPlace.marks.size
        turn = 0f
        ViewRig.placeHead(standingAt(mark), facing(mark))
        Mandir.setEvent(teleportHint(mark))
    }

    /** The member's view origin moved under them (system recentre). Put them back on their mark. */
    fun recentre() {
        ViewRig.placeHead(standingAt(mark), facing(mark))
    }

    /** Thumbstick left or right: a fixed turn behind a short fade, never a smooth spin. */
    fun snapTurn(degrees: Float) {
        if (ViewRig.busy || !Mandir.inside) return
        turn = wrapDegrees(turn + degrees)
        ViewRig.placeHead(standingAt(mark), facing(mark))
    }

    /** Seated mode was switched: raise or lower the room and step the mark. */
    fun applyComfort() {
        if (!Mandir.inside) return
        ViewRig.setLift(if (Comfort.seated) Comfort.SeatedLift else 0f)
        ViewRig.placeHead(standingAt(mark), facing(mark))
    }

    /** Where the member's head goes on [index]: seated members a little nearer the stand. */
    private fun standingAt(index: Int): Vector3 {
        val at = MandirPlace.marks[index]
        if (!Comfort.seated) return at
        val dx = MandirPlace.garbha.x - at.x
        val dz = MandirPlace.garbha.z - at.z
        val length = sqrt(dx * dx + dz * dz).coerceAtLeast(1e-3f)
        return at + Vector3(dx / length * Comfort.SeatedStep, 0f, dz / length * Comfort.SeatedStep)
    }

    private fun facing(index: Int): Float = wrapDegrees(facingGarbha(MandirPlace.marks[index]) + turn)

    /** Called each frame while inside: the ceiling light warms and dims through the aarti moment. */
    fun tickLights() {
        val warmth = MandirCelebration.level()
        val settled = warmth == 0f && ceilingWarmth == 0f
        if (settled || (abs(warmth - ceilingWarmth) < 0.02f && warmth != 0f)) return
        ceilingWarmth = warmth
        val day = if (daytime) 0.8f else 1f
        MandirWorld.ceiling?.setComponent(
            Light(
                type = LightType.POINT,
                color = Vector3(1f, 0.72f - 0.12f * warmth, 0.42f - 0.16f * warmth),
                intensity = 4.5f * day * (1f - 0.25f * warmth),
                range = 12f,
            ),
        )
    }

    /** A small lit flame over deepmala diya [index]. */
    fun lightDeepmala(index: Int) {
        MandirWorld.deepmalaFlames.getOrNull(index)?.setComponent(Visible(Mandir.inside))
    }

    @StringRes
    private fun teleportHint(mark: Int): Int = if (mark == 1) R.string.hint_threshold else R.string.hint_mandap

    private fun facingGarbha(from: Vector3): Float {
        val garbha = MandirPlace.garbha
        return Math.toDegrees(atan2(garbha.x - from.x, garbha.z - from.z).toDouble()).toFloat()
    }

    /** The ghanta hanging from its hook, turned by a swing given as a rotation vector in radians. */
    fun ghantaPose(aboutX: Float, aboutZ: Float): Pose {
        val angle = sqrt(aboutX * aboutX + aboutZ * aboutZ)
        if (angle < 1e-6f) return Pose(MandirPlace.ghantaPivot)
        val s = sin(angle / 2f) / angle
        return Pose(MandirPlace.ghantaPivot, Quaternion(cos(angle / 2f), aboutX * s, 0f, aboutZ * s))
    }

    /** Where the bell itself is for a ghanta pose: below the hook, carried round by the swing. */
    fun bellCentre(pose: Pose): Vector3 = pose.t + pose.q.times(Vector3(0f, -ghantaDrop, 0f))

    fun setDiyaLight(on: Boolean, where: Vector3) {
        MandirWorld.diyaLight?.let { light ->
            light.setComponent(Visible(on))
            light.setComponent(Transform(Pose(where + Vector3(0f, 0.04f, 0f))))
        }
        MandirSound.setDiya(on)
    }

    fun setIncenseLight(on: Boolean, where: Vector3) {
        MandirWorld.incenseLight?.let { light ->
            light.setComponent(Visible(on))
            light.setComponent(Transform(Pose(where + Vector3(0f, 0.04f, 0f))))
        }
    }

    private fun roomFile(): String =
        if (daytime && MandirPlace.hasAsset("mandir/sanctum_day.glb")) "mandir/sanctum_day.glb" else "mandir/sanctum.glb"

    /** The room for this visit's light. Swapping it waits behind the fade like the first load. */
    private fun loadRoom() {
        val file = roomFile()
        if (file == loadedRoom) return
        loadedRoom = file
        val mesh = Mesh(file.toUri(), hittable = MeshCollision.NoCollision)
        val room = MandirWorld.room
        if (room == null) {
            MandirWorld.room = Entity.create(listOf(mesh, Transform(Pose(Vector3(0f, 0f, 0f))), Visible(true)))
        } else {
            room.setComponent(mesh)
        }
        ready = false
        watchingLoad = false
        loadStartedAt = SystemClock.elapsedRealtime()
    }

    private fun createProp(name: String, where: Vector3) {
        val file = "mandir/props/$name.glb"
        if (!MandirPlace.hasAsset(file)) {
            Log.w(Tag, "$file is missing; leaving it out")
            return
        }
        MandirWorld.props[name] = Entity.create(
            listOf(
                Mesh(file.toUri(), hittable = MeshCollision.NoCollision),
                Transform(Pose(where)),
                Scale(Vector3(1f)),
                Visible(true),
            ),
        )
    }

    /** The tray and its nine flames, out only for a deepmala festival. */
    private fun buildDeepmala(wanted: Boolean) {
        if (!wanted) {
            MandirWorld.deepmala?.destroy()
            MandirWorld.deepmala = null
            MandirWorld.deepmalaFlames.forEach { it.destroy() }
            MandirWorld.deepmalaFlames.clear()
            return
        }
        if (MandirWorld.deepmala != null) return
        if (!MandirPlace.hasAsset("mandir/props/deepmala.glb")) {
            Log.w(Tag, "deepmala.glb is missing; no deepmala today")
            return
        }
        MandirWorld.deepmala = Entity.create(
            listOf(
                Mesh("mandir/props/deepmala.glb".toUri(), hittable = MeshCollision.NoCollision),
                Transform(Pose(festivalHome)),
                Visible(true),
            ),
        )
        if (!MandirPlace.hasAsset("mandir/props/flame.glb")) return
        deepmalaWicks().forEachIndexed { index, wick ->
            MandirWorld.deepmalaFlames += Entity.create(
                listOf(
                    Mesh("mandir/props/flame.glb".toUri(), hittable = MeshCollision.NoCollision),
                    Transform(Pose(wick)),
                    Visible(index in Mandir.deepmalaLit),
                ),
            )
        }
    }

    /** Small unlit spheres for the abhishek's stream of water. */
    fun ensureDrops(count: Int) {
        while (MandirWorld.drops.size < count) {
            MandirWorld.drops += Entity.create(
                listOf(
                    Mesh("mesh://sphere".toUri(), hittable = MeshCollision.NoCollision),
                    Sphere(0.012f),
                    Material().apply {
                        baseColor = Color4(0.72f, 0.84f, 1f, 1f)
                        unlit = true
                    },
                    Transform(Pose(MandirPlace.garbha)),
                    Visible(false),
                ),
            )
        }
    }

    private fun build() {
        built = true
        loadRoom()
        // The room's own light is baked in; these light the props, which move.
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
        MandirWorld.jyotiLight = Entity.create(
            listOf(
                Transform(Pose(MandirPlace.jyoti + Vector3(0f, 0.05f, 0f))),
                Light(
                    type = LightType.POINT,
                    color = Vector3(1f, 0.6f, 0.26f),
                    intensity = 1.4f,
                    range = 1.6f,
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
        homes.forEach { (name, where) -> createProp(name, where) }
    }

    private fun setVisible(show: Boolean) {
        listOfNotNull(
            MandirWorld.room,
            MandirWorld.ceiling,
            MandirWorld.garbhaLight,
            MandirWorld.jyotiLight,
        ).forEach { it.setComponent(Visible(show)) }
        MandirWorld.diyaLight?.setComponent(Visible(show && Mandir.diyaLit))
        MandirWorld.incenseLight?.setComponent(Visible(show && Mandir.agarbattiLit))
        MandirWorld.props.values.forEach { it.setComponent(Visible(show)) }
        MandirWorld.deepmala?.setComponent(Visible(show))
        MandirWorld.deepmalaFlames.forEachIndexed { index, flame -> flame.setComponent(Visible(show && index in Mandir.deepmalaLit)) }
        if (!show) MandirWorld.drops.forEach { it.setComponent(Visible(false)) }
    }

    /** Props that only come out for a festival. */
    private val festivalProps = FestivalKind.entries.map { it.prop }.filter { it != "deepmala" }
}

/** Day between 05:30 and 17:30, lamplight otherwise. Pure, for tests. */
fun isDaytime(now: LocalTime): Boolean = now >= LocalTime.of(5, 30) && now < LocalTime.of(17, 30)
