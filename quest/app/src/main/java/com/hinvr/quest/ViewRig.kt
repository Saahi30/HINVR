package com.hinvr.quest

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.meta.spatial.core.Pose
import com.meta.spatial.core.Vector3
import com.meta.spatial.runtime.Scene
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Yaw of a pose, in the same degrees the panels use: 0 faces +Z. */
fun yawOf(pose: Pose): Float {
    val forward = pose.forward()
    return Math.toDegrees(atan2(forward.x, forward.z).toDouble()).toFloat()
}

fun wrapDegrees(value: Float): Float {
    var wrapped = value % 360f
    if (wrapped > 180f) wrapped -= 360f
    if (wrapped < -180f) wrapped += 360f
    return wrapped
}

/**
 * Owns the view origin, so every move of the member goes through one place.
 *
 * Moves happen behind a short fade to black. While black, the member's head is put on the
 * target mark facing the target direction, using where the head actually is in the room
 * (people stand off-centre and face anywhere). The placement is then measured and nudged
 * until it lands, which also holds if the runtime already compensates part of it.
 */
object ViewRig {
    private enum class Phase { Idle, FadingOut, Placing, Settling, FadingIn }

    private const val FadeOutSec = 0.22f
    private const val FadeInSec = 0.35f
    private const val PositionSlack = 0.06f
    private const val YawSlack = 4f
    private const val MaxCorrections = 4

    private var scene = WeakReference<Scene>(null)
    private var originX = 0f
    private var originZ = 0f
    private var originYaw = 0f

    private var phase = Phase.Idle
    private var target: Vector3? = null
    private var targetYaw: Float? = null
    private var settleFrames = 0
    private var corrections = 0
    private var yawSign = 1f
    private var lastAppliedYaw = 0f
    private var lastMeasuredYaw = Float.NaN

    /** 0 is clear, 1 is black. The fade panel reads it. */
    var fade by mutableFloatStateOf(0f)
        private set

    /** Hold the fade at black, e.g. while the sanctum is still loading. */
    var holdBlack by mutableStateOf(false)

    val busy: Boolean get() = phase != Phase.Idle

    /** Counts finished moves, so head-anchored panels know to re-anchor. */
    var moves = 0
        private set

    fun attach(host: Scene) {
        scene = WeakReference(host)
        apply(0f, 0f, 0f)
    }

    /** Put the member's head at [mark] (floor height ignored) facing [yawDeg]. */
    fun placeHead(mark: Vector3, yawDeg: Float, faded: Boolean = true) {
        target = mark
        targetYaw = yawDeg
        corrections = 0
        lastMeasuredYaw = Float.NaN
        phase = if (faded) Phase.FadingOut else Phase.Placing
    }

    /** Back to the room as the headset sees it. */
    fun reset(faded: Boolean = false) {
        target = null
        targetYaw = null
        if (faded) {
            phase = Phase.FadingOut
        } else {
            phase = Phase.Idle
            fade = 0f
            apply(0f, 0f, 0f)
            moves++
        }
    }

    /** Start from black and fade in: for leaving a place whose scene is already gone. */
    fun fadeFromBlack() {
        fade = 1f
        if (phase == Phase.Idle) phase = Phase.FadingIn
    }

    /** The ring turns the room around the member; no fade, the cards move with it. */
    fun turnRoom(yawDeg: Float) {
        if (busy) return
        apply(originX, originZ, yawDeg)
    }

    val roomYaw: Float get() = originYaw

    /** Run each frame with the viewer's current pose. */
    fun tick(viewer: Pose, dt: Float) {
        when (phase) {
            Phase.Idle -> if (fade > 0f && !holdBlack) fade = (fade - dt / FadeInSec).coerceAtLeast(0f)
            Phase.FadingOut -> {
                fade = (fade + dt / FadeOutSec).coerceAtMost(1f)
                if (fade >= 1f) phase = Phase.Placing
            }
            Phase.Placing -> {
                fade = 1f
                val mark = target
                val yaw = targetYaw
                if (mark == null || yaw == null) {
                    apply(0f, 0f, 0f)
                } else {
                    placeFrom(viewer, mark, yaw)
                }
                settleFrames = 3
                phase = Phase.Settling
            }
            Phase.Settling -> {
                fade = 1f
                if (--settleFrames > 0) return
                if (needsCorrection(viewer) && corrections < MaxCorrections) {
                    corrections++
                    correct(viewer)
                    settleFrames = 3
                    return
                }
                phase = Phase.FadingIn
                moves++
            }
            Phase.FadingIn -> {
                if (holdBlack) {
                    fade = 1f
                    return
                }
                fade = (fade - dt / FadeInSec).coerceAtLeast(0f)
                if (fade <= 0f) phase = Phase.Idle
            }
        }
    }

    /** Solve origin = mark − R(yaw) · headInRoom, treating the origin as the room's placement. */
    private fun placeFrom(viewer: Pose, mark: Vector3, yaw: Float) {
        val local = toRoom(viewer)
        val headYawInRoom = wrapDegrees(yawOf(viewer) - originYaw * yawSign)
        val newYaw = wrapDegrees((yaw - headYawInRoom) * yawSign)
        val (rx, rz) = rotate(local.first, local.second, newYaw * yawSign)
        apply(mark.x - rx, mark.z - rz, newYaw)
    }

    private fun needsCorrection(viewer: Pose): Boolean {
        val mark = target ?: return false
        val yaw = targetYaw ?: return false
        val dx = mark.x - viewer.t.x
        val dz = mark.z - viewer.t.z
        return abs(wrapDegrees(yaw - yawOf(viewer))) > YawSlack || dx * dx + dz * dz > PositionSlack * PositionSlack
    }

    private fun correct(viewer: Pose) {
        val mark = target ?: return
        val yaw = targetYaw ?: return
        val measured = yawOf(viewer)
        // Learn which way the runtime turns the room the first time a turn is measured.
        if (!lastMeasuredYaw.isNaN() && abs(wrapDegrees(originYaw - lastAppliedYaw)) > 1f) {
            val turned = wrapDegrees(measured - lastMeasuredYaw)
            val applied = wrapDegrees(originYaw - lastAppliedYaw)
            if (turned * applied < 0f) yawSign = -yawSign
        }
        lastMeasuredYaw = measured
        lastAppliedYaw = originYaw
        val yawError = wrapDegrees(yaw - measured)
        if (abs(yawError) > YawSlack) {
            placeFrom(viewer, mark, yaw)
            return
        }
        // Heading is right; slide the room so the head sits on the mark.
        apply(originX + (mark.x - viewer.t.x), originZ + (mark.z - viewer.t.z), originYaw)
    }

    /** The head's floor position inside the room, undoing the current origin. */
    private fun toRoom(viewer: Pose): Pair<Float, Float> =
        rotate(viewer.t.x - originX, viewer.t.z - originZ, -originYaw * yawSign)

    private fun rotate(x: Float, z: Float, yawDeg: Float): Pair<Float, Float> {
        val r = Math.toRadians(yawDeg.toDouble())
        val c = cos(r).toFloat()
        val s = sin(r).toFloat()
        return Pair(x * c + z * s, -x * s + z * c)
    }

    private fun apply(x: Float, z: Float, yaw: Float) {
        originX = x
        originZ = z
        originYaw = wrapDegrees(yaw)
        scene.get()?.let { runCatching { it.setViewOrigin(x, 0f, z, originYaw) } }
    }
}
