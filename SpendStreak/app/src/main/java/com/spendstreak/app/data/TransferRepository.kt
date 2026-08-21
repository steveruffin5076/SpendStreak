package com.spendstreak.app.data

import kotlinx.coroutines.flow.Flow

// No UserProgressRepository dependency and no withTransaction here on purpose — a
// transfer is a single-table insert with no streak/XP side effect, unlike Expense/Income.
class TransferRepository(private val database: SpendStreakDatabase) {

    val transfers: Flow<List<Transfer>> = database.transferDao().getAll()

    suspend fun addTransfer(fromAccountId: Long, toAccountId: Long, amount: Double, note: String) {
        database.transferDao().insert(
            Transfer(
                fromAccountId = fromAccountId,
                toAccountId = toAccountId,
                amount = amount,
                note = note,
                timestampMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateTransfer(transfer: Transfer) {
        database.transferDao().update(transfer)
    }

    suspend fun deleteTransfer(transferId: Long) {
        database.transferDao().delete(transferId)
    }

    suspend fun clearAllData() {
        database.transferDao().deleteAll()
    }
}
