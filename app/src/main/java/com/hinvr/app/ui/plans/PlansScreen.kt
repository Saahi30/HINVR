package com.hinvr.app.ui.plans

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hinvr.app.data.MembershipTier
import com.hinvr.app.data.SessionSnapshot
import com.hinvr.app.navigation.LocalSessionRepository
import com.hinvr.app.ui.components.HinvrBackground
import com.hinvr.app.ui.components.HinvrPrimaryButton
import com.hinvr.app.ui.components.HinvrTextButton
import com.hinvr.app.ui.components.IvoryCard
import com.hinvr.app.ui.components.SabhaTopBar
import com.hinvr.app.ui.theme.Atmosphere
import com.hinvr.app.ui.theme.HinvrCardRadius
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography
import com.hinvr.app.ui.motion.HinvrMotion
import kotlinx.coroutines.launch

private data class PlanCard(
    val tier: MembershipTier,
    val name: String,
    val price: String,
    val amountInr: Int,
    val audience: String,
    val benefits: List<String>,
    val invoiceNote: String? = null,
    val recommended: Boolean = false,
)

@Composable
fun PlansScreen(onBack: () -> Unit) {
    val session = LocalSessionRepository.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snap by session.snapshot.collectAsStateWithLifecycle(initialValue = SessionSnapshot())
    var opening by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var browserError by remember { mutableStateOf<String?>(null) }
    var requestError by remember { mutableStateOf<String?>(null) }
    val colors = HinvrTheme.colors
    val plans = listOf(
        PlanCard(
            MembershipTier.Darshan,
            "Darshan",
            "₹999",
            999,
            "For watching and planning",
            listOf("Official live darshan", "Mandir directory and favourites", "Basic concierge requests"),
        ),
        PlanCard(
            MembershipTier.Gold,
            "Gold",
            "₹4,999",
            4999,
            "For parents and regular visits",
            listOf("Everything in Darshan", "Digital QR temple pass", "Visit and accessibility requests", "Priority concierge chat"),
            recommended = true,
        ),
        PlanCard(
            MembershipTier.Platinum,
            "Platinum",
            "₹14,999",
            14999,
            "For families needing a human desk",
            listOf("Everything in Gold", "Phone concierge", "Family profile support", "Physical card request", "Partner assist priority"),
        ),
        PlanCard(
            MembershipTier.Nri,
            "NRI",
            "$149",
            12499,
            "For family abroad",
            listOf("Everything in Platinum", "International support hours"),
            invoiceNote = "Desk invoice ₹12,499",
        ),
    )
    var selectedTier by remember {
        mutableStateOf(
            snap.tier.takeIf { tier -> plans.any { it.tier == tier } } ?: MembershipTier.Gold,
        )
    }
    LaunchedEffect(Unit) {
        session.syncRemote()
    }
    LaunchedEffect(snap.tier) {
        if (snap.tier != MembershipTier.None && plans.any { it.tier == snap.tier }) {
            selectedTier = snap.tier
        }
    }
    HinvrBackground(atmosphere = Atmosphere.Sabha) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            SabhaTopBar(title = "Membership", onBack = onBack)
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 22.dp,
                    end = 22.dp,
                    bottom = 20.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text("ONE YEAR. ONE DESK.", style = HinvrTypography.labelSmall, color = colors.gold)
                    Spacer(Modifier.height(8.dp))
                    Text("Choose how much help your family needs.", style = HinvrTypography.headlineLarge, color = colors.ink)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Live darshan stays simple. Higher plans add the pass and a human who can help plan the visit.",
                        style = HinvrTypography.bodyLarge,
                        color = colors.inkMuted,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                items(plans.size, key = { plans[it].tier.name }) { index ->
                    val plan = plans[index]
                    PlanOptionCard(
                        plan = plan,
                        selected = selectedTier == plan.tier,
                        current = snap.tier == plan.tier,
                        onSelect = { selectedTier = plan.tier },
                    )
                }
                item {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Partner access depends on the mandir and confirmed services. We never sell unofficial queue-jumping.",
                        style = HinvrTypography.bodyMedium,
                        color = colors.inkMuted,
                    )
                }
            }

            Column(
                Modifier
                    .background(colors.ivory.copy(alpha = 0.98f))
                    .navigationBarsPadding()
                    .padding(horizontal = 22.dp, vertical = 14.dp),
            ) {
                val plan = plans.first { it.tier == selectedTier }
                val pending = snap.requestStatus == "pending"
                val samePending = pending && snap.requestTier == selectedTier.name
                val button = when {
                    sending -> "Sending…"
                    samePending -> "Request sent"
                    pending -> "Change request to ${plan.name}"
                    snap.tier == selectedTier && snap.tier != MembershipTier.None -> "Request ${plan.name} again"
                    else -> "Request ${plan.name}"
                }
                HinvrPrimaryButton(
                    text = button,
                    enabled = !sending && !samePending,
                    onClick = {
                        scope.launch {
                            sending = true
                            requestError = null
                            requestError = session.requestPlan(plan.tier, plan.amountInr)
                            sending = false
                        }
                    },
                )
                requestError?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    Text(message, style = HinvrTypography.bodyMedium, color = colors.inkMuted)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    when {
                        samePending -> "The desk has this request. The pass opens after they confirm."
                        snap.requestStatus == "declined" && snap.requestTier == selectedTier.name ->
                            "The desk declined this request.${snap.requestNote.takeIf { it.isNotBlank() }?.let { " $it" }.orEmpty()}"
                        else -> "No charge in the app. The desk confirms the plan, then the pass and invoice open."
                    },
                    style = HinvrTypography.bodyMedium,
                    color = colors.inkMuted,
                )
                HinvrTextButton(
                    text = if (opening) "Opening invoices…" else "View invoices",
                    onClick = {
                        if (opening) return@HinvrTextButton
                        scope.launch {
                            opening = true
                            browserError = openMembershipInBrowser(context, session)
                            opening = false
                        }
                    },
                )
                browserError?.let { message ->
                    Text(message, style = HinvrTypography.bodyMedium, color = colors.inkMuted)
                }
            }
        }
    }
}

@Composable
private fun PlanOptionCard(
    plan: PlanCard,
    selected: Boolean,
    current: Boolean,
    onSelect: () -> Unit,
) {
    val colors = HinvrTheme.colors
    val container by animateColorAsState(
        targetValue = if (selected) colors.stone else colors.ivory,
        animationSpec = tween(HinvrMotion.Standard, easing = HinvrMotion.EnterEasing),
        label = "${plan.name} background",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.985f,
        animationSpec = tween(HinvrMotion.Standard, easing = HinvrMotion.EnterEasing),
        label = "${plan.name} scale",
    )
    val titleColor = if (selected) colors.cream else colors.ink
    val bodyColor = if (selected) colors.creamMuted else colors.inkMuted

    IvoryCard(
        onClick = onSelect,
        containerColor = container,
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (selected) Modifier.border(1.dp, colors.gold, RoundedCornerShape(HinvrCardRadius))
                else Modifier,
            ),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(plan.name, style = HinvrTypography.titleLarge, color = titleColor)
                    if (plan.recommended) PlanChip("MOST POPULAR", selected)
                    if (current) PlanChip("CURRENT", selected)
                }
                Spacer(Modifier.height(3.dp))
                Text(plan.audience, style = HinvrTypography.bodyMedium, color = bodyColor)
            }
            SelectionMark(selected)
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(plan.price, style = HinvrTypography.headlineLarge, color = if (selected) colors.gold else colors.ink)
            Text(" / year", style = HinvrTypography.bodyMedium, color = bodyColor, modifier = Modifier.padding(bottom = 5.dp))
        }
        plan.invoiceNote?.let { note ->
            Spacer(Modifier.height(4.dp))
            Text(note, style = HinvrTypography.bodyMedium, color = bodyColor)
        }
        Spacer(Modifier.height(12.dp))
        plan.benefits.take(2).forEach { BenefitRow(it, selected) }
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(tween(HinvrMotion.Standard)) + expandVertically(tween(HinvrMotion.Standard)),
            exit = fadeOut(tween(HinvrMotion.Quick)) + shrinkVertically(tween(HinvrMotion.Quick)),
        ) {
            Column {
                plan.benefits.drop(2).forEach { BenefitRow(it, selected = true) }
            }
        }
        if (!selected && plan.benefits.size > 2) {
            Text(
                "+ ${plan.benefits.size - 2} more benefits",
                style = HinvrTypography.labelLarge,
                color = colors.gold,
                modifier = Modifier.padding(top = 5.dp),
            )
        }
    }
}

@Composable
private fun BenefitRow(text: String, selected: Boolean) {
    val colors = HinvrTheme.colors
    Row(
        Modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("✓", style = HinvrTypography.labelLarge, color = colors.gold)
        Text(
            text,
            style = HinvrTypography.bodyMedium,
            color = if (selected) colors.creamMuted else colors.inkMuted,
        )
    }
}

@Composable
private fun PlanChip(text: String, selected: Boolean) {
    val colors = HinvrTheme.colors
    Text(
        text,
        style = HinvrTypography.labelSmall,
        color = if (selected) colors.dusk else colors.ink,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(colors.gold)
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

@Composable
private fun SelectionMark(selected: Boolean) {
    val colors = HinvrTheme.colors
    Box(
        Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(999.dp))
            .border(1.dp, if (selected) colors.gold else colors.inkMuted, RoundedCornerShape(999.dp)),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(selected, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier
                    .size(14.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.gold),
            )
        }
    }
}
