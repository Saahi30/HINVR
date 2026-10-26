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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PairPanel() {
    // Hidden panels compose nothing, so nothing pulses behind the member's back.
    if (Ring.stage != Stage.Pair) return
    val step = QuestAccount.step
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(PanelShape)
            .background(InkGlass)
            .border(1.dp, Gold.copy(alpha = 0.6f), PanelShape)
            .padding(horizontal = 36.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (step == PairStep.Welcome || step == PairStep.Paired) {
            Welcome()
            return@Column
        }
        Text(
            stringResource(R.string.pair_heading),
            color = Gold,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.6.sp,
        )
        Text(
            stringResource(R.string.pair_title),
            color = Cream,
            fontFamily = FontFamily.Serif,
            fontSize = 36.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(Modifier.height(18.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StepLine("1", stringResource(R.string.pair_step_1))
            StepLine("2", stringResource(R.string.pair_step_2))
        }
        Spacer(Modifier.height(18.dp))
        when (step) {
            PairStep.Offline -> Status(stringResource(R.string.pair_offline), Vermillion, pulse = false)
            PairStep.NeedCamera -> {
                Status(stringResource(R.string.pair_need_camera), CreamMuted, pulse = false)
                Spacer(Modifier.height(12.dp))
                Pill(stringResource(R.string.action_allow_camera)) { PairScanner.askAgain() }
            }
            PairStep.Looking -> Status(stringResource(R.string.pair_looking), CreamMuted, pulse = true)
            PairStep.Claiming -> Status(stringResource(R.string.pair_claiming), Gold, pulse = true)
            PairStep.Welcome, PairStep.Paired -> Unit
        }
        val note = QuestAccount.note
        if (note != null && step != PairStep.Claiming) {
            Spacer(Modifier.height(10.dp))
            Text(stringResource(note), color = Vermillion, fontSize = 17.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(14.dp))
        Text(
            stringResource(R.string.action_credits),
            color = CreamMuted,
            fontSize = 16.sp,
            modifier = Modifier
                .clip(PillShape)
                .border(1.dp, CreamMuted.copy(alpha = 0.5f), PillShape)
                .clickable { Ring.openCredits() }
                .padding(horizontal = 18.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun Welcome() {
    val name = QuestAccount.profile?.displayName.orEmpty()
    DiyaFlame(lit = 1f, size = 72.dp)
    Text(
        if (name.isBlank()) stringResource(R.string.pair_welcome) else stringResource(R.string.pair_welcome_name, name),
        color = Cream,
        fontFamily = FontFamily.Serif,
        fontSize = 40.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp),
    )
    Text(
        stringResource(R.string.pair_done),
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (pulse) {
            PulseDot()
            Spacer(Modifier.width(10.dp))
        }
        Text(text, color = color, fontSize = 18.sp, textAlign = TextAlign.Center)
    }
}

/** Only exists while something is actually in progress. */
@Composable
private fun PulseDot() {
    val glow by rememberInfiniteTransition(label = "pair-status").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pair-pulse",
    )
    Box(
        Modifier
            .size(10.dp)
            .alpha(glow)
            .background(Gold, CircleShape),
    )
}
