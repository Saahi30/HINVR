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

/** Shared layout tokens; screens should compose these instead of inventing spacing. */
object HinvrSpacing {
    val xs = 6.dp
    val sm = 12.dp
    val md = 20.dp
    val lg = 32.dp
    val xl = 48.dp
}

enum class Atmosphere { Sabha, Sanctum }

@Immutable
data class HinvrColors(
    val dusk: Color = TempleStone,
    val duskDeep: Color = TempleStoneDeep,
    val stone: Color = TempleStoneMid,
    val stoneRaised: Color = TempleStoneLight,
    val gold: Color = AntiqueGold,
    val goldDim: Color = AntiqueGoldDim,
    val saffron: Color = Clay,
    val saffronDeep: Color = ClayDeep,
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
    val olive: Color = Olive,
    val burgundy: Color = Burgundy,
    val photoWarmth: Color = PhotoWarmth,
)

val LocalHinvrColors = staticCompositionLocalOf { HinvrColors() }
val LocalAtmosphere = staticCompositionLocalOf { Atmosphere.Sanctum }

val HinvrCardRadius = 4.dp
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
    primary = Burgundy,
    onPrimary = Surface,
    primaryContainer = Color(0xFFE4D5C8),
    onPrimaryContainer = Ink,
    secondary = AntiqueGold,
    onSecondary = Ink,
    secondaryContainer = Surface,
    onSecondaryContainer = Ink,
    tertiary = Clay,
    onTertiary = Surface,
    background = Paper,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = Paper,
    onSurfaceVariant = InkMuted,
    outline = AntiqueGold,
    error = ClayDeep,
    onError = Surface,
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
