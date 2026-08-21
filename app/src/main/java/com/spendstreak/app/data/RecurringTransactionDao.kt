package com.spendstreak.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {
    @Insert
    suspend fun insert(recurring: RecurringTransaction)

    @Update
    suspend fun update(recurring: RecurringTransaction)

    @Query("DELETE FROM ${RecurringTransaction.TABLE_NAME} WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM ${RecurringTransaction.TABLE_NAME}")
    suspend fun deleteAll()

    @Query("SELECT * FROM ${RecurringTransaction.TABLE_NAME} ORDER BY nextDueEpochDay ASC")
    fun getAll(): Flow<List<RecurringTransaction>>

    @Query("SELECT * FROM ${RecurringTransaction.TABLE_NAME} WHERE id = :id")
    suspend fun getById(id: Long): RecurringTransaction?

    // Read by ReminderWorker's daily background check — plain suspend, not Flow, since a
    // Worker runs a one-shot check outside any Composition.
    @Query(
        "SELECT * FROM ${RecurringTransaction.TABLE_NAME} " +
            "WHERE active = 1 AND nextDueEpochDay <= :todayEpochDay"
    )
    suspend fun getDue(todayEpochDay: Long): List<RecurringTransaction>
}
