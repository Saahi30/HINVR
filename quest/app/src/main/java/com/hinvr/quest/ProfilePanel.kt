package com.hinvr.quest

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Stone = Color(0xF2100B08)
private val Cream = Color(0xFFF3E6D0)
private val CreamMuted = Color(0xFFD9C7A8)
private val Gold = Color(0xFFC9A227)
private val Vermillion = Color(0xFFE0674F)
private val Rounded = RoundedCornerShape(28.dp)

@Composable
fun ProfilePanel() {
    val profile = QuestAccount.profile ?: return
    var confirming by remember { mutableStateOf(false) }
    LaunchedEffect(Ring.stage) {
        confirming = false
        if (Ring.stage == Stage.Profile) QuestAccount.refresh()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(Rounded)
            .background(Stone)
            .border(1.dp, Gold.copy(alpha = 0.6f), Rounded)
            .padding(horizontal = 40.dp, vertical = 30.dp),
    ) {
        Text(
            "HINVR MEMBER",
            color = Gold,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.6.sp,
        )
        Text(
            profile.displayName.ifBlank { "Member" },
            color = Cream,
            fontFamily = FontFamily.Serif,
            fontSize = 40.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (profile.email.isNotBlank()) {
            Text(profile.email, color = CreamMuted, fontSize = 17.sp)
        }
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Detail("MEMBERSHIP", tierLabel(profile.tier))
                Detail("MEMBER ID", profile.memberId.ifBlank { "—" })
                Detail("VALID UNTIL", profile.validUntil.ifBlank { "—" })
            }
            Spacer(Modifier.width(24.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Detail("CITY", profile.city.ifBlank { "—" })
                Detail("HEADSET", profile.deviceName.ifBlank { "Meta Quest" })
                Detail("PAIRED SINCE", shortDate(profile.pairedAt))
            }
        }
        Spacer(Modifier.height(18.dp))
        MandirCredits()
        Spacer(Modifier.weight(1f))
        QuestAccount.unpairError?.let {
            Text(it, color = Vermillion, fontSize = 16.sp)
            Spacer(Modifier.height(10.dp))
        }
        if (confirming && !QuestAccount.unpairing) {
            Text(
                "You’ll need your phone to pair this headset again.",
                color = CreamMuted,
                fontSize = 16.sp,
            )
            Spacer(Modifier.height(10.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Pill("Back to menu") { Ring.back() }
            Pill(
                when {
                    QuestAccount.unpairing -> "Unpairing…"
                    confirming -> "Tap again to unpair"
                    else -> "Unpair this headset"
                },
                filled = false,
                tint = Vermillion,
            ) {
                if (QuestAccount.unpairing) return@Pill
                if (!confirming) confirming = true else QuestAccount.unpair()
            }
            if (confirming && !QuestAccount.unpairing) {
                Pill("Cancel", filled = false, tint = CreamMuted) { confirming = false }
            }
        }
    }
}

/** Sits above the menu cards; opens the profile. */
@Composable
fun ProfileChip() {
    val profile = QuestAccount.profile
    val shape = RoundedCornerShape(999.dp)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (profile == null) return@Box
        Row(
            modifier = Modifier
                .clip(shape)
                .background(Color(0xE6100B08))
                .border(1.dp, Gold.copy(alpha = 0.45f), shape)
                .clickable { Ring.openProfile() }
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                profile.displayName.ifBlank { "Member" },
                color = Cream,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "  ·  ${tierLabel(profile.tier).uppercase()}  ›",
                color = Gold,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.1.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun Detail(label: String, value: String) {
    Column {
        Text(label, color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.3.sp)
        Text(value, color = Cream, fontSize = 21.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun tierLabel(tier: String): String = when (tier) {
    "", "None" -> "No membership yet"
    "Nri" -> "NRI"
    else -> tier
}

private fun shortDate(iso: String): String {
    val parsed = runCatching { OffsetDateTime.parse(iso) }.getOrNull() ?: return "—"
    return DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
        .format(parsed.atZoneSameInstant(ZoneId.systemDefault()))
}
