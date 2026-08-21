package com.spendstreak.app.reminder

import android.content.Context
import com.spendstreak.app.util.SPENDSTREAK_PREFS_NAME

// Whether the user has turned on recurring-bill reminders. Kept in SharedPreferences, not
// Room — local device UI state, not user data, same reasoning as the theme/currency prefs.
private const val KEY_REMINDERS_ENABLED = "reminders_enabled"

fun loadRemindersEnabled(context: Context): Boolean {
    val prefs = context.applicationContext.getSharedPreferences(SPENDSTREAK_PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_REMINDERS_ENABLED, false)
}

fun saveRemindersEnabled(context: Context, enabled: Boolean) {
    context.applicationContext.getSharedPreferences(SPENDSTREAK_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_REMINDERS_ENABLED, enabled)
        .apply()
}
