package com.hinvr.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.hinvr.app.R
import com.hinvr.app.data.Ago
import com.hinvr.app.data.DeskRequest
import com.hinvr.app.data.DeskRequestState
import com.hinvr.app.data.agoOf
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

/** The wall clock, ticking every [stepMs], so "5 min ago" stays true while a screen is open. */
@Composable
fun rememberNow(stepMs: Long = 30_000L): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(stepMs) {
        while (true) {
            delay(stepMs)
            now = System.currentTimeMillis()
        }
    }
    return now
}

/** "Just now", "12 min ago", "3 h ago", then the date. Real time, in the app's language. */
@Composable
fun agoLabel(epochMs: Long, now: Long = rememberNow()): String {
    val locale = LocalConfiguration.current.locales[0]
    return when (val ago = agoOf(epochMs, now)) {
        Ago.JustNow -> stringResource(R.string.time_just_now)
        is Ago.Minutes -> stringResource(R.string.time_minutes_ago, ago.count.toInt())
        is Ago.Hours -> stringResource(R.string.time_hours_ago, ago.count.toInt())
        is Ago.OnDate -> DateTimeFormatter.ofPattern("d MMM", locale)
            .format(Instant.ofEpochMilli(ago.epochMs).atZone(ZoneId.systemDefault()))
    }
}

@Composable
fun deskKindLabel(kind: String): String = when (kind) {
    "VISIT" -> stringResource(R.string.desk_kind_visit)
    "POOJA" -> stringResource(R.string.desk_kind_pooja)
    "YATRA" -> stringResource(R.string.desk_kind_yatra)
    else -> stringResource(R.string.desk_request)
}

/** One request and the honest state of its delivery to the desk. */
@Composable
fun DeskRequestCard(
    request: DeskRequest,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HinvrTheme.colors
    Column(modifier) {
        Text(agoLabel(request.createdAt), style = HinvrTypography.labelSmall, color = colors.inkMuted)
        Spacer(Modifier.height(8.dp))
        IvoryCard {
            Text(deskKindLabel(request.kind), style = HinvrTypography.titleMedium, color = colors.ink)
            Spacer(Modifier.height(6.dp))
            Text(request.summary, style = HinvrTypography.bodyLarge, color = colors.inkMuted)
            Spacer(Modifier.height(8.dp))
            DeskStateLine(request, onRetry)
        }
    }
}

@Composable
fun DeskStateLine(request: DeskRequest, onRetry: () -> Unit) {
    val colors = HinvrTheme.colors
    when (request.state) {
        DeskRequestState.Sending -> Text(
            stringResource(R.string.sending),
            style = HinvrTypography.labelSmall,
            color = colors.inkMuted,
        )
        DeskRequestState.Sent -> Text(
            if (request.city.isNotBlank()) {
                stringResource(R.string.sent_desk, request.city)
            } else {
                stringResource(R.string.desk_state_sent)
            },
            style = HinvrTypography.labelSmall,
            color = colors.gold,
        )
        DeskRequestState.Waiting -> {
            Text(stringResource(R.string.desk_state_waiting), style = HinvrTypography.labelSmall, color = colors.saffron)
            RetryLink(onRetry)
        }
        DeskRequestState.Failed -> {
            Text(stringResource(R.string.desk_state_failed), style = HinvrTypography.labelSmall, color = colors.vermillion)
            RetryLink(onRetry)
        }
    }
}

@Composable
private fun RetryLink(onRetry: () -> Unit) {
    Text(
        stringResource(R.string.try_again),
        style = HinvrTypography.labelLarge,
        color = HinvrTheme.colors.ink,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onRetry)
            .padding(vertical = 14.dp),
    )
}
