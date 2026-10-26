package com.hinvr.app.ui.plans

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hinvr.app.R
import com.hinvr.app.data.MembershipTier
import com.hinvr.app.data.hasDeskPass
import com.hinvr.app.ui.components.HinvrPrimaryButton
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MembershipGateSheet(
    reason: String,
    onDismiss: () -> Unit,
    onSeePlans: () -> Unit,
    eyebrow: String = stringResource(R.string.gate_membership),
    body: String = stringResource(R.string.gate_gold_body),
    actionLabel: String = stringResource(R.string.gate_see_membership),
) {
    val colors = HinvrTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.ivory,
        contentColor = colors.ink,
    ) {
        Column(
            Modifier
                .padding(horizontal = 22.dp)
                .navigationBarsPadding(),
        ) {
            Text(eyebrow, style = HinvrTypography.labelSmall, color = colors.gold)
            Spacer(Modifier.height(8.dp))
            Text(reason, style = HinvrTypography.headlineMedium, color = colors.ink)
            Spacer(Modifier.height(10.dp))
            Text(body, style = HinvrTypography.bodyLarge, color = colors.inkMuted)
            Spacer(Modifier.height(20.dp))
            HinvrPrimaryButton(actionLabel, onSeePlans)
            Spacer(Modifier.height(18.dp))
        }
    }
}

/** Why a mandir's Assist action can't go straight to the visit form. */
enum class MandirAssistPrompt { Gate, Desk }

/** Null means the member can plan the visit now. */
fun assistPromptFor(passAccepted: Boolean, tier: MembershipTier): MandirAssistPrompt? = when {
    !passAccepted -> MandirAssistPrompt.Desk
    !tier.hasDeskPass -> MandirAssistPrompt.Gate
    else -> null
}

/** One sheet for both reasons, so Home and the temple page say the same thing. */
@Composable
fun MandirAssistSheet(
    prompt: MandirAssistPrompt?,
    onDismiss: () -> Unit,
    onPlans: () -> Unit,
    onConcierge: () -> Unit,
) {
    when (prompt) {
        MandirAssistPrompt.Gate -> MembershipGateSheet(
            reason = stringResource(R.string.gate_visit),
            onDismiss = onDismiss,
            onSeePlans = {
                onDismiss()
                onPlans()
            },
        )
        MandirAssistPrompt.Desk -> MembershipGateSheet(
            reason = stringResource(R.string.gate_not_accepted),
            onDismiss = onDismiss,
            onSeePlans = {
                onDismiss()
                onConcierge()
            },
            eyebrow = stringResource(R.string.gate_desk),
            body = stringResource(R.string.gate_desk_body),
            actionLabel = stringResource(R.string.ask_concierge),
        )
        null -> Unit
    }
}
