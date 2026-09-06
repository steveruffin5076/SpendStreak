package com.spendstreak.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// A simpler sibling to Budget: an optional monthly spending cap per expense category, no
// period type/history/active flag needed — setting a new limit for a category just
// replaces its row, and removing the cap just deletes it. categoryId is the primary key
// since a category can have at most one cap.
@Entity(tableName = "category_budgets")
data class CategoryBudget(
    @PrimaryKey val categoryId: Long,
    val monthlyLimit: Double
)
