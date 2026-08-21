package com.spendstreak.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeDao {
    @Insert
    suspend fun insert(income: Income)

    @Update
    suspend fun update(income: Income)

    @Query("SELECT * FROM income ORDER BY timestampMillis DESC")
    fun getAll(): Flow<List<Income>>

    @Query("DELETE FROM income WHERE id = :incomeId")
    suspend fun delete(incomeId: Long)

    @Query("DELETE FROM income")
    suspend fun deleteAll()
}
