package com.spendstreak.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spendstreak.app.util.CurrencyOption
import com.spendstreak.app.util.allWorldCurrencies

@Composable
fun CurrencyPickerDialog(
    currentCode: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val allCurrencies = remember { allWorldCurrencies() }
    val filtered = remember(query, allCurrencies) {
        if (query.isBlank()) {
            allCurrencies
        } else {
            allCurrencies.filter {
                it.code.contains(query, ignoreCase = true) || it.displayName.contains(query, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("CLOSE") }
        },
        title = { Text("SELECT CURRENCY") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("SEARCH") },
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .padding(top = 8.dp)
                ) {
                    items(filtered, key = { it.code }) { option ->
                        CurrencyRow(
                            option = option,
                            selected = option.code == currentCode,
                            onClick = { onSelect(option.code) }
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun CurrencyRow(option: CurrencyOption, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)) {
            Text(text = "${option.code} (${option.symbol})", style = MaterialTheme.typography.bodyMedium)
            Text(text = option.displayName, style = MaterialTheme.typography.bodySmall)
        }
    }
}
