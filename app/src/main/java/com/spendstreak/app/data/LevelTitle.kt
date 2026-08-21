package com.spendstreak.app.data

// Cosmetic-only, level-up reward — flavor text shown on the Dashboard header, no mechanical
// effect. Derived live from level (never stored), same "derive, don't store" shape as
// Achievements.compute(), so a title once shown is never taken away (level only ever grows).
fun titleForLevel(level: Int): String? = when {
    level >= 20 -> "Budget Sensei"
    level >= 15 -> "Money Marshal"
    else -> null
}
