package com.hinvr.quest

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.sqrt

/** A small vector for the offering rules, so they can be tested without the headset runtime. */
data class Vec(val x: Float, val y: Float, val z: Float) {
    operator fun minus(o: Vec) = Vec(x - o.x, y - o.y, z - o.z)
    operator fun plus(o: Vec) = Vec(x + o.x, y + o.y, z + o.z)
    operator fun times(s: Float) = Vec(x * s, y * s, z * s)
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
class Gesture(start: Vec, startNs: Long) {
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
    const val GrabReach = 0.30f

    /** Far enough that a prop was carried, not just lifted and let go. */
    const val MinPath = 0.20f

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

    /** How close a wick or incense tip has to come to a flame to catch it. */
    const val FlameReach = 0.10f

    /** Above a prop's origin (its base): the diya's wick, the agarbatti's tip, the thali's camphor. */
    const val WickHeight = 0.06f
    const val IncenseTipHeight = 0.22f
    const val ThaliFlameHeight = 0.05f

    /** Half the stand's top, and how high above it a prop can be let go and still be set on it. */
    const val StandHalfWidth = 0.55f
    const val StandHalfDepth = 0.23f
    const val StandDropHeight = 0.35f

    /** A wick or incense tip touched to a flame. */
    fun touchesFlame(tip: Vec, flame: Vec): Boolean = near(tip, flame, FlameReach)

    /** Let go over the stand's top: the prop is set down where it is. */
    fun overStand(release: Vec, standTop: Vec): Boolean =
        abs(release.x - standTop.x) <= StandHalfWidth &&
            abs(release.z - standTop.z) <= StandHalfDepth &&
            release.y - standTop.y in -0.05f..StandDropHeight

    /** Rays reach props from this close to this far. */
    const val RayMin = 0.15f
    const val RayReach = 3.0f

    /** How far a ray may pass beside a prop and still aim at it; wider the further it is. */
    const val RaySlack = 0.06f
    const val RaySlackPerMetre = 0.03f

    /**
     * How well a ray from [origin] along unit [dir] aims at [target]: 0 is dead on, 1 is the
     * edge of what counts. Null when it misses, is behind, or is out of reach.
     */
    fun rayAim(origin: Vec, dir: Vec, target: Vec): Float? {
        val to = target - origin
        val along = to.dot(dir)
        if (along < RayMin || along > RayReach) return null
        val miss = (to - dir * along).length()
        val slack = RaySlack + RaySlackPerMetre * along
        return if (miss < slack) miss / slack else null
    }

    /** The order the guide suggests. Aarti is last because it completes the visit. */
    val PujaOrder = listOf(
        OfferingKind.Diya,
        OfferingKind.Agarbatti,
        OfferingKind.Pushp,
        OfferingKind.Prasad,
        OfferingKind.Ghanta,
        OfferingKind.Tilak,
        OfferingKind.Daan,
        OfferingKind.Aarti,
    )

    /** The first step not yet done, or null when every step is. */
    fun nextStep(done: Collection<OfferingKind>): OfferingKind? = PujaOrder.firstOrNull { it !in done }

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

    /** A lota tipped past this pours. */
    const val PourTiltDeg = 70f

    /** How nearly toward the garbha the lota must tip when the member isn't at the threshold. */
    const val PourAimCosine = 0.8f

    /**
     * The lota, with its own up axis [up], pours toward the lingam: tipped far enough, and either
     * the member is at the threshold or the mouth leans toward [toGarbha].
     */
    fun pours(up: Vec, toGarbha: Vec, atThreshold: Boolean): Boolean {
        val length = up.length()
        if (length < 1e-4f) return false
        val tilt = Math.toDegrees(kotlin.math.acos((up.y / length).coerceIn(-1f, 1f)).toDouble())
        if (tilt < PourTiltDeg) return false
        if (atThreshold) return true
        val lean = sqrt(up.x * up.x + up.z * up.z)
        val flat = sqrt(toGarbha.x * toGarbha.x + toGarbha.z * toGarbha.z)
        if (lean < 1e-4f || flat < 1e-4f) return false
        return (up.x * toGarbha.x + up.z * toGarbha.z) / (lean * flat) >= PourAimCosine
    }

    /** One chip on the guide: an offering, or the day's festival puja. */
    sealed interface Step {
        data class Offering(val kind: OfferingKind) : Step
        data object Festival : Step
    }

    /** The guide's steps: the eight, with a festival's puja just before aarti. */
    fun steps(festival: Boolean): List<Step> {
        val offerings = PujaOrder.map { Step.Offering(it) }
        if (!festival) return offerings
        return offerings.dropLast(1) + Step.Festival + offerings.last()
    }

    /** The first step not done yet; null once all are. */
    fun nextStep(done: Collection<OfferingKind>, festival: Boolean, festivalDone: Boolean): Step? =
        steps(festival).firstOrNull { step ->
            when (step) {
                is Step.Offering -> step.kind !in done
                Step.Festival -> !festivalDone
            }
        }

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

/**
 * Counts aarti circles drawn in a plane (the member's lateral axis and world up). The centre
 * is a running average of where the thali has been, so circles count wherever they are drawn;
 * only motion at least [minRadius] from it turns the count, so jitter and a line back and
 * forth do not.
 */
class AartiCircles(private val minRadius: Float = 0.06f, private val centreSec: Float = 0.6f) {
    private var cx = 0f
    private var cy = 0f
    private var primed = false
    private var lastAngle = Float.NaN
    private var turned = 0f

    /** Whole circles so far, either direction; turning back undoes them. */
    val circles: Int get() = (abs(turned) / (2f * PI.toFloat())).toInt()

    fun reset() {
        primed = false
        lastAngle = Float.NaN
        turned = 0f
    }

    /** Adds one sample, [dt] seconds after the last, and returns the circle count. */
    fun add(lateral: Float, up: Float, dt: Float): Int {
        if (!primed) {
            cx = lateral
            cy = up
            primed = true
            return circles
        }
        val follow = 1f - exp(-dt / centreSec)
        cx += (lateral - cx) * follow
        cy += (up - cy) * follow
        val dx = lateral - cx
        val dy = up - cy
        if (dx * dx + dy * dy < minRadius * minRadius) {
            lastAngle = Float.NaN
            return circles
        }
        val angle = atan2(dy, dx)
        val previous = lastAngle
        lastAngle = angle
        if (previous.isNaN()) return circles
        var delta = angle - previous
        if (delta > PI) delta -= (2.0 * PI).toFloat()
        if (delta < -PI) delta += (2.0 * PI).toFloat()
        // A jump this large is a lost frame or a flick through the centre, not a circle.
        if (abs(delta) < PI.toFloat() / 2f) turned += delta
        return circles
    }
}

/**
 * The hanging ghanta as a damped pendulum about its hook. The swing is a small rotation
 * vector (radians, about world X and Z); a ring kicks it the way the hand moved.
 */
class BellSwing(length: Float, private val decaySec: Float = 2.5f) {
    private val omega = sqrt(9.81f / length.coerceAtLeast(0.1f))
    var aboutX = 0f
        private set
    var aboutZ = 0f
        private set
    private var spinX = 0f
    private var spinZ = 0f

    val moving: Boolean get() = abs(aboutX) + abs(aboutZ) > 1e-4f || abs(spinX) + abs(spinZ) > 1e-3f

    /** The swing's angle right now, in degrees. */
    val angleDeg: Float get() = Math.toDegrees(sqrt(aboutX * aboutX + aboutZ * aboutZ).toDouble()).toFloat()

    /**
     * Pushes the bell along the horizontal direction ([dirX], [dirZ]) so the swing peaks near
     * [amplitudeDeg]. A bell already swinging is re-energised, never past [MaxDeg].
     */
    fun kick(dirX: Float, dirZ: Float, amplitudeDeg: Float = 6f) {
        val length = sqrt(dirX * dirX + dirZ * dirZ)
        val x = if (length < 1e-3f) 0f else dirX / length
        val z = if (length < 1e-3f) -1f else dirZ / length
        val speed = Math.toRadians(amplitudeDeg.toDouble()).toFloat() * omega
        // A rotation about +X carries the hanging bell toward -Z; about +Z, toward +X.
        spinX += -z * speed
        spinZ += x * speed
        val cap = Math.toRadians(MaxDeg.toDouble()).toFloat() * omega
        val spin = sqrt(spinX * spinX + spinZ * spinZ)
        if (spin > cap) {
            spinX *= cap / spin
            spinZ *= cap / spin
        }
    }

    /** Advances the swing by [dt] seconds. */
    fun step(dt: Float) {
        if (!moving) return
        // Small fixed steps keep the pendulum stable at any frame rate.
        var left = dt.coerceIn(0f, 0.1f)
        while (left > 0f) {
            val h = minOf(left, 1f / 240f)
            spinX += (-omega * omega * aboutX - 2f / decaySec * spinX) * h
            spinZ += (-omega * omega * aboutZ - 2f / decaySec * spinZ) * h
            aboutX += spinX * h
            aboutZ += spinZ * h
            left -= h
        }
        if (!moving) stop()
    }

    fun stop() {
        aboutX = 0f
        aboutZ = 0f
        spinX = 0f
        spinZ = 0f
    }

    companion object {
        const val MaxDeg = 10f
    }
}
