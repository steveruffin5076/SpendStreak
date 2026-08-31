package com.spendstreak.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TRANSACTION_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy")

fun formatTransactionDate(timestampMillis: Long): String =
    Instant.ofEpochMilli(timestampMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        .format(TRANSACTION_DATE_FORMATTER)

// Swaps only the calendar-date part of an existing timestamp, keeping its original
// time-of-day — so picking a different date for a transaction doesn't also silently
// shift what time it happened.
fun withDate(timestampMillis: Long, newDateUtcMidnightMillis: Long): Long {
    val newDate = LocalDate.ofEpochDay(newDateUtcMidnightMillis / MILLIS_PER_DAY)
    val zone = ZoneId.systemDefault()
    val originalTime = Instant.ofEpochMilli(timestampMillis).atZone(zone).toLocalTime()
    return newDate.atTime(originalTime).atZone(zone).toInstant().toEpochMilli()
}

// Same compact-trigger-opens-a-picker pattern as the CATEGORY/ACCOUNT sections next to
// it on both the Add and Edit forms.
@Composable
fun DateFieldSection(
    timestampMillis: Long,
    onDateChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "DATE", style = MaterialTheme.typography.labelLarge)
        RetroPanel(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showPicker = true }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = formatTransactionDate(timestampMillis), style = MaterialTheme.typography.bodyMedium)
                Text(text = "CHANGE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    if (showPicker) {
        val currentLocalDate = Instant.ofEpochMilli(timestampMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val state = rememberDatePickerState(initialSelectedDateMillis = currentLocalDate.toEpochDay() * MILLIS_PER_DAY)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onDateChange(withDate(timestampMillis, it)) }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("CANCEL") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}
