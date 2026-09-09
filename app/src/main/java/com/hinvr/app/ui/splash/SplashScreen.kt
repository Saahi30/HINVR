package com.hinvr.app.ui.splash

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.TransformOrigin
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
import com.hinvr.app.R
import com.hinvr.app.navigation.LocalCatalogRepository
import com.hinvr.app.navigation.LocalSessionRepository
import com.hinvr.app.navigation.startRoute
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

private val SealShape = RoundedCornerShape(percent = 50)
private val GoldKnockout = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0.30f, 0.52f, 0.18f, 0f, -0.04f,
        ),
    ),
)

/**
 * Sanctum launch. Hall first, gold seal second, wordmark last.
 * Skippable after 800ms; auto-advance at 2500ms.
 */
@Composable
fun SplashScreen(onFinished: (String) -> Unit) {
    val session = LocalSessionRepository.current
    val catalog = LocalCatalogRepository.current
    val context = LocalContext.current
    val finishOnce = rememberUpdatedState(onFinished)
    var navigated by remember { mutableStateOf(false) }
    var canSkip by remember { mutableStateOf(false) }
    var lightDiya by remember { mutableStateOf(false) }
    var showSeal by remember { mutableStateOf(false) }
    var showMark by remember { mutableStateOf(false) }

    val bellId = remember {
        context.resources.getIdentifier("temple_bell", "raw", context.packageName)
    }

    val go: () -> Unit = remember {
        {
            if (!navigated) navigated = true
        }
    }

    LaunchedEffect(navigated) {
        if (!navigated) return@LaunchedEffect
        runCatching { session.syncRemote() }
        runCatching { catalog.refresh() }
        val route = session.snapshot.first().startRoute()
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

    DisposableEffect(bellId) {
        val player = if (bellId != 0) {
            runCatching {
                MediaPlayer.create(context, bellId)?.apply {
                    setOnCompletionListener { release() }
                    start()
                }
            }.getOrNull()
        } else {
            null
        }
        onDispose { runCatching { player?.release() } }
    }

    SplashCanvas(
        showSeal = showSeal,
        lightDiya = lightDiya,
        showMark = showMark,
        onSkip = { if (canSkip) go() },
    )
}

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
            GaneshaSeal(
                modifier = Modifier.graphicsLayer {
                    alpha = sealAlpha
                    scaleX = sealScale
                    scaleY = sealScale
                    transformOrigin = TransformOrigin.Center
                },
            )
            Spacer(Modifier.height(20.dp))
            DiyaFlame(lit = flame, size = 64.dp)
            Spacer(Modifier.height(10.dp))
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

@Composable
private fun GaneshaSeal(modifier: Modifier = Modifier) {
    val colors = HinvrTheme.colors
    Box(
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
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ganesha_stencil),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 28.dp),
            contentScale = ContentScale.Fit,
            colorFilter = GoldKnockout,
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 22.dp)
                .width(36.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(50))
                .background(colors.gold.copy(alpha = 0.45f)),
        )
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
