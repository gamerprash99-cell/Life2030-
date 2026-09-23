# LifeOS Task-Reminder, Navigation & Notifications-Permission Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the Back-stack bug (Home → Profile → Habits & Routines → Back must return to Profile, then Home), make the task reminder behave exactly like the existing habit reminder (same full-screen alarm UI, editable from the task card, correct handling when edited/time changes/disabled), ask for `POST_NOTIFICATIONS` only when a timed reminder is actually created/enabled (never at startup), and eliminate "zombie" past-trigger reminders that silently never fire — without touching the Room schema.

**Architecture:** Keep Compose Navigation (single NavController, nested `root_tabs` graph — do NOT duplicate stacks), Room v4 (no schema/migration change), manual DI, AlarmManager inexact `setAndAllowWhileIdle`. All reminder state keeps flowing through `ReminderEntity → ReminderRepository → ReminderScheduler → AlarmReceiver → AlarmFullScreenActivity` (already shared by task + habit; do not fork it).

**Tech Stack:** Kotlin, Jetpack Compose, navigation-compose 2.8.4, Room 2.6.1 + SQLCipher, AlarmManager, core-ktx permission APIs.

**Spec:** User request phases 2–11 (nav, notification permission, task reminder parity, shared architecture, lifecycle, DB safety, UI/UX, offline/security, testing, docs/PR).

## Global Constraints

- Do NOT rewrite architecture, replace Compose Navigation, or change Kotlin/Room/DI/repositories unnecessarily.
- Do NOT remove working features; do NOT add cloud/Firebase/network/telemetry/AI dependencies; offline-first stays.
- Do NOT change `AppDatabase` version, schema, entities, or migrations; v4 stays.
- Never request `POST_NOTIFICATIONS` at startup or app open — only at the moment a timed reminder is created/enabled; never re-request when granted; no `SCHEDULE_EXACT_ALARM`.
- No duplicate NavHost, no second navigation stack, no suppressed errors, no disabled tests.
- Alarm-firing behavior stays: inexact, Doze-aware, PendingIntent identity `lifeos://reminder/<id>` replaces/cancels cleanly.

## Root Causes (from audit)

1. **Nav Back-stack bug** (`LifeOSBottomBar.kt:100`): every tab tap uses the canonical `popUpTo(findStartDestination()) { saveState=true }` pattern. From an overlay destination OUTSIDE the `root_tabs` graph (Profile), tapping a tab pops the overlay (Profile) off the stack, so Back from Habits lands on Home instead of Profile, and saveState/restoreState can flash/ghost the overlay.
2. **Zombie past-trigger reminders** (`ReminderRepository.kt:78`, `ReminderScheduler.kt:54`, `rebuildAllActive` `:174`, `rebuildFromMirrors` tail): a row whose trigger is in the past (e.g. a DAILY reminder created after today's time, or alarm heavily delayed) is left `enabled` with a past `nextTriggerAtEpochMillis` and is silently skipped — it can never fire.
3. **Permission only in Settings** (`SettingsScreen.kt:268`): `POST_NOTIFICATIONS` is requested only from the reminders toggle, never when the user actually creates/sets a timed reminder in Tasks/Habits/HabitDetail; permanently-denied toggles silently no-op.

## Review Focus

- Home → Profile → Habits tab → Back → Profile → Back → Home: each destination exactly once; no duplicate tabs.
- Tab tap from an overlay pushes the tab on top (Back returns through the overlay); tapping an already-beneath tab pops back to it instead of duplicating.
- Task reminder shows the identical full-screen alarm UI as habit ("Task reminder [title] STOP 5m 10m 15m 30m") — verify existing `AlarmFullScreenActivity` path, no change needed.
- Editing a task reminder (time change, repeat change, clear) reuses `syncReminderForTask` (same stable id → replaces/cancels, no duplicates).
- `POST_NOTIFICATIONS` dialog appears only when a timed reminder is being created/set; grant → proceeds; deny → nothing is scheduled; permanently denied → routes to app Settings.
- Past-due recurring triggers advance to the next future occurrence; past-due one-shots are disabled (never a silent zombie); unit tests cover the math.
- `BottomNavItemsTest`, `ReminderScheduleCalculatorTest`, schema tests still pass; compile/lint clean.

---

### Task 1: Navigation — overlay-aware bottom-bar tap

**Files:**
- Modify: `app/src/main/java/com/lifeos/app/ui/navigation/Screen.kt` (add `internal const val ROOT_TABS_GRAPH`)
- Modify: `app/src/main/java/com/lifeos/app/ui/navigation/LifeOSNavHost.kt` (use shared const)
- Modify: `app/src/main/java/com/lifeos/app/ui/components/LifeOSBottomBar.kt`
- Test: `app/src/test/java/com/lifeos/app/ui/components/BottomNavItemsTest.kt` (existing tests must pass)

- [ ] Expose `ROOT_TABS_GRAPH` as `internal const val` in `Screen.kt`; reference it from `LifeOSNavHost`.
- [ ] In `LifeOSBottomBar` onClick: if current destination hierarchy contains the tabs graph (in-app tab switch) keep the canonical `popUpTo+saveState+restoreState+launchSingleTop`; if the current destination is an overlay (not in the tabs graph), navigate the tapped tab with `launchSingleTop` and NO `popUpTo`, deduping first via `popBackStack(tabRoute, false)` when the tab already exists beneath the overlay.
- [ ] Keep route-driven `selected` from `currentBackStackEntryAsState` unchanged; do not touch NavHost structure.

### Task 2: Notification permission gate at reminder creation

**Files:**
- Add: `app/src/main/java/com/lifeos/app/ui/components/ReminderPermissionHost.kt` (companion doc of the Settings UX)
- Modify: `app/src/main/java/com/lifeos/app/ui/tasks/TasksScreen.kt` (NewTaskDialog Add)
- Modify: `app/src/main/java/com/lifeos/app/ui/habits/HabitsScreen.kt` (add-habit confirm)
- Modify: `app/src/main/java/com/lifeos/app/ui/habits/HabitDetailScreen.kt` (ReminderCard set/change)
- Modify: `app/src/main/java/com/lifeos/app/ui/settings/SettingsScreen.kt` (permanently-denied → open app settings)

- [ ] `rememberReminderPermissionHost()`: own launcher; API<33 → run actions immediately; granted → run immediately; else show explanation dialog (Allow / Not now); permanently denied → "Open Settings" via `PermissionManager.openAppSettings`; on OS grant run the held action; on deny drop it. Never request at startup.
- [ ] NewTaskDialog: gate the Add action only when `reminderTime != null`.
- [ ] Habits add-habit dialog: gate the confirm action only when `reminderTime != null`.
- [ ] HabitDetailScreen ReminderCard: gate time picker confirm + repeat change (arm paths); Clear stays ungated.
- [ ] Settings RemindersCard: when toggling ON and permission is permanently denied, `openSettings()` instead of a silent `request()`.

### Task 3: Task reminder editing + lifecycle parity

**Files:**
- Modify: `app/src/main/java/com/lifeos/app/data/repository/TaskRepository.kt`
- Modify: `app/src/main/java/com/lifeos/app/ui/tasks/TasksScreen.kt` (TasksViewModel + TaskCard + editor dialog)

- [ ] `TaskRepository.setReminder(taskId, reminderEpochMillis, repeatType)` mirroring `HabitRepository.setReminder` (upsert mirror + `syncReminderForTask`).
- [ ] `TaskRepository.getReminderFor(taskId)` reads the row for the editor's initial repeat type.
- [ ] `TasksViewModel.updateReminder` + `reminderRepeatFor`.
- [ ] TaskCard: tappable reminder chip (Notifications icon + time) when a reminder is set; opens `TaskReminderEditorDialog` (time picker + repeat selector + Save/Clear), gated by the permission host when arming, insets-safe like NewTaskDialog.
- [ ] Lifecycle checks: create (Add) ✓ existing; edit/change/clear → new path; complete → `deleteForTask`+cancel ✓ existing; delete → ✓ existing; reboot → BootReceiver ✓; snooze leave real occurrence untouched ✓; no duplicates (same PendingIntent id) ✓.

### Task 4: Zombie past-trigger normalization

**Files:**
- Modify: `app/src/main/java/com/lifeos/app/core/reminders/ReminderScheduleCalculator.kt` (pure, tested)
- Modify: `app/src/main/java/com/lifeos/app/data/repository/ReminderRepository.kt`
- Test: `app/src/test/java/com/lifeos/app/core/reminders/ReminderScheduleCalculatorTest.kt`

- [ ] Add `nextFutureOccurrenceMillis(repeatType, repeatDaysCsv, fromMillis, nowMillis)`: next occurrence > `nowMillis` (fast-forward recurring), `null` for one-shots/unarmable.
- [ ] `ReminderRepository.armNextOccurrence(reminder)`: snooze-return future → schedule as-is; real trigger future → schedule; recurring past → advance + persist + schedule; one-shot past → disable + cancel (no zombie).
- [ ] Reuse in `syncReminderForEntity`, `rebuildAllActive`, `rebuildFromMirrors` tail, and `handleFired` recurring branch (keep `handleFired` ONCE disable + clock-moved guard).

### Task 5: DB safety — no change

- [ ] No `AppDatabase` version, schema, entity, or migration change (verify by diff + schema test).

### Task 6: Verify (no emulator in sandbox — compile + JVM tests + lint + reasoning)

- [ ] `ANDROID_HOME=/opt/android-sdk gradle :app:compileDebugKotlin`
- [ ] `ANDROID_HOME=/opt/android-sdk gradle :app:assembleDebug`
- [ ] `ANDROID_HOME=/opt/android-sdk gradle :app:testDebugUnitTest`
- [ ] `ANDROID_HOME=/opt/android-sdk gradle :app:lintDebug`
- [ ] Diff review vs checklist; grep for leftover symbols; no suppressions.

### Task 7: Docs, commit, PR

- [ ] Append UPDATE.md (root causes, fixes, files, verification) — no history rewrite.
- [ ] Stage only intended files; atomic commits on `fix/startup-navigation-reminders`.
- [ ] STOP — ask user for GitHub PAT before any push; never write PAT to disk. Open PR to `main`.