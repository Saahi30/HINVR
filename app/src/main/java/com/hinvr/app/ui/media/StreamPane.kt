package com.hinvr.app.ui.media

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

fun embedMediaUrl(raw: String): String {
    val url = raw.trim()
    if (url.contains("youtube.com/embed/live_stream")) return url

    val channelId = Regex("""youtube\.com/channel/([\w-]+)(?:/live)?""")
        .find(url)
        ?.groupValues
        ?.get(1)
    if (channelId != null) {
        return "https://www.youtube.com/embed/live_stream?channel=$channelId&autoplay=1"
    }

    val id = Regex(
        """(?:youtube\.com/watch\?v=|youtube\.com/embed/|youtube\.com/live/|youtu\.be/)([\w-]{11})""",
    ).find(url)?.groupValues?.get(1)
    return if (id != null) {
        "https://www.youtube.com/embed/$id?rel=0&modestbranding=1&autoplay=1"
    } else {
        url
    }
}

private const val HinvrPlayerOrigin = "https://hinvr.app/"

private fun youtubeEmbedHtml(embedUrl: String): String? {
    if (!embedUrl.startsWith("https://www.youtube.com/embed/")) return null
    val separator = if ("?" in embedUrl) "&" else "?"
    val source = "$embedUrl${separator}playsinline=1&enablejsapi=1&origin=https%3A%2F%2Fhinvr.app"
        .replace("&", "&amp;")
        .replace("\"", "&quot;")
    return """
        <!doctype html>
        <html>
          <head>
            <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
            <meta name="referrer" content="strict-origin-when-cross-origin">
            <style>
              html, body, iframe { width:100%; height:100%; margin:0; background:#100B08; border:0; overflow:hidden; }
            </style>
          </head>
          <body>
            <iframe
              src="$source"
              title="HINVR official stream"
              allow="autoplay; encrypted-media; picture-in-picture; fullscreen"
              referrerpolicy="strict-origin-when-cross-origin"
              allowfullscreen>
            </iframe>
          </body>
        </html>
    """.trimIndent()
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StreamPane(url: String, modifier: Modifier = Modifier) {
    val embed = remember(url) { embedMediaUrl(url) }
    val youtubeHtml = remember(embed) { youtubeEmbedHtml(embed) }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.loadsImagesAutomatically = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                setBackgroundColor(Color.TRANSPARENT)
                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()
            }
        },
        update = { view ->
            if (view.tag != embed) {
                view.tag = embed
                if (youtubeHtml != null) {
                    view.loadDataWithBaseURL(
                        HinvrPlayerOrigin,
                        youtubeHtml,
                        "text/html",
                        "UTF-8",
                        null,
                    )
                } else {
                    view.loadUrl(embed, mapOf("Referer" to HinvrPlayerOrigin))
                }
            }
        },
        onRelease = { view ->
            view.stopLoading()
            view.destroy()
        },
    )
}
