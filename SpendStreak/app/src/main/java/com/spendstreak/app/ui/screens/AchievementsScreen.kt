package com.spendstreak.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spendstreak.app.data.Achievement
import com.spendstreak.app.data.UserProgress
import com.spendstreak.app.ui.components.RetroPanel
import com.spendstreak.app.ui.components.RetroProgressBar

@Composable
fun AchievementsScreen(
    progress: UserProgress,
    achievements: List<Achievement>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "LEVEL & ACHIEVEMENTS", style = MaterialTheme.typography.headlineMedium)

        LevelSummaryPanel(
            level = progress.level,
            xpIntoLevel = progress.xp,
            xpForNextLevel = progress.level * 100
        )

        Text(text = "ACHIEVEMENTS", style = MaterialTheme.typography.labelLarge)

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(achievements) { achievement ->
                AchievementBadge(achievement)
            }
        }
    }
}

@Composable
private fun LevelSummaryPanel(level: Int, xpIntoLevel: Int, xpForNextLevel: Int) {
    RetroPanel(
        modifier = Modifier.fillMaxWidth(),
        borderColor = MaterialTheme.colorScheme.primary
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "LEVEL $level",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(text = "$xpIntoLevel / $xpForNextLevel XP", style = MaterialTheme.typography.bodyMedium)
        }
        RetroProgressBar(
            progress = xpIntoLevel.toFloat() / xpForNextLevel.toFloat(),
            segments = 20,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        )
    }
}

@Composable
private fun AchievementBadge(achievement: Achievement) {
    val targetColor = if (achievement.unlocked) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    }
    val emphasisColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 500)
    )
    RetroPanel(
        modifier = Modifier.fillMaxWidth(),
        borderColor = emphasisColor
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = emphasisColor
        )
        Text(
            text = achievement.name,
            style = MaterialTheme.typography.labelLarge,
            color = if (achievement.unlocked) MaterialTheme.colorScheme.onSurface else emphasisColor,
            modifier = Modifier.padding(top = 6.dp)
        )
        Text(
            text = achievement.description,
            style = MaterialTheme.typography.bodySmall,
            color = if (achievement.unlocked) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            },
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
