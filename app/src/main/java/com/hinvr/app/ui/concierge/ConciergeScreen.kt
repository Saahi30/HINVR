package com.hinvr.app.ui.concierge

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
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
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hinvr.app.R
import com.hinvr.app.data.SessionSnapshot
import com.hinvr.app.navigation.LocalSessionRepository
import com.hinvr.app.ui.catalog.HinvrCatalog
import com.hinvr.app.ui.components.HinvrBackground
import com.hinvr.app.ui.components.IvoryCard
import com.hinvr.app.ui.components.LocalDockClearance
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
    // The keyboard already covers the dock, so only clear it while the keyboard is down.
    val imeOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val dockClearance = if (imeOpen) 0.dp else LocalDockClearance.current
    val faqChips = listOf(
        "wear-kashi" to stringResource(R.string.faq_wear_chip),
        "shraddh" to stringResource(R.string.faq_shraddh_chip),
        "tirupati-elderly" to stringResource(R.string.faq_tirupati_chip),
    )
    val cityFallback = stringResource(R.string.city_confirm)
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
                Text(stringResource(R.string.concierge_kicker), style = HinvrTypography.labelSmall, color = colors.gold)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.concierge_title),
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
                    Text(stringResource(R.string.concierge_hours), style = HinvrTypography.bodyMedium, color = colors.inkMuted)
                }
                Spacer(Modifier.height(22.dp))
                Text(stringResource(R.string.quick_asks), style = HinvrTypography.titleMedium, color = colors.ink)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    faqChips.forEach { (id, label) ->
                        Text(
                            label,
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
                    val time = when (block.id) {
                        "1" -> stringResource(R.string.thread_yesterday)
                        else -> block.time
                    }
                    Text(
                        time,
                        style = HinvrTypography.labelSmall,
                        color = colors.inkMuted,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    if (block.photoTitle != null && block.photoScene != null) {
                        Box {
                            PortraitPhotoCard(
                                title = stringResource(R.string.thread_photo_title),
                                place = stringResource(R.string.thread_photo_place),
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
                            val factTitle = when (block.id) {
                                "1" -> stringResource(R.string.thread_hours_title)
                                "3" -> stringResource(R.string.thread_kashi_title)
                                else -> block.factTitle
                            }
                            val factBody = when (block.id) {
                                "1" -> stringResource(R.string.thread_hours_body)
                                "3" -> stringResource(R.string.thread_kashi_body)
                                else -> block.factBody.orEmpty()
                            }
                            if (factTitle != null) {
                                Text(factTitle, style = HinvrTypography.titleMedium, color = colors.ink)
                                Spacer(Modifier.height(6.dp))
                            }
                            Text(factBody, style = HinvrTypography.bodyLarge, color = colors.inkMuted)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
                snap.localRequests.filter { it.startsWith("CONCIERGE ·") }.forEach { request ->
                    Text(stringResource(R.string.just_now), style = HinvrTypography.labelSmall, color = colors.inkMuted)
                    Spacer(Modifier.height(8.dp))
                    IvoryCard {
                        Text(stringResource(R.string.desk_request), style = HinvrTypography.titleMedium, color = colors.ink)
                        Spacer(Modifier.height(6.dp))
                        Text(request.substringAfter(" · "), style = HinvrTypography.bodyLarge, color = colors.inkMuted)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (session.configured) {
                                stringResource(R.string.sent_desk, snap.city.ifBlank { cityFallback })
                            } else {
                                stringResource(R.string.saved_local, snap.city.ifBlank { cityFallback })
                            },
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
                    .padding(horizontal = HinvrSideInset, vertical = 10.dp)
                    .padding(bottom = dockClearance),
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
                    Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.cd_attach), tint = colors.ink)
                }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    SabhaSearchField(draft, { draft = it }, stringResource(R.string.ask_hint))
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
                        contentDescription = stringResource(R.string.cd_send),
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
    val title = when (id) {
        "wear-kashi" -> stringResource(R.string.faq_wear_title)
        "shraddh" -> stringResource(R.string.faq_shraddh_title)
        "tirupati-elderly" -> stringResource(R.string.faq_tirupati_title)
        else -> stringResource(R.string.faq_fallback)
    }
    val body = when (id) {
        "wear-kashi" -> stringResource(R.string.faq_wear_body)
        "shraddh" -> stringResource(R.string.faq_shraddh_body)
        else -> stringResource(R.string.faq_tirupati_body)
    }
    HinvrBackground(atmosphere = Atmosphere.Sabha) {
        Column(Modifier.fillMaxSize().padding(bottom = 24.dp)) {
            SabhaTopBar(onBack = onBack)
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
