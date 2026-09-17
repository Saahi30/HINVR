package com.hinvr.app.ui.motion

import androidx.compose.animation.core.CubicBezierEasing

/**
 * HINVR motion is quiet and ceremonial: quick exits, slower arrivals, no bounce.
 * Immersive screens fade like entering a sanctum; navigation screens move on a
 * shallow shared axis so the member never loses their place.
 */
object HinvrMotion {
    const val Quick = 220
    const val Standard = 420
    const val Immersive = 540

    val EnterEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
    val ExitEasing = CubicBezierEasing(0.4f, 0f, 1f, 1f)
}
