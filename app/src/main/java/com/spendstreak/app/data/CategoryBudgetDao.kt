package com.spendstreak.app.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryBudgetDao {
    @Query("SELECT * FROM category_budgets")
    fun observeAll(): Flow<List<CategoryBudget>>

    // Setting a limit for a category that already has one just replaces it — categoryId is
    // the primary key, so there's at most one row per category.
    @Upsert
    suspend fun upsert(categoryBudget: CategoryBudget)

    @Query("DELETE FROM category_budgets WHERE categoryId = :categoryId")
    suspend fun deleteByCategory(categoryId: Long)

    // Only for the app-wide "Clear All Data" path, same as BudgetDao.deleteAll().
    @Query("DELETE FROM category_budgets")
    suspend fun deleteAll()
}
