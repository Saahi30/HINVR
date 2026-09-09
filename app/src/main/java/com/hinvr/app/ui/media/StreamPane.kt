package com.hinvr.app.ui.media

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

fun embedMediaUrl(raw: String): String {
    val url = raw.trim()
    val id = Regex(
        """(?:youtube\.com/watch\?v=|youtube\.com/embed/|youtube\.com/live/|youtu\.be/)([\w-]{11})""",
    ).find(url)?.groupValues?.get(1)
    return if (id != null) {
        "https://www.youtube.com/embed/$id?rel=0&modestbranding=1"
    } else {
        url
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StreamPane(url: String, modifier: Modifier = Modifier) {
    val embed = remember(url) { embedMediaUrl(url) }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                webViewClient = WebViewClient()
                loadUrl(embed)
            }
        },
        update = { view ->
            if (view.url != embed) view.loadUrl(embed)
        },
        onRelease = { view ->
            view.loadUrl("about:blank")
            view.destroy()
        },
    )
}
