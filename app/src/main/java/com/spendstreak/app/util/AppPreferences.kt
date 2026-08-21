package com.spendstreak.app.util

// The single SharedPreferences file name shared by every local-device-state preference
// helper in the app (currency, theme, reminders-enabled, has-launched-before) — a typo'd
// or independently-edited copy of this string would silently split reads/writes into a
// second, disconnected prefs file with no compiler error.
const val SPENDSTREAK_PREFS_NAME = "spendstreak_prefs"
