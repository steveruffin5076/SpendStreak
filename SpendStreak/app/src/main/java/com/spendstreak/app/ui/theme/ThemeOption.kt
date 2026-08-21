package com.spendstreak.app.ui.theme

// Level-up cosmetic reward. Every option here changes appearance only — never a real feature
// (export, categories, reports, etc. all stay free at every level) — so this can't become the
// same "feature held hostage" complaint the app's Play Store research flagged, just re-gated
// by grind instead of by payment.
enum class ThemeOption(val displayName: String, val unlockLevel: Int) {
    RETRO_GOLD("Retro Gold", unlockLevel = 1),
    CYBER_TEAL("Cyber Teal", unlockLevel = 3),
    SUNSET_ARCADE("Sunset Arcade", unlockLevel = 5),
    NEON_GRID("Neon Grid", unlockLevel = 10)
}

// Derived live from level, never stored — same "derive, don't store" shape as
// Achievements.compute(), so this stays consistent with the "only grows" invariant (level
// never decreases, so a theme once unlocked is never re-locked).
fun unlockedThemesForLevel(level: Int): List<ThemeOption> =
    ThemeOption.entries.filter { level >= it.unlockLevel }
