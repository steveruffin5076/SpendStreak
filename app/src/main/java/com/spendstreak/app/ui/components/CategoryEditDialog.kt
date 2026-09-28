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
import androidx.compose.material3.CircularProgressIndicator
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

private val EMOJI_CHOICES = listOf(
    "🍔", "🚗", "🛍", "🧾", "🎬", "💰", "💼", "🎁", "🏠", "💊",
    "📚", "✈️", "☕", "🎮", "🐾", "🎓", "🛠️", "💡", "📱", "🎵",
    "🏋️", "🍺", "👶", "❓"
)

@Composable
fun CategoryEditDialog(
    existing: Category?,
    onSave: (name: String, emoji: String, onComplete: () -> Unit) -> Unit,
    onDelete: ((onResult: (Boolean) -> Unit) -> Unit)?,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var selectedEmoji by remember { mutableStateOf(existing?.emoji ?: EMOJI_CHOICES.first()) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    val isBusy = isSaving || isDeleting

    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteConfirm = false },
            title = { Text("Delete this category?") },
            text = { Text("This can't be undone. Categories with transactions can't be deleted.") },
            confirmButton = {
                TextButton(
                    enabled = !isDeleting,
                    onClick = {
                        isDeleting = true
                        onDelete { success ->
                            isDeleting = false
                            if (success) {
                                showDeleteConfirm = false
                                onDismiss()
                            } else {
                                showDeleteConfirm = false
                                statusMessage = "Can't delete — it has transactions."
                            }
                        }
                    }
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("DELETE", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(enabled = !isDeleting, onClick = { showDeleteConfirm = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        title = { Text(if (existing == null) "ADD CATEGORY" else "EDIT CATEGORY") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; statusMessage = null },
                    label = { Text("NAME") },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "ICON",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 12.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    EMOJI_CHOICES.forEach { emoji ->
                        val selected = emoji == selectedEmoji
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(
                                    if (selected) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                                .clickable(enabled = !isBusy) { selectedEmoji = emoji },
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
            TextButton(
                enabled = !isBusy,
                onClick = {
                    if (name.isBlank()) {
                        statusMessage = "Enter a name."
                    } else {
                        isSaving = true
                        onSave(name.trim(), selectedEmoji) {
                            isSaving = false
                            onDismiss()
                        }
                    }
                }
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("SAVE")
                }
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(enabled = !isBusy, onClick = { showDeleteConfirm = true }) {
                        Text(text = "DELETE", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(enabled = !isBusy, onClick = onDismiss) {
                    Text("CANCEL")
                }
            }
        }
    )
}
