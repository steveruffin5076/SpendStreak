package com.spendstreak.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budget WHERE isActive = 1 LIMIT 1")
    fun observeActive(): Flow<Budget?>

    @Query("SELECT * FROM budget ORDER BY id DESC")
    fun observeAll(): Flow<List<Budget>>

    @Insert
    suspend fun insert(budget: Budget)

    // In-place edit of the active budget — no deactivate/insert, so it never creates a
    // spurious history entry the way setMonthlyBudget/setCustomBudget deliberately do.
    @Update
    suspend fun update(budget: Budget)

    @Query("UPDATE budget SET isActive = 0 WHERE isActive = 1")
    suspend fun deactivateCurrent()

    // Deletes one specific budget (active or history) — unlike deleteAll() below, this is
    // the normal per-row delete used when the user removes a single past budget.
    @Query("DELETE FROM budget WHERE id = :budgetId")
    suspend fun delete(budgetId: Long)

    // Only for the app-wide "Clear All Data" path — wipes history too, unlike
    // deactivateCurrent() above, which just retires the active budget into history.
    @Query("DELETE FROM budget")
    suspend fun deleteAll()
}
