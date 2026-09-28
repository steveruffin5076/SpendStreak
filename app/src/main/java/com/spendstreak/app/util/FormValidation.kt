package com.spendstreak.app.util

/** Shared field validation for transaction/budget/recurring forms (unit-testable). */
object FormValidation {
    val AMOUNT_INPUT_PATTERN: Regex = Regex("^\\d{0,9}(\\.\\d{0,2})?$")

    fun validatePositiveAmount(amountText: String): String? {
        val parsed = amountText.toDoubleOrNull()
        return if (parsed != null && parsed > 0) null else "Enter a valid amount."
    }

    fun validateNonBlankName(name: String, emptyMessage: String = "Enter a name."): String? =
        if (name.isBlank()) emptyMessage else null

    fun validateTransferAccounts(fromId: Long?, toId: Long?): String? = when {
        fromId == null || toId == null -> "No account available yet."
        fromId == toId -> "Choose two different accounts."
        else -> null
    }

    fun validateCustomDateRange(startMillis: Long?, endMillis: Long?): String? = when {
        startMillis == null || endMillis == null -> "Pick a start and end date."
        endMillis < startMillis -> "End date must be on or after the start date."
        else -> null
    }

    fun validateBudgetName(name: String): String? =
        validateNonBlankName(name, "Enter a name for this budget.")

    fun validateRecurringFields(
        amountText: String,
        categoryId: Long?,
        accountId: Long?
    ): String? = when {
        validatePositiveAmount(amountText) != null -> validatePositiveAmount(amountText)
        categoryId == null -> "Choose a category."
        accountId == null -> "Choose an account."
        else -> null
    }
}
