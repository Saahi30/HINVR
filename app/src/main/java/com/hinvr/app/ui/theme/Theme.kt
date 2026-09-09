package com.hinvr.app.ui.theme

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp

enum class Atmosphere { Sabha, Sanctum }

@Immutable
data class HinvrColors(
    val dusk: Color = TempleStone,
    val duskDeep: Color = TempleStoneDeep,
    val stone: Color = TempleStoneMid,
    val stoneRaised: Color = TempleStoneLight,
    val gold: Color = AntiqueGold,
    val goldDim: Color = AntiqueGoldDim,
    val saffron: Color = Saffron,
    val saffronDeep: Color = SaffronDeep,
    val vermillion: Color = Vermillion,
    val amber: Color = DiyaAmber,
    val flame: Color = FlameCore,
    val cream: Color = Cream,
    val creamMuted: Color = CreamMuted,
    val ink: Color = Ink,
    val inkMuted: Color = InkMuted,
    val haze: Color = IncenseHaze,
    val linen: Color = Linen,
    val ivory: Color = Ivory,
)

val LocalHinvrColors = staticCompositionLocalOf { HinvrColors() }
val LocalAtmosphere = staticCompositionLocalOf { Atmosphere.Sanctum }

val HinvrCardRadius = 28.dp
val HinvrSheetRadius = 32.dp
val HinvrPillRadius = 999.dp
val HinvrSideInset = 22.dp

private val HinvrDarkScheme = darkColorScheme(
    primary = Saffron,
    onPrimary = Cream,
    primaryContainer = SaffronDeep,
    onPrimaryContainer = Cream,
    secondary = AntiqueGold,
    onSecondary = Ink,
    secondaryContainer = TempleStoneLight,
    onSecondaryContainer = Cream,
    tertiary = DiyaAmber,
    onTertiary = Ink,
    background = TempleStone,
    onBackground = Cream,
    surface = TempleStoneMid,
    onSurface = Cream,
    surfaceVariant = TempleStoneLight,
    onSurfaceVariant = CreamMuted,
    outline = AntiqueGoldDim,
    error = Vermillion,
    onError = Cream,
)

private val HinvrLightScheme = lightColorScheme(
    primary = Saffron,
    onPrimary = Cream,
    primaryContainer = Color(0xFFF3D9C4),
    onPrimaryContainer = Ink,
    secondary = AntiqueGold,
    onSecondary = Ink,
    secondaryContainer = Ivory,
    onSecondaryContainer = Ink,
    tertiary = DiyaAmber,
    onTertiary = Ink,
    background = Linen,
    onBackground = Ink,
    surface = Ivory,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE8DCCE),
    onSurfaceVariant = InkMuted,
    outline = Color(0xFFD4C4B0),
    error = Vermillion,
    onError = Cream,
)

@Composable
fun HinvrTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalHinvrColors provides HinvrColors()) {
        MaterialTheme(
            colorScheme = HinvrDarkScheme,
            typography = HinvrTypography,
            content = content,
        )
    }
}

@Composable
fun HinvrThemeFor(
    atmosphere: Atmosphere,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalHinvrColors provides HinvrColors(),
        LocalAtmosphere provides atmosphere,
    ) {
        MaterialTheme(
            colorScheme = if (atmosphere == Atmosphere.Sabha) HinvrLightScheme else HinvrDarkScheme,
            typography = HinvrTypography,
            content = content,
        )
    }
}

object HinvrTheme {
    val colors: HinvrColors
        @Composable
        get() = LocalHinvrColors.current
}

@Composable
fun ApplyAtmosphereBars(
    atmosphere: Atmosphere,
    darkIcons: Boolean = atmosphere == Atmosphere.Sabha,
) {
    val view = LocalView.current
    val navColor = if (atmosphere == Atmosphere.Sabha) {
        Linen.toArgb()
    } else {
        TempleStoneDeep.toArgb()
    }
    SideEffect {
        val activity = view.context as? ComponentActivity ?: return@SideEffect
        val transparent = android.graphics.Color.TRANSPARENT
        activity.enableEdgeToEdge(
            statusBarStyle = if (darkIcons) {
                SystemBarStyle.light(transparent, transparent)
            } else {
                SystemBarStyle.dark(transparent)
            },
            navigationBarStyle = if (darkIcons) {
                SystemBarStyle.light(navColor, navColor)
            } else {
                SystemBarStyle.dark(navColor)
            },
        )
    }
}
