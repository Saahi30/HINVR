package com.hinvr.app.ui.splash

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hinvr.app.HinvrApplication
import com.hinvr.app.R
import com.hinvr.app.data.attempt
import com.hinvr.app.navigation.LocalCatalogRepository
import com.hinvr.app.navigation.LocalSessionRepository
import com.hinvr.app.navigation.startRoute
import com.hinvr.app.ui.components.HinvrSealLetters
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

private val SealShape = RoundedCornerShape(percent = 50)

/** Session and catalog get this long behind the splash; after it, cached data is enough. */
private const val SyncBudgetMs = 3_000L

/**
 * Sanctum launch. Hall first, the HINVR seal second, one diya, the wordmark last.
 * Membership sync and the catalog run alongside the animation. Skippable after
 * 800ms; auto-advance at 2500ms.
 */
@Composable
fun SplashScreen(onFinished: (String) -> Unit) {
    val session = LocalSessionRepository.current
    val catalog = LocalCatalogRepository.current
    val context = LocalContext.current
    val finishOnce = rememberUpdatedState(onFinished)
    val appScope = (context.applicationContext as HinvrApplication).appScope
    var navigated by remember { mutableStateOf(false) }
    var canSkip by remember { mutableStateOf(false) }
    var lightDiya by remember { mutableStateOf(false) }
    var showSeal by remember { mutableStateOf(false) }
    var showMark by remember { mutableStateOf(false) }

    // Started with the first frame, not after the animation, and in the app scope
    // so a slow sync still finishes after the splash has moved on.
    val background = remember {
        listOf(
            appScope.async { attempt { session.syncRemote() } },
            appScope.async { attempt { catalog.refresh() } },
        )
    }

    val go: () -> Unit = remember {
        {
            if (!navigated) navigated = true
        }
    }

    LaunchedEffect(navigated) {
        if (!navigated) return@LaunchedEffect
        // Weak signal at launch: go on with what the phone already has.
        withTimeoutOrNull(SyncBudgetMs) { background.awaitAll() }
        val route = session.snapshot.first { it.loaded }.startRoute()
        finishOnce.value(route)
    }

    LaunchedEffect(Unit) {
        delay(180)
        showSeal = true
        delay(320)
        lightDiya = true
        delay(300)
        canSkip = true
        delay(200)
        showMark = true
        delay(1500)
        go()
    }

    DisposableEffect(Unit) {
        val player = if (bellAllowed(context)) playBellOnce(context) else null
        onDispose { attempt { player?.release() } }
    }

    SplashCanvas(
        showSeal = showSeal,
        lightDiya = lightDiya,
        showMark = showMark,
        onSkip = { if (canSkip) go() },
    )
}

/** One bell, and only when the phone is allowed to ring: not on silent, vibrate or Do Not Disturb. */
private fun bellAllowed(context: Context): Boolean {
    val audio = context.getSystemService(AudioManager::class.java) ?: return false
    if (audio.ringerMode != AudioManager.RINGER_MODE_NORMAL) return false
    val notifications = context.getSystemService(NotificationManager::class.java) ?: return true
    return notifications.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL ||
        notifications.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_UNKNOWN
}

private fun playBellOnce(context: Context): MediaPlayer? = attempt {
    val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    MediaPlayer.create(context, R.raw.temple_bell, attributes, 0)?.apply {
        setOnCompletionListener { it.release() }
        start()
    }
}.getOrNull()

@Composable
private fun SplashCanvas(
    showSeal: Boolean,
    lightDiya: Boolean,
    showMark: Boolean,
    onSkip: () -> Unit,
) {
    val colors = HinvrTheme.colors
    val skip = rememberUpdatedState(onSkip)
    var hallReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { hallReady = true }

    val hallScale by animateFloatAsState(
        targetValue = if (showMark) 1.06f else 1.0f,
        animationSpec = tween(2500, easing = LinearOutSlowInEasing),
        label = "hall-zoom",
    )
    val hallAlpha by animateFloatAsState(
        targetValue = if (hallReady) 1f else 0.35f,
        animationSpec = tween(700),
        label = "hall-fade",
    )
    val sealAlpha by animateFloatAsState(
        targetValue = if (showSeal) 1f else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "seal-fade",
    )
    val sealScale by animateFloatAsState(
        targetValue = if (showSeal) 1f else 0.92f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "seal-scale",
    )
    val flame by animateFloatAsState(
        targetValue = if (lightDiya) 1f else 0.04f,
        animationSpec = tween(1100, easing = FastOutSlowInEasing),
        label = "flame",
    )
    val markAlpha by animateFloatAsState(
        targetValue = if (showMark) 1f else 0f,
        animationSpec = tween(700),
        label = "mark",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.duskDeep)
            .pointerInput(Unit) {
                detectTapGestures { skip.value() }
            },
    ) {
        Image(
            painter = painterResource(R.drawable.splash_background),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = hallAlpha
                    scaleX = hallScale
                    scaleY = hallScale
                    transformOrigin = TransformOrigin.Center
                },
            contentScale = ContentScale.Crop,
        )

        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x66100B08),
                            Color(0xCC100B08),
                        ),
                    ),
                ),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0x88100B08),
                            0.22f to Color.Transparent,
                            0.52f to Color(0x55100B08),
                            0.72f to Color(0xCC100B08),
                            1.00f to Color(0xF5100B08),
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SealPill(
                flame = flame,
                modifier = Modifier.graphicsLayer {
                    alpha = sealAlpha
                    scaleX = sealScale
                    scaleY = sealScale
                    transformOrigin = TransformOrigin.Center
                },
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.splash_wordmark),
                style = HinvrTypography.displayLarge.copy(
                    fontSize = 42.sp,
                    letterSpacing = 14.sp,
                ),
                color = colors.gold,
                modifier = Modifier.alpha(markAlpha),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .width(56.dp)
                    .height(1.5.dp)
                    .alpha(markAlpha)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                colors.gold,
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
        }
    }
}

/** The dark pill from the bible: the HINVR seal above, the one diya below. No deity art. */
@Composable
private fun SealPill(flame: Float, modifier: Modifier = Modifier) {
    val colors = HinvrTheme.colors
    Column(
        modifier = modifier
            .size(width = 168.dp, height = 268.dp)
            .shadow(
                elevation = 28.dp,
                shape = SealShape,
                ambientColor = colors.gold.copy(alpha = 0.35f),
                spotColor = Color.Black,
            )
            .clip(SealShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2C1C12),
                        Color(0xFF120C08),
                        Color(0xFF0A0706),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colors.gold.copy(alpha = 0.55f),
                        colors.gold.copy(alpha = 0.12f),
                        colors.gold.copy(alpha = 0.35f),
                    ),
                ),
                shape = SealShape,
            )
            .padding(top = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF3A2618), Color(0xFF1A110B)),
                    ),
                )
                .drawWithContent {
                    drawContent()
                    drawCircle(
                        color = colors.gold.copy(alpha = 0.7f),
                        radius = size.minDimension / 2f - 6.dp.toPx(),
                        style = Stroke(width = 1.dp.toPx()),
                    )
                }
                .border(1.dp, colors.gold.copy(alpha = 0.85f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            HinvrSealLetters(letterSize = 22.sp, top = colors.cream, bottom = colors.gold)
        }
        Spacer(Modifier.height(10.dp))
        DiyaFlame(lit = flame, size = 96.dp)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800, backgroundColor = 0xFF100B08)
@Composable
private fun SplashCanvasPreview() {
    HinvrTheme {
        SplashCanvas(
            showSeal = true,
            lightDiya = true,
            showMark = true,
            onSkip = {},
        )
    }
}
