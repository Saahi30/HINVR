package com.hinvr.quest

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Stone = Color(0xFF100B08)
private val Cream = Color(0xFFF3E6D0)
private val Gold = Color(0xFFC9A227)
private val Rounded = RoundedCornerShape(28.dp)

@Composable
fun TourEndPanel() {
    val title = Ring.sphereTour?.title.orEmpty()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(Rounded)
            .background(Stone)
            .border(1.dp, Gold.copy(alpha = 0.6f), Rounded)
            .padding(horizontal = 32.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            title,
            color = Gold,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.4.sp,
        )
        Text(
            "दर्शन पूर्ण हुए",
            color = Cream,
            fontFamily = FontFamily.Serif,
            fontSize = 34.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
        Spacer(Modifier.height(26.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            EndChoice("वापस दर्शन करें", filled = true, Modifier.weight(1f)) { Ring.watchTourAgain() }
            EndChoice("Back to menu", filled = false, Modifier.weight(1f)) { Ring.leaveSphere() }
        }
    }
}

@Composable
private fun EndChoice(label: String, filled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(999.dp)
    Text(
        label,
        color = if (filled) Stone else Cream,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(shape)
            .background(if (filled) Gold else Color.Transparent)
            .border(1.5.dp, Gold, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    )
}
