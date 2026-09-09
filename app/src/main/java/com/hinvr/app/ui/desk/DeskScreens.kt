package com.hinvr.app.ui.desk

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hinvr.app.ui.catalog.HinvrCatalog
import com.hinvr.app.ui.catalog.TileScene
import com.hinvr.app.ui.components.CustomRequestPill
import com.hinvr.app.ui.components.HinvrBackground
import com.hinvr.app.ui.components.PortraitPhotoCard
import com.hinvr.app.ui.components.SabhaTopBar
import com.hinvr.app.ui.components.SectionTitle
import com.hinvr.app.ui.theme.Atmosphere
import com.hinvr.app.ui.theme.HinvrSideInset
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography

@Composable
fun PoojaScreen(onBack: () -> Unit, onConcierge: () -> Unit) {
    val colors = HinvrTheme.colors
    HinvrBackground(atmosphere = Atmosphere.Sabha) {
        Column(Modifier.fillMaxSize()) {
            SabhaTopBar(onBack = onBack)
            Column(Modifier.padding(horizontal = HinvrSideInset)) {
                SectionTitle("A pandit at the door.")
                Spacer(Modifier.height(8.dp))
                Text(
                    "Coming to your city. Join the waitlist — we won’t invent pandits.",
                    style = HinvrTypography.bodyLarge,
                    color = colors.inkMuted,
                )
                Spacer(Modifier.height(18.dp))
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = HinvrSideInset),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(HinvrCatalog.mandirs.take(3)) { row ->
                    PortraitPhotoCard(
                        title = "Verified in ${row.city}",
                        place = "Waitlist",
                        scene = TileScene.PanditDoor,
                        live = false,
                        onClick = onConcierge,
                    )
                }
            }
            Column(Modifier.padding(HinvrSideInset)) {
                Spacer(Modifier.height(20.dp))
                CustomRequestPill(onClick = onConcierge)
            }
        }
    }
}

@Composable
fun YatraScreen(onBack: () -> Unit, onConcierge: () -> Unit) {
    val colors = HinvrTheme.colors
    HinvrBackground(atmosphere = Atmosphere.Sabha) {
        Column(Modifier.fillMaxSize()) {
            SabhaTopBar(onBack = onBack)
            Column(Modifier.padding(horizontal = HinvrSideInset)) {
                SectionTitle("Club desk, not a portal.")
                Spacer(Modifier.height(8.dp))
                Text(
                    "Enquire first. We arrange the host, not a packaged tour dump.",
                    style = HinvrTypography.bodyLarge,
                    color = colors.inkMuted,
                )
                Spacer(Modifier.height(18.dp))
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = HinvrSideInset),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(HinvrCatalog.mandirs) { row ->
                    PortraitPhotoCard(
                        title = row.name,
                        place = row.city,
                        scene = if (row.id == "kedarnath") TileScene.YatraRoad else row.scene,
                        live = false,
                        onClick = onConcierge,
                    )
                }
            }
            Column(Modifier.padding(HinvrSideInset)) {
                Spacer(Modifier.height(20.dp))
                CustomRequestPill(onClick = onConcierge)
            }
        }
    }
}
