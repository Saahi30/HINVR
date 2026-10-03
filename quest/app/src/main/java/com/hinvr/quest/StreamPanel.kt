package com.hinvr.quest

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

private const val PlayerOrigin = "https://hinvr.app/"

private val Stone = Color(0xFF100B08)
private val Cream = Color(0xFFF3E6D0)
private val CreamMuted = Color(0xFFD9C7A8)
private val Gold = Color(0xFFC9A227)

@Composable
fun StreamPanel() {
    val url = Ring.watchingUrl.orEmpty()
    val title = Ring.watchingTitle
    val spherical = Ring.watchingSpherical
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(Stone),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (spherical) "VR360" else "LIVE DARSHAN",
                    color = Gold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.4.sp,
                )
                Text(
                    title.ifBlank { "Darshan" },
                    color = Cream,
                    fontFamily = FontFamily.Serif,
                    fontSize = 26.sp,
                )
                if (spherical) {
                    Text(
                        "Drag inside the picture to look around.",
                        color = CreamMuted,
                        fontSize = 14.sp,
                    )
                }
            }
            Text(
                "Back",
                color = Stone,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Gold)
                    .clickable { Ring.leaveStream() }
                    .padding(horizontal = 18.dp, vertical = 8.dp),
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Stone),
            contentAlignment = Alignment.Center,
        ) {
            if (url.isBlank()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No official stream",
                        color = Cream,
                        fontFamily = FontFamily.Serif,
                        fontSize = 28.sp,
                    )
                    Text(
                        "There is no live darshan for this mandir yet.",
                        color = CreamMuted,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            } else {
                YoutubeWebView(url, spherical, Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun LivePlayer(url: String, spherical: Boolean, modifier: Modifier) {
    YoutubeWebView(url, spherical, modifier)
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YoutubeWebView(url: String, spherical: Boolean, modifier: Modifier) {
    val html = remember(url, spherical) { youtubePlayerHtml(url, spherical) }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                setBackgroundColor(AndroidColor.parseColor("#100B08"))
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
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
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val target = request.url?.toString().orEmpty()
                        if (target.startsWith("intent:") ||
                            target.startsWith("market:") ||
                            target.startsWith("vnd.youtube") ||
                            target.startsWith("youtube://")
                        ) {
                            return true
                        }
                        if (!request.isForMainFrame) return false
                        return target != "about:blank" && !target.startsWith(PlayerOrigin)
                    }
                }
            }
        },
        update = { view ->
            if (html.isBlank()) {
                if (view.tag != "blank") {
                    view.tag = "blank"
                    view.loadUrl("about:blank")
                }
            } else if (view.tag != html) {
                view.tag = html
                view.loadDataWithBaseURL(PlayerOrigin, html, "text/html", "UTF-8", null)
            }
        },
        onRelease = { view ->
            view.loadUrl("about:blank")
            view.stopLoading()
            view.destroy()
        },
    )
}

private fun youtubePlayerHtml(raw: String, spherical: Boolean): String {
    val channel = Regex("""[?&]channel=([\w-]+)""").find(raw)?.groupValues?.get(1).orEmpty()
    val videoId = Regex("""(?:embed/(?!live_stream)|watch\?.*?v=|youtu\.be/)([\w-]{11})""")
        .find(raw)
        ?.groupValues
        ?.get(1)
        .orEmpty()
    val quality = if (spherical) "&vq=hd1080" else ""
    val iframeSrc = when {
        videoId.isNotBlank() ->
            "https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&mute=0&controls=1&rel=0&modestbranding=1&playsinline=1$quality"
        channel.isNotBlank() ->
            "https://www.youtube-nocookie.com/embed/live_stream?channel=$channel&autoplay=1&mute=0&controls=1&rel=0&modestbranding=1&playsinline=1"
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
              allow="accelerometer; autoplay; encrypted-media; fullscreen; gyroscope"
              referrerpolicy="origin"
              allowfullscreen>
            </iframe>
          </body>
        </html>
    """.trimIndent()
}
