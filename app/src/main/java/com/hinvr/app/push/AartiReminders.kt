package com.hinvr.app.push

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import com.hinvr.app.MainActivity
import com.hinvr.app.R
import com.hinvr.app.data.AartiSchedule
import com.hinvr.app.ui.catalog.Mandir
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

const val PUSH_OPEN_AARTI = "aarti"
const val EXTRA_MANDIR = "mandir"
const val EXTRA_VIEW_IN_VR = "vr"
private const val REMINDER_CHANNEL = "aarti_reminders"

/** One mandir the member wants a reminder for, with what it takes to remind them offline. */
@Serializable
data class AartiReminder(val mandirId: String, val name: String, val times: List<String>, val lead: Int)

/**
 * Reminders before aarti, kept on the phone: no server, no push. Each mandir has one inexact
 * alarm for its next aarti; when it fires it shows the reminder and sets the one after.
 */
object AartiReminders {
    private const val Prefs = "hinvr_aarti_reminders"
    private const val Key = "reminders"
    private val json = Json { ignoreUnknownKeys = true }

    private val state = MutableStateFlow<Map<String, AartiReminder>>(emptyMap())
    private var loaded = false

    /** Mandir id to its reminder, for the toggles. */
    fun reminders(context: Context): StateFlow<Map<String, AartiReminder>> {
        load(context)
        return state.asStateFlow()
    }

    /** Turns a mandir's reminder on (or changes its lead) and sets its next alarm. */
    fun set(context: Context, mandir: Mandir, lead: Int) {
        if (mandir.aartiTimes.isEmpty()) return
        val reminder = AartiReminder(mandir.id, mandir.name, mandir.aartiTimes, lead)
        save(context, all(context) + (mandir.id to reminder))
        schedule(context, reminder, Instant.now())
    }

    fun remove(context: Context, mandirId: String) {
        cancel(context, mandirId)
        save(context, all(context) - mandirId)
    }

    /** After boot, a clock or time-zone change, an update, or app start. */
    fun rescheduleAll(context: Context) {
        val now = Instant.now()
        all(context).values.forEach { schedule(context, it, now) }
    }

    /** The desk's catalog was read: follow changed times, and drop mandirs that no longer have any. */
    fun syncWithCatalog(context: Context, mandirs: List<Mandir>) {
        val current = all(context)
        if (current.isEmpty()) return
        val byId = mandirs.associateBy { it.id }
        val kept = current.mapNotNull { (id, reminder) ->
            val mandir = byId[id]
            if (mandir == null || mandir.aartiTimes.isEmpty()) {
                cancel(context, id)
                null
            } else {
                id to reminder.copy(name = mandir.name, times = mandir.aartiTimes)
            }
        }.toMap()
        if (kept != current) save(context, kept)
        val now = Instant.now()
        kept.values.forEach { schedule(context, it, now) }
    }

    internal fun find(context: Context, mandirId: String): AartiReminder? = all(context)[mandirId]

    /** Sets the alarm for the first reminder after [after]. */
    internal fun schedule(context: Context, reminder: AartiReminder, after: Instant) {
        val next = AartiSchedule.next(AartiSchedule.parse(reminder.times), reminder.lead, after) ?: return
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val window = 10 * 60_000L
        // Inexact, so no exact-alarm permission: anywhere within five minutes either side.
        alarms.setWindow(
            AlarmManager.RTC_WAKEUP,
            next.remindAt.toEpochMilli() - window / 2,
            window,
            alarmIntent(context, reminder.mandirId, next.start.toEpochMilli()),
        )
    }

    private fun cancel(context: Context, mandirId: String) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        alarms.cancel(alarmIntent(context, mandirId, 0L))
    }

    private fun alarmIntent(context: Context, mandirId: String, startMs: Long): PendingIntent {
        val intent = Intent(context, AartiAlarmReceiver::class.java)
            .setAction("com.hinvr.app.AARTI.$mandirId")
            .putExtra(EXTRA_MANDIR, mandirId)
            .putExtra(AartiAlarmReceiver.ExtraStart, startMs)
        return PendingIntent.getBroadcast(
            context,
            mandirId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun all(context: Context): Map<String, AartiReminder> {
        load(context)
        return state.value
    }

    private fun load(context: Context) {
        if (loaded) return
        loaded = true
        val raw = context.getSharedPreferences(Prefs, Context.MODE_PRIVATE).getString(Key, null) ?: return
        state.value = runCatching { json.decodeFromString<List<AartiReminder>>(raw) }
            .getOrDefault(emptyList())
            .associateBy { it.mandirId }
    }

    private fun save(context: Context, reminders: Map<String, AartiReminder>) {
        state.value = reminders
        context.getSharedPreferences(Prefs, Context.MODE_PRIVATE).edit {
            putString(Key, json.encodeToString(reminders.values.toList()))
        }
    }
}

fun ensureReminderChannel(context: Context) {
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    if (manager.getNotificationChannel(REMINDER_CHANNEL) != null) return
    manager.createNotificationChannel(
        NotificationChannel(
            REMINDER_CHANNEL,
            context.getString(R.string.notification_channel_reminders),
            NotificationManager.IMPORTANCE_DEFAULT,
        ),
    )
}

/** Shows the reminder for the aarti it was set for, then sets the next one. */
class AartiAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val mandirId = intent.getStringExtra(EXTRA_MANDIR) ?: return
        val startMs = intent.getLongExtra(ExtraStart, 0L)
        val reminder = AartiReminders.find(context, mandirId) ?: return
        val now = Instant.now()
        val start = Instant.ofEpochMilli(startMs)
        if (startMs > 0L && !now.isAfter(start.plus(AartiSchedule.LateBy))) notify(context, reminder, start, now)
        // From just after this reminder's own time, so an early window doesn't pick the same aarti again.
        val reminded = start.minusSeconds(reminder.lead * 60L).plusSeconds(1)
        AartiReminders.schedule(context, reminder, maxOf(now, reminded))
    }

    private fun notify(context: Context, reminder: AartiReminder, start: Instant, now: Instant) {
        if (!notificationsAllowed(context)) return
        ensureReminderChannel(context)
        val at = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault()).format(start)
        val minutes = AartiSchedule.minutesUntil(start, now)
        val title = context.getString(R.string.reminder_title, reminder.name, at)
        val body = if (minutes <= 0) {
            context.getString(R.string.reminder_body_now)
        } else {
            context.resources.getQuantityString(R.plurals.reminder_body_in, minutes, minutes)
        }
        val code = reminder.mandirId.hashCode()
        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_notice)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(open(context, reminder.mandirId, vr = false, code * 2))
            .addAction(0, context.getString(R.string.reminder_watch), open(context, reminder.mandirId, vr = false, code * 2))
            .addAction(0, context.getString(R.string.view_in_vr), open(context, reminder.mandirId, vr = true, code * 2 + 1))
            .build()
        try {
            NotificationManagerCompat.from(context).notify(code, notification)
        } catch (_: SecurityException) {
            // Notifications were turned off between the check and now.
        }
    }

    private fun open(context: Context, mandirId: String, vr: Boolean, requestCode: Int): PendingIntent {
        val launch = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open", PUSH_OPEN_AARTI)
            putExtra(EXTRA_MANDIR, mandirId)
            putExtra(EXTRA_VIEW_IN_VR, vr)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ExtraStart = "start"
    }
}

/** Alarms don't survive a reboot or a clock change; set them again. */
class AartiRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            -> AartiReminders.rescheduleAll(context)
        }
    }
}
