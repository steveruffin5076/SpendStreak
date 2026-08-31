package com.spendstreak.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    // For every type except Credit Card, this is the account's starting balance and
    // contributes to its running balance. For Credit Card, the same column instead
    // holds the credit limit — informational only, never added into any balance
    // total (see SpendStreakViewModel.accountBalances).
    val openingBalance: Double = 0.0
) {
    companion object {
        const val TYPE_CREDIT_CARD = "Credit Card"
    }
}
