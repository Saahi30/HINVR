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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** End of a recorded tour, or an honest note that it couldn't play. */
@Composable
fun TourEndPanel() {
    val sphere = Ring.sphere ?: return
    val failed = sphere.status == SphereStatus.Failed
    if (!failed && sphere.status != SphereStatus.Ended) return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(PanelShape)
            .background(Ink)
            .border(1.dp, Gold.copy(alpha = 0.6f), PanelShape)
            .padding(horizontal = 32.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            sphere.tour.title,
            color = Gold,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.4.sp,
        )
        Text(
            stringResource(if (failed) R.string.tour_failed_title else R.string.tour_done_title),
            color = Cream,
            fontFamily = FontFamily.Serif,
            fontSize = 34.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (failed) {
            Text(
                stringResource(R.string.tour_failed_body),
                color = CreamMuted,
                fontSize = 17.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Spacer(Modifier.height(22.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            EndChoice(
                stringResource(if (failed) R.string.action_try_again else R.string.tour_again),
                filled = true,
                Modifier.weight(1f),
            ) { Ring.watchTourAgain() }
            EndChoice(stringResource(R.string.action_back_to_menu), filled = false, Modifier.weight(1f)) { Ring.leaveSphere() }
        }
    }
}

@Composable
private fun EndChoice(label: String, filled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Text(
        label,
        color = if (filled) Ink else Cream,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(PillShape)
            .background(if (filled) Gold else Color.Transparent)
            .border(1.5.dp, Gold, PillShape)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    )
}
