package com.hinvr.quest

import com.meta.spatial.core.Color4
import com.meta.spatial.core.Pose
import com.meta.spatial.core.Quaternion
import com.meta.spatial.core.SystemBase
import com.meta.spatial.core.Vector3
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

object RingLayout {
    const val ArcCardLimit = 5
    const val ArcSpacingDeg = 38f

    fun cardYaw(cardIndex: Int, count: Int): Float {
        if (count <= 0) return 0f
        if (count <= ArcCardLimit) {
            val start = -((count - 1) * ArcSpacingDeg) / 2f
            return start + cardIndex * ArcSpacingDeg
        }
        return wrapDegrees(cardIndex * (360f / count))
    }

    fun placeOnCircle(head: Pose, yawDeg: Float, distance: Float, yOffset: Float): Pose {
        val theta = Math.toRadians(yawDeg.toDouble()).toFloat()
        val local = Vector3(sin(theta) * distance, yOffset, cos(theta) * distance)
        return Pose(head.t + head.q.times(local), head.q.times(Quaternion(0f, yawDeg, 0f)))
    }

    /** A panel's front faces its local -Z, so it takes the same yaw as the line from the head to it. */
    fun placeInFront(head: Pose, distance: Float, yOffset: Float): Pose =
        Pose(head.t + head.q.times(Vector3(0f, yOffset, distance)), head.q)

    /** The phone handoff notice, high in view on every screen. */
    fun placeHandoff(head: Pose) {
        val panel = RingWorld.handoff ?: return
        panel.show(Handoff.showingNotice)
        if (Handoff.showingNotice) panel.place(placeInFront(head, 1.5f, 0.42f))
    }
}

/**
 * Where every panel is and whether it shows, plus passthrough, lighting, ambience and the
 * view rig. Components are only written when they change (see [Panel]).
 */
class RingLayoutSystem : SystemBase() {
    private var streamPresence = 0f
    private var lastStage = Stage.Splash
    private var introAge = 0f
    private var passthroughOn: Boolean? = null
    private var lastRecenters = 0
    private var lastMoves = 0
    private var lastCount = -1

    private var anchorPos: Vector3? = null
    private var anchorYaw = 0f
    private var targetPos = Vector3(0f, 0f, 0f)
    private var targetYaw = 0f
    private var anchoredStage: Stage? = null
    private var anchoredWatching = false
    private var lookingAwaySinceNs = 0L
    private var endPose: Pose? = null

    // The last card layout, so a still ring writes nothing.
    private val layoutKey = FloatArray(7) { Float.NaN }
    private var slotsDirty = true

    override fun execute() {
        val dt = Frame.dt
        val now = System.nanoTime()
        val nav = Ring.nav
        if (nav.stage != lastStage) {
            lastStage = nav.stage
            introAge = 0f
            for (slot in RingSlots.shown.indices) RingSlots.shown[slot] = -1
            slotsDirty = true
        }
        val mandir = nav.stage == Stage.Mandir
        val sphere = nav.overlay as? Overlay.Sphere
        val watching = nav.overlay is Overlay.Stream
        val leaving = (nav.overlay as? Overlay.Stream)?.leaving == true
        val splash = nav.stage == Stage.Splash
        val pairing = nav.stage == Stage.Pair

        val approach = 1f - exp(-4.2f * dt)
        if (Ring.showsCards) introAge += dt
        val introT = (1f - exp(-3.4f * introAge)).coerceIn(0f, 1f)
        streamPresence += ((if (watching && !leaving) 1f else 0f) - streamPresence) * approach
        if (leaving && streamPresence < 0.03f) Ring.finishLeave()

        applyPassthrough(sphere == null && !mandir && (splash || pairing || Ring.usePassthrough))
        PairScanner.sync(pairing && QuestAccount.wantsCamera)
        Lighting.tick(mandir, if (splash) Ring.flame else 1f, dt)
        SanctumSound.sync(
            when {
                AudioMix.held || splash || watching || sphere != null -> Ambience.Off
                // In the sanctum the chant comes from the garbha itself (MandirSound).
                mandir -> Ambience.Off
                else -> Ambience.Hall
            },
        )
        if (nav.stage == Stage.States || nav.stage == Stage.Live || mandir) QuestCatalog.refreshIfStale()

        val darshanUp = mandir && Mandir.darshanClose && Mandir.phase != MandirPhase.Sankalp && Mandir.darshan?.hasStream == true
        CastTip.tick((dt * 1000f).toLong(), (watching && !leaving) || darshanUp)
        val viewerRaw = getScene().getViewerPose()
        ViewRig.tick(viewerRaw, dt)
        placeFade(viewerRaw)
        if (Ring.recenters != lastRecenters) {
            lastRecenters = Ring.recenters
            anchorPos = null
            if (mandir) MandirScene.recentre()
        }
        // The member was just moved; panels that follow the head start again from where they are now.
        if (ViewRig.moves != lastMoves) {
            lastMoves = ViewRig.moves
            anchorPos = null
        }
        val viewer = viewerRaw.removePitchAndRoll()
        if (viewer.t.y < 0.5f) return
        val head = anchorFor(viewer, now, dt)
        RingLayout.placeHandoff(head)

        if (mandir) {
            hideRing()
            hideSphere()
            MandirScene.watchLoad()
            MandirLayout.place(head)
            return
        }
        MandirLayout.hide()
        if (sphere != null) {
            hideRing()
            RingWorld.castTip?.show(false)
            showSphere(head, viewer, sphere)
            return
        }
        hideSphere()
        placeOpening(head, splash)
        placeHall(head, viewer, show = !splash && !pairing && !Ring.usePassthrough)
        placeChrome(head, nav)
        placeAir(head, show = splash || pairing)
        RingWorld.stream?.let { stream ->
            val showing = streamPresence > 0.02f
            stream.show(showing)
            if (showing) {
                stream.place(RingLayout.placeInFront(head, 1.7f, -0.02f))
                val full = if (Ring.stream?.spherical == true) 1.42f else 1f
                val scale = 0.68f + (full - 0.68f) * streamPresence
                stream.setScale(scale)
                // Under the stream's bottom edge.
                placeCastTip(RingLayout.placeInFront(head, 1.62f, -0.02f - 0.54f * scale - 0.15f))
            } else {
                RingWorld.castTip?.show(false)
            }
        }
        placeRing(head, viewer, 1f - streamPresence, introT)
    }

    private fun placeCastTip(pose: Pose) {
        val panel = RingWorld.castTip ?: return
        panel.show(CastTip.showing)
        if (CastTip.showing) panel.place(pose)
    }

    private fun placeChrome(head: Pose, nav: NavState) {
        val menu = nav.stage == Stage.Menu && nav.overlay == Overlay.None
        RingWorld.environment?.let { panel ->
            panel.show(menu)
            if (menu) panel.place(RingLayout.placeInFront(head, 1.52f, -0.57f))
        }
        val statusShown = (nav.stage == Stage.States || nav.stage == Stage.Live) && nav.overlay == Overlay.None
        RingWorld.status?.let { panel ->
            panel.show(statusShown)
            if (statusShown) panel.place(RingLayout.placeInFront(head, 1.52f, -0.57f))
        }
        RingWorld.pair?.let { panel ->
            val show = nav.stage == Stage.Pair
            panel.show(show)
            // Above the eye line, so the phone held out in front stays in view.
            if (show) panel.place(RingLayout.placeInFront(head, 1.5f, 0.30f))
        }
        RingWorld.profile?.let { panel ->
            val show = nav.stage == Stage.Profile
            panel.show(show)
            if (show) panel.place(RingLayout.placeInFront(head, 1.45f, -0.06f))
        }
        RingWorld.credits?.let { panel ->
            val show = nav.stage == Stage.Credits
            panel.show(show)
            if (show) panel.place(RingLayout.placeInFront(head, 1.45f, -0.04f))
        }
        RingWorld.chip?.let { panel ->
            val show = menu && QuestAccount.isPaired
            panel.show(show)
            if (show) panel.place(RingLayout.placeInFront(head, 1.6f, 0.36f))
        }
    }

    private fun placeRing(head: Pose, viewer: Pose, ringPresence: Float, introT: Float) {
        val count = Ring.deckSize
        val cards = Ring.showsCards && count > 0
        if (!cards) {
            RingWorld.cardPoses.clear()
            RingWorld.slots.forEach { it?.show(false) }
            layoutKey[0] = Float.NaN
            return
        }
        if (count != lastCount) {
            lastCount = count
            slotsDirty = true
        }
        if (count > RingSlots.COUNT || slotsDirty) {
            val facing = wrapDegrees(yawOf(viewer) - yawOf(head))
            if (RingWindow.assign(RingSlots.shown, RingWindow.wanted(count, RingSlots.COUNT, facing))) slotsDirty = true
        }
        val moved = keyChanged(0, head.t.x) or keyChanged(1, head.t.z) or keyChanged(2, anchorYaw) or
            keyChanged(3, ringPresence) or keyChanged(4, introT) or keyChanged(5, Ring.index.toFloat()) or
            keyChanged(6, streamPresence)
        if (!slotsDirty && !moved) return
        slotsDirty = false
        RingWorld.cardPoses.clear()
        val radius = 1.78f + streamPresence * 0.35f
        RingWorld.slots.forEachIndexed { slot, panel ->
            if (panel == null) return@forEachIndexed
            val cardIndex = RingSlots.shown[slot]
            if (cardIndex !in 0 until count) {
                panel.show(false)
                return@forEachIndexed
            }
            val pose = RingLayout.placeOnCircle(head, RingLayout.cardYaw(cardIndex, count), radius, -0.12f)
            RingWorld.cardPoses[cardIndex] = pose
            val focused = cardIndex == Ring.index
            panel.show(ringPresence > 0.04f)
            panel.place(pose)
            panel.setScale((if (focused) 1.10f else 0.90f) * ringPresence * (0.72f + 0.28f * introT))
        }
    }

    private fun keyChanged(slot: Int, value: Float): Boolean {
        if (abs(layoutKey[slot] - value) < 1e-4f) return false
        layoutKey[slot] = value
        return true
    }

    private fun anchorFor(viewer: Pose, now: Long, dt: Float): Pose {
        val viewerYaw = yawOf(viewer)
        val pos = anchorPos
        val watching = Ring.nav.overlay != Overlay.None
        if (pos == null) {
            anchorPos = viewer.t
            anchorYaw = viewerYaw
            targetPos = viewer.t
            targetYaw = viewerYaw
            anchoredStage = Ring.stage
            anchoredWatching = watching
            return anchorPose()
        }
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
        val dYaw = wrapDegrees(targetYaw - anchorYaw)
        val current = anchorPos ?: viewer.t
        if (abs(dYaw) < 0.01f && abs(targetPos.x - current.x) < 1e-4f && abs(targetPos.z - current.z) < 1e-4f &&
            abs(targetPos.y - current.y) < 1e-4f
        ) {
            return anchorPose()
        }
        anchorYaw += dYaw * follow
        anchorPos = current + (targetPos - current) * follow
        return anchorPose()
    }

    private var cachedAnchor: Pose? = null
    private var cachedAnchorYaw = Float.NaN
    private var cachedAnchorPos: Vector3? = null

    private fun anchorPose(): Pose {
        val pos = anchorPos ?: Vector3(0f, 0f, 0f)
        val cached = cachedAnchor
        if (cached != null && cachedAnchorYaw == anchorYaw && cachedAnchorPos === pos) return cached
        return Pose(pos, Quaternion(0f, anchorYaw, 0f)).also {
            cachedAnchor = it
            cachedAnchorYaw = anchorYaw
            cachedAnchorPos = pos
        }
    }

    private fun showSphere(head: Pose, viewer: Pose, sphere: Overlay.Sphere) {
        RingWorld.sphere?.let { panel ->
            panel.show(true)
            panel.place(Pose(viewer.t, head.q))
        }
        RingWorld.tourEnd?.let { end ->
            val over = sphere.status == SphereStatus.Ended || sphere.status == SphereStatus.Failed
            if (!over) {
                endPose = null
                end.show(false)
                return@let
            }
            val pose = endPose ?: RingLayout.placeInFront(viewer, 1.5f, -0.1f).also { endPose = it }
            end.show(true)
            end.place(pose)
        }
    }

    private fun hideSphere() {
        endPose = null
        RingWorld.sphere?.show(false)
        RingWorld.tourEnd?.show(false)
    }

    private fun hideRing() {
        RingWorld.ringPanels.forEach { it.show(false) }
        RingWorld.cardPoses.clear()
        layoutKey[0] = Float.NaN
    }

    private fun placeFade(viewer: Pose) {
        val panel = RingWorld.fade ?: return
        val show = ViewRig.fade > 0.003f
        panel.show(show)
        if (show) panel.place(Pose(viewer.t + viewer.q.times(Vector3(0f, 0f, 0.3f)), viewer.q))
    }

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

    private fun placeOpening(head: Pose, show: Boolean) {
        val opening = RingWorld.opening ?: return
        opening.show(show)
        if (!show) return
        opening.place(RingLayout.placeInFront(head, 1.05f, -0.08f))
        Ring.markSplashShown()
    }

    private fun placeHall(head: Pose, viewer: Pose, show: Boolean) {
        val hall = RingWorld.hall ?: return
        hall.show(show)
        if (show) hall.place(Pose(viewer.t, head.q))
    }

    /** Six overlapping walls, so the petal rain wraps all the way around the member. */
    private fun placeAir(head: Pose, show: Boolean) {
        RingWorld.air.forEachIndexed { index, panel ->
            if (panel == null) return@forEachIndexed
            panel.show(show)
            if (!show) return@forEachIndexed
            val yawDeg = index * 60f
            panel.place(RingLayout.placeOnCircle(head, yawDeg, 2.02f, 0.32f))
        }
    }
}
