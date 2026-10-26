package com.hinvr.quest

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * The moment aarti completes: petals fall, the chant swells, the lamps glow warmer and lower.
 * It lasts [LengthMs] and then everything is as it was.
 */
object MandirCelebration {
    const val LengthMs = 6_000L

    private val main = Handler(Looper.getMainLooper())
    private var startedAt = 0L
    private val end = Runnable { showering = false }

    /** The petal shower's panels are up. */
    var showering by mutableStateOf(false)
        private set

    fun start() {
        startedAt = SystemClock.elapsedRealtime()
        showering = true
        main.removeCallbacks(end)
        main.postDelayed(end, LengthMs + 1_500L)
    }

    fun stop() {
        startedAt = 0L
        showering = false
        main.removeCallbacks(end)
    }

    /** 0 to 1 and back over the moment: up in a second, held, eased out. */
    fun level(nowMs: Long = SystemClock.elapsedRealtime()): Float =
        celebrationLevel(if (startedAt == 0L) -1L else nowMs - startedAt)
}

/** Pure, for tests: the celebration's level [ageMs] into the moment; negative means none. */
fun celebrationLevel(ageMs: Long, lengthMs: Long = MandirCelebration.LengthMs): Float = when {
    ageMs < 0L || ageMs >= lengthMs -> 0f
    ageMs < 1_000L -> ageMs / 1_000f
    ageMs > lengthMs - 2_000L -> (lengthMs - ageMs) / 2_000f
    else -> 1f
}
