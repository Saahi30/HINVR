package com.hinvr.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
                // Ochre is the paper. Scripture contrast is baked in at about 8–12%.
                Image(
                    painter = painterResource(R.drawable.parchment_texture),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 1f,
                )
            }
            content()
        }
    }
}
