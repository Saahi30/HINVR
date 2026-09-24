package com.hinvr.app.ui.media

import android.annotation.SuppressLint
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

private fun youtubePlayerHtml(playable: YoutubePlayable): String {
    val videoId = playable.videoId.orEmpty()
    val channelId = playable.channelId.orEmpty()
    val iframeSrc = when {
        videoId.isNotBlank() ->
            "https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&mute=1&controls=0&rel=0&modestbranding=1&playsinline=1&fs=0&iv_load_policy=3&disablekb=1&cc_load_policy=0&showinfo=0&enablejsapi=1&origin=https://hinvr.app"
        channelId.isNotBlank() ->
            "https://www.youtube-nocookie.com/embed/live_stream?channel=$channelId&autoplay=1&mute=1&controls=0&rel=0&modestbranding=1&playsinline=1&fs=0&iv_load_policy=3&disablekb=1&cc_load_policy=0&enablejsapi=1&origin=https://hinvr.app"
        else -> return ""
    }.replace("&", "&amp;")
    return """
        <!doctype html>
        <html>
          <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
            <style>
              html, body { margin:0; padding:0; width:100%; height:100%; background:#100B08; overflow:hidden; }
              #crop {
                position:fixed; inset:0; overflow:hidden; background:#100B08;
              }
              iframe {
                position:absolute;
                top:-70px; left:-4%;
                width:108%; height:calc(100% + 140px);
                border:0; background:#100B08;
              }
              #shield {
                position:fixed; inset:0; z-index:4;
                background:transparent;
              }
            </style>
          </head>
          <body>
            <div id="crop">
              <iframe
                src="$iframeSrc"
                title="HINVR darshan"
                allow="autoplay; encrypted-media; fullscreen"
                referrerpolicy="origin"
                allowfullscreen>
              </iframe>
            </div>
            <div id="shield"></div>
            <script>
              var player;
              function readyApi() {
                if (!window.YT || !YT.Player) return;
                player = new YT.Player(document.querySelector('iframe'), {
                  events: {
                    onReady: function(e) {
                      try { e.target.mute(); e.target.playVideo(); } catch (err) {}
                      try { e.target.unMute(); e.target.setVolume(100); } catch (err) {}
                    }
                  }
                });
              }
              document.getElementById('shield').addEventListener('click', function() {
                if (!player) return;
                try { player.unMute(); player.playVideo(); } catch (err) {}
              });
              var tag = document.createElement('script');
              tag.src = 'https://www.youtube.com/iframe_api';
              document.body.appendChild(tag);
              window.onYouTubeIframeAPIReady = readyApi;
            </script>
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
        return url != "about:blank" && !url.startsWith(PlayerOrigin)
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
                            mediaPlaybackRequiresUserGesture = false
                            loadsImagesAutomatically = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            javaScriptCanOpenWindowsAutomatically = false
                            setSupportMultipleWindows(false)
                            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            cacheMode = WebSettings.LOAD_DEFAULT
                            userAgentString =
                                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
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
