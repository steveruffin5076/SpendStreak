package com.spendstreak.app.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class BudgetRepository(
    private val database: SpendStreakDatabase,
    private val userProgressRepository: UserProgressRepository
) {

    val budget: Flow<Budget?> = database.budgetDao().observeActive()
    val budgetHistory: Flow<List<Budget>> = database.budgetDao().observeAll()

    suspend fun setMonthlyBudget(name: String, amountLimit: Double) {
        database.withTransaction {
            database.budgetDao().deactivateCurrent()
            database.budgetDao().insert(
                Budget(name = name, amountLimit = amountLimit, periodType = BudgetPeriodType.MONTHLY)
            )
        }
        userProgressRepository.markBudgetEverSet()
    }

    suspend fun setCustomBudget(name: String, amountLimit: Double, startEpochDay: Long, endEpochDay: Long) {
        database.withTransaction {
            database.budgetDao().deactivateCurrent()
            database.budgetDao().insert(
                Budget(
                    name = name,
                    amountLimit = amountLimit,
                    periodType = BudgetPeriodType.CUSTOM,
                    startEpochDay = startEpochDay,
                    endEpochDay = endEpochDay
                )
            )
        }
        userProgressRepository.markBudgetEverSet()
    }

    // Plain in-place edit — unlike setMonthlyBudget/setCustomBudget, this never
    // deactivates+inserts, so correcting a typo or nudging the amount never spawns a
    // spurious history entry.
    suspend fun updateBudget(budget: Budget) {
        database.budgetDao().update(budget)
    }

    // Retires the active budget into history rather than deleting it — "clear" always
    // keeps history, unlike deleteBudget() below which is an explicit, deliberate removal
    // of one specific row (typically a past budget the user no longer wants kept).
    suspend fun clearBudget() {
        database.budgetDao().deactivateCurrent()
    }

    suspend fun deleteBudget(id: Long) {
        database.budgetDao().delete(id)
    }

    // Only called from the app-wide "Clear All Data" path — wipes history too, unlike
    // clearBudget() above.
    suspend fun clearAllData() {
        database.budgetDao().deleteAll()
    }
}
