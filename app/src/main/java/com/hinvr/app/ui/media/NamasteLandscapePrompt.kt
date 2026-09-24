package com.hinvr.app.ui.media

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hinvr.app.ui.motion.HinvrMotion
import com.hinvr.app.ui.theme.HinvrSideInset
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography

@Composable
fun isViewerPortrait(): Boolean =
    LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT

/**
 * A quiet namaste over the darshan viewer until the member turns the phone sideways.
 */
@Composable
fun NamasteLandscapePrompt(modifier: Modifier = Modifier) {
    val colors = HinvrTheme.colors
    val portrait = isViewerPortrait()
    val bow by rememberInfiniteTransition(label = "namaste-bow").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bow",
    )
    AnimatedVisibility(
        visible = portrait,
        enter = fadeIn(tween(HinvrMotion.Immersive, easing = HinvrMotion.EnterEasing)),
        exit = fadeOut(tween(HinvrMotion.Standard, easing = HinvrMotion.ExitEasing)),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF2100B08)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = HinvrSideInset),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "🙏",
                    fontSize = 64.sp,
                    modifier = Modifier.graphicsLayer {
                        translationY = 8f * bow
                        scaleX = 1f - 0.03f * bow
                        scaleY = 1f - 0.03f * bow
                    },
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    "Namaste.",
                    style = HinvrTypography.headlineMedium,
                    color = colors.cream,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Kindly turn your phone sideways\nfor darshan.",
                    style = HinvrTypography.bodyLarge,
                    color = colors.creamMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
