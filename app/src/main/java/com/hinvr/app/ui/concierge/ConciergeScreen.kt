package com.hinvr.app.ui.concierge

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hinvr.app.R
import com.hinvr.app.navigation.LocalSessionRepository
import com.hinvr.app.ui.components.DeskRequestCard
import com.hinvr.app.ui.components.HinvrBackground
import com.hinvr.app.ui.components.IvoryCard
import com.hinvr.app.ui.components.LocalDockClearance
import com.hinvr.app.ui.components.SabhaSearchField
import com.hinvr.app.ui.components.SabhaTopBar
import com.hinvr.app.ui.theme.Atmosphere
import com.hinvr.app.ui.theme.HinvrSideInset
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography
import kotlinx.coroutines.launch

/**
 * The desk, not a chatbot. The thread is only what this member actually sent, each
 * with its real time and whether the desk has it. Nothing here is a sample.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConciergeScreen(onOpenFaq: (String) -> Unit, onBack: (() -> Unit)? = null) {
    val colors = HinvrTheme.colors
    val session = LocalSessionRepository.current
    val snap by session.snapshot.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var draft by rememberSaveable { mutableStateOf("") }
    // The keyboard already covers the dock, so only clear it while the keyboard is down.
    val imeOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val dockClearance = if (imeOpen) 0.dp else LocalDockClearance.current
    val faqChips = listOf(
        "wear-kashi" to stringResource(R.string.faq_wear_chip),
        "shraddh" to stringResource(R.string.faq_shraddh_chip),
        "tirupati-elderly" to stringResource(R.string.faq_tirupati_chip),
    )
    val requests = snap.deskRequests
    val sendLabel = stringResource(R.string.cd_send)
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
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    faqChips.forEach { (id, label) ->
                        Text(
                            label,
                            style = HinvrTypography.labelLarge,
                            color = colors.ink,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(colors.ivory)
                                .clickable(role = Role.Button) { onOpenFaq(id) }
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                        )
                    }
                }
                Spacer(Modifier.height(22.dp))
                // A standing note from the desk, not a message: no timestamp.
                IvoryCard {
                    Text(stringResource(R.string.thread_hours_title), style = HinvrTypography.titleMedium, color = colors.ink)
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.thread_hours_body), style = HinvrTypography.bodyLarge, color = colors.inkMuted)
                }
                Spacer(Modifier.height(26.dp))
                Text(stringResource(R.string.your_requests), style = HinvrTypography.titleMedium, color = colors.ink)
                Spacer(Modifier.height(10.dp))
                if (requests.isEmpty()) {
                    Text(
                        stringResource(R.string.your_requests_empty),
                        style = HinvrTypography.bodyMedium,
                        color = colors.inkMuted,
                    )
                    Spacer(Modifier.height(14.dp))
                }
                requests.forEach { request ->
                    DeskRequestCard(
                        request = request,
                        onRetry = { scope.launch { session.retryDeskRequest(request.id) } },
                    )
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
                Box(Modifier.weight(1f)) {
                    SabhaSearchField(draft, { draft = it }, stringResource(R.string.ask_hint))
                }
                Spacer(Modifier.width(8.dp))
                val canSend = draft.isNotBlank()
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (canSend) colors.saffron else colors.ivory)
                        .clickable(enabled = canSend, role = Role.Button) {
                            val request = draft
                            draft = ""
                            scope.launch { session.submitDeskRequest("CONCIERGE", request) }
                        }
                        .semantics { contentDescription = sendLabel },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Send,
                        contentDescription = null,
                        tint = if (canSend) colors.cream else colors.inkMuted,
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
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.faq_family_custom), style = HinvrTypography.bodyMedium, color = colors.inkMuted)
            }
        }
    }
}
