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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.hinvr.app.R
import com.hinvr.app.i18n.userMessage
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = HinvrTheme.colors
    var stage by remember { mutableStateOf(if (startInSignIn) AccountStage.SignIn else AccountStage.SignUp) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var recoveryLink by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    var opensInApp by remember { mutableStateOf(true) }
    var reveal by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val linkError by session.authLinkMessage.collectAsStateWithLifecycle()
    val emailOk = emailOk(email)
    val passwordOk = password.length >= 6
    val signIn = stage == AccountStage.SignIn
    val resetting = stage == AccountStage.Reset
    val pasted = recoveryLink.isNotBlank()
    LaunchedEffect(linkError) {
        if (!linkError.isNullOrBlank()) {
            error = context.userMessage(linkError)
            session.clearAuthLinkMessage()
        }
    }

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
                    when (stage) {
                        AccountStage.SignIn -> stringResource(R.string.auth_welcome)
                        AccountStage.SignUp -> stringResource(R.string.auth_create)
                        AccountStage.Reset -> stringResource(R.string.auth_reset_title)
                    },
                    style = HinvrTypography.headlineLarge,
                    color = colors.ink,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    when (stage) {
                        AccountStage.SignIn -> stringResource(R.string.auth_sign_in_body)
                        AccountStage.SignUp -> stringResource(R.string.auth_create_body)
                        AccountStage.Reset -> stringResource(R.string.auth_reset_body)
                    },
                    style = HinvrTypography.bodyMedium,
                    color = colors.inkMuted,
                )
                Spacer(Modifier.height(28.dp))
                AccountField(
                    value = email,
                    onValueChange = { email = it.trim() },
                    label = stringResource(R.string.field_email),
                    keyboardType = KeyboardType.Email,
                )
                if (resetting) {
                    if (sent) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (opensInApp) {
                                stringResource(R.string.auth_reset_sent_app)
                            } else {
                                stringResource(R.string.auth_reset_sent_paste)
                            },
                            style = HinvrTypography.bodyMedium,
                            color = colors.inkMuted,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    AccountField(
                        value = recoveryLink,
                        onValueChange = { recoveryLink = it.trim() },
                        label = stringResource(R.string.field_reset_link),
                        keyboardType = KeyboardType.Uri,
                    )
                } else {
                    Spacer(Modifier.height(12.dp))
                    AccountField(
                        value = password,
                        onValueChange = { password = it },
                        label = stringResource(R.string.field_password),
                        keyboardType = KeyboardType.Password,
                        visualTransformation = if (reveal) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                    )
                    HinvrTextButton(
                        text = if (reveal) stringResource(R.string.auth_hide_password) else stringResource(R.string.auth_show_password),
                        onClick = { reveal = !reveal },
                    )
                }
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                if (error != null) {
                    Text(error!!, style = HinvrTypography.bodyMedium, color = colors.vermillion)
                    Spacer(Modifier.height(12.dp))
                }
                HinvrPrimaryButton(
                    text = when {
                        resetting && busy && pasted -> stringResource(R.string.auth_checking_link)
                        resetting && busy -> stringResource(R.string.sending)
                        resetting && pasted -> stringResource(R.string.auth_use_link)
                        resetting -> stringResource(R.string.auth_send_reset)
                        busy && signIn -> stringResource(R.string.auth_signing_in)
                        busy -> stringResource(R.string.auth_creating)
                        signIn -> stringResource(R.string.cta_sign_in)
                        else -> stringResource(R.string.cta_join)
                    },
                    enabled = !busy && when {
                        resetting -> pasted || emailOk
                        else -> emailOk && passwordOk
                    },
                    onClick = {
                        busy = true
                        error = null
                        scope.launch {
                            if (resetting && pasted) {
                                runCatching { session.acceptRecoveryLink(recoveryLink) }
                                    .onFailure { error = context.userMessage(it.message, R.string.err_use_link) }
                            } else if (resetting) {
                                runCatching { session.requestPasswordReset(email) }
                                    .onSuccess {
                                        opensInApp = it
                                        sent = true
                                    }
                                    .onFailure { error = context.userMessage(it.message, R.string.err_send_reset) }
                            } else {
                                val result = runCatching {
                                    if (signIn) {
                                        session.signIn(email, password)
                                    } else {
                                        session.createAccount(email, password)
                                    }
                                }
                                result
                                    .onSuccess { onAuthenticated(it) }
                                    .onFailure { error = context.userMessage(it.message, R.string.err_save_account) }
                            }
                            busy = false
                        }
                    },
                )
                if (signIn) {
                    HinvrTextButton(
                        text = stringResource(R.string.auth_forgot),
                        onClick = {
                            stage = AccountStage.Reset
                            error = null
                            sent = false
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }
                Spacer(Modifier.height(8.dp))
                HinvrTextButton(
                    text = when (stage) {
                        AccountStage.SignIn -> stringResource(R.string.cta_continue_phone)
                        AccountStage.SignUp -> stringResource(R.string.cta_have_account)
                        AccountStage.Reset -> stringResource(R.string.auth_back_sign_in)
                    },
                    onClick = {
                        stage = when (stage) {
                            AccountStage.SignUp -> AccountStage.SignIn
                            AccountStage.SignIn -> AccountStage.SignUp
                            AccountStage.Reset -> AccountStage.SignIn
                        }
                        error = null
                        sent = false
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

private enum class AccountStage { SignUp, SignIn, Reset }

@Composable
fun ResetPasswordScreen(
    onPasswordSaved: (profileComplete: Boolean) -> Unit,
    onBackToSignIn: () -> Unit,
) {
    val session = LocalSessionRepository.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = HinvrTheme.colors
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var reveal by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var ready by remember { mutableStateOf<Boolean?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val passwordOk = password.length >= 6
    val matches = password == confirm
    LaunchedEffect(Unit) {
        ready = session.hasAuthSession()
        if (ready == false) {
            error = context.userMessage("That link has expired. Request a new one from sign in.")
        }
    }

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
                Text(stringResource(R.string.reset_choose), style = HinvrTypography.headlineLarge, color = colors.ink)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.reset_body),
                    style = HinvrTypography.bodyMedium,
                    color = colors.inkMuted,
                )
                if (ready == true) {
                    Spacer(Modifier.height(28.dp))
                    AccountField(
                        value = password,
                        onValueChange = { password = it },
                        label = stringResource(R.string.field_new_password),
                        keyboardType = KeyboardType.Password,
                        visualTransformation = if (reveal) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                    )
                    Spacer(Modifier.height(12.dp))
                    AccountField(
                        value = confirm,
                        onValueChange = { confirm = it },
                        label = stringResource(R.string.field_confirm_password),
                        keyboardType = KeyboardType.Password,
                        visualTransformation = if (reveal) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                    )
                    HinvrTextButton(
                        text = if (reveal) stringResource(R.string.auth_hide_password) else stringResource(R.string.auth_show_password),
                        onClick = { reveal = !reveal },
                    )
                }
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                if (confirm.isNotEmpty() && !matches) {
                    Text(
                        stringResource(R.string.passwords_mismatch),
                        style = HinvrTypography.bodyMedium,
                        color = colors.vermillion,
                    )
                    Spacer(Modifier.height(12.dp))
                }
                if (error != null) {
                    Text(error!!, style = HinvrTypography.bodyMedium, color = colors.vermillion)
                    Spacer(Modifier.height(12.dp))
                }
                if (ready == true) {
                    HinvrPrimaryButton(
                        text = if (busy) stringResource(R.string.saving) else stringResource(R.string.save_password),
                        enabled = passwordOk && matches && !busy,
                        onClick = {
                            busy = true
                            error = null
                            scope.launch {
                                runCatching { session.completePasswordReset(password) }
                                    .onSuccess { onPasswordSaved(it) }
                                    .onFailure { error = context.userMessage(it.message, R.string.err_save_password) }
                                busy = false
                            }
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                }
                HinvrTextButton(
                    text = stringResource(R.string.auth_back_sign_in),
                    onClick = {
                        if (busy) return@HinvrTextButton
                        busy = true
                        scope.launch {
                            runCatching { session.signOut() }
                            onBackToSignIn()
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}
