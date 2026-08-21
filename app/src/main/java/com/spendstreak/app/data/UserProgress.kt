package com.spendstreak.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// Singleton row — id is always 0, there's only ever one progress record.
@Entity(tableName = "user_progress")
data class UserProgress(
    @PrimaryKey val id: Int = 0,
    val level: Int = 1,
    val xp: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastLoggedEpochDay: Long? = null,
    // Set once, never cleared — even if the user later clears their budget — so the
    // PLANNER achievement can't be un-earned. Budget itself has no history to check.
    val hasSetBudget: Boolean = false,
    // How many logs have already granted XP today — resets to 0 whenever lastLoggedEpochDay
    // changes. Exists purely to cap XP-per-day (see UserProgressRepository.XP_DAILY_LOG_CAP);
    // spamming trivial entries in one sitting no longer yields unlimited XP.
    val logsToday: Int = 0
)
