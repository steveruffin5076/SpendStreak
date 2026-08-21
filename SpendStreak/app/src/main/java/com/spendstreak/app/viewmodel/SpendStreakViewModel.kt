package com.spendstreak.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.withTransaction
import com.spendstreak.app.data.Account
import com.spendstreak.app.data.AccountRepository
import com.spendstreak.app.data.Achievement
import com.spendstreak.app.data.Achievements
import com.spendstreak.app.data.Budget
import com.spendstreak.app.data.BudgetPeriodType
import com.spendstreak.app.data.BudgetRepository
import com.spendstreak.app.data.Category
import com.spendstreak.app.data.CategoryRepository
import com.spendstreak.app.data.Expense
import com.spendstreak.app.data.ExpenseRepository
import com.spendstreak.app.data.Income
import com.spendstreak.app.data.IncomeRepository
import com.spendstreak.app.data.RecurringTransaction
import com.spendstreak.app.data.RecurringTransactionRepository
import com.spendstreak.app.data.SpendStreakDatabase
import com.spendstreak.app.data.Transfer
import com.spendstreak.app.data.TransferRepository
import com.spendstreak.app.data.UserProgress
import com.spendstreak.app.data.UserProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class WeeklySummary(val count: Int, val total: Double)

data class BudgetProgress(val limit: Double, val spent: Double, val isOverBudget: Boolean)

// `accountName` deliberately isn't on the interface: it's never read polymorphically
// (only `timestampMillis` is, for sorting) and a transfer has two account names, not
// one, so forcing a single shared property here would just invite an awkward synthetic
// combined string. Each subtype declares whatever account-name shape actually fits it.
sealed interface HistoryEntry {
    val timestampMillis: Long

    data class ExpenseEntry(
        val expense: Expense,
        val accountName: String,
        val categoryName: String,
        val categoryEmoji: String
    ) : HistoryEntry {
        override val timestampMillis get() = expense.timestampMillis
    }

    data class IncomeEntry(
        val income: Income,
        val accountName: String,
        val categoryName: String,
        val categoryEmoji: String
    ) : HistoryEntry {
        override val timestampMillis get() = income.timestampMillis
    }

    data class TransferEntry(
        val transfer: Transfer,
        val fromAccountName: String,
        val toAccountName: String
    ) : HistoryEntry {
        override val timestampMillis get() = transfer.timestampMillis
    }
}

class SpendStreakViewModel(
    private val database: SpendStreakDatabase,
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val accountRepository: AccountRepository,
    private val budgetRepository: BudgetRepository,
    private val transferRepository: TransferRepository,
    private val userProgressRepository: UserProgressRepository,
    private val categoryRepository: CategoryRepository,
    private val recurringTransactionRepository: RecurringTransactionRepository
) : ViewModel() {

    val expenses: StateFlow<List<Expense>> = expenseRepository.expenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val income: StateFlow<List<Income>> = incomeRepository.income
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val accounts: StateFlow<List<Account>> = accountRepository.accounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<Category>> = categoryRepository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recurringTransactions: StateFlow<List<RecurringTransaction>> = recurringTransactionRepository.recurringTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val budget: StateFlow<Budget?> = budgetRepository.budget
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val budgetHistory: StateFlow<List<Budget>> = budgetRepository.budgetHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val transfers: StateFlow<List<Transfer>> = transferRepository.transfers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val progress: StateFlow<UserProgress> = userProgressRepository.progress
        .map { it ?: UserProgress() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProgress())

    val weeklySummary: StateFlow<WeeklySummary> = expenses
        .map { list ->
            val cutoff = System.currentTimeMillis() - SEVEN_DAYS_MILLIS
            val recent = list.filter { it.timestampMillis >= cutoff }
            WeeklySummary(count = recent.size, total = recent.sumOf { it.amount })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeeklySummary(0, 0.0))

    val balance: StateFlow<Double> = combine(expenses, income) { exp, inc ->
        inc.sumOf { it.amount } - exp.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    // Net balance per account, computed in one pass — the single source of truth for
    // "balance", so screens never recompute this formula themselves. Transfers move
    // money between accounts (source down, destination up) without touching the
    // app-wide `balance` above, since they're neither income nor expense.
    val accountBalances: StateFlow<Map<Long, Double>> = combine(expenses, income, transfers) { exp, inc, trans ->
        val balances = mutableMapOf<Long, Double>()
        inc.forEach { balances[it.accountId] = (balances[it.accountId] ?: 0.0) + it.amount }
        exp.forEach { balances[it.accountId] = (balances[it.accountId] ?: 0.0) - it.amount }
        trans.forEach {
            balances[it.fromAccountId] = (balances[it.fromAccountId] ?: 0.0) - it.amount
            balances[it.toAccountId] = (balances[it.toAccountId] ?: 0.0) + it.amount
        }
        balances
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val historyEntries: StateFlow<List<HistoryEntry>> =
        combine(expenses, income, transfers, accounts, categories) { exp, inc, trans, accts, cats ->
            val nameById = accts.associate { it.id to it.name }
            val categoryById = cats.associateBy { it.id }
            val expenseEntries = exp.map {
                val category = categoryById[it.categoryId]
                HistoryEntry.ExpenseEntry(
                    expense = it,
                    accountName = nameById[it.accountId] ?: "Unknown",
                    categoryName = category?.name ?: "Other",
                    categoryEmoji = category?.emoji ?: "❓"
                )
            }
            val incomeEntries = inc.map {
                val category = categoryById[it.categoryId]
                HistoryEntry.IncomeEntry(
                    income = it,
                    accountName = nameById[it.accountId] ?: "Unknown",
                    categoryName = category?.name ?: "Other",
                    categoryEmoji = category?.emoji ?: "❓"
                )
            }
            val transferEntries = trans.map {
                HistoryEntry.TransferEntry(
                    it,
                    nameById[it.fromAccountId] ?: "Unknown",
                    nameById[it.toAccountId] ?: "Unknown"
                )
            }
            (expenseEntries + incomeEntries + transferEntries).sortedByDescending { it.timestampMillis }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val budgetProgress: StateFlow<BudgetProgress?> = combine(expenses, budget) { exp, activeBudget ->
        if (activeBudget == null) return@combine null
        val zone = ZoneId.systemDefault()
        val (periodStartMillis, periodEndMillis) = if (activeBudget.periodType == BudgetPeriodType.MONTHLY) {
            val startOfMonth = LocalDate.now(zone).withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
            startOfMonth to System.currentTimeMillis()
        } else {
            val start = activeBudget.startEpochDay
                ?.let { LocalDate.ofEpochDay(it).atStartOfDay(zone).toInstant().toEpochMilli() }
                ?: 0L
            val end = activeBudget.endEpochDay
                ?.let { LocalDate.ofEpochDay(it).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() }
                ?: System.currentTimeMillis()
            start to end
        }
        val spent = exp.filter { it.timestampMillis in periodStartMillis until periodEndMillis }.sumOf { it.amount }
        BudgetProgress(limit = activeBudget.amountLimit, spent = spent, isOverBudget = spent > activeBudget.amountLimit)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val achievements: StateFlow<List<Achievement>> =
        combine(expenses, income, transfers, progress) { exp, inc, trans, prog ->
            Achievements.compute(exp, inc, trans, prog)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Tracked independently of `progress` (and its synthetic seed value) so a level-up is
    // detected exactly once no matter which tab is on screen when it happens, and stays
    // pending — not time-limited — until whichever screen shows the banner acknowledges it.
    private var lastSeenLevel: Int? = null
    private val _pendingLevelUp = MutableStateFlow<Int?>(null)
    val pendingLevelUp: StateFlow<Int?> = _pendingLevelUp.asStateFlow()

    init {
        viewModelScope.launch {
            userProgressRepository.progress.collect { current ->
                val currentLevel = current?.level ?: return@collect
                val previous = lastSeenLevel
                if (previous != null && currentLevel > previous) {
                    _pendingLevelUp.value = currentLevel
                }
                lastSeenLevel = currentLevel
            }
        }
    }

    fun acknowledgeLevelUp() {
        _pendingLevelUp.value = null
    }

    fun addExpense(amount: Double, categoryId: Long, accountId: Long, note: String) {
        viewModelScope.launch { expenseRepository.addExpense(amount, categoryId, accountId, note) }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch { expenseRepository.updateExpense(expense) }
    }

    fun deleteExpense(expenseId: Long, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(expenseId)
            onResult(true)
        }
    }

    fun addIncome(amount: Double, categoryId: Long, accountId: Long, note: String) {
        viewModelScope.launch { incomeRepository.addIncome(amount, categoryId, accountId, note) }
    }

    fun updateIncome(income: Income) {
        viewModelScope.launch { incomeRepository.updateIncome(income) }
    }

    fun deleteIncome(incomeId: Long, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            incomeRepository.deleteIncome(incomeId)
            onResult(true)
        }
    }

    fun addCategory(name: String, kind: String, emoji: String) {
        viewModelScope.launch { categoryRepository.addCategory(name, kind, emoji) }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch { categoryRepository.updateCategory(category) }
    }

    fun deleteCategory(categoryId: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(categoryRepository.deleteCategory(categoryId)) }
    }

    // Named arguments at the call site (and in TransferRepository) are deliberate here —
    // fromAccountId/toAccountId are both plain Long, so a positional swap would compile
    // silently and move money the wrong direction.
    fun addTransfer(fromAccountId: Long, toAccountId: Long, amount: Double, note: String) {
        viewModelScope.launch {
            transferRepository.addTransfer(
                fromAccountId = fromAccountId,
                toAccountId = toAccountId,
                amount = amount,
                note = note
            )
        }
    }

    fun updateTransfer(transfer: Transfer) {
        viewModelScope.launch { transferRepository.updateTransfer(transfer) }
    }

    fun deleteTransfer(transferId: Long, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            transferRepository.deleteTransfer(transferId)
            onResult(true)
        }
    }

    fun addAccount(name: String, type: String) {
        viewModelScope.launch { accountRepository.addAccount(name, type) }
    }

    fun deleteAccount(accountId: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(accountRepository.deleteAccount(accountId)) }
    }

    fun setMonthlyBudget(name: String, amountLimit: Double) {
        viewModelScope.launch { budgetRepository.setMonthlyBudget(name, amountLimit) }
    }

    fun setCustomBudget(name: String, amountLimit: Double, startEpochDay: Long, endEpochDay: Long) {
        viewModelScope.launch { budgetRepository.setCustomBudget(name, amountLimit, startEpochDay, endEpochDay) }
    }

    fun updateBudget(budget: Budget) {
        viewModelScope.launch { budgetRepository.updateBudget(budget) }
    }

    fun deleteBudget(id: Long) {
        viewModelScope.launch { budgetRepository.deleteBudget(id) }
    }

    fun addRecurring(recurring: RecurringTransaction) {
        viewModelScope.launch { recurringTransactionRepository.addRecurring(recurring) }
    }

    fun updateRecurring(recurring: RecurringTransaction) {
        viewModelScope.launch { recurringTransactionRepository.updateRecurring(recurring) }
    }

    fun deleteRecurring(id: Long) {
        viewModelScope.launch { recurringTransactionRepository.deleteRecurring(id) }
    }

    fun clearBudget() {
        viewModelScope.launch { budgetRepository.clearBudget() }
    }

    // Runs as one transaction so a mid-sequence failure can't leave data partially
    // cleared, and reports success/failure back to the caller instead of assuming it
    // worked — the UI shouldn't say "cleared" before this coroutine actually finishes.
    fun clearAllData(onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = try {
                database.withTransaction {
                    expenseRepository.clearAllData()
                    incomeRepository.clearAllData()
                    transferRepository.clearAllData()
                    budgetRepository.clearAllData()
                    recurringTransactionRepository.clearAllData()
                    userProgressRepository.clear()
                }
                true
            } catch (e: Exception) {
                false
            }
            onResult(success)
        }
    }

    companion object {
        private const val SEVEN_DAYS_MILLIS = 7L * 24 * 60 * 60 * 1000

        fun factory(
            database: SpendStreakDatabase,
            expenseRepository: ExpenseRepository,
            incomeRepository: IncomeRepository,
            accountRepository: AccountRepository,
            budgetRepository: BudgetRepository,
            transferRepository: TransferRepository,
            userProgressRepository: UserProgressRepository,
            categoryRepository: CategoryRepository,
            recurringTransactionRepository: RecurringTransactionRepository
        ) = viewModelFactory {
            initializer {
                SpendStreakViewModel(
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
            }
        }
    }
}
