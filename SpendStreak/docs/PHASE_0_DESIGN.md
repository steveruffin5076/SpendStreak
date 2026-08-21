# SpendStreak — Phase 0: Product Design

**Status:** Approved 2026-08-03

## Concept
A gamified expense tracker. Users log expenses quickly, and the app rewards the *habit* of logging consistently — not how much or how little they spend. Streaks, levels, and achievements are earned by showing up daily, not by hitting a savings target.

## Style
**Revised 2026-08-03:** Retro Game / RPG-leveling aesthetic (pivoted from the original minimalist/clean-corporate direction after seeing it in practice — felt too flat for a habit/leveling app). Dark indigo-purple background, bold saturated accent colors (gold for XP/level, orange for streak, green for success), monospace bold typography with wide letter-spacing, blocky low-radius panels with thick borders, and a segmented retro-game-style XP bar instead of a standard progress indicator. No pixel-art font is bundled (would require downloading a font file) — monospace+bold is used as a stand-in; can revisit if a true pixel font is wanted later.

## Target Audience
General — anyone who wants a low-friction way to build a consistent expense-logging habit. No age or profession-specific assumptions; tone stays simple and approachable for all users.

## Scope (v1 vs v2)
- **v1:** Expense logging, streaks, XP/leveling, achievements, local history, settings, **income tracking, accounts (payment-mode tags), a single spending budget (monthly or custom date range)** — pulled forward from v2 on 2026-08-04 — **and transfers between accounts, an expanded achievement set, and a Reports screen** — added 2026-08-05, see Key Decisions Log.
- **Deferred to v2:** Savings goals, per-category budgets, multiple concurrent budgets.

## Monetization
None. No ads, no in-app purchases, no subscriptions in v1.

## v1 Screens (5 bottom-nav tabs, plus 3 sub-screens reached from Settings)
1. **Dashboard** — current streak, level/XP progress, budget progress (if set), balance, weekly summary.
2. **Add** — fast entry for an Expense, Income, or Transfer. Expense/Income get a category/source picker plus a single account picker; Transfer gets a FROM/TO account picker instead (no category). All three take an optional note.
3. **History** — chronological list merging Expenses, Income, and Transfers (transfers shown neutrally, no +/− sign).
4. **Level & Achievements** — XP progress, unlocked/locked achievements across expenses, income, budgeting, and transfers.
5. **Settings** — preferences, data management, about/credits, plus three navigable sub-screens:
   - **Accounts** — manage payment-mode tags (Bank/Cash/Credit Card/custom), each showing a computed balance.
   - **Budget** — set a single spending limit (monthly-recurring or a custom date range) and view progress against it.
   - **Reports** — category/source breakdown and an income-vs-expense trend, for a selectable range (week/month/custom). Transfers are excluded — they're not real spending or earning.

## App Identity
- **Name:** SpendStreak
- **Package ID:** `com.spendstreak.app`

## Key Decisions Log
- 2026-08-03: App concept confirmed — gamified expense tracker, minimalist/clean-corporate style, habit-based leveling (not spend-amount-based)
- 2026-08-03: Full budgeting (income + savings goals + budget caps) explicitly deferred to v2 to protect timeline at 2 hrs/week beginner pace
- 2026-08-03: App name chosen: SpendStreak
- 2026-08-03: 5-screen v1 set confirmed: Dashboard, Add Expense, History, Level & Achievements, Settings
- 2026-08-03: Target audience confirmed as general/broad, no niche targeting
- 2026-08-03: Package ID set to `com.spendstreak.app` (changed from Android Studio default `com.hollowpines.spendstreak`)
- 2026-08-03: Style pivoted from minimalist/clean-corporate to Retro Game/RPG-leveling after seeing the plain version built — dark palette, bold monospace type, chunky bordered panels, segmented XP bar
- 2026-08-04: Income, Accounts, and Budget pulled forward from v2 into v1. Budget is a single overall limit at a time (not per-category, not multiple concurrent budgets) — monthly-recurring or one custom date range. Logging Income counts toward the daily streak/XP habit loop exactly like logging an Expense. The Room schema bump for this uses destructive migration (acceptable pre-launch) rather than a real migration.
- 2026-08-05: Added Transfers (move money between your own accounts), 6 more achievements (Income/Budget/Transfer-themed), and a Reports screen. Transfers do **not** count toward the daily streak/XP habit loop (unlike Income). Reports lives in Settings (not a 6th bottom-nav tab), shows both a category/source breakdown and an income-vs-expense trend, supports week/month/custom ranges, and excludes transfers entirely. Another destructive schema bump (v2→v3, another accepted pre-launch data wipe) — same tradeoff as the previous bump.
