package com.hinvr.app.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import com.hinvr.app.ui.components.HinvrBackground
import com.hinvr.app.ui.components.IvoryCard
import com.hinvr.app.ui.components.HinvrPrimaryButton
import com.hinvr.app.ui.components.SabhaSearchField
import com.hinvr.app.ui.components.SabhaTopBar
import com.hinvr.app.ui.theme.Atmosphere
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLegal: () -> Unit,
    onPlans: () -> Unit,
    onSignedOut: () -> Unit,
) {
    val session = LocalSessionRepository.current
    val snap by session.snapshot.collectAsStateWithLifecycle(initialValue = SessionSnapshot())
    val scope = rememberCoroutineScope()
    val colors = HinvrTheme.colors
    var editing by remember { mutableStateOf(false) }
    var name by remember(snap.displayName) { mutableStateOf(snap.displayName) }
    var city by remember(snap.city) { mutableStateOf(snap.city) }

    HinvrBackground(atmosphere = Atmosphere.Sabha) {
        Column(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            SabhaTopBar(onBack = onBack)
            Column(Modifier.padding(horizontal = 22.dp)) {
                Text(snap.displayName.ifBlank { "Member" }, style = HinvrTypography.headlineLarge, color = colors.ink)
                Text(snap.phoneE164, style = HinvrTypography.bodyMedium, color = colors.inkMuted)
                Text("${snap.city} · ${snap.tier.name}", style = HinvrTypography.bodyMedium, color = colors.gold)
                if (snap.memberId.isNotBlank()) {
                    Text(
                        "${snap.memberId} · ${snap.validUntilLabel}",
                        style = HinvrTypography.bodyMedium,
                        color = colors.inkMuted,
                    )
                }
                Spacer(Modifier.height(22.dp))
                if (editing) {
                    SabhaSearchField(name, { name = it }, "Member name")
                    Spacer(Modifier.height(10.dp))
                    SabhaSearchField(city, { city = it }, "City")
                    Spacer(Modifier.height(12.dp))
                    HinvrPrimaryButton(
                        "Save profile",
                        onClick = {
                            scope.launch {
                                session.completeProfile(name, city, snap.languageTag, snap.audience)
                                editing = false
                            }
                        },
                    )
                    Spacer(Modifier.height(16.dp))
                }
                IvoryCard {
                    Text(
                        if (editing) "Cancel editing" else "Edit name and city",
                        style = HinvrTypography.titleMedium,
                        color = colors.ink,
                        modifier = Modifier.clickable { editing = !editing }.padding(vertical = 6.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Membership",
                        style = HinvrTypography.titleMedium,
                        color = colors.ink,
                        modifier = Modifier.clickable(onClick = onPlans).padding(vertical = 6.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Legal",
                        style = HinvrTypography.titleMedium,
                        color = colors.ink,
                        modifier = Modifier.clickable(onClick = onLegal).padding(vertical = 6.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Sign out",
                    style = HinvrTypography.titleMedium,
                    color = colors.vermillion,
                    modifier = Modifier
                        .clickable {
                            scope.launch {
                                session.signOut()
                                onSignedOut()
                            }
                        }
                        .padding(vertical = 14.dp),
                )
            }
        }
    }
}

@Composable
fun LegalScreen(onBack: () -> Unit) {
    val colors = HinvrTheme.colors
    HinvrBackground(atmosphere = Atmosphere.Sabha) {
        Column(Modifier.fillMaxSize()) {
            SabhaTopBar(title = "Legal", onBack = onBack)
            Column(Modifier.padding(horizontal = 22.dp)) {
                IvoryCard {
                    Text(
                        "Terms, privacy, refunds.\nHINVR is not a temple board. Streams are attributed to the temple.",
                        style = HinvrTypography.bodyLarge,
                        color = colors.ink,
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val colors = HinvrTheme.colors
    HinvrBackground(atmosphere = Atmosphere.Sabha) {
        Column(Modifier.fillMaxSize()) {
            SabhaTopBar(title = "Notifications", onBack = onBack)
            Column(Modifier.padding(horizontal = 22.dp, vertical = 24.dp)) {
                Text("We will remind you before aarti.", style = HinvrTypography.headlineMedium, color = colors.ink)
                Spacer(Modifier.height(8.dp))
                Text("Nothing waiting right now.", style = HinvrTypography.bodyLarge, color = colors.inkMuted)
            }
        }
    }
}
