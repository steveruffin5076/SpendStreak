# SpendStreak — Market Research & Feature Plan Session

This document summarizes a single extended session covering: Play Store market research for expense-manager apps, a Remove/Enhance/Add roadmap built from that research and applied to SpendStreak, and a first round of implementation. It complements `PHASE_0_DESIGN.md` (original product design) and `PHASE_STATUS.md` (phase-by-phase build log) — this file is the record of the research-driven roadmap and the QC/feature-request cycles that followed it.

The full, living plan (with per-item Why/What/Cost/Files detail and the current build-order table) lives outside this repo at the Claude Code plan file used during the session. This document is the narrative summary for anyone picking up the project later.

---

## 1. Market research (Play Store expense-manager apps)

Research covered top expense trackers (Money Manager, Spendee, Money Lover, Wallet by BudgetBakers, Monefy), the closest gamified competitor (Fortune City), and aggregate sentiment from YNAB/Goodbudget/PocketGuard/Rocket Money/Monarch/r-personalfinance-style discussion.

**Key findings:**
- **Friction, not missing features, is the #1 reason people abandon an app.** Never make logging slower to add a feature.
- **The #1 complaint pattern**: apps market "free," then paywall basic things — categories, export, reports — within the first week. Read as "my own data held hostage."
- **The #2 complaint pattern**: unreliable multi-device sync — duplicate entries, stuck sync states, silent data loss.
- **Most-wanted features, ranked** (after low-friction entry): automation (bank/SMS/receipt) → reliable sync → uncapped core features → recurring bills/reminders → shared/family budgets → multi-currency → custom subcategories → clean UI that doesn't regress on redesign → export/backup.
- **Gamification risk case study**: Fortune City (the closest comparable app) is hated for rewarding *spending* — its game mechanics unlock rewards for buying more, which pushes the opposite of a healthy money habit.

## 2. Should SpendStreak's Level & Achievements system be removed?

**No.** It's the app's core differentiator, not incidental scope — every competitor studied is a generic ledger competing on feature count, while "make logging a daily habit" is a distinct, validated market segment. Verified in code: XP is a flat +15 per logged Expense/Income regardless of amount, all 12 achievements are count/streak/diversity/boolean-based (never amount-based), and Transfers/Budget-setting are deliberately excluded from XP — structurally the opposite of Fortune City's "rewards spending" problem. The one real gap found (XP uncapped per day, so spamming trivial entries yields unlimited XP) was fixed as a small safeguard rather than used as a reason to remove the system.

## 3. The Remove / Enhance / Add roadmap

Built directly from the research findings, cross-referenced against SpendStreak's actual code (not guesses):

**Remove:** a non-functional "Daily Reminder" toggle; the hardcoded `"RM "` currency string scattered across 7+ files; left the hand-rolled navigation as-is for now (not a user complaint).

**Enhance:** a real Room migration strategy (the database had only ever used destructive fallback — two prior schema bumps wiped all on-device data, "accepted pre-launch," not acceptable once real users have real data); per-row edit/delete for Expense/Income/Transfer; user-editable categories and income sources; a small XP anti-spam daily cap.

**Add (kept in scope):** recurring transactions + reminders; multi-currency; a home-screen widget; a level-up cosmetic-unlock system (the user's own idea, folded into the plan) — new theme skins and titles unlocked by level, deliberately cosmetic-only so it can never become the same "feature held hostage" complaint the research flagged, just re-gated by grind instead of by paywall.

**Dropped from scope:** shared/family budgets — not a "feature" but a backend/sync/auth project, the exact complexity category the research's #2 complaint warns about when done wrong.

## 4. QC rounds

Three rounds of user testing on-device produced real bug reports and additional feature requests, each folded into the living plan:

- **Round 1**: Reports screen's weekly trend chart was silently cutting off 2 of 7 days (LazyRow with no scroll indicator — data was correct, layout wasn't); default account seeding changed from three accounts to one ("Cash"); a first pass at a 4-column category/account grid; raised the max transaction amount to 999,999,999.99; scoped a "named budgets with kept history" feature (supersedes the original single-budget assumption); confirmed the level-15/20 title test isn't realistically reachable by hand-tapping (~700–1,267 logs needed).
- **Round 2 (screenshot-driven)**: the 4-column grid from Round 1 turned out to look bad in two different ways — long category words ("TRANSPORT", "ENTERTAINMENT") truncated illegibly, while short account names left conspicuous empty grid slots. Redesigned as: a `FlowRow` for Account/Transfer chips (content-sized, wraps only when needed), and a bottom-sheet picker with a 2-column grid for Category/Income-Source (room for full, un-truncated, possibly-2-line labels) — validated against Material Design guidance and how comparable apps (Wallet by BudgetBakers, Spendee) handle category selection.
- **Round 3**: full category add/edit/delete, requested alongside the picker redesign — built directly into the picker sheet (an EDIT toggle, tap-to-rename, delete blocked if a category has linked transactions) rather than a separate Settings screen.

## 5. What shipped this session

- Removed the dead Daily Reminder toggle; centralized currency formatting into `formatCurrency()`/`currentCurrencySymbol()`.
- **Level-up cosmetic unlocks**: 4 theme skins (Retro Gold, Cyber Teal, Sunset Arcade, Neon Grid) unlocked at levels 1/3/5/10, plus cosmetic titles at 15/20 — all derived live from level, persisted via SharedPreferences, zero schema dependency.
- Fixed the Reports weekly-trend chart (all 7 days now always visible, no silent scroll).
- Changed default account seeding to a single "Cash" account.
- Raised the max transaction amount to 999,999,999.99.
- **Full-world multi-currency picker**: backed by the JDK's own `java.util.Currency` table (no hand-maintained list, no new dependency) — a searchable dialog over all ISO 4217 currencies, propagated live via a `CompositionLocal` so every screen updates immediately on change.
- **Picker redesign**: `FlowRow` for Account/Transfer chips; a `ModalBottomSheet`-based `CategoryPickerSheet` for Category/Income-Source with a 2-column grid, full labels, and emoji icons (system emoji font, no icon library dependency). The old fixed-4-column `ChipGrid` component was deleted once nothing used it anymore.
- **Real Room migration strategy**: replaced blind reliance on `fallbackToDestructiveMigration` with a proper `MIGRATIONS` array wired via `.addMigrations(...)`, with the destructive fallback demoted to a safety net rather than the default path.
- **Category CRUD (the biggest single change)**: a new `Category` entity/DAO/Repository; `Expense.category`/`Income.source` converted from free-text `String` to a `categoryId` foreign-key-style column; a real `Migration(3, 4)` that creates the `categories` table, seeds the 10 default categories/sources, and recreates the `expenses`/`income` tables with backfilled `categoryId` values matched by name. Add/edit/delete lives inside the picker sheet (an EDIT toggle, rename dialog with a curated emoji picker, delete blocked if a category has linked transactions — mirroring the existing Account-deletion rule exactly). Ripple fixes followed through `Achievement.kt` (distinct-category counting), `ReportsScreen.kt` (breakdown-by-category), and `HistoryScreen.kt` (category display + a new deterministic per-category color drawn from the app's own theme palette, so no user choice can ever clash with the Retro theme).

## 6. Still pending (per the current build order)

- Named budgets with kept history (real schema change, needs on-device verify of the category migration first as a matter of sequencing discipline, not a hard technical dependency).
- CSV export (no dependency — a good next quick win).
- Per-row edit/delete for Expense/Income/Transfer.
- XP anti-spam daily cap.
- Recurring transactions + reminders (the single biggest remaining item — first-ever background-work feature in the app, needs a new WorkManager dependency and a runtime notification permission).
- Home-screen widget (kept in scope, lowest priority of what was kept).
- Multi-currency true per-transaction conversion (explicitly deferred — only the "pick a display symbol" half was built; real conversion is a much bigger lift, build only if actually needed).

## 7. Key decisions worth remembering

- **Habit-based gamification stays** — it's the product's identity, and the code already avoids the one real risk pattern found in this niche.
- **Every future schema change must ship with a real `Migration` object** — no more destructive wipes on real user data, now that the infrastructure exists.
- **Category/account/currency management deliberately lives close to where it's used** (in the picker sheet, in Settings) rather than in separate management screens, to keep the surface area small for a beginner to maintain.
- **Cosmetic rewards must never gate a real feature** — this guardrail was applied explicitly to the level-up unlock system and is worth re-applying to any future gamification idea.
