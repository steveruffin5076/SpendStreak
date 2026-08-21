package com.spendstreak.app.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

// Owns the streak/XP/level math shared by every kind of daily log (Expense, Income, ...).
class UserProgressRepository(private val database: SpendStreakDatabase) {

    val progress: Flow<UserProgress?> = database.userProgressDao().observe()

    // Wrapped in a single DB transaction so two logs fired close together (e.g. an
    // expense and an income within the same moment) can't both read stale progress
    // and have the second write clobber the first — SQLite serializes transactions.
    suspend fun recordDailyActivity() {
        database.withTransaction {
            val current = database.userProgressDao().get() ?: UserProgress()
            val today = LocalDate.now().toEpochDay()

            val newStreak = when (current.lastLoggedEpochDay) {
                today -> current.currentStreak
                today - 1 -> current.currentStreak + 1
                else -> 1
            }

            // Only affects XP, never the streak above — streak already only increments
            // once per day regardless of log count, so it was never gameable this way.
            val newLogsToday = if (current.lastLoggedEpochDay == today) current.logsToday + 1 else 1
            val grantsXp = newLogsToday <= XP_DAILY_LOG_CAP

            var newLevel = current.level
            var newXp = current.xp
            if (grantsXp) {
                newXp += XP_PER_LOG
                while (newXp >= xpForLevel(newLevel)) {
                    newXp -= xpForLevel(newLevel)
                    newLevel++
                }
            }

            database.userProgressDao().upsert(
                current.copy(
                    level = newLevel,
                    xp = newXp,
                    currentStreak = newStreak,
                    longestStreak = maxOf(current.longestStreak, newStreak),
                    lastLoggedEpochDay = today,
                    logsToday = newLogsToday
                )
            )
        }
    }

    // Called once from BudgetRepository whenever a budget is set — never cleared by
    // clearBudget(), so the PLANNER achievement stays earned even after the budget
    // itself is later removed.
    suspend fun markBudgetEverSet() {
        database.withTransaction {
            val current = database.userProgressDao().get() ?: UserProgress()
            if (!current.hasSetBudget) {
                database.userProgressDao().upsert(current.copy(hasSetBudget = true))
            }
        }
    }

    suspend fun clear() {
        database.userProgressDao().deleteAll()
    }

    companion object {
        private const val XP_PER_LOG = 15
        // Generous enough for genuine heavy use, low enough to rule out spamming
        // hundreds of trivial entries in one sitting for unlimited XP.
        private const val XP_DAILY_LOG_CAP = 20
        fun xpForLevel(level: Int) = level * 100
    }
}
