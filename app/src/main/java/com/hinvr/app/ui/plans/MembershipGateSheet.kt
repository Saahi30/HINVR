package com.hinvr.app.ui.plans

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hinvr.app.ui.components.HinvrPrimaryButton
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MembershipGateSheet(
    reason: String,
    onDismiss: () -> Unit,
    onSeePlans: () -> Unit,
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
            Text("MEMBERSHIP", style = HinvrTypography.labelSmall, color = colors.gold)
            Spacer(Modifier.height(8.dp))
            Text(reason, style = HinvrTypography.headlineMedium, color = colors.ink)
            Spacer(Modifier.height(10.dp))
            Text(
                "Gold includes the digital pass, visit assist, and recorded VR darshan.",
                style = HinvrTypography.bodyLarge,
                color = colors.inkMuted,
            )
            Spacer(Modifier.height(20.dp))
            HinvrPrimaryButton("See membership", onSeePlans)
            Spacer(Modifier.height(18.dp))
        }
    }
}
