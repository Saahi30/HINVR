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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hinvr.app.R
import com.hinvr.app.data.Audience
import com.hinvr.app.data.MemberPlace
import com.hinvr.app.data.readyPlaces
import com.hinvr.app.navigation.LocalSessionRepository
import com.hinvr.app.ui.components.HinvrBackground
import com.hinvr.app.ui.components.HinvrPrimaryButton
import com.hinvr.app.ui.components.MemberPlacesEditor
import com.hinvr.app.ui.theme.Atmosphere
import com.hinvr.app.ui.theme.HinvrPillRadius
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography
import kotlinx.coroutines.launch

private data class DialCode(val flag: String, val code: String)

private val DialCodes = listOf(
    DialCode("🇮🇳", "+91"),
    DialCode("🇺🇸", "+1"),
    DialCode("🇬🇧", "+44"),
    DialCode("🇦🇪", "+971"),
)

@Composable
fun ProfileSetupScreen(onEnter: () -> Unit) {
    val session = LocalSessionRepository.current
    val scope = rememberCoroutineScope()
    val colors = HinvrTheme.colors
    var name by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var places by remember { mutableStateOf(listOf(MemberPlace(label = "Home"))) }
    var dial by remember { mutableStateOf(DialCodes.first()) }
    var phone by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    var language by remember { mutableStateOf("en") }
    var audience by remember { mutableStateOf(Audience.Me) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val phoneOk = when (dial.code) {
        "+91" -> phone.length == 10
        else -> phone.length in 8..12
    }

    HinvrBackground(atmosphere = Atmosphere.Sabha) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            Text("PROFILE", style = HinvrTypography.labelSmall, color = colors.gold)
            Spacer(Modifier.height(12.dp))
            Text("A name for the pass", style = HinvrTypography.headlineLarge, color = colors.ink)
            Spacer(Modifier.height(8.dp))
            Text(
                "We won’t sell membership here. Walk the home screen first.",
                style = HinvrTypography.bodyMedium,
                color = colors.inkMuted,
            )
            Spacer(Modifier.height(28.dp))
            LinenField(value = name, onValueChange = { name = it }, label = "Name")
            Spacer(Modifier.height(16.dp))
            LinenField(value = city, onValueChange = { city = it }, label = "City")
            Spacer(Modifier.height(20.dp))
            MemberPlacesEditor(
                places = places,
                onChange = { places = it },
                onLocated = { foundCity ->
                    if (city.isBlank() && foundCity.isNotBlank()) city = foundCity
                    error = null
                },
                onError = { error = it },
                allowAdd = false,
            )
            Spacer(Modifier.height(16.dp))
            Text("Phone", style = HinvrTypography.labelLarge, color = colors.ink)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    TextButton(onClick = { menu = true }) {
                        Text("${dial.flag}  ${dial.code}", style = HinvrTypography.titleLarge, color = colors.ink)
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DialCodes.forEach { item ->
                            DropdownMenuItem(
                                text = { Text("${item.flag}  ${item.code}") },
                                onClick = {
                                    dial = item
                                    menu = false
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                TextField(
                    value = phone,
                    onValueChange = { phone = it.filter { ch -> ch.isDigit() }.take(12) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("98xxx xxxxx") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = colors.ivory,
                        unfocusedContainerColor = colors.ivory,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = colors.gold,
                        focusedTextColor = colors.ink,
                        unfocusedTextColor = colors.ink,
                    ),
                )
            }
            Spacer(Modifier.height(24.dp))
            Text("Language", style = HinvrTypography.labelLarge, color = colors.ink)
            Spacer(Modifier.height(8.dp))
            ChipRow(
                options = listOf("en" to "English", "hi" to "हिन्दी"),
                selected = language,
                onSelect = { language = it },
            )
            Spacer(Modifier.height(24.dp))
            Text("Who is this for", style = HinvrTypography.labelLarge, color = colors.ink)
            Spacer(Modifier.height(8.dp))
            ChipRow(
                options = listOf(
                    Audience.Me.name to "Me",
                    Audience.Parents.name to "Parents in India",
                    Audience.Family.name to "Whole family",
                ),
                selected = audience.name,
                onSelect = { audience = Audience.valueOf(it) },
            )
            Spacer(Modifier.height(40.dp))
            if (error != null) {
                Text(error!!, style = HinvrTypography.bodyMedium, color = colors.vermillion)
                Spacer(Modifier.height(12.dp))
            }
            HinvrPrimaryButton(
                text = if (saving) "Saving…" else stringResource(R.string.cta_enter),
                enabled = name.isNotBlank() && city.isNotBlank() && phoneOk && places.readyPlaces() != null && !saving,
                onClick = {
                    saving = true
                    error = null
                    scope.launch {
                        val saved = runCatching {
                            session.completeProfile(
                                name.trim(),
                                city.trim(),
                                language,
                                audience,
                                dial.code + phone,
                                places.readyPlaces().orEmpty(),
                            )
                        }
                        saved
                            .onSuccess { onEnter() }
                            .onFailure { error = it.message ?: "Couldn’t save your profile." }
                        saving = false
                    }
                },
            )
        }
    }
}

@Composable
private fun LinenField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = true,
) {
    val colors = HinvrTheme.colors
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)),
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        label = { Text(label) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = colors.ivory,
            unfocusedContainerColor = colors.ivory,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = colors.gold,
            focusedLabelColor = colors.gold,
            unfocusedLabelColor = colors.inkMuted,
            focusedTextColor = colors.ink,
            unfocusedTextColor = colors.ink,
        ),
    )
}

@Composable
private fun ChipRow(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    val colors = HinvrTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (key, label) ->
            val on = key == selected
            Text(
                text = label,
                style = HinvrTypography.labelLarge,
                color = if (on) colors.cream else colors.ink,
                modifier = Modifier
                    .clip(RoundedCornerShape(HinvrPillRadius))
                    .background(if (on) colors.ink else Color.Transparent)
                    .border(1.dp, colors.ink.copy(alpha = 0.2f), RoundedCornerShape(HinvrPillRadius))
                    .clickable { onSelect(key) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}
