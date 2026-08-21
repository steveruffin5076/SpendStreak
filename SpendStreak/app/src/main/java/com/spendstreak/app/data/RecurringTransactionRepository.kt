package com.spendstreak.app.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class RecurringTransactionRepository(private val database: SpendStreakDatabase) {

    val recurringTransactions: Flow<List<RecurringTransaction>> = database.recurringTransactionDao().getAll()

    suspend fun addRecurring(recurring: RecurringTransaction) {
        database.recurringTransactionDao().insert(recurring)
    }

    suspend fun updateRecurring(recurring: RecurringTransaction) {
        database.recurringTransactionDao().update(recurring)
    }

    suspend fun deleteRecurring(id: Long) {
        database.recurringTransactionDao().delete(id)
    }

    suspend fun clearAllData() {
        database.recurringTransactionDao().deleteAll()
    }

    suspend fun getById(id: Long): RecurringTransaction? = database.recurringTransactionDao().getById(id)

    suspend fun getDue(todayEpochDay: Long): List<RecurringTransaction> =
        database.recurringTransactionDao().getDue(todayEpochDay)

    // Called by ReminderActionReceiver on both LOG IT and SKIP — either way, this occurrence
    // is done, so the rule always advances to its next period. Only LOG IT additionally
    // inserts the actual Expense/Income row; SKIP just moves on without one.
    suspend fun advanceToNextPeriod(id: Long) {
        val current = database.recurringTransactionDao().getById(id) ?: return
        val nextDue = when (current.intervalType) {
            RecurringInterval.WEEKLY -> current.nextDueEpochDay + 7
            else -> LocalDate.ofEpochDay(current.nextDueEpochDay).plusMonths(1).toEpochDay()
        }
        database.recurringTransactionDao().update(current.copy(nextDueEpochDay = nextDue))
    }
}
