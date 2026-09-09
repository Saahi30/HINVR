package com.hinvr.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hinvr.app.ui.theme.Atmosphere
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography
import com.hinvr.app.ui.theme.LocalAtmosphere

@Composable
fun PlaceholderScreen(
    title: String,
    body: String,
    onBack: (() -> Unit)? = null,
    atmosphere: Atmosphere = Atmosphere.Sabha,
) {
    val colors = HinvrTheme.colors
    HinvrBackground(atmosphere = atmosphere) {
        Column(Modifier.fillMaxSize()) {
            SabhaTopBar(onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val sabha = LocalAtmosphere.current == Atmosphere.Sabha
                Text(
                    text = title,
                    style = HinvrTypography.headlineMedium,
                    color = if (sabha) colors.ink else colors.gold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = body,
                    style = HinvrTypography.bodyLarge,
                    color = if (sabha) colors.inkMuted else colors.creamMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
