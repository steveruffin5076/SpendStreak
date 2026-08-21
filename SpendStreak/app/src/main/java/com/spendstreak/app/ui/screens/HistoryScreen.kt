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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import com.spendstreak.app.ui.components.EditTransactionSheet
import com.spendstreak.app.ui.components.EditableTransaction
import com.spendstreak.app.ui.theme.RetroBlue
import com.spendstreak.app.util.formatCurrency
import com.spendstreak.app.viewmodel.HistoryEntry
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    modifier: Modifier = Modifier
) {
    var editingEntry by remember { mutableStateOf<HistoryEntry?>(null) }

    Column(modifier = modifier.fillMaxSize().padding(20.dp)) {
        Text(text = "HISTORY", style = MaterialTheme.typography.headlineMedium)

        if (entries.isEmpty()) {
            Text(
                text = "No transactions logged yet. Add one from the Add tab to get started.",
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
                items(entries, key = { entryKey(it) }) { entry ->
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
