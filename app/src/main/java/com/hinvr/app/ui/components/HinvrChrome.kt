package com.hinvr.app.ui.components

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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.hinvr.app.ui.theme.HinvrCardRadius
import com.hinvr.app.ui.theme.HinvrDisplay
import com.hinvr.app.ui.theme.HinvrPillRadius
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography
import com.hinvr.app.ui.theme.LocalAtmosphere
import com.hinvr.app.ui.theme.ServiceClay
import com.hinvr.app.ui.theme.ServiceClayDeep
import com.hinvr.app.ui.theme.ServiceClayLift
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
    tint: Color = HinvrTheme.colors.ink,
    background: Color = HinvrTheme.colors.ivory,
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
            .border(1.dp, HinvrTheme.colors.gold, CircleShape)
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
fun LivePill(modifier: Modifier = Modifier, label: String = stringResource(R.string.badge_live)) {
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
            .background(colors.saffron)
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
                .background(colors.cream),
        )
        Text(label, style = HinvrTypography.labelSmall.copy(letterSpacing = 1.4.sp), color = colors.cream)
    }
}

/** A quiet editorial label for media and access state — intentionally not a Material chip. */
@Composable
fun StatusLabel(
    label: String,
    modifier: Modifier = Modifier,
    darkSurface: Boolean = true,
) {
    val colors = HinvrTheme.colors
    Text(
        label.uppercase(),
        style = HinvrTypography.labelSmall.copy(letterSpacing = 1.5.sp),
        color = if (darkSurface) colors.cream else colors.saffron,
        modifier = modifier
            .border(1.dp, colors.gold, RoundedCornerShape(2.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
    )
}

@Composable
fun MembershipBadge(
    tier: String,
    validity: String,
    modifier: Modifier = Modifier,
) {
    val colors = HinvrTheme.colors
    val onPaper = LocalAtmosphere.current == Atmosphere.Sabha
    Column(
        modifier
            .border(1.dp, colors.gold, RoundedCornerShape(2.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Text(
            tier.uppercase(),
            style = HinvrTypography.labelSmall.copy(letterSpacing = 1.4.sp),
            color = if (onPaper) colors.ink else colors.gold,
        )
        if (validity.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                validity,
                style = HinvrTypography.bodyMedium.copy(fontSize = 11.sp),
                color = if (onPaper) colors.inkMuted else colors.creamMuted,
            )
        }
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
            .clip(RoundedCornerShape(HinvrCardRadius))
            .background(colors.ivory)
            .border(1.dp, colors.gold, RoundedCornerShape(HinvrCardRadius))
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
                .background(colors.saffron),
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
    alignment: Alignment = Alignment.TopCenter,
    contentDescription: String? = null,
) {
    val gradedModifier = modifier.warmPhotoGrade()
    if (photoUrl.isNotBlank()) {
        AsyncImage(
            model = photoUrl,
            contentDescription = contentDescription,
            modifier = gradedModifier,
            contentScale = contentScale,
            alignment = alignment,
            placeholder = painterResource(fallback),
            error = painterResource(fallback),
        )
    } else {
        Image(
            painter = painterResource(fallback),
            contentDescription = contentDescription,
            modifier = gradedModifier,
            contentScale = contentScale,
            alignment = alignment,
        )
    }
}

/**
 * Keeps photographs documentary and warm, rather than the overexposed,
 * synthetic-gold treatment in the source images. Applied at the single shared
 * image entry point so Home, Mandirs, and booking imagery remain consistent.
 */
@Composable
private fun Modifier.warmPhotoGrade(): Modifier = this

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
    val shape = RoundedCornerShape(12.dp)
    val press = rememberPressMotion()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                scaleX = press.scale
                scaleY = press.scale
            }
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    0f to ServiceClayLift,
                    0.55f to ServiceClay,
                    1f to ServiceClayDeep,
                ),
            )
            .border(1.dp, colors.gold.copy(alpha = 0.45f), shape)
            .clickable(
                interactionSource = press.interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        CatalogPhoto(
            photoUrl = "",
            fallback = scene.serviceDrawable(),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.00f to Color.Black,
                            0.38f to Color.Black,
                            0.66f to Color.Transparent,
                            1.00f to Color.Transparent,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                },
            alignment = Alignment.TopCenter,
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Box(
                Modifier
                    .size(width = 18.dp, height = 2.dp)
                    .background(colors.gold),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                title,
                style = HinvrTypography.titleLarge.copy(fontSize = 22.sp, lineHeight = 26.sp),
                color = colors.cream,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                benefit,
                style = BenefitLine,
                color = colors.cream.copy(alpha = 0.78f),
            )
        }
    }
}

// Real photographs, cropped for the tiles.
// Kadwaha sanctum and Deobaloda pillars: Ms Sarah Welch, CC0.
// Kodandarama entrance: P. Madhusudan, CC0.
// Kedarnath: Niranjan, CC0.
// Temple offering: Mukundh balajee, CC BY-SA 4.0.
// Temple priest: Steve Evans, CC BY 2.0.
private fun TileScene.serviceDrawable(): Int = when (this) {
    TileScene.LiveAarti -> R.drawable.card_live_darshan
    TileScene.VrHall -> R.drawable.card_vr_darshan
    TileScene.PassDesk -> R.drawable.card_priority_pass
    TileScene.PanditDoor -> R.drawable.card_book_pandit
    TileScene.ConciergeDesk -> R.drawable.card_concierge
    TileScene.YatraRoad -> R.drawable.card_yatra
    else -> R.drawable.card_live_darshan
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
    mandirId: String = "",
    vr: Boolean = false,
    passAccepted: Boolean = false,
) {
    val colors = HinvrTheme.colors
    val press = rememberPressMotion()
    Column(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
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
        Box(Modifier.fillMaxWidth().aspectRatio(3f / 4f)) {
            CatalogPhoto(
                photoUrl = if (photoUrl.startsWith("http")) "" else photoUrl,
                fallback = if (mandirId.isNotBlank()) mandirArtwork(mandirId) else scene.templeDrawable(),
                modifier = Modifier.fillMaxSize(),
                alignment = Alignment.TopCenter,
            )
            Row(
                modifier = Modifier.padding(12.dp).align(Alignment.TopStart),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (live) LivePill()
                if (vr) PhotoBadge(stringResource(R.string.badge_vr))
                if (passAccepted) PhotoBadge(stringResource(R.string.badge_pass))
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .background(colors.ivory)
                .border(width = 1.dp, color = colors.gold)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(place.uppercase(), style = HinvrTypography.labelSmall, color = colors.saffron)
            Text(title, style = HinvrTypography.titleLarge.copy(fontSize = 18.sp, lineHeight = 22.sp), color = colors.ink, maxLines = 2)
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
    mandirId: String = "",
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
                .aspectRatio(3f / 4f)
                .clickable(
                    interactionSource = press.interactionSource,
                    indication = null,
                    onClick = onPhoto,
                ),
        ) {
            CatalogPhoto(
                photoUrl = if (photoUrl.startsWith("http")) "" else photoUrl,
                fallback = if (mandirId.isNotBlank()) mandirArtwork(mandirId) else scene.templeDrawable(),
                modifier = Modifier.fillMaxSize(),
            )
            if (live) LivePill(Modifier.padding(14.dp).align(Alignment.TopStart))
        }
        Column(
            Modifier
                .fillMaxWidth()
                .background(colors.ivory)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.gold))
            Spacer(Modifier.height(12.dp))
            Text(place.uppercase(), style = HinvrTypography.labelSmall, color = colors.saffron)
            Text(title, style = HinvrTypography.headlineMedium, color = colors.ink)
        }
        GlassDock(
            items = listOf(
                stringResource(R.string.dock_live) to onLive,
                stringResource(R.string.dock_vr) to onVr,
                stringResource(R.string.dock_pass) to onPass,
                stringResource(R.string.dock_assist) to onAssist,
            ),
        )
    }
}

internal fun TileScene.templeDrawable(): Int = when (this) {
    TileScene.Kashi -> R.drawable.temple_kashi
    TileScene.Shirdi -> R.drawable.temple_shirdi
    TileScene.Kedarnath -> R.drawable.temple_kedarnath
    TileScene.Somnath -> R.drawable.temple_somnath
    else -> R.drawable.temple_tirupati
}

fun mandirArtwork(id: String): Int = when (id) {
    "tirupati" -> R.drawable.temple_tirupati
    "kashi" -> R.drawable.temple_kashi
    "shirdi" -> R.drawable.temple_shirdi
    "kedarnath" -> R.drawable.temple_kedarnath
    "somnath" -> R.drawable.temple_somnath
    "vaishno-devi" -> R.drawable.temple_vaishno_devi
    "meenakshi" -> R.drawable.temple_meenakshi
    "jagannath" -> R.drawable.temple_jagannath
    "dwarkadhish" -> R.drawable.temple_dwarkadhish
    "badrinath" -> R.drawable.temple_badrinath
    "golden-temple" -> R.drawable.temple_harmandir
    "siddhivinayak" -> R.drawable.temple_siddhivinayak
    "iskcon-bengaluru" -> R.drawable.temple_iskcon
    else -> R.drawable.temple_tirupati
}

@Composable
fun GlassDock(
    items: List<Pair<String, () -> Unit>>,
    modifier: Modifier = Modifier,
) {
    val colors = HinvrTheme.colors
    Box(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(colors.ivory)
                .border(1.dp, colors.gold)
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
            .clip(CircleShape)
            .background(colors.saffron)
            .drawWithContent {
                drawContent()
                val d = this.size.minDimension
                drawCircle(
                    color = colors.gold,
                    radius = d / 2f - 4.dp.toPx(),
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
            .border(1.dp, colors.gold, CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "HI",
                fontFamily = HinvrDisplay,
                fontSize = (size.value * 0.22f).sp,
                color = colors.cream,
                lineHeight = (size.value * 0.22f).sp,
            )
            Text(
                "NV",
                fontFamily = HinvrDisplay,
                fontSize = (size.value * 0.22f).sp,
                color = colors.cream,
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
fun EditorialSectionHeader(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier,
) {
    val sabha = LocalAtmosphere.current == Atmosphere.Sabha
    val colors = HinvrTheme.colors
    Column(modifier) {
        Text(
            eyebrow.uppercase(),
            style = HinvrTypography.labelSmall.copy(letterSpacing = 1.8.sp),
            color = colors.saffron,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            title,
            style = HinvrTypography.headlineMedium.copy(fontSize = 28.sp, lineHeight = 33.sp),
            color = if (sabha) colors.ink else colors.cream,
        )
    }
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
            .clip(RoundedCornerShape(2.dp))
            .background(if (selected) colors.burgundy else colors.ivory)
            .border(1.dp, if (selected) colors.burgundy else colors.gold, RoundedCornerShape(2.dp))
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
            .clip(RoundedCornerShape(HinvrCardRadius))
            .background(containerColor ?: colors.ivory)
            .border(1.dp, colors.gold, RoundedCornerShape(HinvrCardRadius))
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = tint)
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
            .clip(RoundedCornerShape(HinvrPillRadius))
            .background(colors.ivory)
            .border(1.dp, colors.gold, RoundedCornerShape(HinvrPillRadius))
            .clickable(
                interactionSource = press.interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 22.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.custom_request_prefix), style = HinvrTypography.bodyLarge, color = colors.ink)
        Text(
            stringResource(R.string.app_name),
            style = HinvrTypography.bodyLarge.copy(fontFamily = HinvrDisplay),
            color = colors.ink,
        )
    }
}
