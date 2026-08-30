package com.spendstreak.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spendstreak.app.data.Account

// Same "compact trigger opens a sheet" pattern as CategoryPickerSheet, for the same
// reason — a fixed-width inline chip/tab row cut off long account names ("PBB JOINT
// NAME...", "HL CAR SAVING(3...") that are common once someone adds their real bank
// accounts. Full-width rows here have room for the whole name, disabled param
// excluded on purpose — enabled/disabled a la TransferAccountsSection is
// this sheet's caller's concern, not its own.
//
// Account add/delete already has a home (Settings -> Manage Accounts), so unlike
// CategoryPickerSheet this stays selection-only — no inline edit mode to duplicate it.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountPickerSheet(
    title: String,
    accounts: List<Account>,
    selectedAccountId: Long?,
    disabledAccountId: Long? = null,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(accounts, key = { it.id }) { account ->
                val enabled = account.id != disabledAccountId
                AccountRow(
                    account = account,
                    selected = account.id == selectedAccountId,
                    enabled = enabled,
                    onClick = {
                        if (enabled) {
                            onSelect(account.id)
                            onDismiss()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AccountRow(
    account: Account,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    RetroPanel(
        borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Column {
            Text(
                text = account.name.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
            )
            Text(
                text = account.type,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
