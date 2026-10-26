package com.hinvr.quest

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import com.meta.spatial.compose.ComposeFeature
import com.meta.spatial.compose.ComposeViewPanelRegistration
import com.meta.spatial.core.Color4
import com.meta.spatial.core.SpatialFeature
import com.meta.spatial.runtime.ReferenceSpace
import com.meta.spatial.runtime.SessionState
import com.meta.spatial.runtime.StereoMode
import com.meta.spatial.toolkit.AppSystemActivity
import com.meta.spatial.toolkit.DpPerMeterDisplayOptions
import com.meta.spatial.toolkit.Equirect360ShapeOptions
import com.meta.spatial.toolkit.MediaPanelRenderOptions
import com.meta.spatial.toolkit.MediaPanelSettings
import com.meta.spatial.toolkit.PanelRegistration
import com.meta.spatial.toolkit.PanelStyleOptions
import com.meta.spatial.toolkit.PixelDisplayOptions
import com.meta.spatial.toolkit.QuadShapeOptions
import com.meta.spatial.toolkit.UIPanelSettings
import com.meta.spatial.toolkit.VideoSurfacePanelRegistration
import com.meta.spatial.vr.VRFeature

class QuestActivity : AppSystemActivity() {
    private val main = Handler(Looper.getMainLooper())
    private var pausedAtMs = 0L
    private var resumed = false
    private var focused = true

    override fun registerFeatures(): List<SpatialFeature> {
        return listOf(
            VRFeature(this),
            ComposeFeature(),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AudioMix.attach(this)
        QuestAccount.attach(this)
        MandirPlace.load(this)
        PhotoCredits.load(this)
        QuestCatalog.refresh()
        PairScanner.attach(this)
        SanctumSound.attach(this)
        Tour360.attach(this)
        SpherePlayer.attach(this)
        HallSphere.attach(this)
        Haptics.sink = { hand, amplitude, durationMs ->
            spatial.applyHapticFeedback(hand, amplitude, durationMs * 1_000_000L, 200f)
        }
        systemManager.registerSystem(RingInputSystem())
        systemManager.registerSystem(RingLayoutSystem())
        systemManager.registerSystem(OfferingSystem())
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(OpenMandirExtra, false)) {
            // Members only; whatever was playing stops first (Ring.go handles both).
            Ring.openMandir(explore = true)
        } else {
            Ring.greetAgain()
        }
    }

    override fun onPause() {
        resumed = false
        pausedAtMs = SystemClock.elapsedRealtime()
        PairScanner.onPause()
        QuestAccount.onPause()
        holdMedia()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        val fromYoutube = Ring.consumeYoutubeHandoff()
        // The greeting replays after a real break, not after a glance at the system menu.
        if (!fromYoutube && pausedAtMs != 0L && SystemClock.elapsedRealtime() - pausedAtMs > GreetAgainAfterMs) {
            Ring.greetAgain()
        }
        pausedAtMs = 0L
        PairScanner.onResume()
        QuestAccount.onResume()
        releaseMedia()
    }

    /** Headset off or the system menu over the app: the OpenXR session loses focus. */
    override fun onSessionStateChanged(state: SessionState) {
        super.onSessionStateChanged(state)
        main.post {
            focused = state == SessionState.FOCUSED
            if (!focused && (state == SessionState.VISIBLE || state == SessionState.SYNCHRONIZED ||
                    state == SessionState.STOPPING)
            ) {
                holdMedia()
            } else if (focused) {
                releaseMedia()
            }
        }
    }

    override fun onVRPause() {
        super.onVRPause()
        main.post { holdMedia() }
    }

    override fun onRecenter(isUserInitiated: Boolean) {
        super.onRecenter(isUserInitiated)
        main.post { Ring.recentered() }
    }

    private fun holdMedia() {
        AudioMix.hold(true)
        SpherePlayer.pauseForApp()
        WebPlayers.pauseAll()
    }

    private fun releaseMedia() {
        if (!resumed || !focused) return
        AudioMix.hold(false)
        SpherePlayer.resumeForApp()
        WebPlayers.resumeAll()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PairScanner.PermissionRequest) PairScanner.onPermissionResult()
    }

    override fun onDestroy() {
        Haptics.sink = null
        PairScanner.detach()
        SanctumSound.release()
        SpherePlayer.stop()
        MandirSound.stopVisit()
        MandirSound.release()
        super.onDestroy()
    }

    override fun onSceneReady() {
        super.onSceneReady()
        scene.setReferenceSpace(ReferenceSpace.LOCAL_FLOOR)
        ViewRig.attach(scene)
        Lighting.attach(scene)
        scene.enablePassthrough(true)
        scene.enableHolePunching(true)
        scene.setBackfillColor(Color4(0f, 0f, 0f, 0f))
        slotIds().forEachIndexed { index, panelId ->
            RingWorld.slots[index] = Panel.create(panelId)
        }
        RingWorld.stream = Panel.create(R.id.ring_stream)
        RingWorld.opening = Panel.create(R.id.ring_opening, hittable = false)
        // The 360 spheres surround the viewer, so they must not swallow the controller ray.
        RingWorld.hall = Panel.create(R.id.ring_hall, hittable = false, collapse = true)
        RingWorld.environment = Panel.create(R.id.ring_environment)
        RingWorld.status = Panel.create(R.id.ring_status, hittable = false)
        RingWorld.tourEnd = Panel.create(R.id.ring_tour_end)
        RingWorld.pair = Panel.create(R.id.ring_pair)
        airIds().forEachIndexed { index, panelId ->
            RingWorld.air[index] = Panel.create(panelId, hittable = false, collapse = true)
        }
        RingWorld.profile = Panel.create(R.id.ring_profile)
        RingWorld.chip = Panel.create(R.id.ring_profile_chip)
        RingWorld.credits = Panel.create(R.id.ring_credits)
        RingWorld.fade = Panel.create(R.id.ring_fade, hittable = false)
        MandirSound.attach(this, scene)
        MandirScene.attach(scene, systemManager)
        MandirWorld.darshan = Panel.create(R.id.ring_darshan)
        MandirWorld.sankalp = Panel.create(R.id.ring_sankalp)
        MandirWorld.guide = Panel.create(R.id.ring_guide)
        MandirWorld.blessing = Panel.create(R.id.ring_blessing)
        MandirWorld.flame = Panel.create(R.id.ring_flame, hittable = false)
        MandirWorld.smoke = Panel.create(R.id.ring_smoke, hittable = false)
        MandirWorld.tilak = Panel.create(R.id.ring_tilak, hittable = false)
        if (intent?.getBooleanExtra(OpenMandirExtra, false) == true) {
            Ring.openMandir(explore = true)
        }
    }

    companion object {
        const val OpenMandirExtra = "open_mandir"
        private const val GreetAgainAfterMs = 10 * 60_000L
    }

    override fun registerPanels(): List<PanelRegistration> {
        val cards = slotIds().mapIndexed { index, panelId ->
            composePanel(panelId, width = 0.50f, height = 0.74f) { SlotCard(index) }
        }
        val hall = VideoSurfacePanelRegistration(
            R.id.ring_hall,
            { _, surface -> HallSphere.drawOn(surface) },
            {
                MediaPanelSettings(
                    shape = Equirect360ShapeOptions(radius = 40f),
                    display = PixelDisplayOptions(width = 1280, height = 640),
                    rendering = MediaPanelRenderOptions(stereoMode = StereoMode.None, zIndex = -10),
                )
            },
        )
        // Registered up front but only created (with its 7680×3840 surface) on the first tour.
        val sphere = VideoSurfacePanelRegistration(
            R.id.ring_sphere,
            { _, surface -> SpherePlayer.attachSurface(surface) },
            {
                MediaPanelSettings(
                    shape = Equirect360ShapeOptions(radius = 40f),
                    display = PixelDisplayOptions(width = 7680, height = 3840),
                    rendering = MediaPanelRenderOptions(stereoMode = StereoMode.None),
                )
            },
        )
        val air = airIds().mapIndexed { index, panelId ->
            composePanel(panelId, width = 2.50f, height = 2.20f) { SanctumAir(index) }
        }
        return cards + air + listOf(
            hall,
            sphere,
            composePanel(R.id.ring_stream, width = 1.72f, height = 1.08f) { StreamPanel() },
            composePanel(R.id.ring_opening, width = 1.20f, height = 1.30f) { SanctumOpening() },
            composePanel(R.id.ring_environment, width = 0.78f, height = 0.11f) { EnvironmentToggle() },
            composePanel(R.id.ring_status, width = 0.90f, height = 0.10f) { CatalogStatusPanel() },
            composePanel(R.id.ring_tour_end, width = 1.10f, height = 0.62f) { TourEndPanel() },
            composePanel(R.id.ring_pair, width = 1.05f, height = 0.70f) { PairPanel() },
            composePanel(R.id.ring_profile, width = 1.20f, height = 0.86f) { ProfilePanel() },
            composePanel(R.id.ring_profile_chip, width = 0.56f, height = 0.11f) { ProfileChip() },
            composePanel(R.id.ring_credits, width = 1.20f, height = 0.92f) { CreditsPanel() },
            composePanel(R.id.ring_fade, width = 1.60f, height = 1.60f) { FadePanel() },
            composePanel(R.id.ring_darshan, width = 0.95f, height = 1.18f) { DarshanPanel() },
            composePanel(R.id.ring_sankalp, width = 1.12f, height = 0.78f) { SankalpPanel() },
            composePanel(R.id.ring_guide, width = 1.10f, height = 0.34f) { GuidePanel() },
            composePanel(R.id.ring_blessing, width = 1.08f, height = 0.58f) { BlessingPanel() },
            composePanel(R.id.ring_flame, width = 0.14f, height = 0.18f) { FlamePanel() },
            composePanel(R.id.ring_smoke, width = 0.22f, height = 0.36f) { SmokePanel() },
            composePanel(R.id.ring_tilak, width = 0.60f, height = 0.40f) { TilakOverlay() },
        )
    }

    private fun slotIds() = intArrayOf(
        R.id.ring_slot_0,
        R.id.ring_slot_1,
        R.id.ring_slot_2,
        R.id.ring_slot_3,
        R.id.ring_slot_4,
        R.id.ring_slot_5,
        R.id.ring_slot_6,
        R.id.ring_slot_7,
        R.id.ring_slot_8,
        R.id.ring_slot_9,
        R.id.ring_slot_10,
        R.id.ring_slot_11,
        R.id.ring_slot_12,
        R.id.ring_slot_13,
        R.id.ring_slot_14,
        R.id.ring_slot_15,
    )

    private fun airIds() = intArrayOf(
        R.id.ring_air_0,
        R.id.ring_air_1,
        R.id.ring_air_2,
        R.id.ring_air_3,
        R.id.ring_air_4,
        R.id.ring_air_5,
    )

    private fun composePanel(
        panelId: Int,
        width: Float,
        height: Float,
        content: @Composable () -> Unit,
    ): PanelRegistration =
        ComposeViewPanelRegistration(
            panelId,
            composeViewCreator = { _, context ->
                ComposeView(context).apply { setContent { content() } }
            },
            settingsCreator = {
                UIPanelSettings(
                    shape = QuadShapeOptions(width = width, height = height),
                    style = PanelStyleOptions(themeResourceId = R.style.PanelAppThemeTransparent),
                    display = DpPerMeterDisplayOptions(),
                )
            },
        )
}
