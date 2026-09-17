package com.hinvr.app.ui.pass

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import com.hinvr.app.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hinvr.app.data.MembershipTier
import com.hinvr.app.data.SessionSnapshot
import com.hinvr.app.navigation.LocalSessionRepository
import com.hinvr.app.ui.components.HinvrBackground
import com.hinvr.app.ui.components.IvoryCard
import com.hinvr.app.ui.components.SabhaTopBar
import com.hinvr.app.ui.motion.HinvrMotion
import com.hinvr.app.ui.theme.Atmosphere
import com.hinvr.app.ui.theme.HinvrPillRadius
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography

@Composable
fun PassScreen(
    onOpenPlans: () -> Unit,
    onOpenHow: () -> Unit,
    onPlanVisit: () -> Unit,
) {
    val session = LocalSessionRepository.current
    val snap by session.snapshot.collectAsStateWithLifecycle(initialValue = SessionSnapshot())
    val hasCredential = snap.tier in setOf(MembershipTier.Gold, MembershipTier.Platinum, MembershipTier.Nri)
    val colors = HinvrTheme.colors
    var passRevealed by rememberSaveable { mutableStateOf(false) }
    KeepScreenBright(enabled = hasCredential)

    HinvrBackground(atmosphere = Atmosphere.Sanctum) {
        PassVaultAtmosphere()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(top = 8.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PassVaultHeader(active = hasCredential)
            Spacer(Modifier.height(28.dp))
            Text(
                if (hasCredential) "DESK CREDENTIAL" else "MEMBERSHIP PREVIEW",
                style = HinvrTypography.labelSmall.copy(letterSpacing = 2.sp),
                color = colors.gold,
            )
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Box(
                    Modifier
                        .matchParentSize()
                        .offset(y = 18.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    colors.gold.copy(alpha = 0.3f),
                                    colors.amber.copy(alpha = 0.08f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )
                PassDrawerReveal(
                    alreadyRevealed = passRevealed,
                    onFinished = { passRevealed = true },
                ) {
                    PassCredentialCard(snap = snap, hasCredential = hasCredential)
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                if (hasCredential) {
                    "Hold this under the scanner."
                } else {
                    "Your name on a temple pass."
                },
                style = HinvrTypography.titleLarge,
                color = colors.cream,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(22.dp))
            PassLedger(snap = snap, hasCredential = hasCredential)
            Spacer(Modifier.height(22.dp))
            PassVaultButton(
                text = if (hasCredential) "Plan a visit" else "Choose membership",
                onClick = if (hasCredential) onPlanVisit else onOpenPlans,
            )
            Text(
                "How the pass works",
                style = HinvrTypography.labelLarge,
                color = colors.gold,
                modifier = Modifier
                    .clickable(onClick = onOpenHow)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            )
            Text(
                "Official entry and assist only.\nThis is not an unofficial queue-jump.",
                style = HinvrTypography.bodyMedium,
                color = colors.creamMuted.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun BoxScope.PassVaultAtmosphere() {
    val colors = HinvrTheme.colors
    Image(
        painter = painterResource(R.drawable.temple_kashi),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.56f)
            .align(Alignment.TopCenter)
            .offset(y = 36.dp)
            .graphicsLayer { alpha = 0.2f },
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to colors.duskDeep.copy(alpha = 0.72f),
                        0.28f to colors.duskDeep.copy(alpha = 0.28f),
                        0.58f to colors.dusk.copy(alpha = 0.7f),
                        1f to colors.duskDeep,
                    ),
                ),
            ),
    )
}

@Composable
private fun PassVaultHeader(active: Boolean) {
    val colors = HinvrTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "HINVR",
                style = HinvrTypography.titleMedium.copy(letterSpacing = 3.sp),
                color = colors.cream,
            )
            Text("MEMBER PASS", style = HinvrTypography.labelSmall, color = colors.gold)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (active) colors.gold else colors.creamMuted.copy(alpha = 0.45f)),
            )
            Text(
                if (active) "READY" else "LOCKED",
                style = HinvrTypography.labelSmall,
                color = if (active) colors.gold else colors.creamMuted,
            )
        }
    }
}

@Composable
private fun PassLedger(snap: SessionSnapshot, hasCredential: Boolean) {
    val colors = HinvrTheme.colors
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.gold.copy(alpha = 0.28f), shape)
            .clip(shape)
            .background(colors.dusk.copy(alpha = 0.55f))
            .padding(horizontal = 8.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LedgerCell(
            eyebrow = "DESK",
            value = if (hasCredential) "Ready" else "Locked",
        )
        LedgerRule()
        LedgerCell(
            eyebrow = "MEMBER",
            value = if (hasCredential) snap.memberId.ifBlank { "HNV-00000" } else "—",
        )
        LedgerRule()
        LedgerCell(
            eyebrow = "VALID",
            value = if (hasCredential) snap.validUntilLabel.ifBlank { "Annual" } else "Join",
        )
    }
}

@Composable
private fun RowScope.LedgerCell(eyebrow: String, value: String) {
    val colors = HinvrTheme.colors
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(eyebrow, style = HinvrTypography.labelSmall, color = colors.goldDim)
        Spacer(Modifier.height(6.dp))
        Text(
            value,
            style = HinvrTypography.labelLarge,
            color = colors.cream,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LedgerRule() {
    val colors = HinvrTheme.colors
    Box(
        Modifier
            .width(1.dp)
            .height(28.dp)
            .background(colors.gold.copy(alpha = 0.22f)),
    )
}

@Composable
private fun PassVaultButton(text: String, onClick: () -> Unit) {
    val colors = HinvrTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = tween(HinvrMotion.Quick, easing = HinvrMotion.EnterEasing),
        label = "vault button press",
    )
    val shape = RoundedCornerShape(HinvrPillRadius)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 12.dp,
                shape = shape,
                ambientColor = colors.gold.copy(alpha = 0.18f),
                spotColor = Color.Black.copy(alpha = 0.28f),
            )
            .clip(shape)
            .background(colors.cream)
            .border(1.dp, colors.gold.copy(alpha = 0.55f), shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = HinvrTypography.labelLarge, color = colors.ink)
    }
}

@Composable
private fun PassDrawerReveal(
    alreadyRevealed: Boolean,
    onFinished: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = HinvrTheme.colors
    val seal = remember { Animatable(if (alreadyRevealed) 0f else 1f) }
    val drawer = remember { Animatable(if (alreadyRevealed) 1f else 0f) }
    val folio = remember { Animatable(if (alreadyRevealed) 0f else 1f) }

    LaunchedEffect(alreadyRevealed) {
        if (alreadyRevealed) return@LaunchedEffect
        delay(360)
        coroutineScope {
            launch {
                seal.animateTo(
                    0f,
                    animationSpec = tween(800, easing = HinvrMotion.ExitEasing),
                )
            }
            launch {
                delay(380)
                drawer.animateTo(
                    1f,
                    animationSpec = tween(4200, easing = HinvrMotion.EnterEasing),
                )
            }
            launch {
                delay(1900)
                folio.animateTo(
                    0f,
                    animationSpec = tween(2680, easing = HinvrMotion.ExitEasing),
                )
            }
        }
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clipToBounds(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            Modifier
                .zIndex(1f)
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = 0.35f + drawer.value * 0.65f
                    translationX = -size.width * 0.38f * (1f - drawer.value)
                    val scale = 0.98f + drawer.value * 0.02f
                    scaleX = scale
                    scaleY = scale
                },
        ) {
            content()
        }

        Box(
            Modifier
                .zIndex(2f)
                .fillMaxWidth()
                .aspectRatio(1.586f)
                .graphicsLayer {
                    alpha = folio.value
                    translationX = -size.width * 0.45f * drawer.value
                }
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = colors.gold.copy(alpha = 0.15f),
                    spotColor = Color.Black.copy(alpha = 0.16f),
                )
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, colors.gold.copy(alpha = 0.58f), RoundedCornerShape(24.dp)),
        ) {
            Image(
                painter = painterResource(R.drawable.pass_envelope_art),
                contentDescription = "HINVR invitation folio",
                contentScale = ContentScale.Crop,
                alignment = Alignment.BottomCenter,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(colors.saffron.copy(alpha = 0.025f)),
            )
            Column(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(18.dp),
            ) {
                Text("HINVR", style = HinvrTypography.labelLarge, color = colors.ink)
                Text("MEMBER FOLIO", style = HinvrTypography.labelSmall, color = colors.goldDim)
            }
            Text(
                "DRAW TO OPEN  →",
                style = HinvrTypography.labelSmall,
                color = colors.ink.copy(alpha = 0.58f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(18.dp),
            )
        }

        Box(
            Modifier
                .zIndex(3f)
                .align(Alignment.Center)
                .size(52.dp)
                .graphicsLayer {
                    alpha = folio.value * seal.value
                    val scale = 0.86f + seal.value * 0.14f
                    scaleX = scale
                    scaleY = scale
                }
                .shadow(10.dp, CircleShape)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(colors.saffron, colors.vermillion)))
                .border(2.dp, colors.gold.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("श्री", style = HinvrTypography.titleMedium, color = colors.cream)
        }
    }
}

@Composable
private fun PassCredentialCard(snap: SessionSnapshot, hasCredential: Boolean) {
    val colors = HinvrTheme.colors
    var showExpandedQr by remember { mutableStateOf(false) }
    val sheen = rememberInfiniteTransition(label = "pass sheen")
    val sheenX by sheen.animateFloat(
        initialValue = -180f,
        targetValue = 430f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pass sheen position",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.586f)
            .shadow(
                elevation = 18.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = colors.gold.copy(alpha = 0.18f),
                spotColor = Color.Black.copy(alpha = 0.2f),
            )
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, Color(0xFFE2C879), RoundedCornerShape(24.dp)),
    ) {
        Image(
            painter = painterResource(R.drawable.pass_metal_texture),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0x1A3B2608),
                            Color.Transparent,
                            Color(0x0D3B2608),
                        ),
                    ),
                ),
        )
        Box(
            Modifier
                .offset(x = sheenX.dp)
                .width(74.dp)
                .fillMaxHeight()
                .graphicsLayer { rotationZ = -8f }
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.17f), Color.Transparent),
                    ),
                ),
        )

        Row(
            Modifier
                .fillMaxSize()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.ink.copy(alpha = 0.9f))
                            .border(1.dp, Color(0xFFE8D18A), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("HI", style = HinvrTypography.labelSmall, color = colors.cream)
                            Text("NV", style = HinvrTypography.labelSmall, color = colors.gold)
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("HINVR PRIORITY", style = HinvrTypography.labelSmall, color = colors.ink)
                        Text(
                            if (hasCredential) snap.tier.name.uppercase() else "GOLD PREVIEW",
                            style = HinvrTypography.bodyMedium,
                            color = colors.ink.copy(alpha = 0.72f),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    if (hasCredential) snap.displayName.ifBlank { "MEMBER" }.uppercase() else "YOUR NAME",
                    style = HinvrTypography.titleLarge,
                    color = colors.ink,
                    maxLines = 1,
                )
                Text(
                    if (hasCredential) snap.memberId.ifBlank { "HNV-00000" } else "HNV-•••••",
                    style = HinvrTypography.bodyMedium,
                    color = colors.ink.copy(alpha = 0.72f),
                )
                Spacer(Modifier.height(12.dp))
                Text("VALID THROUGH", style = HinvrTypography.labelSmall, color = colors.ink.copy(alpha = 0.62f))
                Text(
                    if (hasCredential) snap.validUntilLabel.ifBlank { "Annual" } else "Join to activate",
                    style = HinvrTypography.bodyMedium,
                    color = colors.ink,
                )
            }

            Column(
                modifier = Modifier.width(124.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(124.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.ivory)
                        .border(1.dp, colors.ink.copy(alpha = 0.14f), RoundedCornerShape(18.dp))
                        .clickable(enabled = hasCredential) { showExpandedQr = true }
                        .padding(9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (hasCredential) {
                        PassQr(
                            payload = "HNV|${snap.memberId}|${snap.tier.name}|${snap.validUntilLabel}",
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = colors.goldDim,
                                modifier = Modifier.size(28.dp),
                            )
                            Spacer(Modifier.height(7.dp))
                            Text("MEMBER QR", style = HinvrTypography.labelSmall, color = colors.ink)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (hasCredential) "TAP TO ENLARGE" else "LOCKED",
                    style = HinvrTypography.labelSmall,
                    color = colors.ink.copy(alpha = 0.65f),
                )
            }
        }
    }

    if (showExpandedQr && hasCredential) {
        Dialog(onDismissRequest = { showExpandedQr = false }) {
            IvoryCard {
                Text("DESK CREDENTIAL", style = HinvrTypography.labelSmall, color = colors.goldDim)
                Spacer(Modifier.height(6.dp))
                Text("Hold this under the scanner", style = HinvrTypography.titleLarge, color = colors.ink)
                Spacer(Modifier.height(16.dp))
                PassQr(
                    payload = "HNV|${snap.memberId}|${snap.tier.name}|${snap.validUntilLabel}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "${snap.displayName} · ${snap.memberId}",
                    style = HinvrTypography.bodyMedium,
                    color = colors.inkMuted,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tap outside to close",
                    style = HinvrTypography.labelSmall,
                    color = colors.goldDim,
                )
            }
        }
    }
}

@Composable
private fun KeepScreenBright(enabled: Boolean) {
    val activity = LocalView.current.context.findActivity()
    DisposableEffect(activity, enabled) {
        if (enabled) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.window?.let { window ->
                val params = window.attributes
                params.screenBrightness = 1f
                window.attributes = params
            }
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.window?.let { window ->
                val params = window.attributes
                params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = params
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun HowPassWorksScreen(onBack: () -> Unit) {
    val colors = HinvrTheme.colors
    val steps = listOf(
        "Show the QR" to "Hold the bright card at a confirmed partner desk.",
        "Host confirms you" to "They read the member, not a screenshot.",
        "Official assist" to "Entry, buggy, or wheelchair — as booked.",
    )
    HinvrBackground(atmosphere = Atmosphere.Sabha) {
        Column(Modifier.fillMaxSize()) {
            SabhaTopBar(title = "How the pass works", onBack = onBack)
            Column(Modifier.padding(horizontal = 22.dp, vertical = 8.dp)) {
                Text(
                    "Three quiet moves\nat the desk.",
                    style = HinvrTypography.headlineMedium,
                    color = colors.ink,
                )
                Spacer(Modifier.height(22.dp))
                steps.forEachIndexed { index, (title, body) ->
                    Row(
                        modifier = Modifier.padding(bottom = 22.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            "0${index + 1}",
                            style = HinvrTypography.labelSmall,
                            color = colors.gold,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        Column {
                            Text(title, style = HinvrTypography.titleLarge, color = colors.ink)
                            Spacer(Modifier.height(4.dp))
                            Text(body, style = HinvrTypography.bodyMedium, color = colors.inkMuted)
                        }
                    }
                }
                Box(
                    Modifier
                        .padding(top = 4.dp, bottom = 16.dp)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.gold.copy(alpha = 0.22f)),
                )
                Text(
                    "We do not sell unofficial skip-the-line at board-run temples.",
                    style = HinvrTypography.bodyMedium,
                    color = colors.inkMuted,
                )
            }
        }
    }
}
