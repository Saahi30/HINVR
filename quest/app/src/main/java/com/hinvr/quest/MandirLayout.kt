package com.hinvr.quest

import android.os.SystemClock
import com.meta.spatial.core.Pose
import com.meta.spatial.core.Quaternion
import com.meta.spatial.core.Vector3
import com.meta.spatial.toolkit.Transform
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** A wick's flame panel, life-size: about 3.5 cm of flame and its warmth. */
const val FlamePanelWidth = 0.07f
const val FlamePanelHeight = 0.09f

/** Where the sanctum's panels sit. Called by the layout system each frame while inside. */
object MandirLayout {
    /** The tilak's glow lasts this long, then the view is clear again. */
    const val TilakGlowMs = 2_000L

    private val FacingMandap = Quaternion(0f, 180f, 0f)

    fun place(head: Pose) {
        val phase = Mandir.phase
        val ready = MandirScene.ready
        val visiting = ready && phase == MandirPhase.Visit
        MandirSound.listener = head
        MandirScene.tickLights()
        Tutorial.tick(holdingDiya = Mandir.heldProp == "diya", diyaLit = Mandir.diyaLit)
        MandirWorld.tutorial?.let { panel ->
            // Around the diya until it is in a hand, then around the jyoti's flame.
            val at = when (Tutorial.step) {
                TutorialStep.Take -> MandirWorld.props["diya"]?.tryGetComponent<Transform>()?.transform?.t?.let { it + Vector3(0f, 0.03f, 0f) }
                TutorialStep.Light -> MandirPlace.jyoti
                else -> null
            }
            val show = visiting && at != null
            panel.show(show)
            if (show && at != null) panel.place(facing(at, head))
        }
        RingWorld.castTip?.let { panel ->
            val show = CastTip.showing && Mandir.darshanClose
            panel.show(show)
            // Under the brought-forward darshan (1.6 × 0.9 m at 2.3 m).
            if (show) panel.place(onAxis(head, 2.2f, -0.12f - 0.45f - 0.17f))
        }
        MandirWorld.darshan?.let { panel ->
            val show = ready && phase != MandirPhase.Sankalp
            panel.show(show)
            if (show) {
                // Closer: 1.6 m wide at 2.3 m, about 38° of view; otherwise the window in the garbha.
                panel.place(if (Mandir.darshanClose) onAxis(head, 2.3f, -0.12f) else Pose(MandirPlace.darshan, FacingMandap))
            }
        }
        MandirWorld.sankalp?.let { panel ->
            val show = ready && phase == MandirPhase.Sankalp
            panel.show(show)
            if (show) {
                panel.place(onAxis(head, 1.35f, -0.04f))
                panel.setScale(0.9f)
            }
        }
        MandirWorld.guide?.let { panel ->
            panel.show(visiting)
            // Just behind the stand and low, so it reads at arm's length without hiding the murti.
            if (visiting) {
                panel.place(Pose(MandirPlace.stand + Vector3(0f, 0.34f, -0.40f), FacingMandap))
                // About a metre from the eyes: 1 m wide, not the panel's full 1.4 m.
                panel.setScale(0.72f)
            }
        }
        MandirWorld.blessing?.let { panel ->
            val show = phase == MandirPhase.Blessing
            panel.show(show)
            if (show) panel.place(onAxis(head, 1.4f, -0.06f))
        }
        // Flame panels draw the flame in their lower half, so they sit a little above the wick.
        flame(MandirWorld.jyoti, ready, MandirPlace.jyoti, head)
        flame(MandirWorld.flame, Mandir.diyaLit, MandirScene.diyaWick, head)
        flame(MandirWorld.thaliFlame, Mandir.thaliHeld, MandirScene.thaliFlame, head)
        MandirWorld.smoke?.let { panel ->
            val show = Mandir.agarbattiLit
            panel.show(show)
            if (show) panel.place(facing(MandirScene.incenseTip + Vector3(0f, 0.17f, 0f), head))
        }
        MandirWorld.label?.let { panel ->
            val over = if (visiting) Mandir.hover?.let { labelAt(it) } else null
            panel.show(over != null)
            if (over != null) {
                panel.place(facing(over, head))
                // Read at arm's length: small type, not a sign.
                panel.setScale(0.55f)
            }
        }
        // Soft glows over the room's own flames.
        MandirWorld.halos.forEachIndexed { index, panel ->
            panel ?: return@forEachIndexed
            val lamp = MandirPlace.lamps.getOrNull(index)
            panel.show(ready && lamp != null)
            if (ready && lamp != null) {
                // The lamp stands' wide rings of wicks get a big glow, a single wick a small one,
                // and a lamp on the floor a small one held clear of the floor.
                val scale = when {
                    lamp.y > 1.1f && kotlin.math.abs(lamp.x) < 2f -> 0.8f
                    lamp.y < 0.3f -> 0.25f
                    else -> 0.32f
                }
                val lift = if (lamp.y < 0.3f) 0.11f else 0.04f
                panel.place(facing(lamp + Vector3(0f, lift, 0f), head))
                panel.setScale(scale)
            }
        }
        MandirWorld.japa?.let { panel ->
            val show = visiting && (Japa.active || Mandir.hover == OfferingSystem.Mala || Japa.beads > 0 || Japa.rounds > 0)
            panel.show(show)
            if (show) {
                panel.place(facing(MandirPlace.side + Vector3(0f, 0.46f, -0.12f), head))
                panel.setScale(0.7f)
            }
        }
        // Aarti complete: petals fall around the member, as at the greeting.
        RingWorld.air.forEachIndexed { index, panel ->
            panel ?: return@forEachIndexed
            val show = MandirCelebration.showering
            panel.show(show)
            if (show) panel.place(RingLayout.placeOnCircle(head, index * 60f, 2.02f, 0.32f))
        }
        MandirWorld.tilak?.let { panel ->
            val at = Mandir.tilakAtMs
            val show = at != 0L && SystemClock.elapsedRealtime() - at < TilakGlowMs
            panel.show(show)
            // High and a little further out, so the warmth sits at the brow, not over the eyes.
            if (show) panel.place(RingLayout.placeInFront(head, 0.75f, 0.16f))
        }
    }

    fun hide() {
        MandirWorld.panels.forEach { it.show(false) }
        MandirWorld.halos.forEach { it?.show(false) }
    }

    private fun flame(panel: Panel?, show: Boolean, wick: Vector3, head: Pose) {
        panel ?: return
        panel.show(show)
        // The panel's centre sits above the wick so the drawn flame's base is on it.
        if (show) panel.place(facing(wick + Vector3(0f, FlamePanelHeight * (WickFlameBase - 0.5f), 0f), head))
    }

    /**
     * [distance] ahead of the member on the line to the garbha, whichever way they happen to be
     * looking, so the panel is centred on the shrine rather than on a glance.
     */
    private fun onAxis(head: Pose, distance: Float, yOffset: Float): Pose {
        val garbha = MandirPlace.garbha
        val yaw = atan2(garbha.x - head.t.x, garbha.z - head.t.z)
        val at = head.t + Vector3(sin(yaw) * distance, yOffset, cos(yaw) * distance)
        return Pose(at, Quaternion(0f, Math.toDegrees(yaw.toDouble()).toFloat(), 0f))
    }

    /** Above the hovered prop: over the bell for the ghanta, a hand's width over anything else. */
    private fun labelAt(name: String): Vector3? {
        val pose = MandirWorld.props[name]?.tryGetComponent<Transform>()?.transform ?: return null
        return if (name == OfferingSystem.Ghanta) {
            MandirScene.bellCentre(pose) + Vector3(0f, 0.24f, 0f)
        } else {
            pose.t + Vector3(0f, 0.17f, 0f)
        }
    }

    /** A panel at [at] turned to face the member. */
    private fun facing(at: Vector3, head: Pose): Pose {
        val yaw = Math.toDegrees(atan2(at.x - head.t.x, at.z - head.t.z).toDouble()).toFloat()
        return Pose(at, Quaternion(0f, yaw, 0f))
    }
}
