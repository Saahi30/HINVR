package com.hinvr.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hinvr.app.R
import com.hinvr.app.data.MembershipTier
import com.hinvr.app.data.SessionSnapshot
import com.hinvr.app.navigation.Destinations
import com.hinvr.app.navigation.LocalSessionRepository
import com.hinvr.app.ui.catalog.HinvrCatalog
import com.hinvr.app.ui.catalog.ServiceTile
import com.hinvr.app.ui.components.BentoTile
import com.hinvr.app.ui.components.CircleIconButton
import com.hinvr.app.ui.components.CustomRequestPill
import com.hinvr.app.ui.components.HinvrBackground
import com.hinvr.app.ui.components.MandirHeroCard
import com.hinvr.app.ui.components.PortraitPhotoCard
import com.hinvr.app.ui.components.SectionTitle
import com.hinvr.app.ui.components.StatusCard
import com.hinvr.app.ui.theme.Atmosphere
import com.hinvr.app.ui.theme.HinvrSideInset
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography

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
    val snap by session.snapshot.collectAsStateWithLifecycle(initialValue = SessionSnapshot())
    val colors = HinvrTheme.colors
    val live = HinvrCatalog.liveNow
    val member = snap.tier != MembershipTier.None
    val tiles = HinvrCatalog.services

    HinvrBackground(atmosphere = Atmosphere.Sabha, darkIcons = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Box(Modifier.fillMaxWidth().height(372.dp)) {
                Image(
                    painter = painterResource(R.drawable.splash_background),
                    contentDescription = null,
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
                            icon = Icons.Outlined.Person,
                            contentDescription = "Profile",
                            onClick = onOpenProfile,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "You see the aarti.\nThe internet sees a thumbnail.",
                        style = HinvrTypography.headlineLarge.copy(fontSize = 30.sp, lineHeight = 36.sp),
                        color = colors.cream,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(28.dp))
                }
            }

            Column(Modifier.padding(horizontal = HinvrSideInset)) {
                StatusCard(
                    title = "${live.city} · LIVE",
                    subtitle = live.updatedLabel,
                    icon = Icons.AutoMirrored.Outlined.Assignment,
                    onClick = { onOpenRoute(Destinations.livePlayer(live.id)) },
                    modifier = Modifier.padding(top = 4.dp),
                )
                Spacer(Modifier.height(22.dp))
                BentoGrid(
                    tiles = tiles,
                    onOpenPass = onOpenPass,
                    onOpenConcierge = onOpenConcierge,
                    onOpenRoute = onOpenRoute,
                )
                Spacer(Modifier.height(18.dp))
                CustomRequestPill(onClick = onOpenConcierge)
                Spacer(Modifier.height(32.dp))
                SectionTitle("Nearby mandirs")
                Spacer(Modifier.height(14.dp))
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = HinvrSideInset),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(HinvrCatalog.mandirs, key = { it.id }) { mandir ->
                    PortraitPhotoCard(
                        title = mandir.name,
                        place = mandir.city,
                        scene = mandir.scene,
                        live = mandir.live,
                        onClick = { onOpenRoute(Destinations.mandir(mandir.id)) },
                    )
                }
            }

            Column(Modifier.padding(horizontal = HinvrSideInset)) {
                Spacer(Modifier.height(36.dp))
                SectionTitle("Happening in the sabha tonight")
                Spacer(Modifier.height(14.dp))
                MandirHeroCard(
                    title = live.name,
                    place = live.city,
                    scene = live.scene,
                    live = true,
                    onPhoto = { onOpenRoute(Destinations.mandir(live.id)) },
                    onLive = { onOpenRoute(Destinations.livePlayer(live.id)) },
                    onVr = { onOpenRoute(Destinations.vrPlayer(live.id)) },
                    onPass = onOpenPass,
                    onAssist = onOpenConcierge,
                )
                Spacer(Modifier.height(28.dp))
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
    val live = tiles.first { it.route == Destinations.Live }
    val vr = tiles.first { it.route == Destinations.Vr }
    val pass = tiles.first { it.route == Destinations.Pass }
    val pandit = tiles.first { it.route == Destinations.Pooja }
    val concierge = tiles.first { it.route == Destinations.Concierge }
    val yatra = tiles.first { it.route == Destinations.Yatra }

    fun open(tile: ServiceTile) {
        when (tile.route) {
            Destinations.Pass -> onOpenPass()
            Destinations.Concierge -> onOpenConcierge()
            else -> onOpenRoute(tile.route)
        }
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BentoTile(live.title, live.benefit, live.scene, 228.dp, { open(live) })
            BentoTile(pandit.title, pandit.benefit, pandit.scene, 158.dp, { open(pandit) })
            BentoTile(concierge.title, concierge.benefit, concierge.scene, 158.dp, { open(concierge) })
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BentoTile(vr.title, vr.benefit, vr.scene, 158.dp, { open(vr) })
            BentoTile(pass.title, pass.benefit, pass.scene, 158.dp, { open(pass) })
            BentoTile(yatra.title, yatra.benefit, yatra.scene, 228.dp, { open(yatra) })
        }
    }
}
