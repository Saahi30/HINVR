package com.hinvr.app.ui.onboarding

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hinvr.app.R
import com.hinvr.app.navigation.LocalSessionRepository
import com.hinvr.app.ui.catalog.TileScene
import com.hinvr.app.ui.components.HinvrBackground
import com.hinvr.app.ui.components.HinvrGoldOutlineButton
import com.hinvr.app.ui.components.HinvrPrimaryButton
import com.hinvr.app.ui.illustrations.TempleScene
import com.hinvr.app.ui.theme.Atmosphere
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val kicker: String,
    val title: String,
    val body: String,
    val scene: TileScene?,
    val photo: Boolean = false,
)

@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    val session = LocalSessionRepository.current
    val scope = rememberCoroutineScope()
    val pages = listOf(
        OnboardingPage(
            kicker = "DARSHAN",
            title = "Sit in the sabha from anywhere.",
            body = "Live aarti, still. Official temple streams, not a novelty feed.",
            scene = null,
            photo = true,
        ),
        OnboardingPage(
            kicker = "PASS",
            title = "Your name on a temple pass.",
            body = "A credential you can show at the desk. Assist, not unofficial skip-the-line.",
            scene = TileScene.PassDesk,
        ),
        OnboardingPage(
            kicker = "CONCIERGE",
            title = "Ask. Book. Be received.",
            body = "Dates, dress, pandit, a host at the mandir. Someone handles the logistics.",
            scene = TileScene.ConciergeDesk,
        ),
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val colors = HinvrTheme.colors

    fun finish() {
        scope.launch {
            session.completeOnboarding()
            onContinue()
        }
    }

    HinvrBackground(atmosphere = Atmosphere.Sabha, darkIcons = false) {
        Box(Modifier.fillMaxSize()) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { index ->
                val page = pages[index]
                Box(Modifier.fillMaxSize()) {
                    if (page.photo) {
                        Image(
                            painter = painterResource(R.drawable.splash_background),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        TempleScene(page.scene ?: TileScene.Tirupati, Modifier.fillMaxSize())
                    }
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0f to Color(0x66100B08),
                                        0.4f to Color.Transparent,
                                        1f to colors.linen,
                                    ),
                                ),
                            ),
                    )
                }
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            ) {
                Box(Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.skip),
                        style = HinvrTypography.labelLarge,
                        color = colors.gold,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .clickable { finish() }
                            .padding(8.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                val page = pages[pagerState.currentPage]
                Text(page.kicker, style = HinvrTypography.labelSmall, color = colors.gold)
                Spacer(Modifier.height(12.dp))
                Text(page.title, style = HinvrTypography.headlineLarge, color = colors.ink)
                Spacer(Modifier.height(10.dp))
                Text(page.body, style = HinvrTypography.bodyLarge, color = colors.inkMuted)
                Spacer(Modifier.height(24.dp))
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    pages.indices.forEach { i ->
                        val selected = pagerState.currentPage == i
                        Box(
                            Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (selected) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (selected) colors.gold else colors.ink.copy(alpha = 0.2f)),
                        )
                    }
                }
                HinvrPrimaryButton(text = stringResource(R.string.cta_continue_phone), onClick = { finish() })
                Spacer(Modifier.height(8.dp))
                HinvrGoldOutlineButton(text = stringResource(R.string.cta_have_account), onClick = { finish() })
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
