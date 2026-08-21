package com.spendstreak.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spendstreak.app.data.AccountRepository
import com.spendstreak.app.data.BudgetRepository
import com.spendstreak.app.data.CategoryRepository
import com.spendstreak.app.data.ExpenseRepository
import com.spendstreak.app.data.IncomeRepository
import com.spendstreak.app.data.RecurringTransactionRepository
import com.spendstreak.app.data.SpendStreakDatabase
import com.spendstreak.app.data.TransferRepository
import com.spendstreak.app.data.UserProgressRepository
import com.spendstreak.app.export.exportAndShareCsv
import com.spendstreak.app.reminder.cancelReminderChecks
import com.spendstreak.app.reminder.loadRemindersEnabled
import com.spendstreak.app.reminder.runReminderCheckNow
import com.spendstreak.app.reminder.saveRemindersEnabled
import com.spendstreak.app.reminder.scheduleReminderChecks
import com.spendstreak.app.ui.navigation.AppScreen
import com.spendstreak.app.ui.screens.AccountsScreen
import com.spendstreak.app.ui.screens.AchievementsScreen
import com.spendstreak.app.ui.screens.AddTransactionScreen
import com.spendstreak.app.ui.screens.BudgetScreen
import com.spendstreak.app.ui.screens.DashboardScreen
import com.spendstreak.app.ui.screens.HistoryScreen
import com.spendstreak.app.ui.screens.ReportsScreen
import com.spendstreak.app.ui.screens.RecurringTransactionsScreen
import com.spendstreak.app.ui.screens.SettingsScreen
import com.spendstreak.app.ui.theme.SpendStreakTheme
import com.spendstreak.app.ui.theme.loadSelectedTheme
import com.spendstreak.app.ui.theme.saveSelectedTheme
import com.spendstreak.app.ui.theme.unlockedThemesForLevel
import com.spendstreak.app.util.LocalCurrencyCode
import com.spendstreak.app.util.loadCurrencyCode
import com.spendstreak.app.util.saveCurrencyCode
import com.spendstreak.app.viewmodel.SpendStreakViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Theming lives inside SpendStreakApp (not wrapped here) because the selected
            // theme is a level-up cosmetic unlock — it depends on the ViewModel's progress
            // state, which is only available once SpendStreakApp has created its ViewModel.
            SpendStreakApp()
        }
    }
}

private enum class SettingsSubScreen { Accounts, Budget, Reports, Recurring }

@Composable
fun SpendStreakApp() {
    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.Dashboard) }
    var settingsSubScreen by rememberSaveable { mutableStateOf<SettingsSubScreen?>(null) }

    // No back-stack (this app uses a hand-rolled screen switch, not Navigation Compose),
    // so without this, system back from a Settings sub-screen falls through and exits
    // the app instead of returning to the Settings root.
    BackHandler(enabled = settingsSubScreen != null) {
        settingsSubScreen = null
    }

    val appContext = LocalContext.current.applicationContext
    val coroutineScope = rememberCoroutineScope()
    val database = remember { SpendStreakDatabase.getInstance(appContext) }
    val userProgressRepository = remember { UserProgressRepository(database) }
    val expenseRepository = remember { ExpenseRepository(database, userProgressRepository) }
    val incomeRepository = remember { IncomeRepository(database, userProgressRepository) }
    val accountRepository = remember { AccountRepository(database) }
    val budgetRepository = remember { BudgetRepository(database, userProgressRepository) }
    val transferRepository = remember { TransferRepository(database) }
    val categoryRepository = remember { CategoryRepository(database) }
    val recurringTransactionRepository = remember { RecurringTransactionRepository(database) }
    val viewModel: SpendStreakViewModel = viewModel(
        factory = SpendStreakViewModel.factory(
            database,
            expenseRepository,
            incomeRepository,
            accountRepository,
            budgetRepository,
            transferRepository,
            userProgressRepository,
            categoryRepository,
            recurringTransactionRepository
        )
    )

    var showDataResetNotice by remember { mutableStateOf(SpendStreakDatabase.dataWasResetOnLaunch) }
    var selectedTheme by remember { mutableStateOf(loadSelectedTheme(appContext)) }
    var selectedCurrencyCode by remember { mutableStateOf(loadCurrencyCode(appContext)) }
    var remindersEnabled by remember { mutableStateOf(loadRemindersEnabled(appContext)) }

    // Re-arms the periodic job on every launch when the preference is already on, since
    // enqueueUniquePeriodicWork's KEEP policy makes this a no-op if it's already scheduled
    // — without this, a Force-Stop (which cancels all pending WorkManager work) leaves the
    // toggle showing ON while nothing is actually scheduled anymore.
    LaunchedEffect(Unit) {
        if (remindersEnabled) {
            scheduleReminderChecks(appContext)
        }
    }

    // Permission result is ignored here on purpose — ReminderWorker checks the permission
    // itself before ever posting a notification, so whether the user grants or denies it,
    // the app degrades gracefully (reminders just silently don't show) instead of crashing
    // or needing to branch on the result here too.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    fun onToggleReminders(enabled: Boolean) {
        remindersEnabled = enabled
        saveRemindersEnabled(appContext, enabled)
        if (enabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            scheduleReminderChecks(appContext)
        } else {
            cancelReminderChecks(appContext)
        }
    }

    val progress by viewModel.progress.collectAsState()
    val weeklySummary by viewModel.weeklySummary.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val income by viewModel.income.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val recurringTransactions by viewModel.recurringTransactions.collectAsState()
    val accountBalances by viewModel.accountBalances.collectAsState()
    val budget by viewModel.budget.collectAsState()
    val budgetHistory by viewModel.budgetHistory.collectAsState()
    val budgetProgress by viewModel.budgetProgress.collectAsState()
    val balance by viewModel.balance.collectAsState()
    val historyEntries by viewModel.historyEntries.collectAsState()
    val achievements by viewModel.achievements.collectAsState()
    val pendingLevelUp by viewModel.pendingLevelUp.collectAsState()
    val unlockedThemes = unlockedThemesForLevel(progress.level)

    CompositionLocalProvider(LocalCurrencyCode provides selectedCurrencyCode) {
    SpendStreakTheme(themeOption = selectedTheme) {
    if (showDataResetNotice) {
        AlertDialog(
            onDismissRequest = { showDataResetNotice = false },
            confirmButton = {
                TextButton(onClick = { showDataResetNotice = false }) { Text("OK") }
            },
            title = { Text("Data reset") },
            text = {
                Text(
                    "This update required a one-time database reset, so your previous " +
                        "expenses, streak, and level were cleared. Sorry about that — " +
                        "everything you log from here on will carry forward normally."
                )
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                AppScreen.entries.forEach { screen ->
                    NavigationBarItem(
                        selected = currentScreen == screen,
                        onClick = {
                            currentScreen = screen
                            settingsSubScreen = null
                        },
                        icon = { Icon(imageVector = screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (currentScreen) {
            AppScreen.Dashboard -> DashboardScreen(
                modifier = contentModifier,
                progress = progress,
                weeklySummary = weeklySummary,
                budgetProgress = budgetProgress,
                balance = balance,
                pendingLevelUp = pendingLevelUp,
                onLevelUpAcknowledged = { viewModel.acknowledgeLevelUp() }
            )
            AppScreen.AddExpense -> AddTransactionScreen(
                modifier = contentModifier,
                accounts = accounts,
                categories = categories,
                onSaveExpense = { amount, categoryId, accountId, note ->
                    viewModel.addExpense(amount, categoryId, accountId, note)
                },
                onSaveIncome = { amount, categoryId, accountId, note ->
                    viewModel.addIncome(amount, categoryId, accountId, note)
                },
                onSaveTransfer = { amount, fromAccountId, toAccountId, note ->
                    viewModel.addTransfer(
                        fromAccountId = fromAccountId,
                        toAccountId = toAccountId,
                        amount = amount,
                        note = note
                    )
                },
                onAddCategory = { name, kind, emoji -> viewModel.addCategory(name, kind, emoji) },
                onRenameCategory = { category, name, emoji ->
                    viewModel.updateCategory(category.copy(name = name, emoji = emoji))
                },
                onDeleteCategory = { categoryId, onResult -> viewModel.deleteCategory(categoryId, onResult) }
            )
            AppScreen.History -> HistoryScreen(
                modifier = contentModifier,
                entries = historyEntries,
                accounts = accounts,
                categories = categories,
                onUpdateExpense = { viewModel.updateExpense(it) },
                onDeleteExpense = { id, onResult -> viewModel.deleteExpense(id, onResult) },
                onUpdateIncome = { viewModel.updateIncome(it) },
                onDeleteIncome = { id, onResult -> viewModel.deleteIncome(id, onResult) },
                onUpdateTransfer = { viewModel.updateTransfer(it) },
                onDeleteTransfer = { id, onResult -> viewModel.deleteTransfer(id, onResult) }
            )
            AppScreen.Achievements -> AchievementsScreen(
                modifier = contentModifier,
                progress = progress,
                achievements = achievements
            )
            AppScreen.Settings -> when (settingsSubScreen) {
                SettingsSubScreen.Accounts -> AccountsScreen(
                    modifier = contentModifier,
                    accounts = accounts,
                    accountBalances = accountBalances,
                    onAddAccount = { name, type -> viewModel.addAccount(name, type) },
                    onDeleteAccount = { accountId, onResult -> viewModel.deleteAccount(accountId, onResult) },
                    onBack = { settingsSubScreen = null }
                )
                SettingsSubScreen.Budget -> BudgetScreen(
                    modifier = contentModifier,
                    budget = budget,
                    budgetHistory = budgetHistory,
                    budgetProgress = budgetProgress,
                    onSetMonthlyBudget = { name, amount -> viewModel.setMonthlyBudget(name, amount) },
                    onSetCustomBudget = { name, amount, start, end -> viewModel.setCustomBudget(name, amount, start, end) },
                    onUpdateBudget = { viewModel.updateBudget(it) },
                    onDeleteBudget = { viewModel.deleteBudget(it) },
                    onClearBudget = { viewModel.clearBudget() },
                    onBack = { settingsSubScreen = null }
                )
                SettingsSubScreen.Reports -> ReportsScreen(
                    modifier = contentModifier,
                    expenses = expenses,
                    income = income,
                    categories = categories,
                    onBack = { settingsSubScreen = null }
                )
                SettingsSubScreen.Recurring -> RecurringTransactionsScreen(
                    modifier = contentModifier,
                    recurringTransactions = recurringTransactions,
                    accounts = accounts,
                    categories = categories,
                    onAdd = {
                        viewModel.addRecurring(it)
                        if (remindersEnabled) runReminderCheckNow(appContext)
                    },
                    onUpdate = {
                        viewModel.updateRecurring(it)
                        if (remindersEnabled) runReminderCheckNow(appContext)
                    },
                    onDelete = { viewModel.deleteRecurring(it) },
                    onBack = { settingsSubScreen = null }
                )
                null -> SettingsScreen(
                    modifier = contentModifier,
                    onClearData = { onResult -> viewModel.clearAllData(onResult) },
                    onManageAccounts = { settingsSubScreen = SettingsSubScreen.Accounts },
                    onManageBudget = { settingsSubScreen = SettingsSubScreen.Budget },
                    onViewReports = { settingsSubScreen = SettingsSubScreen.Reports },
                    onManageRecurring = { settingsSubScreen = SettingsSubScreen.Recurring },
                    remindersEnabled = remindersEnabled,
                    onToggleReminders = { onToggleReminders(it) },
                    unlockedThemes = unlockedThemes,
                    selectedTheme = selectedTheme,
                    onSelectTheme = { theme ->
                        selectedTheme = theme
                        saveSelectedTheme(appContext, theme)
                    },
                    currencyCode = selectedCurrencyCode,
                    onSelectCurrency = { code ->
                        selectedCurrencyCode = code
                        saveCurrencyCode(appContext, code)
                    },
                    onExportData = {
                        coroutineScope.launch { exportAndShareCsv(appContext, historyEntries) }
                    }
                )
            }
        }
    }
    }
    }
}
