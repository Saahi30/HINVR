package com.hinvr.app.ui.mandirs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hinvr.app.navigation.LocalCatalogRepository
import com.hinvr.app.ui.components.FilterChip
import com.hinvr.app.ui.components.HinvrBackground
import com.hinvr.app.ui.components.HinvrPrimaryButton
import com.hinvr.app.ui.components.IvoryCard
import com.hinvr.app.ui.components.MandirHeroCard
import com.hinvr.app.ui.components.PortraitPhotoCard
import com.hinvr.app.ui.components.SabhaSearchField
import com.hinvr.app.ui.components.SabhaTopBar
import com.hinvr.app.ui.components.SectionTitle
import com.hinvr.app.ui.theme.Atmosphere
import com.hinvr.app.ui.theme.HinvrSideInset
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography

@Composable
fun MandirsScreen(onOpenTemple: (String) -> Unit) {
    val colors = HinvrTheme.colors
    val catalog = LocalCatalogRepository.current
    val mandirs by catalog.mandirs.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var chip by remember { mutableStateOf<String?>(null) }
    val chips = listOf("Live", "VR", "Pass accepted", "Nearby")
    val filtered = mandirs.filter { row ->
        val q = query.isBlank() || row.name.contains(query, true) || row.place.contains(query, true)
        val c = when (chip) {
            "Live" -> row.live
            "VR" -> row.vr
            "Pass accepted" -> row.passAccepted
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
            SectionTitle("Mandirs")
            Spacer(Modifier.height(14.dp))
            SabhaSearchField(query, { query = it }, "Kashi, Balaji, your kuldevi…")
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(chips) { label ->
                    FilterChip(label, selected = chip == label, onClick = { chip = if (chip == label) null else label })
                }
            }
            Spacer(Modifier.height(16.dp))
            if (filtered.isEmpty()) {
                Text(
                    "No mandir by that name.",
                    style = HinvrTypography.headlineMedium,
                    color = colors.ink,
                    modifier = Modifier.padding(top = 32.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Ask Concierge — we will add it.",
                    style = HinvrTypography.bodyLarge,
                    color = colors.inkMuted,
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    items(filtered, key = { it.id }) { row ->
                        PortraitPhotoCard(
                            title = row.name,
                            place = row.city,
                            scene = row.scene,
                            live = row.live,
                            photoUrl = row.photoUrl,
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
) {
    val catalog = LocalCatalogRepository.current
    val mandirs by catalog.mandirs.collectAsStateWithLifecycle()
    val mandir = remember(id, mandirs) { catalog.mandir(id) }
    val colors = HinvrTheme.colors
    HinvrBackground(atmosphere = Atmosphere.Sabha, darkIcons = false) {
        Column(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            SabhaTopBar(onBack = onBack, onPhoto = true)
            Column(Modifier.padding(horizontal = HinvrSideInset)) {
                MandirHeroCard(
                    title = mandir.name,
                    place = mandir.place,
                    scene = mandir.scene,
                    live = mandir.live,
                    photoUrl = mandir.photoUrl,
                    onPhoto = {},
                    onLive = onLive,
                    onVr = onVr,
                    onPass = onPass,
                    onAssist = onAssist,
                )
                Spacer(Modifier.height(22.dp))
                Text("Official timings", style = HinvrTypography.titleMedium, color = colors.ink)
                Spacer(Modifier.height(8.dp))
                IvoryCard {
                    Text(mandir.timings, style = HinvrTypography.bodyLarge, color = colors.ink)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "The feed is the temple’s, not ours. Never a fake stream.",
                        style = HinvrTypography.bodyMedium,
                        color = colors.inkMuted,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (mandir.live) Badge("LIVE")
                    if (mandir.vr) Badge("VR")
                    if (mandir.passAccepted) Badge("PASS")
                }
                Spacer(Modifier.height(24.dp))
                HinvrPrimaryButton("Watch live darshan", onLive)
            }
        }
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
