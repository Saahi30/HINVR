package com.hinvr.quest

import com.meta.spatial.core.Entity
import com.meta.spatial.core.Pose
import com.meta.spatial.core.Query
import com.meta.spatial.core.Vector3
import com.meta.spatial.toolkit.Controller
import com.meta.spatial.toolkit.Hittable
import com.meta.spatial.toolkit.MeshCollision
import com.meta.spatial.toolkit.Scale
import com.meta.spatial.toolkit.Transform
import com.meta.spatial.toolkit.Visible
import com.meta.spatial.toolkit.createPanelEntity
import kotlin.math.abs

/**
 * A panel entity that only writes a component when its value actually changes, so a still
 * screen costs nothing per frame.
 */
class Panel(val entity: Entity, private val collapseWhenHidden: Boolean = false) {
    private var visible: Boolean? = null
    private var px = Float.NaN
    private var py = 0f
    private var pz = 0f
    private var qx = 0f
    private var qy = 0f
    private var qz = 0f
    private var qw = 0f
    private var scale = Float.NaN

    val shown: Boolean get() = visible == true

    fun show(on: Boolean) {
        if (visible == on) return
        visible = on
        entity.setComponent(Visible(on))
        // A hidden equirect layer still covers the cameras unless it is collapsed.
        if (collapseWhenHidden) setScale(if (on) 1f else 0f)
    }

    fun place(pose: Pose) {
        val t = pose.t
        val q = pose.q
        if (!px.isNaN() &&
            abs(t.x - px) < 5e-4f && abs(t.y - py) < 5e-4f && abs(t.z - pz) < 5e-4f &&
            abs(q.x - qx) < 1e-4f && abs(q.y - qy) < 1e-4f && abs(q.z - qz) < 1e-4f && abs(q.w - qw) < 1e-4f
        ) {
            return
        }
        px = t.x
        py = t.y
        pz = t.z
        qx = q.x
        qy = q.y
        qz = q.z
        qw = q.w
        entity.setComponent(Transform(pose))
    }

    fun setScale(value: Float) {
        if (abs(value - scale) < 1e-3f) return
        scale = value
        entity.setComponent(Scale(Vector3(value, value, value)))
    }

    companion object {
        fun create(panelId: Int, hittable: Boolean = true, collapse: Boolean = false): Panel {
            val entity = Entity.createPanelEntity(panelId, Transform(Pose()))
            if (!hittable) entity.setComponent(Hittable(MeshCollision.NoCollision))
            return Panel(entity, collapse).also { it.show(false) }
        }
    }
}

object RingWorld {
    val slots: Array<Panel?> = arrayOfNulls(RingSlots.COUNT)
    var stream: Panel? = null
    var opening: Panel? = null
    var hall: Panel? = null
    var environment: Panel? = null
    var status: Panel? = null
    var sphere: Panel? = null
    var tourEnd: Panel? = null
    var pair: Panel? = null
    val air: Array<Panel?> = arrayOfNulls(6)
    var profile: Panel? = null
    var chip: Panel? = null
    var credits: Panel? = null
    var fade: Panel? = null

    /** Where each card panel sits this frame, for the controller ray. Card index → pose. */
    val cardPoses = HashMap<Int, Pose>()

    /** Everything that belongs to the ring, for screens that hide it all at once. */
    val ringPanels: List<Panel> by lazy {
        listOfNotNull(stream, opening, hall, environment, status, pair, profile, chip, credits) +
            air.filterNotNull() + slots.filterNotNull()
    }
}

/** The 7680×3840 tour surface is only made the first time a tour plays. */
object SphereStage {
    fun ensure() {
        if (RingWorld.sphere != null) return
        RingWorld.sphere = Panel.create(R.id.ring_sphere, hittable = false, collapse = true)
    }
}

/** One frame's shared input, so each system doesn't query the controllers again. */
object Frame {
    var index = 0L
        private set
    var dt = 0.016f
        private set
    private var lastNs = 0L
    private var controllers: List<Entity> = emptyList()
    private var controllersAt = Long.MIN_VALUE

    fun begin() {
        val now = System.nanoTime()
        dt = if (lastNs == 0L) 0.016f else ((now - lastNs) / 1_000_000_000f).coerceIn(0f, 0.05f)
        lastNs = now
        index++
    }

    /** The local controllers and hands. Re-queried about once a second; they rarely change. */
    fun controllers(): List<Entity> {
        if (controllers.isEmpty() || index - controllersAt > 90) {
            controllers = Query.where { has(Controller.id) }.eval().filter { it.isLocal() }.toList()
            controllersAt = index
        }
        return controllers
    }
}
