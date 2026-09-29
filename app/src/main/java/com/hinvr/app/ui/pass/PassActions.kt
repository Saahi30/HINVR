package com.hinvr.app.ui.pass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hinvr.app.R
import com.hinvr.app.ui.components.IvoryCard
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatPassClock(epochMs: Long): String {
    val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    return formatter.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))
}

@Composable
fun physicalCardLabel(status: String): String = when (status) {
    "waitlist" -> stringResource(R.string.card_waitlist)
    "printing" -> stringResource(R.string.card_printing)
    "shipped" -> stringResource(R.string.card_shipped)
    else -> stringResource(R.string.physical_card)
}

@Composable
fun PassCredentialActions(
    busy: Boolean,
    cardStatus: String,
    onNewCode: () -> Unit,
    onWallet: () -> Unit,
    onCard: () -> Unit,
) {
    val cardOpen = cardStatus == "waitlist" || cardStatus == "printing" || cardStatus == "shipped"
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PassLink(
            if (busy) stringResource(R.string.issuing) else stringResource(R.string.new_code),
            enabled = !busy,
            onClick = onNewCode,
        )
        PassLink(stringResource(R.string.google_wallet), enabled = !busy, onClick = onWallet)
        PassLink(physicalCardLabel(cardStatus), enabled = !busy && !cardOpen, onClick = onCard)
    }
}

@Composable
private fun PassLink(text: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = HinvrTheme.colors
    Text(
        text,
        style = HinvrTypography.labelLarge,
        color = if (enabled) colors.gold else colors.creamMuted.copy(alpha = 0.7f),
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
fun PhysicalCardDialog(
    initialName: String,
    initialAddress: String,
    busy: Boolean,
    error: String,
    onDismiss: () -> Unit,
    onSubmit: (name: String, address: String) -> Unit,
) {
    val colors = HinvrTheme.colors
    var name by rememberSaveable { mutableStateOf(initialName) }
    var address by rememberSaveable { mutableStateOf(initialAddress) }
    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        IvoryCard {
            Text(stringResource(R.string.physical_card_kicker), style = HinvrTypography.labelSmall, color = colors.goldDim)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.join_print_list), style = HinvrTypography.titleLarge, color = colors.ink)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.card_body),
                style = HinvrTypography.bodyMedium,
                color = colors.inkMuted,
            )
            Spacer(Modifier.height(16.dp))
            CardField(value = name, onValueChange = { name = it }, hint = stringResource(R.string.card_name))
            Spacer(Modifier.height(10.dp))
            CardField(value = address, onValueChange = { address = it }, hint = stringResource(R.string.card_address), tall = true)
            if (error.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(error, style = HinvrTypography.bodyMedium, color = colors.vermillion)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                if (busy) stringResource(R.string.sending) else stringResource(R.string.request_card),
                style = HinvrTypography.labelLarge,
                color = if (busy || name.isBlank() || address.isBlank()) colors.goldDim else colors.gold,
                modifier = Modifier
                    .clickable(enabled = !busy && name.isNotBlank() && address.isNotBlank()) {
                        onSubmit(name.trim(), address.trim())
                    }
                    .padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun CardField(value: String, onValueChange: (String) -> Unit, hint: String, tall: Boolean = false) {
    val colors = HinvrTheme.colors
    val shape = RoundedCornerShape(16.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(color = colors.ink),
        cursorBrush = SolidColor(colors.gold),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (tall) 88.dp else 48.dp)
            .clip(shape)
            .background(colors.linen)
            .border(1.dp, colors.gold.copy(alpha = 0.35f), shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(hint, style = HinvrTypography.bodyMedium, color = colors.inkMuted)
                }
                inner()
            }
        },
    )
}
