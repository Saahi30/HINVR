package com.hinvr.quest

import com.meta.spatial.core.Color4
import com.meta.spatial.core.Entity
import com.meta.spatial.core.Pose
import com.meta.spatial.core.Quaternion
import com.meta.spatial.core.Query
import com.meta.spatial.core.SystemBase
import com.meta.spatial.core.Vector3
import com.meta.spatial.runtime.ButtonBits
import com.meta.spatial.toolkit.Controller
import com.meta.spatial.toolkit.Scale
import com.meta.spatial.toolkit.Transform
import com.meta.spatial.toolkit.Visible
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

class RingInputSystem : SystemBase() {
    private var nextScrollAtMs = 0L
    private var splashUntilMs = 0L
    private var splashGreeting = 0
    private var displayedCenter = 0f
    private var streamPresence = 0f
    private var lastFrameNs = 0L
    private var lastStage = Stage.Splash
    private var grabSpin = 0f
    private var idleSpin = 0f
    private var introAge = 0f
    private var grabbing = false
    private var lastGrabAngle = 0f
    private val cardPoseByIndex = HashMap<Int, Pose>()
    private var dt = 0.016f

    override fun execute() {
        readControllers()
        layout()
    }

    private fun readControllers() {
        val now = System.currentTimeMillis()
        val controllers = Query.where { has(Controller.id) }.eval().filter { it.isLocal() }
        var scroll = 0
        var select = false
        var squeeze = false
        var grabAngle: Float? = null
        var aimed = -1
        val viewer = getScene().getViewerPose().removePitchAndRoll()
        for (controllerEntity in controllers) {
            val controller = controllerEntity.getComponent<Controller>()
            if (!controller.isActive) continue
            val left = controller.isPressed(ButtonBits.ButtonThumbRL) ||
                controller.isPressed(ButtonBits.ButtonThumbLL)
            val right = controller.isPressed(ButtonBits.ButtonThumbRR) ||
                controller.isPressed(ButtonBits.ButtonThumbLR)
            val holdLeft = controller.isDown(ButtonBits.ButtonThumbRL) ||
                controller.isDown(ButtonBits.ButtonThumbLL)
            val holdRight = controller.isDown(ButtonBits.ButtonThumbRR) ||
                controller.isDown(ButtonBits.ButtonThumbLR)
            if (left || (holdLeft && now >= nextScrollAtMs)) scroll -= 1
            if (right || (holdRight && now >= nextScrollAtMs)) scroll += 1
            val gripping = controller.isDown(ButtonBits.ButtonSqueezeL) ||
                controller.isDown(ButtonBits.ButtonSqueezeR)
            if (gripping) {
                squeeze = true
                controllerEntity.tryGetComponent<Transform>()?.transform?.let { pose ->
                    grabAngle = orbitAngle(viewer, pose)
                }
            }
            val pointed = aimedCard(controllerEntity)
            if (pointed >= 0) aimed = pointed
            if (
                controller.isPressed(ButtonBits.ButtonTriggerR) ||
                controller.isPressed(ButtonBits.ButtonTriggerL)
            ) {
                if (!aimsAt(togglePose, controllerEntity, 0.28f, 0.09f) &&
                    !aimsAt(chipPose, controllerEntity, 0.26f, 0.07f)
                ) {
                    select = true
                }
            }
            if (
                Ring.stage == Stage.Menu && !Ring.isWatching &&
                (controller.isPressed(ButtonBits.ButtonA) || controller.isPressed(ButtonBits.ButtonX))
            ) {
                Ring.setPassthrough(!Ring.usePassthrough)
            }
            if (
                controller.isPressed(ButtonBits.ButtonB) ||
                controller.isPressed(ButtonBits.ButtonY)
            ) {
                Ring.back()
            }
        }
        if (squeeze && grabAngle != null) {
            if (grabbing) grabSpin += wrapDegrees(grabAngle!! - lastGrabAngle)
            grabbing = true
            lastGrabAngle = grabAngle!!
        } else {
            grabbing = false
        }
        if (Ring.isWatching || Ring.inSphere) return
        if (Ring.stage == Stage.Splash) {
            if (splashGreeting != Ring.greeting) {
                splashGreeting = Ring.greeting
                splashUntilMs = 0L
            }
            if (splashUntilMs == 0L) splashUntilMs = now + OpeningLengthMs
            if (select || now >= splashUntilMs) Ring.openMenu()
            return
        }
        if (!Ring.showsCards) return
        val count = Ring.deckSize
        if (scroll != 0 && count > 0) {
            grabSpin -= scroll.coerceIn(-1, 1) * (360f / count)
            nextScrollAtMs = now + 420L
        }
        if (aimed >= 0) {
            aimedNoneLast = false
            Ring.lookAt(aimed)
            if (select) Ring.focus(aimed)
        } else {
            aimedNoneLast = true
            if (select) Ring.selectCentered()
        }
    }

    private fun aimedCard(controllerEntity: Entity): Int {
        var best = -1
        var bestScore = Float.MAX_VALUE
        val aim = controllerEntity.tryGetComponent<Transform>()?.transform
        val origin = aim?.t
        val dir = aim?.forward()
        for ((index, pose) in cardPoseByIndex) {
            val along = hitDistance(pose, controllerEntity, 0.32f, 0.44f)
            if (along != null && along < bestScore) {
                bestScore = along
                best = index
                continue
            }
            if (origin == null || dir == null) continue
            val dx = pose.t.x - origin.x
            val dy = pose.t.y - origin.y
            val dz = pose.t.z - origin.z
            val depth = dx * dir.x + dy * dir.y + dz * dir.z
            if (depth < 0.25f) continue
            val px = origin.x + dir.x * depth - pose.t.x
            val py = origin.y + dir.y * depth - pose.t.y
            val pz = origin.z + dir.z * depth - pose.t.z
            val dist = kotlin.math.sqrt((px * px + py * py + pz * pz).toDouble()).toFloat()
            val score = 8f + dist
            if (dist < 0.45f && score < bestScore) {
                bestScore = score
                best = index
            }
        }
        return best
    }

    private fun layout() {
        val now = System.nanoTime()
        dt = if (lastFrameNs == 0L) 0.016f else ((now - lastFrameNs) / 1_000_000_000f).coerceIn(0f, 0.05f)
        lastFrameNs = now
        if (Ring.stage != lastStage) {
            lastStage = Ring.stage
            displayedCenter = Ring.index.toFloat()
            introAge = 0f
            grabSpin = 0f
            idleSpin = 0f
            grabbing = false
            slotOfCard.clear()
            for (slot in RingSlots.shown.indices) RingSlots.shown[slot] = -1
        }
        val count = Ring.deckSize
        val glide = 1f - exp(-7.5f * dt)
        val approach = 1f - exp(-4.2f * dt)
        if (count > 0) {
            displayedCenter = wrapUnit(
                displayedCenter + wrapDelta(Ring.index - displayedCenter, count.toFloat()) * glide,
                count.toFloat(),
            )
        }
        if (Ring.showsCards) {
            introAge += dt
            if (!grabbing && aimedNoneLast) idleSpin += 8f * dt
        }
        val introT = (1f - exp(-3.4f * introAge)).coerceIn(0f, 1f)
        val streamTarget = if (Ring.isWatching && !Ring.leaving) 1f else 0f
        streamPresence += (streamTarget - streamPresence) * approach
        if (Ring.leaving && streamPresence < 0.03f) Ring.finishLeave()

        val ringPresence = 1f - streamPresence
        val splash = Ring.stage == Stage.Splash
        val pairing = Ring.stage == Stage.Pair
        val sphere = Ring.inSphere
        // While pairing the member has to see their phone.
        val passthrough = !sphere && (splash || pairing || Ring.usePassthrough)
        applyPassthrough(passthrough)
        PairScanner.sync(pairing && QuestAccount.wantsCamera)
        lightTheHall()
        SanctumSound.onFrame(Ring.stage, Ring.isWatching || sphere, Ring.diyaLit)
        val viewer = getScene().getViewerPose().removePitchAndRoll()
        if (viewer.t.y < 0.5f) return
        val head = anchorFor(viewer, now, dt)
        if (sphere) {
            showSphereTour(head, viewer)
            return
        }
        hideSphereTour()
        placeOpening(head, splash)
        placeHall(head, viewer, show = !splash && !pairing && !Ring.usePassthrough)
        placeEnvironment(head, show = Ring.stage == Stage.Menu && !Ring.isWatching)
        placePair(head, show = pairing)
        placeCourtyard(head, show = splash || pairing)
        placeProfile(head, show = Ring.stage == Stage.Profile)
        placeChip(head, show = Ring.stage == Stage.Menu && !Ring.isWatching && QuestAccount.isPaired)
        RingWorld.stream?.let { stream ->
            val showing = streamPresence > 0.02f
            stream.setComponent(Visible(showing))
            if (showing) {
                stream.setComponent(
                    Transform(placeInFront(head, 0f, 1.7f, -0.02f)),
                )
                val full = if (Ring.watchingSpherical) 1.42f else 1f
                val scale = 0.68f + (full - 0.68f) * streamPresence
                stream.setComponent(Scale(Vector3(scale, scale, scale)))
            }
        }
        placeRing(head, count, ringPresence, introT)
    }

    private var aimedNoneLast = true

    private fun placeRing(head: Pose, count: Int, ringPresence: Float, introT: Float) {
        cardPoseByIndex.clear()
        if (count > 0) assignSlots(count)
        val spacing = if (count > 0) 360f / count else 360f
        val spin = grabSpin + idleSpin + (1f - introT) * 160f
        val radius = (1.78f + streamPresence * 0.35f) * (0.38f + 0.62f * introT)
        RingWorld.slots.forEachIndexed { slot, entity ->
            if (entity == null) return@forEachIndexed
            if (!Ring.showsCards) {
                entity.setComponent(Visible(false))
                return@forEachIndexed
            }
            val cardIndex = RingSlots.shown[slot]
            if (cardIndex !in 0 until count) {
                entity.setComponent(Visible(false))
                return@forEachIndexed
            }
            val yaw = wrapDegrees(cardIndex * spacing + spin)
            val pose = placeOnCircle(head, yaw, radius, -0.12f)
            cardPoseByIndex[cardIndex] = pose
            val focused = cardIndex == Ring.index
            val scale = (if (focused) 1.10f else 0.90f) * ringPresence * (0.72f + 0.28f * introT)
            entity.setComponent(Visible(ringPresence > 0.04f))
            entity.setComponent(Transform(pose))
            entity.setComponent(Scale(Vector3(scale, scale, scale)))
        }
    }

    private var anchorPos: Vector3? = null
    private var anchorYaw = 0f
    private var targetPos = Vector3(0f, 0f, 0f)
    private var targetYaw = 0f
    private var anchoredStage: Stage? = null
    private var anchoredWatching = false
    private var lookingAwaySinceNs = 0L

    private fun anchorFor(viewer: Pose, now: Long, dt: Float): Pose {
        val viewerYaw = yawOf(viewer)
        val pos = anchorPos
        if (pos == null) {
            anchorPos = viewer.t
            anchorYaw = viewerYaw
            targetPos = viewer.t
            targetYaw = viewerYaw
            anchoredStage = Ring.stage
            anchoredWatching = Ring.isWatching
            return anchorPose()
        }
        val watching = Ring.isWatching || Ring.inSphere
        val screenChanged = anchoredStage != Ring.stage || anchoredWatching != watching
        val away = !Ring.inSphere && !Ring.showsCards && abs(wrapDegrees(viewerYaw - targetYaw)) > 62f
        if (away) {
            if (lookingAwaySinceNs == 0L) lookingAwaySinceNs = now
        } else {
            lookingAwaySinceNs = 0L
        }
        val lingeredAway = lookingAwaySinceNs != 0L && now - lookingAwaySinceNs > 900_000_000L
        if (screenChanged || lingeredAway) {
            targetPos = viewer.t
            targetYaw = viewerYaw
            anchoredStage = Ring.stage
            anchoredWatching = watching
            lookingAwaySinceNs = 0L
            if (screenChanged) {
                anchorPos = viewer.t
                anchorYaw = viewerYaw
            }
        }
        val follow = 1f - exp(-5f * dt)
        anchorYaw += wrapDegrees(targetYaw - anchorYaw) * follow
        anchorPos = pos + (targetPos - pos) * follow
        return anchorPose()
    }

    private fun anchorPose(): Pose =
        Pose(anchorPos ?: Vector3(0f, 0f, 0f), Quaternion(0f, anchorYaw, 0f))

    private fun yawOf(pose: Pose): Float {
        val forward = pose.forward()
        return Math.toDegrees(atan2(forward.x, forward.z).toDouble()).toFloat()
    }

    private fun wrapDegrees(value: Float): Float {
        var wrapped = value % 360f
        if (wrapped > 180f) wrapped -= 360f
        if (wrapped < -180f) wrapped += 360f
        return wrapped
    }

    private var endPose: Pose? = null

    private fun showSphereTour(head: Pose, viewer: Pose) {
        togglePose = null
        chipPose = null
        listOfNotNull(
            RingWorld.opening,
            RingWorld.hall,
            RingWorld.environment,
            RingWorld.stream,
            RingWorld.pair,
            RingWorld.profile,
            RingWorld.chip,
            *RingWorld.air,
        ).forEach {
            it.setComponent(Visible(false))
        }
        RingWorld.slots.forEach { it?.setComponent(Visible(false)) }
        RingWorld.sphere?.let { sphere ->
            sphere.setComponent(Visible(true))
            sphere.setComponent(Transform(Pose(viewer.t, head.q)))
        }
        RingWorld.tourEnd?.let { end ->
            if (!Ring.tourEnded) {
                endPose = null
                end.setComponent(Visible(false))
                return@let
            }
            val pose = endPose ?: placeInFront(viewer, 0f, 1.5f, -0.1f).also { endPose = it }
            end.setComponent(Visible(true))
            end.setComponent(Transform(pose))
        }
    }

    private fun hideSphereTour() {
        endPose = null
        RingWorld.sphere?.setComponent(Visible(false))
        RingWorld.tourEnd?.setComponent(Visible(false))
    }

    private var passthroughOn: Boolean? = null
    private var lastLit = -1f

    private fun applyPassthrough(enabled: Boolean) {
        if (passthroughOn == enabled) return
        val scene = getScene()
        runCatching {
            scene.enablePassthrough(enabled)
            scene.enableHolePunching(enabled)
            // An opaque backfill covers the cameras even after passthrough is enabled.
            scene.setBackfillColor(
                if (enabled) Color4(0f, 0f, 0f, 0f) else Color4(0.04f, 0.025f, 0.015f, 1f),
            )
        }.onSuccess { passthroughOn = enabled }
    }

    private fun lightTheHall() {
        val breath = 0.5f + 0.5f * sin((System.nanoTime() / 1_000_000_000f) * 2.2f)
        val base = if (Ring.stage == Stage.Splash) Ring.flame else 1f
        val lit = (base * (0.86f + 0.14f * breath)).coerceIn(0f, 1f)
        if (abs(lit - lastLit) < 0.02f) return
        lastLit = lit
        getScene().setLightingEnvironment(
            ambientColor = Vector3(0.04f + 0.10f * lit, 0.025f + 0.045f * lit, 0.015f + 0.01f * lit),
            sunColor = Vector3(0.20f + 0.95f * lit, 0.10f + 0.48f * lit, 0.04f + 0.14f * lit),
            sunDirection = -Vector3(0.15f, 1f, -0.35f),
            environmentIntensity = 0.12f + 0.28f * lit,
        )
    }

    private fun placeOpening(head: Pose, show: Boolean) {
        val opening = RingWorld.opening ?: return
        opening.setComponent(Visible(show))
        if (!show) return
        opening.setComponent(Transform(placeInFront(head, 0f, 1.05f, -0.08f)))
        opening.setComponent(Scale(Vector3(1f, 1f, 1f)))
    }

    private fun placeHall(head: Pose, viewer: Pose, show: Boolean) {
        val hall = RingWorld.hall ?: return
        hall.setComponent(Visible(show))
        // A hidden equirect layer still covers the cameras unless it is collapsed.
        hall.setComponent(Scale(if (show) Vector3(1f, 1f, 1f) else Vector3(0f, 0f, 0f)))
        if (!show) return
        hall.setComponent(Transform(Pose(viewer.t, head.q)))
    }

    private var togglePose: Pose? = null

    private fun placeEnvironment(head: Pose, show: Boolean) {
        val environment = RingWorld.environment ?: return
        environment.setComponent(Visible(show))
        if (!show) {
            togglePose = null
            return
        }
        val pose = placeInFront(head, 0f, 1.52f, -0.57f)
        togglePose = pose
        environment.setComponent(Transform(pose))
    }

    private fun placePair(head: Pose, show: Boolean) {
        val pair = RingWorld.pair ?: return
        pair.setComponent(Visible(show))
        if (!show) return
        // Above the eye line, so the phone held out in front stays in view.
        pair.setComponent(Transform(placeInFront(head, 0f, 1.5f, 0.30f)))
    }

    /** Six overlapping walls, so the rain wraps all the way around the member. */
    private fun placeCourtyard(head: Pose, show: Boolean) {
        RingWorld.air.forEachIndexed { index, entity ->
            placeAirAround(entity, head, yawDeg = index * 60f, distance = 2.02f, yOffset = 0.32f, show = show)
        }
    }

    private fun placeAirAround(
        entity: Entity?,
        head: Pose,
        yawDeg: Float,
        distance: Float,
        yOffset: Float,
        show: Boolean,
    ) {
        val air = entity ?: return
        air.setComponent(Visible(show))
        if (!show) {
            air.setComponent(Scale(Vector3(0f, 0f, 0f)))
            return
        }
        val theta = Math.toRadians(yawDeg.toDouble()).toFloat()
        val local = Vector3(sin(theta) * distance, yOffset, cos(theta) * distance)
        air.setComponent(
            Transform(
                Pose(
                    head.t + head.q.times(local),
                    head.q.times(Quaternion(0f, yawDeg, 0f)),
                ),
            ),
        )
        air.setComponent(Scale(Vector3(1f, 1f, 1f)))
    }

    private fun placeProfile(head: Pose, show: Boolean) {
        val profile = RingWorld.profile ?: return
        profile.setComponent(Visible(show))
        if (!show) return
        profile.setComponent(Transform(placeInFront(head, 0f, 1.45f, -0.06f)))
    }

    private var chipPose: Pose? = null

    private fun placeChip(head: Pose, show: Boolean) {
        val chip = RingWorld.chip ?: return
        chip.setComponent(Visible(show))
        if (!show) {
            chipPose = null
            return
        }
        val pose = placeInFront(head, 0f, 1.6f, 0.36f)
        chipPose = pose
        chip.setComponent(Transform(pose))
    }

    private fun aimsAt(target: Pose?, controllerEntity: Entity, halfWidth: Float, halfHeight: Float): Boolean =
        hitDistance(target, controllerEntity, halfWidth, halfHeight) != null

    private fun hitDistance(
        target: Pose?,
        controllerEntity: Entity,
        halfWidth: Float,
        halfHeight: Float,
    ): Float? {
        val panel = target ?: return null
        val aim = controllerEntity.tryGetComponent<Transform>()?.transform ?: return null
        val toLocal = panel.inverse()
        val origin = toLocal.times(aim.t)
        val direction = toLocal.times(aim.t + aim.forward()) - origin
        if (abs(direction.z) < 1e-4f) return null
        val along = -origin.z / direction.z
        if (along <= 0f) return null
        val hit = origin + direction * along
        if (abs(hit.x) >= halfWidth || abs(hit.y) >= halfHeight) return null
        return along
    }

    private fun orbitAngle(head: Pose, controller: Pose): Float {
        val d = controller.t - head.t
        return Math.toDegrees(atan2(d.x, d.z).toDouble()).toFloat()
    }

    private fun placeOnCircle(head: Pose, yawDeg: Float, distance: Float, yOffset: Float): Pose {
        val theta = Math.toRadians(yawDeg.toDouble()).toFloat()
        val local = Vector3(sin(theta) * distance, yOffset, cos(theta) * distance)
        return Pose(
            head.t + head.q.times(local),
            head.q.times(Quaternion(0f, yawDeg, 0f)),
        )
    }

    private fun placeInFront(head: Pose, delta: Float, distance: Float, yOffset: Float): Pose {
        val theta = delta * 0.46f
        val local = Vector3(sin(theta) * distance, yOffset, cos(theta) * distance)
        // A panel's front faces its local -Z, so it takes the same yaw as the line from the head to it. Adding 180 shows its blank back.
        val yaw = Math.toDegrees(theta.toDouble()).toFloat()
        return Pose(
            head.t + head.q.times(local),
            head.q.times(Quaternion(0f, yaw, 0f)),
        )
    }

    private val slotOfCard = HashMap<Int, Int>()

    private fun assignSlots(count: Int) {
        val wanted = if (count <= RingSlots.COUNT) {
            (0 until count).toSet()
        } else {
            val half = RingSlots.COUNT / 2
            (0 until RingSlots.COUNT).map { wrapIndex(Ring.index + it - half, count) }.toSet()
        }
        val free = ArrayList<Int>(RingSlots.COUNT)
        for (slot in 0 until RingSlots.COUNT) {
            val card = RingSlots.shown[slot]
            if (card !in wanted) {
                free.add(slot)
                if (card >= 0) slotOfCard.remove(card)
            }
        }
        for (card in wanted) {
            if (card in slotOfCard) continue
            if (free.isEmpty()) break
            val slot = free.removeAt(0)
            RingSlots.shown[slot] = card
            slotOfCard[card] = slot
        }
    }

    private fun wrapIndex(value: Int, count: Int): Int {
        val wrapped = value % count
        return if (wrapped < 0) wrapped + count else wrapped
    }

    private fun wrapDelta(delta: Float, count: Float): Float {
        var value = delta % count
        if (value > count / 2f) value -= count
        if (value < -count / 2f) value += count
        return value
    }

    private fun wrapUnit(value: Float, count: Float): Float {
        var wrapped = value % count
        if (wrapped < 0f) wrapped += count
        return wrapped
    }
}

object RingWorld {
    val slots: Array<Entity?> = arrayOfNulls(RingSlots.COUNT)
    var stream: Entity? = null
    var opening: Entity? = null
    var hall: Entity? = null
    var environment: Entity? = null
    var sphere: Entity? = null
    var tourEnd: Entity? = null
    var pair: Entity? = null
    val air: Array<Entity?> = arrayOfNulls(6)
    var profile: Entity? = null
    var chip: Entity? = null
}
