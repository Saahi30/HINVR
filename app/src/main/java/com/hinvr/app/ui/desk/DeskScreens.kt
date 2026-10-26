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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hinvr.app.R
import com.hinvr.app.data.DeskRequest
import com.hinvr.app.data.DeskRequestState
import com.hinvr.app.data.MemberPlace
import com.hinvr.app.data.attempt
import com.hinvr.app.i18n.localized
import com.hinvr.app.navigation.LocalCatalogRepository
import com.hinvr.app.i18n.userMessage
import com.hinvr.app.navigation.LocalSessionRepository
import com.hinvr.app.ui.catalog.TileScene
import com.hinvr.app.ui.components.CustomRequestPill
import com.hinvr.app.ui.components.DeskStateLine
import com.hinvr.app.ui.components.HinvrBackground
import com.hinvr.app.ui.components.HinvrDateDialog
import com.hinvr.app.ui.components.HinvrGoldOutlineButton
import com.hinvr.app.ui.components.HinvrPrimaryButton
import com.hinvr.app.ui.components.IvoryCard
import com.hinvr.app.ui.components.PortraitPhotoCard
import com.hinvr.app.ui.components.SabhaTopBar
import com.hinvr.app.ui.components.SectionTitle
import com.hinvr.app.ui.components.ServicePlacePicker
import com.hinvr.app.ui.components.SabhaSearchField
import com.hinvr.app.ui.theme.Atmosphere
import com.hinvr.app.ui.theme.HinvrSideInset
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private data class PujaOption(val key: String, val labelRes: Int)

private val HomePujas = listOf(
    PujaOption("Satyanarayan Katha", R.string.puja_satyanarayan),
    PujaOption("Griha Pravesh", R.string.puja_griha),
    PujaOption("Vastu Shanti", R.string.puja_vastu),
    PujaOption("Ganesh Puja", R.string.puja_ganesh),
    PujaOption("Lakshmi Puja", R.string.puja_lakshmi),
    PujaOption("Navagraha Shanti", R.string.puja_navagraha),
    PujaOption("Havan", R.string.puja_havan),
    PujaOption("Rudrabhishek", R.string.puja_rudra),
    PujaOption("Namkaran", R.string.puja_namkaran),
    PujaOption("Annaprashan", R.string.puja_annaprashan),
    PujaOption("Mundan", R.string.puja_mundan),
    PujaOption("Shraddha", R.string.puja_shraddha),
    PujaOption("Custom", R.string.puja_custom),
)

@Composable
private fun pujaDateFormat(): DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", LocalConfiguration.current.locales[0])

/** The desk reads English and an ISO date, whatever language the member uses. */
private fun deskDate(date: LocalDate): String =
    "$date (" + date.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)) + ")"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PoojaScreen(onBack: () -> Unit, onConcierge: () -> Unit) {
    val session = LocalSessionRepository.current
    val context = LocalContext.current
    val snap by session.snapshot.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val colors = HinvrTheme.colors
    var puja by rememberSaveable { mutableStateOf<String?>(null) }
    var customPuja by rememberSaveable { mutableStateOf("") }
    var preferredDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var timeOfDay by rememberSaveable { mutableStateOf("Morning") }
    var specialRequest by rememberSaveable { mutableStateOf("") }
    var selectedPlace by rememberSaveable(stateSaver = PlaceSaver) { mutableStateOf<MemberPlace?>(null) }
    var placeError by rememberSaveable { mutableStateOf<String?>(null) }
    var sentId by rememberSaveable { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }
    val sent = sentId?.let { id -> snap.deskRequests.find { it.id == id } }
    val place = selectedPlace ?: snap.places.firstOrNull()
    val pujaLabel = when {
        puja == "Custom" -> customPuja.trim()
        puja != null -> stringResource(HomePujas.first { it.key == puja }.labelRes)
        else -> ""
    }
    val timeLabel = if (timeOfDay == "Evening") {
        stringResource(R.string.evening)
    } else {
        stringResource(R.string.morning)
    }
    val ready = pujaLabel.isNotBlank() && preferredDate != null && place != null

    HinvrBackground(atmosphere = Atmosphere.Sabha) {
        Column(Modifier.fillMaxSize()) {
            SabhaTopBar(title = stringResource(R.string.pooja_title), onBack = onBack)
            if (sent != null && preferredDate != null) {
                PujaSent(
                    request = sent,
                    puja = pujaLabel,
                    date = preferredDate!!,
                    timeOfDay = timeLabel,
                    specialRequest = specialRequest.trim(),
                    place = place,
                    onBack = onBack,
                    onRetry = { scope.launch { session.retryDeskRequest(sent.id) } },
                    onEdit = { sentId = null },
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
                        stringResource(R.string.pooja_lead),
                        style = HinvrTypography.bodyLarge,
                        color = colors.inkMuted,
                    )

                    Spacer(Modifier.height(28.dp))
                    QuietLabel(stringResource(R.string.pooja_label))
                    Spacer(Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        HomePujas.forEach { option ->
                            QuietChip(
                                label = stringResource(option.labelRes),
                                selected = puja == option.key,
                                onClick = { puja = option.key },
                            )
                        }
                    }
                    if (puja == "Custom") {
                        Spacer(Modifier.height(12.dp))
                        SabhaSearchField(customPuja, { customPuja = it }, stringResource(R.string.name_puja))
                    }

                    Spacer(Modifier.height(28.dp))
                    QuietLabel(stringResource(R.string.when_label))
                    Spacer(Modifier.height(12.dp))
                    WhenCard(
                        date = preferredDate,
                        timeOfDay = timeOfDay,
                        onDateClick = { showDatePicker = true },
                        onTimeClick = { timeOfDay = it },
                    )

                    Spacer(Modifier.height(28.dp))
                    QuietLabel(stringResource(R.string.note_label))
                    Spacer(Modifier.height(12.dp))
                    SabhaSearchField(specialRequest, { specialRequest = it }, stringResource(R.string.special_request))

                    Spacer(Modifier.height(28.dp))
                    ServicePlacePicker(
                        places = snap.places,
                        selected = place,
                        onSelect = { selectedPlace = it },
                        onSaveNew = { newPlace ->
                            scope.launch {
                                placeError = null
                                attempt { session.addPlace(newPlace) }
                                    .onSuccess { selectedPlace = newPlace }
                                    .onFailure {
                                        placeError = context.userMessage(it.message, R.string.err_save_place)
                                    }
                            }
                        },
                        onLocated = { },
                        onError = { placeError = it },
                    )
                    if (placeError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(placeError!!, style = HinvrTypography.bodyMedium, color = colors.vermillion)
                    }

                    Spacer(Modifier.height(28.dp))
                    HinvrPrimaryButton(
                        text = if (sending) stringResource(R.string.sending) else stringResource(R.string.send_request),
                        enabled = ready && !sending,
                        onClick = {
                            val date = preferredDate ?: return@HinvrPrimaryButton
                            val chosen = place ?: return@HinvrPrimaryButton
                            // English for the desk: the puja's key name, ISO date, morning or evening.
                            val pujaName = if (puja == "Custom") customPuja.trim() else puja.orEmpty()
                            val note = specialRequest.trim().ifBlank { "No special request" }
                            val city = snap.city.trim().let { if (it.isBlank()) "" else " · $it" }
                            val summary = "$pujaName · ${deskDate(date)} · $timeOfDay$city · " +
                                "${chosen.label}: ${chosen.address} · $note"
                            scope.launch {
                                sending = true
                                try {
                                    sentId = session.submitDeskRequest("POOJA", summary)?.id
                                } finally {
                                    sending = false
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        HinvrDateDialog(
            current = preferredDate,
            confirmLabel = stringResource(R.string.choose_this_day),
            title = stringResource(R.string.choose_day),
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
    val catalog = LocalCatalogRepository.current
    val mandirs by catalog.mandirs.collectAsStateWithLifecycle()
    val rows = mandirs.localized()
    HinvrBackground(atmosphere = Atmosphere.Sabha) {
        Column(
            Modifier
                .fillMaxSize()
                .imePadding()
                .navigationBarsPadding(),
        ) {
            SabhaTopBar(onBack = onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Column(Modifier.padding(horizontal = HinvrSideInset)) {
                    SectionTitle(stringResource(R.string.yatra_title))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.yatra_body),
                        style = HinvrTypography.bodyLarge,
                        color = colors.inkMuted,
                    )
                    Spacer(Modifier.height(18.dp))
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = HinvrSideInset),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(rows, key = { it.id }) { row ->
                        PortraitPhotoCard(
                            title = row.name,
                            place = row.city,
                            scene = if (row.id == "kedarnath") TileScene.YatraRoad else row.scene,
                            live = false,
                            photoUrl = row.photoUrl,
                            mandirId = row.id,
                            onClick = onConcierge,
                        )
                    }
                }
                Column(Modifier.padding(HinvrSideInset)) {
                    Spacer(Modifier.height(20.dp))
                    CustomRequestPill(onClick = onConcierge)
                    Spacer(Modifier.height(18.dp))
                    WaitlistForm(kind = "YATRA", defaultCity = null)
                    Spacer(Modifier.height(18.dp))
                }
            }
        }
    }
}

/** Success only once the server has it; otherwise an honest "saved, not sent yet". */
@Composable
private fun PujaSent(
    request: DeskRequest,
    puja: String,
    date: LocalDate,
    timeOfDay: String,
    specialRequest: String,
    place: MemberPlace?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onEdit: () -> Unit,
) {
    val colors = HinvrTheme.colors
    val (title, body) = when (request.state) {
        DeskRequestState.Sending -> stringResource(R.string.sending) to stringResource(R.string.visit_sending_body)
        DeskRequestState.Sent -> stringResource(R.string.request_sent) to stringResource(R.string.puja_sent_body)
        DeskRequestState.Waiting -> stringResource(R.string.request_saved) to stringResource(R.string.request_saved_body)
        DeskRequestState.Failed -> stringResource(R.string.request_not_sent) to stringResource(R.string.request_not_sent_body)
    }
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = HinvrSideInset)
            .padding(top = 12.dp, bottom = 28.dp),
    ) {
        Text(title, style = HinvrTypography.headlineLarge, color = colors.ink)
        Spacer(Modifier.height(10.dp))
        Text(body, style = HinvrTypography.bodyLarge, color = colors.inkMuted)
        Spacer(Modifier.height(22.dp))
        IvoryCard {
            Text(puja, style = HinvrTypography.titleMedium, color = colors.ink)
            Spacer(Modifier.height(6.dp))
            Text(
                "${date.format(pujaDateFormat())} · $timeOfDay",
                style = HinvrTypography.bodyLarge,
                color = colors.ink,
            )
            if (place != null) {
                Spacer(Modifier.height(6.dp))
                Text("${place.label} · ${place.address}", style = HinvrTypography.bodyLarge, color = colors.inkMuted)
            }
            if (specialRequest.isNotBlank()) {
                Text(specialRequest, style = HinvrTypography.bodyLarge, color = colors.inkMuted)
            }
            Spacer(Modifier.height(8.dp))
            DeskStateLine(request, onRetry)
        }
        Spacer(Modifier.height(22.dp))
        HinvrPrimaryButton(stringResource(R.string.back), onBack)
        if (request.state == DeskRequestState.Failed) {
            Spacer(Modifier.height(8.dp))
            HinvrGoldOutlineButton(stringResource(R.string.edit_request), onEdit)
        }
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
            .selectable(
                selected = selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
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
    val morning = stringResource(R.string.morning)
    val evening = stringResource(R.string.evening)
    val dayCd = stringResource(R.string.cd_day)
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
                .semantics { contentDescription = dayCd }
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (date == null) stringResource(R.string.choose_day) else date.format(pujaDateFormat()),
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
            listOf("Morning" to morning, "Evening" to evening).forEach { (key, label) ->
                val selected = timeOfDay == key
                Text(
                    label,
                    style = HinvrTypography.bodyMedium,
                    color = if (selected) colors.ink else colors.inkMuted,
                    modifier = Modifier
                        .weight(1f)
                        .padding(3.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) colors.ivory else colors.linen)
                        .selectable(
                            selected = selected,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.RadioButton,
                            onClick = { onTimeClick(key) },
                        )
                        .padding(vertical = 14.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun WaitlistForm(kind: String, defaultCity: String?) {
    val session = LocalSessionRepository.current
    val snap by session.snapshot.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val colors = HinvrTheme.colors
    var city by rememberSaveable(defaultCity, snap.city) { mutableStateOf(defaultCity ?: snap.city) }
    var sentId by rememberSaveable { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }
    val sent = sentId?.let { id -> snap.deskRequests.find { it.id == id } }

    if (sent != null) {
        Text(
            when (sent.state) {
                DeskRequestState.Sent -> if (kind == "POOJA") {
                    stringResource(R.string.waitlist_done_pooja)
                } else {
                    stringResource(R.string.waitlist_done_yatra)
                }
                else -> stringResource(R.string.waitlist_saved)
            },
            style = HinvrTypography.bodyLarge,
            color = colors.gold,
        )
        DeskStateLine(sent) { scope.launch { session.retryDeskRequest(sent.id) } }
        return
    }
    Text(
        if (kind == "POOJA") stringResource(R.string.waitlist_pooja) else stringResource(R.string.waitlist_yatra),
        style = HinvrTypography.titleMedium,
        color = colors.ink,
    )
    Spacer(Modifier.height(10.dp))
    SabhaSearchField(city, { city = it }, stringResource(R.string.your_city))
    Spacer(Modifier.height(12.dp))
    HinvrPrimaryButton(
        text = if (sending) stringResource(R.string.sending) else stringResource(R.string.join_waitlist),
        enabled = !sending,
        onClick = {
            scope.launch {
                sending = true
                try {
                    // English for the desk, whatever the app language.
                    val where = city.trim().ifBlank { "City to confirm" }
                    sentId = session.submitDeskRequest(kind, "Waitlist · $where")?.id
                } finally {
                    sending = false
                }
            }
        },
    )
}

private val PlaceSaver = listSaver<MemberPlace?, String>(
    save = { place -> if (place == null) emptyList() else listOf(place.label, place.address) },
    restore = { saved -> if (saved.size == 2) MemberPlace(saved[0], saved[1]) else null },
)
