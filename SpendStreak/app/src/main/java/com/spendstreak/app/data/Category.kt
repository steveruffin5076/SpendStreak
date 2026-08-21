package com.spendstreak.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

object CategoryKind {
    const val EXPENSE = "EXPENSE"
    const val INCOME = "INCOME"
}

@Entity(tableName = Category.TABLE_NAME)
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: String,
    val emoji: String
) {
    companion object {
        const val TABLE_NAME = "categories"
    }
}
