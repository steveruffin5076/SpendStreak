package com.spendstreak.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

// Note: uses only the core Icons.Filled set (from material-icons-core) to avoid
// requiring the separate material-icons-extended dependency.
enum class AppScreen(val label: String, val icon: ImageVector) {
    Dashboard("Dashboard", Icons.Filled.Home),
    AddExpense("Add", Icons.Filled.Add),
    History("History", Icons.AutoMirrored.Filled.List),
    Achievements("Level", Icons.Filled.Star),
    Settings("Settings", Icons.Filled.Settings)
}
