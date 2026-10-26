package com.hinvr.app.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.hinvr.app.HinvrApplication
import com.hinvr.app.MainActivity
import com.hinvr.app.R
import com.hinvr.app.data.attempt
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

const val PUSH_OPEN_NOTIFICATIONS = "notifications"
private const val CHANNEL_ID = "aarti"

fun ensureAartiChannel(context: Context) {
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    if (manager.getNotificationChannel(CHANNEL_ID) != null) return
    manager.createNotificationChannel(
        NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_aarti),
            NotificationManager.IMPORTANCE_DEFAULT,
        ),
    )
}

fun notificationsAllowed(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < 33) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
}

/**
 * FCM is optional: builds without app/google-services.json never initialise
 * Firebase, and FirebaseMessaging.getInstance() would throw. Every call checks first.
 */
object PushTokens {
    fun available(context: Context): Boolean =
        runCatching { FirebaseApp.getApps(context).isNotEmpty() }.getOrDefault(false)

    suspend fun current(context: Context): String? {
        if (!available(context)) return null
        return attempt { FirebaseMessaging.getInstance().token.awaitResult() }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    suspend fun delete(context: Context) {
        if (!available(context)) return
        attempt { FirebaseMessaging.getInstance().deleteToken().awaitDone() }
    }
}

class HinvrMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        val app = application as? HinvrApplication ?: return
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            app.sessionRepository.registerPush(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"] ?: return
        if (!notificationsAllowed(this)) return
        ensureAartiChannel(this)
        val launch = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open", PUSH_OPEN_NOTIFICATIONS)
        }
        val pending = PendingIntent.getActivity(
            this,
            0,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notice)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        val id = (message.messageId ?: title).hashCode()
        try {
            NotificationManagerCompat.from(this).notify(id, notification)
        } catch (_: SecurityException) {
            // Permission withdrawn between the check above and now. Nothing to show.
        }
    }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCoroutine { cont ->
    addOnCompleteListener { task ->
        // task.result throws on a failed task, so read it only after success.
        val value = if (task.isSuccessful) task.result else null
        if (value != null) cont.resume(value)
        else cont.resumeWithException(task.exception ?: IllegalStateException("Firebase task failed"))
    }
}

private suspend fun Task<Void>.awaitDone() = suspendCoroutine { cont ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) cont.resume(Unit)
        else cont.resumeWithException(task.exception ?: IllegalStateException("Firebase task failed"))
    }
}
