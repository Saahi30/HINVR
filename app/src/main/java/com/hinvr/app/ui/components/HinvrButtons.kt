package com.hinvr.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hinvr.app.ui.theme.Atmosphere
import com.hinvr.app.ui.theme.HinvrPillRadius
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.LocalAtmosphere

private val ButtonShape = RoundedCornerShape(HinvrPillRadius)

@Composable
fun HinvrPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = HinvrTheme.colors
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.saffron,
            contentColor = colors.cream,
            disabledContainerColor = colors.stoneRaised,
            disabledContentColor = colors.creamMuted,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        Text(text)
    }
}

@Composable
fun HinvrGoldOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = HinvrTheme.colors
    val sabha = LocalAtmosphere.current == Atmosphere.Sabha
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = ButtonShape,
        border = BorderStroke(1.dp, colors.gold.copy(alpha = if (sabha) 0.7f else 1f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (sabha) colors.ink else colors.gold,
        ),
    ) {
        Text(text)
    }
}

@Composable
fun HinvrTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HinvrTheme.colors
    val sabha = LocalAtmosphere.current == Atmosphere.Sabha
    TextButton(onClick = onClick, modifier = modifier) {
        Text(text, color = if (sabha) colors.inkMuted else colors.gold)
    }
}
