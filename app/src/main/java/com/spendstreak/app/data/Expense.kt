package com.spendstreak.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = Expense.TABLE_NAME)
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val categoryId: Long,
    val note: String,
    val accountId: Long,
    val timestampMillis: Long,
    val excludedFromBudget: Boolean = false
) {
    companion object {
        const val TABLE_NAME = "expenses"
    }
}
