package com.hinvr.quest

import android.os.SystemClock
import com.meta.spatial.core.Entity
import com.meta.spatial.core.Pose
import com.meta.spatial.core.SystemBase
import com.meta.spatial.runtime.ButtonBits
import com.meta.spatial.toolkit.Controller
import com.meta.spatial.toolkit.Transform
import kotlin.math.abs

/** How long the greeting plays, and when a trigger may cut it short. */
const val OpeningLengthMs = 2_500L
const val OpeningSkippableAfterMs = 800L

/**
 * Buttons, thumbsticks and the controller ray. Card clicks themselves arrive through the
 * panels (controller trigger or hand pinch), so the ray here only aims; it never selects.
 */
class RingInputSystem : SystemBase() {
    private var nextScrollAtMs = 0L

    override fun execute() {
        Frame.begin()
        // "View in VR" from the phone: asks the server every few seconds while the app runs.
        Handoff.tick()
        val now = SystemClock.elapsedRealtime()
        var scroll = 0
        var trigger = false
        var aimed = -1
        for (controllerEntity in Frame.controllers()) {
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
            if (Ring.showsCards) {
                val pointed = aimedCard(controllerEntity)
                if (pointed >= 0) aimed = pointed
            }
            if (controller.isPressed(ButtonBits.ButtonTriggerR) || controller.isPressed(ButtonBits.ButtonTriggerL)) {
                trigger = true
            }
            val primary = controller.isPressed(ButtonBits.ButtonA) || controller.isPressed(ButtonBits.ButtonX)
            if (primary && Ring.stage == Stage.Menu && Ring.nav.overlay == Overlay.None) {
                Ring.setPassthrough(!Ring.usePassthrough)
            }
            if (primary && Ring.stage == Stage.Mandir && Mandir.phase == MandirPhase.Visit) {
                MandirScene.nextTeleport()
            }
            // The thumbstick turns the member in the sanctum, a fixed step at a time.
            if (Ring.stage == Stage.Mandir && (left || right)) {
                MandirScene.snapTurn(if (right) -Comfort.SnapDegrees else Comfort.SnapDegrees)
            }
            // B / Y on controllers; the left-hand menu pinch arrives as the menu button.
            if (controller.isPressed(ButtonBits.ButtonB) ||
                controller.isPressed(ButtonBits.ButtonY) ||
                controller.isPressed(ButtonBits.ButtonMenu)
            ) {
                Ring.back()
            }
        }
        if (Ring.stage == Stage.Splash) {
            tickSplash(now, trigger)
            return
        }
        if (!Ring.showsCards || Ring.nav.overlay != Overlay.None) return
        val count = Ring.deckSize
        if (scroll != 0 && count > 0) {
            val delta = scroll.coerceIn(-1, 1)
            Ring.scroll(delta)
            if (count > RingLayout.ArcCardLimit) {
                ViewRig.turnRoom(wrapDegrees(ViewRig.roomYaw + delta * (360f / count)))
            }
            nextScrollAtMs = now + 420L
        }
        if (aimed >= 0) Ring.lookAt(aimed)
    }

    /** One clock ends the greeting. A trigger skips it once it has had a moment. */
    private fun tickSplash(now: Long, trigger: Boolean) {
        val shownAt = Ring.splashShownAtMs
        if (shownAt == 0L) return
        val age = now - shownAt
        if (age >= OpeningLengthMs || (trigger && age >= OpeningSkippableAfterMs)) Ring.endSplash()
    }

    private fun aimedCard(controllerEntity: Entity): Int {
        var best = -1
        var bestScore = Float.MAX_VALUE
        val aim = controllerEntity.tryGetComponent<Transform>()?.transform ?: return -1
        val origin = aim.t
        val dir = aim.forward()
        for ((index, pose) in RingWorld.cardPoses) {
            val along = hitDistance(pose, aim, 0.32f, 0.44f)
            if (along != null && along < bestScore) {
                bestScore = along
                best = index
                continue
            }
            val dx = pose.t.x - origin.x
            val dy = pose.t.y - origin.y
            val dz = pose.t.z - origin.z
            val depth = dx * dir.x + dy * dir.y + dz * dir.z
            if (depth < 0.25f) continue
            val px = origin.x + dir.x * depth - pose.t.x
            val py = origin.y + dir.y * depth - pose.t.y
            val pz = origin.z + dir.z * depth - pose.t.z
            val dist = kotlin.math.sqrt(px * px + py * py + pz * pz)
            val score = 8f + dist
            if (dist < 0.45f && score < bestScore) {
                bestScore = score
                best = index
            }
        }
        return best
    }

    private fun hitDistance(panel: Pose, aim: Pose, halfWidth: Float, halfHeight: Float): Float? {
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
}
