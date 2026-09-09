package com.hinvr.app.ui.pass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hinvr.app.data.MembershipTier
import com.hinvr.app.data.SessionSnapshot
import com.hinvr.app.navigation.LocalSessionRepository
import com.hinvr.app.ui.components.HinvrBackground
import com.hinvr.app.ui.components.HinvrGoldOutlineButton
import com.hinvr.app.ui.components.HinvrPrimaryButton
import com.hinvr.app.ui.components.IvoryCard
import com.hinvr.app.ui.components.SabhaTopBar
import com.hinvr.app.ui.theme.Atmosphere
import com.hinvr.app.ui.theme.HinvrCardRadius
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography

@Composable
fun PassScreen(
    onOpenPlans: () -> Unit,
    onOpenHow: () -> Unit,
    onPlanVisit: () -> Unit,
) {
    val session = LocalSessionRepository.current
    val snap by session.snapshot.collectAsStateWithLifecycle(initialValue = SessionSnapshot())
    val member = snap.tier != MembershipTier.None
    val colors = HinvrTheme.colors

    HinvrBackground(atmosphere = Atmosphere.Sanctum) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(22.dp),
        ) {
            Text("PASS", style = HinvrTypography.labelSmall, color = colors.gold)
            Spacer(Modifier.height(10.dp))
            Text(
                if (member) "Show this at the HINVR desk" else "Your name on a temple pass.",
                style = HinvrTypography.headlineLarge,
                color = colors.cream,
            )
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(HinvrCardRadius))
                    .background(
                        Brush.linearGradient(
                            listOf(colors.stoneRaised, colors.stone, colors.dusk),
                        ),
                    )
                    .border(1.dp, colors.gold.copy(alpha = 0.45f), RoundedCornerShape(HinvrCardRadius))
                    .then(if (member) Modifier else Modifier.blur(12.dp))
                    .padding(22.dp),
            ) {
                Column {
                    Text(
                        "HINVR  ·  ${if (member) snap.tier.name.uppercase() else "GOLD"}",
                        style = HinvrTypography.labelSmall,
                        color = colors.gold,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        snap.displayName.ifBlank { "MEMBER" }.uppercase(),
                        style = HinvrTypography.headlineMedium,
                        color = colors.cream,
                    )
                    Text(
                        snap.memberId.ifBlank { "HNV-00000" },
                        style = HinvrTypography.bodyMedium,
                        color = colors.creamMuted,
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(76.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.cream.copy(alpha = 0.92f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("QR", color = colors.ink, style = HinvrTypography.titleLarge)
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            if (!member) {
                HinvrPrimaryButton("See membership", onOpenPlans)
            } else {
                HinvrPrimaryButton("Plan a visit", onPlanVisit)
            }
            Spacer(Modifier.height(10.dp))
            HinvrGoldOutlineButton("How the pass works", onOpenHow)
            Spacer(Modifier.height(20.dp))
            Text(
                "We do not sell unofficial skip-the-line at board-run temples.",
                style = HinvrTypography.bodyMedium,
                color = colors.creamMuted,
            )
        }
    }
}

@Composable
fun HowPassWorksScreen(onBack: () -> Unit) {
    val colors = HinvrTheme.colors
    val steps = listOf(
        "Show QR or metal card at the partner desk.",
        "Host confirms the member.",
        "Official entry, buggy, or wheelchair as booked.",
    )
    HinvrBackground(atmosphere = Atmosphere.Sabha) {
        Column(Modifier.fillMaxSize()) {
            SabhaTopBar(title = "How the pass works", onBack = onBack)
            Column(Modifier.padding(horizontal = 22.dp, vertical = 8.dp)) {
                steps.forEachIndexed { index, line ->
                    IvoryCard(modifier = Modifier.padding(bottom = 12.dp)) {
                        Text("0${index + 1}", style = HinvrTypography.labelSmall, color = colors.gold)
                        Spacer(Modifier.height(8.dp))
                        Text(line, style = HinvrTypography.titleLarge, color = colors.ink)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "We do not sell unofficial skip-the-line at board-run temples.",
                    style = HinvrTypography.bodyMedium,
                    color = colors.inkMuted,
                )
            }
        }
    }
}
