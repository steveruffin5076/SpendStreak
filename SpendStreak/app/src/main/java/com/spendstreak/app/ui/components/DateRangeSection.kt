package com.spendstreak.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

const val MILLIS_PER_DAY = 86_400_000L
private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy")

// DatePicker's selectedDateMillis is UTC midnight for the picked calendar date, which is
// exactly what epoch-day * MILLIS_PER_DAY represents too — so this stays a plain UTC
// conversion for display, no local-timezone shifting.
fun formatEpochMillis(millis: Long): String =
    LocalDate.ofEpochDay(millis / MILLIS_PER_DAY).format(DATE_FORMATTER)

// Shared by BudgetScreen (custom budget period) and ReportsScreen (custom report range) —
// pulled out so the epoch-day/DatePicker conversion logic lives in exactly one place.
@Composable
fun DateRangeSection(
    startMillis: Long?,
    endMillis: Long?,
    onStartChange: (Long?) -> Unit,
    onEndChange: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        OutlinedButton(
            onClick = { showStartPicker = true },
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.weight(1f)
        ) {
            Text(startMillis?.let { formatEpochMillis(it) } ?: "START DATE")
        }
        OutlinedButton(
            onClick = { showEndPicker = true },
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.weight(1f)
        ) {
            Text(endMillis?.let { formatEpochMillis(it) } ?: "END DATE")
        }
    }

    if (showStartPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = startMillis)
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onStartChange(state.selectedDateMillis)
                    showStartPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text("CANCEL") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showEndPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = endMillis)
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onEndChange(state.selectedDateMillis)
                    showEndPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) { Text("CANCEL") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}
