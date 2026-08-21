package com.spendstreak.app.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration

private const val UNIQUE_WORK_NAME = "recurring_reminder_check"

// KEEP is what makes this safe to call on every app launch — if the daily check is
// already scheduled, this is a no-op rather than restarting the schedule's clock. The
// flip side: KEEP also means a rule added/edited right after that daily check already
// ran won't get looked at again until the next tick, up to 24h later — see
// runReminderCheckNow() below for the fix.
fun scheduleReminderChecks(context: Context) {
    val request = PeriodicWorkRequestBuilder<ReminderWorker>(Duration.ofDays(1)).build()
    WorkManager.getInstance(context)
        .enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
}

fun cancelReminderChecks(context: Context) {
    WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
}

// Fires a one-off due-check right away, independent of the daily periodic job's own
// schedule. Call this whenever a recurring rule is added or edited — without it, a rule
// due today created right after the daily check already ran that day would otherwise
// sit silently until the next day's tick, which reads as "reminders don't work" even
// though the feature is enabled.
fun runReminderCheckNow(context: Context) {
    WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<ReminderWorker>().build())
}
