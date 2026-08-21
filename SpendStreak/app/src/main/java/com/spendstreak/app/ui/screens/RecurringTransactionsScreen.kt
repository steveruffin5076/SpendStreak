package com.spendstreak.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spendstreak.app.data.Account
import com.spendstreak.app.data.Category
import com.spendstreak.app.data.RecurringInterval
import com.spendstreak.app.data.RecurringTransaction
import com.spendstreak.app.data.RecurringTransactionType
import com.spendstreak.app.ui.components.RecurringTransactionSheet
import com.spendstreak.app.ui.components.RetroPanel
import com.spendstreak.app.ui.components.formatEpochMillis
import com.spendstreak.app.util.formatCurrency

private const val MILLIS_PER_DAY = 86_400_000L

@Composable
fun RecurringTransactionsScreen(
    recurringTransactions: List<RecurringTransaction>,
    accounts: List<Account>,
    categories: List<Category>,
    onAdd: (RecurringTransaction) -> Unit,
    onUpdate: (RecurringTransaction) -> Unit,
    onDelete: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddSheet by remember { mutableStateOf(false) }
    var editingRecurring by remember { mutableStateOf<RecurringTransaction?>(null) }
    val categoryById = remember(categories) { categories.associateBy { it.id } }
    val accountById = remember(accounts) { accounts.associateBy { it.id } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(text = "RECURRING", style = MaterialTheme.typography.headlineMedium)
        }

        Button(onClick = { showAddSheet = true }, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
            Text("+ ADD RECURRING")
        }

        if (recurringTransactions.isEmpty()) {
            Text(
                text = "No recurring transactions yet. Add a bill or paycheck to get reminders.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            recurringTransactions.forEach { recurring ->
                val category = categoryById[recurring.categoryId]
                val account = accountById[recurring.accountId]
                RetroPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editingRecurring = recurring }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${category?.emoji ?: "❓"} ${(category?.name ?: "Unknown").uppercase()}",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = "${account?.name?.uppercase() ?: "UNKNOWN"} · " +
                                    if (recurring.intervalType == RecurringInterval.WEEKLY) "WEEKLY" else "MONTHLY",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            val prefix = if (recurring.type == RecurringTransactionType.INCOME) "+" else ""
                            Text(text = "$prefix${formatCurrency(recurring.amount)}", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Next: ${formatEpochMillis(recurring.nextDueEpochDay * MILLIS_PER_DAY)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        RecurringTransactionSheet(
            existing = null,
            accounts = accounts,
            categories = categories,
            onSave = { onAdd(it); showAddSheet = false },
            onDelete = null,
            onDismiss = { showAddSheet = false }
        )
    }

    editingRecurring?.let { existing ->
        RecurringTransactionSheet(
            existing = existing,
            accounts = accounts,
            categories = categories,
            onSave = { onUpdate(it); editingRecurring = null },
            onDelete = { onDelete(existing.id); editingRecurring = null },
            onDismiss = { editingRecurring = null }
        )
    }
}
