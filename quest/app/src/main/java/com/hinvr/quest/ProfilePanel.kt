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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.ZoneId

@Composable
fun ProfilePanel() {
    if (Ring.stage != Stage.Profile) return
    val profile = QuestAccount.profile ?: return
    var confirming by remember { mutableStateOf(false) }
    LaunchedEffect(Ring.stage) {
        confirming = false
        if (Ring.stage == Stage.Profile) QuestAccount.refresh()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(PanelShape)
            .background(InkGlass)
            .border(1.dp, Gold.copy(alpha = 0.6f), PanelShape)
            .padding(horizontal = 40.dp, vertical = 30.dp),
    ) {
        Text(
            stringResource(R.string.profile_heading),
            color = Gold,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.6.sp,
        )
        Text(
            profile.displayName.ifBlank { stringResource(R.string.profile_member) },
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
                Detail(stringResource(R.string.profile_membership), tierLabel(profile.tier))
                Detail(stringResource(R.string.profile_member_id), profile.memberId.ifBlank { "—" })
                Detail(stringResource(R.string.profile_valid_until), MemberFormat.validUntil(profile.validUntil))
            }
            Spacer(Modifier.width(24.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Detail(stringResource(R.string.profile_city), profile.city.ifBlank { "—" })
                Detail(stringResource(R.string.profile_headset), profile.deviceName.ifBlank { "Meta Quest" })
                Detail(stringResource(R.string.profile_paired_since), MemberFormat.shortDate(profile.pairedAt, ZoneId.systemDefault()))
            }
        }
        Spacer(Modifier.weight(1f))
        QuestAccount.unpairError?.let {
            Text(stringResource(it), color = Vermillion, fontSize = 16.sp)
            Spacer(Modifier.height(10.dp))
        }
        if (confirming && !QuestAccount.unpairing) {
            Text(
                stringResource(R.string.profile_unpair_warning),
                color = CreamMuted,
                fontSize = 16.sp,
            )
            Spacer(Modifier.height(10.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Pill(stringResource(R.string.action_back_to_menu)) { Ring.back() }
            Pill(stringResource(R.string.action_credits), filled = false, tint = CreamMuted) { Ring.openCredits() }
            Pill(stringResource(R.string.language_switch), filled = false, tint = CreamMuted) { QuestLocale.toggle() }
            Pill(
                when {
                    QuestAccount.unpairing -> stringResource(R.string.profile_unpairing)
                    confirming -> stringResource(R.string.profile_unpair_confirm)
                    else -> stringResource(R.string.profile_unpair)
                },
                filled = false,
                tint = Vermillion,
            ) {
                if (QuestAccount.unpairing) return@Pill
                if (!confirming) confirming = true else QuestAccount.unpair()
            }
            if (confirming && !QuestAccount.unpairing) {
                Pill(stringResource(R.string.action_cancel), filled = false, tint = CreamMuted) { confirming = false }
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
                .background(InkVeil)
                .border(1.dp, Gold.copy(alpha = 0.45f), shape)
                .clickable { Ring.openProfile() }
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                profile.displayName.ifBlank { stringResource(R.string.profile_member) },
                color = Cream,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "  ·  ${tierLabel(profile.tier).uppercase()}  ›",
                color = Gold,
                fontSize = 14.sp,
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
        Text(label, color = Gold, fontSize = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.3.sp)
        Text(value, color = Cream, fontSize = 21.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun tierLabel(tier: String): String = when (tier) {
    "", "None" -> stringResource(R.string.profile_no_membership)
    "Nri" -> "NRI"
    else -> tier
}
