package com.spendstreak.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spendstreak.app.data.Category

// A compact "current selection" trigger (see AddTransactionScreen's CategoryOrSourceSection)
// opens this sheet instead of always showing every option inline — a 4-column inline grid
// truncated long names ("TRANSPORT" -> "TR…"); a 2-column grid here has room for full,
// possibly-2-line labels. Safe to use LazyVerticalGrid here (unlike inline in the form):
// the sheet's content is its own top-level container, not nested inside a
// verticalScroll(Column), so there's no infinite-height-constraint crash risk.
//
// Category add/edit/delete lives here too, toggled via the EDIT button, rather than a
// separate "Manage Categories" screen — one surface to build/maintain, and it's more
// discoverable (you edit categories right where you pick them).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerSheet(
    title: String,
    categories: List<Category>,
    selectedCategoryId: Long,
    onSelect: (Long) -> Unit,
    onAddCategory: (name: String, emoji: String) -> Unit,
    onRenameCategory: (category: Category, name: String, emoji: String) -> Unit,
    onDeleteCategory: (category: Category, onResult: (Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var editMode by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = { editMode = !editMode }) {
                Text(if (editMode) "DONE" else "EDIT")
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories, key = { it.id }) { category ->
                CategoryTile(
                    category = category,
                    selected = category.id == selectedCategoryId,
                    editMode = editMode,
                    onClick = {
                        if (editMode) {
                            editingCategory = category
                        } else {
                            onSelect(category.id)
                            onDismiss()
                        }
                    }
                )
            }
            item {
                AddCategoryTile(onClick = { showAddDialog = true })
            }
        }
    }

    if (showAddDialog) {
        CategoryEditDialog(
            existing = null,
            onSave = { name, emoji ->
                onAddCategory(name, emoji)
                showAddDialog = false
            },
            onDelete = null,
            onDismiss = { showAddDialog = false }
        )
    }

    editingCategory?.let { category ->
        CategoryEditDialog(
            existing = category,
            onSave = { name, emoji ->
                onRenameCategory(category, name, emoji)
                editingCategory = null
            },
            onDelete = { onResult ->
                onDeleteCategory(category) { success ->
                    if (success) editingCategory = null
                    onResult(success)
                }
            },
            onDismiss = { editingCategory = null }
        )
    }
}

@Composable
private fun CategoryTile(
    category: Category,
    selected: Boolean,
    editMode: Boolean,
    onClick: () -> Unit
) {
    RetroPanel(
        borderColor = if (selected && !editMode) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outline
        },
        containerColor = if (selected && !editMode) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.2f)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = category.emoji,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = category.name.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
        )
        if (editMode) {
            Text(
                text = "TAP TO EDIT",
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun AddCategoryTile(onClick: () -> Unit) {
    RetroPanel(
        borderColor = MaterialTheme.colorScheme.outline,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.2f)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = "+",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "ADD",
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
        )
    }
}
