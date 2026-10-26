package com.hinvr.app.ui.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hinvr.app.R
import com.hinvr.app.data.VrTarget
import com.hinvr.app.ui.vr.ViewInVrButton
import com.hinvr.app.ui.vr.ViewInVrSheetFor
import com.hinvr.app.ui.components.AartiReminderToggle
import com.hinvr.app.data.MembershipTier
import com.hinvr.app.i18n.localized
import com.hinvr.app.navigation.LocalCatalogRepository
import com.hinvr.app.navigation.LocalSessionRepository
import com.hinvr.app.ui.catalog.Mandir
import com.hinvr.app.ui.components.CatalogPhoto
import com.hinvr.app.ui.components.HinvrBackground
import com.hinvr.app.ui.components.HinvrPrimaryButton
import com.hinvr.app.ui.components.IvoryCard
import com.hinvr.app.ui.components.LivePill
import com.hinvr.app.ui.components.PortraitPhotoCard
import com.hinvr.app.ui.components.SabhaTopBar
import com.hinvr.app.ui.components.SectionTitle
import com.hinvr.app.ui.components.StatusLabel
import com.hinvr.app.ui.components.mandirArtwork
import com.hinvr.app.ui.media.NamasteLandscapePrompt
import com.hinvr.app.ui.media.StreamPane
import com.hinvr.app.ui.media.isViewerPortrait
import com.hinvr.app.ui.theme.Atmosphere
import com.hinvr.app.ui.theme.HinvrSideInset
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography

@Composable
fun LiveListScreen(onBack: () -> Unit, onOpenPlayer: (String) -> Unit) {
    val colors = HinvrTheme.colors
    val catalog = LocalCatalogRepository.current
    val mandirs by catalog.mandirs.collectAsStateWithLifecycle()
    val localizedMandirs = mandirs.localized()
    val live = localizedMandirs.filter { it.live }
    val upcoming = localizedMandirs.filter { !it.live && it.nextAarti != null }
    HinvrBackground(atmosphere = Atmosphere.Sabha) {
        Column(Modifier.fillMaxSize()) {
            SabhaTopBar(onBack = onBack)
            LazyColumn(
                contentPadding = PaddingValues(horizontal = HinvrSideInset, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { SectionTitle(stringResource(R.string.live_title)) }
                item {
                    Text(
                        stringResource(R.string.live_body),
                        style = HinvrTypography.bodyLarge,
                        color = colors.inkMuted,
                    )
                }
                if (live.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.live_none_now),
                            style = HinvrTypography.bodyMedium,
                            color = colors.inkMuted,
                        )
                    }
                }
                items(live, key = { it.id }) { row ->
                    PortraitPhotoCard(
                        title = row.name,
                        place = row.city,
                        scene = row.scene,
                        live = true,
                        photoUrl = row.photoUrl,
                        mandirId = row.id,
                        onClick = { onOpenPlayer(row.id) },
                        width = null,
                        height = 200.dp,
                    )
                }
                items(upcoming, key = { "n-${it.id}" }) { row ->
                    IvoryCard(onClick = { onOpenPlayer(row.id) }) {
                        Text(stringResource(R.string.next_aarti), style = HinvrTypography.labelSmall, color = colors.gold)
                        Spacer(Modifier.height(6.dp))
                        Text("${row.city} · ${row.nextAarti}", style = HinvrTypography.titleLarge, color = colors.ink)
                        Text(row.name, style = HinvrTypography.bodyMedium, color = colors.inkMuted)
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
fun LivePlayerScreen(
    id: String,
    onBack: () -> Unit,
    onPlans: () -> Unit = {},
    onHeadsets: () -> Unit = {},
    openViewInVr: Boolean = false,
) {
    val catalog = LocalCatalogRepository.current
    val mandirs by catalog.mandirs.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val mandir = remember(id, mandirs, context) { catalog.mandir(id)?.localized(context) }
    val snap by LocalSessionRepository.current.snapshot.collectAsStateWithLifecycle()
    val isMember = !snap.loaded || snap.tier != MembershipTier.None
    val colors = HinvrTheme.colors
    // Only a feed the server's checker marked live plays here. A channel that is
    // off air would otherwise sit behind a "live" label.
    val stream = mandir?.takeIf { it.live }?.liveUrl.orEmpty()
    val portrait = isViewerPortrait()
    var watchUpright by rememberSaveable { mutableStateOf(false) }
    // Opened from an aarti reminder's "View in VR": offer it once, over whatever is showing.
    var askVr by rememberSaveable { mutableStateOf(openViewInVr) }
    if (askVr && mandir != null) {
        ViewInVrSheetFor(
            target = VrTarget.Live,
            mandirId = mandir.id,
            title = mandir.name,
            onDismiss = { askVr = false },
            onHeadsets = {
                askVr = false
                onHeadsets()
            },
        )
    }
    val showInfo = !portrait || watchUpright
    HinvrBackground(atmosphere = Atmosphere.Sanctum) {
        Box(Modifier.fillMaxSize()) {
            if (mandir != null && stream.isNotBlank()) {
                StreamPane(url = stream, modifier = Modifier.fillMaxSize())
                NamasteLandscapePrompt(visible = portrait && !watchUpright, onWatchUpright = { watchUpright = true })
                Box(
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xCC100B08), Color.Transparent),
                            ),
                        )
                        .statusBarsPadding(),
                ) {
                    SabhaTopBar(onBack = onBack, onPhoto = true)
                }
                if (showInfo) {
                    Column(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color(0xE6100B08)),
                                ),
                            )
                            .navigationBarsPadding()
                            .padding(horizontal = HinvrSideInset, vertical = 16.dp),
                    ) {
                        LivePill(label = stringResource(R.string.live_darshan))
                        Spacer(Modifier.height(10.dp))
                        Text(mandir.name, style = HinvrTypography.headlineMedium, color = colors.cream)
                        Text(
                            stringResource(R.string.live_feed),
                            style = HinvrTypography.bodyMedium,
                            color = colors.creamMuted,
                        )
                        Spacer(Modifier.height(12.dp))
                        ViewInVrButton(VrTarget.Live, mandir.id, mandir.name, onHeadsets)
                        if (mandir.aartiTimes.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            AartiReminderToggle(mandir, onDark = true)
                        }
                        if (!isMember) {
                            Spacer(Modifier.height(12.dp))
                            HinvrPrimaryButton(stringResource(R.string.join_club), onPlans)
                        }
                    }
                }
            } else {
                FeedResting(mandir = mandir, onBack = onBack)
            }
        }
    }
}

/**
 * No live feed: the temple's own still, its name, and why. No LIVE or "official
 * stream" label and no play button, because nothing here would play.
 */
@Composable
private fun FeedResting(mandir: Mandir?, onBack: () -> Unit) {
    val colors = HinvrTheme.colors
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
        Column(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            SabhaTopBar(onBack = onBack, onPhoto = true)
            Spacer(Modifier.weight(1f))
            Column(Modifier.padding(HinvrSideInset)) {
                StatusLabel(stringResource(R.string.feed_resting_label))
                Spacer(Modifier.height(12.dp))
                Text(
                    mandir?.name ?: stringResource(R.string.mandir_missing_title),
                    style = HinvrTypography.headlineMedium,
                    color = colors.cream,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.live_feed_off),
                    style = HinvrTypography.bodyMedium,
                    color = colors.creamMuted,
                )
                mandir?.nextAarti?.let { aarti ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.next_aarti_at, aarti),
                        style = HinvrTypography.bodyMedium,
                        color = colors.cream,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
