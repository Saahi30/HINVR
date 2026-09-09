package com.hinvr.app.ui.components

import android.os.Build
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hinvr.app.ui.catalog.TileScene
import com.hinvr.app.ui.illustrations.TempleScene
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

@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    background: Color = HinvrTheme.colors.dusk.copy(alpha = 0.55f),
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(HinvrCardRadius), ambientColor = Color.Black.copy(alpha = 0.08f))
            .clip(RoundedCornerShape(HinvrCardRadius))
            .background(colors.ivory)
            .clickable(onClick = onClick)
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
fun BentoTile(
    title: String,
    benefit: String,
    scene: TileScene,
    height: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HinvrTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .shadow(
                12.dp,
                RoundedCornerShape(HinvrCardRadius),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.08f),
            )
            .clip(RoundedCornerShape(HinvrCardRadius))
            .background(colors.ivory)
            .clickable(onClick = onClick),
    ) {
        TempleScene(scene)
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
) {
    val colors = HinvrTheme.colors
    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .shadow(8.dp, RoundedCornerShape(HinvrCardRadius), ambientColor = Color.Black.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(HinvrCardRadius))
            .clickable(onClick = onClick),
    ) {
        TempleScene(scene)
        PhotoScrim()
        if (live) {
            LivePill(Modifier.padding(12.dp).align(Alignment.TopStart))
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
) {
    val colors = HinvrTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(HinvrCardRadius), ambientColor = Color.Black.copy(alpha = 0.07f))
            .clip(RoundedCornerShape(HinvrCardRadius)),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clickable(onClick = onPhoto),
        ) {
            TempleScene(scene)
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
) {
    val colors = HinvrTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { scaleX = if (pressed) 0.96f else 1f; scaleY = if (pressed) 0.96f else 1f }
            .shadow(
                elevation = if (selected) 12.dp else 4.dp,
                shape = CircleShape,
                ambientColor = colors.gold.copy(alpha = 0.4f),
            )
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        if (selected) Color(0xFFC45C26) else Color(0xFF3D2E22),
                        if (selected) Color(0xFF6F1D16) else Color(0xFF1A120C),
                    ),
                ),
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "HI",
                fontFamily = Figtree,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                color = colors.cream,
                lineHeight = 12.sp,
            )
            Text(
                "NV",
                fontFamily = Figtree,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                color = colors.gold,
                lineHeight = 12.sp,
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
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = HinvrTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(HinvrCardRadius), ambientColor = Color.Black.copy(alpha = 0.05f))
            .clip(RoundedCornerShape(HinvrCardRadius))
            .background(colors.ivory)
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(HinvrPillRadius), ambientColor = Color.Black.copy(alpha = 0.05f))
            .clip(RoundedCornerShape(HinvrPillRadius))
            .background(colors.ivory)
            .clickable(onClick = onClick)
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
