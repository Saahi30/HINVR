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
import com.meta.spatial.toolkit.Transform
import kotlin.math.atan2
import kotlin.math.hypot

class OfferingSystem : SystemBase() {
    private var held: String? = null
    private var heldHand: Entity? = null
    private var gesture: Gesture? = null
    private var lastPos: Vector3? = null
    private var lastAngle: Float? = null
    private var angleAcc = 0f
    private var ghantaUntil = 0L
    private var wasInside = false

    override fun execute() {
        val inside = Mandir.inside
        if (inside && !wasInside) reset()
        wasInside = inside
        if (!inside || Mandir.phase != MandirPhase.Visit || !MandirScene.ready) {
            held = null
            heldHand = null
            gesture = null
            return
        }
        val viewer = runCatching { getScene().getViewerPose().t }.getOrNull() ?: Vector3(0f, 1.5f, 0.05f)
        var holding = false
        for (hand in Frame.controllers()) {
            val pad = hand.getComponent<Controller>()
            if (!pad.isActive) continue
            val pose = hand.tryGetComponent<Transform>()?.transform ?: continue
            val gripping = pad.isDown(ButtonBits.ButtonTriggerR) || pad.isDown(ButtonBits.ButtonTriggerL) ||
                pad.isDown(ButtonBits.ButtonSqueezeR) || pad.isDown(ButtonBits.ButtonSqueezeL)
            if (!gripping) {
                if (heldHand == hand) heldHand = null
                continue
            }
            if (held == null) {
                grabNearest(pose.t, hand)
                if (held != null) holding = true
            } else if (heldHand == null || heldHand == hand) {
                heldHand = hand
                holding = true
                follow(pose)
            }
        }
        if (!holding && held != null) drop(viewer)
        if (held != "aarti_thali") lastAngle = null
    }

    private fun reset() {
        held = null
        heldHand = null
        gesture = null
        lastPos = null
        lastAngle = null
        angleAcc = 0f
        MandirWorld.props.forEach { (name, entity) ->
            val home = MandirScene.homes[name] ?: return@forEach
            entity.setComponent(Transform(Pose(home)))
        }
        MandirScene.diyaPose = Pose(MandirScene.homes["diya"] ?: MandirPlace.stand)
        MandirScene.incensePose = Pose(MandirPlace.incense + Vector3(0f, 0.08f, 0f))
    }

    private fun grabNearest(hand: Vector3, controller: Entity) {
        var best: String? = null
        var bestDist = OfferingRules.GrabReach
        for ((name, entity) in MandirWorld.props) {
            val pos = entity.tryGetComponent<Transform>()?.transform?.t ?: continue
            val dist = distance(hand, pos)
            if (dist < bestDist) {
                bestDist = dist
                best = name
            }
        }
        val name = best ?: return
        val at = MandirWorld.props[name]?.tryGetComponent<Transform>()?.transform?.t ?: return
        held = name
        heldHand = controller
        lastPos = at
        gesture = Gesture(at.vec(), System.nanoTime())
        Haptics.pulse(controller, 0.35f, 40)
        if (name == "ghanta") ringGhanta(at, controller)
    }

    private fun follow(pose: Pose) {
        val name = held ?: return
        val entity = MandirWorld.props[name] ?: return
        val tip = pose.t + pose.forward() * 0.14f
        val last = lastPos
        entity.setComponent(Transform(Pose(tip, pose.q)))
        gesture?.add(tip.vec(), System.nanoTime())
        if (name == "ghanta" && last != null && distance(last, tip) > 0.09f) ringGhanta(tip, heldHand)
        if (name == "diya") MandirScene.diyaPose = Pose(tip)
        if (name == "agarbatti") MandirScene.incensePose = Pose(tip)
        lastPos = tip
        if (name == "aarti_thali") trackCircle(tip)
        if (name == "kumkum" && headNear(tip)) applyTilak()
        if (name == "agarbatti" && Mandir.diyaLit && near(tip, MandirScene.diyaPose.t, 0.16f) && !Mandir.agarbattiLit) {
            Mandir.agarbattiLit = true
            MandirSound.at("ignite", tip, 0.7f)
            MandirScene.setIncenseLight(true, tip)
            Haptics.pulse(heldHand, 0.3f, 40)
        }
    }

    private fun drop(viewer: Vector3) {
        val name = held ?: return
        val entity = MandirWorld.props[name] ?: return
        val pos = entity.tryGetComponent<Transform>()?.transform?.t ?: return
        val move = gesture ?: Gesture(pos.vec(), System.nanoTime())
        val release = pos.vec()
        val velocity = move.velocity()
        val home = MandirScene.homes[name] ?: pos
        when (name) {
            "diya" -> {
                if (OfferingRules.diyaSetDown(move, release, MandirPlace.stand.vec())) lightDiya(home)
                park(name, home)
            }
            "marigold", "lotus" -> {
                if (OfferingRules.offeredToGarbha(move, release, velocity, MandirPlace.feet.vec(), MandirPlace.garbha.vec())) {
                    Mandir.mark(OfferingKind.Pushp, R.string.offer_pushp)
                    Haptics.pulse(heldHand, 0.4f, 50)
                    park(name, MandirPlace.feet + Vector3(if (name == "lotus") 0.09f else -0.09f, 0f, 0f))
                } else {
                    park(name, home)
                }
            }
            "coconut", "laddoo" -> {
                when {
                    near(pos, viewer, 0.32f) && OfferingKind.Prasad in Mandir.done -> {
                        Mandir.receivePrasad()
                        park(name, home)
                    }
                    OfferingRules.offeredToGarbha(move, release, velocity, MandirPlace.feet.vec(), MandirPlace.garbha.vec()) -> {
                        Mandir.mark(OfferingKind.Prasad, R.string.offer_prasad)
                        if (name == "coconut") MandirSound.at("coconut", MandirPlace.feet, 0.85f)
                        Haptics.pulse(heldHand, 0.4f, 50)
                        park(name, MandirPlace.feet + Vector3(if (name == "coconut") 0.16f else 0.24f, 0f, 0.04f))
                    }
                    else -> park(name, home)
                }
            }
            "coin" -> {
                if (OfferingRules.intoPeti(move, release, velocity, MandirPlace.peti.vec())) {
                    Mandir.mark(OfferingKind.Daan, R.string.offer_daan)
                    MandirSound.at("coins", MandirPlace.peti, 0.9f)
                    Haptics.pulse(heldHand, 0.5f, 60)
                    park(name, MandirPlace.peti + Vector3(0f, 0.22f, 0f))
                } else {
                    park(name, home)
                }
            }
            "agarbatti" -> {
                if (OfferingRules.incensePlanted(move, release, MandirPlace.incense.vec())) {
                    Mandir.agarbattiPlanted = true
                    if (Mandir.agarbattiLit || Mandir.diyaLit) {
                        Mandir.agarbattiLit = true
                        MandirScene.setIncenseLight(true, MandirPlace.incense)
                    }
                    Mandir.mark(OfferingKind.Agarbatti, R.string.offer_agarbatti)
                    park(name, MandirPlace.incense + Vector3(0f, 0.06f, 0f))
                    MandirScene.incensePose = Pose(MandirPlace.incense + Vector3(0f, 0.08f, 0f))
                } else {
                    park(name, home)
                }
            }
            "kumkum" -> {
                if (headNear(pos)) applyTilak()
                park(name, home)
            }
            "aarti_thali" -> {
                park(name, home)
                lastAngle = null
            }
            "ghanta" -> park(name, MandirPlace.ghanta)
            else -> park(name, home)
        }
        held = null
        heldHand = null
        gesture = null
        lastPos = null
    }

    private fun park(name: String, where: Vector3) {
        MandirWorld.props[name]?.setComponent(Transform(Pose(where)))
        if (name == "diya") MandirScene.diyaPose = Pose(where)
        if (name == "agarbatti") MandirScene.incensePose = Pose(where)
    }

    private fun lightDiya(where: Vector3) {
        if (Mandir.diyaLit) return
        Mandir.diyaLit = true
        MandirScene.setDiyaLight(true, where)
        MandirSound.at("ignite", where, 0.8f)
        Mandir.mark(OfferingKind.Diya, R.string.offer_diya)
        Haptics.pulse(heldHand, 0.55f, 70)
    }

    private fun ringGhanta(where: Vector3, hand: Entity?) {
        val now = System.currentTimeMillis()
        if (now < ghantaUntil) return
        ghantaUntil = now + 900L
        MandirSound.at("ghanta", where, 1f)
        Mandir.mark(OfferingKind.Ghanta, R.string.offer_ghanta)
        Haptics.pulse(hand, 0.8f, 90)
    }

    private fun applyTilak() {
        if (Mandir.tilakOn) return
        Mandir.applyTilak()
        Mandir.mark(OfferingKind.Tilak, R.string.offer_tilak)
        Haptics.pulse(heldHand, 0.4f, 50)
    }

    private fun trackCircle(pos: Vector3) {
        val dx = pos.x - MandirPlace.stand.x
        val dz = pos.z - MandirPlace.stand.z
        if (hypot(dx, dz) < 0.16f) return
        val angle = atan2(dx, dz)
        val previous = lastAngle
        lastAngle = angle
        if (previous == null) return
        var delta = angle - previous
        if (delta > Math.PI) delta -= (2.0 * Math.PI).toFloat()
        if (delta < -Math.PI) delta += (2.0 * Math.PI).toFloat()
        angleAcc += kotlin.math.abs(delta)
        val circles = (angleAcc / (2f * Math.PI.toFloat())).toInt()
        if (circles > Mandir.aartiCircles) {
            Mandir.aartiCircles = circles.coerceAtMost(7)
            Mandir.setHint(R.string.hint_aarti_count)
            Haptics.pulse(heldHand, 0.25f, 30)
            if (Mandir.aartiCircles >= 7 && !Mandir.aartiDone) {
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
