package com.hinvr.quest

import android.app.Activity
import android.content.Intent
import android.net.Uri
import java.lang.ref.WeakReference

object Tour360 {
    private const val YoutubeVr = "com.google.android.apps.youtube.vr.oculus"
    private var activity = WeakReference<Activity>(null)

    fun attach(host: Activity) {
        activity = WeakReference(host)
    }

    fun open(url: String): Boolean {
        val host = activity.get() ?: return false
        val videoId = Regex("""(?:embed/|watch\?.*?v=|youtu\.be/)([\w-]{11})""")
            .find(url)
            ?.groupValues
            ?.get(1)
            ?: return false
        val link = Uri.parse("https://www.youtube.com/watch?v=$videoId")
        // YouTube VR has three link handlers; without the immersive one named, Horizon OS shows a chooser.
        val immersive = Intent(Intent.ACTION_VIEW, link)
            .setClassName(YoutubeVr, "com.google.android.apps.youtube.vr.activities.YouTubeVrActivity")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { host.startActivity(immersive) }.isSuccess) return true
        val anyHandler = Intent(Intent.ACTION_VIEW, link)
            .setPackage(YoutubeVr)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { host.startActivity(anyHandler) }.isSuccess
    }
}
