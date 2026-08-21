# SpendStreak — Session Handoff

Paste this whole file as your first message in a new Claude conversation (or attach it) so the new session has full context. Written 2026-08-13.

## What SpendStreak is

Gamified Android expense-tracker app, package `com.spendstreak.app`, built with Kotlin + Jetpack Compose (Material3) + Room (KSP), MVVM with a single `SpendStreakViewModel`. Goal: publish publicly on the Google Play Store.

## Where things stand right now

Everything is done and working **except** AdMob banner ads, which are the active, unresolved issue.

### Done and working
- Recurring transactions + reminders (WorkManager periodic + one-off checks, notification action buttons).
- Code review pass completed and 10 findings applied (see "Permanent code-review fixes" below).
- Home-screen widget (Jetpack Glance) was built, then **fully removed** per user request ("not friendly in practice") — no trace of it left in the codebase.
- 4 on-device bugs reported and fixed:
  1. Text clipping in Settings' "Recurring Reminders" row — fixed with `Modifier.weight(1f)` (no explicit `weight` import — see gotcha below).
  2. Reminders not auto-triggering for a newly-added rule until toggle off/on — fixed by adding an immediate one-off WorkManager check (`ReminderScheduler.runReminderCheckNow`) fired whenever a recurring rule is added/edited.
  3. No confirmation dialog before "Clear All Data" — added an `AlertDialog`.
  4. Widget balance not updating live — moot, since the widget was removed entirely.
- "ABOUT & CREDITS" panel updated: removed "Built with Kotlin, Jetpack Compose, and Room.", added "A Besi Works Production" / "Designed and developed by Besi" / "Copyright © 2026 Besi Works".
- AdMob banner ad code written and wired into Settings, Reports, and History screens (secondary screens only — never Dashboard or Add Expense, per explicit user decision).

### Active, unresolved issue: AdMob test banner not appearing

**Symptom:** User reports no test banner shows up on Settings/Reports/History screens.

**Debugging so far (following the systematic-debugging Iron Law — no fixes without root-cause evidence):**
1. Added diagnostic `Log.d`/`Log.w` logging throughout `ads/AdsInitializer.kt` and `ads/BannerAdView.kt`, tag `"SpendStreakAds"`.
2. First Logcat dump (unfiltered) showed: `requestConsentInfoUpdate` failed with UMP error code 3 ("Publisher misconfiguration... no form(s) configured for the input app ID"). This is because no "Privacy & messaging" consent campaign is set up yet in the AdMob console for this app — **not fatal**, the code's own `canRequestAds()`-gated fallback correctly proceeded past it (log showed `canRequestAds=true`, and `MobileAds.initialize()` visibly began loading its Dynamite module). Whether the ad actually finished loading was unconfirmed at that point (log was cut off).
3. User then pasted a second, much larger Logcat dump — but on inspection it contained **zero lines with the `SpendStreakAds` tag at all**. It was generic Android boot/system noise (Bluetooth, lmkd, Google Play services, etc.), meaning the filter wasn't actually applied when it was captured.
4. User then filtered Logcat properly (screenshot showing `SpendStreakAds` in the Logcat filter box, on the History screen) — result: **"All log entries are hidden by the filter"**, i.e. zero matches even for the very first log line (`requestConsentInfoUpdate: starting`) that `BannerAdView`'s `LaunchedEffect` should unconditionally emit on every render.

**Current working theory (not yet confirmed):** the app currently running on the emulator is a **stale build** — installed before the ad logging (and possibly before the ad code itself) was added — so nothing in the ad path ever executes. Instructed the user to:
1. Fully stop the app process on the emulator.
2. Do a real fresh **Run** (green ▶ button) in Android Studio — not "Apply Changes" — to force a genuine rebuild + reinstall.
3. Revisit History or Settings.
4. Re-check the `SpendStreakAds`-filtered Logcat.

**This is the next thing to check when resuming.** If log lines appear after a clean rebuild, resume analysis from there (last confirmed point: consent flow proceeds via fallback; unconfirmed whether `MobileAds.initialize complete`, `onAdLoaded`, or `onAdFailedToLoad` ever fires). If Logcat is still completely empty after a genuine clean rebuild, that's a new real finding — it would mean `BannerAdView` composable itself isn't being reached/rendered, and that needs its own investigation (e.g. confirm the emulator Logcat is attached to the right device — it showed "Pixel 7 (emulator-5554)" matching the screenshot, so that part checked out already).

### Deferred until ads are confirmed working
- Swap `BannerAdView.kt`'s `TEST_BANNER_AD_UNIT_ID` to the real Ad Unit ID `ca-app-pub-6165653121014687/2402284086` — **do this only right before Play Store publishing**, never during dev/testing (interacting with real ads on your own device counts as invalid traffic and risks suspending a brand-new AdMob account).
- Create a "Privacy & messaging" consent form/message in the AdMob console for this app (fixes the non-fatal error code 3 warning).

### Pre-publish checklist (not started)
1. Draft + host a Privacy Policy URL.
2. Fill in Play Console's Data Safety section.
3. Create the Play Console app listing.
4. Link the Play Store listing back into AdMob ("Add stores to your AdMob app").

## Key facts an assistant needs before touching code

- **Never explicitly import `weight`** (`androidx.compose.foundation.layout.weight`) — it's a `RowScope`/`ColumnScope` member function available implicitly inside `Row`/`Column` lambdas, not a top-level extension. Importing it explicitly causes `Cannot access 'val RowColumnParentData?.weight: Float': it is internal in file`. Confirmed no other file in the codebase does this.
- **Room migrations**: every schema change needs a real `Migration` entry in the array in `SpendStreakDatabase.kt`; current version is 7 (`recurring_transactions`). `fallbackToDestructiveMigration` is kept only as an unused safety net.
- **AdMob real App ID** (already wired into `AndroidManifest.xml`): `ca-app-pub-6165653121014687~6403490303`.
- **AdMob real Banner Ad Unit ID** (NOT wired in yet, kept only as a comment in `BannerAdView.kt`): `ca-app-pub-6165653121014687/2402284086`.
- **Google's official TEST App ID**: `ca-app-pub-3940256099942544~3347511713` (not currently used — real App ID is already safe to use with a test ad unit).
- **Google's official TEST Banner Ad Unit ID** (currently active in code): `ca-app-pub-3940256099942544/6300978111`.
- User's AdMob/Play Console email: waisian.chong@irs.com.my (per global memory).

## Active files for the ads feature

- `app/src/main/java/com/spendstreak/app/ads/AdsInitializer.kt` — UMP consent flow → `MobileAds.initialize()`, all gated behind `canRequestAds()`. Heavily logged for debugging (tag `SpendStreakAds`).
- `app/src/main/java/com/spendstreak/app/ads/BannerAdView.kt` — the `@Composable BannerAdView`, mounted in Settings/Reports/History. Currently uses the TEST ad unit ID.
- `app/src/main/AndroidManifest.xml` — has `INTERNET` permission + AdMob `APPLICATION_ID` meta-data.
- `app/src/main/java/com/spendstreak/app/ui/screens/SettingsScreen.kt`, `ReportsScreen.kt`, `HistoryScreen.kt` — each restructured with an outer non-scrolling `Column` + inner scrolling content + `BannerAdView(Modifier.fillMaxWidth())` as a final sibling/footer.
- `gradle/libs.versions.toml` / `app/build.gradle.kts` — `play-services-ads` (23.6.0) and `user-messaging-platform` (3.1.0) dependencies.

## Permanent code-review fixes already applied (for reference, not action items)

1. `SpendStreakViewModel.clearAllData()` now also clears recurring transactions.
2. `AccountDao`/`CategoryDao`'s "count transactions using X" queries now include recurring-transaction references, so deleting an account/category referenced by a recurring rule is correctly blocked.
3. `ReminderActionReceiver`'s LOG IT/SKIP DB-write coroutine wrapped in `catch (_: Exception) {}`.
4. `MainActivity.kt` re-arms the periodic reminder job on every launch if `remindersEnabled` (`LaunchedEffect(Unit)`).
5–9. Various minor fixes (see prior session transcript for exact list if ever needed — not currently load-bearing for any open task).
10. `ReminderPreferences.kt`, `ThemePreferences.kt`, `CurrencyPreferences.kt`, `SpendStreakDatabase.kt` all now share one `SPENDSTREAK_PREFS_NAME` constant from the new `util/AppPreferences.kt`, instead of each declaring their own `PREFS_NAME`.

## Existing plan file (separate, still open)

There's a saved plan at `C:\Users\IRSRnDSteve\.claude\plans\what-s-next-to-check-memoized-bumblebee.md` for a **different, not-yet-started feature**: adding in-place budget editing (vs. always creating a new budget-history entry when the "SAVE BUDGET" button is pressed). This is unrelated to the ads work and was not being actively worked on this session — pick it up separately if the user wants it next.

## Immediate next step when resuming

Ask the user: "Did you do a full clean rebuild + reinstall (green ▶ Run, not Apply Changes), reopen History/Settings, and check the `SpendStreakAds`-filtered Logcat again? What did it show?" Then continue the systematic-debugging trail from there — do not guess or apply a fix without fresh log evidence, per the user's explicit process preference established earlier this session.
