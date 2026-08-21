package com.spendstreak.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.spendstreak.app.data.ExpenseRepository
import com.spendstreak.app.data.IncomeRepository
import com.spendstreak.app.data.RecurringTransactionRepository
import com.spendstreak.app.data.RecurringTransactionType
import com.spendstreak.app.data.SpendStreakDatabase
import com.spendstreak.app.data.UserProgressRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Handles the LOG IT / SKIP action buttons on a recurring-reminder notification (see
// ReminderWorker). This is the "one-tap confirmation" the app's design deliberately
// requires — a recurring rule never silently inserts a transaction on its own; only an
// explicit tap here does, so streak/XP still reflect a genuine user action rather than a
// background job quietly inflating them.
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val recurringId = intent.getLongExtra(EXTRA_RECURRING_ID, -1L)
        val action = intent.action
        if (recurringId == -1L) return

        // A BroadcastReceiver's process can be killed the moment onReceive() returns —
        // goAsync() + pendingResult.finish() is what keeps it alive long enough for the
        // suspend work below to actually complete.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                val database = SpendStreakDatabase.getInstance(appContext)
                val recurringRepository = RecurringTransactionRepository(database)
                val recurring = recurringRepository.getById(recurringId)
                if (recurring != null) {
                    if (action == ACTION_LOG) {
                        val userProgressRepository = UserProgressRepository(database)
                        if (recurring.type == RecurringTransactionType.EXPENSE) {
                            ExpenseRepository(database, userProgressRepository)
                                .addExpense(recurring.amount, recurring.categoryId, recurring.accountId, recurring.note)
                        } else {
                            IncomeRepository(database, userProgressRepository)
                                .addIncome(recurring.amount, recurring.categoryId, recurring.accountId, recurring.note)
                        }
                    }
                    // Both LOG IT and SKIP advance the schedule — either way this occurrence
                    // is resolved, so it shouldn't keep nagging every day until acted on.
                    recurringRepository.advanceToNextPeriod(recurringId)
                }
                NotificationManagerCompat.from(appContext).cancel(notificationIdFor(recurringId))
            } catch (_: Exception) {
                // A tap on LOG IT/SKIP must never crash the app process — this coroutine
                // has no CoroutineExceptionHandler, so an uncaught exception here (e.g. a
                // transient SQLite busy/locked error) would otherwise propagate to the
                // thread's default handler. Mirrors the try/catch already used around
                // risky writes in SpendStreakViewModel.clearAllData().
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_LOG = "com.spendstreak.app.reminder.ACTION_LOG"
        const val ACTION_SKIP = "com.spendstreak.app.reminder.ACTION_SKIP"
        const val EXTRA_RECURRING_ID = "recurring_id"

        // Long.hashCode() folds all 64 bits of the id (high xor low 32 bits) rather than
        // truncating/doubling it, so a collision needs a genuine hash collision instead
        // of a simple `id * 2` overflow once the recurring_transactions autoincrement
        // counter passes ~1.07 billion rows. Shared by ReminderWorker (which posts the
        // notification and builds its PendingIntents) and this receiver's cancel() call,
        // so both sides always agree on the same id for the same recurring rule.
        fun notificationIdFor(recurringId: Long): Int = recurringId.hashCode()

        fun requestCodeFor(recurringId: Long, isLog: Boolean): Int {
            val base = recurringId.hashCode()
            return if (isLog) base else base xor 1
        }
    }
}
