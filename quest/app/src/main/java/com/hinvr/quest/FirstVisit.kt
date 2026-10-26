package com.hinvr.quest

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.core.content.edit

/** Where the first visit's walk-through is: take the diya, light it, then over to the guide. */
enum class TutorialStep { Off, Take, Light, Done }

/** The walk-through's moves, without the headset: what the next step is, given what the member did. */
object TutorialRules {
    /** A step left alone this long ends the walk-through quietly; the guide carries on. */
    const val StepTimeoutMs = 90_000L

    /** "That's how every offering works" stays up this long. */
    const val DoneMs = 5_000L

    fun next(step: TutorialStep, holdingDiya: Boolean, diyaLit: Boolean, inStepMs: Long): TutorialStep = when (step) {
        TutorialStep.Off -> TutorialStep.Off
        TutorialStep.Take -> when {
            diyaLit -> TutorialStep.Done
            holdingDiya -> TutorialStep.Light
            inStepMs > StepTimeoutMs -> TutorialStep.Off
            else -> TutorialStep.Take
        }
        TutorialStep.Light -> when {
            diyaLit -> TutorialStep.Done
            // Put down unlit: back to taking it.
            !holdingDiya -> TutorialStep.Take
            inStepMs > StepTimeoutMs -> TutorialStep.Off
            else -> TutorialStep.Light
        }
        TutorialStep.Done -> if (inStepMs > DoneMs) TutorialStep.Off else TutorialStep.Done
    }
}

/**
 * The first visit on this headset walks the member through one offering before the guide takes
 * over. It never holds anything back: every prop works throughout, and Skip ends it.
 */
object Tutorial {
    private var prefs: SharedPreferences? = null
    private var stepAtMs = 0L

    var step by mutableStateOf(TutorialStep.Off)
        private set

    /** "Show me again" on the sankalp: the next Begin walks through it again. */
    var replay by mutableStateOf(false)
        private set

    val active: Boolean get() = step != TutorialStep.Off

    fun attach(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("hinvr_quest_prefs", Context.MODE_PRIVATE)
    }

    private val seen: Boolean get() = prefs?.getBoolean("tutorial_seen", false) ?: true

    fun toggleReplay() {
        replay = !replay
    }

    /** Called as the visit begins. Marked seen at once, so a crash midway can't loop it. */
    fun begin() {
        if (!replay && seen) return
        replay = false
        prefs?.edit { putBoolean("tutorial_seen", true) }
        move(TutorialStep.Take)
    }

    fun skip() = move(TutorialStep.Off)

    /** Every frame inside the sanctum. */
    fun tick(holdingDiya: Boolean, diyaLit: Boolean) {
        if (step == TutorialStep.Off) return
        val next = TutorialRules.next(step, holdingDiya, diyaLit, SystemClock.elapsedRealtime() - stepAtMs)
        if (next != step) move(next)
    }

    private fun move(to: TutorialStep) {
        step = to
        stepAtMs = SystemClock.elapsedRealtime()
    }
}

/** A soft gold ring that breathes around whatever the walk-through points at. */
@Composable
fun TutorialRing() {
    if (!Mandir.inside || (Tutorial.step != TutorialStep.Take && Tutorial.step != TutorialStep.Light)) return
    val breath by rememberInfiniteTransition(label = "tutorial-ring").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath",
    )
    Canvas(Modifier.fillMaxSize()) {
        val r = size.minDimension * (0.40f + 0.03f * breath)
        drawCircle(
            Brush.radialGradient(
                listOf(Color.Transparent, DiyaAmber.copy(alpha = 0.18f + 0.12f * breath), Color.Transparent),
                center = center,
                radius = size.minDimension * 0.5f,
            ),
            radius = size.minDimension * 0.5f,
        )
        drawCircle(GoldLight.copy(alpha = 0.55f + 0.35f * breath), radius = r, style = Stroke(width = size.minDimension * 0.025f))
    }
}
