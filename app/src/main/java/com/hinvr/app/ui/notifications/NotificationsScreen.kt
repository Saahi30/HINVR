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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hinvr.app.R
import com.hinvr.app.data.NoticeRow
import com.hinvr.app.navigation.LocalSessionRepository
import com.hinvr.app.ui.components.HinvrBackground
import com.hinvr.app.ui.components.SabhaTopBar
import com.hinvr.app.ui.theme.Atmosphere
import com.hinvr.app.ui.theme.HinvrSideInset
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val session = LocalSessionRepository.current
    val colors = HinvrTheme.colors
    var notices by remember { mutableStateOf<List<NoticeRow>>(emptyList()) }
    LaunchedEffect(Unit) {
        notices = session.fetchNotifications()
    }
    HinvrBackground(atmosphere = Atmosphere.Sabha) {
        Column(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            SabhaTopBar(title = stringResource(R.string.notifications), onBack = onBack)
            if (notices.isEmpty()) {
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
                    notices.forEach { notice ->
                        Spacer(Modifier.height(18.dp))
                        Text(notice.title, style = HinvrTypography.titleLarge, color = colors.ink)
                        Spacer(Modifier.height(4.dp))
                        Text(notice.body, style = HinvrTypography.bodyMedium, color = colors.inkMuted)
                        val whenLabel = formatNoticeTime(notice.createdAt)
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

private fun formatNoticeTime(raw: String): String {
    val instant = runCatching { Instant.parse(raw) }.getOrNull() ?: return ""
    return DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.getDefault())
        .format(instant.atZone(ZoneId.systemDefault()))
}
