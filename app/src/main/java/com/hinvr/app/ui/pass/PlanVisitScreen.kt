package com.hinvr.app.ui.pass

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hinvr.app.data.SessionSnapshot
import com.hinvr.app.navigation.LocalCatalogRepository
import com.hinvr.app.navigation.LocalSessionRepository
import com.hinvr.app.ui.catalog.Mandir
import com.hinvr.app.ui.components.HinvrBackground
import com.hinvr.app.ui.components.HinvrPrimaryButton
import com.hinvr.app.ui.components.IvoryCard
import com.hinvr.app.ui.components.SabhaTopBar
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

private val VisitDateFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH)

private data class DeskHelp(
    val label: String,
    val detail: String,
)

private val DeskHelpOptions = listOf(
    DeskHelp("Wheelchair", "Someone in the group cannot walk the distance"),
    DeskHelp("Buggy", "A cart inside the temple, where the temple has one"),
    DeskHelp("Prasad", "Prasad packed to take home"),
    DeskHelp("Meet at the gate", "A person to receive you and walk with you"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanVisitScreen(onBack: () -> Unit) {
    val catalog = LocalCatalogRepository.current
    val session = LocalSessionRepository.current
    val mandirs by catalog.mandirs.collectAsStateWithLifecycle()
    val snap by session.snapshot.collectAsStateWithLifecycle(initialValue = SessionSnapshot())
    val scope = rememberCoroutineScope()
    val colors = HinvrTheme.colors
    val temples = mandirs.filter { it.passAccepted }
    var mandirId by remember { mutableStateOf("") }
    var visitDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var partySize by remember { mutableIntStateOf(2) }
    var notes by remember { mutableStateOf("") }
    var assist by remember { mutableStateOf(setOf<String>()) }
    var submitted by remember { mutableStateOf(false) }
    val selected = temples.find { it.id == mandirId }
    val ready = selected != null && visitDate != null

    HinvrBackground(atmosphere = Atmosphere.Sabha) {
        Column(Modifier.fillMaxSize()) {
            SabhaTopBar(title = "Plan a visit", onBack = onBack)
            if (submitted && selected != null && visitDate != null) {
                VisitSent(
                    mandir = selected,
                    date = visitDate!!,
                    partySize = partySize,
                    assist = assist,
                    onBack = onBack,
                )
                return@Column
            }

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
                    "Tell the desk where you are going, and who is coming.",
                    style = HinvrTypography.headlineMedium,
                    color = colors.ink,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "They will call you on this phone to confirm. Official entry only.",
                    style = HinvrTypography.bodyLarge,
                    color = colors.inkMuted,
                )

                Spacer(Modifier.height(26.dp))
                SectionHeading("Which temple")
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tap one. Every temple here accepts the pass.",
                    style = HinvrTypography.bodyMedium,
                    color = colors.inkMuted,
                )
                Spacer(Modifier.height(12.dp))
                temples.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { mandir ->
                            TempleChoice(
                                mandir = mandir,
                                selected = mandir.id == mandirId,
                                onClick = { mandirId = mandir.id },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                }

                Spacer(Modifier.height(16.dp))
                SectionHeading("Visit date")
                Spacer(Modifier.height(10.dp))
                DateChoice(
                    date = visitDate,
                    onClick = { showDatePicker = true },
                )

                Spacer(Modifier.height(26.dp))
                SectionHeading("How many people")
                Spacer(Modifier.height(4.dp))
                Text(
                    "Count everyone, including you.",
                    style = HinvrTypography.bodyMedium,
                    color = colors.inkMuted,
                )
                Spacer(Modifier.height(10.dp))
                PartyStepper(
                    count = partySize,
                    onChange = { partySize = it },
                )

                Spacer(Modifier.height(26.dp))
                SectionHeading("Help at the temple")
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tap any that apply. You can leave this blank.",
                    style = HinvrTypography.bodyMedium,
                    color = colors.inkMuted,
                )
                Spacer(Modifier.height(10.dp))
                DeskHelpOptions.forEach { option ->
                    HelpChoice(
                        option = option,
                        selected = option.label in assist,
                        onClick = {
                            assist = if (option.label in assist) assist - option.label else assist + option.label
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(18.dp))
                SectionHeading("A note for the desk")
                Spacer(Modifier.height(4.dp))
                Text(
                    "Optional. Names, age, or anything they should know.",
                    style = HinvrTypography.bodyMedium,
                    color = colors.inkMuted,
                )
                Spacer(Modifier.height(10.dp))
                NoteField(
                    value = notes,
                    onValueChange = { notes = it },
                )

                if (selected != null && visitDate != null) {
                    Spacer(Modifier.height(22.dp))
                    VisitRecap(
                        mandir = selected,
                        date = visitDate!!,
                        partySize = partySize,
                        assist = assist,
                    )
                }

                Spacer(Modifier.height(18.dp))
                if (!ready) {
                    Text(
                        when {
                            selected == null && visitDate == null -> "Choose a temple and a date to send."
                            selected == null -> "Choose a temple to send."
                            else -> "Choose a date to send."
                        },
                        style = HinvrTypography.bodyLarge,
                        color = colors.ink,
                    )
                    Spacer(Modifier.height(10.dp))
                }
                HinvrPrimaryButton(
                    text = "Send visit request",
                    enabled = ready,
                    onClick = {
                        val mandir = selected ?: return@HinvrPrimaryButton
                        val date = visitDate ?: return@HinvrPrimaryButton
                        val help = assist.ifEmpty { setOf("No extra help") }.joinToString()
                        val note = notes.trim().ifBlank { "No note" }
                        scope.launch {
                            session.addLocalRequest(
                                "VISIT",
                                "${templeHeadline(mandir)}, ${mandir.place} · ${date.format(VisitDateFormat)} · $partySize people · $help · ${snap.displayName.ifBlank { snap.city }} · $note",
                            )
                            submitted = true
                        }
                    },
                )
            }
        }
    }

    if (showDatePicker) {
        VisitDateDialog(
            current = visitDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { picked ->
                visitDate = picked
                showDatePicker = false
            },
        )
    }
}

@Composable
private fun VisitSent(
    mandir: Mandir,
    date: LocalDate,
    partySize: Int,
    assist: Set<String>,
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
            "The desk will call you on this phone before the visit. Entry and help depend on the temple.",
            style = HinvrTypography.bodyLarge,
            color = colors.inkMuted,
        )
        Spacer(Modifier.height(22.dp))
        VisitRecap(mandir = mandir, date = date, partySize = partySize, assist = assist)
        Spacer(Modifier.height(22.dp))
        HinvrPrimaryButton("Back to pass", onBack)
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(text, style = HinvrTypography.titleLarge, color = HinvrTheme.colors.ink)
}

@Composable
private fun TempleChoice(
    mandir: Mandir,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HinvrTheme.colors
    val shape = RoundedCornerShape(20.dp)
    val headline = templeHeadline(mandir)
    Column(
        modifier
            .heightIn(min = 78.dp)
            .clip(shape)
            .background(if (selected) colors.ink else colors.ivory)
            .border(1.dp, colors.gold.copy(alpha = if (selected) 0.2f else 0.28f), shape)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.RadioButton
                contentDescription = "$headline, ${templeSubline(mandir)}"
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                headline,
                style = HinvrTypography.titleMedium,
                color = if (selected) colors.cream else colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = null,
                    tint = colors.gold,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            templeSubline(mandir),
            style = HinvrTypography.bodyMedium,
            color = if (selected) colors.creamMuted else colors.inkMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DateChoice(date: LocalDate?, onClick: () -> Unit) {
    val colors = HinvrTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(shape)
            .background(colors.ivory)
            .border(1.dp, colors.gold.copy(alpha = 0.28f), shape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Choose visit date" }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                if (date == null) "Tap to choose a date" else date.format(VisitDateFormat),
                style = HinvrTypography.titleMedium,
                color = if (date == null) colors.inkMuted else colors.ink,
            )
            if (date == null) {
                Text(
                    "A calendar will open",
                    style = HinvrTypography.bodyMedium,
                    color = colors.inkMuted,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Icon(
            Icons.Outlined.CalendarMonth,
            contentDescription = null,
            tint = colors.gold,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun PartyStepper(count: Int, onChange: (Int) -> Unit) {
    val colors = HinvrTheme.colors
    IvoryCard {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StepperButton(
                label = "−",
                description = "Fewer people",
                enabled = count > 1,
                onClick = { onChange(count - 1) },
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "$count",
                    style = HinvrTypography.headlineLarge,
                    color = colors.ink,
                )
                Text(
                    if (count == 1) "person" else "people",
                    style = HinvrTypography.bodyMedium,
                    color = colors.inkMuted,
                )
            }
            StepperButton(
                label = "+",
                description = "More people",
                enabled = count < 12,
                onClick = { onChange(count + 1) },
            )
        }
        if (count >= 12) {
            Spacer(Modifier.height(8.dp))
            Text(
                "For more than 12, write the number in the note.",
                style = HinvrTypography.bodyMedium,
                color = colors.inkMuted,
            )
        }
    }
}

@Composable
private fun StepperButton(
    label: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = HinvrTheme.colors
    Box(
        Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(if (enabled) colors.ink else colors.linen)
            .border(1.dp, colors.gold.copy(alpha = if (enabled) 0.2f else 0.35f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = HinvrTypography.headlineMedium,
            color = if (enabled) colors.cream else colors.inkMuted,
        )
    }
}

@Composable
private fun HelpChoice(
    option: DeskHelp,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = HinvrTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(shape)
            .background(colors.ivory)
            .border(1.dp, if (selected) colors.gold else colors.gold.copy(alpha = 0.22f), shape)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Checkbox
                contentDescription = option.label
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (selected) colors.ink else colors.linen)
                .border(1.dp, if (selected) colors.ink else colors.gold.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = null,
                    tint = colors.gold,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(option.label, style = HinvrTypography.titleMedium, color = colors.ink)
            Text(option.detail, style = HinvrTypography.bodyMedium, color = colors.inkMuted)
        }
    }
}

@Composable
private fun NoteField(value: String, onValueChange: (String) -> Unit) {
    val colors = HinvrTheme.colors
    val shape = RoundedCornerShape(20.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .clip(shape)
            .background(colors.ivory)
            .border(1.dp, colors.gold.copy(alpha = 0.22f), shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        textStyle = HinvrTypography.bodyLarge.copy(color = colors.ink),
        cursorBrush = SolidColor(colors.gold),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        "Example: Mother cannot climb stairs. We will arrive by 9 in the morning.",
                        style = HinvrTypography.bodyLarge,
                        color = colors.inkMuted,
                    )
                }
                inner()
            }
        },
    )
}

@Composable
private fun VisitRecap(
    mandir: Mandir,
    date: LocalDate,
    partySize: Int,
    assist: Set<String>,
) {
    val colors = HinvrTheme.colors
    val people = if (partySize == 1) "1 person" else "$partySize people"
    val help = assist.joinToString().ifBlank { "No extra help" }
    IvoryCard {
        Text("Please check", style = HinvrTypography.titleMedium, color = colors.ink)
        Spacer(Modifier.height(8.dp))
        Text(
            "${templeHeadline(mandir)} · ${templeSubline(mandir)}",
            style = HinvrTypography.bodyLarge,
            color = colors.ink,
        )
        Text(date.format(VisitDateFormat), style = HinvrTypography.bodyLarge, color = colors.ink)
        Text("$people · $help", style = HinvrTypography.bodyLarge, color = colors.inkMuted)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VisitDateDialog(
    current: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val today = LocalDate.now()
    val latest = today.plusMonths(18)
    val state = rememberDatePickerState(
        initialSelectedDateMillis = (current ?: today).toPickerMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val day = utcTimeMillis.toVisitDate()
                return !day.isBefore(today) && !day.isAfter(latest)
            }

            override fun isSelectableYear(year: Int): Boolean {
                return year in today.year..latest.year
            }
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val picked = state.selectedDateMillis?.toVisitDate() ?: return@TextButton
                    onConfirm(picked)
                },
            ) {
                Text("Choose this date")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    ) {
        DatePicker(
            state = state,
            title = {
                Text(
                    "Choose the visit date",
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                    style = HinvrTypography.titleLarge,
                )
            },
            showModeToggle = true,
        )
    }
}

private fun templeHeadline(mandir: Mandir): String = when (mandir.id) {
    "vaishno-devi" -> "Vaishno Devi"
    "meenakshi" -> "Meenakshi"
    "siddhivinayak" -> "Siddhivinayak"
    "iskcon-bengaluru" -> "ISKCON"
    "kashi" -> "Kashi"
    else -> mandir.city
}

private fun templeSubline(mandir: Mandir): String = when (mandir.id) {
    "vaishno-devi" -> "Katra"
    "meenakshi" -> "Madurai"
    "siddhivinayak" -> "Mumbai"
    "iskcon-bengaluru" -> "Bengaluru"
    "kashi" -> "Vishwanath, Varanasi"
    "tirupati" -> "Sri Venkateswara"
    "shirdi" -> "Sai Baba"
    "dwarkadhish" -> "Dwarkadhish"
    else -> mandir.name
}

private fun LocalDate.toPickerMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toVisitDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
