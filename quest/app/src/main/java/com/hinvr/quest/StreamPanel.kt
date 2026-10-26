package com.hinvr.quest

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import java.util.WeakHashMap
import kotlinx.coroutines.delay

private const val PlayerOrigin = "https://hinvr.app/"
private const val Tag = "HinvrPlayer"

@Composable
fun StreamPanel() {
    val source = Ring.stream
    val active = source != null && !Ring.leaving && !AudioMix.held
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(Ink),
    ) {
        if (source == null) return@Column
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StreamBadge(source.live, source.spherical)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(streamHeading(source)),
                        color = Gold,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.4.sp,
                    )
                }
                Text(
                    source.title.ifBlank { stringResource(R.string.stream_untitled) },
                    color = Cream,
                    fontFamily = FontFamily.Serif,
                    fontSize = 28.sp,
                    maxLines = 1,
                )
                Text(
                    stringResource(if (source.spherical) R.string.stream_credit_360 else R.string.stream_credit_official),
                    color = CreamMuted,
                    fontSize = 15.sp,
                )
                if (source.spherical) {
                    Text(stringResource(R.string.stream_drag_hint), color = CreamMuted, fontSize = 15.sp)
                }
            }
            Pill(stringResource(R.string.action_back)) { Ring.leaveStream() }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Ink),
            contentAlignment = Alignment.Center,
        ) {
            YoutubePlayer(source.url, active, Modifier.fillMaxSize())
        }
    }
}

@StringRes
fun streamHeading(source: StreamSource): Int = when {
    source.spherical -> R.string.stream_heading_360
    source.live == LiveState.Live -> R.string.stream_heading_live
    source.live == LiveState.NotLive -> R.string.stream_heading_recorded
    else -> R.string.stream_heading_official
}

/** LIVE only when the server's check says so. Everything else says what it is. */
@Composable
fun StreamBadge(live: LiveState, spherical: Boolean = false, size: TextUnit = 14.sp) {
    val (label, fill, ink) = when {
        spherical -> Triple(R.string.badge_recorded_360, Gold, Ink)
        live == LiveState.Live -> Triple(R.string.badge_live, LiveRed, Cream)
        live == LiveState.NotLive -> Triple(R.string.badge_not_live, InkShade, CreamMuted)
        else -> Triple(R.string.badge_stream, InkShade, CreamMuted)
    }
    Text(
        stringResource(label),
        color = ink,
        fontSize = size,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        modifier = Modifier
            .clip(PillShape)
            .background(fill)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

private enum class PlayerStatus { Loading, Playing, Error }

/**
 * A YouTube embed with the standard IFrame API and its own controls. It pauses whenever
 * [active] is false (hidden panel, paused app), and reports playing so ambience can duck.
 */
@Composable
fun YoutubePlayer(url: String, active: Boolean, modifier: Modifier, textScale: Float = 1f) {
    val ref = remember(url) { YouTube.parse(url) }
    var generation by remember(url) { mutableIntStateOf(0) }
    var status by remember(url, generation) { mutableStateOf(PlayerStatus.Loading) }
    var errorCode by remember(url, generation) { mutableIntStateOf(0) }
    Box(modifier, contentAlignment = Alignment.Center) {
        if (ref == null) {
            PlayerMessage(R.string.stream_none_title, R.string.stream_none_body, textScale)
            return@Box
        }
        key(generation) {
            val handle = remember { PlayerHandle() }
            handle.onStatus = { kind, value ->
                when (kind) {
                    // Once the player is up it shows its own buffering and play button.
                    "ready" -> if (status == PlayerStatus.Loading) status = PlayerStatus.Playing
                    "state" -> if (value == 1) status = PlayerStatus.Playing
                    "error" -> {
                        errorCode = value
                        status = PlayerStatus.Error
                    }
                    "neterror" -> {
                        errorCode = -1
                        status = PlayerStatus.Error
                    }
                    "gone" -> generation++
                }
            }
            DisposableEffect(handle) {
                onDispose { handle.release() }
            }
            LaunchedEffect(handle) {
                // The embed script never arrived: say so instead of a black box forever.
                delay(20_000L)
                if (status == PlayerStatus.Loading) {
                    errorCode = -1
                    status = PlayerStatus.Error
                }
            }
            YoutubeWebView(ref, handle, active, Modifier.fillMaxSize())
        }
        when (status) {
            PlayerStatus.Loading -> PlayerMessage(R.string.stream_loading, null, textScale, translucent = true)
            PlayerStatus.Playing -> Unit
            PlayerStatus.Error -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Ink)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    stringResource(R.string.stream_error_title),
                    color = Cream,
                    fontFamily = FontFamily.Serif,
                    fontSize = 28.sp * textScale,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(
                        when (errorCode) {
                            -1 -> R.string.err_network
                            100 -> R.string.stream_error_gone
                            101, 150 -> R.string.stream_error_embed
                            else -> R.string.stream_error_generic
                        },
                    ),
                    color = CreamMuted,
                    fontSize = 17.sp * textScale,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Spacer(Modifier.height(16.dp))
                Pill(stringResource(R.string.action_try_again)) { generation++ }
            }
        }
    }
}

@Composable
private fun PlayerMessage(@StringRes title: Int, @StringRes body: Int?, textScale: Float, translucent: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(20.dp)
            .background(if (translucent) InkShade else Color.Transparent, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text(
            stringResource(title),
            color = Cream,
            fontFamily = FontFamily.Serif,
            fontSize = 26.sp * textScale,
            textAlign = TextAlign.Center,
        )
        if (body != null) {
            Text(
                stringResource(body),
                color = CreamMuted,
                fontSize = 17.sp * textScale,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/** One embedded player. Lives as long as its WebView. */
class PlayerHandle {
    private val main = Handler(Looper.getMainLooper())
    var view: WebView? = null
    var onStatus: (String, Int) -> Unit = { _, _ -> }
    private var active: Boolean? = null
    var playing = false
        private set

    fun setActive(on: Boolean) {
        if (active == on) return
        active = on
        if (!on) setPlaying(false)
        view?.evaluateJavascript("window.hinvrSetActive && hinvrSetActive($on)", null)
    }

    val wantsPlay: Boolean get() = active == true

    fun applyVolume() {
        view?.evaluateJavascript("window.hinvrSetVolume && hinvrSetVolume(${(AudioMix.level.gain * 100).toInt()})", null)
    }

    @JavascriptInterface
    fun onEvent(kind: String, value: Int) {
        main.post {
            if (kind == "state") setPlaying(value == 1 && active == true)
            onStatus(kind, value)
        }
    }

    fun gone() {
        setPlaying(false)
        onStatus("gone", 0)
    }

    private fun setPlaying(now: Boolean) {
        if (playing == now) return
        playing = now
        WebPlayers.playingChanged()
    }

    fun release() {
        setPlaying(false)
        WebPlayers.remove(this)
        view = null
    }
}

/** Every live player, so the activity can pause them all and the mix can duck under them. */
object WebPlayers {
    private val handles = WeakHashMap<PlayerHandle, Unit>()
    private var paused = false

    fun add(handle: PlayerHandle) {
        handles[handle] = Unit
        if (paused) handle.view?.onPause()
    }

    fun remove(handle: PlayerHandle) {
        handles.remove(handle)
        playingChanged()
    }

    fun playingChanged() {
        AudioMix.streamPlaying = handles.keys.any { it.playing }
    }

    fun applyVolume() {
        handles.keys.forEach { it.applyVolume() }
    }

    fun pauseAll() {
        if (paused) return
        paused = true
        handles.keys.forEach { handle ->
            handle.view?.evaluateJavascript("window.hinvrSetActive && hinvrSetActive(false)", null)
            handle.view?.onPause()
        }
        // Timers are process-wide; one call pauses every WebView's JavaScript.
        handles.keys.firstOrNull()?.view?.pauseTimers()
        playingChanged()
    }

    fun resumeAll() {
        if (!paused) return
        paused = false
        handles.keys.firstOrNull()?.view?.resumeTimers()
        handles.keys.forEach { handle ->
            handle.view?.onResume()
            handle.view?.evaluateJavascript("window.hinvrSetActive && hinvrSetActive(${handle.wantsPlay})", null)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
private fun YoutubeWebView(ref: YoutubeRef, handle: PlayerHandle, active: Boolean, modifier: Modifier) {
    val html = remember(ref) { youtubePlayerHtml(ref, (AudioMix.level.gain * 100).toInt()) }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                setBackgroundColor("#100B08".toColorInt())
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    loadsImagesAutomatically = true
                    javaScriptCanOpenWindowsAutomatically = false
                    setSupportMultipleWindows(false)
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    cacheMode = WebSettings.LOAD_DEFAULT
                    allowFileAccess = false
                    allowContentAccess = false
                }
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                webChromeClient = WebChromeClient()
                webViewClient = PlayerClient(handle)
                addJavascriptInterface(handle, "HinvrPlayer")
                handle.view = this
                WebPlayers.add(handle)
                loadDataWithBaseURL(PlayerOrigin, html, "text/html", "UTF-8", null)
            }
        },
        update = { handle.setActive(active) },
        onRelease = { view ->
            handle.release()
            runCatching {
                view.removeJavascriptInterface("HinvrPlayer")
                view.loadUrl("about:blank")
                view.stopLoading()
                (view.parent as? ViewGroup)?.removeView(view)
                view.destroy()
            }
        },
    )
}

private class PlayerClient(private val handle: PlayerHandle) : WebViewClient() {
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

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        if (request.url?.toString().orEmpty().contains("youtube.com/iframe_api")) {
            Log.w(Tag, "Player script failed: ${error.errorCode} ${error.description}")
            handle.onStatus("neterror", error.errorCode)
        }
    }

    /** The WebView's renderer died (often memory). Drop this player and build a new one; keep the app. */
    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        Log.w(Tag, "Renderer gone (crash=${detail.didCrash()}); recreating the player")
        handle.gone()
        return true
    }
}

private fun youtubePlayerHtml(ref: YoutubeRef, volume: Int): String {
    val params = "enablejsapi=1&autoplay=1&controls=1&playsinline=1&rel=0&origin=https%3A%2F%2Fhinvr.app"
    val src = when (ref) {
        is YoutubeRef.Video -> "https://www.youtube-nocookie.com/embed/${ref.id}?$params"
        is YoutubeRef.Channel -> "https://www.youtube-nocookie.com/embed/live_stream?channel=${ref.id}&$params"
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
            <iframe id="player"
              src="$src"
              title="HINVR darshan"
              allow="accelerometer; autoplay; encrypted-media; fullscreen; gyroscope"
              referrerpolicy="strict-origin-when-cross-origin"
              allowfullscreen>
            </iframe>
            <script>
              var player = null, wanted = true, volume = $volume;
              function report(kind, value) { try { HinvrPlayer.onEvent(kind, value); } catch (e) {} }
              function apply() {
                if (!player || !player.playVideo) return;
                if (volume <= 0) { player.mute(); } else { player.unMute(); player.setVolume(volume); }
                if (wanted) { player.playVideo(); } else { player.pauseVideo(); }
              }
              window.hinvrSetActive = function (on) { wanted = on; apply(); };
              window.hinvrSetVolume = function (v) { volume = v; apply(); };
              window.onYouTubeIframeAPIReady = function () {
                player = new YT.Player('player', { events: {
                  onReady: function () { report('ready', 0); apply(); },
                  onStateChange: function (e) { report('state', e.data); },
                  onError: function (e) { report('error', e.data); }
                } });
              };
            </script>
            <script src="https://www.youtube.com/iframe_api"></script>
          </body>
        </html>
    """.trimIndent()
}
