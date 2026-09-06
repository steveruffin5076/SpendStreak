package com.spendstreak.app.data

import kotlinx.coroutines.flow.Flow

class CategoryBudgetRepository(private val database: SpendStreakDatabase) {

    val categoryBudgets: Flow<List<CategoryBudget>> = database.categoryBudgetDao().observeAll()

    suspend fun setLimit(categoryId: Long, monthlyLimit: Double) {
        database.categoryBudgetDao().upsert(CategoryBudget(categoryId = categoryId, monthlyLimit = monthlyLimit))
    }

    suspend fun clearLimit(categoryId: Long) {
        database.categoryBudgetDao().deleteByCategory(categoryId)
    }

    // Only called from the app-wide "Clear All Data" path, same as BudgetRepository.clearAllData().
    suspend fun clearAllData() {
        database.categoryBudgetDao().deleteAll()
    }
}
