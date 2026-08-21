package com.spendstreak.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Retro RPG type: bold monospace with wide letter-spacing, layered onto Material3's
// default sizes/line-heights so every component (buttons, chips, fields) stays legible.
private val defaults = Typography()

val Typography = Typography(
    displayLarge = defaults.displayLarge.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black),
    displayMedium = defaults.displayMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black),
    displaySmall = defaults.displaySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, letterSpacing = 1.sp),
    headlineLarge = defaults.headlineLarge.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
    headlineMedium = defaults.headlineMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
    headlineSmall = defaults.headlineSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
    titleLarge = defaults.titleLarge.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
    titleMedium = defaults.titleMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
    titleSmall = defaults.titleSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
    bodyLarge = defaults.bodyLarge.copy(fontFamily = FontFamily.Monospace),
    bodyMedium = defaults.bodyMedium.copy(fontFamily = FontFamily.Monospace),
    bodySmall = defaults.bodySmall.copy(fontFamily = FontFamily.Monospace),
    labelLarge = defaults.labelLarge.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp),
    labelMedium = defaults.labelMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
    labelSmall = defaults.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
)
