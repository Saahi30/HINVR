package com.hinvr.app.ui.concierge

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hinvr.app.data.SessionSnapshot
import com.hinvr.app.navigation.LocalSessionRepository
import com.hinvr.app.ui.catalog.HinvrCatalog
import com.hinvr.app.ui.components.HinvrBackground
import com.hinvr.app.ui.components.IvoryCard
import com.hinvr.app.ui.components.PortraitPhotoCard
import com.hinvr.app.ui.components.SabhaSearchField
import com.hinvr.app.ui.components.SabhaTopBar
import com.hinvr.app.ui.theme.Atmosphere
import com.hinvr.app.ui.theme.HinvrSideInset
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography
import kotlinx.coroutines.launch

@Composable
fun ConciergeScreen(onOpenFaq: (String) -> Unit, onBack: (() -> Unit)? = null) {
    val colors = HinvrTheme.colors
    val session = LocalSessionRepository.current
    val snap by session.snapshot.collectAsStateWithLifecycle(initialValue = SessionSnapshot())
    val scope = rememberCoroutineScope()
    var draft by remember { mutableStateOf("") }
    HinvrBackground(atmosphere = Atmosphere.Sabha) {
        Column(
            Modifier
                .fillMaxSize()
                .then(if (onBack == null) Modifier.statusBarsPadding() else Modifier)
                .imePadding(),
        ) {
            if (onBack != null) {
                SabhaTopBar(onBack = onBack)
            }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = HinvrSideInset),
            ) {
                Spacer(Modifier.height(8.dp))
                Text("CONCIERGE", style = HinvrTypography.labelSmall, color = colors.gold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Ask. Then a human.",
                    style = HinvrTypography.headlineLarge,
                    color = colors.ink,
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.ivory)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text("Specialists 8am–10pm IST", style = HinvrTypography.bodyMedium, color = colors.inkMuted)
                }
                Spacer(Modifier.height(22.dp))
                Text("Quick asks", style = HinvrTypography.titleMedium, color = colors.ink)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HinvrCatalog.faqs.forEach { (id, title) ->
                        Text(
                            title.substringBefore(" —").substringBefore(" at"),
                            style = HinvrTypography.labelLarge,
                            color = colors.ink,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(colors.ivory)
                                .clickable { onOpenFaq(id) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
                Spacer(Modifier.height(22.dp))
                HinvrCatalog.conciergeThread.forEach { block ->
                    Text(
                        block.time,
                        style = HinvrTypography.labelSmall,
                        color = colors.inkMuted,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    if (block.photoTitle != null && block.photoScene != null) {
                        Box {
                            PortraitPhotoCard(
                                title = block.photoTitle,
                                place = block.photoPlace.orEmpty(),
                                scene = block.photoScene,
                                live = false,
                                onClick = { onOpenFaq("tirupati-elderly") },
                                width = null,
                                height = 180.dp,
                            )
                            Box(
                                Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp)
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(colors.dusk)
                                    .clickable { onOpenFaq("tirupati-elderly") },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = colors.cream, modifier = Modifier.size(16.dp))
                            }
                        }
                    } else {
                        IvoryCard {
                            if (block.factTitle != null) {
                                Text(block.factTitle, style = HinvrTypography.titleMedium, color = colors.ink)
                                Spacer(Modifier.height(6.dp))
                            }
                            Text(block.factBody.orEmpty(), style = HinvrTypography.bodyLarge, color = colors.inkMuted)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
                snap.localRequests.filter { it.startsWith("CONCIERGE ·") }.forEach { request ->
                    Text("JUST NOW", style = HinvrTypography.labelSmall, color = colors.inkMuted)
                    Spacer(Modifier.height(8.dp))
                    IvoryCard {
                        Text("Desk request", style = HinvrTypography.titleMedium, color = colors.ink)
                        Spacer(Modifier.height(6.dp))
                        Text(request.substringAfter(" · "), style = HinvrTypography.bodyLarge, color = colors.inkMuted)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Saved locally · ${snap.city.ifBlank { "City to confirm" }}",
                            style = HinvrTypography.labelSmall,
                            color = colors.gold,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = HinvrSideInset, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(colors.ivory)
                        .clickable { },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Attach", tint = colors.ink)
                }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    SabhaSearchField(draft, { draft = it }, "Ask about puja, dates, dress…")
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (draft.isBlank()) colors.ivory else colors.saffron)
                        .clickable(enabled = draft.isNotBlank()) {
                            val request = draft
                            draft = ""
                            scope.launch { session.addLocalRequest("CONCIERGE", request) }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (draft.isBlank()) Icons.Outlined.Mic else Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "Send",
                        tint = if (draft.isBlank()) colors.ink else colors.cream,
                    )
                }
            }
        }
    }
}

@Composable
fun FaqArticleScreen(id: String, onBack: () -> Unit) {
    val colors = HinvrTheme.colors
    val title = HinvrCatalog.faqs.find { it.first == id }?.second ?: "Concierge note"
    val body = when (id) {
        "wear-kashi" -> "Modest clothing, covered shoulders. Leather is often left outside. This is general guidance — follow the notice at the mandir."
        "shraddh" -> "We will not invent a shraaddha procedure. Speak with your kulguru. A specialist can call you on Platinum."
        else -> "Early morning darshan is kinder with elderly parents. Book the buggy at the desk. We will not invent a VIP lane."
    }
    HinvrBackground(atmosphere = Atmosphere.Sabha) {
        Column(Modifier.fillMaxSize().padding(bottom = 24.dp)) {
            com.hinvr.app.ui.components.SabhaTopBar(onBack = onBack)
            Column(Modifier.padding(horizontal = HinvrSideInset)) {
                Text(title, style = HinvrTypography.headlineLarge, color = colors.ink)
                Spacer(Modifier.height(16.dp))
                IvoryCard {
                    Text(body, style = HinvrTypography.bodyLarge, color = colors.ink)
                }
            }
        }
    }
}
