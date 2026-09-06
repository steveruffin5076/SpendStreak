package com.spendstreak.app.ui.theme

// Independent of which ThemeOption skin is selected (see ThemeOption.kt) — this only
// decides whether that skin renders as its light or dark variant. SYSTEM follows the
// device's own light/dark setting; LIGHT/DARK pin it regardless of the device setting.
enum class ThemeMode { LIGHT, DARK, SYSTEM }
