package com.spendstreak.app.export

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.spendstreak.app.data.Account
import com.spendstreak.app.data.Category
import com.spendstreak.app.data.CategoryKind
import com.spendstreak.app.data.Expense
import com.spendstreak.app.data.Income
import com.spendstreak.app.data.SpendStreakDatabase
import com.spendstreak.app.data.Transfer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

sealed interface ImportResult {
    data class Success(val expenseCount: Int, val incomeCount: Int, val transferCount: Int) : ImportResult
    data class Failure(val reason: String) : ImportResult
}

private enum class RowType { EXPENSE, INCOME, TRANSFER }

private data class ParsedRow(
    val type: RowType,
    val timestampMillis: Long,
    val amount: Double,
    // Expense/Income: the category name. Transfer: unused (fromName/toName below instead).
    val categoryName: String,
    // Expense/Income: the account name. Transfer: unused.
    val accountName: String,
    val fromAccountName: String,
    val toAccountName: String,
    val note: String
)

private val EXPECTED_HEADER = listOf("Type", "Date", "Amount", "Category", "Account", "Note")

// Mirrors CsvExporter's ROW_DATE_FORMATTER exactly — this only round-trips SpendStreak's
// own export format, so it must match that writer's format string exactly. isLenient=false
// so e.g. a typo'd "2026-13-40" is rejected instead of silently rolling over to a real date.
private fun newRowDateParser(): SimpleDateFormat =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { isLenient = false }

// Small hand-rolled CSV parser (matching CsvExporter's equally hand-rolled writer) rather
// than pulling in a library — this only ever needs to read back the one flat, quote-escaped
// format that writer produces. Scans character-by-character (not line-by-line) specifically
// so a quoted field containing a literal newline (which the writer does allow — see
// CsvExporter.csvField) doesn't get incorrectly split into two rows.
private fun parseCsvRows(text: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    var field = StringBuilder()
    var row = mutableListOf<String>()
    var inQuotes = false
    var i = 0
    val n = text.length
    var sawAnyContentInRow = false

    fun endField() {
        row.add(field.toString())
        field = StringBuilder()
    }

    fun endRow() {
        endField()
        rows.add(row)
        row = mutableListOf()
        sawAnyContentInRow = false
    }

    while (i < n) {
        val c = text[i]
        if (inQuotes) {
            when {
                c == '"' && i + 1 < n && text[i + 1] == '"' -> {
                    field.append('"')
                    i += 2
                }
                c == '"' -> {
                    inQuotes = false
                    i++
                }
                else -> {
                    field.append(c)
                    i++
                }
            }
        } else {
            when (c) {
                '"' -> {
                    inQuotes = true
                    sawAnyContentInRow = true
                    i++
                }
                ',' -> {
                    endField()
                    sawAnyContentInRow = true
                    i++
                }
                '\r' -> i++
                '\n' -> {
                    endRow()
                    i++
                }
                else -> {
                    field.append(c)
                    sawAnyContentInRow = true
                    i++
                }
            }
        }
    }
    if (sawAnyContentInRow || field.isNotEmpty()) {
        endRow()
    }
    return rows
}

// Parses and fully validates every row before any database access — an import either
// applies in full or leaves existing data completely untouched, never a partial mix.
private fun parseAndValidate(text: String): Result<List<ParsedRow>> {
    val rows = parseCsvRows(text)
    if (rows.isEmpty()) {
        return Result.failure(IllegalArgumentException("The file is empty."))
    }
    if (rows.first() != EXPECTED_HEADER) {
        return Result.failure(
            IllegalArgumentException(
                "This doesn't look like a SpendStreak export — expected the header " +
                    "\"${EXPECTED_HEADER.joinToString(",")}\"."
            )
        )
    }

    val dateParser = newRowDateParser()
    val parsed = mutableListOf<ParsedRow>()

    rows.drop(1).forEachIndexed { index, cols ->
        val lineNumber = index + 2 // 1-based, +1 for the header row already consumed
        if (cols.size != EXPECTED_HEADER.size) {
            return Result.failure(
                IllegalArgumentException(
                    "Row $lineNumber: expected ${EXPECTED_HEADER.size} columns, found ${cols.size}."
                )
            )
        }
        val (typeStr, dateStr, amountStr, categoryStr, accountStr, note) = cols

        val type = when (typeStr) {
            "Expense" -> RowType.EXPENSE
            "Income" -> RowType.INCOME
            "Transfer" -> RowType.TRANSFER
            else -> return Result.failure(
                IllegalArgumentException(
                    "Row $lineNumber: unknown type \"$typeStr\" (expected Expense, Income, or Transfer)."
                )
            )
        }

        val timestampMillis = try {
            dateParser.parse(dateStr)?.time
                ?: return Result.failure(IllegalArgumentException("Row $lineNumber: invalid date \"$dateStr\"."))
        } catch (e: Exception) {
            return Result.failure(
                IllegalArgumentException(
                    "Row $lineNumber: invalid date \"$dateStr\" (expected yyyy-MM-dd HH:mm)."
                )
            )
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount < 0.0) {
            return Result.failure(
                IllegalArgumentException("Row $lineNumber: invalid amount \"$amountStr\".")
            )
        }

        val row = when (type) {
            RowType.TRANSFER -> {
                val separatorIndex = categoryStr.indexOf(" -> ")
                if (separatorIndex < 0) {
                    return Result.failure(
                        IllegalArgumentException(
                            "Row $lineNumber: a transfer's Category column should read " +
                                "\"FromAccount -> ToAccount\", found \"$categoryStr\"."
                        )
                    )
                }
                val fromName = categoryStr.substring(0, separatorIndex).trim()
                val toName = categoryStr.substring(separatorIndex + 4).trim()
                if (fromName.isBlank() || toName.isBlank()) {
                    return Result.failure(
                        IllegalArgumentException("Row $lineNumber: transfer is missing a from/to account name.")
                    )
                }
                ParsedRow(
                    type = type,
                    timestampMillis = timestampMillis,
                    amount = amount,
                    categoryName = "",
                    accountName = "",
                    fromAccountName = fromName,
                    toAccountName = toName,
                    note = note
                )
            }
            RowType.EXPENSE, RowType.INCOME -> {
                if (categoryStr.isBlank()) {
                    return Result.failure(IllegalArgumentException("Row $lineNumber: missing category."))
                }
                if (accountStr.isBlank()) {
                    return Result.failure(IllegalArgumentException("Row $lineNumber: missing account."))
                }
                ParsedRow(
                    type = type,
                    timestampMillis = timestampMillis,
                    amount = amount,
                    categoryName = categoryStr,
                    accountName = accountStr,
                    fromAccountName = "",
                    toAccountName = "",
                    note = note
                )
            }
        }
        parsed.add(row)
    }
    return Result.success(parsed)
}

// Component-destructuring helper for the 6-column row list above.
private operator fun List<String>.component6(): String = this[5]

private const val DEFAULT_ACCOUNT_TYPE = "Other"
private const val DEFAULT_CATEGORY_EMOJI = "❓"

// Replaces all expenses, income, and transfers with what's in the CSV. Deliberately leaves
// accounts, categories, budgets, and recurring rules untouched — none of those are present
// in the exported CSV, so wiping them here would just be irrecoverable data loss with
// nothing to restore them from, not a "replace".
suspend fun importCsvReplacingAll(context: Context, database: SpendStreakDatabase, uri: Uri): ImportResult =
    withContext(Dispatchers.IO) {
        val text = try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: return@withContext ImportResult.Failure("Couldn't open the selected file.")
        } catch (e: Exception) {
            return@withContext ImportResult.Failure("Couldn't read the selected file.")
        }

        val parsedResult = parseAndValidate(text)
        val parsedRows = parsedResult.getOrElse { error ->
            return@withContext ImportResult.Failure(error.message ?: "The file isn't a valid SpendStreak export.")
        }

        val existingAccounts = database.accountDao().getAll().first()
        val existingCategories = database.categoryDao().getAll().first()

        var expenseCount = 0
        var incomeCount = 0
        var transferCount = 0

        database.withTransaction {
            database.expenseDao().deleteAll()
            database.incomeDao().deleteAll()
            database.transferDao().deleteAll()

            val accountIdByName = existingAccounts.associateTo(mutableMapOf()) { it.name to it.id }
            val categoryIdByKey = existingCategories.associateTo(mutableMapOf()) {
                "${it.name}|${it.kind}" to it.id
            }

            suspend fun resolveAccountId(name: String): Long =
                accountIdByName.getOrPut(name) {
                    database.accountDao().insert(Account(name = name, type = DEFAULT_ACCOUNT_TYPE))
                }

            suspend fun resolveCategoryId(name: String, kind: String): Long =
                categoryIdByKey.getOrPut("$name|$kind") {
                    database.categoryDao().insert(Category(name = name, kind = kind, emoji = DEFAULT_CATEGORY_EMOJI))
                }

            parsedRows.forEach { row ->
                when (row.type) {
                    RowType.EXPENSE -> {
                        database.expenseDao().insert(
                            Expense(
                                amount = row.amount,
                                categoryId = resolveCategoryId(row.categoryName, CategoryKind.EXPENSE),
                                note = row.note,
                                accountId = resolveAccountId(row.accountName),
                                timestampMillis = row.timestampMillis
                            )
                        )
                        expenseCount++
                    }
                    RowType.INCOME -> {
                        database.incomeDao().insert(
                            Income(
                                amount = row.amount,
                                categoryId = resolveCategoryId(row.categoryName, CategoryKind.INCOME),
                                note = row.note,
                                accountId = resolveAccountId(row.accountName),
                                timestampMillis = row.timestampMillis
                            )
                        )
                        incomeCount++
                    }
                    RowType.TRANSFER -> {
                        database.transferDao().insert(
                            Transfer(
                                fromAccountId = resolveAccountId(row.fromAccountName),
                                toAccountId = resolveAccountId(row.toAccountName),
                                amount = row.amount,
                                note = row.note,
                                timestampMillis = row.timestampMillis
                            )
                        )
                        transferCount++
                    }
                }
            }
        }

        ImportResult.Success(expenseCount, incomeCount, transferCount)
    }
