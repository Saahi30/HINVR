package com.hinvr.quest

import android.os.SystemClock
import com.meta.spatial.core.Pose
import com.meta.spatial.core.Quaternion
import com.meta.spatial.core.Vector3

/** Where the sanctum's panels sit. Called by the layout system each frame while inside. */
object MandirLayout {
    /** The tilak's glow lasts this long, then the view is clear again. */
    const val TilakGlowMs = 2_000L

    private val FacingMandap = Quaternion(0f, 180f, 0f)

    fun place(head: Pose) {
        val phase = Mandir.phase
        val ready = MandirScene.ready
        MandirWorld.darshan?.let { panel ->
            val show = ready && phase != MandirPhase.Sankalp
            panel.show(show)
            if (show) panel.place(Pose(MandirPlace.darshan, FacingMandap))
        }
        MandirWorld.sankalp?.let { panel ->
            val show = ready && phase == MandirPhase.Sankalp
            panel.show(show)
            if (show) panel.place(RingLayout.placeInFront(head, 1.35f, -0.04f))
        }
        MandirWorld.guide?.let { panel ->
            val show = ready && phase == MandirPhase.Visit
            panel.show(show)
            // Over the offering stand, about a metre and a half away: close enough to read.
            if (show) panel.place(Pose(MandirPlace.stand + Vector3(0f, 0.50f, -0.42f), FacingMandap))
        }
        MandirWorld.blessing?.let { panel ->
            val show = phase == MandirPhase.Blessing
            panel.show(show)
            if (show) panel.place(RingLayout.placeInFront(head, 1.4f, -0.06f))
        }
        MandirWorld.flame?.let { panel ->
            val show = Mandir.diyaLit
            panel.show(show)
            if (show) panel.place(MandirScene.diyaPose)
        }
        MandirWorld.smoke?.let { panel ->
            val show = Mandir.agarbattiLit
            panel.show(show)
            if (show) panel.place(MandirScene.incensePose)
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
    }
}
