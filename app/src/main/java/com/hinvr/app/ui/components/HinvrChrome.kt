package com.hinvr.app.ui.components

import android.os.Build
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hinvr.app.R
import com.hinvr.app.ui.catalog.TileScene
import coil3.compose.AsyncImage
import com.hinvr.app.ui.theme.Atmosphere
import com.hinvr.app.ui.theme.BenefitLine
import com.hinvr.app.ui.theme.CardTitleOnPhoto
import com.hinvr.app.ui.theme.Figtree
import com.hinvr.app.ui.theme.Fraunces
import com.hinvr.app.ui.theme.HinvrCardRadius
import com.hinvr.app.ui.theme.HinvrPillRadius
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography
import com.hinvr.app.ui.theme.LocalAtmosphere
import com.hinvr.app.ui.motion.HinvrMotion

private data class PressMotion(
    val interactionSource: MutableInteractionSource,
    val scale: Float,
)

@Composable
private fun rememberPressMotion(): PressMotion {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.965f else 1f,
        animationSpec = tween(HinvrMotion.Quick, easing = HinvrMotion.EnterEasing),
        label = "card press",
    )
    return PressMotion(interactionSource, scale)
}

@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    background: Color = HinvrTheme.colors.dusk.copy(alpha = 0.55f),
) {
    val press = rememberPressMotion()
    Box(
        modifier = modifier
            .size(42.dp)
            .graphicsLayer {
                scaleX = press.scale
                scaleY = press.scale
            }
            .clip(CircleShape)
            .background(background)
            .clickable(
                interactionSource = press.interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun PhotoScrim(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color.Transparent,
                        0.45f to Color.Transparent,
                        1f to Color(0xCC1A120C),
                    ),
                ),
            ),
    )
}

@Composable
fun LivePill(modifier: Modifier = Modifier, label: String = "LIVE") {
    val colors = HinvrTheme.colors
    val pulse = rememberInfiniteTransition(label = "live pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = HinvrMotion.EnterEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "live dot",
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(HinvrPillRadius))
            .background(Color(0xCC1A120C))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(6.dp)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                    alpha = 1.45f - pulseScale * 0.45f
                }
                .clip(CircleShape)
                .background(colors.vermillion),
        )
        Text(label, style = HinvrTypography.labelSmall.copy(letterSpacing = 1.4.sp), color = colors.cream)
    }
}

@Composable
fun StatusCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HinvrTheme.colors
    val press = rememberPressMotion()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = press.scale
                scaleY = press.scale
            }
            .shadow(16.dp, RoundedCornerShape(HinvrCardRadius), ambientColor = Color.Black.copy(alpha = 0.08f))
            .clip(RoundedCornerShape(HinvrCardRadius))
            .background(colors.ivory)
            .clickable(
                interactionSource = press.interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = HinvrTypography.titleMedium.copy(fontFamily = Figtree, fontWeight = FontWeight.SemiBold), color = colors.ink)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = HinvrTypography.bodyMedium, color = colors.inkMuted)
        }
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.dusk),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = colors.cream, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun CatalogPhoto(
    photoUrl: String,
    fallback: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null,
) {
    if (photoUrl.isNotBlank()) {
        AsyncImage(
            model = photoUrl,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            placeholder = painterResource(fallback),
            error = painterResource(fallback),
        )
    } else {
        Image(
            painter = painterResource(fallback),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
    }
}

@Composable
fun BentoTile(
    title: String,
    benefit: String,
    scene: TileScene,
    height: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    photoUrl: String = "",
) {
    val colors = HinvrTheme.colors
    val press = rememberPressMotion()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                scaleX = press.scale
                scaleY = press.scale
            }
            .shadow(
                12.dp,
                RoundedCornerShape(HinvrCardRadius),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.08f),
            )
            .clip(RoundedCornerShape(HinvrCardRadius))
            .background(colors.ivory)
            .clickable(
                interactionSource = press.interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        CatalogPhoto(
            photoUrl = photoUrl,
            fallback = scene.homeCardDrawable(),
            modifier = Modifier.fillMaxSize(),
        )
        // Light footer scrim so the illustration reads as a Crew-style airy card
        // and the dark serif title/benefit stay legible on any scene.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.5f to Color.Transparent,
                            0.74f to colors.ivory.copy(alpha = 0.7f),
                            1f to colors.ivory.copy(alpha = 0.97f),
                        ),
                    ),
                ),
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp, vertical = 15.dp),
        ) {
            Text(
                title,
                style = HinvrTypography.titleLarge.copy(fontSize = 21.sp),
                color = colors.ink,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                benefit,
                style = BenefitLine,
                color = colors.inkMuted,
            )
        }
    }
}

private fun TileScene.homeCardDrawable(): Int = when (this) {
    TileScene.LiveAarti -> R.drawable.home_card_live_darshan
    TileScene.VrHall -> R.drawable.home_card_vr_darshan
    TileScene.PassDesk -> R.drawable.home_card_priority_pass
    TileScene.PanditDoor -> R.drawable.home_card_book_pandit
    TileScene.ConciergeDesk -> R.drawable.home_card_concierge
    TileScene.YatraRoad -> R.drawable.home_card_yatra
    else -> R.drawable.home_card_live_darshan
}

@Composable
fun PortraitPhotoCard(
    title: String,
    place: String,
    scene: TileScene,
    live: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp? = 168.dp,
    height: Dp = 220.dp,
    photoUrl: String = "",
    vr: Boolean = false,
    passAccepted: Boolean = false,
) {
    val colors = HinvrTheme.colors
    val press = rememberPressMotion()
    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .graphicsLayer {
                scaleX = press.scale
                scaleY = press.scale
            }
            .shadow(8.dp, RoundedCornerShape(HinvrCardRadius), ambientColor = Color.Black.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(HinvrCardRadius))
            .clickable(
                interactionSource = press.interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        CatalogPhoto(
            photoUrl = photoUrl,
            fallback = scene.templeDrawable(),
            modifier = Modifier.fillMaxSize(),
        )
        PhotoScrim()
        Row(
            modifier = Modifier.padding(12.dp).align(Alignment.TopStart),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (live) LivePill()
            if (vr) PhotoBadge("VR")
            if (passAccepted) PhotoBadge("PASS")
        }
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp),
        ) {
            Text(place.uppercase(), style = HinvrTypography.labelSmall, color = colors.gold)
            Text(title, style = CardTitleOnPhoto, color = colors.cream)
        }
    }
}

@Composable
private fun PhotoBadge(label: String) {
    val colors = HinvrTheme.colors
    Text(
        label,
        style = HinvrTypography.labelSmall.copy(letterSpacing = 1.sp),
        color = colors.cream,
        modifier = Modifier
            .clip(RoundedCornerShape(HinvrPillRadius))
            .background(colors.dusk.copy(alpha = 0.82f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
fun MandirHeroCard(
    title: String,
    place: String,
    scene: TileScene,
    live: Boolean,
    onPhoto: () -> Unit,
    onLive: () -> Unit,
    onVr: () -> Unit,
    onPass: () -> Unit,
    onAssist: () -> Unit,
    modifier: Modifier = Modifier,
    photoUrl: String = "",
) {
    val colors = HinvrTheme.colors
    val press = rememberPressMotion()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = press.scale
                scaleY = press.scale
            }
            .shadow(12.dp, RoundedCornerShape(HinvrCardRadius), ambientColor = Color.Black.copy(alpha = 0.07f))
            .clip(RoundedCornerShape(HinvrCardRadius)),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clickable(
                    interactionSource = press.interactionSource,
                    indication = null,
                    onClick = onPhoto,
                ),
        ) {
            CatalogPhoto(
                photoUrl = photoUrl,
                fallback = scene.templeDrawable(),
                modifier = Modifier.fillMaxSize(),
            )
            PhotoScrim()
            if (live) LivePill(Modifier.padding(14.dp).align(Alignment.TopStart))
            Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Text(place.uppercase(), style = HinvrTypography.labelSmall, color = colors.gold)
                Text(title, style = HinvrTypography.headlineMedium, color = colors.cream)
            }
        }
        GlassDock(
            items = listOf("Live" to onLive, "VR" to onVr, "Pass" to onPass, "Assist" to onAssist),
        )
    }
}

internal fun TileScene.templeDrawable(): Int = when (this) {
    TileScene.Tirupati -> R.drawable.temple_tirupati
    TileScene.Kashi -> R.drawable.temple_kashi
    TileScene.Shirdi -> R.drawable.temple_shirdi
    TileScene.Kedarnath -> R.drawable.temple_kedarnath
    TileScene.Somnath -> R.drawable.temple_somnath
    else -> R.drawable.temple_tirupati
}

@Composable
fun GlassDock(
    items: List<Pair<String, () -> Unit>>,
    modifier: Modifier = Modifier,
) {
    val colors = HinvrTheme.colors
    val blurMod = if (Build.VERSION.SDK_INT >= 31) Modifier.blur(20.dp) else Modifier
    Box(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .matchParentSize()
                .then(blurMod)
                .background(colors.ivory.copy(alpha = 0.88f)),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .background(colors.ivory.copy(alpha = 0.78f))
                .padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            items.forEach { (label, onClick) ->
                Text(
                    label,
                    style = HinvrTypography.labelLarge,
                    color = colors.ink,
                    modifier = Modifier
                        .clip(RoundedCornerShape(HinvrPillRadius))
                        .clickable(onClick = onClick)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
fun PassSeal(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 54.dp,
    halo: Boolean = true,
) {
    val colors = HinvrTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val selectedScale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = tween(HinvrMotion.Standard, easing = HinvrMotion.EnterEasing),
        label = "pass selection",
    )
    val glow by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(HinvrMotion.Standard, easing = HinvrMotion.EnterEasing),
        label = "pass glow",
    )
    // A lacquered vermillion orb with a gold ring: the Crew orb, in temple colors.
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                val scale = if (pressed) selectedScale * 0.95f else selectedScale
                scaleX = scale
                scaleY = scale
            }
            .drawBehind {
                if (halo && glow > 0f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(colors.amber.copy(alpha = 0.34f * glow), Color.Transparent),
                            center = center,
                            radius = this.size.minDimension * 0.95f,
                        ),
                        radius = this.size.minDimension * 0.95f,
                    )
                }
            }
            .shadow(
                elevation = if (selected) 12.dp else 7.dp,
                shape = CircleShape,
                ambientColor = colors.vermillion.copy(alpha = 0.35f),
                spotColor = Color(0xFF4A1A12).copy(alpha = 0.5f),
            )
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    0f to Color(0xFFC94C37),
                    0.45f to colors.vermillion,
                    0.8f to Color(0xFF7B2E20),
                    1f to Color(0xFF4A1A12),
                    center = Offset.Unspecified,
                ),
            )
            .drawWithContent {
                drawContent()
                val d = this.size.minDimension
                // Specular sheen across the upper dome.
                drawOval(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.42f), Color.White.copy(alpha = 0f)),
                        startY = d * 0.06f,
                        endY = d * 0.5f,
                    ),
                    topLeft = Offset(d * 0.2f, d * 0.06f),
                    size = Size(d * 0.6f, d * 0.4f),
                )
                // Stamped inner ring, like a wax seal.
                drawCircle(
                    color = colors.gold.copy(alpha = 0.28f + 0.2f * glow),
                    radius = d / 2f - 5.dp.toPx(),
                    style = Stroke(width = 0.8.dp.toPx()),
                )
            }
            .border(
                1.5.dp,
                Brush.verticalGradient(
                    listOf(
                        colors.flame.copy(alpha = 0.55f + 0.35f * glow),
                        colors.gold.copy(alpha = 0.7f + 0.3f * glow),
                        colors.goldDim.copy(alpha = 0.8f),
                    ),
                ),
                CircleShape,
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "HI",
                fontFamily = Fraunces,
                fontWeight = FontWeight.SemiBold,
                fontSize = (size.value * 0.2f).sp,
                letterSpacing = 1.5.sp,
                color = colors.cream,
                lineHeight = (size.value * 0.22f).sp,
            )
            Text(
                "NV",
                fontFamily = Fraunces,
                fontWeight = FontWeight.SemiBold,
                fontSize = (size.value * 0.2f).sp,
                letterSpacing = 1.5.sp,
                color = colors.flame.copy(alpha = 0.92f),
                lineHeight = (size.value * 0.22f).sp,
            )
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    val sabha = LocalAtmosphere.current == Atmosphere.Sabha
    Text(
        text,
        style = HinvrTypography.headlineMedium.copy(fontSize = 24.sp),
        color = if (sabha) HinvrTheme.colors.ink else HinvrTheme.colors.cream,
        modifier = modifier,
    )
}

@Composable
fun SabhaSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val colors = HinvrTheme.colors
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(HinvrPillRadius))
            .background(colors.ivory)
            .border(1.dp, colors.gold.copy(alpha = 0.18f), RoundedCornerShape(HinvrPillRadius))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        singleLine = true,
        textStyle = HinvrTypography.bodyLarge.copy(color = colors.ink),
        cursorBrush = SolidColor(colors.gold),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(placeholder, style = HinvrTypography.bodyLarge, color = colors.inkMuted)
                }
                inner()
            }
        },
    )
}

@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = HinvrTheme.colors
    Text(
        label,
        style = HinvrTypography.labelLarge,
        color = if (selected) colors.cream else colors.ink,
        modifier = Modifier
            .clip(RoundedCornerShape(HinvrPillRadius))
            .background(if (selected) colors.ink else colors.ivory)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
fun IvoryCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = HinvrTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(HinvrCardRadius), ambientColor = Color.Black.copy(alpha = 0.05f))
            .clip(RoundedCornerShape(HinvrCardRadius))
            .background(containerColor ?: colors.ivory)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(18.dp),
        content = content,
    )
}

@Composable
fun SabhaTopBar(
    title: String? = null,
    onBack: (() -> Unit)? = null,
    onPhoto: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = HinvrTheme.colors
    val sabha = LocalAtmosphere.current == Atmosphere.Sabha
    val tint = if (onPhoto || !sabha) colors.cream else colors.ink
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = tint)
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }
        if (title != null) {
            Text(title, style = HinvrTypography.titleLarge, color = tint, modifier = Modifier.weight(1f))
        } else {
            Spacer(Modifier.weight(1f))
        }
        actions()
    }
}

@Composable
fun CustomRequestPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = HinvrTheme.colors
    val press = rememberPressMotion()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = press.scale
                scaleY = press.scale
            }
            .shadow(8.dp, RoundedCornerShape(HinvrPillRadius), ambientColor = Color.Black.copy(alpha = 0.05f))
            .clip(RoundedCornerShape(HinvrPillRadius))
            .background(colors.ivory)
            .clickable(
                interactionSource = press.interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 22.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text("Custom request for ", style = HinvrTypography.bodyLarge, color = colors.ink)
        Text(
            "HINVR",
            style = HinvrTypography.bodyLarge.copy(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold),
            color = colors.ink,
        )
    }
}
