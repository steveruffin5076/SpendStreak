package com.spendstreak.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Insert
    suspend fun insert(account: Account): Long

    @Query("SELECT * FROM accounts ORDER BY id ASC")
    fun getAll(): Flow<List<Account>>

    // Every entity that references an account needs a term added here — there's no
    // compile-time enumeration of "tables referencing accounts", only this manual list.
    // Transfer has two account columns (not one), so it needs an OR rather than a
    // straight column match.
    @Query(
        "SELECT (SELECT COUNT(*) FROM ${Expense.TABLE_NAME} WHERE accountId = :accountId) + " +
            "(SELECT COUNT(*) FROM ${Income.TABLE_NAME} WHERE accountId = :accountId) + " +
            "(SELECT COUNT(*) FROM ${Transfer.TABLE_NAME} WHERE fromAccountId = :accountId OR toAccountId = :accountId) + " +
            "(SELECT COUNT(*) FROM ${RecurringTransaction.TABLE_NAME} WHERE accountId = :accountId)"
    )
    suspend fun countTransactionsUsing(accountId: Long): Int

    @Query("DELETE FROM accounts WHERE id = :accountId")
    suspend fun delete(accountId: Long)

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()
}
