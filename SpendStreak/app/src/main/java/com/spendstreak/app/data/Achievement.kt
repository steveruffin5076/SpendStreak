package com.spendstreak.app.data

import java.time.Instant
import java.time.ZoneId

data class Achievement(val name: String, val description: String, val unlocked: Boolean)

// Achievement state is derived from expenses/income/transfers + progress rather than
// stored separately — simpler than keeping another table in sync, and there's no way to
// "lose" one once earned since every condition here only grows (counts, streaks,
// UserProgress.hasSetBudget) rather than reflecting current, possibly-reverted state.
object Achievements {
    fun compute(
        expenses: List<Expense>,
        income: List<Income>,
        transfers: List<Transfer>,
        progress: UserProgress
    ): List<Achievement> {
        val totalExpenses = expenses.size
        val distinctCategories = expenses.map { it.categoryId }.distinct().size
        val hasLateNightExpense = expenses.any { expense ->
            val hour = Instant.ofEpochMilli(expense.timestampMillis)
                .atZone(ZoneId.systemDefault())
                .hour
            hour >= 23
        }
        val distinctIncomeSources = income.map { it.categoryId }.distinct().size

        return listOf(
            Achievement("FIRST STEPS", "Log your first expense", totalExpenses >= 1),
            Achievement("WEEK WARRIOR", "Reach a 7-day streak", progress.longestStreak >= 7),
            Achievement("BUDGET NOVICE", "Log expenses in 3 categories", distinctCategories >= 3),
            Achievement("TWO WEEK TITAN", "Reach a 14-day streak", progress.longestStreak >= 14),
            Achievement("HALF CENTURY", "Log 50 expenses", totalExpenses >= 50),
            Achievement("CENTURY CLUB", "Log 100 expenses", totalExpenses >= 100),
            Achievement("NIGHT OWL", "Log an expense after 11 PM", hasLateNightExpense),
            Achievement("FIRST PAYCHECK", "Log your first income", income.isNotEmpty()),
            Achievement("DIVERSE INCOME", "Log income from 3 different sources", distinctIncomeSources >= 3),
            Achievement("PLANNER", "Set a spending budget", progress.hasSetBudget),
            Achievement("MONEY MOVER", "Make your first transfer", transfers.isNotEmpty()),
            Achievement("ACCOUNT JUGGLER", "Make 10 transfers", transfers.size >= 10)
        )
    }
}
