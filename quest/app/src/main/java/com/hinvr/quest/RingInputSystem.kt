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
    private var streamPresence = 0f
    private var lastFrameNs = 0L
    private var lastStage = Stage.Splash
    private var introAge = 0f
    private var bodyYaw = 0f
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
        var aimed = -1
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
                Ring.stage == Stage.Menu && !Ring.isWatching && !Mandir.inside &&
                (controller.isPressed(ButtonBits.ButtonA) || controller.isPressed(ButtonBits.ButtonX))
            ) {
                Ring.setPassthrough(!Ring.usePassthrough)
            }
            if (
                Ring.stage == Stage.Mandir &&
                (controller.isPressed(ButtonBits.ButtonA) || controller.isPressed(ButtonBits.ButtonX))
            ) {
                MandirScene.nextTeleport()
            }
            if (
                controller.isPressed(ButtonBits.ButtonB) ||
                controller.isPressed(ButtonBits.ButtonY)
            ) {
                Ring.back()
            }
        }
        if (Ring.isWatching || Ring.inSphere) return
        if (Ring.stage == Stage.Mandir) return
        if (Ring.stage == Stage.Splash) {
            if (splashGreeting != Ring.greeting) {
                splashGreeting = Ring.greeting
                splashUntilMs = 0L
            }
            if (!Ring.splashShown) return
            if (splashUntilMs == 0L) splashUntilMs = now + OpeningLengthMs
            if (now >= splashUntilMs) Ring.openMenu()
            return
        }
        if (!Ring.showsCards) return
        val count = Ring.deckSize
        if (scroll != 0 && count > 0) {
            val delta = scroll.coerceIn(-1, 1)
            Ring.scroll(delta)
            if (usesFullRing(count)) {
                bodyYaw = wrapDegrees(bodyYaw + delta * (360f / count))
                getScene().setViewOrigin(0f, 0f, 0f, bodyYaw)
            }
            nextScrollAtMs = now + 420L
        }
        if (aimed >= 0) {
            Ring.lookAt(aimed)
            if (select) Ring.focus(aimed)
        } else if (select) {
            Ring.selectCentered()
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
            val fromMandir = lastStage == Stage.Mandir
            lastStage = Ring.stage
            introAge = 0f
            slotOfCard.clear()
            for (slot in RingSlots.shown.indices) RingSlots.shown[slot] = -1
            if (fromMandir || Ring.stage == Stage.Mandir) bodyYaw = 0f
        }
        val count = Ring.deckSize
        val approach = 1f - exp(-4.2f * dt)
        if (Ring.showsCards) introAge += dt
        val introT = (1f - exp(-3.4f * introAge)).coerceIn(0f, 1f)
        val streamTarget = if (Ring.isWatching && !Ring.leaving) 1f else 0f
        streamPresence += (streamTarget - streamPresence) * approach
        if (Ring.leaving && streamPresence < 0.03f) Ring.finishLeave()

        val ringPresence = 1f - streamPresence
        val splash = Ring.stage == Stage.Splash
        val pairing = Ring.stage == Stage.Pair
        val sphere = Ring.inSphere
        val mandir = Ring.stage == Stage.Mandir
        val passthrough = !sphere && !mandir && (splash || pairing || Ring.usePassthrough)
        applyPassthrough(passthrough)
        PairScanner.sync(pairing && QuestAccount.wantsCamera)
        if (!mandir) lightTheHall()
        SanctumSound.onFrame(Ring.stage, Ring.isWatching || sphere || mandir, Ring.diyaLit)
        val viewer = getScene().getViewerPose().removePitchAndRoll()
        if (viewer.t.y < 0.5f) return
        val head = anchorFor(viewer, now, dt)
        if (mandir) {
            showMandir(head)
            return
        }
        hideMandir()
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
        placeRing(head, viewer, count, ringPresence, introT)
    }

    private fun placeRing(head: Pose, viewer: Pose, count: Int, ringPresence: Float, introT: Float) {
        cardPoseByIndex.clear()
        if (count > 0) assignSlots(count, head, viewer)
        val radius = 1.78f + streamPresence * 0.35f
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
            val yaw = cardYaw(cardIndex, count)
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
            MandirWorld.darshan,
            MandirWorld.sankalp,
            MandirWorld.hint,
            MandirWorld.blessing,
            MandirWorld.courtyard,
            MandirWorld.flame,
            MandirWorld.smoke,
            MandirWorld.tilak,
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

    private fun showMandir(head: Pose) {
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
            RingWorld.sphere,
            RingWorld.tourEnd,
            *RingWorld.air,
        ).forEach { it.setComponent(Visible(false)) }
        RingWorld.slots.forEach { it?.setComponent(Visible(false)) }

        // This equirect covers the cameras. Leaving it up hides the sanctum and the offerings.
        MandirWorld.courtyard?.let { sky ->
            sky.setComponent(Visible(false))
            sky.setComponent(Scale(Vector3(0f, 0f, 0f)))
        }
        MandirWorld.darshan?.let { panel ->
            val show = !Mandir.sankalpOpen
            panel.setComponent(Visible(show))
            if (show) {
                panel.setComponent(
                    Transform(
                        Pose(
                            MandirPlace.darshan,
                            Quaternion(0f, 180f, 0f),
                        ),
                    ),
                )
            }
        }
        MandirWorld.sankalp?.let { panel ->
            val show = Mandir.sankalpOpen
            panel.setComponent(Visible(show))
            if (show) panel.setComponent(Transform(placeInFront(head, 0f, 1.35f, -0.04f)))
        }
        MandirWorld.hint?.let { panel ->
            val show = !Mandir.sankalpOpen && !Mandir.blessing
            panel.setComponent(Visible(show))
            if (show) {
                panel.setComponent(
                    Transform(
                        Pose(
                            MandirPlace.stand + Vector3(0f, 0.48f, -0.42f),
                            Quaternion(0f, 180f, 0f),
                        ),
                    ),
                )
            }
        }
        MandirWorld.blessing?.let { panel ->
            val show = Mandir.blessing
            panel.setComponent(Visible(show))
            if (show) panel.setComponent(Transform(placeInFront(head, 0f, 1.4f, -0.06f)))
        }
        MandirWorld.flame?.let { panel ->
            val show = Mandir.diyaLit
            panel.setComponent(Visible(show))
            if (show) {
                panel.setComponent(Transform(MandirScene.diyaPose))
                panel.setComponent(Scale(Vector3(1f, 1f, 1f)))
            }
        }
        MandirWorld.smoke?.let { panel ->
            val show = Mandir.agarbattiLit
            panel.setComponent(Visible(show))
            if (show) {
                panel.setComponent(Transform(MandirScene.incensePose))
                panel.setComponent(Scale(Vector3(1f, 1f, 1f)))
            }
        }
        MandirWorld.tilak?.let { panel ->
            val show = Mandir.tilakOn
            panel.setComponent(Visible(show))
            if (show) panel.setComponent(Transform(placeInFront(head, 0f, 0.55f, 0.12f)))
        }
    }

    private fun hideMandir() {
        listOfNotNull(
            MandirWorld.darshan,
            MandirWorld.sankalp,
            MandirWorld.hint,
            MandirWorld.blessing,
            MandirWorld.courtyard,
            MandirWorld.flame,
            MandirWorld.smoke,
            MandirWorld.tilak,
        ).forEach { it.setComponent(Visible(false)) }
        MandirWorld.courtyard?.setComponent(Scale(Vector3(0f, 0f, 0f)))
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
        Ring.markSplashShown()
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

    private fun assignSlots(count: Int, head: Pose, viewer: Pose) {
        val wanted = if (count <= RingSlots.COUNT) {
            (0 until count).toSet()
        } else {
            val facing = wrapDegrees(yawOf(viewer) - yawOf(head))
            val spacing = 360f / count
            val nearest = kotlin.math.round(facing / spacing).toInt()
            val half = RingSlots.COUNT / 2
            (0 until RingSlots.COUNT).map { wrapIndex(nearest + it - half, count) }.toSet()
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

    private fun usesFullRing(count: Int): Boolean = count > ArcCardLimit

    private fun cardYaw(cardIndex: Int, count: Int): Float {
        if (count <= 0) return 0f
        if (!usesFullRing(count)) {
            val start = -((count - 1) * ArcSpacingDeg) / 2f
            return start + cardIndex * ArcSpacingDeg
        }
        return wrapDegrees(cardIndex * (360f / count))
    }

    companion object {
        private const val ArcCardLimit = 5
        private const val ArcSpacingDeg = 38f
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
