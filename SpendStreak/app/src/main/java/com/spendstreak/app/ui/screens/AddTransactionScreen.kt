package com.spendstreak.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spendstreak.app.data.Account
import com.spendstreak.app.data.Category
import com.spendstreak.app.data.CategoryKind
import com.spendstreak.app.ui.components.CategoryPickerSheet
import com.spendstreak.app.ui.components.RetroPanel
import com.spendstreak.app.util.currentCurrencySymbol

private val AMOUNT_PATTERN = Regex("^\\d{0,9}(\\.\\d{0,2})?$")

private enum class TransactionMode { EXPENSE, INCOME, TRANSFER }

@Composable
fun AddTransactionScreen(
    accounts: List<Account>,
    categories: List<Category>,
    onSaveExpense: (amount: Double, categoryId: Long, accountId: Long, note: String) -> Unit,
    onSaveIncome: (amount: Double, categoryId: Long, accountId: Long, note: String) -> Unit,
    onSaveTransfer: (amount: Double, fromAccountId: Long, toAccountId: Long, note: String) -> Unit,
    onAddCategory: (name: String, kind: String, emoji: String) -> Unit,
    onRenameCategory: (category: Category, name: String, emoji: String) -> Unit,
    onDeleteCategory: (categoryId: Long, onResult: (Boolean) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var mode by rememberSaveable { mutableStateOf(TransactionMode.EXPENSE) }
    var amount by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var selectedExpenseCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedIncomeCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedFromAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedToAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val amountFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(accounts) {
        if (selectedAccountId == null || accounts.none { it.id == selectedAccountId }) {
            selectedAccountId = accounts.firstOrNull()?.id
        }
        if (selectedFromAccountId == null || accounts.none { it.id == selectedFromAccountId }) {
            selectedFromAccountId = accounts.firstOrNull()?.id
        }
        if (selectedToAccountId == null ||
            accounts.none { it.id == selectedToAccountId } ||
            selectedToAccountId == selectedFromAccountId
        ) {
            selectedToAccountId = accounts.firstOrNull { it.id != selectedFromAccountId }?.id
        }
    }

    // Keeps FROM/TO from ever landing on the same account when the user changes FROM
    // after TO was already set to match it.
    LaunchedEffect(selectedFromAccountId) {
        if (selectedToAccountId == selectedFromAccountId) {
            selectedToAccountId = accounts.firstOrNull { it.id != selectedFromAccountId }?.id
        }
    }

    // Same "repair on change" pattern as the account defaulting above — also covers a
    // category the user just deleted out from under the current selection.
    LaunchedEffect(categories) {
        val expenseCategories = categories.filter { it.kind == CategoryKind.EXPENSE }
        if (selectedExpenseCategoryId == null || expenseCategories.none { it.id == selectedExpenseCategoryId }) {
            selectedExpenseCategoryId = expenseCategories.firstOrNull()?.id
        }
        val incomeCategories = categories.filter { it.kind == CategoryKind.INCOME }
        if (selectedIncomeCategoryId == null || incomeCategories.none { it.id == selectedIncomeCategoryId }) {
            selectedIncomeCategoryId = incomeCategories.firstOrNull()?.id
        }
    }

    // Early-return per mode so each branch's account id(s) smart-cast to non-null with
    // no `!!` — validate amount first (mode-agnostic), then mode-specific requirements.
    fun submit() {
        val parsedAmount = amount.toDoubleOrNull()
        if (parsedAmount == null || parsedAmount <= 0) {
            statusMessage = "Enter a valid amount."
            return
        }
        when (mode) {
            TransactionMode.EXPENSE, TransactionMode.INCOME -> {
                val accountId = selectedAccountId
                if (accountId == null) {
                    statusMessage = "No account available yet."
                    return
                }
                if (mode == TransactionMode.EXPENSE) {
                    val categoryId = selectedExpenseCategoryId
                    if (categoryId == null) {
                        statusMessage = "No category available yet."
                        return
                    }
                    onSaveExpense(parsedAmount, categoryId, accountId, note)
                } else {
                    val categoryId = selectedIncomeCategoryId
                    if (categoryId == null) {
                        statusMessage = "No source available yet."
                        return
                    }
                    onSaveIncome(parsedAmount, categoryId, accountId, note)
                }
            }
            TransactionMode.TRANSFER -> {
                val fromId = selectedFromAccountId
                val toId = selectedToAccountId
                if (fromId == null || toId == null) {
                    statusMessage = "No account available yet."
                    return
                }
                if (fromId == toId) {
                    statusMessage = "Choose two different accounts."
                    return
                }
                onSaveTransfer(parsedAmount, fromId, toId, note)
            }
        }
        amount = ""
        note = ""
        keyboardController?.hide()
        statusMessage = "Saved!"
    }

    LaunchedEffect(Unit) {
        amountFocusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = when (mode) {
                TransactionMode.EXPENSE -> "ADD EXPENSE"
                TransactionMode.INCOME -> "ADD INCOME"
                TransactionMode.TRANSFER -> "ADD TRANSFER"
            },
            style = MaterialTheme.typography.headlineMedium
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(TransactionMode.entries) { option ->
                FilterChip(
                    selected = mode == option,
                    enabled = option != TransactionMode.TRANSFER || accounts.size >= 2,
                    onClick = { mode = option; statusMessage = null },
                    label = { Text(option.name) }
                )
            }
        }
        if (accounts.size < 2) {
            Text(
                text = "Add a second account in Settings to enable transfers.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        OutlinedTextField(
            value = amount,
            onValueChange = { new ->
                if (AMOUNT_PATTERN.matches(new)) {
                    amount = new
                    statusMessage = null
                }
            },
            label = { Text("AMOUNT (${currentCurrencySymbol()})") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(amountFocusRequester)
        )

        when (mode) {
            TransactionMode.EXPENSE, TransactionMode.INCOME -> {
                CategoryOrSourceSection(
                    mode = mode,
                    categories = categories,
                    selectedExpenseCategoryId = selectedExpenseCategoryId,
                    selectedIncomeCategoryId = selectedIncomeCategoryId,
                    onExpenseCategorySelected = { selectedExpenseCategoryId = it; statusMessage = null },
                    onIncomeCategorySelected = { selectedIncomeCategoryId = it; statusMessage = null },
                    onAddCategory = onAddCategory,
                    onRenameCategory = onRenameCategory,
                    onDeleteCategory = onDeleteCategory
                )
                AccountSection(
                    accounts = accounts,
                    selectedAccountId = selectedAccountId,
                    onAccountSelected = { selectedAccountId = it }
                )
            }
            TransactionMode.TRANSFER -> {
                TransferAccountsSection(
                    accounts = accounts,
                    selectedFromAccountId = selectedFromAccountId,
                    selectedToAccountId = selectedToAccountId,
                    onFromSelected = { selectedFromAccountId = it },
                    onToSelected = { selectedToAccountId = it }
                )
            }
        }

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("NOTE (OPTIONAL)") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { submit() },
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                when (mode) {
                    TransactionMode.EXPENSE -> "SAVE EXPENSE"
                    TransactionMode.INCOME -> "SAVE INCOME"
                    TransactionMode.TRANSFER -> "SAVE TRANSFER"
                }
            )
        }

        statusMessage?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// Only ever called with mode == EXPENSE or INCOME (see the `when(mode)` dispatch above),
// so the binary checks inside are safe despite TransactionMode having a third case.
//
// Shows a compact "current selection" trigger instead of always expanding every option
// inline — the previous always-expanded 4-column chip grid truncated long names like
// "TRANSPORT"/"ENTERTAINMENT" illegibly. Tapping the trigger opens CategoryPickerSheet,
// a bottom sheet with room for full, un-truncated labels plus add/edit/delete.
@Composable
private fun CategoryOrSourceSection(
    mode: TransactionMode,
    categories: List<Category>,
    selectedExpenseCategoryId: Long?,
    selectedIncomeCategoryId: Long?,
    onExpenseCategorySelected: (Long) -> Unit,
    onIncomeCategorySelected: (Long) -> Unit,
    onAddCategory: (name: String, kind: String, emoji: String) -> Unit,
    onRenameCategory: (category: Category, name: String, emoji: String) -> Unit,
    onDeleteCategory: (categoryId: Long, onResult: (Boolean) -> Unit) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    val isExpense = mode == TransactionMode.EXPENSE
    val kind = if (isExpense) CategoryKind.EXPENSE else CategoryKind.INCOME
    val options = categories.filter { it.kind == kind }
    val selectedId = if (isExpense) selectedExpenseCategoryId else selectedIncomeCategoryId
    val selected = options.find { it.id == selectedId }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (isExpense) "CATEGORY" else "SOURCE",
            style = MaterialTheme.typography.labelLarge
        )
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
                Text(
                    text = "${selected?.emoji ?: "❓"} ${(selected?.name ?: "SELECT").uppercase()}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "CHANGE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (showPicker) {
        CategoryPickerSheet(
            title = if (isExpense) "SELECT CATEGORY" else "SELECT SOURCE",
            categories = options,
            selectedCategoryId = selectedId ?: -1L,
            onSelect = { id -> if (isExpense) onExpenseCategorySelected(id) else onIncomeCategorySelected(id) },
            onAddCategory = { name, emoji -> onAddCategory(name, kind, emoji) },
            onRenameCategory = onRenameCategory,
            onDeleteCategory = { category, onResult -> onDeleteCategory(category.id, onResult) },
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
private fun AccountSection(
    accounts: List<Account>,
    selectedAccountId: Long?,
    onAccountSelected: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "ACCOUNT", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            accounts.forEach { account ->
                FilterChip(
                    selected = selectedAccountId == account.id,
                    onClick = { onAccountSelected(account.id) },
                    label = {
                        Text(text = account.name.uppercase(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    modifier = Modifier.widthIn(max = 160.dp)
                )
            }
        }
    }
}

@Composable
private fun TransferAccountsSection(
    accounts: List<Account>,
    selectedFromAccountId: Long?,
    selectedToAccountId: Long?,
    onFromSelected: (Long) -> Unit,
    onToSelected: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "FROM", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            accounts.forEach { account ->
                FilterChip(
                    selected = selectedFromAccountId == account.id,
                    onClick = { onFromSelected(account.id) },
                    label = {
                        Text(text = account.name.uppercase(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    modifier = Modifier.widthIn(max = 160.dp)
                )
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "TO", style = MaterialTheme.typography.labelLarge)
        // The chip matching the current FROM selection is disabled so the two pickers
        // can't both point at the same account — submit() still re-checks this, since
        // disabling alone doesn't help when fewer than 2 accounts exist.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            accounts.forEach { account ->
                FilterChip(
                    selected = selectedToAccountId == account.id,
                    enabled = account.id != selectedFromAccountId,
                    onClick = { onToSelected(account.id) },
                    label = {
                        Text(text = account.name.uppercase(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    modifier = Modifier.widthIn(max = 160.dp)
                )
            }
        }
    }
}
