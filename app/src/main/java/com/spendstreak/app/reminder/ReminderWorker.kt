package com.spendstreak.app.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.spendstreak.app.R
import com.spendstreak.app.data.RecurringTransaction
import com.spendstreak.app.data.RecurringTransactionRepository
import com.spendstreak.app.data.RecurringTransactionType
import com.spendstreak.app.data.SpendStreakDatabase
import com.spendstreak.app.util.currencyOptionFor
import com.spendstreak.app.util.formatAmount
import com.spendstreak.app.util.loadCurrencyCode
import java.time.LocalDate

private const val CHANNEL_ID = "recurring_reminders"

// Runs once a day (see ReminderScheduler) and posts one notification per due recurring
// rule. Never inserts a transaction itself — see ReminderActionReceiver for why "one-tap
// confirmation" is a deliberate design choice, not an oversight.
class ReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val database = SpendStreakDatabase.getInstance(applicationContext)
        val recurringRepository = RecurringTransactionRepository(database)
        val today = LocalDate.now().toEpochDay()
        val due = recurringRepository.getDue(today)

        if (due.isNotEmpty() && hasNotificationPermission(applicationContext)) {
            ensureNotificationChannel(applicationContext)
            due.forEach { showDueNotification(applicationContext, it) }
        }
        return Result.success()
    }
}

private fun hasNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}

private fun ensureNotificationChannel(context: Context) {
    val channel = NotificationChannel(
        CHANNEL_ID,
        "Recurring bill reminders",
        NotificationManager.IMPORTANCE_DEFAULT
    )
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}

private fun showDueNotification(context: Context, recurring: RecurringTransaction) {
    val logIntent = Intent(context, ReminderActionReceiver::class.java).apply {
        action = ReminderActionReceiver.ACTION_LOG
        putExtra(ReminderActionReceiver.EXTRA_RECURRING_ID, recurring.id)
    }
    val skipIntent = Intent(context, ReminderActionReceiver::class.java).apply {
        action = ReminderActionReceiver.ACTION_SKIP
        putExtra(ReminderActionReceiver.EXTRA_RECURRING_ID, recurring.id)
    }
    val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    val logPendingIntent = PendingIntent.getBroadcast(
        context, ReminderActionReceiver.requestCodeFor(recurring.id, isLog = true), logIntent, flags
    )
    val skipPendingIntent = PendingIntent.getBroadcast(
        context, ReminderActionReceiver.requestCodeFor(recurring.id, isLog = false), skipIntent, flags
    )

    val currencyCode = loadCurrencyCode(context)
    val symbol = currencyOptionFor(currencyCode).symbol
    val amountText = "$symbol ${formatAmount(recurring.amount)}"
    val typeLabel = if (recurring.type == RecurringTransactionType.EXPENSE) "expense" else "income"

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("Recurring $typeLabel due: $amountText")
        .setContentText(recurring.note.ifBlank { "Tap LOG IT to record it" })
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .addAction(0, "LOG IT", logPendingIntent)
        .addAction(0, "SKIP", skipPendingIntent)
        .setAutoCancel(true)
        .build()

    NotificationManagerCompat.from(context).notify(ReminderActionReceiver.notificationIdFor(recurring.id), notification)
}
