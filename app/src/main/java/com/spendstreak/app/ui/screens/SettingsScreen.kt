package com.spendstreak.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.spendstreak.app.ads.BannerAdView
import com.spendstreak.app.ui.components.CurrencyPickerDialog
import com.spendstreak.app.ui.components.RetroPanel
import com.spendstreak.app.ui.theme.ThemeOption
import com.spendstreak.app.util.currencyOptionFor

@Composable
fun SettingsScreen(
    onClearData: (onResult: (Boolean) -> Unit) -> Unit,
    onManageAccounts: () -> Unit,
    onManageBudget: () -> Unit,
    onViewReports: () -> Unit,
    onManageRecurring: () -> Unit,
    remindersEnabled: Boolean,
    onToggleReminders: (Boolean) -> Unit,
    unlockedThemes: List<ThemeOption>,
    selectedTheme: ThemeOption,
    onSelectTheme: (ThemeOption) -> Unit,
    currencyCode: String,
    onSelectCurrency: (String) -> Unit,
    onExportData: () -> Unit,
    onImportData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dataMessage by remember { mutableStateOf<String?>(null) }
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var showClearDataConfirm by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf(false) }

    if (showCurrencyPicker) {
        CurrencyPickerDialog(
            currentCode = currencyCode,
            onSelect = { code ->
                onSelectCurrency(code)
                showCurrencyPicker = false
            },
            onDismiss = { showCurrencyPicker = false }
        )
    }

    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            title = { Text("Import CSV?") },
            text = {
                Text(
                    "This will replace all your current expenses, income, and transfers " +
                        "with what's in the file you pick. Accounts, categories, budgets, " +
                        "and recurring rules are kept. This can't be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showImportConfirm = false
                    onImportData()
                }) {
                    Text("IMPORT", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false }) { Text("CANCEL") }
            }
        )
    }

    if (showClearDataConfirm) {
        AlertDialog(
            onDismissRequest = { showClearDataConfirm = false },
            title = { Text("Clear all data?") },
            text = {
                Text(
                    "This permanently deletes all expenses, income, transfers, budgets, " +
                        "and recurring rules. Accounts are kept. This can't be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showClearDataConfirm = false
                    onClearData { success ->
                        dataMessage = if (success) {
                            "All data cleared (accounts kept)."
                        } else {
                            "Something went wrong — please try again."
                        }
                    }
                }) {
                    Text("CLEAR", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataConfirm = false }) { Text("CANCEL") }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "SETTINGS", style = MaterialTheme.typography.headlineMedium)

        RetroPanel(modifier = Modifier.fillMaxWidth()) {
            Text(text = "PREFERENCES", style = MaterialTheme.typography.labelLarge)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val currentOption = currencyOptionFor(currencyCode)
                Text(text = "Currency", style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${currentOption.code} (${currentOption.symbol})",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    OutlinedButton(
                        onClick = { showCurrencyPicker = true },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text("CHANGE")
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(text = "Recurring Reminders", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Get notified when a recurring bill or paycheck is due.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Switch(checked = remindersEnabled, onCheckedChange = onToggleReminders)
            }
        }

        RetroPanel(modifier = Modifier.fillMaxWidth()) {
            Text(text = "APPEARANCE", style = MaterialTheme.typography.labelLarge)
            Text(
                text = "Unlocked automatically as you level up — a cosmetic reward, never a paywall.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )
            ThemeOption.entries.forEach { theme ->
                val isUnlocked = theme in unlockedThemes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = theme.displayName.uppercase(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUnlocked) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        }
                    )
                    when {
                        !isUnlocked -> Text(
                            text = "Unlocks at Level ${theme.unlockLevel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        theme == selectedTheme -> Text(
                            text = "SELECTED",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        else -> OutlinedButton(
                            onClick = { onSelectTheme(theme) },
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text("SELECT")
                        }
                    }
                }
            }
        }

        RetroPanel(modifier = Modifier.fillMaxWidth()) {
            Text(text = "MANAGE", style = MaterialTheme.typography.labelLarge)
            OutlinedButton(
                onClick = onManageAccounts,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Text("MANAGE ACCOUNTS")
            }
            OutlinedButton(
                onClick = onManageBudget,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("SET BUDGET")
            }
            OutlinedButton(
                onClick = onViewReports,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("VIEW REPORTS")
            }
            OutlinedButton(
                onClick = onManageRecurring,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("MANAGE RECURRING")
            }
        }

        RetroPanel(modifier = Modifier.fillMaxWidth()) {
            Text(text = "DATA", style = MaterialTheme.typography.labelLarge)
            OutlinedButton(
                onClick = onExportData,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Text("EXPORT DATA (CSV)")
            }
            OutlinedButton(
                onClick = { showImportConfirm = true },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("IMPORT DATA (CSV)")
            }
            OutlinedButton(
                onClick = { showClearDataConfirm = true },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("CLEAR ALL DATA")
            }
            dataMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        RetroPanel(modifier = Modifier.fillMaxWidth()) {
            Text(text = "ABOUT & CREDITS", style = MaterialTheme.typography.labelLarge)
            Text(
                text = "SpendStreak v1.0",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text = "A gamified expense tracker built to make logging habits stick — level up by showing up, not by spending less.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "A Besi Works Production",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "Designed and developed by Besi",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                text = "Copyright © 2026 Besi Works",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            val context = LocalContext.current
            OutlinedButton(
                onClick = {
                    val uri = Uri.parse("market://details?id=${context.packageName}")
                    try {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, uri).apply { setPackage("com.android.vending") }
                        )
                    } catch (_: ActivityNotFoundException) {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                            )
                        )
                    }
                },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Text("RATE SPENDSTREAK")
            }
        }
    }
    BannerAdView(modifier = Modifier.fillMaxWidth())
    }
}
