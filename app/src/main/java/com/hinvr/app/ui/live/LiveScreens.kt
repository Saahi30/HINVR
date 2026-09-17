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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hinvr.app.navigation.LocalCatalogRepository
import com.hinvr.app.navigation.LocalSessionRepository
import com.hinvr.app.data.MembershipTier
import com.hinvr.app.data.SessionSnapshot
import com.hinvr.app.ui.components.HinvrBackground
import com.hinvr.app.ui.components.IvoryCard
import com.hinvr.app.ui.components.LivePill
import com.hinvr.app.ui.components.PortraitPhotoCard
import com.hinvr.app.ui.components.SabhaTopBar
import com.hinvr.app.ui.components.SectionTitle
import com.hinvr.app.ui.components.HinvrPrimaryButton
import com.hinvr.app.ui.illustrations.TempleScene
import com.hinvr.app.ui.media.StreamPane
import com.hinvr.app.ui.theme.Atmosphere
import com.hinvr.app.ui.theme.HinvrSideInset
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography

@Composable
fun LiveListScreen(onBack: () -> Unit, onOpenPlayer: (String) -> Unit) {
    val colors = HinvrTheme.colors
    val catalog = LocalCatalogRepository.current
    val mandirs by catalog.mandirs.collectAsStateWithLifecycle()
    val live = mandirs.filter { it.live }
    val upcoming = mandirs.filter { !it.live && it.nextAarti != null }
    HinvrBackground(atmosphere = Atmosphere.Sabha) {
        Column(Modifier.fillMaxSize()) {
            SabhaTopBar(onBack = onBack)
            LazyColumn(
                contentPadding = PaddingValues(horizontal = HinvrSideInset, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { SectionTitle("Sit in the sabha from anywhere") }
                item {
                    Text(
                        "Official temple streams only.",
                        style = HinvrTypography.bodyLarge,
                        color = colors.inkMuted,
                    )
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
                        Text("NEXT AARTI", style = HinvrTypography.labelSmall, color = colors.gold)
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
fun LivePlayerScreen(id: String, onBack: () -> Unit, onPlans: () -> Unit = {}) {
    val catalog = LocalCatalogRepository.current
    val mandirs by catalog.mandirs.collectAsStateWithLifecycle()
    val mandir = remember(id, mandirs) { catalog.mandir(id) }
    val snap by LocalSessionRepository.current.snapshot.collectAsStateWithLifecycle(initialValue = SessionSnapshot())
    val isMember = snap.tier != MembershipTier.None
    val colors = HinvrTheme.colors
    val stream = mandir.liveUrl
    HinvrBackground(atmosphere = Atmosphere.Sanctum) {
        Box(Modifier.fillMaxSize()) {
            if (stream.isNotBlank()) {
                Column(Modifier.fillMaxSize().navigationBarsPadding()) {
                    SabhaTopBar(onBack = onBack, onPhoto = true)
                    LivePill(Modifier.padding(horizontal = HinvrSideInset), label = "OFFICIAL STREAM")
                    Spacer(Modifier.height(12.dp))
                    StreamPane(
                        url = stream,
                        modifier = Modifier
                            .padding(horizontal = HinvrSideInset)
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp)),
                    )
                    Column(Modifier.padding(HinvrSideInset)) {
                        Text(mandir.name, style = HinvrTypography.headlineMedium, color = colors.cream)
                        Text(
                            "The feed is the temple’s, not ours.",
                            style = HinvrTypography.bodyMedium,
                            color = colors.creamMuted,
                        )
                        if (!isMember) {
                            Spacer(Modifier.height(12.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                                    .background(colors.ivory)
                                    .padding(14.dp),
                            ) {
                                Column {
                                    Text("Darshan stays open.", style = HinvrTypography.titleMedium, color = colors.ink)
                                    Text("Join for the pass and visit desk.", style = HinvrTypography.bodyMedium, color = colors.inkMuted)
                                    Spacer(Modifier.height(10.dp))
                                    HinvrPrimaryButton("Join the club", onPlans)
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            } else {
                TempleScene(mandir.scene, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Color(0x66100B08)))
                Column(
                    Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                ) {
                    SabhaTopBar(onBack = onBack, onPhoto = true)
                    Spacer(Modifier.height(8.dp))
                    LivePill(Modifier.padding(horizontal = HinvrSideInset), label = "OFFICIAL STREAM")
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(colors.dusk.copy(alpha = 0.55f))
                            .padding(18.dp),
                    ) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = "Play", tint = colors.cream)
                    }
                    Spacer(Modifier.weight(1f))
                    Column(Modifier.padding(HinvrSideInset)) {
                        Text(mandir.name, style = HinvrTypography.headlineMedium, color = colors.cream)
                        Text(
                            "The feed is the temple’s, not ours. It is not broadcasting right now.",
                            style = HinvrTypography.bodyMedium,
                            color = colors.creamMuted,
                        )
                        if (!isMember) {
                            Spacer(Modifier.height(12.dp))
                            HinvrPrimaryButton("Join for the pass and visit desk", onPlans)
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
