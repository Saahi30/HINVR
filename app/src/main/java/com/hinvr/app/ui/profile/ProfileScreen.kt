package com.hinvr.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hinvr.app.data.Audience
import com.hinvr.app.data.MemberPlace
import com.hinvr.app.data.MembershipTier
import com.hinvr.app.data.SessionSnapshot
import com.hinvr.app.data.readyPlaces
import com.hinvr.app.navigation.LocalSessionRepository
import com.hinvr.app.ui.components.HinvrBackground
import com.hinvr.app.ui.components.HinvrPrimaryButton
import com.hinvr.app.ui.components.IvoryCard
import com.hinvr.app.ui.components.MemberPlacesEditor
import com.hinvr.app.ui.components.SabhaSearchField
import com.hinvr.app.ui.components.SabhaTopBar
import com.hinvr.app.ui.plans.openMembershipInBrowser
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

private val Languages = listOf("en" to "English", "hi" to "हिन्दी")

private val Audiences = listOf(
    Audience.Me to "Me",
    Audience.Parents to "Parents in India",
    Audience.Family to "Whole family",
)

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLegal: () -> Unit,
    onPlans: () -> Unit,
    onSignedOut: () -> Unit,
) {
    val session = LocalSessionRepository.current
    val context = LocalContext.current
    val snap by session.snapshot.collectAsStateWithLifecycle(initialValue = SessionSnapshot())
    val scope = rememberCoroutineScope()
    val colors = HinvrTheme.colors
    var editing by remember { mutableStateOf(false) }
    var name by remember(snap.displayName) { mutableStateOf(snap.displayName) }
    var city by remember(snap.city) { mutableStateOf(snap.city) }
    var dial by remember(snap.phoneE164) { mutableStateOf(dialFor(snap.phoneE164)) }
    var phone by remember(snap.phoneE164) { mutableStateOf(nationalNumber(snap.phoneE164)) }
    var language by remember(snap.languageTag) { mutableStateOf(snap.languageTag) }
    var audience by remember(snap.audience) { mutableStateOf(snap.audience) }
    var places by remember(snap.places) {
        mutableStateOf(snap.places.ifEmpty { listOf(MemberPlace(label = "Home")) })
    }
    var menu by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var openingMembership by remember { mutableStateOf(false) }
    var browserError by remember { mutableStateOf<String?>(null) }
    val phoneOk = when (dial.code) {
        "+91" -> phone.length == 10
        else -> phone.length in 8..12
    }
    val placesOk = places.readyPlaces() != null

    fun resetForm() {
        name = snap.displayName
        city = snap.city
        dial = dialFor(snap.phoneE164)
        phone = nationalNumber(snap.phoneE164)
        language = snap.languageTag
        audience = snap.audience
        places = snap.places.ifEmpty { listOf(MemberPlace(label = "Home")) }
        saveError = null
    }

    HinvrBackground(atmosphere = Atmosphere.Sabha) {
        Column(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            SabhaTopBar(onBack = onBack)
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 28.dp),
            ) {
                Text(snap.displayName.ifBlank { "Member" }, style = HinvrTypography.headlineLarge, color = colors.ink)
                if (snap.email.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(snap.email, style = HinvrTypography.bodyMedium, color = colors.inkMuted)
                }
                Spacer(Modifier.height(22.dp))

                if (editing) {
                    SabhaSearchField(name, { name = it }, "Member name")
                    Spacer(Modifier.height(10.dp))
                    SabhaSearchField(city, { city = it }, "City")
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
                    Spacer(Modifier.height(20.dp))
                    Text("Language", style = HinvrTypography.labelLarge, color = colors.ink)
                    Spacer(Modifier.height(8.dp))
                    ChipRow(options = Languages, selected = language, onSelect = { language = it })
                    Spacer(Modifier.height(20.dp))
                    Text("Who is this for", style = HinvrTypography.labelLarge, color = colors.ink)
                    Spacer(Modifier.height(8.dp))
                    ChipRow(
                        options = Audiences.map { it.first.name to it.second },
                        selected = audience.name,
                        onSelect = { audience = Audience.valueOf(it) },
                    )
                    Spacer(Modifier.height(8.dp))
                    MemberPlacesEditor(
                        places = places,
                        onChange = { places = it },
                        onLocated = { foundCity ->
                            if (city.isBlank() && foundCity.isNotBlank()) city = foundCity
                            saveError = null
                        },
                        onError = { saveError = it },
                    )
                    if (saveError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(saveError!!, style = HinvrTypography.bodyMedium, color = colors.vermillion)
                    }
                    Spacer(Modifier.height(16.dp))
                    HinvrPrimaryButton(
                        text = if (saving) "Saving…" else "Save profile",
                        enabled = name.isNotBlank() && city.isNotBlank() && phoneOk && placesOk && !saving,
                        onClick = {
                            val savedPlaces = places.readyPlaces() ?: return@HinvrPrimaryButton
                            saving = true
                            saveError = null
                            scope.launch {
                                val saved = runCatching {
                                    session.completeProfile(
                                        name.trim(),
                                        city.trim(),
                                        language,
                                        audience,
                                        dial.code + phone,
                                        savedPlaces,
                                    )
                                }
                                saved
                                    .onSuccess { editing = false }
                                    .onFailure { saveError = it.message ?: "Couldn’t save your profile." }
                                saving = false
                            }
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                } else {
                    IvoryCard {
                        DetailRow("City", snap.city.ifBlank { "Not set" })
                        Spacer(Modifier.height(12.dp))
                        DetailRow("Phone", snap.phoneE164.ifBlank { "Not set" })
                        Spacer(Modifier.height(12.dp))
                        DetailRow("Language", languageLabel(snap.languageTag))
                        Spacer(Modifier.height(12.dp))
                        DetailRow("For", audienceLabel(snap.audience))
                    }
                    Spacer(Modifier.height(14.dp))
                    IvoryCard {
                        Text("Places", style = HinvrTypography.labelLarge, color = colors.gold)
                        Spacer(Modifier.height(10.dp))
                        if (snap.places.isEmpty()) {
                            Text("No places yet.", style = HinvrTypography.bodyMedium, color = colors.inkMuted)
                        } else {
                            snap.places.forEachIndexed { index, place ->
                                if (index > 0) Spacer(Modifier.height(12.dp))
                                Text(place.label, style = HinvrTypography.titleMedium, color = colors.ink)
                                Text(place.address, style = HinvrTypography.bodyMedium, color = colors.inkMuted)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                IvoryCard(onClick = onPlans) {
                    Text("Membership", style = HinvrTypography.labelLarge, color = colors.gold)
                    Spacer(Modifier.height(8.dp))
                    Text(tierLabel(snap.tier), style = HinvrTypography.titleMedium, color = colors.ink)
                    if (snap.memberId.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            listOf(snap.memberId, snap.validUntilLabel).filter { it.isNotBlank() }.joinToString(" · "),
                            style = HinvrTypography.bodyMedium,
                            color = colors.inkMuted,
                        )
                    } else {
                        Spacer(Modifier.height(4.dp))
                        Text("See plans", style = HinvrTypography.bodyMedium, color = colors.inkMuted)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (openingMembership) "Opening…" else "Manage subscription",
                    style = HinvrTypography.titleMedium,
                    color = colors.ink,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !openingMembership) {
                            scope.launch {
                                openingMembership = true
                                browserError = openMembershipInBrowser(context, session)
                                openingMembership = false
                            }
                        }
                        .padding(vertical = 10.dp),
                )
                browserError?.let { message ->
                    Text(message, style = HinvrTypography.bodyMedium, color = colors.inkMuted)
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(6.dp))
                IvoryCard {
                    Text(
                        if (editing) "Cancel editing" else "Edit profile",
                        style = HinvrTypography.titleMedium,
                        color = colors.ink,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (editing) resetForm()
                                editing = !editing
                            }
                            .padding(vertical = 6.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Legal",
                        style = HinvrTypography.titleMedium,
                        color = colors.ink,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onLegal)
                            .padding(vertical = 6.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Sign out",
                    style = HinvrTypography.titleMedium,
                    color = colors.vermillion,
                    modifier = Modifier
                        .clickable {
                            scope.launch {
                                session.signOut()
                                onSignedOut()
                            }
                        }
                        .padding(vertical = 14.dp),
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val colors = HinvrTheme.colors
    Column {
        Text(label, style = HinvrTypography.labelSmall, color = colors.gold)
        Spacer(Modifier.height(2.dp))
        Text(value, style = HinvrTypography.bodyLarge, color = colors.ink)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    val colors = HinvrTheme.colors
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
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

private fun dialFor(phoneE164: String): DialCode {
    val ordered = DialCodes.sortedByDescending { it.code.length }
    return ordered.firstOrNull { phoneE164.startsWith(it.code) } ?: DialCodes.first()
}

private fun nationalNumber(phoneE164: String): String {
    val code = dialFor(phoneE164).code
    return if (phoneE164.startsWith(code)) phoneE164.removePrefix(code) else phoneE164.filter { it.isDigit() }
}

private fun languageLabel(tag: String): String = Languages.firstOrNull { it.first == tag }?.second ?: tag

private fun audienceLabel(audience: Audience): String =
    Audiences.firstOrNull { it.first == audience }?.second ?: audience.name

private fun tierLabel(tier: MembershipTier): String = when (tier) {
    MembershipTier.None -> "No membership yet"
    MembershipTier.Darshan -> "Darshan"
    MembershipTier.Gold -> "Gold"
    MembershipTier.Platinum -> "Platinum"
    MembershipTier.Nri -> "NRI"
}

@Composable
fun LegalScreen(onBack: () -> Unit) {
    val colors = HinvrTheme.colors
    HinvrBackground(atmosphere = Atmosphere.Sabha) {
        Column(Modifier.fillMaxSize()) {
            SabhaTopBar(title = "Legal", onBack = onBack)
            Column(Modifier.padding(horizontal = 22.dp)) {
                IvoryCard {
                    Text(
                        "Terms, privacy, refunds.\nHINVR is not a temple board. Streams are attributed to the temple.",
                        style = HinvrTypography.bodyLarge,
                        color = colors.ink,
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val colors = HinvrTheme.colors
    HinvrBackground(atmosphere = Atmosphere.Sabha) {
        Column(Modifier.fillMaxSize()) {
            SabhaTopBar(title = "Notifications", onBack = onBack)
            Column(Modifier.padding(horizontal = 22.dp, vertical = 24.dp)) {
                Text("We will remind you before aarti.", style = HinvrTypography.headlineMedium, color = colors.ink)
                Spacer(Modifier.height(8.dp))
                Text("Nothing waiting right now.", style = HinvrTypography.bodyLarge, color = colors.inkMuted)
            }
        }
    }
}
