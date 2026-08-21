package com.spendstreak.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Always dark — a retro RPG HUD doesn't adapt to system light/dark theme.
//
// Material3 components lean on "container" roles (secondaryContainer for selected
// FilterChips, surfaceContainer for NavigationBar, surfaceContainerHigh for AlertDialog,
// etc.) that darkColorScheme() otherwise fills with its own generic baseline tones if left
// unset — so every one of those components would render off-theme. Mapping them onto the
// existing flat Retro colors (rather than inventing new tonal variants) keeps them on-theme
// while staying consistent with the blocky, non-elevated panel look used everywhere else.
// The same mapping is reused for every level-up cosmetic skin below — only the color values
// change, never which roles are filled.
private fun retroScheme(
    background: Color,
    surface: Color,
    surfaceVariant: Color,
    primary: Color,
    primaryOn: Color,
    secondary: Color,
    secondaryOn: Color,
    tertiary: Color,
    tertiaryOn: Color
): ColorScheme = darkColorScheme(
    primary = primary,
    onPrimary = primaryOn,
    primaryContainer = primary,
    onPrimaryContainer = primaryOn,
    secondary = secondary,
    onSecondary = secondaryOn,
    secondaryContainer = secondary,
    onSecondaryContainer = secondaryOn,
    tertiary = tertiary,
    onTertiary = tertiaryOn,
    tertiaryContainer = tertiary,
    onTertiaryContainer = tertiaryOn,
    background = background,
    onBackground = RetroCream,
    surface = surface,
    onSurface = RetroCream,
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = RetroCream,
    surfaceContainer = surface,
    surfaceContainerHigh = surfaceVariant,
    error = RetroRed,
    onError = RetroCream,
    errorContainer = RetroRed,
    onErrorContainer = RetroCream,
    outline = RetroCream
)

private val RetroColorScheme = retroScheme(
    background = RetroBackground,
    surface = RetroSurface,
    surfaceVariant = RetroSurfaceVariant,
    primary = RetroGold,
    primaryOn = RetroGoldOn,
    secondary = RetroOrange,
    secondaryOn = RetroOrangeOn,
    tertiary = RetroGreen,
    tertiaryOn = RetroGreenOn
)

private val CyberTealColorScheme = retroScheme(
    background = CyberTealBackground,
    surface = CyberTealSurface,
    surfaceVariant = CyberTealSurfaceVariant,
    primary = CyberTealPrimary,
    primaryOn = CyberTealPrimaryOn,
    secondary = CyberTealSecondary,
    secondaryOn = CyberTealSecondaryOn,
    tertiary = CyberTealTertiary,
    tertiaryOn = CyberTealTertiaryOn
)

private val SunsetArcadeColorScheme = retroScheme(
    background = SunsetArcadeBackground,
    surface = SunsetArcadeSurface,
    surfaceVariant = SunsetArcadeSurfaceVariant,
    primary = SunsetArcadePrimary,
    primaryOn = SunsetArcadePrimaryOn,
    secondary = SunsetArcadeSecondary,
    secondaryOn = SunsetArcadeSecondaryOn,
    tertiary = SunsetArcadeTertiary,
    tertiaryOn = SunsetArcadeTertiaryOn
)

private val NeonGridColorScheme = retroScheme(
    background = NeonGridBackground,
    surface = NeonGridSurface,
    surfaceVariant = NeonGridSurfaceVariant,
    primary = NeonGridPrimary,
    primaryOn = NeonGridPrimaryOn,
    secondary = NeonGridSecondary,
    secondaryOn = NeonGridSecondaryOn,
    tertiary = NeonGridTertiary,
    tertiaryOn = NeonGridTertiaryOn
)

private fun colorSchemeFor(themeOption: ThemeOption): ColorScheme = when (themeOption) {
    ThemeOption.RETRO_GOLD -> RetroColorScheme
    ThemeOption.CYBER_TEAL -> CyberTealColorScheme
    ThemeOption.SUNSET_ARCADE -> SunsetArcadeColorScheme
    ThemeOption.NEON_GRID -> NeonGridColorScheme
}

// Low corner radius everywhere for a blocky, chunky-panel look.
private val RetroShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(4.dp),
    extraLarge = RoundedCornerShape(4.dp)
)

@Composable
fun SpendStreakTheme(
    themeOption: ThemeOption = ThemeOption.RETRO_GOLD,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = colorSchemeFor(themeOption),
        typography = Typography,
        shapes = RetroShapes,
        content = content
    )
}
