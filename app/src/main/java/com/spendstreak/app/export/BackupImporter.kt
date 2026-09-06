package com.spendstreak.app.export

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
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
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

sealed interface BackupImportResult {
    data class Success(
        val accountCount: Int,
        val categoryCount: Int,
        val expenseCount: Int,
        val incomeCount: Int,
        val transferCount: Int
    ) : BackupImportResult
    data class Failure(val reason: String) : BackupImportResult
}

private fun JSONObject.optLongOrNull(key: String): Long? = if (isNull(key)) null else getLong(key)

private fun accountFromJson(json: JSONObject) = Account(
    id = json.getLong("id"),
    name = json.getString("name"),
    type = json.getString("type"),
    openingBalance = json.optDouble("openingBalance", 0.0)
)

private fun categoryFromJson(json: JSONObject) = Category(
    id = json.getLong("id"),
    name = json.getString("name"),
    kind = json.getString("kind"),
    emoji = json.getString("emoji")
)

private fun expenseFromJson(json: JSONObject) = Expense(
    id = json.getLong("id"),
    amount = json.getDouble("amount"),
    categoryId = json.getLong("categoryId"),
    note = json.getString("note"),
    accountId = json.getLong("accountId"),
    timestampMillis = json.getLong("timestampMillis"),
    excludedFromBudget = json.optBoolean("excludedFromBudget", false)
)

private fun incomeFromJson(json: JSONObject) = Income(
    id = json.getLong("id"),
    amount = json.getDouble("amount"),
    categoryId = json.getLong("categoryId"),
    note = json.getString("note"),
    accountId = json.getLong("accountId"),
    timestampMillis = json.getLong("timestampMillis"),
    excludedFromBudget = json.optBoolean("excludedFromBudget", false)
)

private fun transferFromJson(json: JSONObject) = Transfer(
    id = json.getLong("id"),
    fromAccountId = json.getLong("fromAccountId"),
    toAccountId = json.getLong("toAccountId"),
    amount = json.getDouble("amount"),
    note = json.getString("note"),
    timestampMillis = json.getLong("timestampMillis")
)

private fun budgetFromJson(json: JSONObject) = Budget(
    id = json.getLong("id"),
    name = json.getString("name"),
    amountLimit = json.getDouble("amountLimit"),
    periodType = json.getString("periodType"),
    startEpochDay = json.optLongOrNull("startEpochDay"),
    endEpochDay = json.optLongOrNull("endEpochDay"),
    isActive = json.getBoolean("isActive")
)

private fun categoryBudgetFromJson(json: JSONObject) = CategoryBudget(
    categoryId = json.getLong("categoryId"),
    monthlyLimit = json.getDouble("monthlyLimit")
)

private fun recurringFromJson(json: JSONObject) = RecurringTransaction(
    id = json.getLong("id"),
    type = json.getString("type"),
    amount = json.getDouble("amount"),
    categoryId = json.getLong("categoryId"),
    accountId = json.getLong("accountId"),
    note = json.getString("note"),
    intervalType = json.getString("intervalType"),
    nextDueEpochDay = json.getLong("nextDueEpochDay"),
    active = json.optBoolean("active", true)
)

private fun userProgressFromJson(json: JSONObject) = UserProgress(
    level = json.optInt("level", 1),
    xp = json.optInt("xp", 0),
    currentStreak = json.optInt("currentStreak", 0),
    longestStreak = json.optInt("longestStreak", 0),
    lastLoggedEpochDay = json.optLongOrNull("lastLoggedEpochDay"),
    hasSetBudget = json.optBoolean("hasSetBudget", false),
    logsToday = json.optInt("logsToday", 0)
)

private fun <T> JSONObject.parseArray(key: String, fromJson: (JSONObject) -> T): List<T> {
    val array = optJSONArray(key) ?: JSONArray()
    return (0 until array.length()).map { fromJson(array.getJSONObject(it)) }
}

// Replaces every table (unlike importCsvReplacingAll, which deliberately leaves accounts/
// categories/budgets/recurring rules untouched) — a full backup restore reconstructs the
// entire app, so it wholesale replaces accounts and categories too rather than merging by
// name. Original row ids are preserved (every table is cleared first, so there's no
// conflict), which is what keeps every foreign-key-style reference — expense.accountId,
// categoryBudget.categoryId, etc. — correct without needing any id-remapping step.
suspend fun importBackupReplacingAll(context: Context, database: SpendStreakDatabase, uri: Uri): BackupImportResult =
    withContext(Dispatchers.IO) {
        val text = try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: return@withContext BackupImportResult.Failure("Couldn't open the selected file.")
        } catch (e: Exception) {
            return@withContext BackupImportResult.Failure("Couldn't read the selected file.")
        }

        val json = try {
            JSONObject(text)
        } catch (e: JSONException) {
            return@withContext BackupImportResult.Failure("This doesn't look like a SpendStreak backup file.")
        }

        val schemaVersion = json.optInt("schemaVersion", -1)
        if (schemaVersion != BACKUP_SCHEMA_VERSION) {
            return@withContext BackupImportResult.Failure(
                "This backup was made by a different app version and can't be restored here."
            )
        }

        // Fully parsed and validated before touching the database — same discipline as
        // importCsvReplacingAll: an import either applies in full, or (on any failure below)
        // leaves existing data completely untouched.
        val accounts: List<Account>
        val categories: List<Category>
        val expenses: List<Expense>
        val income: List<Income>
        val transfers: List<Transfer>
        val budgets: List<Budget>
        val categoryBudgets: List<CategoryBudget>
        val recurring: List<RecurringTransaction>
        val userProgress: UserProgress?
        try {
            accounts = json.parseArray("accounts", ::accountFromJson)
            categories = json.parseArray("categories", ::categoryFromJson)
            expenses = json.parseArray("expenses", ::expenseFromJson)
            income = json.parseArray("income", ::incomeFromJson)
            transfers = json.parseArray("transfers", ::transferFromJson)
            budgets = json.parseArray("budgets", ::budgetFromJson)
            categoryBudgets = json.parseArray("categoryBudgets", ::categoryBudgetFromJson)
            recurring = json.parseArray("recurringTransactions", ::recurringFromJson)
            userProgress = if (json.isNull("userProgress")) {
                null
            } else {
                userProgressFromJson(json.getJSONObject("userProgress"))
            }
        } catch (e: JSONException) {
            return@withContext BackupImportResult.Failure("The backup file is corrupted or incomplete.")
        }

        database.withTransaction {
            database.expenseDao().deleteAll()
            database.incomeDao().deleteAll()
            database.transferDao().deleteAll()
            database.budgetDao().deleteAll()
            database.categoryBudgetDao().deleteAll()
            database.recurringTransactionDao().deleteAll()
            database.accountDao().deleteAll()
            database.categoryDao().deleteAll()
            database.userProgressDao().deleteAll()

            accounts.forEach { database.accountDao().insert(it) }
            categories.forEach { database.categoryDao().insert(it) }
            expenses.forEach { database.expenseDao().insert(it) }
            income.forEach { database.incomeDao().insert(it) }
            transfers.forEach { database.transferDao().insert(it) }
            budgets.forEach { database.budgetDao().insert(it) }
            categoryBudgets.forEach { database.categoryBudgetDao().upsert(it) }
            recurring.forEach { database.recurringTransactionDao().insert(it) }
            userProgress?.let { database.userProgressDao().upsert(it) }
        }

        BackupImportResult.Success(
            accountCount = accounts.size,
            categoryCount = categories.size,
            expenseCount = expenses.size,
            incomeCount = income.size,
            transferCount = transfers.size
        )
    }
