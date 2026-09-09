package com.hinvr.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.hinvr.app.ui.theme.ApplyAtmosphereBars
import com.hinvr.app.ui.theme.Atmosphere
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrThemeFor
import com.hinvr.app.ui.theme.LocalAtmosphere

@Composable
fun HinvrBackground(
    modifier: Modifier = Modifier,
    atmosphere: Atmosphere = LocalAtmosphere.current,
    darkIcons: Boolean = atmosphere == Atmosphere.Sabha,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = HinvrTheme.colors
    HinvrThemeFor(atmosphere) {
        ApplyAtmosphereBars(atmosphere, darkIcons)
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    if (atmosphere == Atmosphere.Sabha) {
                        Brush.verticalGradient(listOf(colors.linen, colors.linen, colors.ivory))
                    } else {
                        Brush.verticalGradient(listOf(colors.duskDeep, colors.dusk, colors.stone))
                    },
                ),
            content = content,
        )
    }
}
