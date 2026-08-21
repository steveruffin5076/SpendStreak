package com.spendstreak.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.spendstreak.app.data.Budget
import com.spendstreak.app.data.BudgetPeriodType
import com.spendstreak.app.util.currentCurrencySymbol
import java.util.Locale

private val AMOUNT_PATTERN = Regex("^\\d{0,9}(\\.\\d{0,2})?$")

// Edit/delete for a budget history row — a plain in-place edit, same as the active
// budget's own "EDIT BUDGET" path in BudgetScreen, just reachable from the history list
// instead of the main form. Delete is a real, permanent removal (unlike "Clear Budget" on
// the active one, which retires it into history) — confirmed first, matching the same
// confirm-before-delete pattern already used for transactions in EditTransactionSheet.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBudgetSheet(
    budget: Budget,
    onSave: (Budget) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(budget.name) }
    var amount by remember { mutableStateOf(String.format(Locale.US, "%.2f", budget.amountLimit)) }
    var periodType by remember { mutableStateOf(budget.periodType) }
    var startDateMillis by remember { mutableStateOf(budget.startEpochDay?.let { it * MILLIS_PER_DAY }) }
    var endDateMillis by remember { mutableStateOf(budget.endEpochDay?.let { it * MILLIS_PER_DAY }) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    fun save() {
        val parsedAmount = amount.toDoubleOrNull()
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            statusMessage = "Enter a name for this budget."
            return
        }
        if (parsedAmount == null || parsedAmount <= 0) {
            statusMessage = "Enter a valid amount."
            return
        }
        if (periodType == BudgetPeriodType.CUSTOM) {
            val start = startDateMillis
            val end = endDateMillis
            if (start == null || end == null) {
                statusMessage = "Pick a start and end date."
                return
            }
            if (end < start) {
                statusMessage = "End date must be on or after the start date."
                return
            }
            onSave(
                budget.copy(
                    name = trimmedName,
                    amountLimit = parsedAmount,
                    periodType = BudgetPeriodType.CUSTOM,
                    startEpochDay = start / MILLIS_PER_DAY,
                    endEpochDay = end / MILLIS_PER_DAY
                )
            )
        } else {
            onSave(
                budget.copy(
                    name = trimmedName,
                    amountLimit = parsedAmount,
                    periodType = BudgetPeriodType.MONTHLY,
                    startEpochDay = null,
                    endEpochDay = null
                )
            )
        }
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "EDIT PAST BUDGET", style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("NAME") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { new -> if (AMOUNT_PATTERN.matches(new)) amount = new },
                label = { Text("LIMIT (${currentCurrencySymbol()})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = periodType == BudgetPeriodType.MONTHLY,
                    onClick = { periodType = BudgetPeriodType.MONTHLY },
                    label = { Text("MONTHLY") }
                )
                FilterChip(
                    selected = periodType == BudgetPeriodType.CUSTOM,
                    onClick = { periodType = BudgetPeriodType.CUSTOM },
                    label = { Text("CUSTOM RANGE") }
                )
            }

            if (periodType == BudgetPeriodType.CUSTOM) {
                DateRangeSection(
                    startMillis = startDateMillis,
                    endMillis = endDateMillis,
                    onStartChange = { startDateMillis = it },
                    onEndChange = { endDateMillis = it }
                )
            }

            statusMessage?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            Button(onClick = { save() }, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                Text("SAVE")
            }
            OutlinedButton(
                onClick = { showDeleteConfirm = true },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("DELETE", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this budget?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                    onDismiss()
                }) {
                    Text("DELETE", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}
