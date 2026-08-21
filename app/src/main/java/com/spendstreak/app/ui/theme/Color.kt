package com.spendstreak.app.ui.theme

import androidx.compose.ui.graphics.Color

// Retro RPG palette — deep indigo night-sky background with bold saturated accents.
val RetroBackground = Color(0xFF14122B)
val RetroSurface = Color(0xFF211F45)
val RetroSurfaceVariant = Color(0xFF2C2A5A)
val RetroCream = Color(0xFFF4F1E8)

val RetroGold = Color(0xFFFFC93C)
val RetroGoldOn = Color(0xFF1A1330)

val RetroOrange = Color(0xFFFF6B4A)
val RetroOrangeOn = Color(0xFF1A1330)

val RetroGreen = Color(0xFF5CDB6B)
val RetroGreenOn = Color(0xFF1A1330)

val RetroRed = Color(0xFFFF4757)

// Used only for Transfer rows in History — distinct from every category/income color so
// a transfer never renders identically to an "Other"/"Entertainment" expense (both of
// which fall back to the neutral cream-on-dark tone elsewhere).
val RetroBlue = Color(0xFF4FA8FF)

// Level-up cosmetic unlocks (see ThemeOption.kt) — each is a full alternate accent/background
// set, never a functional change. RetroRed and RetroCream are deliberately reused across every
// variant below so error states and body text stay equally legible regardless of skin.

// Unlocked at level 3.
val CyberTealBackground = Color(0xFF0B1F1D)
val CyberTealSurface = Color(0xFF123330)
val CyberTealSurfaceVariant = Color(0xFF1B4741)
val CyberTealPrimary = Color(0xFF2EE6C7)
val CyberTealPrimaryOn = Color(0xFF04211D)
val CyberTealSecondary = Color(0xFFFF4FA3)
val CyberTealSecondaryOn = Color(0xFF2B0716)
val CyberTealTertiary = Color(0xFFB6FF3C)
val CyberTealTertiaryOn = Color(0xFF1A2600)

// Unlocked at level 5.
val SunsetArcadeBackground = Color(0xFF2B0F1E)
val SunsetArcadeSurface = Color(0xFF3D1730)
val SunsetArcadeSurfaceVariant = Color(0xFF4E1F3D)
val SunsetArcadePrimary = Color(0xFFFF4D8D)
val SunsetArcadePrimaryOn = Color(0xFF2B0714)
val SunsetArcadeSecondary = Color(0xFFFFA53C)
val SunsetArcadeSecondaryOn = Color(0xFF2E1800)
val SunsetArcadeTertiary = Color(0xFF3CD8C7)
val SunsetArcadeTertiaryOn = Color(0xFF002824)

// Unlocked at level 10.
val NeonGridBackground = Color(0xFF0A0A1F)
val NeonGridSurface = Color(0xFF14142E)
val NeonGridSurfaceVariant = Color(0xFF1E1E44)
val NeonGridPrimary = Color(0xFFB24BFF)
val NeonGridPrimaryOn = Color(0xFF1A0733)
val NeonGridSecondary = Color(0xFF3CF0FF)
val NeonGridSecondaryOn = Color(0xFF002E33)
val NeonGridTertiary = Color(0xFF5CFF7A)
val NeonGridTertiaryOn = Color(0xFF003309)
