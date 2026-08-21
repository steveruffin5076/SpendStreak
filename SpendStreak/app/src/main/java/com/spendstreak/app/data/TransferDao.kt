package com.spendstreak.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {
    @Insert
    suspend fun insert(transfer: Transfer)

    @Update
    suspend fun update(transfer: Transfer)

    @Query("SELECT * FROM ${Transfer.TABLE_NAME} ORDER BY timestampMillis DESC")
    fun getAll(): Flow<List<Transfer>>

    @Query("DELETE FROM ${Transfer.TABLE_NAME} WHERE id = :transferId")
    suspend fun delete(transferId: Long)

    @Query("DELETE FROM ${Transfer.TABLE_NAME}")
    suspend fun deleteAll()
}
