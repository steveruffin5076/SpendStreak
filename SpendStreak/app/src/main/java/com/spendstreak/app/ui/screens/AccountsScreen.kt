package com.spendstreak.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spendstreak.app.data.Account
import com.spendstreak.app.ui.components.RetroPanel
import com.spendstreak.app.util.formatCurrency

private val ACCOUNT_TYPES = listOf("Bank", "Cash", "Credit Card", "Other")

@Composable
fun AccountsScreen(
    accounts: List<Account>,
    accountBalances: Map<Long, Double>,
    onAddAccount: (name: String, type: String) -> Unit,
    onDeleteAccount: (accountId: Long, onResult: (Boolean) -> Unit) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ACCOUNT_TYPES.first()) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 10.dp)
            ) {
                ACCOUNT_TYPES.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = {
                            Text(text = type.uppercase(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    )
                }
            }
            Button(
                onClick = {
                    if (name.isBlank()) {
                        statusMessage = "Enter a name for the account."
                    } else {
                        onAddAccount(name.trim(), selectedType)
                        name = ""
                        statusMessage = "Account added."
                    }
                },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                Text("ADD ACCOUNT")
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
                    onDelete = {
                        onDeleteAccount(account.id) { deleted ->
                            statusMessage = if (deleted) {
                                "Deleted ${account.name}."
                            } else {
                                "Can't delete ${account.name} — it has transactions."
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AccountRow(account: Account, balance: Double, onDelete: () -> Unit) {
    val balanceColor = if (balance >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
    RetroPanel(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Weighted so a long (free-typed) account name is bounded and ellipsized
            // instead of pushing the balance/delete button off the edge of the row.
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
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatCurrency(balance),
                    style = MaterialTheme.typography.titleMedium,
                    color = balanceColor
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete ${account.name}")
                }
            }
        }
    }
}
