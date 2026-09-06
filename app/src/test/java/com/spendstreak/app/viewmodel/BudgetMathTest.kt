package com.spendstreak.app.viewmodel

import com.spendstreak.app.data.Account
import com.spendstreak.app.data.Budget
import com.spendstreak.app.data.BudgetPeriodType
import com.spendstreak.app.data.CategoryBudget
import com.spendstreak.app.data.Expense
import com.spendstreak.app.data.Income
import com.spendstreak.app.data.Transfer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZoneOffset

// UTC everywhere so budgetPeriodBounds' calendar-day math is deterministic regardless of
// the machine running the test.
private val UTC: ZoneId = ZoneOffset.UTC
private const val MILLIS_PER_DAY = 86_400_000L

private fun expense(
    amount: Double,
    accountId: Long = 1,
    timestampMillis: Long = 0,
    excludedFromBudget: Boolean = false,
    categoryId: Long = 1
) = Expense(amount = amount, categoryId = categoryId, note = "", accountId = accountId, timestampMillis = timestampMillis, excludedFromBudget = excludedFromBudget)

private fun income(amount: Double, accountId: Long = 1) =
    Income(amount = amount, categoryId = 1, note = "", accountId = accountId, timestampMillis = 0)

private fun transfer(amount: Double, fromAccountId: Long, toAccountId: Long) =
    Transfer(fromAccountId = fromAccountId, toAccountId = toAccountId, amount = amount, note = "", timestampMillis = 0)

private fun account(id: Long, type: String = "Bank", openingBalance: Double = 0.0) =
    Account(id = id, name = "Account $id", type = type, openingBalance = openingBalance)

class BudgetMathTest {

    // --- computeAccountBalances -------------------------------------------------------

    @Test
    fun `regular account starts from its opening balance`() {
        val balances = computeAccountBalances(
            expenses = emptyList(),
            income = emptyList(),
            transfers = emptyList(),
            accounts = listOf(account(id = 1, openingBalance = 100.0))
        )
        assertEquals(100.0, balances[1])
    }

    @Test
    fun `credit card account starts at zero regardless of its opening balance field`() {
        // For a Credit Card account, openingBalance actually holds the credit limit, not
        // money owned — must never seed the balance with it.
        val balances = computeAccountBalances(
            expenses = emptyList(),
            income = emptyList(),
            transfers = emptyList(),
            accounts = listOf(account(id = 1, type = Account.TYPE_CREDIT_CARD, openingBalance = 5000.0))
        )
        assertEquals(0.0, balances[1])
    }

    @Test
    fun `income adds and expense subtracts from the owning account only`() {
        val balances = computeAccountBalances(
            expenses = listOf(expense(amount = 30.0, accountId = 1)),
            income = listOf(income(amount = 200.0, accountId = 1)),
            transfers = emptyList(),
            accounts = listOf(account(id = 1, openingBalance = 100.0), account(id = 2, openingBalance = 50.0))
        )
        assertEquals(270.0, balances[1])
        assertEquals(50.0, balances[2])
    }

    @Test
    fun `transfer moves balance from the source account to the destination account`() {
        val balances = computeAccountBalances(
            expenses = emptyList(),
            income = emptyList(),
            transfers = listOf(transfer(amount = 40.0, fromAccountId = 1, toAccountId = 2)),
            accounts = listOf(account(id = 1, openingBalance = 100.0), account(id = 2, openingBalance = 100.0))
        )
        assertEquals(60.0, balances[1])
        assertEquals(140.0, balances[2])
    }

    // --- budgetPeriodBounds -------------------------------------------------------------

    @Test
    fun `custom period bounds are the start day at midnight through the end day exclusive`() {
        val budget = Budget(
            name = "Trip",
            amountLimit = 1000.0,
            periodType = BudgetPeriodType.CUSTOM,
            startEpochDay = 0L, // 1970-01-01
            endEpochDay = 2L    // 1970-01-03, inclusive
        )
        val (start, end) = budgetPeriodBounds(budget, UTC)
        assertEquals(0L, start)
        assertEquals(3 * MILLIS_PER_DAY, end) // exclusive upper bound: day after the end day
    }

    @Test
    fun `monthly period bounds start at midnight on the 1st of the current month`() {
        val budget = Budget(name = "Monthly", amountLimit = 500.0, periodType = BudgetPeriodType.MONTHLY)
        val beforeCall = System.currentTimeMillis()
        val (start, end) = budgetPeriodBounds(budget, UTC)
        assertTrue("start should be a whole day boundary", start % MILLIS_PER_DAY == 0L)
        assertTrue("end should be roughly now", end in beforeCall..(beforeCall + 5_000))
    }

    // --- computeBudgetProgress -----------------------------------------------------------

    @Test
    fun `null active budget yields null progress`() {
        assertNull(computeBudgetProgress(expenses = listOf(expense(50.0)), activeBudget = null, zone = UTC))
    }

    @Test
    fun `only expenses inside the budget period are counted as spent`() {
        val budget = Budget(
            name = "Trip",
            amountLimit = 100.0,
            periodType = BudgetPeriodType.CUSTOM,
            startEpochDay = 1L,
            endEpochDay = 1L // spans [1 day, 2 days) in millis
        )
        val insideBudget = expense(amount = 30.0, timestampMillis = MILLIS_PER_DAY + 1)
        val beforePeriod = expense(amount = 999.0, timestampMillis = 0)
        val afterPeriod = expense(amount = 999.0, timestampMillis = 2 * MILLIS_PER_DAY)

        val progress = computeBudgetProgress(listOf(insideBudget, beforePeriod, afterPeriod), budget, UTC)

        assertEquals(30.0, progress?.spent)
    }

    @Test
    fun `expenses excluded from budget are never counted even inside the period`() {
        val budget = Budget(name = "Monthly", amountLimit = 100.0, periodType = BudgetPeriodType.MONTHLY)
        // A moment ago rather than exactly "now" — the MONTHLY period's end is also
        // System.currentTimeMillis(), computed fresh inside the function under test, so an
        // expense timestamped at the literal same instant could tie against its exclusive
        // upper bound on a fast machine.
        val aMomentAgo = System.currentTimeMillis() - 1_000
        val excluded = expense(amount = 500.0, timestampMillis = aMomentAgo, excludedFromBudget = true)
        val counted = expense(amount = 20.0, timestampMillis = aMomentAgo, excludedFromBudget = false)

        val progress = computeBudgetProgress(listOf(excluded, counted), budget, UTC)

        assertEquals(20.0, progress?.spent)
    }

    @Test
    fun `isOverBudget is true only when spent strictly exceeds the limit`() {
        val budget = Budget(name = "Monthly", amountLimit = 100.0, periodType = BudgetPeriodType.MONTHLY)
        val aMomentAgo = System.currentTimeMillis() - 1_000

        val atLimit = computeBudgetProgress(listOf(expense(100.0, timestampMillis = aMomentAgo)), budget, UTC)
        val overLimit = computeBudgetProgress(listOf(expense(100.01, timestampMillis = aMomentAgo)), budget, UTC)

        assertEquals(false, atLimit?.isOverBudget)
        assertEquals(true, overLimit?.isOverBudget)
    }

    // --- computeCategoryBudgetProgress ---------------------------------------------------

    @Test
    fun `no category budgets yields an empty map`() {
        val progress = computeCategoryBudgetProgress(
            expenses = listOf(expense(50.0, categoryId = 1)),
            categoryBudgets = emptyList(),
            zone = UTC
        )
        assertTrue(progress.isEmpty())
    }

    @Test
    fun `each category budget only counts expenses in its own category`() {
        val aMomentAgo = System.currentTimeMillis() - 1_000
        val expenses = listOf(
            expense(30.0, categoryId = 1, timestampMillis = aMomentAgo),
            expense(70.0, categoryId = 2, timestampMillis = aMomentAgo)
        )
        val budgets = listOf(
            CategoryBudget(categoryId = 1, monthlyLimit = 100.0),
            CategoryBudget(categoryId = 2, monthlyLimit = 50.0)
        )

        val progress = computeCategoryBudgetProgress(expenses, budgets, UTC)

        assertEquals(30.0, progress[1]?.spent)
        assertEquals(false, progress[1]?.isOverBudget)
        assertEquals(70.0, progress[2]?.spent)
        assertEquals(true, progress[2]?.isOverBudget)
    }

    @Test
    fun `a category with no spend this month still reports zero spent`() {
        val progress = computeCategoryBudgetProgress(
            expenses = emptyList(),
            categoryBudgets = listOf(CategoryBudget(categoryId = 1, monthlyLimit = 200.0)),
            zone = UTC
        )
        assertEquals(0.0, progress[1]?.spent)
    }
}
