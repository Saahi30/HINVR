package com.hinvr.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hinvr.app.R
import com.hinvr.app.ui.theme.HinvrTypography
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** A day from today up to 18 months out. Shared by visit, pooja and any later booking. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HinvrDateDialog(
    current: LocalDate?,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    title: String? = null,
) {
    val today = LocalDate.now()
    val latest = today.plusMonths(18)
    val state = rememberDatePickerState(
        initialSelectedDateMillis = (current ?: today).toPickerMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val day = utcTimeMillis.toPickerDate()
                return !day.isBefore(today) && !day.isAfter(latest)
            }

            override fun isSelectableYear(year: Int): Boolean = year in today.year..latest.year
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val picked = state.selectedDateMillis?.toPickerDate() ?: return@TextButton
                    onConfirm(picked)
                },
            ) {
                Text(confirmLabel)
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
            title = title?.let {
                {
                    Text(
                        it,
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                        style = HinvrTypography.titleLarge,
                    )
                }
            },
            showModeToggle = true,
        )
    }
}

// The picker speaks UTC midnight; a visit date is a calendar day, not an instant.
private fun LocalDate.toPickerMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toPickerDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
