package com.hinvr.app.ui.pass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hinvr.app.data.SessionSnapshot
import com.hinvr.app.navigation.LocalCatalogRepository
import com.hinvr.app.navigation.LocalSessionRepository
import com.hinvr.app.ui.components.FilterChip
import com.hinvr.app.ui.components.HinvrBackground
import com.hinvr.app.ui.components.HinvrPrimaryButton
import com.hinvr.app.ui.components.IvoryCard
import com.hinvr.app.ui.components.SabhaSearchField
import com.hinvr.app.ui.components.SabhaTopBar
import com.hinvr.app.ui.theme.Atmosphere
import com.hinvr.app.ui.theme.HinvrSideInset
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography
import kotlinx.coroutines.launch

@Composable
fun PlanVisitScreen(onBack: () -> Unit) {
    val catalog = LocalCatalogRepository.current
    val session = LocalSessionRepository.current
    val mandirs by catalog.mandirs.collectAsStateWithLifecycle()
    val snap by session.snapshot.collectAsStateWithLifecycle(initialValue = SessionSnapshot())
    val scope = rememberCoroutineScope()
    val colors = HinvrTheme.colors
    var mandirId by remember(mandirs) {
        mutableStateOf(mandirs.firstOrNull { it.passAccepted }?.id.orEmpty())
    }
    var date by remember { mutableStateOf("") }
    var partySize by remember { mutableIntStateOf(2) }
    var notes by remember { mutableStateOf("") }
    var assist by remember { mutableStateOf(setOf<String>()) }
    var submitted by remember { mutableStateOf(false) }

    HinvrBackground(atmosphere = Atmosphere.Sabha) {
        Column(Modifier.fillMaxSize()) {
            SabhaTopBar(title = "Plan a visit", onBack = onBack)
            Column(Modifier.padding(horizontal = HinvrSideInset)) {
                if (submitted) {
                    Spacer(Modifier.height(32.dp))
                    Text("The desk has your request.", style = HinvrTypography.headlineLarge, color = colors.ink)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "We’ll call before your visit. Official entry and assist depend on the mandir.",
                        style = HinvrTypography.bodyLarge,
                        color = colors.inkMuted,
                    )
                    Spacer(Modifier.height(22.dp))
                    HinvrPrimaryButton("Back to pass", onBack)
                    return@Column
                }

                Text(
                    "Tell the desk who is travelling.",
                    style = HinvrTypography.headlineMedium,
                    color = colors.ink,
                )
                Spacer(Modifier.height(16.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(mandirs.filter { it.passAccepted }, key = { it.id }) { mandir ->
                        FilterChip(
                            mandir.city,
                            selected = mandirId == mandir.id,
                            onClick = { mandirId = mandir.id },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                SabhaSearchField(date, { date = it }, "Visit date · DD/MM/YYYY")
                Spacer(Modifier.height(14.dp))
                IvoryCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Party size", style = HinvrTypography.titleMedium, color = colors.ink, modifier = Modifier.weight(1f))
                        Text("−", style = HinvrTypography.headlineMedium, color = colors.gold, modifier = Modifier.clickable { partySize = (partySize - 1).coerceAtLeast(1) }.padding(10.dp))
                        Text("$partySize", style = HinvrTypography.titleLarge, color = colors.ink)
                        Text("+", style = HinvrTypography.headlineMedium, color = colors.gold, modifier = Modifier.clickable { partySize = (partySize + 1).coerceAtMost(12) }.padding(10.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Assist needed", style = HinvrTypography.titleMedium, color = colors.ink)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("Buggy", "Wheelchair", "Prasad", "Local host")) { item ->
                        FilterChip(
                            item,
                            selected = item in assist,
                            onClick = {
                                assist = if (item in assist) assist - item else assist + item
                            },
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                SabhaSearchField(notes, { notes = it }, "Anything the host should know?")
                Spacer(Modifier.height(20.dp))
                HinvrPrimaryButton(
                    text = "Send visit request",
                    onClick = {
                        val mandir = mandirs.find { it.id == mandirId }?.name ?: mandirId
                        scope.launch {
                            session.addLocalRequest(
                                "VISIT",
                                "$mandir · ${date.ifBlank { "date to confirm" }} · $partySize people · ${assist.ifEmpty { setOf("No assist selected") }.joinToString()} · ${snap.city} · $notes",
                            )
                            submitted = true
                        }
                    },
                )
            }
        }
    }
}
