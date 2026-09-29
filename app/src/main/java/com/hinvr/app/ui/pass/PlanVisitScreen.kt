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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hinvr.app.R
import com.hinvr.app.i18n.userMessage
import com.hinvr.app.data.MemberPlace
import com.hinvr.app.data.SessionSnapshot
import com.hinvr.app.navigation.LocalCatalogRepository
import com.hinvr.app.navigation.LocalSessionRepository
import com.hinvr.app.ui.catalog.Mandir
import com.hinvr.app.ui.components.CatalogPhoto
import com.hinvr.app.ui.components.HinvrBackground
import com.hinvr.app.ui.components.HinvrPrimaryButton
import com.hinvr.app.ui.components.IvoryCard
import com.hinvr.app.ui.components.PhotoScrim
import com.hinvr.app.ui.components.SabhaSearchField
import com.hinvr.app.ui.components.SabhaTopBar
import com.hinvr.app.ui.components.ServicePlacePicker
import com.hinvr.app.ui.components.mandirArtwork
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

private fun visitDateFormat(): DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())

private data class DeskHelp(
    val key: String,
    val labelRes: Int,
    val detailRes: Int,
)

private val PopularTempleIds = listOf("tirupati", "kashi", "shirdi", "somnath")

private val DeskHelpOptions = listOf(
    DeskHelp("Wheelchair", R.string.help_wheelchair, R.string.help_wheelchair_detail),
    DeskHelp("Buggy", R.string.help_buggy, R.string.help_buggy_detail),
    DeskHelp("Prasad", R.string.help_prasad, R.string.help_prasad_detail),
    DeskHelp("Meet at the gate", R.string.help_gate, R.string.help_gate_detail),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanVisitScreen(onBack: () -> Unit, initialMandirId: String = "") {
    val catalog = LocalCatalogRepository.current
    val session = LocalSessionRepository.current
    val mandirs by catalog.mandirs.collectAsStateWithLifecycle()
    val snap by session.snapshot.collectAsStateWithLifecycle(initialValue = SessionSnapshot())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val colors = HinvrTheme.colors
    val temples = mandirs.filter { it.passAccepted }
    var mandirId by remember(initialMandirId) { mutableStateOf(initialMandirId) }
    var templeQuery by remember { mutableStateOf("") }
    var visitDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var partySize by remember { mutableIntStateOf(2) }
    var notes by remember { mutableStateOf("") }
    var assist by remember { mutableStateOf(setOf<String>()) }
    var selectedPlace by remember { mutableStateOf<MemberPlace?>(null) }
    var placeError by remember { mutableStateOf<String?>(null) }
    var submitted by remember { mutableStateOf(false) }
    val place = selectedPlace ?: snap.places.firstOrNull()
    val selected = temples.find { it.id == mandirId }
    val searching = templeQuery.isNotBlank()
    val popular = popularTemples(temples)
    val shown = if (searching) temples.filter { it.matchesTempleQuery(templeQuery) } else popular
    val pinned = selected?.takeIf { !searching && shown.none { temple -> temple.id == it.id } }
    val ready = selected != null && visitDate != null && place != null
    val noExtraHelp = stringResource(R.string.no_extra_help)
    val noNote = stringResource(R.string.no_note)
    val peopleWord = stringResource(R.string.people)
    val personWord = stringResource(R.string.person)
    val helpLabels = DeskHelpOptions.associate { it.key to stringResource(it.labelRes) }

    HinvrBackground(atmosphere = Atmosphere.Sabha) {
        Column(Modifier.fillMaxSize()) {
            SabhaTopBar(title = stringResource(R.string.visit_title), onBack = onBack)
            if (submitted && selected != null && visitDate != null) {
                VisitSent(
                    mandir = selected,
                    date = visitDate!!,
                    partySize = partySize,
                    assist = assist,
                    place = place,
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
                    stringResource(R.string.visit_lead),
                    style = HinvrTypography.headlineMedium,
                    color = colors.ink,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.visit_sub),
                    style = HinvrTypography.bodyLarge,
                    color = colors.inkMuted,
                )

                Spacer(Modifier.height(26.dp))
                SectionHeading(stringResource(R.string.which_temple))
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.which_temple_help),
                    style = HinvrTypography.bodyMedium,
                    color = colors.inkMuted,
                )
                Spacer(Modifier.height(12.dp))
                SabhaSearchField(
                    value = templeQuery,
                    onValueChange = { templeQuery = it },
                    placeholder = stringResource(R.string.temple_search),
                )
                Spacer(Modifier.height(16.dp))
                if (pinned != null) {
                    Text(
                        stringResource(R.string.your_choice),
                        style = HinvrTypography.labelSmall,
                        color = colors.gold,
                    )
                    Spacer(Modifier.height(10.dp))
                    if (pinned.hasOwnTemplePhoto()) {
                        TemplePhotoChoice(
                            mandir = pinned,
                            selected = true,
                            onClick = { mandirId = pinned.id },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        TempleChoice(
                            mandir = pinned,
                            selected = true,
                            onClick = { mandirId = pinned.id },
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
                if (!searching) {
                    Text(
                        stringResource(R.string.popular_now),
                        style = HinvrTypography.labelSmall,
                        color = colors.gold,
                    )
                    Spacer(Modifier.height(10.dp))
                    shown.chunked(2).forEach { row ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            row.forEach { mandir ->
                                TemplePhotoChoice(
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
                } else if (shown.isEmpty()) {
                    Text(
                        stringResource(R.string.no_temple),
                        style = HinvrTypography.titleMedium,
                        color = colors.ink,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.no_temple_body),
                        style = HinvrTypography.bodyMedium,
                        color = colors.inkMuted,
                    )
                } else {
                    shown.forEach { mandir ->
                        if (mandir.hasOwnTemplePhoto()) {
                            TemplePhotoChoice(
                                mandir = mandir,
                                selected = mandir.id == mandirId,
                                onClick = { mandirId = mandir.id },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            TempleChoice(
                                mandir = mandir,
                                selected = mandir.id == mandirId,
                                onClick = { mandirId = mandir.id },
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))
                SectionHeading(stringResource(R.string.visit_date))
                Spacer(Modifier.height(10.dp))
                DateChoice(
                    date = visitDate,
                    onClick = { showDatePicker = true },
                )

                Spacer(Modifier.height(26.dp))
                SectionHeading(stringResource(R.string.how_many))
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.how_many_help),
                    style = HinvrTypography.bodyMedium,
                    color = colors.inkMuted,
                )
                Spacer(Modifier.height(10.dp))
                PartyStepper(
                    count = partySize,
                    onChange = { partySize = it },
                )

                Spacer(Modifier.height(26.dp))
                SectionHeading(stringResource(R.string.help_at_temple))
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.help_at_temple_hint),
                    style = HinvrTypography.bodyMedium,
                    color = colors.inkMuted,
                )
                Spacer(Modifier.height(10.dp))
                DeskHelpOptions.forEach { option ->
                    HelpChoice(
                        option = option,
                        selected = option.key in assist,
                        onClick = {
                            assist = if (option.key in assist) assist - option.key else assist + option.key
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(18.dp))
                SectionHeading(stringResource(R.string.desk_note))
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.desk_note_hint),
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
                ServicePlacePicker(
                    places = snap.places,
                    selected = place,
                    onSelect = { selectedPlace = it },
                    onSaveNew = { newPlace ->
                        scope.launch {
                            placeError = null
                            runCatching { session.addPlace(newPlace) }
                                .onSuccess { selectedPlace = newPlace }
                                .onFailure { placeError = context.userMessage(it.message, R.string.err_save_place) }
                        }
                    },
                    onLocated = { },
                    onError = { placeError = it },
                )
                if (placeError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(placeError!!, style = HinvrTypography.bodyMedium, color = colors.vermillion)
                }

                Spacer(Modifier.height(18.dp))
                if (!ready) {
                    Text(
                        when {
                            selected == null && visitDate == null -> stringResource(R.string.need_temple_date)
                            selected == null -> stringResource(R.string.need_temple)
                            visitDate == null -> stringResource(R.string.need_date)
                            else -> stringResource(R.string.need_place)
                        },
                        style = HinvrTypography.bodyLarge,
                        color = colors.ink,
                    )
                    Spacer(Modifier.height(10.dp))
                }
                HinvrPrimaryButton(
                    text = stringResource(R.string.send_visit),
                    enabled = ready,
                    onClick = {
                        val mandir = selected ?: return@HinvrPrimaryButton
                        val date = visitDate ?: return@HinvrPrimaryButton
                        val chosen = place ?: return@HinvrPrimaryButton
                        val help = assist.map { helpLabels[it] ?: it }.ifEmpty { listOf(noExtraHelp) }.joinToString()
                        val note = notes.trim().ifBlank { noNote }
                        val partyLabel = if (partySize == 1) "1 $personWord" else "$partySize $peopleWord"
                        scope.launch {
                            session.addLocalRequest(
                                "VISIT",
                                "${templeHeadline(mandir)}, ${mandir.place} · ${date.format(visitDateFormat())} · $partyLabel · $help · ${chosen.label}: ${chosen.address} · ${snap.displayName.ifBlank { snap.city }} · $note",
                                mandir.id,
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
    place: MemberPlace?,
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
        Text(stringResource(R.string.request_sent), style = HinvrTypography.headlineLarge, color = colors.ink)
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.visit_sent_body),
            style = HinvrTypography.bodyLarge,
            color = colors.inkMuted,
        )
        Spacer(Modifier.height(22.dp))
        VisitRecap(mandir = mandir, date = date, partySize = partySize, assist = assist)
        if (place != null) {
            Spacer(Modifier.height(8.dp))
            Text("${place.label} · ${place.address}", style = HinvrTypography.bodyLarge, color = colors.inkMuted)
        }
        Spacer(Modifier.height(22.dp))
        HinvrPrimaryButton(stringResource(R.string.back_to_pass), onBack)
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
            .fillMaxWidth()
            .heightIn(min = 72.dp)
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
private fun TemplePhotoChoice(
    mandir: Mandir,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 156.dp,
) {
    val colors = HinvrTheme.colors
    val shape = RoundedCornerShape(20.dp)
    val headline = templeHeadline(mandir)
    val subline = templeSubline(mandir)
    Box(
        modifier
            .height(height)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colors.gold else colors.gold.copy(alpha = 0.28f),
                shape = shape,
            )
            .clip(shape)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.RadioButton
                contentDescription = "$headline, $subline"
            },
    ) {
        CatalogPhoto(
            photoUrl = if (mandir.photoUrl.startsWith("http")) "" else mandir.photoUrl,
            fallback = mandirArtwork(mandir.id),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            contentDescription = null,
        )
        PhotoScrim()
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(colors.ink.copy(alpha = 0.78f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = null,
                    tint = colors.gold,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                headline,
                style = HinvrTypography.titleMedium,
                color = colors.cream,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subline,
                style = HinvrTypography.bodyMedium,
                color = colors.creamMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DateChoice(date: LocalDate?, onClick: () -> Unit) {
    val colors = HinvrTheme.colors
    val shape = RoundedCornerShape(20.dp)
    val visitDateCd = stringResource(R.string.cd_visit_date)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(shape)
            .background(colors.ivory)
            .border(1.dp, colors.gold.copy(alpha = 0.28f), shape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = visitDateCd }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                if (date == null) stringResource(R.string.tap_date) else date.format(visitDateFormat()),
                style = HinvrTypography.titleMedium,
                color = if (date == null) colors.inkMuted else colors.ink,
            )
            if (date == null) {
                Text(
                    stringResource(R.string.calendar_opens),
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
                description = stringResource(R.string.fewer_people),
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
                    if (count == 1) stringResource(R.string.person) else stringResource(R.string.people),
                    style = HinvrTypography.bodyMedium,
                    color = colors.inkMuted,
                )
            }
            StepperButton(
                label = "+",
                description = stringResource(R.string.more_people),
                enabled = count < 12,
                onClick = { onChange(count + 1) },
            )
        }
        if (count >= 12) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.more_than_12),
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
    val label = stringResource(option.labelRes)
    val detail = stringResource(option.detailRes)
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
                contentDescription = label
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
            Text(label, style = HinvrTypography.titleMedium, color = colors.ink)
            Text(detail, style = HinvrTypography.bodyMedium, color = colors.inkMuted)
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
                        stringResource(R.string.note_example),
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
    val people = if (partySize == 1) {
        "1 ${stringResource(R.string.person)}"
    } else {
        "$partySize ${stringResource(R.string.people)}"
    }
    val helpLabels = DeskHelpOptions.associate { it.key to stringResource(it.labelRes) }
    val help = assist.map { helpLabels[it] ?: it }.joinToString().ifBlank { stringResource(R.string.no_extra_help) }
    IvoryCard {
        Text(stringResource(R.string.please_check), style = HinvrTypography.titleMedium, color = colors.ink)
        Spacer(Modifier.height(8.dp))
        Text(
            "${templeHeadline(mandir)} · ${templeSubline(mandir)}",
            style = HinvrTypography.bodyLarge,
            color = colors.ink,
        )
        Text(date.format(visitDateFormat()), style = HinvrTypography.bodyLarge, color = colors.ink)
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
                Text(stringResource(R.string.choose_date))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    ) {
        DatePicker(
            state = state,
            title = {
                Text(
                    stringResource(R.string.choose_visit_date),
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                    style = HinvrTypography.titleLarge,
                )
            },
            showModeToggle = true,
        )
    }
}

private fun popularTemples(temples: List<Mandir>): List<Mandir> {
    val byId = temples.associateBy { it.id }
    val picked = PopularTempleIds.mapNotNull { byId[it] }.toMutableList()
    if (picked.size < 4) {
        for (mandir in temples) {
            if (picked.size >= 4) break
            if (picked.none { it.id == mandir.id }) picked += mandir
        }
    }
    return picked.take(4)
}

private fun Mandir.hasOwnTemplePhoto(): Boolean =
    photoUrl.isNotBlank() || id in PopularTempleIds

private fun Mandir.matchesTempleQuery(query: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true
    return name.contains(q, true) ||
        place.contains(q, true) ||
        city.contains(q, true) ||
        deity.contains(q, true) ||
        templeHeadline(this).contains(q, true) ||
        templeSubline(this).contains(q, true)
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
