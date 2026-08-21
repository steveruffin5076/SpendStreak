package com.spendstreak.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = Transfer.TABLE_NAME)
data class Transfer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromAccountId: Long,
    val toAccountId: Long,
    val amount: Double,
    val note: String,
    val timestampMillis: Long
) {
    companion object {
        const val TABLE_NAME = "transfers"
    }
}
