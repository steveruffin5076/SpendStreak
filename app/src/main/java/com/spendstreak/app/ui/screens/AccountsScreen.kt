package com.spendstreak.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spendstreak.app.data.Account
import com.spendstreak.app.ui.components.RetroPanel
import com.spendstreak.app.util.FormValidation
import com.spendstreak.app.util.formatCurrency
import kotlinx.coroutines.delay

private val ACCOUNT_TYPES = listOf("Bank", "Cash", Account.TYPE_CREDIT_CARD, "Other")
private val BALANCE_PATTERN = Regex("^\\d{0,9}(\\.\\d{0,2})?$")

@Composable
fun AccountsScreen(
    accounts: List<Account>,
    accountBalances: Map<Long, Double>,
    onAddAccount: (name: String, type: String, openingBalance: Double, onComplete: () -> Unit) -> Unit,
    onDeleteAccount: (accountId: Long, onResult: (Boolean) -> Unit) -> Unit,
    onViewAccount: (accountId: Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ACCOUNT_TYPES.first()) }
    var openingBalanceText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isAddingAccount by remember { mutableStateOf(false) }
    var accountPendingDelete by remember { mutableStateOf<Account?>(null) }
    var deletingAccountId by remember { mutableStateOf<Long?>(null) }
    val isCreditCard = selectedType == Account.TYPE_CREDIT_CARD

    accountPendingDelete?.let { account ->
        AlertDialog(
            onDismissRequest = {
                if (deletingAccountId == null) accountPendingDelete = null
            },
            title = { Text("Delete ${account.name}?") },
            text = { Text("This can't be undone. Accounts with transactions can't be deleted.") },
            confirmButton = {
                TextButton(
                    enabled = deletingAccountId == null,
                    onClick = {
                        deletingAccountId = account.id
                        onDeleteAccount(account.id) { deleted ->
                            deletingAccountId = null
                            accountPendingDelete = null
                            statusMessage = if (deleted) {
                                "Deleted ${account.name}."
                            } else {
                                "Can't delete ${account.name} — it has transactions."
                            }
                        }
                    }
                ) {
                    if (deletingAccountId == account.id) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("DELETE", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = deletingAccountId == null,
                    onClick = { accountPendingDelete = null }
                ) {
                    Text("CANCEL")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(text = "ACCOUNTS", style = MaterialTheme.typography.headlineMedium)
        }

        RetroPanel(modifier = Modifier.fillMaxWidth()) {
            Text(text = "ADD ACCOUNT", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("NAME") },
                enabled = !isAddingAccount,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 10.dp)
            ) {
                ACCOUNT_TYPES.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { if (!isAddingAccount) selectedType = type },
                        label = {
                            Text(text = type.uppercase(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    )
                }
            }
            OutlinedTextField(
                value = openingBalanceText,
                onValueChange = { new -> if (BALANCE_PATTERN.matches(new)) openingBalanceText = new },
                label = { Text(if (isCreditCard) "CREDIT LIMIT" else "OPENING BALANCE (OPTIONAL)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                enabled = !isAddingAccount,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            )
            if (isCreditCard) {
                Text(
                    text = "Your spending limit — not counted as money in your Dashboard balance.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Button(
                onClick = {
                    val nameError = FormValidation.validateNonBlankName(name, "Enter a name for the account.")
                    if (nameError != null) {
                        statusMessage = nameError
                    } else if (!isAddingAccount) {
                        isAddingAccount = true
                        onAddAccount(name.trim(), selectedType, openingBalanceText.toDoubleOrNull() ?: 0.0) {
                            isAddingAccount = false
                            name = ""
                            openingBalanceText = ""
                            statusMessage = "Account added."
                        }
                    }
                },
                enabled = !isAddingAccount,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                if (isAddingAccount) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("ADD ACCOUNT")
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

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(accounts, key = { it.id }) { account ->
                AccountRow(
                    account = account,
                    balance = accountBalances[account.id] ?: 0.0,
                    onClick = { onViewAccount(account.id) },
                    onDeleteClick = { accountPendingDelete = account }
                )
            }
        }
    }
}

@Composable
fun EditOpeningBalancePanel(account: Account, onSave: (Double) -> Unit, modifier: Modifier = Modifier) {
    var balanceText by remember(account.id) { mutableStateOf(if (account.openingBalance == 0.0) "" else account.openingBalance.toString()) }
    var savedMessage by remember(account.id) { mutableStateOf<String?>(null) }
    val isCreditCard = account.type == Account.TYPE_CREDIT_CARD

    LaunchedEffect(savedMessage) {
        if (savedMessage != null) {
            delay(2000)
            savedMessage = null
        }
    }

    RetroPanel(modifier = modifier.fillMaxWidth()) {
        Text(text = if (isCreditCard) "CREDIT LIMIT" else "OPENING BALANCE", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = balanceText,
            onValueChange = { new ->
                if (BALANCE_PATTERN.matches(new)) {
                    balanceText = new
                    savedMessage = null
                }
            },
            label = { Text(if (isCreditCard) "CREDIT LIMIT" else "OPENING BALANCE") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        )
        Button(
            onClick = {
                onSave(balanceText.toDoubleOrNull() ?: 0.0)
                savedMessage = "Saved!"
            },
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        ) {
            Text("SAVE")
        }
        savedMessage?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun AccountRow(
    account: Account,
    balance: Double,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val balanceColor = if (balance >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
    val isCreditCard = account.type == Account.TYPE_CREDIT_CARD
    RetroPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = account.name.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = account.type,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isCreditCard && account.openingBalance > 0) {
                    Text(
                        text = "Limit: ${formatCurrency(account.openingBalance)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatCurrency(balance),
                    style = MaterialTheme.typography.titleMedium,
                    color = balanceColor
                )
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete ${account.name}")
                }
            }
        }
    }
}
