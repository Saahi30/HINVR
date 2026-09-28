package com.hinvr.quest

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Stone = Color(0xE6100B08)
private val Cream = Color(0xFFF3E6D0)
private val Gold = Color(0xFFC9A227)

@Composable
fun EnvironmentToggle() {
    val passthrough = Ring.usePassthrough
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .background(Stone, shape)
                .border(1.dp, Gold.copy(alpha = 0.32f), shape)
                .padding(3.dp),
        ) {
            ToggleChoice("SANCTUM", selected = !passthrough) {
                Ring.setPassthrough(false)
            }
            ToggleChoice("PASSTHROUGH", selected = passthrough) {
                Ring.setPassthrough(true)
            }
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
        color = if (selected) Stone else Cream.copy(alpha = 0.58f),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.1.sp,
        modifier = Modifier
            .background(background, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 6.dp),
    )
}
