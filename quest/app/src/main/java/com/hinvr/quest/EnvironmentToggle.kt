package com.hinvr.quest

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Above the menu: sanctum or passthrough around the cards, and the sound level. */
@Composable
fun EnvironmentToggle() {
    if (Ring.stage != Stage.Menu) return
    val passthrough = Ring.usePassthrough
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .background(InkVeil, PillShape)
                    .border(1.dp, Gold.copy(alpha = 0.32f), PillShape)
                    .padding(3.dp),
            ) {
                ToggleChoice(stringResource(R.string.toggle_sanctum), selected = !passthrough) {
                    Ring.setPassthrough(false)
                }
                ToggleChoice(stringResource(R.string.toggle_passthrough), selected = passthrough) {
                    Ring.setPassthrough(true)
                }
            }
            Text(
                stringResource(R.string.sound_label, stringResource(AudioMix.level.label)).uppercase(),
                color = Cream.copy(alpha = 0.8f),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.1.sp,
                modifier = Modifier
                    .background(InkVeil, PillShape)
                    .border(1.dp, Gold.copy(alpha = 0.32f), PillShape)
                    .clickable { AudioMix.cycle() }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun ToggleChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    val background by animateColorAsState(
        targetValue = if (selected) Gold.copy(alpha = 0.9f) else Color.Transparent,
        label = "environment-choice",
    )
    Text(
        text = label,
        color = if (selected) Ink else Cream.copy(alpha = 0.7f),
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.1.sp,
        modifier = Modifier
            .background(background, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
    )
}
