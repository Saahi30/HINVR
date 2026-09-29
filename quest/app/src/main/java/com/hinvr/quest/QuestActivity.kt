package com.hinvr.quest

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import com.meta.spatial.compose.ComposeFeature
import com.meta.spatial.core.Color4
import com.meta.spatial.core.Entity
import com.meta.spatial.core.Pose
import com.meta.spatial.compose.ComposeViewPanelRegistration
import com.meta.spatial.core.SpatialFeature
import com.meta.spatial.core.Vector3
import com.meta.spatial.runtime.ReferenceSpace
import com.meta.spatial.runtime.StereoMode
import com.meta.spatial.toolkit.AppSystemActivity
import com.meta.spatial.toolkit.DpPerMeterDisplayOptions
import com.meta.spatial.toolkit.Equirect360ShapeOptions
import com.meta.spatial.toolkit.Hittable
import com.meta.spatial.toolkit.MeshCollision
import com.meta.spatial.toolkit.MediaPanelRenderOptions
import com.meta.spatial.toolkit.MediaPanelSettings
import com.meta.spatial.toolkit.PanelRegistration
import com.meta.spatial.toolkit.PixelDisplayOptions
import com.meta.spatial.toolkit.VideoSurfacePanelRegistration
import com.meta.spatial.toolkit.Visible
import com.meta.spatial.toolkit.PanelStyleOptions
import com.meta.spatial.toolkit.QuadShapeOptions
import com.meta.spatial.toolkit.Transform
import com.meta.spatial.toolkit.UIPanelSettings
import com.meta.spatial.toolkit.createPanelEntity
import com.meta.spatial.vr.VRFeature

class QuestActivity : AppSystemActivity() {
    override fun registerFeatures(): List<SpatialFeature> {
        return listOf(
            VRFeature(this),
            ComposeFeature(),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        QuestAccount.attach(this)
        PairScanner.attach(this)
        SanctumSound.attach(this)
        Tour360.attach(this)
        SpherePlayer.attach(this)
        HallSphere.attach(this)
        systemManager.registerSystem(RingInputSystem())
    }

    private var pausedAtMs = 0L

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Ring.greetAgain()
    }

    override fun onPause() {
        pausedAtMs = SystemClock.elapsedRealtime()
        PairScanner.onPause()
        QuestAccount.onPause()
        SanctumSound.hold()
        SpherePlayer.pauseForApp()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        val fromYoutube = Ring.consumeYoutubeHandoff()
        if (!fromYoutube &&
            pausedAtMs != 0L &&
            SystemClock.elapsedRealtime() - pausedAtMs > 60_000L
        ) {
            Ring.greetAgain()
        }
        pausedAtMs = 0L
        PairScanner.onResume()
        QuestAccount.onResume()
        SanctumSound.releaseHold()
        SpherePlayer.resumeForApp()
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
        PairScanner.detach()
        SanctumSound.release()
        SpherePlayer.stop()
        super.onDestroy()
    }

    override fun onSceneReady() {
        super.onSceneReady()
        scene.setReferenceSpace(ReferenceSpace.LOCAL_FLOOR)
        scene.setViewOrigin(0f, 0f, 0f, 0f)
        scene.enablePassthrough(true)
        scene.enableHolePunching(true)
        scene.setBackfillColor(Color4(0f, 0f, 0f, 0f))
        scene.setLightingEnvironment(
            ambientColor = Vector3(0.04f, 0.025f, 0.015f),
            sunColor = Vector3(0.2f, 0.1f, 0.04f),
            sunDirection = -Vector3(0.15f, 1f, -0.35f),
            environmentIntensity = 0.12f,
        )
        val slotIds = intArrayOf(
            R.id.ring_slot_0,
            R.id.ring_slot_1,
            R.id.ring_slot_2,
            R.id.ring_slot_3,
            R.id.ring_slot_4,
        )
        slotIds.forEachIndexed { index, panelId ->
            RingWorld.slots[index] = Entity.createPanelEntity(panelId, Transform(Pose()))
        }
        RingWorld.stream = Entity.createPanelEntity(
            R.id.ring_stream,
            Transform(Pose()),
        )
        RingWorld.opening = Entity.createPanelEntity(
            R.id.ring_opening,
            Transform(Pose()),
        )
        // The 360 spheres surround the viewer, so they must not swallow the controller ray.
        RingWorld.hall = Entity.createPanelEntity(
            R.id.ring_hall,
            Transform(Pose()),
        ).also {
            it.setComponent(Visible(false))
            it.setComponent(Hittable(MeshCollision.NoCollision))
        }
        RingWorld.environment = Entity.createPanelEntity(
            R.id.ring_environment,
            Transform(Pose()),
        ).also { it.setComponent(Visible(false)) }
        RingWorld.sphere = Entity.createPanelEntity(
            R.id.ring_sphere,
            Transform(Pose()),
        ).also {
            it.setComponent(Visible(false))
            it.setComponent(Hittable(MeshCollision.NoCollision))
        }
        RingWorld.tourEnd = Entity.createPanelEntity(
            R.id.ring_tour_end,
            Transform(Pose()),
        ).also { it.setComponent(Visible(false)) }
        RingWorld.pair = Entity.createPanelEntity(
            R.id.ring_pair,
            Transform(Pose()),
        ).also { it.setComponent(Visible(false)) }
        RingWorld.profile = Entity.createPanelEntity(
            R.id.ring_profile,
            Transform(Pose()),
        ).also { it.setComponent(Visible(false)) }
        RingWorld.chip = Entity.createPanelEntity(
            R.id.ring_profile_chip,
            Transform(Pose()),
        ).also { it.setComponent(Visible(false)) }
    }

    override fun registerPanels(): List<PanelRegistration> {
        val slotIds = intArrayOf(
            R.id.ring_slot_0,
            R.id.ring_slot_1,
            R.id.ring_slot_2,
            R.id.ring_slot_3,
            R.id.ring_slot_4,
        )
        val cards = slotIds.mapIndexed { index, panelId ->
            ComposeViewPanelRegistration(
                panelId,
                composeViewCreator = { _, context ->
                    ComposeView(context).apply {
                        setContent { SlotCard(index) }
                    }
                },
                settingsCreator = {
                    UIPanelSettings(
                        shape = QuadShapeOptions(width = 0.50f, height = 0.74f),
                        style = PanelStyleOptions(themeResourceId = R.style.PanelAppThemeTransparent),
                        display = DpPerMeterDisplayOptions(),
                    )
                },
            )
        }
        val stream = ComposeViewPanelRegistration(
            R.id.ring_stream,
            composeViewCreator = { _, context ->
                ComposeView(context).apply {
                    setContent { StreamPanel() }
                }
            },
            settingsCreator = {
                UIPanelSettings(
                    shape = QuadShapeOptions(width = 1.72f, height = 1.08f),
                    style = PanelStyleOptions(themeResourceId = R.style.PanelAppThemeTransparent),
                    display = DpPerMeterDisplayOptions(),
                )
            },
        )
        val opening = ComposeViewPanelRegistration(
            R.id.ring_opening,
            composeViewCreator = { _, context ->
                ComposeView(context).apply {
                    setContent { SanctumOpening() }
                }
            },
            settingsCreator = {
                UIPanelSettings(
                    shape = QuadShapeOptions(width = 1.20f, height = 1.30f),
                    style = PanelStyleOptions(themeResourceId = R.style.PanelAppThemeTransparent),
                    display = DpPerMeterDisplayOptions(),
                )
            },
        )
        val hall = VideoSurfacePanelRegistration(
            R.id.ring_hall,
            { _, surface ->
                HallSphere.drawOn(surface)
            },
            {
                MediaPanelSettings(
                    shape = Equirect360ShapeOptions(radius = 40f),
                    display = PixelDisplayOptions(width = 1280, height = 640),
                    rendering = MediaPanelRenderOptions(stereoMode = StereoMode.None, zIndex = -10),
                )
            },
        )
        val environment = ComposeViewPanelRegistration(
            R.id.ring_environment,
            composeViewCreator = { _, context ->
                ComposeView(context).apply {
                    setContent { EnvironmentToggle() }
                }
            },
            settingsCreator = {
                UIPanelSettings(
                    shape = QuadShapeOptions(width = 0.42f, height = 0.09f),
                    style = PanelStyleOptions(themeResourceId = R.style.PanelAppThemeTransparent),
                    display = DpPerMeterDisplayOptions(),
                )
            },
        )
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
        val tourEnd = ComposeViewPanelRegistration(
            R.id.ring_tour_end,
            composeViewCreator = { _, context ->
                ComposeView(context).apply {
                    setContent { TourEndPanel() }
                }
            },
            settingsCreator = {
                UIPanelSettings(
                    shape = QuadShapeOptions(width = 1.10f, height = 0.56f),
                    style = PanelStyleOptions(themeResourceId = R.style.PanelAppThemeTransparent),
                    display = DpPerMeterDisplayOptions(),
                )
            },
        )
        val pair = composePanel(R.id.ring_pair, width = 1.05f, height = 0.62f) { PairPanel() }
        val profile = composePanel(R.id.ring_profile, width = 1.20f, height = 0.86f) { ProfilePanel() }
        val chip = composePanel(R.id.ring_profile_chip, width = 0.46f, height = 0.09f) { ProfileChip() }
        return cards + stream + opening + hall + environment + sphere + tourEnd + pair + profile + chip
    }

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
