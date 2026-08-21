package com.spendstreak.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.spendstreak.app.util.SPENDSTREAK_PREFS_NAME

@Database(
    entities = [
        Expense::class, UserProgress::class, Account::class, Income::class, Budget::class,
        Transfer::class, Category::class, RecurringTransaction::class
    ],
    version = 7,
    exportSchema = false
)
abstract class SpendStreakDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun userProgressDao(): UserProgressDao
    abstract fun accountDao(): AccountDao
    abstract fun incomeDao(): IncomeDao
    abstract fun budgetDao(): BudgetDao
    abstract fun transferDao(): TransferDao
    abstract fun categoryDao(): CategoryDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao

    companion object {
        @Volatile
        private var instance: SpendStreakDatabase? = null

        private const val KEY_HAS_LAUNCHED_BEFORE = "has_launched_before"

        // Every real schema change from here on adds a Migration object to this array
        // *before* the version bump ships — this is what stops an update from silently
        // wiping every user's data the way versions 1->2 and 2->3 did pre-launch (accepted
        // then; not acceptable now that real installs may have real data).
        // fallbackToDestructiveMigration() below stays only as a safety net for a version
        // gap nobody wrote a migration for — it must never be the thing that actually runs
        // for a version bump shipped deliberately, like this one.
        //
        // Shared by MIGRATION_3_4 (upgrading an existing install) and the onCreate seed
        // callback below (a fresh install, which never runs migrations at all — Room
        // creates the latest schema directly) so both paths seed identical default rows.
        private val DEFAULT_EXPENSE_CATEGORIES = listOf(
            "Food" to "🍔", "Transport" to "🚗", "Shopping" to "🛍",
            "Bills" to "🧾", "Entertainment" to "🎬", "Other" to "❓"
        )
        private val DEFAULT_INCOME_CATEGORIES = listOf(
            "Salary" to "💰", "Freelance" to "💼", "Gift" to "🎁", "Other" to "❓"
        )

        // 3->4: adds the `categories` table (user-editable categories/income sources) and
        // replaces Expense.category / Income.source (free-text String) with a categoryId
        // foreign-key-style column. SQLite can't DROP COLUMN pre-3.35, so both tables are
        // recreated (standard Room-migration pattern for a column removal) rather than
        // altered in place. Existing free-text values are matched to the newly-seeded rows
        // by name; anything that doesn't match (shouldn't happen, since the seed list is
        // exactly what the app has ever offered) falls back to "Other".
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS categories (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, kind TEXT NOT NULL, emoji TEXT NOT NULL)"
                )

                DEFAULT_EXPENSE_CATEGORIES.forEach { (name, emoji) ->
                    db.execSQL(
                        "INSERT INTO categories (name, kind, emoji) VALUES ('$name', 'EXPENSE', '$emoji')"
                    )
                }
                DEFAULT_INCOME_CATEGORIES.forEach { (name, emoji) ->
                    db.execSQL(
                        "INSERT INTO categories (name, kind, emoji) VALUES ('$name', 'INCOME', '$emoji')"
                    )
                }

                db.execSQL(
                    "CREATE TABLE expenses_new (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, amount REAL NOT NULL, " +
                        "categoryId INTEGER NOT NULL, note TEXT NOT NULL, accountId INTEGER NOT NULL, " +
                        "timestampMillis INTEGER NOT NULL)"
                )
                db.execSQL(
                    "INSERT INTO expenses_new (id, amount, categoryId, note, accountId, timestampMillis) " +
                        "SELECT expenses.id, expenses.amount, " +
                        "COALESCE(" +
                        "(SELECT id FROM categories WHERE categories.name = expenses.category AND categories.kind = 'EXPENSE'), " +
                        "(SELECT id FROM categories WHERE categories.name = 'Other' AND categories.kind = 'EXPENSE')" +
                        "), expenses.note, expenses.accountId, expenses.timestampMillis FROM expenses"
                )
                db.execSQL("DROP TABLE expenses")
                db.execSQL("ALTER TABLE expenses_new RENAME TO expenses")

                db.execSQL(
                    "CREATE TABLE income_new (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, amount REAL NOT NULL, " +
                        "categoryId INTEGER NOT NULL, note TEXT NOT NULL, accountId INTEGER NOT NULL, " +
                        "timestampMillis INTEGER NOT NULL)"
                )
                db.execSQL(
                    "INSERT INTO income_new (id, amount, categoryId, note, accountId, timestampMillis) " +
                        "SELECT income.id, income.amount, " +
                        "COALESCE(" +
                        "(SELECT id FROM categories WHERE categories.name = income.source AND categories.kind = 'INCOME'), " +
                        "(SELECT id FROM categories WHERE categories.name = 'Other' AND categories.kind = 'INCOME')" +
                        "), income.note, income.accountId, income.timestampMillis FROM income"
                )
                db.execSQL("DROP TABLE income")
                db.execSQL("ALTER TABLE income_new RENAME TO income")
            }
        }

        // 4->5: adds UserProgress.logsToday (XP-per-day anti-spam cap) — a plain ADD COLUMN
        // with a default, unlike 3->4's table recreation, since nothing is being removed.
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_progress ADD COLUMN logsToday INTEGER NOT NULL DEFAULT 0")
            }
        }

        // 5->6: Budget goes from a singleton row (id always 0, one row total, overwritten
        // on every save) to one row per budget ever set, with `isActive` marking the
        // current one — this is what makes budget *history* possible. The primary key
        // shape changes too (plain id -> autoGenerate), so — same reasoning as 3->4 — the
        // table is recreated rather than altered in place. The old table held at most one
        // row; if present, it's carried forward as the current active budget under a
        // generic name (there was no name field to preserve).
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE budget_new (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, " +
                        "amountLimit REAL NOT NULL, periodType TEXT NOT NULL, " +
                        "startEpochDay INTEGER, endEpochDay INTEGER, isActive INTEGER NOT NULL)"
                )
                db.execSQL(
                    "INSERT INTO budget_new (name, amountLimit, periodType, startEpochDay, endEpochDay, isActive) " +
                        "SELECT 'Budget', amountLimit, periodType, startEpochDay, endEpochDay, 1 FROM budget"
                )
                db.execSQL("DROP TABLE budget")
                db.execSQL("ALTER TABLE budget_new RENAME TO budget")
            }
        }

        // 6->7: adds the `recurring_transactions` table (recurring bill/income reminders).
        // Brand-new feature, no prior data to carry forward — a plain CREATE TABLE, no
        // recreation needed.
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS recurring_transactions (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, type TEXT NOT NULL, " +
                        "amount REAL NOT NULL, categoryId INTEGER NOT NULL, accountId INTEGER NOT NULL, " +
                        "note TEXT NOT NULL, intervalType TEXT NOT NULL, nextDueEpochDay INTEGER NOT NULL, " +
                        "active INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7
        )

        // Set at most once per process: true only when this launch's onCreate fired (a
        // freshly-created schema) even though a previous launch had already completed —
        // i.e. fallbackToDestructiveMigration just wiped an existing install, not a genuine
        // first run. The UI reads this once at startup to tell the user what happened.
        @Volatile
        var dataWasResetOnLaunch: Boolean = false
            private set

        fun getInstance(context: Context): SpendStreakDatabase =
            instance ?: synchronized(this) {
                instance ?: run {
                    val appContext = context.applicationContext
                    val prefs = appContext.getSharedPreferences(SPENDSTREAK_PREFS_NAME, Context.MODE_PRIVATE)
                    val hadLaunchedBefore = prefs.getBoolean(KEY_HAS_LAUNCHED_BEFORE, false)

                    val seedDefaultAccountsCallback = object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            if (hadLaunchedBefore) {
                                dataWasResetOnLaunch = true
                            }
                            db.execSQL("INSERT INTO accounts (name, type) VALUES ('Cash', 'Cash')")
                            DEFAULT_EXPENSE_CATEGORIES.forEach { (name, emoji) ->
                                db.execSQL(
                                    "INSERT INTO categories (name, kind, emoji) VALUES ('$name', 'EXPENSE', '$emoji')"
                                )
                            }
                            DEFAULT_INCOME_CATEGORIES.forEach { (name, emoji) ->
                                db.execSQL(
                                    "INSERT INTO categories (name, kind, emoji) VALUES ('$name', 'INCOME', '$emoji')"
                                )
                            }
                        }
                    }

                    Room.databaseBuilder(appContext, SpendStreakDatabase::class.java, "spendstreak.db")
                        .addMigrations(*MIGRATIONS)
                        .fallbackToDestructiveMigration(dropAllTables = true)
                        .addCallback(seedDefaultAccountsCallback)
                        .build()
                        .also {
                            instance = it
                            prefs.edit().putBoolean(KEY_HAS_LAUNCHED_BEFORE, true).apply()
                        }
                }
            }
    }
}
