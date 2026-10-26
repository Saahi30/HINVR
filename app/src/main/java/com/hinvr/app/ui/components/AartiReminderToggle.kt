package com.hinvr.app.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hinvr.app.R
import com.hinvr.app.data.AartiSchedule
import com.hinvr.app.push.AartiReminders
import com.hinvr.app.push.notificationsAllowed
import com.hinvr.app.ui.catalog.Mandir
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * "Remind me before aarti" for one mandir, kept on this phone. Shows the aarti times in the
 * phone's own time, says when they are India time, and the lead: 10, 15 or 30 minutes.
 */
@Composable
fun AartiReminderToggle(mandir: Mandir, modifier: Modifier = Modifier, onDark: Boolean = false) {
    val context = LocalContext.current
    val colors = HinvrTheme.colors
    val reminders by remember(context) { AartiReminders.reminders(context) }.collectAsStateWithLifecycle()
    val reminder = reminders[mandir.id]
    var denied by remember { mutableStateOf(false) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        denied = !granted
    }
    val ink = if (onDark) colors.cream else colors.ink
    val muted = if (onDark) colors.creamMuted else colors.inkMuted

    fun turnOn() {
        AartiReminders.set(context, mandir, reminder?.lead ?: AartiSchedule.DefaultLead)
        if (Build.VERSION.SDK_INT >= 33 && !notificationsAllowed(context)) ask.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.reminder_toggle), style = HinvrTypography.titleMedium, color = ink)
                Text(aartiTimesLine(mandir.aartiTimes), style = HinvrTypography.bodyMedium, color = muted)
            }
            Switch(
                checked = reminder != null,
                onCheckedChange = { on -> if (on) turnOn() else AartiReminders.remove(context, mandir.id) },
                colors = SwitchDefaults.colors(checkedThumbColor = colors.ivory, checkedTrackColor = colors.gold),
            )
        }
        if (reminder != null) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AartiSchedule.Leads.forEach { lead ->
                    val chosen = reminder.lead == lead
                    Text(
                        stringResource(R.string.reminder_lead, lead),
                        style = HinvrTypography.labelLarge,
                        color = if (chosen) colors.ivory else ink,
                        modifier = Modifier
                            .clip(RoundedCornerShape(2.dp))
                            .then(if (chosen) Modifier.background(colors.gold) else Modifier.border(1.dp, colors.gold, RoundedCornerShape(2.dp)))
                            .clickable { AartiReminders.set(context, mandir, lead) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
            if (denied || !notificationsAllowed(context)) {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.reminder_denied), style = HinvrTypography.bodyMedium, color = muted)
            }
        }
    }
}

/** "Aarti at 5:00 AM, 7:00 PM", in the phone's time; "(India time)" only when the phone isn't on it. */
@Composable
private fun aartiTimesLine(times: List<String>): String {
    val zone = ZoneId.systemDefault()
    val format = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(zone)
    val today = java.time.LocalDate.now(AartiSchedule.India)
    val shown = AartiSchedule.parse(times).joinToString(", ") { format.format(today.atTime(it).atZone(AartiSchedule.India)) }
    val india = zone.rules.getOffset(java.time.Instant.now()) == AartiSchedule.India.rules.getOffset(java.time.Instant.now())
    return if (india) stringResource(R.string.reminder_times, shown) else stringResource(R.string.reminder_times_local, shown)
}
