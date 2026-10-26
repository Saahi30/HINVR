package com.hinvr.quest

import com.meta.spatial.core.Entity
import com.meta.spatial.core.Hand
import com.meta.spatial.core.Pose
import com.meta.spatial.core.SystemBase
import com.meta.spatial.core.Vector3
import com.meta.spatial.runtime.ButtonBits
import com.meta.spatial.toolkit.AvatarAttachment
import com.meta.spatial.toolkit.Controller
import com.meta.spatial.toolkit.ControllerType
import com.meta.spatial.toolkit.Scale
import com.meta.spatial.toolkit.Transform
import com.meta.spatial.toolkit.Visible

/**
 * The offerings in the hand: what a hand would take (and its label), picking up near or by
 * pointing, the hanging ghanta, flames passed from the jyoti, aarti circles, and props
 * flying to where they were offered.
 */
class OfferingSystem : SystemBase() {
    private var held: String? = null
    private var heldHand: Entity? = null
    private var gesture: Gesture? = null
    private var lastPos: Vector3? = null

    /** A prop taken by pointing flies from here to the hand before it counts as held. */
    private var pullFrom: Vector3? = null
    private var pullT = 0f
    private var ghantaUntil = 0L
    private var wasInside = false
    private val aarti = AartiCircles()
    private var bell = BellSwing(3.0f)
    private val flights = HashMap<String, Flight>()
    private val scaled = HashMap<String, Float>()
    private val hovering = HashMap<Entity, String>()
    private val lastHand = HashMap<Entity, Vector3>()

    /**
     * The mala stays in the hand once taken, so a pinch can count beads. [malaArmed] waits for
     * the press that took it to let go; a long press, or a press over the side table, sets it down.
     */
    private var malaArmed = false
    private var malaDownFor = 0f

    /** The abhishek's water on its way to the lingam: seconds into the pour, or negative when none. */
    private var pourT = -1f
    private var pourFrom = Vector3(0f)

    /** A prop on its way somewhere, along a low arc; [land] runs when it gets there. */
    private class Flight(val from: Vector3, val to: Vector3, val seconds: Float, val rise: Float, val land: (() -> Unit)?) {
        var t = 0f
    }

    override fun execute() {
        val inside = Mandir.inside
        if (inside && !wasInside) reset()
        wasInside = inside
        if (!inside) return
        val dt = Frame.dt
        stepBell(dt)
        stepFlights(dt)
        stepPour(dt)
        if (Mandir.phase != MandirPhase.Visit || !MandirScene.ready) {
            // A prop still in the hand when the visit moves on goes back to the stand.
            held?.let { name -> glide(name, MandirScene.homes[name] ?: return@let) }
            letGo()
            showHover(null)
            return
        }
        val viewer = runCatching { getScene().getViewerPose().t }.getOrNull() ?: Vector3(0f, 1.5f, 0.05f)
        var holding = false
        var shown: String? = null
        for (hand in Frame.controllers()) {
            val pad = hand.getComponent<Controller>()
            if (!pad.isActive) continue
            val pose = hand.tryGetComponent<Transform>()?.transform ?: continue
            val at = pose.t
            val before = lastHand.put(hand, at)
            val down = pad.isDown(ButtonBits.ButtonTriggerR) || pad.isDown(ButtonBits.ButtonTriggerL) ||
                pad.isDown(ButtonBits.ButtonSqueezeR) || pad.isDown(ButtonBits.ButtonSqueezeL)
            val pressed = pad.isPressed(ButtonBits.ButtonTriggerR) || pad.isPressed(ButtonBits.ButtonTriggerL) ||
                pad.isPressed(ButtonBits.ButtonSqueezeR) || pad.isPressed(ButtonBits.ButtonSqueezeL)
            if (before != null && dt > 0f) strikeBell(before, at, dt, hand)
            if (held != null && heldHand == hand) {
                if (held == Mala) {
                    holding = true
                    follow(pose, viewer, dt)
                    tickMala(hand, at, down, pressed, dt)
                } else if (down) {
                    holding = true
                    follow(pose, viewer, dt)
                }
                continue
            }
            // One offering at a time.
            if (held != null) continue
            val near = nearest(at)
            val target = near ?: aimedAt(pose)
            if (target != hovering[hand]) {
                if (target != null) {
                    hovering[hand] = target
                    Haptics.pulse(hand, 0.12f, 12)
                } else {
                    hovering.remove(hand)
                }
            }
            if (shown == null) shown = target
            if (target == Ghanta) {
                if (pressed) {
                    val bellAt = bellCentre()
                    ringGhanta(bellAt, hand, bellAt.x - at.x, bellAt.z - at.z)
                }
                continue
            }
            if (target != null && ((near != null && down) || pressed)) {
                take(target, hand, pulled = near == null)
                holding = true
            }
        }
        if (!holding && held != null) drop(viewer)
        showHover(if (held != null) null else shown)
        Mandir.thaliHeld = held == "aarti_thali"
        Mandir.heldProp = held
    }

    private fun reset() {
        letGo()
        flights.clear()
        hovering.clear()
        lastHand.clear()
        aarti.reset()
        bell = BellSwing(MandirScene.ghantaDrop)
        MandirWorld.props.forEach { (name, entity) ->
            val home = MandirScene.homes[name] ?: return@forEach
            entity.setComponent(Transform(Pose(home)))
        }
        showHover(null)
        MandirScene.resetFlames()
    }

    private fun letGo() {
        if (held == Mala) Japa.active = false
        held = null
        heldHand = null
        gesture = null
        lastPos = null
        pullFrom = null
    }

    private fun propAt(name: String): Vector3? = MandirWorld.props[name]?.tryGetComponent<Transform>()?.transform?.t

    private fun bellCentre(): Vector3 {
        val pose = MandirWorld.props[Ghanta]?.tryGetComponent<Transform>()?.transform ?: return MandirPlace.ghanta
        return MandirScene.bellCentre(pose)
    }

    /** Where a hand or ray takes hold of a prop: a little above its base, or the bell itself. */
    private fun holdPoint(name: String): Vector3? = when {
        name == Ghanta -> bellCentre()
        // Once given, the coin is inside the peti, out of sight and out of reach.
        name == "coin" && OfferingKind.Daan in Mandir.done -> null
        else -> propAt(name)?.let { it + Vector3(0f, 0.04f, 0f) }
    }

    /** The prop within reach of [hand], nearest first; nothing in flight. */
    private fun nearest(hand: Vector3): String? {
        var best: String? = null
        var bestDist = Float.MAX_VALUE
        for (name in MandirWorld.props.keys) {
            if (name in flights) continue
            val point = holdPoint(name) ?: continue
            val reach = if (name == Ghanta) BellReach else OfferingRules.GrabReach
            val dist = distance(hand, point)
            if (dist < reach && dist < bestDist) {
                bestDist = dist
                best = name
            }
        }
        return best
    }

    /** The prop the controller's ray points at, if any. */
    private fun aimedAt(pose: Pose): String? {
        val origin = pose.t.vec()
        val dir = pose.forward().normalize().vec()
        var best: String? = null
        var bestScore = Float.MAX_VALUE
        for (name in MandirWorld.props.keys) {
            if (name in flights) continue
            val point = holdPoint(name) ?: continue
            val score = OfferingRules.rayAim(origin, dir, point.vec()) ?: continue
            if (score < bestScore) {
                bestScore = score
                best = name
            }
        }
        return best
    }

    /** The hovered prop grows a little and gets its label. The ghanta only gets the label. */
    private fun showHover(name: String?) {
        Mandir.hover = name
        for ((prop, entity) in MandirWorld.props) {
            val want = if (prop == name && prop != Ghanta) HoverScale else 1f
            if (scaled[prop] == want) continue
            scaled[prop] = want
            entity.setComponent(Scale(Vector3(want)))
        }
    }

    private fun take(name: String, hand: Entity, pulled: Boolean) {
        val at = propAt(name) ?: return
        held = name
        heldHand = hand
        lastPos = at
        pullFrom = if (pulled) at else null
        pullT = 0f
        gesture = if (pulled) null else Gesture(at.vec(), System.nanoTime())
        if (name == "aarti_thali") aarti.reset()
        if (name == Mala) {
            malaArmed = false
            malaDownFor = 0f
            Japa.active = true
        }
        Haptics.pulse(hand, 0.35f, 40)
    }

    private fun follow(pose: Pose, viewer: Vector3, dt: Float) {
        val name = held ?: return
        val entity = MandirWorld.props[name] ?: return
        val tip = pose.t + pose.forward() * 0.10f
        var at = tip
        pullFrom?.let { from ->
            pullT += dt / PullSec
            if (pullT >= 1f) {
                pullFrom = null
                gesture = Gesture(tip.vec(), System.nanoTime())
            } else {
                at = from.lerp(tip, ease(pullT))
            }
        }
        entity.setComponent(Transform(Pose(at, pose.q)))
        val up = pose.q.times(Vector3(0f, 1f, 0f))
        moveFlames(name, at, up)
        lastPos = at
        // Nothing counts until the prop has reached the hand.
        if (pullFrom != null) return
        gesture?.add(at.vec(), System.nanoTime())
        when (name) {
            "diya" -> if (!Mandir.diyaLit && OfferingRules.touchesFlame(MandirScene.diyaWick.vec(), MandirPlace.jyoti.vec())) {
                lightDiya(MandirScene.diyaWick)
            } else if (Mandir.diyaLit) {
                lightDeepmalaNear(MandirScene.diyaWick)
            }
            "agarbatti" -> {
                val tipAt = MandirScene.incenseTip.vec()
                val fromJyoti = OfferingRules.touchesFlame(tipAt, MandirPlace.jyoti.vec())
                val fromDiya = Mandir.diyaLit && OfferingRules.touchesFlame(tipAt, MandirScene.diyaWick.vec())
                if (!Mandir.agarbattiLit && (fromJyoti || fromDiya)) lightIncense()
                if (Mandir.agarbattiLit) lightDeepmalaNear(MandirScene.incenseTip)
            }
            "lota" -> if (pourT < 0f && !Mandir.festivalDone) {
                val toGarbha = MandirPlace.garbha - at
                if (OfferingRules.pours(up.vec(), toGarbha.vec(), atThreshold = MandirScene.mark == 1)) {
                    startPour(at + up * LotaMouth)
                }
            }
            "aarti_thali" -> trackCircle(at, viewer)
            "kumkum" -> if (headNear(at)) applyTilak()
        }
    }

    /** The flame, smoke and their lights follow whichever prop carries them. */
    private fun moveFlames(name: String, at: Vector3, up: Vector3 = Vector3(0f, 1f, 0f)) {
        when (name) {
            "diya" -> {
                MandirScene.diyaWick = at + up * OfferingRules.WickHeight
                if (Mandir.diyaLit) moveLight(MandirWorld.diyaLight, MandirScene.diyaWick)
            }
            "agarbatti" -> {
                MandirScene.incenseTip = at + up * OfferingRules.IncenseTipHeight
                if (Mandir.agarbattiLit) moveLight(MandirWorld.incenseLight, MandirScene.incenseTip)
            }
            "aarti_thali" -> MandirScene.thaliFlame = at + up * OfferingRules.ThaliFlameHeight
        }
    }

    private fun moveLight(light: Entity?, where: Vector3) {
        light?.setComponent(Transform(Pose(where + Vector3(0f, 0.04f, 0f))))
    }

    private fun drop(viewer: Vector3) {
        val name = held ?: return
        val pos = propAt(name) ?: return
        val home = MandirScene.homes[name] ?: pos
        val hand = heldHand
        val arrived = pullFrom == null
        val move = gesture ?: Gesture(pos.vec(), System.nanoTime())
        val release = pos.vec()
        val velocity = move.velocity()
        val feet = MandirPlace.feet
        letGo()
        if (!arrived) {
            glide(name, home)
            return
        }
        val toGarbha = OfferingRules.offeredToGarbha(move, release, velocity, feet.vec(), MandirPlace.garbha.vec())
        when (name) {
            "diya" -> if (Mandir.diyaLit && OfferingRules.near(release, feet.vec(), 0.45f)) {
                fly(name, feet + Vector3(-0.24f, 0f, 0.02f))
            } else {
                settle(name, pos, home)
            }
            "marigold", "lotus" -> if (toGarbha) {
                Mandir.mark(OfferingKind.Pushp, R.string.offer_pushp)
                Haptics.pulse(hand, 0.4f, 50)
                fly(name, feet + Vector3(if (name == "lotus") 0.10f else -0.10f, 0f, 0f))
            } else {
                settle(name, pos, home)
            }
            "coconut", "laddoo" -> when {
                near(pos, viewer, 0.32f) && OfferingKind.Prasad in Mandir.done -> {
                    Mandir.receivePrasad()
                    glide(name, home)
                }
                toGarbha -> {
                    Mandir.mark(OfferingKind.Prasad, R.string.offer_prasad)
                    Haptics.pulse(hand, 0.4f, 50)
                    val spot = feet + Vector3(if (name == "coconut") 0.22f else 0.30f, 0f, 0.04f)
                    fly(name, spot) { if (name == "coconut") MandirSound.at("coconut", spot, 0.85f) }
                }
                else -> settle(name, pos, home)
            }
            "coin" -> if (OfferingRules.intoPeti(move, release, velocity, MandirPlace.peti.vec())) {
                Mandir.mark(OfferingKind.Daan, R.string.offer_daan)
                Haptics.pulse(hand, 0.5f, 60)
                // Into the slot, out of sight inside the box.
                fly(name, MandirPlace.peti - Vector3(0f, 0.08f, 0f)) { MandirSound.at("coins", MandirPlace.peti, 0.9f) }
            } else {
                settle(name, pos, home)
            }
            "agarbatti" -> when {
                !OfferingRules.incensePlanted(move, release, MandirPlace.incense.vec()) -> settle(name, pos, home)
                Mandir.agarbattiLit -> {
                    Mandir.agarbattiPlanted = true
                    Mandir.mark(OfferingKind.Agarbatti, R.string.offer_agarbatti)
                    // The stick's foot goes a little into the sand.
                    park(name, MandirPlace.incense - Vector3(0f, 0.03f, 0f))
                }
                else -> {
                    Mandir.setEvent(R.string.hint_light_incense)
                    glide(name, home)
                }
            }
            "kumkum" -> {
                if (headNear(pos)) applyTilak()
                settle(name, pos, home)
            }
            // Festival offerings go to the feet the way flowers and naivedya do.
            "modak", "hibiscus" -> if (toGarbha && !Mandir.festivalDone) {
                Mandir.markFestival()
                Haptics.pulse(hand, 0.4f, 50)
                fly(name, feet + Vector3(if (name == "modak") -0.22f else 0f, 0f, 0.08f))
            } else {
                settle(name, pos, home)
            }
            else -> settle(name, pos, home)
        }
    }

    /** The mala in hand: each press is a bead, and a long press or a press over its table sets it down. */
    private fun tickMala(hand: Entity, at: Vector3, down: Boolean, pressed: Boolean, dt: Float) {
        if (!down) {
            malaArmed = true
            malaDownFor = 0f
            return
        }
        if (!malaArmed) return
        malaDownFor += dt
        val home = MandirScene.homes[Mala]
        val overTable = home != null && distance(at, home) < MalaSetDownReach
        if ((pressed && overTable) || malaDownFor >= MalaLongPressSec) {
            malaArmed = false
            letGo()
            if (home != null) glide(Mala, home)
            return
        }
        if (!pressed) return
        when (Japa.advance()) {
            JapaCounter.Bead.Bead -> Haptics.pulse(hand, 0.15f, 12)
            JapaCounter.Bead.Quarter -> Haptics.pulse(hand, 0.45f, 40)
            JapaCounter.Bead.Round -> {
                Haptics.pulse(hand, 0.8f, 120)
                MandirSound.at("aarti_bell", at, 0.6f)
            }
        }
    }

    /** A lit wick or incense tip touched to the deepmala lights each small diya it reaches. */
    private fun lightDeepmalaNear(tip: Vector3) {
        if (Festivals.active?.kind != FestivalKind.Deepmala || Mandir.festivalDone) return
        val wicks = MandirScene.deepmalaWicks()
        wicks.forEachIndexed { index, wick ->
            if (index in Mandir.deepmalaLit || !OfferingRules.touchesFlame(tip.vec(), wick.vec())) return@forEachIndexed
            Mandir.deepmalaLit.add(index)
            MandirScene.lightDeepmala(index)
            MandirSound.at("ignite", wick, 0.4f)
            Haptics.pulse(heldHand, 0.3f, 30)
        }
        if (Mandir.deepmalaLit.size >= wicks.size) Mandir.markFestival()
    }

    /** Water leaves the lota's mouth and arcs to the lingam. */
    private fun startPour(mouth: Vector3) {
        pourT = 0f
        pourFrom = mouth
        MandirScene.ensureDrops(PourDrops)
        Haptics.pulse(heldHand, 0.3f, 60)
    }

    private fun stepPour(dt: Float) {
        if (pourT < 0f) return
        pourT += dt
        val to = MandirPlace.garbha + Vector3(0f, LingamTop, 0f)
        val rise = (distance(pourFrom, to) * 0.18f).coerceIn(0.15f, 0.6f)
        MandirWorld.drops.forEachIndexed { index, drop ->
            // Each drop leaves a little after the last, and each takes the same time to land.
            val t = (pourT - index * PourStagger) / PourFlightSec
            val inAir = t in 0f..1f
            drop.setComponent(Visible(inAir && Mandir.inside))
            if (inAir) {
                val at = pourFrom.lerp(to, t) + Vector3(0f, rise * 4f * t * (1f - t), 0f)
                drop.setComponent(Transform(Pose(at)))
            }
        }
        if (pourT >= PourFlightSec + PourStagger * PourDrops) {
            pourT = -1f
            MandirWorld.drops.forEach { it.setComponent(Visible(false)) }
            Mandir.markFestival()
        }
    }

    /** Let go over the stand: it stays where it was put. Anywhere else: it goes back home. */
    private fun settle(name: String, at: Vector3, home: Vector3) {
        val top = MandirPlace.stand
        if (OfferingRules.overStand(at.vec(), top.vec())) {
            glide(name, Vector3(at.x, top.y + 0.003f, at.z), seconds = 0.15f)
        } else {
            glide(name, home)
        }
    }

    private fun park(name: String, where: Vector3) {
        MandirWorld.props[name]?.setComponent(Transform(Pose(where)))
        moveFlames(name, where)
    }

    /** An offering's arc to where it was offered. */
    private fun fly(name: String, to: Vector3, land: (() -> Unit)? = null) {
        val from = propAt(name) ?: return park(name, to)
        val rise = (distance(from, to) * 0.22f).coerceIn(0.12f, 0.6f)
        flights[name] = Flight(from, to, OfferSec, rise, land)
    }

    /** Back to [to] without fuss. */
    private fun glide(name: String, to: Vector3, seconds: Float = GlideSec) {
        val from = propAt(name) ?: return park(name, to)
        flights[name] = Flight(from, to, seconds, 0.03f, null)
    }

    private fun stepFlights(dt: Float) {
        if (flights.isEmpty()) return
        val landed = ArrayList<String>()
        for ((name, flight) in flights) {
            flight.t = (flight.t + dt / flight.seconds).coerceAtMost(1f)
            val t = flight.t
            val at = flight.from.lerp(flight.to, ease(t)) + Vector3(0f, flight.rise * 4f * t * (1f - t), 0f)
            MandirWorld.props[name]?.setComponent(Transform(Pose(at)))
            moveFlames(name, at)
            if (t >= 1f) landed.add(name)
        }
        for (name in landed) {
            val flight = flights.remove(name) ?: continue
            park(name, flight.to)
            flight.land?.invoke()
        }
    }

    private fun lightDiya(where: Vector3) {
        if (Mandir.diyaLit) return
        Mandir.diyaLit = true
        MandirScene.setDiyaLight(true, where)
        MandirSound.at("ignite", where, 0.8f)
        Mandir.mark(OfferingKind.Diya, R.string.offer_diya)
        Haptics.pulse(heldHand, 0.55f, 70)
    }

    private fun lightIncense() {
        Mandir.agarbattiLit = true
        MandirSound.at("ignite", MandirScene.incenseTip, 0.7f)
        MandirScene.setIncenseLight(true, MandirScene.incenseTip)
        Mandir.setEvent(R.string.hint_incense_lit)
        Haptics.pulse(heldHand, 0.3f, 40)
    }

    /** A hand swept through the bell rings it, the way it would in a temple. */
    private fun strikeBell(before: Vector3, at: Vector3, dt: Float, hand: Entity) {
        val speed = distance(before, at) / dt
        if (speed < StrikeSpeed) return
        if (distance(at, bellCentre()) > StrikeReach) return
        ringGhanta(at, hand, at.x - before.x, at.z - before.z)
    }

    private fun ringGhanta(where: Vector3, hand: Entity?, dirX: Float, dirZ: Float) {
        val now = System.currentTimeMillis()
        if (now < ghantaUntil) return
        ghantaUntil = now + 900L
        MandirSound.at("ghanta", where, 1f)
        Mandir.mark(OfferingKind.Ghanta, R.string.offer_ghanta)
        Haptics.pulse(hand, 0.8f, 90)
        bell.kick(dirX, dirZ)
    }

    private fun stepBell(dt: Float) {
        if (!bell.moving) return
        bell.step(dt)
        MandirWorld.props[Ghanta]?.setComponent(Transform(MandirScene.ghantaPose(bell.aboutX, bell.aboutZ)))
    }

    private fun applyTilak() {
        if (Mandir.tilakOn) return
        Mandir.applyTilak()
        Mandir.mark(OfferingKind.Tilak, R.string.offer_tilak)
        Haptics.pulse(heldHand, 0.4f, 50)
    }

    /** Circles in the upright plane facing the garbha, wherever in front of the member they are drawn. */
    private fun trackCircle(pos: Vector3, viewer: Vector3) {
        if (Mandir.aartiDone) return
        val fx = MandirPlace.garbha.x - viewer.x
        val fz = MandirPlace.garbha.z - viewer.z
        val length = kotlin.math.sqrt(fx * fx + fz * fz).coerceAtLeast(1e-3f)
        // Right of the member, horizontal.
        val lateral = pos.x * (-fz / length) + pos.z * (fx / length)
        val circles = aarti.add(lateral, pos.y, Frame.dt).coerceAtMost(AartiRounds)
        if (circles > Mandir.aartiCircles) {
            Mandir.aartiCircles = circles
            Mandir.setEvent(R.string.hint_aarti_count)
            Haptics.pulse(heldHand, 0.25f, 30)
            if (circles >= AartiRounds) {
                Mandir.mark(OfferingKind.Aarti, R.string.offer_aarti)
                MandirSound.aartiMoment(MandirPlace.stand)
            }
        }
    }

    private fun headNear(pos: Vector3): Boolean {
        val viewer = runCatching { getScene().getViewerPose().t }.getOrNull() ?: return false
        return distance(pos, viewer + Vector3(0f, 0.08f, 0f)) < 0.28f
    }

    private fun near(a: Vector3, b: Vector3, reach: Float) = distance(a, b) < reach

    private fun distance(a: Vector3, b: Vector3): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        val dz = a.z - b.z
        return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    }

    private fun ease(t: Float) = t * t * (3f - 2f * t)

    companion object {
        const val Ghanta = "ghanta"
        const val Mala = "mala"

        /** Setting the mala down: a press this near its place, or a press held this long. */
        const val MalaSetDownReach = 0.25f
        const val MalaLongPressSec = 0.8f

        /** The lota's mouth above its base, and the lingam's top above the garbha floor marker. */
        const val LotaMouth = 0.122f
        const val LingamTop = 1.05f
        const val PourDrops = 10
        const val PourStagger = 0.09f
        const val PourFlightSec = 0.9f
        const val HoverScale = 1.15f
        const val PullSec = 0.18f
        const val OfferSec = 0.65f
        const val GlideSec = 0.35f
        const val AartiRounds = 7

        /** A press this close to the bell rings it; a hand this fast through it strikes it. */
        const val BellReach = 0.30f
        const val StrikeReach = 0.22f
        const val StrikeSpeed = 0.8f
    }
}

fun Vector3.vec() = Vec(x, y, z)

/** Controller rumble through the Spatial SDK. Hands have nothing to rumble, so they're skipped. */
object Haptics {
    /** Set by the activity: hand, amplitude 0..1, duration in ms. */
    var sink: ((Hand, Float, Int) -> Unit)? = null

    fun pulse(controller: Entity?, amplitude: Float, durationMs: Int) {
        val entity = controller ?: return
        val out = sink ?: return
        val pad = entity.tryGetComponent<Controller>() ?: return
        if (pad.type == ControllerType.HAND) return
        val hand = when (entity.tryGetComponent<AvatarAttachment>()?.type) {
            "left_controller" -> Hand.LEFT
            "right_controller" -> Hand.RIGHT
            else -> return
        }
        runCatching { out(hand, amplitude.coerceIn(0f, 1f), durationMs) }
    }
}
