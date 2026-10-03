package com.hinvr.quest

import com.meta.spatial.core.Entity
import com.meta.spatial.core.Pose
import com.meta.spatial.core.Query
import com.meta.spatial.core.SystemBase
import com.meta.spatial.core.Vector3
import com.meta.spatial.runtime.ButtonBits
import com.meta.spatial.toolkit.Controller
import com.meta.spatial.toolkit.Transform
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

private const val GrabReach = 0.32f
private const val DropReach = 0.38f

class OfferingSystem : SystemBase() {
    private var held: String? = null
    private var heldHand: Entity? = null
    private var lastPos: Vector3? = null
    private var lastAngle: Float? = null
    private var angleAcc = 0f
    private var ghantaUntil = 0L
    private var lastNs = 0L
    private var dt = 0.016f
    private var wasInside = false

    override fun execute() {
        if (Mandir.inside && !wasInside) reset()
        wasInside = Mandir.inside
        if (!Mandir.inside || Mandir.sankalpOpen) {
            held = null
            return
        }
        val now = System.nanoTime()
        dt = if (lastNs == 0L) 0.016f else ((now - lastNs) / 1_000_000_000f).coerceIn(0f, 0.05f)
        lastNs = now
        val controllers = Query.where { has(Controller.id) }.eval().filter { it.isLocal() }
        var viewer = Vector3(0f, 1.5f, 1.55f)
        runCatching { viewer = getScene().getViewerPose().t }
        var anyHold = false
        for (hand in controllers) {
            val pad = hand.getComponent<Controller>()
            if (!pad.isActive) continue
            val pose = hand.tryGetComponent<Transform>()?.transform ?: continue
            val trigger = pad.isDown(ButtonBits.ButtonTriggerR) || pad.isDown(ButtonBits.ButtonTriggerL)
            val squeeze = pad.isDown(ButtonBits.ButtonSqueezeR) || pad.isDown(ButtonBits.ButtonSqueezeL)
            val gripping = trigger || squeeze
            if (!gripping) continue
            anyHold = true
            if (held == null) {
                grabNearest(pose.t, hand)
            } else if (heldHand == null || heldHand == hand) {
                heldHand = hand
                follow(pose)
            }
        }
        if (!anyHold && held != null) {
            drop(viewer)
        }
        tickAarti()
    }

    fun reset() {
        held = null
        heldHand = null
        lastPos = null
        lastAngle = null
        angleAcc = 0f
        MandirWorld.props.forEach { (name, entity) ->
            val home = MandirScene.homes[name] ?: return@forEach
            entity.setComponent(Transform(Pose(home)))
        }
    }

    private fun grabNearest(hand: Vector3, controller: Entity) {
        var best: String? = null
        var bestDist = GrabReach
        for ((name, entity) in MandirWorld.props) {
            val pos = entity.tryGetComponent<Transform>()?.transform?.t ?: continue
            val dist = distance(hand, pos)
            if (dist < bestDist) {
                bestDist = dist
                best = name
            }
        }
        val name = best ?: return
        held = name
        heldHand = controller
        lastPos = MandirWorld.props[name]?.tryGetComponent<Transform>()?.transform?.t
        buzz(0.35f, 40)
        if (name == "ghanta") ringGhanta(lastPos ?: MandirPlace.ghanta)
    }

    private fun follow(pose: Pose) {
        val name = held ?: return
        val entity = MandirWorld.props[name] ?: return
        val tip = pose.t + pose.forward() * 0.14f
        val last = lastPos
        entity.setComponent(Transform(Pose(tip, pose.q)))
        if (name == "ghanta" && last != null && distance(last, tip) > 0.09f) {
            ringGhanta(tip)
        }
        if (name == "diya") MandirScene.diyaPose = Pose(tip)
        if (name == "agarbatti") MandirScene.incensePose = Pose(tip)
        lastPos = tip
        if (name == "aarti_thali") trackCircle(tip)
        if (name == "kumkum" && headNear(tip)) applyTilak()
        if (name == "agarbatti" && Mandir.diyaLit && near(tip, MandirScene.diyaPose.t, 0.16f)) {
            if (!Mandir.agarbattiLit) {
                Mandir.agarbattiLit = true
                MandirSound.at("ignite", tip, 0.7f)
                MandirScene.setIncenseLight(true, tip)
            }
        }
    }

    private fun drop(viewer: Vector3) {
        val name = held ?: return
        val entity = MandirWorld.props[name] ?: return
        val pos = entity.tryGetComponent<Transform>()?.transform?.t ?: return
        when (name) {
            "diya" -> {
                if (near(pos, MandirPlace.stand, DropReach)) {
                    lightDiya(pos)
                    park(name, MandirScene.homes[name] ?: MandirPlace.stand)
                }
            }
            "marigold", "lotus" -> {
                if (near(pos, MandirPlace.feet, 0.45f) || near(pos, MandirPlace.garbha, 0.7f)) {
                    Mandir.mark(OfferingKind.Pushp, "Flowers at the feet. ${Mandir.Honesty}")
                    park(name, MandirPlace.feet + Vector3((name.hashCode() % 7) * 0.03f, 0f, 0f))
                } else {
                    park(name, MandirScene.homes[name] ?: pos)
                }
            }
            "coconut", "laddoo" -> {
                when {
                    near(pos, viewer, 0.32f) && OfferingKind.Prasad in Mandir.done -> {
                        Mandir.receivePrasad()
                        park(name, MandirScene.homes[name] ?: pos)
                    }
                    near(pos, MandirPlace.garbha, 0.7f) || near(pos, MandirPlace.feet, 0.45f) -> {
                        Mandir.mark(OfferingKind.Prasad, "Naivedya is offered. ${Mandir.Honesty}")
                        if (name == "coconut") MandirSound.at("coconut", pos, 0.85f)
                        park(name, MandirPlace.feet + Vector3(0.12f, 0f, 0.04f))
                    }
                    else -> park(name, MandirScene.homes[name] ?: pos)
                }
            }
            "coin" -> {
                if (near(pos, MandirPlace.peti, 0.4f)) {
                    Mandir.mark(OfferingKind.Daan, "The peti is closed. ${Mandir.Honesty}")
                    MandirSound.at("coins", MandirPlace.peti, 0.9f)
                    park(name, MandirPlace.peti + Vector3(0f, 0.12f, 0f))
                } else {
                    park(name, MandirScene.homes[name] ?: pos)
                }
            }
            "agarbatti" -> {
                if (near(pos, MandirPlace.incense, 0.28f)) {
                    Mandir.agarbattiPlanted = true
                    if (Mandir.agarbattiLit || Mandir.diyaLit) {
                        Mandir.agarbattiLit = true
                        MandirScene.setIncenseLight(true, MandirPlace.incense)
                    }
                    Mandir.mark(OfferingKind.Agarbatti, "Incense is planted. ${Mandir.Honesty}")
                    park(name, MandirPlace.incense + Vector3(0f, 0.06f, 0f))
                    MandirScene.incensePose = Pose(MandirPlace.incense + Vector3(0f, 0.08f, 0f))
                } else {
                    park(name, MandirScene.homes[name] ?: pos)
                }
            }
            "kumkum" -> {
                if (headNear(pos)) applyTilak()
                park(name, MandirScene.homes[name] ?: pos)
            }
            "aarti_thali" -> {
                park(name, MandirScene.homes[name] ?: MandirPlace.stand)
                lastAngle = null
            }
            "ghanta" -> park(name, MandirPlace.ghanta)
            else -> park(name, MandirScene.homes[name] ?: pos)
        }
        held = null
        heldHand = null
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
        Mandir.mark(OfferingKind.Diya, "The diya is lit. ${Mandir.Honesty}")
        buzz(0.55f, 70)
    }

    private fun ringGhanta(where: Vector3) {
        val now = System.currentTimeMillis()
        if (now < ghantaUntil) return
        ghantaUntil = now + 900L
        MandirSound.at("ghanta", where, 1f)
        Mandir.mark(OfferingKind.Ghanta, "The ghanta sounds. ${Mandir.Honesty}")
        buzz(0.8f, 90)
    }

    private fun applyTilak() {
        if (Mandir.tilakOn) return
        Mandir.tilakOn = true
        Mandir.mark(OfferingKind.Tilak, "Tilak is received. ${Mandir.Honesty}")
        buzz(0.4f, 50)
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
            Mandir.hint = "Aarti · ${Mandir.aartiCircles} of 7"
            buzz(0.25f, 30)
            if (Mandir.aartiCircles >= 7 && !Mandir.aartiDone) {
                Mandir.mark(OfferingKind.Aarti, "Aarti is complete. ${Mandir.Honesty}")
                MandirSound.aartiMoment(MandirPlace.stand)
            }
        }
    }

    private fun tickAarti() {
        if (held != "aarti_thali") lastAngle = null
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
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    private fun buzz(amplitude: Float, durationMs: Int) {
        Haptics.pulse(amplitude, durationMs)
    }
}

object Haptics {
    var pulse: (Float, Int) -> Unit = { _, _ -> }
}
