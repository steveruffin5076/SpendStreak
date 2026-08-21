package com.spendstreak.app.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val database: SpendStreakDatabase) {

    val categories: Flow<List<Category>> = database.categoryDao().getAll()

    suspend fun addCategory(name: String, kind: String, emoji: String) {
        database.categoryDao().insert(Category(name = name, kind = kind, emoji = emoji))
    }

    suspend fun updateCategory(category: Category) {
        database.categoryDao().update(category)
    }

    // Returns false without deleting if the category has any linked Expense/Income —
    // mirrors AccountRepository.deleteAccount() exactly: never cascade, never reassign.
    suspend fun deleteCategory(categoryId: Long): Boolean = database.withTransaction {
        if (database.categoryDao().countTransactionsUsing(categoryId) > 0) {
            false
        } else {
            database.categoryDao().delete(categoryId)
            true
        }
    }
}
