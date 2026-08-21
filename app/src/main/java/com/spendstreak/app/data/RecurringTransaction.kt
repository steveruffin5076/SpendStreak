package com.spendstreak.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

object RecurringTransactionType {
    const val EXPENSE = "EXPENSE"
    const val INCOME = "INCOME"
}

object RecurringInterval {
    const val WEEKLY = "WEEKLY"
    const val MONTHLY = "MONTHLY"
}

// Mirrors Transfer.kt's shape/style. `nextDueEpochDay` advances by one interval every time
// the rule fires (via ReminderActionReceiver on LOG IT/SKIP) — it's the single source of
// truth for "is this due today", read by ReminderWorker's daily check.
@Entity(tableName = RecurringTransaction.TABLE_NAME)
data class RecurringTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val amount: Double,
    val categoryId: Long,
    val accountId: Long,
    val note: String,
    val intervalType: String,
    val nextDueEpochDay: Long,
    val active: Boolean = true
) {
    companion object {
        const val TABLE_NAME = "recurring_transactions"
    }
}
