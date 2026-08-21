package com.spendstreak.app.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class IncomeRepository(
    private val database: SpendStreakDatabase,
    private val userProgressRepository: UserProgressRepository
) {

    val income: Flow<List<Income>> = database.incomeDao().getAll()

    // Insert + streak update run in one transaction so a process death between the two
    // can't leave a durably-saved income entry with no matching streak/XP credit.
    suspend fun addIncome(amount: Double, categoryId: Long, accountId: Long, note: String) {
        database.withTransaction {
            database.incomeDao().insert(
                Income(
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

    // Deliberately does NOT touch streak/XP — same reasoning as ExpenseRepository.
    suspend fun updateIncome(income: Income) {
        database.incomeDao().update(income)
    }

    suspend fun deleteIncome(incomeId: Long) {
        database.incomeDao().delete(incomeId)
    }

    suspend fun clearAllData() {
        database.incomeDao().deleteAll()
    }
}
