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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hinvr.app.data.SessionSnapshot
import com.hinvr.app.navigation.LocalSessionRepository
import com.hinvr.app.ui.catalog.HinvrCatalog
import com.hinvr.app.ui.catalog.TileScene
import com.hinvr.app.ui.components.CustomRequestPill
import com.hinvr.app.ui.components.HinvrBackground
import com.hinvr.app.ui.components.HinvrPrimaryButton
import com.hinvr.app.ui.components.PortraitPhotoCard
import com.hinvr.app.ui.components.SabhaTopBar
import com.hinvr.app.ui.components.SectionTitle
import com.hinvr.app.ui.components.SabhaSearchField
import com.hinvr.app.ui.theme.Atmosphere
import com.hinvr.app.ui.theme.HinvrSideInset
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography
import kotlinx.coroutines.launch

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
                Spacer(Modifier.height(18.dp))
                WaitlistForm(kind = "POOJA", defaultCity = null)
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
                Spacer(Modifier.height(18.dp))
                WaitlistForm(kind = "YATRA", defaultCity = null)
            }
        }
    }
}

@Composable
private fun WaitlistForm(kind: String, defaultCity: String?) {
    val session = LocalSessionRepository.current
    val snap by session.snapshot.collectAsStateWithLifecycle(initialValue = SessionSnapshot())
    val scope = rememberCoroutineScope()
    val colors = HinvrTheme.colors
    var city by remember(defaultCity, snap.city) { mutableStateOf(defaultCity ?: snap.city) }
    var submitted by remember { mutableStateOf(false) }

    if (submitted) {
        Text(
            "You’re on the ${kind.lowercase()} desk list. We’ll contact you before this opens.",
            style = HinvrTypography.bodyLarge,
            color = colors.gold,
        )
        return
    }
    Text(
        if (kind == "POOJA") "Bring verified pandits to my city" else "Tell me when the club desk opens",
        style = HinvrTypography.titleMedium,
        color = colors.ink,
    )
    Spacer(Modifier.height(10.dp))
    SabhaSearchField(city, { city = it }, "Your city")
    Spacer(Modifier.height(12.dp))
    HinvrPrimaryButton(
        text = "Join waitlist",
        onClick = {
            scope.launch {
                session.addLocalRequest(kind, city.ifBlank { "City to confirm" })
                submitted = true
            }
        },
    )
}
