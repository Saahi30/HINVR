package com.hinvr.app.ui.vr

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hinvr.app.R
import com.hinvr.app.data.VrDeviceRow
import com.hinvr.app.data.VrPairingCode
import com.hinvr.app.i18n.userMessage
import com.hinvr.app.navigation.LocalSessionRepository
import com.hinvr.app.ui.components.HinvrBackground
import com.hinvr.app.ui.components.HinvrGoldOutlineButton
import com.hinvr.app.ui.components.HinvrPrimaryButton
import com.hinvr.app.ui.components.IvoryCard
import com.hinvr.app.ui.components.SabhaTopBar
import com.hinvr.app.ui.pass.PassQr
import com.hinvr.app.ui.theme.Atmosphere
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The headset scans this. Keep it in step with the Quest app's reader. */
private const val PairPrefix = "HINVR1:"

@Composable
fun VrHeadsetsScreen(onBack: () -> Unit) {
    val session = LocalSessionRepository.current
    val context = LocalContext.current
    val colors = HinvrTheme.colors
    val scope = rememberCoroutineScope()
    var devices by remember { mutableStateOf<List<VrDeviceRow>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var pairing by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf<VrPairingCode?>(null) }
    var secondsLeft by remember { mutableIntStateOf(0) }
    var justPaired by remember { mutableStateOf(false) }
    var confirming by remember { mutableStateOf<String?>(null) }
    var unlinking by remember { mutableStateOf<String?>(null) }

    suspend fun reload(): List<VrDeviceRow>? =
        runCatching { session.vrDevices() }
            .onSuccess { devices = it }
            .onFailure { error = context.userMessage(it.message) }
            .getOrNull()

    LaunchedEffect(Unit) { reload() }

    // A fresh code while the QR is up, and a check for the headset that claims it.
    LaunchedEffect(pairing) {
        if (!pairing) return@LaunchedEffect
        val before = devices.orEmpty().map { it.deviceId to it.pairedAt }.toSet()
        var expiresAt = 0L
        while (true) {
            val now = System.currentTimeMillis()
            if (now >= expiresAt - 5_000L) {
                val issued = runCatching { session.startVrPairing() }
                    .onFailure { error = context.userMessage(it.message) }
                    .getOrNull()
                if (issued == null) {
                    pairing = false
                    code = null
                    return@LaunchedEffect
                }
                error = null
                code = issued
                expiresAt = runCatching { Instant.parse(issued.expiresAt).toEpochMilli() }
                    .getOrElse { OffsetDateTime.parse(issued.expiresAt).toInstant().toEpochMilli() }
            }
            secondsLeft = ((expiresAt - 5_000L - now) / 1000L).toInt().coerceAtLeast(0)
            delay(1_000L)
            if (secondsLeft % 3 == 0) {
                val latest = runCatching { session.vrDevices() }.getOrNull()
                if (latest != null) {
                    devices = latest
                    if (latest.any { (it.deviceId to it.pairedAt) !in before }) {
                        justPaired = true
                        pairing = false
                        code = null
                        return@LaunchedEffect
                    }
                }
            }
        }
    }

    val view = LocalView.current
    DisposableEffect(pairing) {
        view.keepScreenOn = pairing
        onDispose { view.keepScreenOn = false }
    }

    HinvrBackground(atmosphere = Atmosphere.Sabha) {
        Column(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            SabhaTopBar(title = stringResource(R.string.vr_headsets), onBack = onBack)
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 28.dp),
            ) {
                Text(
                    stringResource(R.string.vr_headsets_body),
                    style = HinvrTypography.bodyLarge,
                    color = colors.inkMuted,
                )
                Spacer(Modifier.height(18.dp))

                val shown = code
                if (pairing && shown != null) {
                    IvoryCard(containerColor = colors.cream) {
                        Text(
                            stringResource(R.string.vr_pair_steps),
                            style = HinvrTypography.bodyLarge,
                            color = colors.ink,
                        )
                        Spacer(Modifier.height(14.dp))
                        PassQr(
                            payload = PairPrefix + shown.code,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            stringResource(R.string.vr_pair_refresh, secondsLeft),
                            style = HinvrTypography.bodyMedium,
                            color = colors.inkMuted,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    HinvrGoldOutlineButton(stringResource(R.string.cancel), onClick = {
                        pairing = false
                        code = null
                    })
                } else {
                    if (justPaired) {
                        Text(
                            stringResource(R.string.vr_paired_now),
                            style = HinvrTypography.titleMedium,
                            color = colors.gold,
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    HinvrPrimaryButton(
                        text = stringResource(R.string.vr_pair_button),
                        enabled = !pairing && devices != null,
                        onClick = {
                            justPaired = false
                            error = null
                            pairing = true
                        },
                    )
                }
                error?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, style = HinvrTypography.bodyMedium, color = colors.vermillion)
                }

                Spacer(Modifier.height(24.dp))
                Text(stringResource(R.string.vr_linked), style = HinvrTypography.labelLarge, color = colors.gold)
                Spacer(Modifier.height(10.dp))
                val list = devices
                when {
                    list == null -> Text(stringResource(R.string.opening), style = HinvrTypography.bodyMedium, color = colors.inkMuted)
                    list.isEmpty() -> Text(stringResource(R.string.vr_no_headsets), style = HinvrTypography.bodyMedium, color = colors.inkMuted)
                    else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        list.forEach { device ->
                            IvoryCard {
                                Text(
                                    device.name.ifBlank { "Meta Quest" },
                                    style = HinvrTypography.titleMedium,
                                    color = colors.ink,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    stringResource(R.string.vr_paired_on, shortDate(device.pairedAt)),
                                    style = HinvrTypography.bodyMedium,
                                    color = colors.inkMuted,
                                )
                                Spacer(Modifier.height(10.dp))
                                val asking = confirming == device.deviceId
                                val busy = unlinking == device.deviceId
                                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                    Text(
                                        when {
                                            busy -> stringResource(R.string.vr_unlinking)
                                            asking -> stringResource(R.string.vr_unlink_confirm)
                                            else -> stringResource(R.string.vr_unlink)
                                        },
                                        style = HinvrTypography.titleMedium,
                                        color = colors.vermillion,
                                        modifier = Modifier
                                            .clickable(enabled = unlinking == null) {
                                                if (!asking) {
                                                    confirming = device.deviceId
                                                    return@clickable
                                                }
                                                unlinking = device.deviceId
                                                scope.launch {
                                                    runCatching { session.unlinkVrDevice(device.deviceId) }
                                                        .onFailure { error = context.userMessage(it.message) }
                                                    reload()
                                                    confirming = null
                                                    unlinking = null
                                                }
                                            }
                                            .padding(vertical = 6.dp),
                                    )
                                    if (asking && !busy) {
                                        Text(
                                            stringResource(R.string.cancel),
                                            style = HinvrTypography.titleMedium,
                                            color = colors.ink,
                                            modifier = Modifier
                                                .clickable { confirming = null }
                                                .padding(vertical = 6.dp),
                                        )
                                    }
                                }
                                if (asking && !busy) {
                                    Text(
                                        stringResource(R.string.vr_unlink_body),
                                        style = HinvrTypography.bodyMedium,
                                        color = colors.inkMuted,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun shortDate(iso: String): String {
    val instant = runCatching { OffsetDateTime.parse(iso).toInstant() }.getOrNull() ?: return iso
    return DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
        .format(instant.atZone(ZoneId.systemDefault()))
}
