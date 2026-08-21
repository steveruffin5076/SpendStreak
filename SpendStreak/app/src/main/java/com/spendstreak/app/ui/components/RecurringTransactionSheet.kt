package com.spendstreak.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.spendstreak.app.data.Account
import com.spendstreak.app.data.Category
import com.spendstreak.app.data.CategoryKind
import com.spendstreak.app.data.RecurringInterval
import com.spendstreak.app.data.RecurringTransaction
import com.spendstreak.app.data.RecurringTransactionType
import com.spendstreak.app.util.currentCurrencySymbol
import java.time.LocalDate
import java.util.Locale

private val AMOUNT_PATTERN = Regex("^\\d{0,9}(\\.\\d{0,2})?$")

// existing == null means "create"; non-null means "edit" (and offers delete). Mirrors the
// shape of EditTransactionSheet/EditBudgetSheet — a plain bottom sheet, not a full second
// screen, following the same pattern already established for editing things in this app.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTransactionSheet(
    existing: RecurringTransaction?,
    accounts: List<Account>,
    categories: List<Category>,
    onSave: (RecurringTransaction) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    var type by remember { mutableStateOf(existing?.type ?: RecurringTransactionType.EXPENSE) }
    var amount by remember {
        mutableStateOf(existing?.amount?.let { String.format(Locale.US, "%.2f", it) } ?: "")
    }
    var selectedCategoryId by remember { mutableStateOf(existing?.categoryId) }
    var selectedAccountId by remember { mutableStateOf(existing?.accountId) }
    var intervalType by remember { mutableStateOf(existing?.intervalType ?: RecurringInterval.MONTHLY) }
    var nextDueEpochDay by remember {
        mutableStateOf(existing?.nextDueEpochDay ?: LocalDate.now().toEpochDay())
    }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    fun save() {
        val parsedAmount = amount.toDoubleOrNull()
        val categoryId = selectedCategoryId
        val accountId = selectedAccountId
        when {
            parsedAmount == null || parsedAmount <= 0 -> statusMessage = "Enter a valid amount."
            categoryId == null -> statusMessage = "Choose a category."
            accountId == null -> statusMessage = "Choose an account."
            else -> {
                onSave(
                    RecurringTransaction(
                        id = existing?.id ?: 0,
                        type = type,
                        amount = parsedAmount,
                        categoryId = categoryId,
                        accountId = accountId,
                        note = note,
                        intervalType = intervalType,
                        nextDueEpochDay = nextDueEpochDay,
                        active = existing?.active ?: true
                    )
                )
                onDismiss()
            }
        }
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
            Text(
                text = if (existing == null) "ADD RECURRING" else "EDIT RECURRING",
                style = MaterialTheme.typography.headlineSmall
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = type == RecurringTransactionType.EXPENSE,
                    onClick = {
                        type = RecurringTransactionType.EXPENSE
                        selectedCategoryId = null
                    },
                    label = { Text("EXPENSE") }
                )
                FilterChip(
                    selected = type == RecurringTransactionType.INCOME,
                    onClick = {
                        type = RecurringTransactionType.INCOME
                        selectedCategoryId = null
                    },
                    label = { Text("INCOME") }
                )
            }

            OutlinedTextField(
                value = amount,
                onValueChange = { new -> if (AMOUNT_PATTERN.matches(new)) amount = new },
                label = { Text("AMOUNT (${currentCurrencySymbol()})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            val kind = if (type == RecurringTransactionType.EXPENSE) CategoryKind.EXPENSE else CategoryKind.INCOME
            val categoryOptions = categories.filter { it.kind == kind }
            Text(
                text = if (type == RecurringTransactionType.EXPENSE) "CATEGORY" else "SOURCE",
                style = MaterialTheme.typography.labelLarge
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categoryOptions.forEach { category ->
                    FilterChip(
                        selected = selectedCategoryId == category.id,
                        onClick = { selectedCategoryId = category.id },
                        label = { Text("${category.emoji} ${category.name.uppercase()}") }
                    )
                }
            }

            Text(text = "ACCOUNT", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                accounts.forEach { account ->
                    FilterChip(
                        selected = selectedAccountId == account.id,
                        onClick = { selectedAccountId = account.id },
                        label = { Text(account.name.uppercase()) }
                    )
                }
            }

            Text(text = "REPEATS", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = intervalType == RecurringInterval.WEEKLY,
                    onClick = { intervalType = RecurringInterval.WEEKLY },
                    label = { Text("WEEKLY") }
                )
                FilterChip(
                    selected = intervalType == RecurringInterval.MONTHLY,
                    onClick = { intervalType = RecurringInterval.MONTHLY },
                    label = { Text("MONTHLY") }
                )
            }

            Text(text = "NEXT DUE", style = MaterialTheme.typography.labelLarge)
            OutlinedButton(onClick = { showDatePicker = true }, shape = MaterialTheme.shapes.small) {
                Text(formatEpochMillis(nextDueEpochDay * MILLIS_PER_DAY))
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("NOTE (OPTIONAL)") },
                modifier = Modifier.fillMaxWidth()
            )

            statusMessage?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            Button(onClick = { save() }, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                Text("SAVE")
            }
            if (onDelete != null) {
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("DELETE", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = nextDueEpochDay * MILLIS_PER_DAY)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { nextDueEpochDay = it / MILLIS_PER_DAY }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("CANCEL") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this recurring transaction?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete?.invoke()
                    onDismiss()
                }) {
                    Text("DELETE", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("CANCEL") }
            }
        )
    }
}
