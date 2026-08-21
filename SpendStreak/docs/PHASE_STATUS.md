# SpendStreak — Build Status Tracker

**Last updated:** 2026-08-05 (Transfers, expanded Achievements, and Reports added)

## Project Context
- Coding background: None (complete beginner)
- Pace: ~2 hrs/week
- Monetization: None
- Package folder attached to Claude Code: ✅ Yes — `C:\Users\IRSRnDSteve\AndroidStudioProjects\SpendStreak` (moved back from the `K3n5h` machine; `local.properties` `sdk.dir` corrected to this machine's SDK path)

---

## Phase Progress

| Phase | Status | Notes |
|-------|--------|-------|
| **Phase 0 — Product Design** | ✅ Done | Design doc finalized (`PHASE_0_DESIGN.md`). Target audience: general. Package ID set to `com.spendstreak.app` (renamed from Android Studio default). |
| **Phase 1 — Foundation** | ✅ Done | Package renamed to `com.spendstreak.app`. Bottom navigation shell built with 5 screen stubs (Dashboard, Add Expense, History, Level & Achievements, Settings) wired via `AppScreen` enum in `MainActivity.kt`. Added `material-icons-core` dependency for nav icons. **Verified running on device/emulator 2026-08-03** — all 5 tabs render and switch correctly. |
| **Phase 2 — Core Screens + Navigation** | ✅ Done | All 5 screens built with the **Retro RPG-leveling visual style** (dark indigo palette, bold monospace type, chunky bordered `RetroPanel`, segmented `RetroProgressBar`) and **verified on device (2026-08-03)**: Dashboard, Add Expense, History, Level & Achievements, Settings. Everything still runs on placeholder mock data / no-op actions honestly labeled as such — real persistence and logic land in Phase 3. |
| **Phase 3 — Data Models + Local Storage** | ✅ Done | Added Room (2.8.4) + KSP (2.2.10-2.0.2) for local persistence: `Expense` and `UserProgress` entities/DAOs, `SpendStreakDatabase`, `ExpenseRepository` (handles streak/XP/level-up math on each log), and `Achievement` (unlocked status derived from expenses+progress, not stored separately). `SpendStreakViewModel` wired into `MainActivity` and all 5 screens — mock data fully removed. **Verified on device 2026-08-03**: logging real expenses correctly updates streak, XP, and unlocks the "First Steps" achievement live; History and Dashboard totals match. Fixed a known KSP/AGP-9-built-in-Kotlin incompatibility along the way via `android.disallowKotlinSourceSets=false` in `gradle.properties` (tracked upstream at github.com/google/ksp/issues/2729). |
| **Phase 4a — Animations/Polish** | ✅ Done — re-verified on device 2026-08-03 | XP bar animates its fill (`animateFloatAsState`) — verified. Achievement badges animate color on unlock — verified. "LEVEL UP!" banner fix **confirmed working on device/emulator**: leveled the app up from 1→4 via automated taps, including one level-up triggered while sitting on the History tab (not Dashboard) and left there 4+ seconds (past the 2.5s auto-dismiss timer) before switching to Dashboard — banner still correctly appeared showing "YOU REACHED LEVEL 4". Confirms the `pendingLevelUp` StateFlow persists regardless of which tab is active when the level-up happens. |
| **Phase 4b — Input/Keyboard Polish** | ✅ Done — verified on device 2026-08-03 | Add Expense: amount field auto-focuses on screen entry (confirmed — cursor active immediately) — verified. Decimal restriction (max 2 decimal places) confirmed: typing a 3rd decimal digit is silently rejected, field stops at "12.99". IME "Done" on the Note field correctly triggers save (fields clear, "Saved!" message, keyboard hides) — verified. IME "Next" on Amount → Note wiring uses the same standard `focusManager.moveFocus`/`KeyboardActions` pattern confirmed working for Done; not independently screenshot-verified due to on-device tap-coordinate flakiness during this session, but the code path is identical and low-risk. |
| **Phase 4c — Credits Screen** | ✅ Done | No separate screen added (would need real back-stack navigation we don't have yet) — folded proper credits content into Settings' About panel instead. |
| **Phase 4d — Monetization** | ➖ Skipped | User chose no monetization |
| **Phase 5 — Launch Prep & Testing** | 🟡 In progress | Custom app icon done + verified on device: retro RPG-style "level-up" double-chevron (gold→orange) on indigo circle, replaces default Android Studio robot. Adaptive icon (`ic_launcher_background.xml`/`ic_launcher_foreground.xml`, API 26+, matches app's minSdk so this is what every real device shows) confirmed rendering correctly in app drawer and as the auto-generated splash screen. Legacy raster fallback icons (`mipmap-*/ic_launcher*.webp`, only relevant on non-adaptive launchers, effectively unreachable given minSdk 26) intentionally left as old placeholder — low priority, can regenerate later if it ever surfaces. Also ran a full manual pass over all 5 screens post-verification (Dashboard, Add, History, Level & Achievements, Settings) confirming no regressions: empty states, achievement unlock animation, and data clearing all still correct. Release signing config also done: keystore at `keystore/spendstreak-release.jks` (RSA 2048, 30-year validity, PKCS12), credentials in `keystore/keystore.properties` (both gitignored — never commit these), wired into `app/build.gradle.kts` so `assembleRelease` auto-signs when the properties file is present and falls back to unsigned if it's missing (keeps the build working on a fresh machine before the keystore is copied over). Verified end-to-end on device 2026-08-03: `assembleRelease` builds, `apksigner verify` confirms v2 signature, installs and launches clean on the emulator. **Important:** back up `keystore/spendstreak-release.jks` and `keystore/keystore.properties` somewhere safe outside the project folder (e.g. password manager or encrypted drive) — if this keystore is ever lost, a Play Store release (or any install expecting this signature) can never be updated again, only replaced as a new app. Remaining for Phase 5: broader device/edge-case test pass, and (if wanted) Play Store listing prep. |

---

## Income / Account / Budget Feature (2026-08-04)
Pulled forward from v2 (see `PHASE_0_DESIGN.md` scope pivot). **Built, not yet verified on device.**

- **Data layer**: new `Account`, `Income`, `Budget` entities + DAOs (`Budget` is a singleton row like `UserProgress`, not "latest row wins"). `Expense` gained `accountId`. Room bumped 1→2 with `fallbackToDestructiveMigration(dropAllTables = true)` — **this wipes whatever's currently on-device the next time the app updates.** 3 default accounts (Bank/Cash/Credit Card) are seeded via raw SQL in `RoomDatabase.Callback.onCreate`.
- **Refactor**: streak/XP logic moved out of `ExpenseRepository` into `UserProgressRepository.recordDailyActivity()`, now wrapped in `database.withTransaction {}` to close a real race (two rapid-fire logs could previously clobber each other's XP/streak update). Both expense and income logging call into it — logging Income counts toward streak/XP exactly like Expense.
- **New repos**: `IncomeRepository`, `AccountRepository` (blocks delete if an account has linked transactions — never cascades or reassigns), `BudgetRepository`.
- **ViewModel**: added `income`, `accounts`, `budget`, `balance` (all-time income − expenses), `budgetProgress` (spent vs limit for the active period, with an `isOverBudget` flag since `RetroProgressBar` clamps to 100% and can't show overflow on its own), and `historyEntries` (Expense+Income merged and sorted in-memory, not a DB-level view — matches the existing "derive don't duplicate" pattern from `Achievement.kt`).
- **UI**: `AddExpenseScreen` renamed `AddTransactionScreen` — Expense/Income toggle, category/source chips, and an account-picker chip row. `HistoryScreen` shows both types merged, Income in `+RM` tertiary color. `DashboardScreen` gained Budget and Balance panels. New `AccountsScreen` (list + computed balance + add/delete) and `BudgetScreen` (amount, Monthly/Custom toggle, Material3 `DatePicker` for custom range) reached via two new rows in `SettingsScreen`, using a hand-rolled nullable sub-screen state in `MainActivity` — no Navigation Compose library added, since it's only one level of nesting.
- **`clearAllData()`** now also clears Income and the Budget, but **keeps Accounts** (treated as configuration, not "data" — recreating custom accounts would be tedious).

**Verification needed on device**: add both an expense and an income and confirm streak/XP updates for both; add a custom account and use it; confirm delete is blocked on an account with transactions; History shows both types merged/colored correctly; set a monthly budget and confirm Dashboard tracks it including the over-budget color swap; set a custom-range budget via the date pickers.

## Code Review Fixes (2026-08-05)
Two review passes over the Income/Account/Budget feature turned up real issues, all fixed. **Correctness**: expense/income insert + streak update now share one `withTransaction`; `AccountRepository.deleteAccount` wraps its check-then-delete in a transaction (was a TOCTOU race); Budget screen's local form now resyncs (`remember(budget)`) instead of going stale after save/clear; `BudgetScreen` amount formatting now uses a shared locale-invariant `formatAmount()` (was breaking resave on comma-decimal locales); `clearAllData()` now runs in one transaction and reports success/failure instead of assuming it worked; `MainActivity` nav state uses `rememberSaveable` + a `BackHandler` (system back from Accounts/Budget was exiting the app entirely); `AddTransactionScreen`'s account selection is `rememberSaveable` (was silently resetting to the default account after process death). **Visual**: `Theme.kt` now sets the Material3 "container" color roles (`secondaryContainer`, `surfaceContainer`, `surfaceContainerHigh`, etc.) so selected chips, the nav bar, and dialogs render in the Retro palette instead of Material3's generic defaults; Dashboard/Add/Budget/Settings all gained vertical scroll; long account names/notes now truncate instead of pushing layouts off-screen.

## Transfers, Achievements, and Reports (2026-08-05)
- **Transfer**: new `Transfer` entity/DAO/repository — move money between your own accounts. Deliberately has **no** `UserProgressRepository` dependency and no `withTransaction` (a plain single-table insert), which is what structurally keeps transfers out of the daily streak/XP habit loop, unlike Income. Shows in History as a neutral (non-green/red) entry. `AccountDao.countTransactionsUsing` extended so deleting an account involved in a transfer is blocked, same as for expenses/income. Room bumped **2→3**, same accepted destructive-migration tradeoff as the previous bump — **another full on-device data wipe** on next install.
- **`AddTransactionScreen`** gained a third TRANSFER mode with FROM/TO account pickers; every place that used to branch on `mode == EXPENSE` is now an exhaustive `when(mode)` so Transfer can't silently fall into the wrong branch.
- **6 new achievements**: HALF CENTURY (50 expenses), FIRST PAYCHECK (first income), DIVERSE INCOME (3 income sources), PLANNER (set a budget), MONEY MOVER (first transfer), ACCOUNT JUGGLER (10 transfers). PLANNER needed a small schema addition — `UserProgress.hasSetBudget`, set once and never cleared — since `Budget` is a singleton with no history and checking "is a budget currently set" would let the achievement flicker off after clearing, breaking the existing "achievements only grow" invariant.
- **Reports** (new Settings sub-screen, not a 6th nav tab): category/source breakdown + income-vs-expense trend, week/month/custom range (custom reuses a new shared `DateRangeSection` component, also now used by `BudgetScreen` instead of its old inline date-picker code). No charting library — bars are built from plain Compose `Box`/`Modifier.fillMaxWidth(fraction)`/`fillMaxHeight(fraction)`. Transfers are excluded from Reports entirely.

**Verification needed on device**: make a transfer and confirm both account balances update, the streak does *not* move, and it shows correctly (and neutrally) in History; confirm transferring to the same account is rejected and deleting an account used in a transfer is blocked; confirm all 6 new achievements unlock correctly and PLANNER survives clearing the budget; open Reports, switch Week/Month/Custom, and confirm the numbers match History for the same period with transfers never appearing.

## Migration Notes (2026-08-04)
- Project moved back from the `K3n5h` machine to `C:\Users\IRSRnDSteve\AndroidStudioProjects\SpendStreak`. The copy landed nested inside the old (stale, pre-Phase-5) project folder rather than replacing it — diffed both copies (only difference: the newer copy was strictly ahead — icon, signing config, keystore, updated docs; nothing unique was left behind in the stale copy), then promoted the newer one up and removed the stale leftovers.
- Cleared `.gradle/`, `.idea/`, `.kotlin/`, `build/`, and `app/build/` since they were cached from the other machine (regenerate automatically, per the note above) — `local.properties` `sdk.dir` corrected to this machine's SDK path.
- `app/release/app-release.apk` (the previously verified signed release build) was left untouched — it's a real deliverable, not a cache.
- Gradle build not re-verified from this session yet (CLI Gradle is not usable in this environment — see below); do that first via Android Studio before continuing.

## Key Decisions Log
- 2026-08-03: App concept confirmed — gamified expense tracker, minimalist/clean-corporate style, habit-based leveling (not spend-amount-based)
- 2026-08-03: Full budgeting (income + savings goals + budget caps) explicitly deferred to v2 to protect timeline at 2 hrs/week beginner pace
- 2026-08-03: App name chosen: **SpendStreak**
- 2026-08-03: 5-screen v1 set confirmed: Dashboard, Add Expense, History, Level & Achievements, Settings
- 2026-08-03: XP/leveling formula set: +15 XP per logged expense, level-up threshold = level × 100 XP
- 2026-08-03: Streak rule set: streak increments once per calendar day logged; a missed day resets it to 1 (not 0) on the next log
- 2026-08-03: Achievements are computed live from expense/progress data rather than stored as their own table — simpler, and their unlock conditions only grow so there's no "losing" one to reconcile
- 2026-08-04: Income, Accounts, and Budget pulled forward from v2 into v1 (see `PHASE_0_DESIGN.md`). Budget is one overall limit at a time; Income logging counts toward streak/XP; the schema bump uses destructive migration (pre-launch, acceptable data loss on-device)
- 2026-08-05: Added Transfers, 6 more achievements, and a Reports screen (see `PHASE_0_DESIGN.md`). Transfers do **not** count toward streak/XP (unlike Income). Reports lives in Settings, not a new bottom-nav tab, and excludes transfers. Another destructive schema bump (2→3, same accepted tradeoff).

## Estimated Timeline
Rough estimate for v1 MVP at ~2 hrs/week, zero prior coding experience: **16–22 weeks** (this includes the fuller data model for streaks/XP/achievements even though income/goals are deferred). Timeline will be refined as we move through phases.

---

## Resuming on a Different Machine

**What to bring over:** the whole project folder — currently
`C:\Users\IRSRnDSteve\AndroidStudioProjects\SpendStreak`. This is not a git repo yet, so it's a plain folder copy (USB drive, cloud storage, etc.), not a `git clone`. **Also bring the `keystore/` folder** — it's gitignored (correctly) but that means a plain folder copy is the only thing that carries it over; without it, release builds on the new machine sign with a different key.

**Lesson from this transfer:** last time, the copied folder landed *inside* the old project folder instead of replacing it, leaving two nested, out-of-sync copies of SpendStreak that had to be manually reconciled (diffed to confirm no unique work was in the stale copy, then the newer one promoted up and the stale one removed). When copying to a new machine, replace the destination folder entirely rather than pasting into/next to an existing one — or better, use the git option noted below, which makes this a non-issue.

**Safe to skip / will regenerate automatically** (no need to copy, and copying them can actually cause path conflicts on the new machine):
- `.gradle/` (build cache)
- `app/build/` (build output)
- `.idea/` (IDE workspace state — Android Studio recreates this on open)
- `local.properties` (points at *this* machine's Android SDK path — Android Studio rewrites it automatically the first time the project opens on the new machine; if it doesn't, set `sdk.dir` to wherever the new machine's SDK lives)

**Everything else** (`app/src/`, `docs/`, `gradle/libs.versions.toml`, `build.gradle.kts`, `gradle.properties`, `settings.gradle.kts`, `gradlew`/`gradlew.bat`) should be copied as-is.

**On first open on the new machine:**
1. Let Android Studio Gradle-sync. It may prompt to download Android SDK Platform 37 (see `compileSdk` note below) — accept that.
2. If the build fails with `Using kotlin.sourceSets DSL to add Kotlin sources is not allowed with built-in Kotlin`, that's *not* a new bug — it's already worked around via `android.disallowKotlinSourceSets=false` in `gradle.properties`, which travels with the copied folder. If it resurfaces, that flag is the fix (context: [github.com/google/ksp/issues/2729](https://github.com/google/ksp/issues/2729)).

**Toolchain this project currently expects** (all pinned in `gradle/libs.versions.toml`, just listed here for quick reference): AGP 9.2.1, Kotlin 2.2.10, KSP 2.2.10-2.0.2, Room 2.8.4, Compose BOM 2026.02.01, `compileSdk` 37, `minSdk` 26, `targetSdk` 36.

**Immediate next step when you resume:** Phase 5 in progress — app icon and release signing config both done and verified (2026-08-03). Left: broader device/edge-case test pass (negative/zero amounts, special chars in notes, rotation state survival, streak-reset-after-missed-day, History with 100+ entries, long category names), and Play Store listing prep if you decide to publish.

**Worth considering:** putting this project under git (even just a local repo, no remote required) would make future transfers a lot cleaner than folder-copying — happy to set that up if you'd like, just say so.
