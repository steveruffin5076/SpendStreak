package com.spendstreak.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.spendstreak.app.data.Budget
import com.spendstreak.app.data.BudgetPeriodType
import com.spendstreak.app.ui.components.DateRangeSection
import com.spendstreak.app.ui.components.EditBudgetSheet
import com.spendstreak.app.ui.components.MILLIS_PER_DAY
import com.spendstreak.app.ui.components.RetroPanel
import com.spendstreak.app.ui.components.RetroProgressBar
import com.spendstreak.app.ui.components.formatEpochMillis
import com.spendstreak.app.util.currentCurrencySymbol
import com.spendstreak.app.util.formatAmount
import com.spendstreak.app.util.formatCurrency
import com.spendstreak.app.viewmodel.BudgetProgress

private val AMOUNT_PATTERN = Regex("^\\d{0,9}(\\.\\d{0,2})?$")

@Composable
fun BudgetScreen(
    budget: Budget?,
    budgetHistory: List<Budget>,
    budgetProgress: BudgetProgress?,
    onSetMonthlyBudget: (name: String, amount: Double) -> Unit,
    onSetCustomBudget: (name: String, amount: Double, startEpochDay: Long, endEpochDay: Long) -> Unit,
    onUpdateBudget: (Budget) -> Unit,
    onDeleteBudget: (Long) -> Unit,
    onClearBudget: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Keyed on `budget` so the form resyncs whenever it changes externally (e.g. right
    // after SAVE round-trips through the DB, or after CLEAR sets it back to null) —
    // otherwise this composable instance survives budget changes and the fields go stale.
    var name by remember(budget) { mutableStateOf(budget?.name ?: "") }
    var amount by remember(budget) { mutableStateOf(budget?.amountLimit?.let { formatAmount(it) } ?: "") }
    var periodType by remember(budget) { mutableStateOf(budget?.periodType ?: BudgetPeriodType.MONTHLY) }
    var startDateMillis by remember(budget) { mutableStateOf(budget?.startEpochDay?.let { it * MILLIS_PER_DAY }) }
    var endDateMillis by remember(budget) { mutableStateOf(budget?.endEpochDay?.let { it * MILLIS_PER_DAY }) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    // Whether the form is drafting a brand-new budget period (which retires the current
    // one to history) instead of editing the active budget in place. Only meaningful when
    // `budget != null` — with no active budget there's nothing to "start new" instead of.
    var startingNew by remember { mutableStateOf(false) }
    var editingHistoryBudget by remember { mutableStateOf<Budget?>(null) }

    fun submit() {
        val parsedAmount = amount.toDoubleOrNull()
        val trimmedName = name.trim()
        val currentBudget = budget
        val editingExisting = currentBudget != null && !startingNew
        statusMessage = when {
            trimmedName.isBlank() -> "Enter a name for this budget."
            parsedAmount == null || parsedAmount <= 0 -> "Enter a valid amount."
            periodType == BudgetPeriodType.MONTHLY -> {
                if (editingExisting && currentBudget != null) {
                    onUpdateBudget(
                        currentBudget.copy(
                            name = trimmedName,
                            amountLimit = parsedAmount,
                            periodType = BudgetPeriodType.MONTHLY,
                            startEpochDay = null,
                            endEpochDay = null
                        )
                    )
                    "Budget updated."
                } else {
                    onSetMonthlyBudget(trimmedName, parsedAmount)
                    startingNew = false
                    "Monthly budget set."
                }
            }
            else -> {
                val start = startDateMillis
                val end = endDateMillis
                when {
                    start == null || end == null -> "Pick a start and end date."
                    end < start -> "End date must be on or after the start date."
                    editingExisting && currentBudget != null -> {
                        onUpdateBudget(
                            currentBudget.copy(
                                name = trimmedName,
                                amountLimit = parsedAmount,
                                periodType = BudgetPeriodType.CUSTOM,
                                startEpochDay = start / MILLIS_PER_DAY,
                                endEpochDay = end / MILLIS_PER_DAY
                            )
                        )
                        "Budget updated."
                    }
                    else -> {
                        onSetCustomBudget(trimmedName, parsedAmount, start / MILLIS_PER_DAY, end / MILLIS_PER_DAY)
                        startingNew = false
                        "Custom budget set."
                    }
                }
            }
        }
    }

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
            Text(text = "BUDGET", style = MaterialTheme.typography.headlineMedium)
        }

        if (budgetProgress != null) {
            val accentColor = if (budgetProgress.isOverBudget) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            }
            RetroPanel(modifier = Modifier.fillMaxWidth(), borderColor = accentColor) {
                Text(text = "CURRENT PROGRESS", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = "${formatCurrency(budgetProgress.spent)} / ${formatAmount(budgetProgress.limit)}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
                RetroProgressBar(
                    progress = (budgetProgress.spent / budgetProgress.limit).toFloat(),
                    filledColor = accentColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
            }
        }

        RetroPanel(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        budget == null -> "SET BUDGET"
                        startingNew -> "START NEW BUDGET"
                        else -> "EDIT BUDGET"
                    },
                    style = MaterialTheme.typography.labelLarge
                )
                if (budget != null) {
                    OutlinedButton(
                        onClick = {
                            startingNew = !startingNew
                            if (startingNew) {
                                name = ""
                                amount = ""
                                periodType = BudgetPeriodType.MONTHLY
                                startDateMillis = null
                                endDateMillis = null
                            } else {
                                name = budget.name
                                amount = formatAmount(budget.amountLimit)
                                periodType = budget.periodType
                                startDateMillis = budget.startEpochDay?.let { it * MILLIS_PER_DAY }
                                endDateMillis = budget.endEpochDay?.let { it * MILLIS_PER_DAY }
                            }
                            statusMessage = null
                        },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(if (startingNew) "EDIT CURRENT BUDGET" else "START NEW BUDGET")
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("NAME") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { new -> if (AMOUNT_PATTERN.matches(new)) amount = new },
                label = { Text("LIMIT (${currentCurrencySymbol()})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 10.dp)
            ) {
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
                    onEndChange = { endDateMillis = it },
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            Button(
                onClick = { submit() },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                Text("SAVE BUDGET")
            }

            if (budget != null) {
                OutlinedButton(
                    onClick = {
                        onClearBudget()
                        statusMessage = "Budget cleared."
                    },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("CLEAR BUDGET")
                }
            }

            statusMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        val pastBudgets = budgetHistory.filter { it.id != budget?.id }
        if (pastBudgets.isNotEmpty()) {
            RetroPanel(modifier = Modifier.fillMaxWidth()) {
                Text(text = "BUDGET HISTORY", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = "Tap a past budget to edit or delete it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
                pastBudgets.forEach { past ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingHistoryBudget = past }
                            .padding(top = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = past.name.uppercase(), style = MaterialTheme.typography.bodyMedium)
                            Text(text = formatCurrency(past.amountLimit), style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            text = if (past.periodType == BudgetPeriodType.MONTHLY) {
                                "Monthly"
                            } else {
                                val start = past.startEpochDay?.let { formatEpochMillis(it * MILLIS_PER_DAY) } ?: "?"
                                val end = past.endEpochDay?.let { formatEpochMillis(it * MILLIS_PER_DAY) } ?: "?"
                                "$start – $end"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }

    editingHistoryBudget?.let { past ->
        EditBudgetSheet(
            budget = past,
            onSave = { updated ->
                onUpdateBudget(updated)
                editingHistoryBudget = null
            },
            onDelete = {
                onDeleteBudget(past.id)
                editingHistoryBudget = null
            },
            onDismiss = { editingHistoryBudget = null }
        )
    }
}
