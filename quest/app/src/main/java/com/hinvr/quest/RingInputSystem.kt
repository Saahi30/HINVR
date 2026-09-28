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
import kotlin.math.roundToInt
import kotlin.math.sin

class RingInputSystem : SystemBase() {
    private var nextScrollAtMs = 0L
    private var splashUntilMs = 0L
    private var splashGreeting = 0
    private var displayedCenter = 0f
    private var streamPresence = 0f
    private var lastFrameNs = 0L
    private var lastStage = Stage.Splash

    override fun execute() {
        readControllers()
        layout()
    }

    private fun readControllers() {
        val now = System.currentTimeMillis()
        val controllers = Query.where { has(Controller.id) }.eval().filter { it.isLocal() }
        var scroll = 0
        var select = false
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
            if (
                controller.isPressed(ButtonBits.ButtonTriggerR) ||
                controller.isPressed(ButtonBits.ButtonTriggerL)
            ) {
                // The toggle handles its own click; don't also open the centered card.
                if (!aimsAtToggle(controllerEntity)) select = true
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
        if (scroll != 0) {
            Ring.scroll(scroll.coerceIn(-1, 1))
            nextScrollAtMs = now + 420L
        }
        if (select) Ring.selectCentered()
    }

    private fun layout() {
        val now = System.nanoTime()
        val dt = if (lastFrameNs == 0L) 0.016f else ((now - lastFrameNs) / 1_000_000_000f).coerceIn(0f, 0.05f)
        lastFrameNs = now
        if (Ring.stage != lastStage) {
            lastStage = Ring.stage
            displayedCenter = Ring.index.toFloat()
            slotOfCard.clear()
            for (slot in RingSlots.shown.indices) RingSlots.shown[slot] = -1
        }
        val count = Ring.deckSize
        val glide = 1f - exp(-7.5f * dt)
        val approach = 1f - exp(-4.2f * dt)
        displayedCenter = wrapUnit(
            displayedCenter + wrapDelta(Ring.index - displayedCenter, count.toFloat()) * glide,
            count.toFloat(),
        )
        val streamTarget = if (Ring.isWatching && !Ring.leaving) 1f else 0f
        streamPresence += (streamTarget - streamPresence) * approach
        if (Ring.leaving && streamPresence < 0.03f) Ring.finishLeave()

        val ringPresence = 1f - streamPresence
        val splash = Ring.stage == Stage.Splash
        val sphere = Ring.inSphere
        val passthrough = !sphere && (splash || Ring.usePassthrough)
        applyPassthrough(passthrough)
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
        placeHall(head, viewer, show = !splash && !Ring.usePassthrough)
        placeEnvironment(head, show = Ring.stage == Stage.Menu && !Ring.isWatching)
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
        assignSlots(count)
        RingWorld.slots.forEachIndexed { slot, entity ->
            if (entity == null) return@forEachIndexed
            if (splash) {
                entity.setComponent(Visible(false))
                return@forEachIndexed
            }
            val cardIndex = RingSlots.shown[slot]
            if (cardIndex !in 0 until count) {
                entity.setComponent(Visible(false))
                return@forEachIndexed
            }
            val menu = Ring.stage == Stage.Menu || Ring.stage == Stage.Tour
            val delta = when (Ring.stage) {
                Stage.Splash -> 0f
                Stage.Menu -> if (cardIndex == 0) -1.05f else 1.05f
                Stage.Tour -> (cardIndex - 1) * 1.15f
                Stage.Live -> wrapDelta(cardIndex - displayedCenter, count.toFloat())
            }
            val onArc = abs(delta) <= 2.6f && ringPresence > 0.04f
            entity.setComponent(Visible(onArc))
            if (!onArc) return@forEachIndexed
            val distance = 1.85f + streamPresence * 0.45f
            val focused = cardIndex == Ring.index
            val scale = if (menu) {
                (if (focused) 1.08f else 0.92f) * ringPresence
            } else {
                ((1.05f - abs(delta) * 0.12f).coerceAtLeast(0.72f)) * ringPresence
            }
            entity.setComponent(Transform(placeInFront(head, delta, distance, -0.12f)))
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
        val away = !Ring.inSphere && abs(wrapDegrees(viewerYaw - targetYaw)) > 62f
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
        listOfNotNull(
            RingWorld.opening,
            RingWorld.hall,
            RingWorld.environment,
            RingWorld.stream,
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

    private fun aimsAtToggle(controllerEntity: Entity): Boolean {
        val toggle = togglePose ?: return false
        val aim = controllerEntity.tryGetComponent<Transform>()?.transform ?: return false
        val toLocal = toggle.inverse()
        val origin = toLocal.times(aim.t)
        val direction = toLocal.times(aim.t + aim.forward()) - origin
        if (abs(direction.z) < 1e-4f) return false
        val along = -origin.z / direction.z
        if (along <= 0f) return false
        val hit = origin + direction * along
        return abs(hit.x) < 0.28f && abs(hit.y) < 0.09f
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
        val nearest = displayedCenter.roundToInt()
        val wanted = (-2..2).map { wrapIndex(nearest + it, count) }.toSet()
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
}
