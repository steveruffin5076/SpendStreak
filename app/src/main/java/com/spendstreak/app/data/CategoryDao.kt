package com.spendstreak.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert
    suspend fun insert(category: Category): Long

    @Update
    suspend fun update(category: Category)

    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun getAll(): Flow<List<Category>>

    // Mirrors AccountDao.countTransactionsUsing — every entity that references a
    // category needs a term added here.
    @Query(
        "SELECT (SELECT COUNT(*) FROM ${Expense.TABLE_NAME} WHERE categoryId = :categoryId) + " +
            "(SELECT COUNT(*) FROM ${Income.TABLE_NAME} WHERE categoryId = :categoryId) + " +
            "(SELECT COUNT(*) FROM ${RecurringTransaction.TABLE_NAME} WHERE categoryId = :categoryId)"
    )
    suspend fun countTransactionsUsing(categoryId: Long): Int

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun delete(categoryId: Long)

    // Only for the full-backup restore path (BackupImporter) — unlike delete() above,
    // and unlike "Clear All Data" (which deliberately keeps categories), a backup restore
    // wholesale replaces every table including this one.
    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}
