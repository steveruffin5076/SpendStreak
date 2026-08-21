package com.spendstreak.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import com.spendstreak.app.data.Account
import com.spendstreak.app.data.Category
import com.spendstreak.app.data.CategoryKind
import com.spendstreak.app.data.Expense
import com.spendstreak.app.data.Income
import com.spendstreak.app.data.Transfer
import com.spendstreak.app.util.currentCurrencySymbol
import java.util.Locale

// Kept separate from AddTransactionScreen's create flow (which already has its own dense
// mode-switching state machine) rather than bolting an edit mode onto it — lower risk of
// regressing the create flow, at the cost of some duplicated layout here. Category/account
// selection here is a plain FlowRow of chips, not the full CategoryPickerSheet — editing is
// a less-frequent action than creating, and nesting one ModalBottomSheet inside another for
// add/edit-category CRUD would be awkward; you can already add/rename/delete categories from
// the Add screen's picker.
sealed interface EditableTransaction {
    data class ExpenseEdit(val expense: Expense) : EditableTransaction
    data class IncomeEdit(val income: Income) : EditableTransaction
    data class TransferEdit(val transfer: Transfer) : EditableTransaction
}

private val AMOUNT_PATTERN = Regex("^\\d{0,9}(\\.\\d{0,2})?$")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionSheet(
    entry: EditableTransaction,
    accounts: List<Account>,
    categories: List<Category>,
    onSaveExpense: (Expense) -> Unit,
    onSaveIncome: (Income) -> Unit,
    onSaveTransfer: (Transfer) -> Unit,
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

    var amount by remember { mutableStateOf(String.format(Locale.US, "%.2f", initialAmount)) }
    var note by remember { mutableStateOf(initialNote) }
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

    fun save() {
        val parsedAmount = amount.toDoubleOrNull()
        if (parsedAmount == null || parsedAmount <= 0) {
            statusMessage = "Enter a valid amount."
            return
        }
        when (entry) {
            is EditableTransaction.ExpenseEdit -> {
                val categoryId = selectedCategoryId
                val accountId = selectedAccountId
                if (categoryId == null || accountId == null) {
                    statusMessage = "Choose a category and account."
                    return
                }
                onSaveExpense(entry.expense.copy(amount = parsedAmount, categoryId = categoryId, accountId = accountId, note = note))
            }
            is EditableTransaction.IncomeEdit -> {
                val categoryId = selectedCategoryId
                val accountId = selectedAccountId
                if (categoryId == null || accountId == null) {
                    statusMessage = "Choose a source and account."
                    return
                }
                onSaveIncome(entry.income.copy(amount = parsedAmount, categoryId = categoryId, accountId = accountId, note = note))
            }
            is EditableTransaction.TransferEdit -> {
                val fromId = selectedFromAccountId
                val toId = selectedToAccountId
                if (fromId == null || toId == null) {
                    statusMessage = "Choose both accounts."
                    return
                }
                if (fromId == toId) {
                    statusMessage = "Choose two different accounts."
                    return
                }
                onSaveTransfer(entry.transfer.copy(amount = parsedAmount, fromAccountId = fromId, toAccountId = toId, note = note))
            }
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
                onValueChange = { new -> if (AMOUNT_PATTERN.matches(new)) amount = new },
                label = { Text("AMOUNT (${currentCurrencySymbol()})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            when (entry) {
                is EditableTransaction.ExpenseEdit, is EditableTransaction.IncomeEdit -> {
                    val kind = if (entry is EditableTransaction.ExpenseEdit) CategoryKind.EXPENSE else CategoryKind.INCOME
                    val options = categories.filter { it.kind == kind }
                    Text(
                        text = if (entry is EditableTransaction.ExpenseEdit) "CATEGORY" else "SOURCE",
                        style = MaterialTheme.typography.labelLarge
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        options.forEach { category ->
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
                }
                is EditableTransaction.TransferEdit -> {
                    Text(text = "FROM", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        accounts.forEach { account ->
                            FilterChip(
                                selected = selectedFromAccountId == account.id,
                                onClick = { selectedFromAccountId = account.id },
                                label = { Text(account.name.uppercase()) }
                            )
                        }
                    }
                    Text(text = "TO", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        accounts.forEach { account ->
                            FilterChip(
                                selected = selectedToAccountId == account.id,
                                enabled = account.id != selectedFromAccountId,
                                onClick = { selectedToAccountId = account.id },
                                label = { Text(account.name.uppercase()) }
                            )
                        }
                    }
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
