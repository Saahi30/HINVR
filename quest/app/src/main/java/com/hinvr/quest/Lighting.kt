package com.hinvr.quest

import com.meta.spatial.core.Vector3
import com.meta.spatial.runtime.Scene
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

/**
 * The scene's lighting environment. Each frame eases toward the look the current screen
 * wants, so entering the sanctum brightens over about a second instead of jumping, and
 * leaving it always restores the hall.
 */
object Lighting {
    /** ambient rgb, sun rgb, sun direction xyz, environment intensity. Lamplit evening, then daylight. */
    private val MandirLook = floatArrayOf(0.38f, 0.28f, 0.18f, 1.15f, 0.78f, 0.46f, -0.2f, -1f, -0.15f, 1.05f)
    private val MandirDayLook = floatArrayOf(0.42f, 0.38f, 0.32f, 1.05f, 0.95f, 0.82f, -0.1f, -1f, 0.35f, 1.15f)

    private var scene = WeakReference<Scene>(null)
    private val current = FloatArray(10)
    private val target = FloatArray(10)
    private val applied = FloatArray(10) { Float.NaN }
    private var primed = false

    fun attach(host: Scene) {
        scene = WeakReference(host)
        primed = false
    }

    /** [mandir] picks the sanctum; otherwise the hall breathes with the greeting's flame. */
    fun tick(mandir: Boolean, flame: Float, dt: Float) {
        if (mandir) {
            (if (MandirScene.daytime) MandirDayLook else MandirLook).copyInto(target)
            // The aarti moment: a touch warmer and lower, then back.
            val warm = MandirCelebration.level()
            if (warm > 0f) {
                for (i in 0..5) target[i] *= 1f - 0.22f * warm
                target[0] += 0.05f * warm
                target[3] += 0.08f * warm
                target[5] -= 0.08f * warm
            }
        } else {
            val breath = 0.5f + 0.5f * sin((System.nanoTime() / 1_000_000_000f) * 2.2f)
            val lit = (flame * (0.86f + 0.14f * breath)).coerceIn(0f, 1f)
            target[0] = 0.04f + 0.10f * lit
            target[1] = 0.025f + 0.045f * lit
            target[2] = 0.015f + 0.01f * lit
            target[3] = 0.20f + 0.95f * lit
            target[4] = 0.10f + 0.48f * lit
            target[5] = 0.04f + 0.14f * lit
            target[6] = -0.15f
            target[7] = -1f
            target[8] = 0.35f
            target[9] = 0.12f + 0.28f * lit
        }
        if (!primed) {
            target.copyInto(current)
            primed = true
        } else {
            val follow = 1f - exp(-3.2f * dt)
            for (i in current.indices) current[i] += (target[i] - current[i]) * follow
        }
        var changed = false
        for (i in current.indices) {
            if (applied[i].isNaN() || abs(current[i] - applied[i]) > 0.01f) {
                changed = true
                break
            }
        }
        if (!changed) return
        val world = scene.get() ?: return
        current.copyInto(applied)
        runCatching {
            world.setLightingEnvironment(
                ambientColor = Vector3(current[0], current[1], current[2]),
                sunColor = Vector3(current[3], current[4], current[5]),
                sunDirection = Vector3(current[6], current[7], current[8]),
                environmentIntensity = current[9],
            )
        }
    }
}
