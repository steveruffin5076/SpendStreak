package com.spendstreak.app.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val database: SpendStreakDatabase,
    private val userProgressRepository: UserProgressRepository
) {

    val expenses: Flow<List<Expense>> = database.expenseDao().getAll()

    // Insert + streak update run in one transaction so a process death between the two
    // can't leave a durably-saved expense with no matching streak/XP credit.
    suspend fun addExpense(amount: Double, categoryId: Long, accountId: Long, note: String) {
        database.withTransaction {
            database.expenseDao().insert(
                Expense(
                    amount = amount,
                    categoryId = categoryId,
                    note = note,
                    accountId = accountId,
                    timestampMillis = System.currentTimeMillis()
                )
            )
            userProgressRepository.recordDailyActivity()
        }
    }

    // Deliberately does NOT touch streak/XP — that's keyed to "did a log happen today,"
    // not to a row's current field values, and re-running it on every edit would let
    // repeatedly editing the same entry farm extra credit. Consistent with the existing
    // "achievements only grow" invariant: editing/deleting never revokes anything either.
    suspend fun updateExpense(expense: Expense) {
        database.expenseDao().update(expense)
    }

    suspend fun deleteExpense(expenseId: Long) {
        database.expenseDao().delete(expenseId)
    }

    suspend fun clearAllData() {
        database.expenseDao().deleteAll()
    }
}
