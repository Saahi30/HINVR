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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hinvr.app.ui.components.IvoryCard
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ClockFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

fun formatPassClock(epochMs: Long): String =
    ClockFormat.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))

fun physicalCardLabel(status: String): String = when (status) {
    "waitlist" -> "On the card list"
    "printing" -> "Card is printing"
    "shipped" -> "Card was sent"
    else -> "Physical card"
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
        PassLink(if (busy) "Issuing…" else "New code", enabled = !busy, onClick = onNewCode)
        PassLink("Google Wallet", enabled = !busy, onClick = onWallet)
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
            Text("PHYSICAL CARD", style = HinvrTypography.labelSmall, color = colors.goldDim)
            Spacer(Modifier.height(6.dp))
            Text("Join the print list", style = HinvrTypography.titleLarge, color = colors.ink)
            Spacer(Modifier.height(8.dp))
            Text(
                "The desk prints it later. The phone code is what they scan.",
                style = HinvrTypography.bodyMedium,
                color = colors.inkMuted,
            )
            Spacer(Modifier.height(16.dp))
            CardField(value = name, onValueChange = { name = it }, hint = "Name on the card")
            Spacer(Modifier.height(10.dp))
            CardField(value = address, onValueChange = { address = it }, hint = "Where to send it", tall = true)
            if (error.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(error, style = HinvrTypography.bodyMedium, color = colors.vermillion)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                if (busy) "Sending…" else "Request the card",
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
