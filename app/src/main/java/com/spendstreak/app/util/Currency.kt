package com.spendstreak.app.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import java.util.Locale
import java.util.Currency as JavaCurrency

// Always uses '.' as the decimal separator regardless of device locale, so displayed
// amounts always match what every AMOUNT_PATTERN regex in the UI accepts when typed back in.
fun formatAmount(value: Double): String = String.format(Locale.US, "%.2f", value)

const val DEFAULT_CURRENCY_CODE = "MYR"

data class CurrencyOption(val code: String, val symbol: String, val displayName: String)

// Backed by the JDK's own ISO 4217 currency table — every world currency, no new
// dependency and no hand-maintained list to go stale.
fun allWorldCurrencies(): List<CurrencyOption> =
    JavaCurrency.getAvailableCurrencies()
        .map { CurrencyOption(it.currencyCode, it.getSymbol(Locale.US), it.displayName) }
        .sortedBy { it.code }

fun currencyOptionFor(code: String): CurrencyOption {
    val currency = runCatching { JavaCurrency.getInstance(code) }.getOrNull()
        ?: JavaCurrency.getInstance(DEFAULT_CURRENCY_CODE)
    return CurrencyOption(currency.currencyCode, currency.getSymbol(Locale.US), currency.displayName)
}

// Provided once near the app root (see MainActivity's SpendStreakApp) so every screen's
// formatCurrency()/currentCurrencySymbol() call picks up a currency change immediately,
// the same way MaterialTheme's colorScheme change already propagates for level-up skins.
val LocalCurrencyCode = compositionLocalOf { DEFAULT_CURRENCY_CODE }

@Composable
fun currentCurrencySymbol(): String = currencyOptionFor(LocalCurrencyCode.current).symbol

@Composable
fun formatCurrency(value: Double): String = "${currentCurrencySymbol()} ${formatAmount(value)}"
