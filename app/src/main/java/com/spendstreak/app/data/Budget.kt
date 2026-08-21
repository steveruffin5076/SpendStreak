package com.spendstreak.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

object BudgetPeriodType {
    const val MONTHLY = "MONTHLY"
    const val CUSTOM = "CUSTOM"
}

// One row per budget ever set — `isActive` marks the single current one, everything else
// is kept as history (replaces the old "id is always 0, one row total" singleton design).
// periodType is one of BudgetPeriodType.MONTHLY / .CUSTOM (plain String column, no Room
// TypeConverter needed). startEpochDay/endEpochDay are only meaningful when
// periodType == CUSTOM; both stay null for MONTHLY, whose period is always
// "1st of the current month to now".
@Entity(tableName = "budget")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amountLimit: Double,
    val periodType: String,
    val startEpochDay: Long? = null,
    val endEpochDay: Long? = null,
    val isActive: Boolean = true
)
