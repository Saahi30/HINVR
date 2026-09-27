package com.hinvr.app.ui.plans

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.hinvr.app.data.AuthException
import com.hinvr.app.data.SessionRepository

/** Opens the system browser. Membership is managed there, never in a WebView. */
fun openDeviceBrowser(context: Context, url: String): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

/** Signs the current member into the membership site, then opens it in the browser. */
suspend fun openMembershipInBrowser(context: Context, session: SessionRepository): String? {
    val url = try {
        session.membershipPageUrl()
    } catch (e: AuthException) {
        return e.message ?: "Sign in again."
    } catch (_: Exception) {
        return "Couldn't open membership."
    }
    return if (openDeviceBrowser(context, url)) null else "Couldn't open the browser."
}
