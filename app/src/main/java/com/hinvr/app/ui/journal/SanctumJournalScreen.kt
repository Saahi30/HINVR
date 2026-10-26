package com.hinvr.app.ui.journal

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.hinvr.app.R
import com.hinvr.app.data.SanctumJournal
import com.hinvr.app.data.SanctumOffering
import com.hinvr.app.data.SanctumVisit
import com.hinvr.app.data.attempt
import com.hinvr.app.i18n.userMessage
import com.hinvr.app.navigation.LocalSessionRepository
import com.hinvr.app.ui.components.HinvrBackground
import com.hinvr.app.ui.components.IvoryCard
import com.hinvr.app.ui.components.SabhaTopBar
import com.hinvr.app.ui.theme.Atmosphere
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** The member's visits to the sanctum on their Quest, read from their own offerings. */
@Composable
fun SanctumJournalScreen(onBack: () -> Unit) {
    val session = LocalSessionRepository.current
    val context = LocalContext.current
    val colors = HinvrTheme.colors
    var offerings by remember { mutableStateOf<List<SanctumOffering>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attempt { session.sanctumOfferings() }
            .onSuccess { offerings = it }
            .onFailure { error = context.userMessage(it.message) }
    }

    HinvrBackground(atmosphere = Atmosphere.Sabha) {
        Column(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            SabhaTopBar(title = stringResource(R.string.journal_title), onBack = onBack)
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 28.dp),
            ) {
                Text(stringResource(R.string.journal_body), style = HinvrTypography.bodyLarge, color = colors.inkMuted)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.journal_honesty), style = HinvrTypography.bodyMedium, color = colors.gold)
                Spacer(Modifier.height(18.dp))
                val rows = offerings
                when {
                    error != null -> Text(error.orEmpty(), style = HinvrTypography.bodyMedium, color = colors.vermillion)
                    rows == null -> Text(stringResource(R.string.opening), style = HinvrTypography.bodyMedium, color = colors.inkMuted)
                    rows.isEmpty() -> IvoryCard {
                        Text(stringResource(R.string.journal_empty), style = HinvrTypography.bodyLarge, color = colors.ink)
                    }
                    else -> Journal(rows)
                }
            }
        }
    }
}

@Composable
private fun Journal(offerings: List<SanctumOffering>) {
    val colors = HinvrTheme.colors
    val today = LocalDate.now(SanctumJournal.Zone)
    val visits = remember(offerings) { SanctumJournal.visits(offerings) }
    val month = remember(offerings) { SanctumJournal.month(offerings, visits, today) }
    val streak = remember(offerings) { SanctumJournal.streak(visits, today) }

    IvoryCard {
        Text(stringResource(R.string.journal_month), style = HinvrTypography.labelLarge, color = colors.gold)
        Spacer(Modifier.height(8.dp))
        Text(
            listOf(
                pluralStringResource(R.plurals.journal_visits, month.visits, month.visits),
                pluralStringResource(R.plurals.journal_diyas, month.diyas, month.diyas),
                pluralStringResource(R.plurals.journal_japa, month.japaRounds, month.japaRounds),
            ).joinToString(" · "),
            style = HinvrTypography.titleMedium,
            color = colors.ink,
        )
        // Only a run worth noticing; a missed day is never mentioned.
        if (streak >= 2) {
            Spacer(Modifier.height(6.dp))
            Text(
                pluralStringResource(R.plurals.journal_streak, streak, streak),
                style = HinvrTypography.bodyMedium,
                color = colors.inkMuted,
            )
        }
    }
    Spacer(Modifier.height(24.dp))
    Text(stringResource(R.string.journal_recent), style = HinvrTypography.labelLarge, color = colors.gold)
    Spacer(Modifier.height(10.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SanctumJournal.byDay(visits).forEach { (day, dayVisits) ->
            Text(dayLabel(day, today), style = HinvrTypography.titleMedium, color = colors.ink)
            dayVisits.forEach { VisitCard(it, today) }
        }
    }
}

@Composable
private fun VisitCard(visit: SanctumVisit, today: LocalDate) {
    val colors = HinvrTheme.colors
    val context = LocalContext.current
    IvoryCard {
        Text(
            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                .withLocale(Locale.getDefault())
                .format(visit.start.atZone(SanctumJournal.Zone)),
            style = HinvrTypography.bodyMedium,
            color = colors.inkMuted,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            visit.kinds.joinToString(" · ") { kind ->
                val name = kindLabel(kind)?.let { context.getString(it) } ?: kind
                val times = visit.counts[kind] ?: 1
                if (times > 1) "$name ×$times" else name
            },
            style = HinvrTypography.titleMedium,
            color = colors.ink,
        )
        visit.sankalp?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = HinvrTypography.bodyMedium, color = colors.inkMuted)
        }
        val dedication = visit.forName?.let { stringResource(R.string.journal_for_named, it) }
            ?: visit.dedicatedTo?.let { forLabel(it) }?.let { stringResource(it) }
        if (dedication != null) {
            Spacer(Modifier.height(4.dp))
            Text(dedication, style = HinvrTypography.bodyMedium, color = colors.gold)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.journal_share),
                style = HinvrTypography.titleMedium,
                color = colors.vermillion,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button) { share(context, visit, today) }
                    .padding(vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun dayLabel(day: LocalDate, today: LocalDate): String = when (day) {
    today -> stringResource(R.string.journal_today)
    today.minusDays(1) -> stringResource(R.string.journal_yesterday)
    else -> DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()).format(day)
}

/** Plain text to the share sheet: who it was for, and that it was symbolic. */
private fun share(context: Context, visit: SanctumVisit, today: LocalDate) {
    val whom = visit.forName ?: visit.dedicatedTo?.let { forPhrase(it) }?.let { context.getString(it) } ?: return
    val onDay = visit.start.atZone(SanctumJournal.Zone).toLocalDate() == today
    val line = when {
        "diya" in visit.kinds && onDay -> R.string.journal_share_diya_today
        "diya" in visit.kinds -> R.string.journal_share_diya
        onDay -> R.string.journal_share_offering_today
        else -> R.string.journal_share_offering
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, context.getString(line, whom))
    }
    context.startActivity(Intent.createChooser(send, context.getString(R.string.journal_share)))
}

@StringRes
private fun kindLabel(kind: String): Int? = when (kind) {
    "ghanta" -> R.string.journal_kind_ghanta
    "diya" -> R.string.journal_kind_diya
    "aarti" -> R.string.journal_kind_aarti
    "pushp" -> R.string.journal_kind_pushp
    "agarbatti" -> R.string.journal_kind_agarbatti
    "prasad" -> R.string.journal_kind_prasad
    "tilak" -> R.string.journal_kind_tilak
    "daan" -> R.string.journal_kind_daan
    "japa" -> R.string.journal_kind_japa
    "abhishek" -> R.string.journal_kind_abhishek
    "deepmala" -> R.string.journal_kind_deepmala
    else -> null
}

@StringRes
private fun forLabel(dedication: String): Int? = when (dedication) {
    "self" -> R.string.journal_for_self
    "family" -> R.string.journal_for_family
    "parents" -> R.string.journal_for_parents
    "children" -> R.string.journal_for_children
    "ancestors" -> R.string.journal_for_ancestors
    "friend" -> R.string.journal_for_friend
    "teacher" -> R.string.journal_for_teacher
    else -> null
}

@StringRes
private fun forPhrase(dedication: String): Int? = when (dedication) {
    "self" -> R.string.journal_whom_self
    "family" -> R.string.journal_whom_family
    "parents" -> R.string.journal_whom_parents
    "children" -> R.string.journal_whom_children
    "ancestors" -> R.string.journal_whom_ancestors
    "friend" -> R.string.journal_whom_friend
    "teacher" -> R.string.journal_whom_teacher
    else -> null
}
