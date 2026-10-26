package com.hinvr.app.ui.vr

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hinvr.app.R
import com.hinvr.app.data.HandoffPhase
import com.hinvr.app.data.NoHeadsetMessage
import com.hinvr.app.data.VrHandoffs
import com.hinvr.app.data.VrTarget
import com.hinvr.app.data.attempt
import com.hinvr.app.i18n.userMessage
import com.hinvr.app.navigation.LocalSessionRepository
import com.hinvr.app.ui.components.HinvrGoldOutlineButton
import com.hinvr.app.ui.components.HinvrPrimaryButton
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography
import java.time.Instant
import kotlinx.coroutines.delay

/**
 * "View in VR": asks the member's paired headset to open [title]. The headset has no push,
 * so it picks the request up while HINVR is open on it; the sheet shows when it has.
 */
@Composable
fun ViewInVrButton(
    target: VrTarget,
    mandirId: String?,
    title: String,
    onHeadsets: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.view_in_vr),
    officialStream: Boolean = target == VrTarget.Live,
) {
    var open by remember { mutableStateOf(false) }
    HinvrGoldOutlineButton(label, onClick = { open = true }, modifier = modifier)
    if (open) {
        ViewInVrSheet(
            target = target,
            mandirId = mandirId,
            title = title,
            officialStream = officialStream,
            onDismiss = { open = false },
            onHeadsets = {
                open = false
                onHeadsets()
            },
        )
    }
}

/** The View in VR sheet on its own, for when it opens from somewhere other than its button (a reminder). */
@Composable
fun ViewInVrSheetFor(
    target: VrTarget,
    mandirId: String?,
    title: String,
    onDismiss: () -> Unit,
    onHeadsets: () -> Unit,
) {
    ViewInVrSheet(target, mandirId, title, officialStream = target == VrTarget.Live, onDismiss = onDismiss, onHeadsets = onHeadsets)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ViewInVrSheet(
    target: VrTarget,
    mandirId: String?,
    title: String,
    officialStream: Boolean,
    onDismiss: () -> Unit,
    onHeadsets: () -> Unit,
) {
    val session = LocalSessionRepository.current
    val context = LocalContext.current
    val colors = HinvrTheme.colors
    var phase by remember { mutableStateOf<HandoffPhase>(HandoffPhase.Asking) }
    // Bumped by "Try again" so the request is made afresh.
    var attemptNo by remember { mutableIntStateOf(0) }

    LaunchedEffect(attemptNo) {
        phase = HandoffPhase.Asking
        val made = attempt { session.requestVrHandoff(target, mandirId) }
        val handoff = made.getOrElse { error ->
            phase = if (error.message == NoHeadsetMessage) {
                HandoffPhase.NoHeadset
            } else {
                HandoffPhase.Failed(context.userMessage(error.message))
            }
            return@LaunchedEffect
        }
        val expiresAt = VrHandoffs.parseInstant(handoff.expiresAt)
        phase = HandoffPhase.Waiting
        while (!VrHandoffs.settled(phase)) {
            delay(VrHandoffs.PollMs)
            // A dropped connection just means asking again next time, until the request runs out.
            val status = attempt { session.vrHandoffStatus(handoff.id) }.getOrNull()
            phase = VrHandoffs.phaseFor(status, expiresAt, Instant.now())
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.ivory,
        contentColor = colors.ink,
    ) {
        Column(
            Modifier
                .padding(horizontal = 22.dp)
                .navigationBarsPadding(),
        ) {
            Text(stringResource(R.string.view_in_vr_eyebrow), style = HinvrTypography.labelSmall, color = colors.gold)
            Spacer(Modifier.height(8.dp))
            when (val now = phase) {
                HandoffPhase.Asking, HandoffPhase.Waiting -> {
                    Text(title, style = HinvrTypography.headlineMedium, color = colors.ink)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.view_in_vr_waiting, title),
                        style = HinvrTypography.bodyLarge,
                        color = colors.inkMuted,
                    )
                    Spacer(Modifier.height(14.dp))
                    HandoffJourney(now, title)
                    if (officialStream) {
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.view_in_vr_official), style = HinvrTypography.bodyMedium, color = colors.inkMuted)
                    }
                    Spacer(Modifier.height(20.dp))
                    HinvrGoldOutlineButton(stringResource(R.string.view_in_vr_close), onDismiss)
                }
                is HandoffPhase.Opened -> {
                    HandoffJourney(now, title)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        if (now.headset.isBlank()) {
                            stringResource(R.string.view_in_vr_opened_plain)
                        } else {
                            stringResource(R.string.view_in_vr_opened, now.headset)
                        },
                        style = HinvrTypography.headlineMedium,
                        color = colors.ink,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.view_in_vr_opened_body, title), style = HinvrTypography.bodyLarge, color = colors.inkMuted)
                    Spacer(Modifier.height(20.dp))
                    HinvrPrimaryButton(stringResource(R.string.view_in_vr_done), onDismiss)
                }
                HandoffPhase.Expired -> {
                    Text(stringResource(R.string.view_in_vr_expired), style = HinvrTypography.headlineMedium, color = colors.ink)
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.view_in_vr_expired_body), style = HinvrTypography.bodyLarge, color = colors.inkMuted)
                    Spacer(Modifier.height(20.dp))
                    HinvrPrimaryButton(stringResource(R.string.view_in_vr_retry), { attemptNo++ })
                }
                HandoffPhase.NoHeadset -> {
                    Text(stringResource(R.string.view_in_vr_no_headset), style = HinvrTypography.headlineMedium, color = colors.ink)
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.view_in_vr_no_headset_body), style = HinvrTypography.bodyLarge, color = colors.inkMuted)
                    Spacer(Modifier.height(20.dp))
                    HinvrPrimaryButton(stringResource(R.string.view_in_vr_pair), onHeadsets)
                }
                is HandoffPhase.Failed -> {
                    Text(now.message.orEmpty(), style = HinvrTypography.headlineMedium, color = colors.ink)
                    Spacer(Modifier.height(20.dp))
                    HinvrPrimaryButton(stringResource(R.string.view_in_vr_retry), { attemptNo++ })
                }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}
