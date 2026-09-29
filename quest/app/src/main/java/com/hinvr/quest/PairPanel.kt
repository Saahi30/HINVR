package com.hinvr.quest

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Stone = Color(0xF2100B08)
private val Cream = Color(0xFFF3E6D0)
private val CreamMuted = Color(0xFFD9C7A8)
private val Gold = Color(0xFFC9A227)
private val Vermillion = Color(0xFFE0674F)
private val Rounded = RoundedCornerShape(28.dp)

@Composable
fun PairPanel() {
    val step = QuestAccount.step
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(Rounded)
            .background(Stone)
            .border(1.dp, Gold.copy(alpha = 0.6f), Rounded)
            .padding(horizontal = 36.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (step == PairStep.Welcome) {
            Welcome()
            return@Column
        }
        Text(
            "PAIR THIS HEADSET",
            color = Gold,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.6.sp,
        )
        Text(
            "अपना फ़ोन जोड़ें",
            color = Cream,
            fontFamily = FontFamily.Serif,
            fontSize = 36.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(Modifier.height(18.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StepLine("1", "On your phone, open HINVR → Profile → VR headsets → Pair a headset.")
            StepLine("2", "Hold the phone out in front of you and look at its QR code.")
        }
        Spacer(Modifier.height(20.dp))
        when (step) {
            PairStep.Offline -> Status("This build isn’t connected to HINVR.", Vermillion, pulse = false)
            PairStep.NeedCamera -> {
                Status("Allow camera access so the headset can read the code.", CreamMuted, pulse = false)
                Spacer(Modifier.height(12.dp))
                Pill("Allow camera") { PairScanner.askAgain() }
            }
            PairStep.Looking -> Status("Looking for the code…", CreamMuted, pulse = true)
            PairStep.Claiming -> Status("Pairing…", Gold, pulse = true)
            PairStep.Welcome -> Unit
        }
        val note = QuestAccount.note
        if (note != null && step != PairStep.Claiming) {
            Spacer(Modifier.height(10.dp))
            Text(note, color = Vermillion, fontSize = 16.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun Welcome() {
    val name = QuestAccount.profile?.displayName.orEmpty()
    DiyaFlame(lit = 1f, size = 72.dp)
    Text(
        if (name.isBlank()) "नमस्ते" else "नमस्ते, $name",
        color = Cream,
        fontFamily = FontFamily.Serif,
        fontSize = 40.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp),
    )
    Text(
        "Your headset is paired.",
        color = CreamMuted,
        fontSize = 20.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 6.dp),
    )
}

@Composable
private fun StepLine(number: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .border(1.dp, Gold, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(number, color = Gold, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(14.dp))
        Text(text, color = Cream, fontSize = 19.sp, lineHeight = 25.sp)
    }
}

@Composable
private fun Status(text: String, color: Color, pulse: Boolean) {
    val glow by rememberInfiniteTransition(label = "pair-status").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pair-pulse",
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (pulse) {
            Box(
                Modifier
                    .size(10.dp)
                    .alpha(glow)
                    .background(Gold, CircleShape),
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(text, color = color, fontSize = 18.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun Pill(label: String, filled: Boolean = true, tint: Color = Gold, onClick: () -> Unit) {
    val shape = RoundedCornerShape(999.dp)
    Text(
        label,
        color = if (filled) Color(0xFF100B08) else tint,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(shape)
            .background(if (filled) tint else Color.Transparent)
            .border(1.5.dp, tint, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 12.dp),
    )
}
