package com.hinvr.app.ui.media

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.hinvr.app.ui.theme.TempleStoneDeep

private const val PlayerOrigin = "https://hinvr.app/"

/**
 * Known @handle live pages that would otherwise open the full YouTube site.
 * Map them to a video id so playback stays inside HINVR.
 */
private val HandleLiveIds = mapOf(
    "saibabasansthantrust" to "6Uf3aMujHa8",
)

internal data class YoutubePlayable(
    val videoId: String? = null,
    val channelId: String? = null,
)

fun embedMediaUrl(raw: String): String {
    val playable = parseYoutube(raw) ?: return raw.trim()
    return playable.videoId?.let { "https://www.youtube.com/embed/$it" }
        ?: playable.channelId?.let { "https://www.youtube.com/embed/live_stream?channel=$it" }
        ?: raw.trim()
}

internal fun parseYoutube(raw: String): YoutubePlayable? {
    val url = raw.trim()
    if (url.isEmpty()) return null
    if (!url.contains("youtu", ignoreCase = true)) return null

    Regex("""youtube\.com/@([\w.-]+)""")
        .find(url)
        ?.groupValues
        ?.get(1)
        ?.lowercase()
        ?.let { handle -> HandleLiveIds[handle]?.let { return YoutubePlayable(videoId = it) } }

    Regex("""[?&]channel=([\w-]+)""")
        .find(url)
        ?.groupValues
        ?.get(1)
        ?.let { return YoutubePlayable(channelId = it) }

    Regex("""youtube\.com/channel/([\w-]+)""")
        .find(url)
        ?.groupValues
        ?.get(1)
        ?.let { return YoutubePlayable(channelId = it) }

    val videoId = Regex(
        """(?:youtube(?:-nocookie)?\.com/(?:embed/(?!live_stream)|watch\?.*?v=|live/|shorts/|v/)|youtu\.be/)([\w-]{11})""",
    ).find(url)?.groupValues?.get(1)

    return videoId?.let { YoutubePlayable(videoId = it) }
}

/**
 * The standard YouTube embed, used the way YouTube intends: full player chrome and
 * controls, nothing laid over it, the app's own browser identity. It starts muted
 * (the only autoplay browsers allow) and the member unmutes from the player.
 * The fullscreen button is off because the pane already fills the screen sideways.
 */
private fun youtubePlayerHtml(playable: YoutubePlayable): String {
    val videoId = playable.videoId.orEmpty()
    val channelId = playable.channelId.orEmpty()
    val params = "autoplay=1&mute=1&controls=1&playsinline=1&rel=0&fs=0&origin=https://hinvr.app"
    val iframeSrc = when {
        videoId.isNotBlank() -> "https://www.youtube-nocookie.com/embed/$videoId?$params"
        channelId.isNotBlank() -> "https://www.youtube-nocookie.com/embed/live_stream?channel=$channelId&$params"
        else -> return ""
    }.replace("&", "&amp;")
    return """
        <!doctype html>
        <html>
          <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width,initial-scale=1">
            <style>
              html, body { margin:0; padding:0; width:100%; height:100%; background:#100B08; overflow:hidden; }
              iframe { position:fixed; inset:0; width:100%; height:100%; border:0; background:#100B08; }
            </style>
          </head>
          <body>
            <iframe
              src="$iframeSrc"
              title="HINVR darshan"
              allow="autoplay; encrypted-media; picture-in-picture"
              referrerpolicy="strict-origin-when-cross-origin">
            </iframe>
          </body>
        </html>
    """.trimIndent()
}

private class InAppStreamClient : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url?.toString().orEmpty()
        if (url.startsWith("intent:") ||
            url.startsWith("market:") ||
            url.startsWith("vnd.youtube") ||
            url.startsWith("youtube://")
        ) {
            return true
        }
        if (!request.isForMainFrame) return false
        if (url == "about:blank" || url.startsWith(PlayerOrigin)) return false
        // "Watch on YouTube" and the logo leave the player: hand them to YouTube or
        // the browser instead of loading the full site inside the darshan pane.
        if (url.startsWith("https://")) {
            runCatching {
                view.context.startActivity(
                    Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
        return true
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StreamPane(url: String, modifier: Modifier = Modifier) {
    val playable = remember(url) { parseYoutube(url) }
    val html = remember(playable) { playable?.let { youtubePlayerHtml(it) }.orEmpty() }
    Box(modifier.background(TempleStoneDeep)) {
        if (html.isNotBlank()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        setBackgroundColor(Color.parseColor("#100B08"))
                        isVerticalScrollBarEnabled = false
                        isHorizontalScrollBarEnabled = false
                        isNestedScrollingEnabled = false
                        overScrollMode = WebView.OVER_SCROLL_NEVER
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            // Lets the muted start happen; sound still waits for the member.
                            mediaPlaybackRequiresUserGesture = false
                            loadsImagesAutomatically = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            javaScriptCanOpenWindowsAutomatically = false
                            setSupportMultipleWindows(false)
                            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            cacheMode = WebSettings.LOAD_DEFAULT
                            // The WebView's own user agent: no desktop disguise.
                        }
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webChromeClient = object : WebChromeClient() {
                            override fun onCreateWindow(
                                view: WebView?,
                                isDialog: Boolean,
                                isUserGesture: Boolean,
                                resultMsg: android.os.Message?,
                            ): Boolean = false
                        }
                        webViewClient = InAppStreamClient()
                    }
                },
                update = { view ->
                    if (view.tag != html) {
                        view.tag = html
                        view.loadDataWithBaseURL(
                            PlayerOrigin,
                            html,
                            "text/html",
                            "UTF-8",
                            null,
                        )
                    }
                },
                onRelease = { view ->
                    view.loadUrl("about:blank")
                    view.stopLoading()
                    view.destroy()
                },
            )
        }
    }
}
