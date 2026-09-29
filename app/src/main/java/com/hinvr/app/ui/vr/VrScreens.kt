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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hinvr.app.R
import com.hinvr.app.data.SessionSnapshot
import com.hinvr.app.data.hasDeskPass
import com.hinvr.app.i18n.localized
import com.hinvr.app.navigation.LocalCatalogRepository
import com.hinvr.app.navigation.LocalSessionRepository
import com.hinvr.app.ui.components.HinvrBackground
import com.hinvr.app.ui.components.HinvrPrimaryButton
import com.hinvr.app.ui.components.LivePill
import com.hinvr.app.ui.components.PortraitPhotoCard
import com.hinvr.app.ui.components.SabhaTopBar
import com.hinvr.app.ui.components.SectionTitle
import com.hinvr.app.ui.illustrations.TempleScene
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
    val snap by LocalSessionRepository.current.snapshot.collectAsStateWithLifecycle(initialValue = SessionSnapshot())
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
                        stringResource(R.string.vr_body),
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
                            if (hasVr) onOpenPlayer(row.id) else showGate = true
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
                onSeePlans = onPlans,
            )
        }
    }
}

@Composable
fun VrPlayerScreen(id: String, onBack: () -> Unit, onPlans: () -> Unit = {}) {
    val catalog = LocalCatalogRepository.current
    val mandirs by catalog.mandirs.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val mandir = remember(id, mandirs, context) { catalog.mandir(id).localized(context) }
    val snap by LocalSessionRepository.current.snapshot.collectAsStateWithLifecycle(initialValue = SessionSnapshot())
    val hasVr = snap.tier.hasDeskPass
    var showGate by remember(hasVr) { mutableStateOf(!hasVr) }
    val colors = HinvrTheme.colors
    val stream = mandir.vrUrl
    val portrait = isViewerPortrait()
    HinvrBackground(atmosphere = Atmosphere.Sanctum) {
        Box(Modifier.fillMaxSize()) {
            if (stream.isNotBlank()) {
                StreamPane(url = stream, modifier = Modifier.fillMaxSize())
                NamasteLandscapePrompt()
                Column(Modifier.fillMaxSize().navigationBarsPadding()) {
                    SabhaTopBar(onBack = onBack, onPhoto = true)
                    if (!portrait) {
                        LivePill(Modifier.padding(horizontal = HinvrSideInset), label = stringResource(R.string.vr_pill))
                        Spacer(Modifier.weight(1f))
                        Column(Modifier.padding(HinvrSideInset)) {
                            Text(mandir.name, style = HinvrTypography.headlineMedium, color = colors.cream)
                            Text(stringResource(R.string.vr_move), style = HinvrTypography.bodyMedium, color = colors.creamMuted)
                            Spacer(Modifier.height(16.dp))
                            HinvrPrimaryButton(stringResource(R.string.vr_gold_button), onPlans)
                        }
                    }
                }
            } else {
                TempleScene(mandir.scene, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Color(0x55100B08)))
                Column(
                    Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                ) {
                    SabhaTopBar(onBack = onBack, onPhoto = true)
                    LivePill(Modifier.padding(horizontal = HinvrSideInset), label = stringResource(R.string.vr_pill))
                    Spacer(Modifier.weight(1f))
                    Column(Modifier.padding(HinvrSideInset)) {
                        Text(mandir.name, style = HinvrTypography.headlineMedium, color = colors.cream)
                        Text(stringResource(R.string.vr_missing), style = HinvrTypography.bodyMedium, color = colors.creamMuted)
                        Spacer(Modifier.height(16.dp))
                        HinvrPrimaryButton(stringResource(R.string.vr_gold_button), onPlans)
                    }
                }
            }
            if (showGate) {
                MembershipGateSheet(
                    reason = stringResource(R.string.gate_vr),
                    onDismiss = { showGate = false },
                    onSeePlans = onPlans,
                )
            }
        }
    }
}
