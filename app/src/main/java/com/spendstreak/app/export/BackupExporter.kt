package com.spendstreak.app.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.spendstreak.app.data.Account
import com.spendstreak.app.data.Budget
import com.spendstreak.app.data.Category
import com.spendstreak.app.data.CategoryBudget
import com.spendstreak.app.data.Expense
import com.spendstreak.app.data.Income
import com.spendstreak.app.data.RecurringTransaction
import com.spendstreak.app.data.SpendStreakDatabase
import com.spendstreak.app.data.Transfer
import com.spendstreak.app.data.UserProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Full-fidelity backup of every table — unlike CsvExporter, which only round-trips
// expenses/income/transfers for the "open in a spreadsheet" use case, restoring this file
// reconstructs accounts, categories, budgets (+ history), category budgets, recurring
// rules, and streak/level progress too. Plain org.json (built into Android) rather than a
// serialization library, same "no library for flat, one-shot data" reasoning as
// CsvExporter's hand-rolled writer. Bump BACKUP_SCHEMA_VERSION (and handle old versions in
// BackupImporter) if this shape ever needs to change incompatibly.
internal const val BACKUP_SCHEMA_VERSION = 1
private val FILE_TIMESTAMP_FORMATTER = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)

private fun Account.toJson() = JSONObject().apply {
    put("id", id); put("name", name); put("type", type); put("openingBalance", openingBalance)
}

private fun Category.toJson() = JSONObject().apply {
    put("id", id); put("name", name); put("kind", kind); put("emoji", emoji)
}

private fun Expense.toJson() = JSONObject().apply {
    put("id", id); put("amount", amount); put("categoryId", categoryId); put("note", note)
    put("accountId", accountId); put("timestampMillis", timestampMillis)
    put("excludedFromBudget", excludedFromBudget)
}

private fun Income.toJson() = JSONObject().apply {
    put("id", id); put("amount", amount); put("categoryId", categoryId); put("note", note)
    put("accountId", accountId); put("timestampMillis", timestampMillis)
    put("excludedFromBudget", excludedFromBudget)
}

private fun Transfer.toJson() = JSONObject().apply {
    put("id", id); put("fromAccountId", fromAccountId); put("toAccountId", toAccountId)
    put("amount", amount); put("note", note); put("timestampMillis", timestampMillis)
}

private fun Budget.toJson() = JSONObject().apply {
    put("id", id); put("name", name); put("amountLimit", amountLimit); put("periodType", periodType)
    put("startEpochDay", startEpochDay ?: JSONObject.NULL)
    put("endEpochDay", endEpochDay ?: JSONObject.NULL)
    put("isActive", isActive)
}

private fun CategoryBudget.toJson() = JSONObject().apply {
    put("categoryId", categoryId); put("monthlyLimit", monthlyLimit)
}

private fun RecurringTransaction.toJson() = JSONObject().apply {
    put("id", id); put("type", type); put("amount", amount); put("categoryId", categoryId)
    put("accountId", accountId); put("note", note); put("intervalType", intervalType)
    put("nextDueEpochDay", nextDueEpochDay); put("active", active)
}

private fun UserProgress.toJson() = JSONObject().apply {
    put("level", level); put("xp", xp); put("currentStreak", currentStreak)
    put("longestStreak", longestStreak)
    put("lastLoggedEpochDay", lastLoggedEpochDay ?: JSONObject.NULL)
    put("hasSetBudget", hasSetBudget); put("logsToday", logsToday)
}

private fun <T> jsonArrayOf(items: List<T>, toJson: T.() -> JSONObject): JSONArray =
    JSONArray().apply { items.forEach { put(it.toJson()) } }

private suspend fun buildBackupJson(database: SpendStreakDatabase): JSONObject = JSONObject().apply {
    put("schemaVersion", BACKUP_SCHEMA_VERSION)
    put("accounts", jsonArrayOf(database.accountDao().getAll().first(), Account::toJson))
    put("categories", jsonArrayOf(database.categoryDao().getAll().first(), Category::toJson))
    put("expenses", jsonArrayOf(database.expenseDao().getAll().first(), Expense::toJson))
    put("income", jsonArrayOf(database.incomeDao().getAll().first(), Income::toJson))
    put("transfers", jsonArrayOf(database.transferDao().getAll().first(), Transfer::toJson))
    put("budgets", jsonArrayOf(database.budgetDao().observeAll().first(), Budget::toJson))
    put("categoryBudgets", jsonArrayOf(database.categoryBudgetDao().observeAll().first(), CategoryBudget::toJson))
    put(
        "recurringTransactions",
        jsonArrayOf(database.recurringTransactionDao().getAll().first(), RecurringTransaction::toJson)
    )
    put("userProgress", database.userProgressDao().get()?.toJson() ?: JSONObject.NULL)
}

// Same "write to this app's own external-files folder, then hand off to the system share
// sheet via FileProvider" approach as CsvExporter.exportAndShareCsv, for the same reasons.
suspend fun exportAndShareBackup(context: Context, database: SpendStreakDatabase) {
    val file = withContext(Dispatchers.IO) {
        val json = buildBackupJson(database)
        val exportsDir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        val timestamp = FILE_TIMESTAMP_FORMATTER.format(Date())
        File(exportsDir, "spendstreak-backup-$timestamp.json").apply {
            writeText(json.toString(2))
        }
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Export SpendStreak backup").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}
