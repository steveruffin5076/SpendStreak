package com.spendstreak.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = Income.TABLE_NAME)
data class Income(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val categoryId: Long,
    val note: String,
    val accountId: Long,
    val timestampMillis: Long
) {
    companion object {
        const val TABLE_NAME = "income"
    }
}
