package com.hinvr.app.ui.mandirs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hinvr.app.R
import com.hinvr.app.data.SessionSnapshot
import com.hinvr.app.data.hasDeskPass
import com.hinvr.app.i18n.localized
import com.hinvr.app.navigation.LocalCatalogRepository
import com.hinvr.app.navigation.LocalSessionRepository
import com.hinvr.app.ui.components.FilterChip
import com.hinvr.app.ui.components.HinvrBackground
import com.hinvr.app.ui.components.HinvrGoldOutlineButton
import com.hinvr.app.ui.components.HinvrPrimaryButton
import com.hinvr.app.ui.components.IvoryCard
import com.hinvr.app.ui.components.LocalDockClearance
import com.hinvr.app.ui.components.MandirHeroCard
import com.hinvr.app.ui.components.PortraitPhotoCard
import com.hinvr.app.ui.components.SabhaSearchField
import com.hinvr.app.ui.components.SabhaTopBar
import com.hinvr.app.ui.components.SectionTitle
import com.hinvr.app.ui.plans.MembershipGateSheet
import com.hinvr.app.ui.theme.Atmosphere
import com.hinvr.app.ui.theme.HinvrSideInset
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography
import kotlinx.coroutines.launch

private data class MandirChip(val key: String, val labelRes: Int)

@Composable
fun MandirsScreen(onOpenTemple: (String) -> Unit) {
    val colors = HinvrTheme.colors
    val catalog = LocalCatalogRepository.current
    val session = LocalSessionRepository.current
    val mandirs by catalog.mandirs.collectAsStateWithLifecycle()
    val snap by session.snapshot.collectAsStateWithLifecycle(initialValue = SessionSnapshot())
    var query by remember { mutableStateOf("") }
    var chip by remember { mutableStateOf<String?>(null) }
    val chips = listOf(
        MandirChip("Live", R.string.chip_live),
        MandirChip("VR", R.string.chip_vr),
        MandirChip("Pass", R.string.chip_pass),
        MandirChip("Nearby", R.string.chip_nearby),
        MandirChip("Favorites", R.string.chip_favorites),
    )
    val localizedMandirs = mandirs.localized()
    val filtered = localizedMandirs.filter { row ->
        val q = query.isBlank() || row.name.contains(query, true) || row.place.contains(query, true)
        val c = when (chip) {
            "Live" -> row.live
            "VR" -> row.vr
            "Pass" -> row.passAccepted
            "Nearby" -> snap.city.isNotBlank() &&
                (row.city.contains(snap.city, true) || row.place.contains(snap.city, true))
            "Favorites" -> row.id in snap.favoriteMandirs
            else -> true
        }
        q && c
    }

    HinvrBackground(atmosphere = Atmosphere.Sabha) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = HinvrSideInset),
        ) {
            Spacer(Modifier.height(12.dp))
            SectionTitle(stringResource(R.string.nav_mandirs))
            Spacer(Modifier.height(14.dp))
            SabhaSearchField(query, { query = it }, stringResource(R.string.mandirs_search))
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(chips, key = { it.key }) { item ->
                    FilterChip(
                        stringResource(item.labelRes),
                        selected = chip == item.key,
                        onClick = { chip = if (chip == item.key) null else item.key },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            if (filtered.isEmpty()) {
                Text(
                    stringResource(R.string.mandirs_empty),
                    style = HinvrTypography.headlineMedium,
                    color = colors.ink,
                    modifier = Modifier.padding(top = 32.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.mandirs_empty_body),
                    style = HinvrTypography.bodyLarge,
                    color = colors.inkMuted,
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 16.dp + LocalDockClearance.current),
                ) {
                    items(filtered, key = { it.id }) { row ->
                        PortraitPhotoCard(
                            title = row.name,
                            place = row.city,
                            scene = row.scene,
                            live = row.live,
                            vr = row.vr,
                            passAccepted = row.passAccepted,
                            photoUrl = row.photoUrl,
                            mandirId = row.id,
                            onClick = { onOpenTemple(row.id) },
                            width = null,
                            height = 200.dp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TempleDetailScreen(
    id: String,
    onBack: () -> Unit,
    onLive: () -> Unit,
    onVr: () -> Unit,
    onPass: () -> Unit = {},
    onAssist: () -> Unit = {},
    onPlans: () -> Unit = {},
    onConcierge: () -> Unit = {},
) {
    val catalog = LocalCatalogRepository.current
    val session = LocalSessionRepository.current
    val mandirs by catalog.mandirs.collectAsStateWithLifecycle()
    val snap by session.snapshot.collectAsStateWithLifecycle(initialValue = SessionSnapshot())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val mandir = remember(id, mandirs, context) { catalog.mandir(id).localized(context) }
    val colors = HinvrTheme.colors
    val uriHandler = LocalUriHandler.current
    val facilities = remember(mandir.facilities) {
        mandir.facilities.lines().map { it.trim() }.filter { it.isNotBlank() }
    }
    var assistPrompt by remember { mutableStateOf<MandirAssistPrompt?>(null) }
    val genericSummary = stringResource(R.string.mandir_generic_summary, mandir.name, mandir.place)
    HinvrBackground(atmosphere = Atmosphere.Sabha, darkIcons = false) {
        Column(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            SabhaTopBar(title = mandir.city, onBack = onBack)
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = HinvrSideInset),
            ) {
                MandirHeroCard(
                    title = mandir.name,
                    place = mandir.place,
                    scene = mandir.scene,
                    live = mandir.live,
                    photoUrl = mandir.photoUrl,
                    mandirId = mandir.id,
                    onPhoto = {},
                    onLive = onLive,
                    onVr = onVr,
                    onPass = onPass,
                    onAssist = {
                        when {
                            !mandir.passAccepted -> assistPrompt = MandirAssistPrompt.Desk
                            !snap.tier.hasDeskPass -> assistPrompt = MandirAssistPrompt.Gate
                            else -> onAssist()
                        }
                    },
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (mandir.live) Badge(stringResource(R.string.badge_live))
                    if (mandir.vr) Badge(stringResource(R.string.badge_vr))
                    if (mandir.passAccepted) Badge(stringResource(R.string.badge_pass))
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    mandir.summary.ifBlank { genericSummary },
                    style = HinvrTypography.headlineMedium,
                    color = colors.ink,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    if (mandir.id in snap.favoriteMandirs) {
                        stringResource(R.string.saved_mandir)
                    } else {
                        stringResource(R.string.save_mandir)
                    },
                    style = HinvrTypography.titleMedium,
                    color = if (mandir.id in snap.favoriteMandirs) colors.saffron else colors.gold,
                    modifier = Modifier
                        .clickable { scope.launch { session.toggleFavorite(mandir.id) } }
                        .padding(vertical = 8.dp),
                )
                Spacer(Modifier.height(18.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TempleFactCard(
                        eyebrow = stringResource(R.string.fact_focus),
                        value = mandir.deity.ifBlank { mandir.name },
                        modifier = Modifier.weight(1f),
                    )
                    TempleFactCard(
                        eyebrow = stringResource(R.string.fact_aarti),
                        value = mandir.nextAarti ?: stringResource(R.string.see_timings),
                        modifier = Modifier.weight(1f),
                    )
                }

                EditorialSection(
                    eyebrow = stringResource(R.string.why_kicker),
                    title = stringResource(R.string.why_title),
                    body = mandir.significance,
                )
                EditorialSection(
                    eyebrow = stringResource(R.string.history_kicker),
                    title = stringResource(R.string.history_title),
                    body = mandir.history,
                )
                EditorialSection(
                    eyebrow = stringResource(R.string.stone_kicker),
                    title = stringResource(R.string.stone_title),
                    body = mandir.architecture,
                )

                Spacer(Modifier.height(30.dp))
                SectionTitle(stringResource(R.string.before_you_go))
                Spacer(Modifier.height(12.dp))
                TempleGuideCard(stringResource(R.string.guide_dress), mandir.dressCode)
                Spacer(Modifier.height(10.dp))
                TempleGuideCard(stringResource(R.string.guide_time), mandir.bestTime)
                Spacer(Modifier.height(10.dp))
                TempleGuideCard(stringResource(R.string.guide_note), mandir.visitorNotes)

                if (facilities.isNotEmpty()) {
                    Spacer(Modifier.height(28.dp))
                    Text(stringResource(R.string.facilities), style = HinvrTypography.labelSmall, color = colors.gold)
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(facilities) { facility ->
                            FilterChip(facility, selected = false, onClick = {})
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))
                SectionTitle(stringResource(R.string.official_timings))
                Spacer(Modifier.height(10.dp))
                IvoryCard {
                    Text(mandir.timings, style = HinvrTypography.titleMedium, color = colors.ink)
                    if (mandir.address.isNotBlank()) {
                        Spacer(Modifier.height(14.dp))
                        Text(stringResource(R.string.address_label), style = HinvrTypography.labelSmall, color = colors.gold)
                        Spacer(Modifier.height(4.dp))
                        Text(mandir.address, style = HinvrTypography.bodyLarge, color = colors.inkMuted)
                    }
                    if (mandir.contactPhone.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(mandir.contactPhone, style = HinvrTypography.bodyLarge, color = colors.ink)
                    }
                }

                if (mandir.officialWebsite.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    HinvrGoldOutlineButton(
                        stringResource(R.string.open_website),
                        onClick = { uriHandler.openUri(mandir.officialWebsite) },
                    )
                }
                if (mandir.live) {
                    Spacer(Modifier.height(12.dp))
                    HinvrPrimaryButton(stringResource(R.string.watch_live), onLive)
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.mandir_disclaimer),
                    style = HinvrTypography.bodyMedium,
                    color = colors.inkMuted,
                )
                Spacer(Modifier.height(32.dp))
            }
        }
        when (assistPrompt) {
            MandirAssistPrompt.Gate -> MembershipGateSheet(
                reason = stringResource(R.string.gate_visit),
                onDismiss = { assistPrompt = null },
                onSeePlans = {
                    assistPrompt = null
                    onPlans()
                },
            )
            MandirAssistPrompt.Desk -> MembershipGateSheet(
                reason = stringResource(R.string.gate_not_accepted),
                onDismiss = { assistPrompt = null },
                onSeePlans = {
                    assistPrompt = null
                    onConcierge()
                },
                eyebrow = stringResource(R.string.gate_desk),
                body = stringResource(R.string.gate_desk_body),
                actionLabel = stringResource(R.string.ask_concierge),
            )
            null -> Unit
        }
    }
}

private enum class MandirAssistPrompt { Gate, Desk }

@Composable
private fun TempleFactCard(eyebrow: String, value: String, modifier: Modifier = Modifier) {
    val colors = HinvrTheme.colors
    IvoryCard(modifier = modifier) {
        Text(eyebrow, style = HinvrTypography.labelSmall, color = colors.gold)
        Spacer(Modifier.height(7.dp))
        Text(value, style = HinvrTypography.titleLarge, color = colors.ink)
    }
}

@Composable
private fun EditorialSection(eyebrow: String, title: String, body: String) {
    if (body.isBlank()) return
    val colors = HinvrTheme.colors
    Spacer(Modifier.height(32.dp))
    Text(eyebrow, style = HinvrTypography.labelSmall, color = colors.gold)
    Spacer(Modifier.height(7.dp))
    Text(title, style = HinvrTypography.headlineMedium, color = colors.ink)
    Spacer(Modifier.height(9.dp))
    Text(body, style = HinvrTypography.bodyLarge, color = colors.inkMuted)
}

@Composable
private fun TempleGuideCard(label: String, body: String) {
    if (body.isBlank()) return
    val colors = HinvrTheme.colors
    IvoryCard {
        Text(label, style = HinvrTypography.labelSmall, color = colors.saffron)
        Spacer(Modifier.height(6.dp))
        Text(body, style = HinvrTypography.bodyLarge, color = colors.ink)
    }
}

@Composable
private fun Badge(label: String) {
    val colors = HinvrTheme.colors
    Text(
        label,
        style = HinvrTypography.labelSmall,
        color = colors.gold,
        modifier = Modifier.padding(end = 4.dp),
    )
}
