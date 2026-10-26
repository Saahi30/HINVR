package com.hinvr.quest

import kotlin.math.sqrt

/** A small vector for the offering rules, so they can be tested without the headset runtime. */
data class Vec(val x: Float, val y: Float, val z: Float) {
    operator fun minus(o: Vec) = Vec(x - o.x, y - o.y, z - o.z)
    fun length(): Float = sqrt(x * x + y * y + z * z)
    fun flatDistance(o: Vec): Float {
        val dx = x - o.x
        val dz = z - o.z
        return sqrt(dx * dx + dz * dz)
    }
    fun dot(o: Vec) = x * o.x + y * o.y + z * o.z
}

/**
 * What one grab did between pick-up and release. An offering only counts when the member
 * made the gesture on purpose: picked up and put down in place is not an offering.
 */
class Gesture(private val start: Vec, startNs: Long) {
    private val times = LongArray(8)
    private val xs = FloatArray(8)
    private val ys = FloatArray(8)
    private val zs = FloatArray(8)
    private var count = 0
    private var head = 0
    private var lastX = start.x
    private var lastY = start.y
    private var lastZ = start.z

    /** Total distance the prop travelled. */
    var path = 0f
        private set

    /** Highest it was raised above where it was picked up. */
    var lift = 0f
        private set

    init {
        add(start, startNs)
    }

    fun add(at: Vec, ns: Long) {
        val dx = at.x - lastX
        val dy = at.y - lastY
        val dz = at.z - lastZ
        path += sqrt(dx * dx + dy * dy + dz * dz)
        lastX = at.x
        lastY = at.y
        lastZ = at.z
        lift = maxOf(lift, at.y - start.y)
        times[head] = ns
        xs[head] = at.x
        ys[head] = at.y
        zs[head] = at.z
        head = (head + 1) % times.size
        if (count < times.size) count++
    }

    /** Average velocity over the last ~0.1 s, in m/s. */
    fun velocity(): Vec {
        if (count < 2) return Vec(0f, 0f, 0f)
        val newest = (head - 1 + times.size) % times.size
        var oldest = newest
        for (step in 1 until count) {
            val i = (newest - step + times.size) % times.size
            oldest = i
            if (times[newest] - times[i] >= 100_000_000L) break
        }
        val seconds = (times[newest] - times[oldest]) / 1_000_000_000f
        if (seconds <= 0f) return Vec(0f, 0f, 0f)
        return Vec((xs[newest] - xs[oldest]) / seconds, (ys[newest] - ys[oldest]) / seconds, (zs[newest] - zs[oldest]) / seconds)
    }
}

object OfferingRules {
    const val GrabReach = 0.38f
    const val DropReach = 0.38f

    /** Far enough that a prop was carried, not just lifted and let go. */
    const val MinPath = 0.20f
    const val MinLift = 0.08f

    /** A toss toward a target: fast enough, and pointed at it. */
    const val TossSpeed = 0.6f
    const val TossCosine = 0.82f

    /** How wide and how high above the peti a coin can be let go to fall in. */
    const val PetiRadius = 0.45f
    const val PetiHeight = 1.6f

    fun near(a: Vec, b: Vec, reach: Float) = (a - b).length() < reach

    /** Moving at [target] at [TossSpeed] or faster, within ~35°. */
    fun tossedToward(release: Vec, velocity: Vec, target: Vec): Boolean {
        val speed = velocity.length()
        if (speed < TossSpeed) return false
        val toward = target - release
        val distance = toward.length()
        if (distance < 1e-3f) return true
        return velocity.dot(toward) / (speed * distance) >= TossCosine
    }

    /** The diya is raised and set back down on the stand. */
    fun diyaSetDown(gesture: Gesture, release: Vec, stand: Vec): Boolean =
        gesture.lift >= MinLift && gesture.path >= MinPath && near(release, stand, DropReach)

    /** Flowers and naivedya: placed at the feet, or offered toward the garbha from where the member stands. */
    fun offeredToGarbha(gesture: Gesture, release: Vec, velocity: Vec, feet: Vec, garbha: Vec): Boolean {
        if (near(release, feet, 0.45f) || near(release, garbha, 0.7f)) return true
        if (gesture.path < MinPath) return false
        return tossedToward(release, velocity, feet) || tossedToward(release, velocity, garbha)
    }

    /** Let go over the peti and the coin drops in; or toss it in from a step away. */
    fun intoPeti(gesture: Gesture, release: Vec, velocity: Vec, peti: Vec): Boolean {
        if (gesture.path < MinPath) return false
        val above = release.y - peti.y
        if (release.flatDistance(peti) <= PetiRadius && above in -0.1f..PetiHeight) return true
        return tossedToward(release, velocity, peti)
    }

    /** Carried to the incense holder, not just set down where it lay beside it. */
    fun incensePlanted(gesture: Gesture, release: Vec, holder: Vec): Boolean =
        gesture.path >= MinPath && near(release, holder, 0.28f)

    /** The server's cap on sankalp text. */
    const val SankalpLimit = 500

    fun clampSankalp(text: String): String {
        if (text.length <= SankalpLimit) return text
        // Don't split a surrogate pair at the cut.
        var end = SankalpLimit
        if (Character.isHighSurrogate(text[end - 1])) end--
        return text.substring(0, end)
    }
}
