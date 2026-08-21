package com.spendstreak.app.ui.theme

import android.content.Context
import com.spendstreak.app.util.SPENDSTREAK_PREFS_NAME

// Which unlocked theme skin is currently selected. Deliberately kept in SharedPreferences
// rather than Room — this is local device UI state, not user data, so it has no dependency
// on the app's Room migration story and can ship independently of any schema change.
private const val KEY_SELECTED_THEME = "selected_theme"

fun loadSelectedTheme(context: Context): ThemeOption {
    val prefs = context.applicationContext.getSharedPreferences(SPENDSTREAK_PREFS_NAME, Context.MODE_PRIVATE)
    val name = prefs.getString(KEY_SELECTED_THEME, null) ?: return ThemeOption.RETRO_GOLD
    return ThemeOption.entries.find { it.name == name } ?: ThemeOption.RETRO_GOLD
}

fun saveSelectedTheme(context: Context, theme: ThemeOption) {
    context.applicationContext.getSharedPreferences(SPENDSTREAK_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_SELECTED_THEME, theme.name)
        .apply()
}
