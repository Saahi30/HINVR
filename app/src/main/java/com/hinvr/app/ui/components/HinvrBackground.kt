package com.hinvr.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.res.imageResource
import com.hinvr.app.R
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
                        colors.linen
                    } else {
                        colors.duskDeep
                    },
                ),
        ) {
            if (atmosphere == Atmosphere.Sabha) {
                // Ochre is the paper: a small seamless grain, tiled. Text-free by design.
                val grain = ImageBitmap.imageResource(R.drawable.paper_grain)
                val brush = remember(grain) { ShaderBrush(ImageShader(grain, TileMode.Repeated, TileMode.Repeated)) }
                Box(Modifier.fillMaxSize().background(brush))
            }
            content()
        }
    }
}
