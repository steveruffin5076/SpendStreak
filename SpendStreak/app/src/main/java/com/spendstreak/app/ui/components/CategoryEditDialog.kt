package com.spendstreak.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.spendstreak.app.data.Category

// A small curated set, not a full emoji keyboard — no new dependency (system emoji font),
// simple tap-to-pick, same approach already used for the app's built-in category icons.
private val EMOJI_CHOICES = listOf(
    "🍔", "🚗", "🛍", "🧾", "🎬", "💰", "💼", "🎁", "🏠", "💊",
    "📚", "✈️", "☕", "🎮", "🐾", "🎓", "🛠️", "💡", "📱", "🎵",
    "🏋️", "🍺", "👶", "❓"
)

// existing == null means "create"; non-null means "rename" (and offers delete).
// onDelete is null when creating (nothing to delete yet); when non-null it performs the
// actual delete-blocked-if-in-use call and reports success/failure back via onResult, same
// shape as AccountsScreen's onDeleteAccount — a failed delete keeps the dialog open with a
// status message instead of silently doing nothing.
@Composable
fun CategoryEditDialog(
    existing: Category?,
    onSave: (name: String, emoji: String) -> Unit,
    onDelete: ((onResult: (Boolean) -> Unit) -> Unit)?,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var selectedEmoji by remember { mutableStateOf(existing?.emoji ?: EMOJI_CHOICES.first()) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "ADD CATEGORY" else "EDIT CATEGORY") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; statusMessage = null },
                    label = { Text("NAME") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "ICON",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 12.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    EMOJI_CHOICES.forEach { emoji ->
                        val selected = emoji == selectedEmoji
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(
                                    if (selected) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                                .clickable { selectedEmoji = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                statusMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) {
                    statusMessage = "Enter a name."
                } else {
                    onSave(name.trim(), selectedEmoji)
                }
            }) {
                Text("SAVE")
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = {
                        onDelete { success ->
                            if (!success) {
                                statusMessage = "Can't delete — it has transactions."
                            }
                        }
                    }) {
                        Text(text = "DELETE", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("CANCEL")
                }
            }
        }
    )
}
