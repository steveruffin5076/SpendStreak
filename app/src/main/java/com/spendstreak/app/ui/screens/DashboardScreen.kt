package com.spendstreak.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spendstreak.app.data.UserProgress
import com.spendstreak.app.data.titleForLevel
import com.spendstreak.app.ui.components.RetroPanel
import com.spendstreak.app.ui.components.RetroProgressBar
import com.spendstreak.app.util.formatAmount
import com.spendstreak.app.util.formatCurrency
import com.spendstreak.app.viewmodel.BudgetProgress
import com.spendstreak.app.viewmodel.WeeklySummary
import kotlinx.coroutines.delay

private const val LEVEL_UP_BANNER_MILLIS = 2500L

@Composable
fun DashboardScreen(
    progress: UserProgress,
    weeklySummary: WeeklySummary,
    budgetProgress: BudgetProgress?,
    balance: Double,
    pendingLevelUp: Int?,
    onLevelUpAcknowledged: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(pendingLevelUp) {
        if (pendingLevelUp != null) {
            delay(LEVEL_UP_BANNER_MILLIS)
            onLevelUpAcknowledged()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "DASHBOARD", style = MaterialTheme.typography.headlineMedium)
            // Cosmetic-only level-up reward — flavor text, no mechanical effect.
            titleForLevel(progress.level)?.let { title ->
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            StreakPanel(streakDays = progress.currentStreak)

            LevelPanel(
                level = progress.level,
                xpIntoLevel = progress.xp,
                xpForNextLevel = progress.level * 100
            )

            if (budgetProgress != null) {
                BudgetPanel(budgetProgress)
            }

            BalancePanel(balance)

            ThisWeekPanel(
                expenseCount = weeklySummary.count,
                total = weeklySummary.total
            )
        }

        AnimatedVisibility(
            visible = pendingLevelUp != null,
            enter = fadeIn() + scaleIn(initialScale = 0.7f),
            exit = fadeOut() + scaleOut(targetScale = 0.7f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 90.dp)
        ) {
            LevelUpBanner(level = pendingLevelUp ?: progress.level)
        }
    }
}

@Composable
private fun LevelUpBanner(level: Int) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primary)
            .border(2.dp, MaterialTheme.colorScheme.onPrimary)
            .padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "LEVEL UP!",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onPrimary
        )
        Text(
            text = "YOU REACHED LEVEL $level",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun StreakPanel(streakDays: Int) {
    RetroPanel(
        modifier = Modifier.fillMaxWidth(),
        borderColor = MaterialTheme.colorScheme.secondary
    ) {
        Text(text = "CURRENT STREAK", style = MaterialTheme.typography.labelLarge)
        Text(
            text = "$streakDays DAYS",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = "Log an expense or income today to keep it going.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun LevelPanel(level: Int, xpIntoLevel: Int, xpForNextLevel: Int) {
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
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(text = "$xpIntoLevel / $xpForNextLevel XP", style = MaterialTheme.typography.bodyMedium)
        }
        RetroProgressBar(
            progress = xpIntoLevel.toFloat() / xpForNextLevel.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        )
    }
}

@Composable
private fun BudgetPanel(budgetProgress: BudgetProgress) {
    val accentColor = if (budgetProgress.isOverBudget) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
    RetroPanel(
        modifier = Modifier.fillMaxWidth(),
        borderColor = accentColor
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "BUDGET", style = MaterialTheme.typography.labelLarge)
            Text(
                text = "${formatCurrency(budgetProgress.spent)} / ${formatAmount(budgetProgress.limit)}",
                style = MaterialTheme.typography.bodyMedium,
                color = accentColor
            )
        }
        RetroProgressBar(
            progress = (budgetProgress.spent / budgetProgress.limit).toFloat(),
            filledColor = accentColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        )
        if (budgetProgress.isOverBudget) {
            Text(
                text = "Over budget!",
                style = MaterialTheme.typography.bodySmall,
                color = accentColor,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun BalancePanel(balance: Double) {
    val balanceColor = if (balance >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
    RetroPanel(modifier = Modifier.fillMaxWidth()) {
        Text(text = "BALANCE", style = MaterialTheme.typography.labelLarge)
        Text(
            text = formatCurrency(balance),
            style = MaterialTheme.typography.titleMedium,
            color = balanceColor
        )
    }
}

@Composable
private fun ThisWeekPanel(expenseCount: Int, total: Double) {
    RetroPanel(modifier = Modifier.fillMaxWidth()) {
        Text(text = "THIS WEEK", style = MaterialTheme.typography.labelLarge)
        Text(
            text = "$expenseCount expenses logged · ${formatCurrency(total)} total",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
