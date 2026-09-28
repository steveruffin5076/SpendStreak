package com.spendstreak.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormValidationTest {
    @Test
    fun validatePositiveAmount_acceptsDecimals() {
        assertNull(FormValidation.validatePositiveAmount("12.50"))
    }

    @Test
    fun validatePositiveAmount_rejectsZero() {
        assertEquals("Enter a valid amount.", FormValidation.validatePositiveAmount("0"))
    }

    @Test
    fun validateTransferAccounts_rejectsSameAccount() {
        assertEquals("Choose two different accounts.", FormValidation.validateTransferAccounts(1L, 1L))
    }

    @Test
    fun validateCustomDateRange_rejectsInvertedRange() {
        assertEquals(
            "End date must be on or after the start date.",
            FormValidation.validateCustomDateRange(100L, 50L)
        )
    }
}
