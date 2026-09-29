package com.hinvr.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hinvr.app.R
import com.hinvr.app.data.MembershipTier
import com.hinvr.app.data.SessionSnapshot
import com.hinvr.app.data.hasDeskPass
import com.hinvr.app.i18n.LocalAppLanguage
import com.hinvr.app.i18n.localized
import com.hinvr.app.navigation.Destinations
import com.hinvr.app.navigation.LocalCatalogRepository
import com.hinvr.app.navigation.LocalSessionRepository
import com.hinvr.app.ui.catalog.HinvrCatalog
import com.hinvr.app.ui.catalog.ServiceTile
import com.hinvr.app.ui.components.BentoTile
import com.hinvr.app.ui.components.CatalogPhoto
import com.hinvr.app.ui.components.CircleIconButton
import com.hinvr.app.ui.components.CustomRequestPill
import com.hinvr.app.ui.components.HinvrBackground
import com.hinvr.app.ui.components.IvoryCard
import com.hinvr.app.ui.components.LocalDockClearance
import com.hinvr.app.ui.components.MandirHeroCard
import com.hinvr.app.ui.components.mandirArtwork
import com.hinvr.app.ui.components.MembershipBadge
import com.hinvr.app.ui.components.PortraitPhotoCard
import com.hinvr.app.ui.components.SectionTitle
import com.hinvr.app.ui.components.StatusCard
import com.hinvr.app.ui.components.StatusLabel
import com.hinvr.app.ui.motion.HinvrMotion
import com.hinvr.app.ui.plans.MembershipGateSheet
import com.hinvr.app.ui.theme.Atmosphere
import com.hinvr.app.ui.theme.HinvrSideInset
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onOpenProfile: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenPlans: () -> Unit,
    onOpenPass: () -> Unit,
    onOpenConcierge: () -> Unit,
    onOpenRoute: (String) -> Unit,
) {
    val session = LocalSessionRepository.current
    val catalog = LocalCatalogRepository.current
    val snap by session.snapshot.collectAsStateWithLifecycle(initialValue = SessionSnapshot())
    val mandirs by catalog.mandirs.collectAsStateWithLifecycle()
    val tiles by catalog.services.collectAsStateWithLifecycle()
    val headline by catalog.headline.collectAsStateWithLifecycle()
    val colors = HinvrTheme.colors
    val language = LocalAppLanguage.current
    val localizedMandirs = mandirs.localized()
    val localizedTiles = tiles.map { it.localized() }
    val live = localizedMandirs.firstOrNull { it.live }
    val hero = live ?: localizedMandirs.firstOrNull()
    val member = snap.tier != MembershipTier.None
    var revealContent by remember { mutableStateOf(false) }
    var assistPrompt by remember { mutableStateOf<MandirAssistPrompt?>(null) }
    val displayHeadline = if (headline == HinvrCatalog.DefaultHeadline || language == "hi") {
        stringResource(R.string.home_headline)
    } else {
        headline
    }

    LaunchedEffect(Unit) { catalog.refresh() }
    LaunchedEffect(Unit) {
        delay(90)
        revealContent = true
    }

    HinvrBackground(atmosphere = Atmosphere.Sabha, darkIcons = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = HinvrSideInset)
                    .padding(top = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.app_name),
                        style = HinvrTypography.headlineMedium.copy(fontSize = 22.sp),
                        color = colors.ink,
                        modifier = Modifier.weight(1f),
                    )
                    CircleIconButton(
                        icon = if (member) Icons.Outlined.Wallet else Icons.AutoMirrored.Outlined.Assignment,
                        contentDescription = if (member) stringResource(R.string.nav_pass) else stringResource(R.string.cd_plans),
                        onClick = { if (member) onOpenPass() else onOpenPlans() },
                    )
                    Spacer(Modifier.padding(start = 8.dp))
                    CircleIconButton(
                        icon = Icons.Outlined.NotificationsNone,
                        contentDescription = stringResource(R.string.cd_notifications),
                        onClick = onOpenNotifications,
                    )
                    Spacer(Modifier.padding(start = 8.dp))
                    CircleIconButton(
                        icon = Icons.Outlined.Person,
                        contentDescription = stringResource(R.string.cd_profile),
                        onClick = onOpenProfile,
                    )
                }
                Spacer(Modifier.height(28.dp))
                StatusLabel(
                    if (live != null) stringResource(R.string.home_live_now) else stringResource(R.string.home_temple_access),
                    darkSurface = false,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    if (snap.displayName.isBlank()) {
                        stringResource(R.string.home_namaste)
                    } else {
                        stringResource(R.string.home_namaste_name, snap.displayName)
                    },
                    style = HinvrTypography.displayLarge.copy(fontSize = 52.sp, lineHeight = 54.sp),
                    color = colors.ink,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    if (live != null) stringResource(R.string.home_live_darshan, live.name) else displayHeadline,
                    style = HinvrTypography.bodyLarge,
                    color = colors.inkMuted,
                    maxLines = 2,
                )
                Spacer(Modifier.height(16.dp))
                MembershipBadge(
                    tier = if (member) {
                        stringResource(
                            R.string.tier_member,
                            if (snap.tier == MembershipTier.Nri) "NRI" else snap.tier.name,
                        )
                    } else {
                        stringResource(R.string.membership)
                    },
                    validity = if (member) snap.validUntilLabel else stringResource(R.string.explore_access),
                )
                Spacer(Modifier.height(8.dp))
            }

            Column(Modifier.padding(horizontal = HinvrSideInset)) {
                AnimatedVisibility(visible = revealContent, enter = homeReveal(0)) {
                    Column {
                        if (live != null) {
                            StatusCard(
                                title = stringResource(R.string.home_live_darshan, live.name),
                                subtitle = "${live.city} · ${live.updatedLabel}",
                                icon = Icons.AutoMirrored.Outlined.Assignment,
                                onClick = { onOpenRoute(Destinations.livePlayer(live.id)) },
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            Spacer(Modifier.height(22.dp))
                        }
                    }
                }
                AnimatedVisibility(visible = revealContent, enter = homeReveal(90)) {
                    Column {
                        BentoGrid(
                            tiles = localizedTiles,
                            onOpenPass = onOpenPass,
                            onOpenConcierge = onOpenConcierge,
                            onOpenRoute = onOpenRoute,
                        )
                        Spacer(Modifier.height(22.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.gold))
                        Spacer(Modifier.height(22.dp))
                        CatalogPhoto(
                            photoUrl = "",
                            fallback = mandirArtwork(hero?.id.orEmpty()),
                            contentDescription = hero?.name ?: stringResource(R.string.home_courtyard_cd),
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(3f / 4f),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            hero?.place ?: stringResource(R.string.home_architecture),
                            style = HinvrTypography.labelSmall,
                            color = colors.saffron,
                        )
                        Text(
                            hero?.name ?: stringResource(R.string.home_courtyard),
                            style = HinvrTypography.headlineMedium,
                            color = colors.ink,
                        )
                    }
                }
                AnimatedVisibility(visible = revealContent, enter = homeReveal(180)) {
                    Column {
                        Spacer(Modifier.height(18.dp))
                        CustomRequestPill(onClick = onOpenConcierge)
                        Spacer(Modifier.height(32.dp))
                        SectionTitle(stringResource(R.string.home_near))
                        Spacer(Modifier.height(14.dp))
                    }
                }
            }

            AnimatedVisibility(visible = revealContent, enter = homeReveal(250)) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = HinvrSideInset),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(localizedMandirs, key = { it.id }) { mandir ->
                        PortraitPhotoCard(
                            title = mandir.name,
                            place = mandir.city,
                            scene = mandir.scene,
                            live = mandir.live,
                            photoUrl = mandir.photoUrl,
                            mandirId = mandir.id,
                            onClick = { onOpenRoute(Destinations.mandir(mandir.id)) },
                        )
                    }
                }
            }

            AnimatedVisibility(visible = revealContent, enter = homeReveal(330)) {
                Column(Modifier.padding(horizontal = HinvrSideInset)) {
                    Spacer(Modifier.height(36.dp))
                    SectionTitle(stringResource(R.string.home_featured))
                    Spacer(Modifier.height(14.dp))
                    if (hero != null) {
                        MandirHeroCard(
                            title = hero.name,
                            place = hero.city,
                            scene = hero.scene,
                            live = hero.live,
                            photoUrl = hero.photoUrl,
                            mandirId = hero.id,
                            onPhoto = { onOpenRoute(Destinations.mandir(hero.id)) },
                            onLive = { onOpenRoute(Destinations.livePlayer(hero.id)) },
                            onVr = { onOpenRoute(Destinations.vrPlayer(hero.id)) },
                            onPass = onOpenPass,
                            onAssist = {
                                when {
                                    !hero.passAccepted -> assistPrompt = MandirAssistPrompt.Desk
                                    !snap.tier.hasDeskPass -> assistPrompt = MandirAssistPrompt.Gate
                                    else -> onOpenRoute(Destinations.passVisit(hero.id))
                                }
                            },
                        )
                        Spacer(Modifier.height(28.dp))
                    }
                    SectionTitle(stringResource(R.string.home_hosts))
                    Spacer(Modifier.height(14.dp))
                }
            }
            AnimatedVisibility(visible = revealContent, enter = homeReveal(420)) {
                HostRail()
            }
            Spacer(Modifier.height(28.dp + LocalDockClearance.current))
        }
        when (assistPrompt) {
            MandirAssistPrompt.Gate -> MembershipGateSheet(
                reason = stringResource(R.string.gate_visit),
                onDismiss = { assistPrompt = null },
                onSeePlans = {
                    assistPrompt = null
                    onOpenPlans()
                },
            )
            MandirAssistPrompt.Desk -> MembershipGateSheet(
                reason = stringResource(R.string.gate_not_accepted),
                onDismiss = { assistPrompt = null },
                onSeePlans = {
                    assistPrompt = null
                    onOpenConcierge()
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

private fun homeReveal(delayMs: Int): EnterTransition =
    fadeIn(
        animationSpec = tween(
            durationMillis = 520,
            delayMillis = delayMs,
            easing = HinvrMotion.EnterEasing,
        ),
    ) + slideInVertically(
        animationSpec = tween(
            durationMillis = 560,
            delayMillis = delayMs,
            easing = HinvrMotion.EnterEasing,
        ),
        initialOffsetY = { height -> height / 4 },
    ) + scaleIn(
        initialScale = 0.985f,
        animationSpec = tween(
            durationMillis = 560,
            delayMillis = delayMs,
            easing = HinvrMotion.EnterEasing,
        ),
    )

@Composable
private fun HostRail() {
    val hosts = listOf(
        Triple("TIRUPATI", "Ananya Rao", stringResource(R.string.host_tirupati_bio)),
        Triple("KASHI", "Raghav Mishra", stringResource(R.string.host_kashi_bio)),
        Triple("SHIRDI", "Meera Patil", stringResource(R.string.host_shirdi_bio)),
    )
    val colors = HinvrTheme.colors
    LazyRow(
        contentPadding = PaddingValues(horizontal = HinvrSideInset),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(hosts) { (city, name, bio) ->
            Box(Modifier.width(238.dp).padding(top = 22.dp)) {
                IvoryCard(modifier = Modifier.padding(top = 18.dp)) {
                    Spacer(Modifier.height(18.dp))
                    Text(city, style = HinvrTypography.labelSmall, color = colors.saffron)
                    Text(name, style = HinvrTypography.titleLarge, color = colors.ink)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        bio,
                        style = HinvrTypography.bodyMedium,
                        color = colors.inkMuted,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Box(
                    Modifier
                        .padding(start = 18.dp)
                        .size(54.dp)
                        .background(colors.olive, CircleShape)
                        .border(1.dp, colors.gold, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        name.split(" ").mapNotNull { it.firstOrNull() }.joinToString(""),
                        style = HinvrTypography.titleMedium,
                        color = colors.cream,
                    )
                }
            }
        }
    }
}

@Composable
private fun BentoGrid(
    tiles: List<ServiceTile>,
    onOpenPass: () -> Unit,
    onOpenConcierge: () -> Unit,
    onOpenRoute: (String) -> Unit,
) {
    if (tiles.isEmpty()) return

    fun open(tile: ServiceTile) {
        when (tile.route) {
            Destinations.Pass -> onOpenPass()
            Destinations.Concierge -> onOpenConcierge()
            else -> onOpenRoute(tile.route)
        }
    }

    val left = tiles.filterIndexed { index, _ -> index % 2 == 0 }
    val right = tiles.filterIndexed { index, _ -> index % 2 == 1 }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            left.forEach { tile ->
                BentoTile(
                    tile.title,
                    tile.benefit,
                    tile.scene,
                    248.dp,
                    { open(tile) },
                    photoUrl = tile.photoUrl,
                )
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            right.forEach { tile ->
                BentoTile(
                    tile.title,
                    tile.benefit,
                    tile.scene,
                    248.dp,
                    { open(tile) },
                    photoUrl = tile.photoUrl,
                )
            }
        }
    }
}
