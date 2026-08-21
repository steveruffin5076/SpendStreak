package com.spendstreak.app.util

import android.content.Context

// Which currency (an ISO 4217 code, e.g. "MYR") is currently selected. Kept in
// SharedPreferences, not Room — local device UI state, not user data, same reasoning
// as ui/theme/ThemePreferences.kt's selected theme skin.
private const val KEY_CURRENCY_CODE = "currency_code"

fun loadCurrencyCode(context: Context): String {
    val prefs = context.applicationContext.getSharedPreferences(SPENDSTREAK_PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getString(KEY_CURRENCY_CODE, null) ?: DEFAULT_CURRENCY_CODE
}

fun saveCurrencyCode(context: Context, code: String) {
    context.applicationContext.getSharedPreferences(SPENDSTREAK_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_CURRENCY_CODE, code)
        .apply()
}
