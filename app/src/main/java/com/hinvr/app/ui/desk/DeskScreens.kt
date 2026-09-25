package com.hinvr.app.ui.desk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hinvr.app.data.SessionSnapshot
import com.hinvr.app.navigation.LocalSessionRepository
import com.hinvr.app.ui.catalog.HinvrCatalog
import com.hinvr.app.ui.catalog.TileScene
import com.hinvr.app.ui.components.CustomRequestPill
import com.hinvr.app.ui.components.HinvrBackground
import com.hinvr.app.ui.components.HinvrPrimaryButton
import com.hinvr.app.ui.components.IvoryCard
import com.hinvr.app.ui.components.PortraitPhotoCard
import com.hinvr.app.ui.components.SabhaTopBar
import com.hinvr.app.ui.components.SectionTitle
import com.hinvr.app.ui.components.SabhaSearchField
import com.hinvr.app.ui.theme.Atmosphere
import com.hinvr.app.ui.theme.HinvrSideInset
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private val PujaDateFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH)

private val HomePujas = listOf(
    "Satyanarayan Katha",
    "Griha Pravesh",
    "Vastu Shanti",
    "Ganesh Puja",
    "Lakshmi Puja",
    "Navagraha Shanti",
    "Havan",
    "Rudrabhishek",
    "Namkaran",
    "Annaprashan",
    "Mundan",
    "Shraddha",
    "Custom",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PoojaScreen(onBack: () -> Unit, onConcierge: () -> Unit) {
    val session = LocalSessionRepository.current
    val snap by session.snapshot.collectAsStateWithLifecycle(initialValue = SessionSnapshot())
    val scope = rememberCoroutineScope()
    val colors = HinvrTheme.colors
    var puja by remember { mutableStateOf<String?>(null) }
    var customPuja by remember { mutableStateOf("") }
    var preferredDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var timeOfDay by remember { mutableStateOf("Morning") }
    var specialRequest by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    val pujaName = if (puja == "Custom") customPuja.trim() else puja.orEmpty()
    val ready = pujaName.isNotBlank() && preferredDate != null

    HinvrBackground(atmosphere = Atmosphere.Sabha) {
        Column(Modifier.fillMaxSize()) {
            SabhaTopBar(title = "Book a pandit", onBack = onBack)
            if (submitted && preferredDate != null) {
                PujaSent(
                    puja = pujaName,
                    date = preferredDate!!,
                    timeOfDay = timeOfDay,
                    specialRequest = specialRequest.trim(),
                    onBack = onBack,
                )
            } else {
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(horizontal = HinvrSideInset)
                        .padding(bottom = 28.dp),
                ) {
                    Text(
                        "The desk will call to confirm.",
                        style = HinvrTypography.bodyLarge,
                        color = colors.inkMuted,
                    )

                    Spacer(Modifier.height(28.dp))
                    QuietLabel("POOJA")
                    Spacer(Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        HomePujas.forEach { name ->
                            QuietChip(
                                label = name,
                                selected = puja == name,
                                onClick = { puja = name },
                            )
                        }
                    }
                    if (puja == "Custom") {
                        Spacer(Modifier.height(12.dp))
                        SabhaSearchField(customPuja, { customPuja = it }, "Name the puja")
                    }

                    Spacer(Modifier.height(28.dp))
                    QuietLabel("WHEN")
                    Spacer(Modifier.height(12.dp))
                    WhenCard(
                        date = preferredDate,
                        timeOfDay = timeOfDay,
                        onDateClick = { showDatePicker = true },
                        onTimeClick = { timeOfDay = it },
                    )

                    Spacer(Modifier.height(28.dp))
                    QuietLabel("NOTE")
                    Spacer(Modifier.height(12.dp))
                    SabhaSearchField(specialRequest, { specialRequest = it }, "A special request, if any")

                    Spacer(Modifier.height(28.dp))
                    HinvrPrimaryButton(
                        text = "Send request",
                        enabled = ready,
                        onClick = {
                            val date = preferredDate ?: return@HinvrPrimaryButton
                            val note = specialRequest.trim().ifBlank { "No special request" }
                            val city = snap.city.trim().let { if (it.isBlank()) "" else " · $it" }
                            scope.launch {
                                session.addLocalRequest(
                                    "POOJA",
                                    "$pujaName · ${date.format(PujaDateFormat)} · $timeOfDay$city · $note",
                                )
                                submitted = true
                            }
                        },
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        PujaDateDialog(
            current = preferredDate,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                preferredDate = it
                showDatePicker = false
            },
        )
    }
}

@Composable
fun YatraScreen(onBack: () -> Unit, onConcierge: () -> Unit) {
    val colors = HinvrTheme.colors
    HinvrBackground(atmosphere = Atmosphere.Sabha) {
        Column(Modifier.fillMaxSize()) {
            SabhaTopBar(onBack = onBack)
            Column(Modifier.padding(horizontal = HinvrSideInset)) {
                SectionTitle("Club desk, not a portal.")
                Spacer(Modifier.height(8.dp))
                Text(
                    "Enquire first. We arrange the host, not a packaged tour dump.",
                    style = HinvrTypography.bodyLarge,
                    color = colors.inkMuted,
                )
                Spacer(Modifier.height(18.dp))
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = HinvrSideInset),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(HinvrCatalog.mandirs) { row ->
                    PortraitPhotoCard(
                        title = row.name,
                        place = row.city,
                        scene = if (row.id == "kedarnath") TileScene.YatraRoad else row.scene,
                        live = false,
                        onClick = onConcierge,
                    )
                }
            }
            Column(Modifier.padding(HinvrSideInset)) {
                Spacer(Modifier.height(20.dp))
                CustomRequestPill(onClick = onConcierge)
                Spacer(Modifier.height(18.dp))
                WaitlistForm(kind = "YATRA", defaultCity = null)
            }
        }
    }
}

@Composable
private fun PujaSent(
    puja: String,
    date: LocalDate,
    timeOfDay: String,
    specialRequest: String,
    onBack: () -> Unit,
) {
    val colors = HinvrTheme.colors
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = HinvrSideInset)
            .padding(top = 12.dp, bottom = 28.dp),
    ) {
        Text("Request sent.", style = HinvrTypography.headlineLarge, color = colors.ink)
        Spacer(Modifier.height(10.dp))
        Text(
            "The desk will call this phone to confirm.",
            style = HinvrTypography.bodyLarge,
            color = colors.inkMuted,
        )
        Spacer(Modifier.height(22.dp))
        IvoryCard {
            Text(puja, style = HinvrTypography.titleMedium, color = colors.ink)
            Spacer(Modifier.height(6.dp))
            Text(
                "${date.format(PujaDateFormat)} · $timeOfDay",
                style = HinvrTypography.bodyLarge,
                color = colors.ink,
            )
            if (specialRequest.isNotBlank()) {
                Text(specialRequest, style = HinvrTypography.bodyLarge, color = colors.inkMuted)
            }
        }
        Spacer(Modifier.height(22.dp))
        HinvrPrimaryButton("Back", onBack)
    }
}

@Composable
private fun QuietLabel(text: String) {
    Text(
        text,
        style = HinvrTypography.labelSmall,
        color = HinvrTheme.colors.gold,
    )
}

@Composable
private fun QuietChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = HinvrTheme.colors
    val shape = RoundedCornerShape(999.dp)
    Text(
        label,
        style = HinvrTypography.bodyMedium,
        color = colors.ink,
        modifier = Modifier
            .clip(shape)
            .background(if (selected) colors.ivory else colors.linen)
            .border(
                1.dp,
                colors.gold.copy(alpha = if (selected) 0.55f else 0.16f),
                shape,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun WhenCard(
    date: LocalDate?,
    timeOfDay: String,
    onDateClick: () -> Unit,
    onTimeClick: (String) -> Unit,
) {
    val colors = HinvrTheme.colors
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.ivory)
            .border(1.dp, colors.gold.copy(alpha = 0.14f), shape)
            .padding(14.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onDateClick)
                .semantics { contentDescription = "Choose a preferred day" }
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (date == null) "Choose a day" else date.format(PujaDateFormat),
                style = HinvrTypography.bodyLarge,
                color = if (date == null) colors.inkMuted else colors.ink,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = colors.gold.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.linen),
        ) {
            listOf("Morning", "Evening").forEach { label ->
                val selected = timeOfDay == label
                Text(
                    label,
                    style = HinvrTypography.bodyMedium,
                    color = if (selected) colors.ink else colors.inkMuted,
                    modifier = Modifier
                        .weight(1f)
                        .padding(3.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) colors.ivory else colors.linen)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTimeClick(label) },
                        )
                        .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PujaDateDialog(
    current: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val today = LocalDate.now()
    val latest = today.plusMonths(18)
    val state = rememberDatePickerState(
        initialSelectedDateMillis = (current ?: today).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val day = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                return !day.isBefore(today) && !day.isAfter(latest)
            }

            override fun isSelectableYear(year: Int): Boolean = year in today.year..latest.year
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val picked = state.selectedDateMillis?.let {
                    Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                } ?: return@TextButton
                onConfirm(picked)
            }) { Text("Choose this day") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = state)
    }
}

@Composable
private fun WaitlistForm(kind: String, defaultCity: String?) {
    val session = LocalSessionRepository.current
    val snap by session.snapshot.collectAsStateWithLifecycle(initialValue = SessionSnapshot())
    val scope = rememberCoroutineScope()
    val colors = HinvrTheme.colors
    var city by remember(defaultCity, snap.city) { mutableStateOf(defaultCity ?: snap.city) }
    var submitted by remember { mutableStateOf(false) }

    if (submitted) {
        Text(
            "You’re on the ${kind.lowercase()} desk list. We’ll contact you before this opens.",
            style = HinvrTypography.bodyLarge,
            color = colors.gold,
        )
        return
    }
    Text(
        if (kind == "POOJA") "Bring verified pandits to my city" else "Tell me when the club desk opens",
        style = HinvrTypography.titleMedium,
        color = colors.ink,
    )
    Spacer(Modifier.height(10.dp))
    SabhaSearchField(city, { city = it }, "Your city")
    Spacer(Modifier.height(12.dp))
    HinvrPrimaryButton(
        text = "Join waitlist",
        onClick = {
            scope.launch {
                session.addLocalRequest(kind, city.ifBlank { "City to confirm" })
                submitted = true
            }
        },
    )
}
