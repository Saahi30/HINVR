package com.hinvr.quest

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The headset's palette. Every panel draws from these so the sanctum reads as one place. */
val Ink = Color(0xFF100B08)
val InkGlass = Color(0xF2100B08)
val InkVeil = Color(0xE6100B08)
val InkShade = Color(0xAA100B08)
val Stone = Color(0xFF2A2118)
val Cream = Color(0xFFF3E6D0)
val CreamMuted = Color(0xFFD9C7A8)
val Gold = Color(0xFFC9A227)
val GoldLight = Color(0xFFF1D27A)
val Vermillion = Color(0xFFE0674F)
val LiveRed = Color(0xFFB3392B)
val Saffron = Color(0xFFE07A2E)
val Ember = Color(0xFFC45C26)
val Marigold = Color(0xFFF2A516)
val DiyaAmber = Color(0xFFE8A317)
val FlameCore = Color(0xFFFFF1C1)

val PanelShape = RoundedCornerShape(28.dp)
val PillShape = RoundedCornerShape(999.dp)

@Composable
fun Pill(label: String, filled: Boolean = true, tint: Color = Gold, onClick: () -> Unit) {
    Text(
        label,
        color = if (filled) Ink else tint,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(PillShape)
            .background(if (filled) tint else Color.Transparent)
            .border(1.5.dp, tint, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 12.dp),
    )
}

fun Int.floorMod(size: Int): Int = Math.floorMod(this, size)
