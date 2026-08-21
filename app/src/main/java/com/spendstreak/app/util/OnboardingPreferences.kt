package com.spendstreak.app.util

import android.content.Context

// Whether the one-time first-launch welcome dialog has already been shown. Kept in
// SharedPreferences, not Room — local device UI state, not user data, same reasoning as the
// theme/currency/reminders-enabled prefs.
private const val KEY_HAS_SEEN_WELCOME = "has_seen_welcome"

fun loadHasSeenWelcome(context: Context): Boolean {
    val prefs = context.applicationContext.getSharedPreferences(SPENDSTREAK_PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_HAS_SEEN_WELCOME, false)
}

fun markWelcomeSeen(context: Context) {
    context.applicationContext.getSharedPreferences(SPENDSTREAK_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_HAS_SEEN_WELCOME, true)
        .apply()
}
