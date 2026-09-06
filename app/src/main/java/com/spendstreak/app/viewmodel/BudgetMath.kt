package com.spendstreak.app.viewmodel

import com.spendstreak.app.data.Account
import com.spendstreak.app.data.Budget
import com.spendstreak.app.data.BudgetPeriodType
import com.spendstreak.app.data.CategoryBudget
import com.spendstreak.app.data.Expense
import com.spendstreak.app.data.Income
import com.spendstreak.app.data.Transfer
import java.time.LocalDate
import java.time.ZoneId

// Extracted from SpendStreakViewModel's combine{} lambdas so the app's core money math is
// plain-data-in/plain-data-out and unit-testable without a running ViewModel or Room DB.
// Behavior must stay identical to what the ViewModel inlined before — see BudgetMathTest.

// Starts from each account's opening balance (zero for Credit Card accounts, where the
// same column instead means credit limit — a spending cap, not money contributing to a
// balance). Transfers move money between accounts (source down, destination up).
fun computeAccountBalances(
    expenses: List<Expense>,
    income: List<Income>,
    transfers: List<Transfer>,
    accounts: List<Account>
): Map<Long, Double> {
    val balances = mutableMapOf<Long, Double>()
    accounts.forEach { account ->
        balances[account.id] = if (account.type == Account.TYPE_CREDIT_CARD) 0.0 else account.openingBalance
    }
    income.forEach { balances[it.accountId] = (balances[it.accountId] ?: 0.0) + it.amount }
    expenses.forEach { balances[it.accountId] = (balances[it.accountId] ?: 0.0) - it.amount }
    transfers.forEach {
        balances[it.fromAccountId] = (balances[it.fromAccountId] ?: 0.0) - it.amount
        balances[it.toAccountId] = (balances[it.toAccountId] ?: 0.0) + it.amount
    }
    return balances
}

// "1st of the current month, at midnight, through now" — shared by the MONTHLY branch of
// budgetPeriodBounds below and computeCategoryBudgetProgress, which always uses the
// current month (no CUSTOM concept for a per-category cap).
fun currentMonthBounds(zone: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
    val startOfMonth = LocalDate.now(zone).withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
    return startOfMonth to System.currentTimeMillis()
}

// The active period for a Budget: MONTHLY is always "1st of the current month through
// now"; CUSTOM uses its own start/end epoch days, end inclusive of its whole day (hence
// the +1 day exclusive upper bound).
fun budgetPeriodBounds(budget: Budget, zone: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> =
    if (budget.periodType == BudgetPeriodType.MONTHLY) {
        currentMonthBounds(zone)
    } else {
        val start = budget.startEpochDay
            ?.let { LocalDate.ofEpochDay(it).atStartOfDay(zone).toInstant().toEpochMilli() }
            ?: 0L
        val end = budget.endEpochDay
            ?.let { LocalDate.ofEpochDay(it).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() }
            ?: System.currentTimeMillis()
        start to end
    }

fun computeBudgetProgress(
    expenses: List<Expense>,
    activeBudget: Budget?,
    zone: ZoneId = ZoneId.systemDefault()
): BudgetProgress? {
    if (activeBudget == null) return null
    val (periodStartMillis, periodEndMillis) = budgetPeriodBounds(activeBudget, zone)
    val spent = expenses
        .filter { it.timestampMillis in periodStartMillis until periodEndMillis && !it.excludedFromBudget }
        .sumOf { it.amount }
    return BudgetProgress(limit = activeBudget.amountLimit, spent = spent, isOverBudget = spent > activeBudget.amountLimit)
}

// Per-category caps always track the current calendar month — unlike the overall Budget,
// there's no CUSTOM period concept here (see CategoryBudget.kt).
fun computeCategoryBudgetProgress(
    expenses: List<Expense>,
    categoryBudgets: List<CategoryBudget>,
    zone: ZoneId = ZoneId.systemDefault()
): Map<Long, BudgetProgress> {
    if (categoryBudgets.isEmpty()) return emptyMap()
    val (periodStartMillis, periodEndMillis) = currentMonthBounds(zone)
    val spentByCategory = expenses
        .filter { it.timestampMillis in periodStartMillis until periodEndMillis && !it.excludedFromBudget }
        .groupBy { it.categoryId }
        .mapValues { (_, entries) -> entries.sumOf { it.amount } }
    return categoryBudgets.associate { categoryBudget ->
        val spent = spentByCategory[categoryBudget.categoryId] ?: 0.0
        categoryBudget.categoryId to BudgetProgress(
            limit = categoryBudget.monthlyLimit,
            spent = spent,
            isOverBudget = spent > categoryBudget.monthlyLimit
        )
    }
}
