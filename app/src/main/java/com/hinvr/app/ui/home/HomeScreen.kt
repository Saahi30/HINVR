package com.hinvr.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hinvr.app.R
import com.hinvr.app.data.MembershipTier
import com.hinvr.app.data.SessionSnapshot
import com.hinvr.app.navigation.Destinations
import com.hinvr.app.navigation.LocalCatalogRepository
import com.hinvr.app.navigation.LocalSessionRepository
import com.hinvr.app.ui.catalog.ServiceTile
import com.hinvr.app.ui.catalog.TileScene
import com.hinvr.app.ui.components.BentoTile
import com.hinvr.app.ui.components.CatalogPhoto
import com.hinvr.app.ui.components.CircleIconButton
import com.hinvr.app.ui.components.CustomRequestPill
import com.hinvr.app.ui.components.HinvrBackground
import com.hinvr.app.ui.components.MandirHeroCard
import com.hinvr.app.ui.components.PortraitPhotoCard
import com.hinvr.app.ui.components.SectionTitle
import com.hinvr.app.ui.components.StatusCard
import com.hinvr.app.ui.components.IvoryCard
import com.hinvr.app.ui.theme.Atmosphere
import com.hinvr.app.ui.theme.HinvrSideInset
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography
import com.hinvr.app.ui.motion.HinvrMotion
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
    val live = mandirs.firstOrNull { it.live }
    val hero = live ?: mandirs.firstOrNull()
    val member = snap.tier != MembershipTier.None
    var revealContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { catalog.refresh() }
    LaunchedEffect(Unit) {
        delay(90)
        revealContent = true
    }

    HinvrBackground(atmosphere = Atmosphere.Sabha, darkIcons = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to colors.linen,
                            0.48f to lerp(colors.linen, colors.saffron, 0.07f),
                            0.78f to lerp(colors.ivory, colors.saffron, 0.045f),
                            1f to colors.linen,
                        ),
                    ),
                )
                .verticalScroll(rememberScrollState()),
        ) {
            Box(Modifier.fillMaxWidth().height(372.dp)) {
                CatalogPhoto(
                    photoUrl = hero?.photoUrl.orEmpty(),
                    fallback = heroDrawable(hero?.scene),
                    contentDescription = hero?.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to Color(0x33100B08),
                                    0.34f to Color.Transparent,
                                    0.64f to Color(0x99100B08),
                                    0.9f to colors.linen.copy(alpha = 0.92f),
                                    1f to colors.linen,
                                ),
                            ),
                        ),
                )
                Column(
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = HinvrSideInset),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "HINVR",
                            style = HinvrTypography.titleMedium.copy(
                                fontSize = 18.sp,
                                letterSpacing = 3.sp,
                            ),
                            color = colors.cream,
                            modifier = Modifier.weight(1f),
                        )
                        CircleIconButton(
                            icon = if (member) Icons.Outlined.Wallet else Icons.AutoMirrored.Outlined.Assignment,
                            contentDescription = if (member) "Pass" else "Plans",
                            onClick = { if (member) onOpenPass() else onOpenPlans() },
                        )
                        Spacer(Modifier.padding(start = 8.dp))
                        CircleIconButton(
                            icon = Icons.Outlined.NotificationsNone,
                            contentDescription = "Notifications",
                            onClick = onOpenNotifications,
                        )
                        Spacer(Modifier.padding(start = 8.dp))
                        CircleIconButton(
                            icon = Icons.Outlined.Person,
                            contentDescription = "Profile",
                            onClick = onOpenProfile,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        headline,
                        style = HinvrTypography.headlineLarge.copy(fontSize = 30.sp, lineHeight = 36.sp),
                        color = colors.cream,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        buildString {
                            append(if (snap.displayName.isBlank()) "Namaste" else "Jai Shri Ram, ${snap.displayName}")
                            append("  ·  ")
                            append(
                                if (member) {
                                    "${snap.tier.name} member${snap.validUntilLabel.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()}"
                                } else {
                                    "Not a member yet"
                                },
                            )
                        },
                        style = HinvrTypography.bodyMedium,
                        color = colors.creamMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(28.dp))
                }
            }

            Column(Modifier.padding(horizontal = HinvrSideInset)) {
                AnimatedVisibility(visible = revealContent, enter = homeReveal(0)) {
                    Column {
                        if (live != null) {
                            StatusCard(
                                title = "${live.city} · LIVE",
                                subtitle = live.updatedLabel,
                                icon = Icons.AutoMirrored.Outlined.Assignment,
                                onClick = { onOpenRoute(Destinations.livePlayer(live.id)) },
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            Spacer(Modifier.height(22.dp))
                        }
                    }
                }
                AnimatedVisibility(visible = revealContent, enter = homeReveal(90)) {
                    BentoGrid(
                        tiles = tiles,
                        onOpenPass = onOpenPass,
                        onOpenConcierge = onOpenConcierge,
                        onOpenRoute = onOpenRoute,
                    )
                }
                AnimatedVisibility(visible = revealContent, enter = homeReveal(180)) {
                    Column {
                        Spacer(Modifier.height(18.dp))
                        CustomRequestPill(onClick = onOpenConcierge)
                        Spacer(Modifier.height(32.dp))
                        SectionTitle("Nearby mandirs")
                        Spacer(Modifier.height(14.dp))
                    }
                }
            }

            AnimatedVisibility(visible = revealContent, enter = homeReveal(250)) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = HinvrSideInset),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(mandirs, key = { it.id }) { mandir ->
                        PortraitPhotoCard(
                            title = mandir.name,
                            place = mandir.city,
                            scene = mandir.scene,
                            live = mandir.live,
                            photoUrl = mandir.photoUrl,
                            onClick = { onOpenRoute(Destinations.mandir(mandir.id)) },
                        )
                    }
                }
            }

            AnimatedVisibility(visible = revealContent, enter = homeReveal(330)) {
                Column(Modifier.padding(horizontal = HinvrSideInset)) {
                    Spacer(Modifier.height(36.dp))
                    SectionTitle("Happening in the sabha tonight")
                    Spacer(Modifier.height(14.dp))
                    if (hero != null) {
                        MandirHeroCard(
                            title = hero.name,
                            place = hero.city,
                            scene = hero.scene,
                            live = hero.live,
                            photoUrl = hero.photoUrl,
                            onPhoto = { onOpenRoute(Destinations.mandir(hero.id)) },
                            onLive = { onOpenRoute(Destinations.livePlayer(hero.id)) },
                            onVr = { onOpenRoute(Destinations.vrPlayer(hero.id)) },
                            onPass = onOpenPass,
                            onAssist = onOpenConcierge,
                        )
                        Spacer(Modifier.height(28.dp))
                    }
                    SectionTitle("See it like family, not a tourist")
                    Spacer(Modifier.height(14.dp))
                }
            }
            AnimatedVisibility(visible = revealContent, enter = homeReveal(420)) {
                HostRail()
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

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
        Triple("TIRUPATI", "Ananya Rao", "Knows the calm route for elderly parents."),
        Triple("KASHI", "Raghav Mishra", "Ritual timings, ghats, and what to carry."),
        Triple("SHIRDI", "Meera Patil", "A local hand from arrival to prasad."),
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
                    Text(city, style = HinvrTypography.labelSmall, color = colors.gold)
                    Text(name, style = HinvrTypography.titleLarge, color = colors.ink)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        bio,
                        style = HinvrTypography.bodyMedium,
                        color = colors.inkMuted,
                        modifier = Modifier
                            .border(2.dp, colors.saffron, RoundedCornerShape(1.dp))
                            .padding(start = 10.dp),
                    )
                }
                Box(
                    Modifier
                        .padding(start = 18.dp)
                        .size(54.dp)
                        .background(colors.stoneRaised, CircleShape)
                        .border(2.dp, colors.ivory, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        name.split(" ").mapNotNull { it.firstOrNull() }.joinToString(""),
                        style = HinvrTypography.titleMedium,
                        color = colors.gold,
                    )
                }
            }
        }
    }
}

private fun heroDrawable(scene: TileScene?): Int = when (scene) {
    TileScene.Kashi -> R.drawable.temple_kashi
    TileScene.Shirdi -> R.drawable.temple_shirdi
    TileScene.Kedarnath -> R.drawable.temple_kedarnath
    TileScene.Somnath -> R.drawable.temple_somnath
    else -> R.drawable.temple_tirupati
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
                    if (tile.tall) 228.dp else 158.dp,
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
                    if (tile.tall) 228.dp else 158.dp,
                    { open(tile) },
                    photoUrl = tile.photoUrl,
                )
            }
        }
    }
}
