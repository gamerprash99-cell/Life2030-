# LifeOS Startup, Navigation & Reminders Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the custom "Unlocking your data…" startup screen, cut startup lag, fix bottom-nav/Profile/Home navigation bugs, and drop the unnecessary exact-alarm dependency — without architecture rewrites.

**Architecture:** Keep Compose Navigation (single NavController, nested `root_tabs` graph), Room, manual DI. Replace custom BrandSplash with Android SplashScreen API; make DB Ready not wait on reminder re-arm; standardize bottom-bar navigate pattern; schedule reminders with permission-free inexact alarms.

**Tech Stack:** Kotlin, Jetpack Compose, navigation-compose 2.8.4, Room 2.6.1 + SQLCipher, AlarmManager, androidx.core:core-splashscreen.

**Spec:** User request (AUDIT → ROOT CAUSE → FILE PLAN → IMPLEMENT → TEST → DIFF REVIEW → FINAL VERIFY), sections 1–12.

## Global Constraints

- Do NOT rewrite architecture, replace Compose Navigation, or change Kotlin/Room/DI/repositories/ViewModels unnecessarily.
- Do NOT remove working features; do NOT add cloud/Firebase/telemetry/network/AI dependencies.
- Do NOT use Thread.sleep, delay(), artificial animations, or fake loading.
- Preserve App Lock, encrypted DB init, Room as source of truth, offline-first.
- Do NOT redesign Home/Profile/Tasks/Habits/Expenses/Timeline/Diary UI.
- Ignore floating call/PiP overlay in screenshots (external to LifeOS).
- No duplicate NavHost, no second navigation stack, no suppressed errors, no disabled tests.

## Root Causes (from audit)

1. **"Unlocking your data…":** `MainActivity.BrandSplash()` is a custom full-screen composable gated on `databaseState != Ready` (commit 2668f33). Not Android's native splash.
2. **Startup lag:** `LifeOSApplication.launchDatabaseOpen` runs `reminderRepository.rebuildAllActive()` **before** flipping `Ready`, so UI waits on re-arming every alarm.
3. **Bottom-nav re-tap → Home:** `LifeOSBottomBar` re-tap branch calls `popBackStack(findStartDestination().id, false)` which pops **to Home** for any selected tab (Habits→Habits lands on Home; Tasks→Tasks likewise). Home icon unreliability and Profile→Back→Tasks weirdness cascade from this wrong pattern.
4. **Exact-alarm nag:** `ReminderScheduler` prefers `setAlarmClock` (exact-alarm special access); Settings shows "Alarms may be delayed · Allow exact alarms". LifeOS reminders do not need exact-to-the-minute delivery.

## Review Focus

- Cold launch with App Lock enabled still shows App Lock (not bypassed) after splash dismiss.
- DataKeyErrorScreen still reachable when DB key fails (splash must dismiss on Error).
- Re-tap Tasks/Habits stays on that tab; Home re-tap stays on Home.
- Profile → Back returns to prior real destination with correct selected tab.
- Reminders schedule without SCHEDULE_EXACT_ALARM; no SecurityException; edit/delete still cancels/replaces (same PendingIntent).
- Unit tests (BottomNavItemsTest, ReminderScheduleCalculatorTest, schema tests) still pass.

---

### Task 1: Native splash replaces BrandSplash

**Files:**
- Modify: `app/build.gradle.kts` (add core-splashscreen)
- Modify: `app/src/main/res/values/themes.xml`
- Modify: `app/src/main/AndroidManifest.xml` (MainActivity theme; AlarmFullScreen theme)
- Modify: `app/src/main/java/com/lifeos/app/MainActivity.kt`

**Interfaces:**
- Consumes: `LifeOSApplication.databaseState: StateFlow<DatabaseInit>`
- Produces: no new public API; removes private `BrandSplash`

- [x] Add `implementation("androidx.core:core-splashscreen:1.2.0")`
- [x] Theme: `Theme.LifeOS` stays main; add `Theme.LifeOS.Splash` (parent `Theme.SplashScreen`, white background, launcher icon, `postSplashScreenTheme` → `Theme.LifeOS`); MainActivity uses Splash theme; application + AlarmFullScreenActivity keep `Theme.LifeOS`.
- [x] `MainActivity.onCreate`: call `installSplashScreen()` first; `setKeepOnScreenCondition { databaseState is Initializing }` (dismiss on Ready **or** Error).
- [x] Remove `BrandSplash` composable and its `initState != Ready` branch; Initializing safety branch = empty themed `Surface` (no text) only if splash already gone; Error keeps `DataKeyErrorScreen`.
- [x] Verify: no `"Unlocking your data"` string remains.

### Task 2: DB Ready no longer blocked by reminder re-arm

**Files:**
- Modify: `app/src/main/java/com/lifeos/app/LifeOSApplication.kt`

- [x] In `launchDatabaseOpen`: after `warmUpOpen` succeeds, set `_databaseState.value = Ready` **first**, then `appScope.launch { runCatching { rebuildAllActive() } }` (non-blocking safety net; BootReceiver still covers boot/time changes).
- [x] On failure: set Error as today (splash keep-condition must dismiss on Error — covered in Task 1).

### Task 3: Bottom bar — single standard navigate path

**Files:**
- Modify: `app/src/main/java/com/lifeos/app/ui/components/LifeOSBottomBar.kt`
- Test: `app/src/test/java/com/lifeos/app/ui/components/BottomNavItemsTest.kt` (existing tests must pass)

- [x] Remove `if (selected) popBackStack(findStartDestination)` branch.
- [x] Always: `navigate(route) { popUpTo(findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true }`.
- [x] Keep route-driven `selected` from `currentBackStackEntryAsState` (single source of truth).
- [x] Do not touch LifeOSNavHost structure (single NavHost, nested graph stays).

### Task 4: Profile back stays synchronized

**Files:**
- Verify only: `LifeOSNavHost.kt` Profile `onBack = popBackStack()` (already correct)

- [x] Confirm no second stack/boolean screen switching exists (audit already: none).
- [x] Task 3 fix restores correct selected tab after Profile → Back → Tasks/Habits.

### Task 5: Reminders without exact-alarm dependency

**Files:**
- Modify: `app/src/main/java/com/lifeos/app/core/reminders/ReminderScheduler.kt`
- Modify: `app/src/main/java/com/lifeos/app/ui/settings/SettingsScreen.kt` (RemindersCard)
- Modify: `app/src/main/AndroidManifest.xml` (remove SCHEDULE_EXACT_ALARM)

- [x] `schedule()`: always `alarmManager.setAndAllowWhileIdle(RTC_WAKEUP, triggerAt, pendingIntent)` — no `setAlarmClock`, no `canScheduleExactAlarms` branch. Never throws for missing exact-alarm access.
- [x] Remove Settings exact-alarm launcher + "Alarms may be delayed · Allow exact alarms" button; keep POST_NOTIFICATIONS request path and global toggle.
- [x] Remove `SCHEDULE_EXACT_ALARM` from manifest (keep POST_NOTIFICATIONS, VIBRATE, USE_FULL_SCREEN_INTENT).
- [x] Keep PendingIntent identity, cancel/replace, BootReceiver, Room source of truth, past-skip, no-duplicate logic unchanged.

### Task 6: Verify

- [x] `gradle :app:assembleDebug`
- [x] `gradle :app:testDebugUnitTest`
- [x] `gradle :app:lintDebug`
- [x] Diff review vs checklist; append UPDATE.md (no history rewrite).

### Task 7: Git branch, commits, PR

- [x] Branch `fix/startup-navigation-reminders` from current HEAD.
- [ ] Atomic commits per task; push with user-provided credentials; open PR to `main`.
