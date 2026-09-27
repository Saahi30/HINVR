package com.hinvr.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.hinvr.app.R
import com.hinvr.app.navigation.LocalSessionRepository
import com.hinvr.app.ui.components.HinvrBackground
import com.hinvr.app.ui.components.HinvrPrimaryButton
import com.hinvr.app.ui.components.HinvrTextButton
import com.hinvr.app.ui.theme.Atmosphere
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(
    startInSignIn: Boolean,
    onAuthenticated: (profileComplete: Boolean) -> Unit,
) {
    val session = LocalSessionRepository.current
    val scope = rememberCoroutineScope()
    val colors = HinvrTheme.colors
    var signIn by remember { mutableStateOf(startInSignIn) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var reveal by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val emailOk = emailOk(email)
    val passwordOk = password.length >= 6

    HinvrBackground(atmosphere = Atmosphere.Sabha) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("HINVR", style = HinvrTypography.labelSmall, color = colors.gold)
                Spacer(Modifier.height(16.dp))
                Text(
                    if (signIn) "Welcome back" else "Create your account",
                    style = HinvrTypography.headlineLarge,
                    color = colors.ink,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (signIn) {
                        "Sign in with the email you used to join."
                    } else {
                        "This creates your member account. Next, a name, city, phone, and one address."
                    },
                    style = HinvrTypography.bodyMedium,
                    color = colors.inkMuted,
                )
                Spacer(Modifier.height(28.dp))
                AccountField(
                    value = email,
                    onValueChange = { email = it.trim() },
                    label = "Email",
                    keyboardType = KeyboardType.Email,
                )
                Spacer(Modifier.height(12.dp))
                AccountField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    keyboardType = KeyboardType.Password,
                    visualTransformation = if (reveal) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                )
                HinvrTextButton(
                    text = if (reveal) "Hide password" else "Show password",
                    onClick = { reveal = !reveal },
                )
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                if (error != null) {
                    Text(error!!, style = HinvrTypography.bodyMedium, color = colors.vermillion)
                    Spacer(Modifier.height(12.dp))
                }
                HinvrPrimaryButton(
                    text = when {
                        busy && signIn -> "Signing in…"
                        busy -> "Creating…"
                        signIn -> stringResource(R.string.cta_sign_in)
                        else -> stringResource(R.string.cta_join)
                    },
                    enabled = emailOk && passwordOk && !busy,
                    onClick = {
                        busy = true
                        error = null
                        scope.launch {
                            val result = runCatching {
                                if (signIn) {
                                    session.signIn(email, password)
                                } else {
                                    session.createAccount(email, password)
                                }
                            }
                            result
                                .onSuccess { onAuthenticated(it) }
                                .onFailure { error = it.message ?: "Couldn’t save the account." }
                            busy = false
                        }
                    },
                )
                Spacer(Modifier.height(8.dp))
                HinvrTextButton(
                    text = if (signIn) {
                        stringResource(R.string.cta_continue_phone)
                    } else {
                        stringResource(R.string.cta_have_account)
                    },
                    onClick = {
                        signIn = !signIn
                        error = null
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}

private fun emailOk(value: String): Boolean {
    val at = value.indexOf('@')
    return at > 0 && value.indexOf('.', startIndex = at + 1) > at + 1 && !value.contains(' ')
}

@Composable
private fun AccountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val colors = HinvrTheme.colors
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        textStyle = HinvrTypography.titleLarge.copy(color = colors.ink),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = colors.gold,
            unfocusedIndicatorColor = colors.ink.copy(alpha = 0.15f),
            cursorColor = colors.gold,
            focusedLabelColor = colors.gold,
            unfocusedLabelColor = colors.inkMuted,
            focusedTextColor = colors.ink,
            unfocusedTextColor = colors.ink,
        ),
    )
}
