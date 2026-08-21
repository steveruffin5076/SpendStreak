package com.spendstreak.app.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.spendstreak.app.viewmodel.HistoryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Plain manual CSV writer — the data here is a handful of flat columns with no nested
// objects, so kotlinx-serialization or any other library would be pure overhead.
private val ROW_DATE_FORMATTER = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
private val FILE_TIMESTAMP_FORMATTER = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)

private fun csvField(value: String): String =
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
        "\"${value.replace("\"", "\"\"")}\""
    } else {
        value
    }

private fun entryToCsvRow(entry: HistoryEntry): String {
    val type: String
    val amount: Double
    val category: String
    val account: String
    val note: String
    when (entry) {
        is HistoryEntry.ExpenseEntry -> {
            type = "Expense"
            amount = entry.expense.amount
            category = entry.categoryName
            account = entry.accountName
            note = entry.expense.note
        }
        is HistoryEntry.IncomeEntry -> {
            type = "Income"
            amount = entry.income.amount
            category = entry.categoryName
            account = entry.accountName
            note = entry.income.note
        }
        is HistoryEntry.TransferEntry -> {
            type = "Transfer"
            amount = entry.transfer.amount
            // Neither a category nor an income source — a transfer moves money between
            // two of your own accounts, so both ends go in this one column instead.
            category = "${entry.fromAccountName} -> ${entry.toAccountName}"
            account = ""
            note = entry.transfer.note
        }
    }
    return listOf(
        csvField(type),
        csvField(ROW_DATE_FORMATTER.format(Date(entry.timestampMillis))),
        csvField(String.format(Locale.US, "%.2f", amount)),
        csvField(category),
        csvField(account),
        csvField(note)
    ).joinToString(",")
}

private fun buildCsv(entries: List<HistoryEntry>): String {
    val header = listOf("Type", "Date", "Amount", "Category", "Account", "Note").joinToString(",")
    val rows = entries.sortedBy { it.timestampMillis }.map { entryToCsvRow(it) }
    return (listOf(header) + rows).joinToString("\n")
}

// Writes to this app's own external-files "exports" folder (no runtime permission needed
// on any API level, unlike a public Downloads write pre-API-29) then hands the file to the
// system share sheet via FileProvider — the user picks where it actually lands (Downloads
// via a file manager, email, Drive, etc.), which sidesteps needing separate code paths for
// API 26-28 vs. the API-29+-only MediaStore.Downloads collection.
suspend fun exportAndShareCsv(context: Context, entries: List<HistoryEntry>) {
    val file = withContext(Dispatchers.IO) {
        val exportsDir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        val timestamp = FILE_TIMESTAMP_FORMATTER.format(Date())
        File(exportsDir, "spendstreak-export-$timestamp.csv").apply {
            writeText(buildCsv(entries))
        }
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Export SpendStreak data").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}
