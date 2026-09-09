package com.hinvr.app.ui.plans

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hinvr.app.data.MembershipTier
import com.hinvr.app.navigation.LocalSessionRepository
import com.hinvr.app.ui.components.HinvrBackground
import com.hinvr.app.ui.components.HinvrPrimaryButton
import com.hinvr.app.ui.components.IvoryCard
import com.hinvr.app.ui.components.SabhaTopBar
import com.hinvr.app.ui.splash.DiyaFlame
import com.hinvr.app.ui.theme.Atmosphere
import com.hinvr.app.ui.theme.HinvrCardRadius
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private data class PlanCard(
    val tier: MembershipTier,
    val name: String,
    val price: String,
    val blurb: String,
    val recommended: Boolean = false,
)

@Composable
fun PlansScreen(onBack: () -> Unit, onMockPay: () -> Unit) {
    val session = LocalSessionRepository.current
    val scope = rememberCoroutineScope()
    val colors = HinvrTheme.colors
    val plans = listOf(
        PlanCard(MembershipTier.Darshan, "Darshan", "₹999 / yr", "Live + directory"),
        PlanCard(MembershipTier.Gold, "Gold", "₹4,999 / yr", "QR pass, 2 poojas, chat", recommended = true),
        PlanCard(MembershipTier.Platinum, "Platinum", "₹14,999 / yr", "Card, phone, assist, family"),
    )

    HinvrBackground(atmosphere = Atmosphere.Sabha) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            SabhaTopBar(onBack = onBack)
            Column(Modifier.padding(horizontal = 22.dp)) {
                Text("MEMBERSHIP", style = HinvrTypography.labelSmall, color = colors.gold)
                Spacer(Modifier.height(8.dp))
                Text("Join the club", style = HinvrTypography.headlineLarge, color = colors.ink)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Assist depends on partner temples. No unofficial queue-jumping.",
                    style = HinvrTypography.bodyMedium,
                    color = colors.inkMuted,
                )
                Spacer(Modifier.height(20.dp))
                plans.forEach { plan ->
                    IvoryCard(
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .then(
                                if (plan.recommended) {
                                    Modifier.border(1.dp, colors.gold, RoundedCornerShape(HinvrCardRadius))
                                } else {
                                    Modifier
                                },
                            ),
                    ) {
                        if (plan.recommended) {
                            Text("RECOMMENDED", style = HinvrTypography.labelSmall, color = colors.gold)
                            Spacer(Modifier.height(6.dp))
                        }
                        Text(plan.name, style = HinvrTypography.titleLarge, color = colors.ink)
                        Text(plan.price, style = HinvrTypography.headlineMedium, color = colors.gold)
                        Spacer(Modifier.height(4.dp))
                        Text(plan.blurb, style = HinvrTypography.bodyMedium, color = colors.inkMuted)
                        Spacer(Modifier.height(12.dp))
                        HinvrPrimaryButton(
                            text = "Continue",
                            onClick = {
                                scope.launch {
                                    val snap = session.snapshot.first()
                                    val suffix = snap.userId.takeLast(5).uppercase().ifBlank { "48291" }
                                    session.setTier(plan.tier, "HNV-$suffix", "till Apr 2027")
                                    onMockPay()
                                }
                            },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun PaySuccessScreen(onOpenPass: () -> Unit) {
    val colors = HinvrTheme.colors
    HinvrBackground(atmosphere = Atmosphere.Sanctum) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            DiyaFlame(lit = 1f, size = 96.dp)
            Spacer(Modifier.height(20.dp))
            Text("Your pass is ready.", style = HinvrTypography.headlineLarge, color = colors.cream)
            Spacer(Modifier.height(8.dp))
            Text("A quiet diya, not balloons.", style = HinvrTypography.bodyLarge, color = colors.creamMuted)
            Spacer(Modifier.weight(1f))
            HinvrPrimaryButton("Open the pass", onOpenPass)
            Spacer(Modifier.height(16.dp))
        }
    }
}
