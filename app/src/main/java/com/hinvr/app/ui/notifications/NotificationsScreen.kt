package com.hinvr.app.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hinvr.app.R
import com.hinvr.app.data.NoticeRow
import com.hinvr.app.navigation.LocalSessionRepository
import com.hinvr.app.ui.components.HinvrGoldOutlineButton
import com.hinvr.app.ui.components.HinvrBackground
import com.hinvr.app.ui.components.SabhaTopBar
import com.hinvr.app.ui.theme.Atmosphere
import com.hinvr.app.ui.theme.HinvrCardRadius
import com.hinvr.app.ui.theme.HinvrSideInset
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private sealed interface NoticeLoad {
    data object Loading : NoticeLoad
    data object Failed : NoticeLoad
    data class Loaded(val rows: List<NoticeRow>) : NoticeLoad
}

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val session = LocalSessionRepository.current
    val colors = HinvrTheme.colors
    val locale = LocalConfiguration.current.locales[0]
    var load by remember { mutableStateOf<NoticeLoad>(NoticeLoad.Loading) }
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(attempt) {
        load = NoticeLoad.Loading
        load = session.fetchNotifications().fold(
            onSuccess = { NoticeLoad.Loaded(it) },
            onFailure = { NoticeLoad.Failed },
        )
    }
    HinvrBackground(atmosphere = Atmosphere.Sabha) {
        Column(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            SabhaTopBar(title = stringResource(R.string.notifications), onBack = onBack)
            when (val state = load) {
                NoticeLoad.Loading -> Column(Modifier.padding(horizontal = HinvrSideInset).padding(top = 18.dp)) {
                    // Linen skeleton rows, not a spinner.
                    repeat(3) {
                        Spacer(
                            Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clip(RoundedCornerShape(HinvrCardRadius))
                                .background(colors.ivory.copy(alpha = 0.55f)),
                        )
                        Spacer(Modifier.height(14.dp))
                    }
                }
                NoticeLoad.Failed -> Column(Modifier.padding(horizontal = HinvrSideInset).padding(top = 28.dp)) {
                    Text(stringResource(R.string.notifications_failed), style = HinvrTypography.headlineMedium, color = colors.ink)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.notifications_failed_body), style = HinvrTypography.bodyLarge, color = colors.inkMuted)
                    Spacer(Modifier.height(18.dp))
                    HinvrGoldOutlineButton(stringResource(R.string.try_again), onClick = { attempt++ })
                }
                is NoticeLoad.Loaded -> if (state.rows.isEmpty()) {
                    Text(
                        stringResource(R.string.notifications_title),
                        style = HinvrTypography.headlineMedium,
                        color = colors.ink,
                        modifier = Modifier
                            .padding(horizontal = HinvrSideInset)
                            .padding(top = 28.dp),
                    )
                } else {
                    Column(
                        Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = HinvrSideInset)
                            .padding(top = 8.dp, bottom = 28.dp),
                    ) {
                        state.rows.forEach { notice ->
                            Spacer(Modifier.height(18.dp))
                            Text(notice.title, style = HinvrTypography.titleLarge, color = colors.ink)
                            Spacer(Modifier.height(4.dp))
                            Text(notice.body, style = HinvrTypography.bodyMedium, color = colors.inkMuted)
                            val whenLabel = formatNoticeTime(notice.createdAt, locale)
                            if (whenLabel.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(whenLabel, style = HinvrTypography.labelSmall, color = colors.gold)
                            }
                            Spacer(Modifier.height(18.dp))
                            Spacer(
                                Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(colors.gold.copy(alpha = 0.22f)),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatNoticeTime(raw: String, locale: Locale): String {
    val instant = runCatching { Instant.parse(raw) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(raw).toInstant() }.getOrNull()
        ?: return ""
    return DateTimeFormatter.ofPattern("d MMM, h:mm a", locale)
        .format(instant.atZone(ZoneId.systemDefault()))
}
