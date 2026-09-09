package com.hinvr.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hinvr.app.navigation.LocalSessionRepository
import com.hinvr.app.ui.components.HinvrBackground
import com.hinvr.app.ui.components.HinvrTextButton
import com.hinvr.app.ui.theme.Atmosphere
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OtpScreen(
    phone: String,
    onVerified: (profileComplete: Boolean) -> Unit,
    onChangeNumber: () -> Unit,
) {
    val session = LocalSessionRepository.current
    val scope = rememberCoroutineScope()
    val colors = HinvrTheme.colors
    var code by remember { mutableStateOf("") }
    var seconds by remember { mutableIntStateOf(30) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(seconds) {
        if (seconds <= 0) return@LaunchedEffect
        delay(1_000)
        seconds -= 1
    }

    fun submit(value: String) {
        if (value.length != 6 || submitting) return
        submitting = true
        error = null
        scope.launch {
            runCatching { session.verifyOtp(value) }
                .onSuccess { onVerified(it) }
                .onFailure {
                    error = it.message ?: "That code didn’t match."
                    code = ""
                    submitting = false
                }
        }
    }

    HinvrBackground(atmosphere = Atmosphere.Sabha) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
        ) {
            Text("OTP", style = HinvrTypography.labelSmall, color = colors.gold)
            Spacer(Modifier.height(16.dp))
            Text("Enter the code", style = HinvrTypography.headlineLarge, color = colors.ink)
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (session.configured) {
                    "Sent to $phone"
                } else {
                    "Sent to $phone  ·  any 6 digits (mock)"
                },
                style = HinvrTypography.bodyMedium,
                color = colors.inkMuted,
            )
            Spacer(Modifier.height(40.dp))
            BasicTextField(
                value = code,
                onValueChange = { value ->
                    val next = value.filter { it.isDigit() }.take(6)
                    code = next
                    if (next.length == 6) submit(next)
                },
                modifier = Modifier.focusRequester(focus),
                enabled = !submitting,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                decorationBox = {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        repeat(6) { index ->
                            val char = code.getOrNull(index)?.toString().orEmpty()
                            Box(
                                modifier = Modifier
                                    .width(48.dp)
                                    .height(58.dp)
                                    .border(
                                        1.dp,
                                        if (index == code.length) colors.gold else colors.ink.copy(alpha = 0.12f),
                                        RoundedCornerShape(14.dp),
                                    )
                                    .background(colors.ivory, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = char,
                                    style = HinvrTypography.headlineMedium.copy(fontSize = 24.sp),
                                    color = colors.ink,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                },
            )
            if (error != null) {
                Spacer(Modifier.height(16.dp))
                Text(error!!, style = HinvrTypography.bodyMedium, color = colors.vermillion)
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = when {
                    seconds > 0 -> "Resend in ${seconds}s"
                    submitting -> "Checking…"
                    else -> "Resend code"
                },
                color = if (seconds > 0 || submitting) colors.inkMuted else colors.gold,
                style = HinvrTypography.bodyMedium,
                modifier = Modifier.clickable(enabled = seconds == 0 && !submitting) {
                    scope.launch {
                        runCatching { session.sendPhoneOtp(phone) }
                            .onSuccess {
                                seconds = 30
                                error = null
                            }
                            .onFailure { error = it.message }
                    }
                },
            )
            Spacer(Modifier.height(8.dp))
            HinvrTextButton("Change number", onChangeNumber)
        }
    }
}
