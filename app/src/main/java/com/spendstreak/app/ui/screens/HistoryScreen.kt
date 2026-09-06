package com.spendstreak.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spendstreak.app.ads.BannerAdView
import com.spendstreak.app.data.Account
import com.spendstreak.app.data.Category
import com.spendstreak.app.data.Expense
import com.spendstreak.app.data.Income
import com.spendstreak.app.data.Transfer
import com.spendstreak.app.ui.components.DateRangeSection
import com.spendstreak.app.ui.components.EditTransactionSheet
import com.spendstreak.app.ui.components.EditableTransaction
import com.spendstreak.app.ui.components.MILLIS_PER_DAY
import com.spendstreak.app.ui.theme.RetroBlue
import com.spendstreak.app.util.formatCurrency
import com.spendstreak.app.viewmodel.HistoryEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d")

@Composable
fun HistoryScreen(
    entries: List<HistoryEntry>,
    accounts: List<Account>,
    categories: List<Category>,
    onUpdateExpense: (Expense) -> Unit,
    onDeleteExpense: (Long, onResult: (Boolean) -> Unit) -> Unit,
    onUpdateIncome: (Income) -> Unit,
    onDeleteIncome: (Long, onResult: (Boolean) -> Unit) -> Unit,
    onUpdateTransfer: (Transfer) -> Unit,
    onDeleteTransfer: (Long, onResult: (Boolean) -> Unit) -> Unit,
    onAddCategory: (name: String, kind: String, emoji: String) -> Unit,
    onRenameCategory: (category: Category, name: String, emoji: String) -> Unit,
    onDeleteCategory: (categoryId: Long, onResult: (Boolean) -> Unit) -> Unit,
    // Also reused (via AccountsScreen -> MainActivity) as a single account's activity
    // view — same list/edit UI, just pre-filtered, rather than a second near-duplicate
    // screen. filterAccountId null means the ordinary, unfiltered History tab.
    filterAccountId: Long? = null,
    title: String = "HISTORY",
    onBack: (() -> Unit)? = null,
    // Lets the account-detail view (see filterAccountId above) inject its editable
    // opening-balance panel above the transaction list, without HistoryScreen itself
    // needing to know anything about accounts/balances.
    headerContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var editingEntry by remember { mutableStateOf<HistoryEntry?>(null) }
    var startDateMillis by remember { mutableStateOf<Long?>(null) }
    var endDateMillis by remember { mutableStateOf<Long?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val displayedEntries = remember(entries, filterAccountId, startDateMillis, endDateMillis, searchQuery) {
        val (rangeStart, rangeEnd) = dateRangeBounds(startDateMillis, endDateMillis)
        entries.filter { entry ->
            val matchesAccount = filterAccountId == null || when (entry) {
                is HistoryEntry.ExpenseEntry -> entry.expense.accountId == filterAccountId
                is HistoryEntry.IncomeEntry -> entry.income.accountId == filterAccountId
                is HistoryEntry.TransferEntry ->
                    entry.transfer.fromAccountId == filterAccountId || entry.transfer.toAccountId == filterAccountId
            }
            val timestamp = entryTimestamp(entry)
            matchesAccount &&
                timestamp >= rangeStart && timestamp < rangeEnd &&
                entryMatchesQuery(entry, searchQuery)
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
            Text(text = title, style = MaterialTheme.typography.headlineMedium)
        }

        headerContent?.invoke()

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("SEARCH") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )

        Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "FILTER BY DATE", style = MaterialTheme.typography.labelLarge)
                if (startDateMillis != null || endDateMillis != null) {
                    Text(
                        text = "CLEAR",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            startDateMillis = null
                            endDateMillis = null
                        }
                    )
                }
            }
            DateRangeSection(
                startMillis = startDateMillis,
                endMillis = endDateMillis,
                onStartChange = { startDateMillis = it },
                onEndChange = { endDateMillis = it }
            )
        }

        if (displayedEntries.isEmpty()) {
            Text(
                text = when {
                    searchQuery.isNotBlank() -> "No transactions match \"$searchQuery\"."
                    startDateMillis != null || endDateMillis != null -> "No transactions in this date range."
                    filterAccountId == null -> "No transactions logged yet. Add one from the Add tab to get started."
                    else -> "No transactions for this account yet."
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 24.dp)
            )
        } else {
            Text(
                text = "Tap an entry to edit or delete it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )
            LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                items(displayedEntries, key = { entryKey(it) }) { entry ->
                    HistoryRow(entry, onClick = { editingEntry = entry })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                }
            }
        }
        BannerAdView(modifier = Modifier.fillMaxWidth())
    }

    editingEntry?.let { entry ->
        EditTransactionSheet(
            entry = when (entry) {
                is HistoryEntry.ExpenseEntry -> EditableTransaction.ExpenseEdit(entry.expense)
                is HistoryEntry.IncomeEntry -> EditableTransaction.IncomeEdit(entry.income)
                is HistoryEntry.TransferEntry -> EditableTransaction.TransferEdit(entry.transfer)
            },
            accounts = accounts,
            categories = categories,
            onSaveExpense = { onUpdateExpense(it); editingEntry = null },
            onSaveIncome = { onUpdateIncome(it); editingEntry = null },
            onSaveTransfer = { onUpdateTransfer(it); editingEntry = null },
            onAddCategory = onAddCategory,
            onRenameCategory = onRenameCategory,
            onDeleteCategory = onDeleteCategory,
            onDelete = { onResult ->
                when (entry) {
                    is HistoryEntry.ExpenseEntry -> onDeleteExpense(entry.expense.id, onResult)
                    is HistoryEntry.IncomeEntry -> onDeleteIncome(entry.income.id, onResult)
                    is HistoryEntry.TransferEntry -> onDeleteTransfer(entry.transfer.id, onResult)
                }
            },
            onDismiss = { editingEntry = null }
        )
    }
}

private fun entryKey(entry: HistoryEntry): String = when (entry) {
    is HistoryEntry.ExpenseEntry -> "expense-${entry.expense.id}"
    is HistoryEntry.IncomeEntry -> "income-${entry.income.id}"
    is HistoryEntry.TransferEntry -> "transfer-${entry.transfer.id}"
}

private fun entryTimestamp(entry: HistoryEntry): Long = when (entry) {
    is HistoryEntry.ExpenseEntry -> entry.expense.timestampMillis
    is HistoryEntry.IncomeEntry -> entry.income.timestampMillis
    is HistoryEntry.TransferEntry -> entry.transfer.timestampMillis
}

// Matches on category/source name, account name(s), note (all case-insensitive substring),
// or amount — a query that parses as a number matches if it's contained in the entry's
// plain "12.34"-style amount string, so searching "50" finds a 50.00 entry without needing
// the currency symbol or exact decimal precision.
private fun entryMatchesQuery(entry: HistoryEntry, query: String): Boolean {
    if (query.isBlank()) return true
    val q = query.trim()
    val note: String
    val names: List<String>
    val amount: Double
    when (entry) {
        is HistoryEntry.ExpenseEntry -> {
            note = entry.expense.note
            names = listOf(entry.categoryName, entry.accountName)
            amount = entry.expense.amount
        }
        is HistoryEntry.IncomeEntry -> {
            note = entry.income.note
            names = listOf(entry.categoryName, entry.accountName)
            amount = entry.income.amount
        }
        is HistoryEntry.TransferEntry -> {
            note = entry.transfer.note
            names = listOf(entry.fromAccountName, entry.toAccountName)
            amount = entry.transfer.amount
        }
    }
    val textMatch = (names + note).any { it.contains(q, ignoreCase = true) }
    val amountMatch = String.format(Locale.US, "%.2f", amount).contains(q)
    return textMatch || amountMatch
}

// startDateMillis/endDateMillis are DateRangeSection's UTC-midnight epoch-day millis
// (see MILLIS_PER_DAY) — converted here into actual local-zone instants so they can be
// compared directly against entry timestamps. endDateMillis is inclusive of its whole
// day, hence the +1 day exclusive upper bound.
private fun dateRangeBounds(startDateMillis: Long?, endDateMillis: Long?): Pair<Long, Long> {
    val zone = ZoneId.systemDefault()
    val start = startDateMillis
        ?.let { LocalDate.ofEpochDay(it / MILLIS_PER_DAY).atStartOfDay(zone).toInstant().toEpochMilli() }
        ?: Long.MIN_VALUE
    val end = endDateMillis
        ?.let { LocalDate.ofEpochDay(it / MILLIS_PER_DAY).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() }
        ?: Long.MAX_VALUE
    return start to end
}

@Composable
private fun HistoryRow(entry: HistoryEntry, onClick: () -> Unit) {
    when (entry) {
        is HistoryEntry.ExpenseEntry -> ExpenseRow(entry, onClick)
        is HistoryEntry.IncomeEntry -> IncomeRow(entry, onClick)
        is HistoryEntry.TransferEntry -> TransferRow(entry, onClick)
    }
}

@Composable
private fun ExpenseRow(entry: HistoryEntry.ExpenseEntry, onClick: () -> Unit) {
    val expense = entry.expense
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Weighted so a long (free-typed) note is bounded and wraps instead of pushing
        // the amount/date column off the edge of the row.
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(categoryColor(expense.categoryId))
                )
                Text(
                    text = "${entry.categoryEmoji} ${entry.categoryName.uppercase()}",
                    style = MaterialTheme.typography.labelLarge,
                    color = categoryColor(expense.categoryId),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            if (expense.note.isNotEmpty()) {
                Text(
                    text = expense.note,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = formatCurrency(expense.amount), style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${formatDate(expense.timestampMillis)} · ${entry.accountName}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun IncomeRow(entry: HistoryEntry.IncomeEntry, onClick: () -> Unit) {
    val income = entry.income
    val incomeColor = MaterialTheme.colorScheme.tertiary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(incomeColor))
                Text(
                    text = "${entry.categoryEmoji} ${entry.categoryName.uppercase()}",
                    style = MaterialTheme.typography.labelLarge,
                    color = incomeColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            if (income.note.isNotEmpty()) {
                Text(
                    text = income.note,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "+${formatCurrency(income.amount)}",
                style = MaterialTheme.typography.titleMedium,
                color = incomeColor
            )
            Text(
                text = "${formatDate(income.timestampMillis)} · ${entry.accountName}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun TransferRow(entry: HistoryEntry.TransferEntry, onClick: () -> Unit) {
    val transfer = entry.transfer
    // A dedicated, fixed color — transfers aren't a category (they're not income or
    // expense), so they deliberately sit outside categoryColor()'s per-category rotation.
    val neutralColor = RetroBlue
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = "${entry.fromAccountName.uppercase()} → ${entry.toAccountName.uppercase()}",
                style = MaterialTheme.typography.labelLarge,
                color = neutralColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (transfer.note.isNotEmpty()) {
                Text(
                    text = transfer.note,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            // No +/- prefix: a transfer is neither spending nor earning, just moved.
            Text(text = formatCurrency(transfer.amount), style = MaterialTheme.typography.titleMedium)
            Text(text = formatDate(transfer.timestampMillis), style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun formatDate(timestampMillis: Long): String =
    Instant.ofEpochMilli(timestampMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DATE_FORMATTER)

// Deterministic per-category color, not a user-chosen hex — every category (built-in or
// custom) automatically gets a color drawn from the app's own Retro theme palette, so
// nothing a user picks can ever clash with the theme. Stable across recompositions and
// renames (keyed on the immutable categoryId, not the editable name).
@Composable
private fun categoryColor(categoryId: Long): Color {
    val palette = listOf(
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.error,
        RetroBlue
    )
    return palette[(categoryId % palette.size).toInt()]
}
