package com.spendstreak.app.ui.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spendstreak.app.data.Account
import com.spendstreak.app.data.Category
import com.spendstreak.app.data.CategoryKind
import com.spendstreak.app.data.Expense
import com.spendstreak.app.data.Income
import com.spendstreak.app.data.Transfer
import com.spendstreak.app.util.FormValidation
import com.spendstreak.app.util.currentCurrencySymbol
import java.util.Locale

// Kept separate from AddTransactionScreen's create flow (which already has its own dense
// mode-switching state machine) rather than bolting an edit mode onto it — lower risk of
// regressing the create flow, at the cost of some duplicated layout here. Category and
// account selection both use the same compact-trigger-opens-a-sheet pattern as the Add
// screen (CategoryPickerSheet / AccountPickerSheet, nested inside this ModalBottomSheet),
// so add/rename/delete category CRUD is reachable from here too, not just from Add.
sealed interface EditableTransaction {
    data class ExpenseEdit(val expense: Expense) : EditableTransaction
    data class IncomeEdit(val income: Income) : EditableTransaction
    data class TransferEdit(val transfer: Transfer) : EditableTransaction
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionSheet(
    entry: EditableTransaction,
    accounts: List<Account>,
    categories: List<Category>,
    onSaveExpense: (Expense) -> Unit,
    onSaveIncome: (Income) -> Unit,
    onSaveTransfer: (Transfer) -> Unit,
    onAddCategory: (name: String, kind: String, emoji: String, onComplete: () -> Unit) -> Unit,
    onRenameCategory: (category: Category, name: String, emoji: String, onComplete: () -> Unit) -> Unit,
    onDeleteCategory: (categoryId: Long, onResult: (Boolean) -> Unit) -> Unit,
    onDelete: (onResult: (Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val initialAmount = when (entry) {
        is EditableTransaction.ExpenseEdit -> entry.expense.amount
        is EditableTransaction.IncomeEdit -> entry.income.amount
        is EditableTransaction.TransferEdit -> entry.transfer.amount
    }
    val initialNote = when (entry) {
        is EditableTransaction.ExpenseEdit -> entry.expense.note
        is EditableTransaction.IncomeEdit -> entry.income.note
        is EditableTransaction.TransferEdit -> entry.transfer.note
    }
    val initialTimestampMillis = when (entry) {
        is EditableTransaction.ExpenseEdit -> entry.expense.timestampMillis
        is EditableTransaction.IncomeEdit -> entry.income.timestampMillis
        is EditableTransaction.TransferEdit -> entry.transfer.timestampMillis
    }
    val initialExcludedFromBudget = when (entry) {
        is EditableTransaction.ExpenseEdit -> entry.expense.excludedFromBudget
        is EditableTransaction.IncomeEdit -> entry.income.excludedFromBudget
        is EditableTransaction.TransferEdit -> false
    }

    var amount by remember { mutableStateOf(String.format(Locale.US, "%.2f", initialAmount)) }
    var note by remember { mutableStateOf(initialNote) }
    var selectedDateMillis by remember { mutableStateOf(initialTimestampMillis) }
    var excludedFromBudget by remember { mutableStateOf(initialExcludedFromBudget) }
    var selectedCategoryId by remember {
        mutableStateOf(
            when (entry) {
                is EditableTransaction.ExpenseEdit -> entry.expense.categoryId
                is EditableTransaction.IncomeEdit -> entry.income.categoryId
                is EditableTransaction.TransferEdit -> null
            }
        )
    }
    var selectedAccountId by remember {
        mutableStateOf(
            when (entry) {
                is EditableTransaction.ExpenseEdit -> entry.expense.accountId
                is EditableTransaction.IncomeEdit -> entry.income.accountId
                is EditableTransaction.TransferEdit -> null
            }
        )
    }
    var selectedFromAccountId by remember {
        mutableStateOf((entry as? EditableTransaction.TransferEdit)?.transfer?.fromAccountId)
    }
    var selectedToAccountId by remember {
        mutableStateOf((entry as? EditableTransaction.TransferEdit)?.transfer?.toAccountId)
    }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current

    fun save() {
        val amountError = FormValidation.validatePositiveAmount(amount)
        if (amountError != null) {
            statusMessage = amountError
            return
        }
        val parsedAmount = amount.toDoubleOrNull()!!
        when (entry) {
            is EditableTransaction.ExpenseEdit -> {
                val categoryId = selectedCategoryId
                val accountId = selectedAccountId
                if (categoryId == null || accountId == null) {
                    statusMessage = "Choose a category and account."
                    return
                }
                onSaveExpense(
                    entry.expense.copy(
                        amount = parsedAmount,
                        categoryId = categoryId,
                        accountId = accountId,
                        note = note,
                        timestampMillis = selectedDateMillis,
                        excludedFromBudget = excludedFromBudget
                    )
                )
            }
            is EditableTransaction.IncomeEdit -> {
                val categoryId = selectedCategoryId
                val accountId = selectedAccountId
                if (categoryId == null || accountId == null) {
                    statusMessage = "Choose a source and account."
                    return
                }
                onSaveIncome(
                    entry.income.copy(
                        amount = parsedAmount,
                        categoryId = categoryId,
                        accountId = accountId,
                        note = note,
                        timestampMillis = selectedDateMillis,
                        excludedFromBudget = excludedFromBudget
                    )
                )
            }
            is EditableTransaction.TransferEdit -> {
                val fromId = selectedFromAccountId
                val toId = selectedToAccountId
                val transferError = FormValidation.validateTransferAccounts(fromId, toId)
                if (transferError != null) {
                    statusMessage = transferError
                    return
                }
                onSaveTransfer(
                    entry.transfer.copy(
                        amount = parsedAmount,
                        fromAccountId = fromId!!,
                        toAccountId = toId!!,
                        note = note,
                        timestampMillis = selectedDateMillis
                    )
                )
            }
        }
        Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show()
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        SheetFormLayout(
            content = {
            Text(
                text = when (entry) {
                    is EditableTransaction.ExpenseEdit -> "EDIT EXPENSE"
                    is EditableTransaction.IncomeEdit -> "EDIT INCOME"
                    is EditableTransaction.TransferEdit -> "EDIT TRANSFER"
                },
                style = MaterialTheme.typography.headlineSmall
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { new -> if (FormValidation.AMOUNT_INPUT_PATTERN.matches(new)) amount = new },
                label = { Text("AMOUNT (${currentCurrencySymbol()})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            DateFieldSection(
                timestampMillis = selectedDateMillis,
                onDateChange = { selectedDateMillis = it }
            )

            when (entry) {
                is EditableTransaction.ExpenseEdit, is EditableTransaction.IncomeEdit -> {
                    val kind = if (entry is EditableTransaction.ExpenseEdit) CategoryKind.EXPENSE else CategoryKind.INCOME
                    val options = categories.filter { it.kind == kind }
                    val isExpense = entry is EditableTransaction.ExpenseEdit
                    val selectedCategory = options.find { it.id == selectedCategoryId }
                    var showCategoryPicker by remember { mutableStateOf(false) }

                    Text(
                        text = if (isExpense) "CATEGORY" else "SOURCE",
                        style = MaterialTheme.typography.labelLarge
                    )
                    RetroPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCategoryPicker = true }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedCategory?.emoji ?: "❓"} ${(selectedCategory?.name ?: "SELECT").uppercase()}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(text = "CHANGE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (showCategoryPicker) {
                        CategoryPickerSheet(
                            title = if (isExpense) "SELECT CATEGORY" else "SELECT SOURCE",
                            categories = options,
                            selectedCategoryId = selectedCategoryId ?: -1L,
                            onSelect = { selectedCategoryId = it },
                            onAddCategory = { name, emoji, onComplete ->
                                onAddCategory(name, kind, emoji, onComplete)
                            },
                            onRenameCategory = onRenameCategory,
                            onDeleteCategory = { category, onResult -> onDeleteCategory(category.id, onResult) },
                            onDismiss = { showCategoryPicker = false }
                        )
                    }

                    var showAccountPicker by remember { mutableStateOf(false) }
                    val selectedAccount = accounts.find { it.id == selectedAccountId }
                    Text(text = "ACCOUNT", style = MaterialTheme.typography.labelLarge)
                    RetroPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAccountPicker = true }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = (selectedAccount?.name ?: "SELECT").uppercase(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Text(text = "CHANGE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (showAccountPicker) {
                        AccountPickerSheet(
                            title = "SELECT ACCOUNT",
                            accounts = accounts,
                            selectedAccountId = selectedAccountId,
                            onSelect = { selectedAccountId = it },
                            onDismiss = { showAccountPicker = false }
                        )
                    }
                }
                is EditableTransaction.TransferEdit -> {
                    var showFromPicker by remember { mutableStateOf(false) }
                    var showToPicker by remember { mutableStateOf(false) }
                    val selectedFromAccount = accounts.find { it.id == selectedFromAccountId }
                    val selectedToAccount = accounts.find { it.id == selectedToAccountId }

                    Text(text = "FROM", style = MaterialTheme.typography.labelLarge)
                    RetroPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showFromPicker = true }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = (selectedFromAccount?.name ?: "SELECT").uppercase(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Text(text = "CHANGE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (showFromPicker) {
                        AccountPickerSheet(
                            title = "FROM ACCOUNT",
                            accounts = accounts,
                            selectedAccountId = selectedFromAccountId,
                            onSelect = { selectedFromAccountId = it },
                            onDismiss = { showFromPicker = false }
                        )
                    }

                    Text(text = "TO", style = MaterialTheme.typography.labelLarge)
                    RetroPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showToPicker = true }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = (selectedToAccount?.name ?: "SELECT").uppercase(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Text(text = "CHANGE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (showToPicker) {
                        AccountPickerSheet(
                            title = "TO ACCOUNT",
                            accounts = accounts,
                            selectedAccountId = selectedToAccountId,
                            disabledAccountId = selectedFromAccountId,
                            onSelect = { selectedToAccountId = it },
                            onDismiss = { showToPicker = false }
                        )
                    }
                }
            }

            if (entry !is EditableTransaction.TransferEdit) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { excludedFromBudget = !excludedFromBudget }
                ) {
                    Checkbox(checked = excludedFromBudget, onCheckedChange = { excludedFromBudget = it })
                    Text(text = "EXCLUDE FROM MONTHLY BUDGET", style = MaterialTheme.typography.bodyMedium)
                }
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
            },
            actions = {
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
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this transaction?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete { success ->
                        if (success) onDismiss() else statusMessage = "Couldn't delete — please try again."
                    }
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
