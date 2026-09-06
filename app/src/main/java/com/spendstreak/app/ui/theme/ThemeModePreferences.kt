package com.spendstreak.app.ui.theme

import android.content.Context
import com.spendstreak.app.util.SPENDSTREAK_PREFS_NAME

// Same local-device-UI-state reasoning as ThemePreferences.kt (SharedPreferences, not
// Room). Defaults to DARK — the app was always-dark before this setting existed, so an
// existing install's look doesn't change until the user opts into LIGHT or SYSTEM.
private const val KEY_THEME_MODE = "theme_mode"

fun loadThemeMode(context: Context): ThemeMode {
    val prefs = context.applicationContext.getSharedPreferences(SPENDSTREAK_PREFS_NAME, Context.MODE_PRIVATE)
    val name = prefs.getString(KEY_THEME_MODE, null) ?: return ThemeMode.DARK
    return ThemeMode.entries.find { it.name == name } ?: ThemeMode.DARK
}

fun saveThemeMode(context: Context, mode: ThemeMode) {
    context.applicationContext.getSharedPreferences(SPENDSTREAK_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_THEME_MODE, mode.name)
        .apply()
}
