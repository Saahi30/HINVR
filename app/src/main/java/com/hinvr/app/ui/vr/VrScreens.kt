package com.hinvr.app.ui.vr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hinvr.app.R
import com.hinvr.app.data.hasDeskPass
import com.hinvr.app.i18n.localized
import com.hinvr.app.navigation.LocalCatalogRepository
import com.hinvr.app.navigation.LocalSessionRepository
import com.hinvr.app.ui.catalog.Mandir
import com.hinvr.app.ui.components.CatalogPhoto
import com.hinvr.app.ui.components.HinvrBackground
import com.hinvr.app.ui.components.HinvrPrimaryButton
import com.hinvr.app.ui.components.PortraitPhotoCard
import com.hinvr.app.ui.components.SabhaTopBar
import com.hinvr.app.ui.components.SectionTitle
import com.hinvr.app.ui.components.StatusLabel
import com.hinvr.app.ui.components.mandirArtwork
import com.hinvr.app.ui.media.NamasteLandscapePrompt
import com.hinvr.app.ui.media.StreamPane
import com.hinvr.app.ui.media.isViewerPortrait
import com.hinvr.app.ui.plans.MembershipGateSheet
import com.hinvr.app.ui.theme.Atmosphere
import com.hinvr.app.ui.theme.HinvrSideInset
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography

@Composable
fun VrListScreen(onBack: () -> Unit, onOpenPlayer: (String) -> Unit, onPlans: () -> Unit) {
    val colors = HinvrTheme.colors
    val catalog = LocalCatalogRepository.current
    val mandirs by catalog.mandirs.collectAsStateWithLifecycle()
    val snap by LocalSessionRepository.current.snapshot.collectAsStateWithLifecycle()
    val hasVr = snap.tier.hasDeskPass
    var showGate by remember { mutableStateOf(false) }
    val rows = mandirs.localized().filter { it.vr }
    HinvrBackground(atmosphere = Atmosphere.Sabha) {
        Column(Modifier.fillMaxSize()) {
            SabhaTopBar(onBack = onBack)
            LazyColumn(
                contentPadding = PaddingValues(horizontal = HinvrSideInset, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { SectionTitle(stringResource(R.string.vr_title)) }
                item {
                    Text(
                        if (snap.loaded && !hasVr) stringResource(R.string.vr_body) else stringResource(R.string.vr_body_member),
                        style = HinvrTypography.bodyLarge,
                        color = colors.inkMuted,
                    )
                }
                items(rows, key = { it.id }) { row ->
                    PortraitPhotoCard(
                        title = row.name,
                        place = stringResource(R.string.vr_place),
                        scene = row.scene,
                        live = false,
                        photoUrl = row.photoUrl,
                        mandirId = row.id,
                        onClick = {
                            // The player checks again; here we only avoid a pointless trip.
                            if (snap.loaded && !hasVr) showGate = true else onOpenPlayer(row.id)
                        },
                        width = null,
                        height = 200.dp,
                    )
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
        if (showGate) {
            MembershipGateSheet(
                reason = stringResource(R.string.gate_vr),
                onDismiss = { showGate = false },
                onSeePlans = {
                    showGate = false
                    onPlans()
                },
            )
        }
    }
}

/**
 * Recorded 360 for Gold, Platinum and NRI. The stream is not even loaded until the
 * stored tier is known and entitled; everyone else sees the still and the reason.
 */
@Composable
fun VrPlayerScreen(id: String, onBack: () -> Unit, onPlans: () -> Unit = {}) {
    val catalog = LocalCatalogRepository.current
    val mandirs by catalog.mandirs.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val mandir = remember(id, mandirs, context) { catalog.mandir(id)?.localized(context) }
    val snap by LocalSessionRepository.current.snapshot.collectAsStateWithLifecycle()
    val entitled = snap.loaded && snap.tier.hasDeskPass
    val gated = snap.loaded && !snap.tier.hasDeskPass
    var gateDismissed by rememberSaveable { mutableStateOf(false) }
    val colors = HinvrTheme.colors
    val stream = mandir?.vrUrl.orEmpty()
    val portrait = isViewerPortrait()
    var watchUpright by rememberSaveable { mutableStateOf(false) }
    val showInfo = !portrait || watchUpright
    HinvrBackground(atmosphere = Atmosphere.Sanctum) {
        Box(Modifier.fillMaxSize()) {
            if (entitled && mandir != null && stream.isNotBlank()) {
                StreamPane(url = stream, modifier = Modifier.fillMaxSize())
                NamasteLandscapePrompt(visible = portrait && !watchUpright, onWatchUpright = { watchUpright = true })
                Column(Modifier.fillMaxSize().navigationBarsPadding()) {
                    SabhaTopBar(onBack = onBack, onPhoto = true)
                    if (showInfo) {
                        // Static on purpose: a recording never wears the pulsing LIVE pill.
                        StatusLabel(stringResource(R.string.vr_pill), Modifier.padding(horizontal = HinvrSideInset))
                        Spacer(Modifier.weight(1f))
                        Column(Modifier.padding(HinvrSideInset)) {
                            Text(mandir.name, style = HinvrTypography.headlineMedium, color = colors.cream)
                            Text(stringResource(R.string.vr_move), style = HinvrTypography.bodyMedium, color = colors.creamMuted)
                        }
                    }
                }
            } else {
                VrStill(mandir)
                Column(
                    Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                ) {
                    SabhaTopBar(onBack = onBack, onPhoto = true)
                    StatusLabel(stringResource(R.string.vr_pill), Modifier.padding(horizontal = HinvrSideInset))
                    Spacer(Modifier.weight(1f))
                    Column(Modifier.padding(HinvrSideInset)) {
                        Text(
                            mandir?.name ?: stringResource(R.string.mandir_missing_title),
                            style = HinvrTypography.headlineMedium,
                            color = colors.cream,
                        )
                        when {
                            gated -> {
                                Text(stringResource(R.string.gate_gold_body), style = HinvrTypography.bodyMedium, color = colors.creamMuted)
                                Spacer(Modifier.height(16.dp))
                                HinvrPrimaryButton(stringResource(R.string.vr_gold_button), onPlans)
                            }
                            entitled -> Text(
                                stringResource(R.string.vr_missing),
                                style = HinvrTypography.bodyMedium,
                                color = colors.creamMuted,
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
            if (gated && !gateDismissed) {
                MembershipGateSheet(
                    reason = stringResource(R.string.gate_vr),
                    onDismiss = { gateDismissed = true },
                    onSeePlans = {
                        gateDismissed = true
                        onPlans()
                    },
                )
            }
        }
    }
}

@Composable
private fun VrStill(mandir: Mandir?) {
    Box(Modifier.fillMaxSize()) {
        if (mandir != null) {
            CatalogPhoto(
                photoUrl = mandir.photoUrl,
                fallback = mandirArtwork(mandir.id),
                placeholderName = mandir.name,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color(0xAA100B08),
                            0.35f to Color(0x55100B08),
                            1f to Color(0xF2100B08),
                        ),
                    ),
                ),
        )
    }
}
