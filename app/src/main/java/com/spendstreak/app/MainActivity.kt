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
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.spendstreak.app.data.CategoryBudgetRepository
import com.spendstreak.app.data.CategoryRepository
import com.spendstreak.app.data.ExpenseRepository
import com.spendstreak.app.data.IncomeRepository
import com.spendstreak.app.data.RecurringTransactionRepository
import com.spendstreak.app.data.SpendStreakDatabase
import com.spendstreak.app.data.TransferRepository
import com.spendstreak.app.data.UserProgressRepository
import com.spendstreak.app.export.BackupImportResult
import com.spendstreak.app.export.ImportResult
import com.spendstreak.app.export.exportAndShareBackup
import com.spendstreak.app.export.exportAndShareCsv
import com.spendstreak.app.export.importBackupReplacingAll
import com.spendstreak.app.export.importCsvReplacingAll
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
import com.spendstreak.app.ui.screens.EditOpeningBalancePanel
import com.spendstreak.app.ui.screens.HistoryScreen
import com.spendstreak.app.ui.screens.ReportsScreen
import com.spendstreak.app.ui.screens.RecurringTransactionsScreen
import com.spendstreak.app.ui.screens.SettingsScreen
import com.spendstreak.app.ui.theme.SpendStreakTheme
import com.spendstreak.app.ui.theme.ThemeMode
import com.spendstreak.app.ui.theme.loadSelectedTheme
import com.spendstreak.app.ui.theme.loadThemeMode
import com.spendstreak.app.ui.theme.saveSelectedTheme
import com.spendstreak.app.ui.theme.saveThemeMode
import com.spendstreak.app.ui.theme.unlockedThemesForLevel
import com.spendstreak.app.util.LocalCurrencyCode
import com.spendstreak.app.util.loadCurrencyCode
import com.spendstreak.app.util.loadHasSeenWelcome
import com.spendstreak.app.util.markWelcomeSeen
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

private data class DataResultDialog(
    val title: String,
    val message: String,
    val onRetry: (() -> Unit)?
)

private val CSV_MIME_TYPES = arrayOf("text/*", "text/comma-separated-values", "text/csv")
private val BACKUP_MIME_TYPES = arrayOf("application/json", "text/*")

private enum class DocumentPickerRequest { CsvImport, BackupRestore }

@Composable
fun SpendStreakApp() {
    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.Dashboard) }
    var settingsSubScreen by rememberSaveable { mutableStateOf<SettingsSubScreen?>(null) }
    var viewingAccountId by rememberSaveable { mutableStateOf<Long?>(null) }

    // No back-stack (this app uses a hand-rolled screen switch, not Navigation Compose),
    // so without this, system back from a Settings sub-screen falls through and exits
    // the app instead of returning to the Settings root. viewingAccountId is checked
    // first since it's opened from on top of the Accounts sub-screen.
    BackHandler(enabled = viewingAccountId != null || settingsSubScreen != null) {
        if (viewingAccountId != null) viewingAccountId = null else settingsSubScreen = null
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
    val categoryBudgetRepository = remember { CategoryBudgetRepository(database) }
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
            recurringTransactionRepository,
            categoryBudgetRepository
        )
    )

    var showDataResetNotice by remember { mutableStateOf(SpendStreakDatabase.dataWasResetOnLaunch) }
    var showWelcomeDialog by remember { mutableStateOf(!loadHasSeenWelcome(appContext)) }
    var selectedTheme by remember { mutableStateOf(loadSelectedTheme(appContext)) }
    var selectedThemeMode by remember { mutableStateOf(loadThemeMode(appContext)) }
    val isDarkTheme = when (selectedThemeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
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

    var isDataOperationInProgress by remember { mutableStateOf(false) }
    var dataResultDialog by remember { mutableStateOf<DataResultDialog?>(null) }
    var pendingDocumentPicker by remember { mutableStateOf<DocumentPickerRequest?>(null) }

    val importCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isDataOperationInProgress = true
                try {
                    when (val result = importCsvReplacingAll(appContext, database, uri)) {
                        is ImportResult.Success -> {
                            dataResultDialog = DataResultDialog(
                                title = "Import CSV",
                                message = "Import complete: ${result.expenseCount} expenses, " +
                                    "${result.incomeCount} income, ${result.transferCount} transfers.",
                                onRetry = null
                            )
                        }
                        is ImportResult.Failure -> {
                            dataResultDialog = DataResultDialog(
                                title = "Import CSV",
                                message = "Import failed: ${result.reason} Nothing was changed.",
                                onRetry = { pendingDocumentPicker = DocumentPickerRequest.CsvImport }
                            )
                        }
                    }
                } finally {
                    isDataOperationInProgress = false
                }
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isDataOperationInProgress = true
                try {
                    when (val result = importBackupReplacingAll(appContext, database, uri)) {
                        is BackupImportResult.Success -> {
                            dataResultDialog = DataResultDialog(
                                title = "Restore Backup",
                                message = "Restore complete: ${result.accountCount} accounts, " +
                                    "${result.categoryCount} categories, ${result.expenseCount} expenses, " +
                                    "${result.incomeCount} income, ${result.transferCount} transfers.",
                                onRetry = null
                            )
                        }
                        is BackupImportResult.Failure -> {
                            dataResultDialog = DataResultDialog(
                                title = "Restore Backup",
                                message = "Restore failed: ${result.reason} Nothing was changed.",
                                onRetry = { pendingDocumentPicker = DocumentPickerRequest.BackupRestore }
                            )
                        }
                    }
                } finally {
                    isDataOperationInProgress = false
                }
            }
        }
    }

    LaunchedEffect(pendingDocumentPicker) {
        when (pendingDocumentPicker) {
            DocumentPickerRequest.CsvImport -> importCsvLauncher.launch(CSV_MIME_TYPES)
            DocumentPickerRequest.BackupRestore -> importBackupLauncher.launch(BACKUP_MIME_TYPES)
            null -> Unit
        }
        pendingDocumentPicker = null
    }

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

    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val weeklySummary by viewModel.weeklySummary.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val income by viewModel.income.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val recurringTransactions by viewModel.recurringTransactions.collectAsStateWithLifecycle()
    val accountBalances by viewModel.accountBalances.collectAsStateWithLifecycle()
    val budget by viewModel.budget.collectAsStateWithLifecycle()
    val budgetHistory by viewModel.budgetHistory.collectAsStateWithLifecycle()
    val budgetProgress by viewModel.budgetProgress.collectAsStateWithLifecycle()
    val categoryBudgets by viewModel.categoryBudgets.collectAsStateWithLifecycle()
    val categoryBudgetProgress by viewModel.categoryBudgetProgress.collectAsStateWithLifecycle()
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    val historyEntries by viewModel.historyEntries.collectAsStateWithLifecycle()
    val achievements by viewModel.achievements.collectAsStateWithLifecycle()
    val pendingLevelUp by viewModel.pendingLevelUp.collectAsStateWithLifecycle()
    val unlockedThemes = unlockedThemesForLevel(progress.level)

    CompositionLocalProvider(LocalCurrencyCode provides selectedCurrencyCode) {
    SpendStreakTheme(themeOption = selectedTheme, isDark = isDarkTheme) {
    if (showWelcomeDialog) {
        AlertDialog(
            onDismissRequest = {
                showWelcomeDialog = false
                markWelcomeSeen(appContext)
            },
            confirmButton = {
                TextButton(onClick = {
                    showWelcomeDialog = false
                    markWelcomeSeen(appContext)
                }) { Text("GOT IT") }
            },
            title = { Text("Welcome to SpendStreak") },
            text = {
                Text(
                    "Log your expenses and income to build a daily streak and earn XP — " +
                        "level up by showing up, not by spending less. Check the " +
                        "Achievements tab to see what you've unlocked, and set a budget " +
                        "or recurring reminders any time from Settings."
                )
            }
        )
    }

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

    dataResultDialog?.let { dialog ->
        AlertDialog(
            onDismissRequest = { dataResultDialog = null },
            confirmButton = {
                TextButton(onClick = { dataResultDialog = null }) { Text("OK") }
            },
            dismissButton = dialog.onRetry?.let { retry ->
                {
                    TextButton(onClick = {
                        dataResultDialog = null
                        retry()
                    }) {
                        Text("TRY AGAIN")
                    }
                }
            },
            title = { Text(dialog.title) },
            text = { Text(dialog.message) }
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
                            viewingAccountId = null
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
                onLevelUpAcknowledged = { viewModel.acknowledgeLevelUp() },
                onBalanceClick = {
                    currentScreen = AppScreen.Settings
                    settingsSubScreen = SettingsSubScreen.Accounts
                    viewingAccountId = null
                }
            )
            AppScreen.AddExpense -> AddTransactionScreen(
                modifier = contentModifier,
                accounts = accounts,
                categories = categories,
                onSaveExpense = { amount, categoryId, accountId, note, timestampMillis, excludedFromBudget ->
                    viewModel.addExpense(amount, categoryId, accountId, note, timestampMillis, excludedFromBudget)
                },
                onSaveIncome = { amount, categoryId, accountId, note, timestampMillis, excludedFromBudget ->
                    viewModel.addIncome(amount, categoryId, accountId, note, timestampMillis, excludedFromBudget)
                },
                onSaveTransfer = { amount, fromAccountId, toAccountId, note, timestampMillis ->
                    viewModel.addTransfer(
                        fromAccountId = fromAccountId,
                        toAccountId = toAccountId,
                        amount = amount,
                        note = note,
                        timestampMillis = timestampMillis
                    )
                },
                onAddCategory = { name, kind, emoji, onComplete ->
                    viewModel.addCategory(name, kind, emoji, onComplete)
                },
                onRenameCategory = { category, name, emoji, onComplete ->
                    viewModel.updateCategory(category.copy(name = name, emoji = emoji), onComplete)
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
                onDeleteTransfer = { id, onResult -> viewModel.deleteTransfer(id, onResult) },
                onAddCategory = { name, kind, emoji, onComplete ->
                    viewModel.addCategory(name, kind, emoji, onComplete)
                },
                onRenameCategory = { category, name, emoji, onComplete ->
                    viewModel.updateCategory(category.copy(name = name, emoji = emoji), onComplete)
                },
                onDeleteCategory = { categoryId, onResult -> viewModel.deleteCategory(categoryId, onResult) }
            )
            AppScreen.Achievements -> AchievementsScreen(
                modifier = contentModifier,
                progress = progress,
                achievements = achievements
            )
            AppScreen.Settings -> when (settingsSubScreen) {
                SettingsSubScreen.Accounts -> {
                    val viewedAccountId = viewingAccountId
                    val viewedAccount = accounts.find { it.id == viewedAccountId }
                    if (viewedAccountId != null && viewedAccount != null) {
                        HistoryScreen(
                            modifier = contentModifier,
                            entries = historyEntries,
                            accounts = accounts,
                            categories = categories,
                            onUpdateExpense = { viewModel.updateExpense(it) },
                            onDeleteExpense = { id, onResult -> viewModel.deleteExpense(id, onResult) },
                            onUpdateIncome = { viewModel.updateIncome(it) },
                            onDeleteIncome = { id, onResult -> viewModel.deleteIncome(id, onResult) },
                            onUpdateTransfer = { viewModel.updateTransfer(it) },
                            onDeleteTransfer = { id, onResult -> viewModel.deleteTransfer(id, onResult) },
                            onAddCategory = { name, kind, emoji, onComplete ->
                                viewModel.addCategory(name, kind, emoji, onComplete)
                            },
                            onRenameCategory = { category, name, emoji, onComplete ->
                                viewModel.updateCategory(category.copy(name = name, emoji = emoji), onComplete)
                            },
                            onDeleteCategory = { categoryId, onResult -> viewModel.deleteCategory(categoryId, onResult) },
                            filterAccountId = viewedAccountId,
                            title = viewedAccount.name.uppercase(),
                            onBack = { viewingAccountId = null },
                            headerContent = {
                                EditOpeningBalancePanel(
                                    account = viewedAccount,
                                    onSave = { newBalance ->
                                        viewModel.updateAccount(viewedAccount.copy(openingBalance = newBalance))
                                    }
                                )
                            }
                        )
                    } else {
                        AccountsScreen(
                            modifier = contentModifier,
                            accounts = accounts,
                            accountBalances = accountBalances,
                            onAddAccount = { name, type, openingBalance, onComplete ->
                                viewModel.addAccount(name, type, openingBalance, onComplete)
                            },
                            onDeleteAccount = { accountId, onResult -> viewModel.deleteAccount(accountId, onResult) },
                            onViewAccount = { accountId -> viewingAccountId = accountId },
                            onBack = { settingsSubScreen = null }
                        )
                    }
                }
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
                    categories = categories,
                    categoryBudgets = categoryBudgets,
                    categoryBudgetProgress = categoryBudgetProgress,
                    onSetCategoryBudgetLimit = { categoryId, limit -> viewModel.setCategoryBudgetLimit(categoryId, limit) },
                    onClearCategoryBudgetLimit = { categoryId -> viewModel.clearCategoryBudgetLimit(categoryId) },
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
                    themeMode = selectedThemeMode,
                    onSelectThemeMode = { mode ->
                        selectedThemeMode = mode
                        saveThemeMode(appContext, mode)
                    },
                    currencyCode = selectedCurrencyCode,
                    onSelectCurrency = { code ->
                        selectedCurrencyCode = code
                        saveCurrencyCode(appContext, code)
                    },
                    isDataOperationInProgress = isDataOperationInProgress,
                    onExportData = {
                        coroutineScope.launch {
                            isDataOperationInProgress = true
                            try {
                                exportAndShareCsv(appContext, historyEntries)
                            } finally {
                                isDataOperationInProgress = false
                            }
                        }
                    },
                    onImportData = {
                        importCsvLauncher.launch(CSV_MIME_TYPES)
                    },
                    onExportBackup = {
                        coroutineScope.launch {
                            isDataOperationInProgress = true
                            try {
                                exportAndShareBackup(appContext, database)
                            } finally {
                                isDataOperationInProgress = false
                            }
                        }
                    },
                    onImportBackup = {
                        importBackupLauncher.launch(BACKUP_MIME_TYPES)
                    }
                )
            }
        }
    }
    }
    }
}
