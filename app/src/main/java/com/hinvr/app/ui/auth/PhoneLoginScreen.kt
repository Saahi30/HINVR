package com.hinvr.app.ui.auth

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hinvr.app.R
import com.hinvr.app.navigation.LocalSessionRepository
import com.hinvr.app.ui.components.HinvrBackground
import com.hinvr.app.ui.components.HinvrGoldOutlineButton
import com.hinvr.app.ui.components.HinvrPrimaryButton
import com.hinvr.app.ui.theme.Atmosphere
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography
import kotlinx.coroutines.launch

private data class DialCode(val flag: String, val code: String, val label: String)

private val DialCodes = listOf(
    DialCode("🇮🇳", "+91", "India"),
    DialCode("🇺🇸", "+1", "USA"),
    DialCode("🇬🇧", "+44", "UK"),
    DialCode("🇦🇪", "+971", "UAE"),
)

@Composable
fun PhoneLoginScreen(
    onOtpSent: (e164: String) -> Unit,
    onGoogle: () -> Unit = {},
) {
    val session = LocalSessionRepository.current
    val scope = rememberCoroutineScope()
    val colors = HinvrTheme.colors
    var dial by remember { mutableStateOf(DialCodes.first()) }
    var number by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val digitsOk = when (dial.code) {
        "+91" -> number.length == 10
        else -> number.length in 8..12
    }

    HinvrBackground(atmosphere = Atmosphere.Sabha) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("HINVR", style = HinvrTypography.labelSmall, color = colors.gold)
                Spacer(Modifier.height(16.dp))
                Text("Your number", style = HinvrTypography.headlineLarge, color = colors.ink)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (session.configured) {
                        "We’ll text a 6-digit code. Google is faster — we still ask for a phone."
                    } else {
                        "Any 6-digit code works for now. Google is faster — we still ask for a phone."
                    },
                    style = HinvrTypography.bodyMedium,
                    color = colors.inkMuted,
                )
                Spacer(Modifier.height(36.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        TextButton(onClick = { menu = true }) {
                            Text("${dial.flag}  ${dial.code}", style = HinvrTypography.titleLarge, color = colors.ink)
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DialCodes.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text("${item.flag}  ${item.code}  ${item.label}") },
                                    onClick = {
                                        dial = item
                                        menu = false
                                    },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    TextField(
                        value = number,
                        onValueChange = { value ->
                            number = value.filter { it.isDigit() }.take(12)
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("98xxx xxxxx") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        textStyle = HinvrTypography.headlineMedium.copy(fontSize = 28.sp, color = colors.ink),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = colors.gold,
                            unfocusedIndicatorColor = colors.ink.copy(alpha = 0.15f),
                            cursorColor = colors.gold,
                            focusedTextColor = colors.ink,
                            unfocusedTextColor = colors.ink,
                        ),
                    )
                }
            }
            Column {
                if (error != null) {
                    Text(error!!, style = HinvrTypography.bodyMedium, color = colors.vermillion)
                    Spacer(Modifier.height(12.dp))
                }
                HinvrPrimaryButton(
                    text = if (sending) "Sending…" else stringResource(R.string.cta_send_otp),
                    onClick = {
                        val e164 = dial.code + number
                        sending = true
                        error = null
                        scope.launch {
                            runCatching { session.sendPhoneOtp(e164) }
                                .onSuccess { onOtpSent(e164) }
                                .onFailure { error = it.message ?: "Couldn’t send the code." }
                            sending = false
                        }
                    },
                    enabled = digitsOk && !sending,
                )
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = colors.ink.copy(alpha = 0.08f))
                Spacer(Modifier.height(16.dp))
                HinvrGoldOutlineButton(text = "Continue with Google", onClick = onGoogle)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Terms · Privacy",
                    style = HinvrTypography.bodyMedium,
                    color = colors.inkMuted,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}
