package com.spendstreak.app.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class AccountRepository(private val database: SpendStreakDatabase) {

    val accounts: Flow<List<Account>> = database.accountDao().getAll()

    suspend fun addAccount(name: String, type: String) {
        database.accountDao().insert(Account(name = name, type = type))
    }

    // Returns false without deleting if the account has any linked Expense/Income —
    // we never cascade-delete or reassign, that's the caller's/user's call to make.
    // Wrapped in a transaction so a concurrent insert against this account can't land
    // between the usage check and the delete (SQLite serializes transactions).
    suspend fun deleteAccount(accountId: Long): Boolean = database.withTransaction {
        if (database.accountDao().countTransactionsUsing(accountId) > 0) {
            false
        } else {
            database.accountDao().delete(accountId)
            true
        }
    }
}
