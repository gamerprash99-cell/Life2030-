# LifeOS — UPDATE

Change log for the `fix/audit-hardening` branch (UI/UX + navigation audit and redesign, 2026-09-17).

---

## 2026-09-18 — Audit hardening pass (P0 data safety, correctness, polish)

### P0: encrypted-database key handling is now non-destructive (`DatabasePassphraseProvider.kt`)
- **Before:** if the Android Keystore key/wrapped passphrase was missing or unreadable while a database file already existed, startup could generate a *new* passphrase. That made the existing encrypted database permanently unreadable — silent data loss.
- **After:** a pure decision table (`resolveAction`) encodes the contract: healthy install → **REUSE**; fresh install (no wrapped key, no DB file) → **CREATE_FRESH**; anything ambiguous (unreadable wrapped key, or existing DB with no wrapped key) → **FAIL_KEY_UNAVAILABLE**. The passphrase is never rotated and the database is never deleted. Failures throw `DatabaseKeyUnavailableException`, surfaced by a dedicated, non-destructive recovery screen.
- `AppDatabase` now opens `DatabasePassphraseProvider.DATABASE_NAME` so the provider and Room agree on one file name.

### Startup resilience
- `LifeOSApplication` now exposes a nullable `serviceLocator` plus `initializationError` and `retryInitialization()`; `ServiceLocator.get` is wrapped in `runCatching`.
- `MainActivity` gates on this: a locked/failed DB key shows `DataKeyErrorScreen` (Retry / Exit) instead of crashing or wiping data.

### App Lock (4-digit PIN + auto-lock)
- New `PinComponents` (`PinDots`, `PinKeypad`, `PinKeypadPanel`) with haptics and accessibility semantics.
- `AppLockSetupScreen` / `AppLockScreen` rewritten for an exactly-4-digit keypad with auto-advance and auto-submit; lockout countdown and Forgot-PIN recovery preserved.
- Auto-lock: `AUTO_LOCK_GRACE_MILLIS = 30s`; locking on `ON_STOP` and re-prompting after the grace period, with an "Auto-lock" toggle in Settings.

### Profile
- New `ProfileAvatar` (Coil `AsyncImage` with Person fallback). `ProfileScreen` supports a persisted profile photo (`OpenDocument` + `takePersistableUriPermission`), editable display name, App Lock/Backup/Settings rows, and an About dialog. Home header uses the avatar.

### Search
- `SearchScreen` rewritten with `SearchCategory`, `SearchHit`, debounced (300ms) querying, tappable results, and category-aware navigation. Fixes a compile error by using `NoteEntity.plainTextForSearch`.

### Dates & Timeline
- `DateTimeUtils.startOfLocalDayMillis` / `endOfLocalDayMillis` / `nowMinutesOfDay` added. `BuildTimelineUseCase` now uses **local** day boundaries (previously `epochDay * 86_400_000L`, which shifted users off UTC into the wrong calendar day). Timeline caps navigation at today and handles system Back.

### Habits
- Streaks are now schedule-aware (`HabitSchedule`, `isScheduled`, `parseCustomDays`): a Mon/Wed/Fri habit is no longer penalised for missing Tuesday, and an in-progress today does not break a run. `HabitRepository.computeAnalytics`/`computeHeatmap` use the schedule (off-days show as no-data). Legacy consecutive-day overloads retained.

### Tasks
- **Bug:** enum priorities are persisted as strings, so `ORDER BY priority` sorted alphabetically (HIGH < LOW < MEDIUM). Added an explicit `PRIORITY_ORDER` CASE expression for `observeForDay`/`observeAll`, and `observeOverdue(today, nowMinutes)` now includes due-today-past-time items.

### Notes
- `NotesListViewModel` with filters (All/Favorites/Archived/Trash) and folder chips; per-row actions (pin, favorite, archive, trash/restore, delete-forever).
- `NoteEditorViewModel.applyAiResult()` makes AI suggestions **explicit**: Generate title / convert to checklist / append prose, applied only when the user taps Apply (Copy and Close also available). This removes the earlier auto-apply behavior.

### Media capture
- `AudioCaptureScreen` (15-min cap), `VideoCaptureScreen` (5-min cap), and `CameraCaptureScreen` now handle system Back, bound/unbind the camera provider safely, cap duration, clean up partial files, and expose a discard action.

### Diary
- Removed the dedicated AI drafting path entirely (Section 23): entries are user-written and saved only on explicit Save; added delete confirmation and Back handling.

### Onboarding
- Replaced hardcoded colors with `MaterialTheme.colorScheme` tokens so onboarding follows light/dark theme.

### Reminders
- Global "Task & habit reminders" toggle in Settings is now persisted and enforced: turning it off cancels every scheduled WorkManager job (`ReminderScheduler.cancelAllReminders`, tagged work), turning it on re-registers all future task/habit reminders. The scheduler also self-defers when the flag is off, so a stray call can't re-arm notifications.

### Build hygiene & tests
- `app/schemas/` is no longer gitignored so the exported Room schema (`1.json`) is version-controlled.
- New/expanded unit tests: `DatabasePassphraseProviderTest` (data-safety decision table), schedule-aware `HabitStatsCalculatorTest`, and local-day-boundary `DateTimeUtilsTest`. **81 tests pass.**

### Verification
```text
gradle :app:compileDebugKotlin
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
```

## What changed

### Navigation (root cause fix)
- **Before:** a global `BackHandler` in `LifeOSNavHost` intercepted every back press and popped to whatever the single NavController happened to be on. Combined with a flat route list, going Home → Notes and back could leave stale entries on the stack. Home → Notes → Home did not work via the UI.
- **After:** the global `BackHandler` is gone. The four primary destinations (Home, Tasks, Habits, Insights) live in one nested graph (`root_tabs`) with `saveState`/`restoreState`/`launchSingleTop` on bottom-bar taps. Child screens (Notes, Note Editor, Habit Detail, Expenses, Diary, Timeline, Capture Detail, Search, AI Assistant, Profile, Settings, App Lock Setup) are registered outside the graph, so they stack on top of the active tab and system Back dismisses them one level at a time.

### Bottom navigation (`LifeOSBottomBar.kt`)
- Order is now **Home, Tasks, Habits, Insights** (was Home, Habits, Tasks, Insights).
- Labels are **always visible** (previously the label only appeared for the selected item).
- Selected state: animated lavender pill (`secondaryContainer`) with primary icon+text; unselected: neutral `onSurfaceVariant` icon+label.
- 48dp minimum touch target, press-scale animation, tab semantics (`Role.Tab` + `selected`).
- Removed the dead legacy `LifeOSBottomBar(selected, onSelect)` overload from `LifeOSDesignComponents.kt`.

### Home screen (`HomeScreen.kt`)
- **Root-cause fix for the large empty gap:** the decorative gradient was a `Box(Modifier.size(210.dp))` that forced the greeting block to 210dp+; it now uses `matchParentSize()` behind real content.
- Rebuilt header (LifeOS • Today + date, search/settings/profile actions), greeting, Daily Momentum with animated progress, quick-action row, Today's Priorities, habits streak, Recent Activity, LifeOS Intelligence, and a Capture entry point.

### Insights screen (`InsightsScreen.kt`)
- Cognitive Vitality circular indicator is properly sized (was 104dp before, text overlapped the dial), with typography hierarchy and an honest empty state.
- Insight stat cards use a Column (icon/value/label) so values never overlap.
- Pattern Intelligence is clearly presented as the **local** engine; Weekly Narrative now renders its extracted score in a dedicated row ("N / 100").
- Added `internal fun narrativeScore(narrative): Int` that parses both `score N/100` and `N/100` formats (falls back to 0).

### Build hygiene
- `assembleDebug` passes; `testDebugUnitTest` passes (64 tests); `lintDebug` reports **0 errors**.
- Fixed pre-existing lint errors outside the redesign scope (no behavior change):
  - `NotificationHelper`: wrap `notify()` in `SecurityException` handling (Android 13+ permission race) and drop an obsolete `SDK_INT >= O` branch (minSdk is 26).
  - `AndroidManifest.xml`: declared `android.hardware.camera` as `required="false"` (camera works on camera-less/ChromeOS devices).
  - `VideoCaptureScreen`, `MediaPreviewUtils`, `HabitsScreen`, `TimelineScreen`: suppressed the `ProduceStateDoesNotAssignValue` false positives (the lambdas do assign `value`).
- New tests: `BottomNavItemsTest` (order/labels/route uniqueness) and `NarrativeScoreTest` (both score formats + fallbacks).

## Not changed
- All repositories, use cases, ViewModels, DAOs, entities and the Room schema.
- No internet permission, no cloud/AI/telemetry calls; everything remains offline-first.
- README.md and docs/ are preserved (README build-status and snapshot sections were updated).

## Verification
```text
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

## 2026-09-18 — Stitch UI integration + interaction fixes
### Changed
- Integrated the supplied Stitch visual direction into the existing Kotlin/Jetpack Compose implementation for Profile, Expenses, Add Expense, and App Lock.
- Added local profile-photo selection and persistence through `SettingsStore`.
- Removed the Home-screen Settings action; Settings is now intentionally exposed from Profile.
- Kept system Back handling per-screen so secondary destinations consistently return to the previous destination (no global interceptor).
- Changed onboarding startup gating to wait for the persisted completion value before rendering, preventing the onboarding pages from flashing after the first launch.
- Hardened audio capture cleanup, recorder error handling, and local MediaPlayer preparation/playback.

### Preserved
- Existing Room database, repositories, use cases, navigation architecture, offline-first behavior, and app-lock hashing/recovery.
- No Room schema change was required for profile photo storage because the URI is a local DataStore preference.
- No cloud upload, telemetry, remote AI, or new network dependency was introduced.

### Verification
- Source-level audit completed against the supplied latest repository and Stitch HTML/screens.
- Full Gradle verification could not be executed because the supplied repository archive does not contain `gradlew`/`gradle-wrapper.jar`, and no system Gradle executable is available in the execution environment.

### Remaining
- Run the project's normal Android CI/build locally or in GitHub Actions once the wrapper is present to perform the final compiler, unit-test, instrumentation, and lint verification.

---

## 2026-09-19 — Expenses micro UX fix + documentation audit

### Change summary
Two small, additive UX improvements to the existing Expenses screen. No
redesign, no architecture, navigation, schema or business-logic changes.

### Affected files
- `app/src/main/java/com/lifeos/app/ui/expenses/ExpensesScreen.kt`
- `app/src/main/java/com/lifeos/app/ui/components/GlassCard.kt`
- Documentation: `README.md`, `UPDATE.md`, `docs/04_FEATURES.md`,
  `docs/05_DATABASE.md`, `docs/09_FRONTEND.md`, `docs/14_TESTING.md`,
  `docs/16_KNOWN_ISSUES.md`, `docs/17_CHANGELOG.md`, `docs/21_FILE_STRUCTURE.md`,
  `docs/DOCUMENTATION_AUDIT.md`, `docs/00_PROJECT_OVERVIEW.md`,
  `FINAL_RELEASE_CHECKLIST.md`

### Root cause / problem
1. **Sheet opened too low.** `AddExpenseSheet` used a default
   `ModalBottomSheet`, which opens at the *partially expanded* state; users had
   to drag the sheet upward to see the whole Add Expense form.
2. **Unclear category selection.** Category `GlassChip`s had no selected
   visual; selection was only indicated by the "Selected: <name>" text line.

### Implementation
1. **CHANGE #1 — expanded-by-default sheet.** In `AddExpenseSheet`, the
   existing `ModalBottomSheet` now receives
   `sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)`
   (existing Material 3 API) so it opens fully expanded. The form `Column` gained
   `verticalScroll(rememberScrollState())` so every field/button stays reachable
   when available height shrinks (IME, landscape, font scaling). Shape, drag
   handle, background, fields, category UI, buttons, swipe-to-dismiss and Back
   behavior are unchanged. No dialog, new screen, `Box` or hardcoded offsets
   were introduced.
2. **CHANGE #2 — selected category visual state.** The reusable `GlassChip`
   (`ui/components/GlassCard.kt`) gained an optional `selected: Boolean = false`
   parameter. When selected it uses `MaterialTheme.colorScheme.primary` as the
   container/border with `MaterialTheme.colorScheme.onPrimary` content color;
   otherwise it keeps its original glass surface/border. The Expenses call site
   passes `selected = selectedCategory == cat.name`, reusing the **existing**
   `selectedCategory` state (no second state introduced). Only one chip can be
   selected; selecting another moves the highlight immediately. The existing
   "Selected: <name>" line is retained.

### Preserved
- The entire Expenses screen layout, monthly card, daily average, budget/left
  math, recent transactions, empty state and FAB are untouched.
- `ExpenseRepository`, `ExpenseDao`, `ExpenseEntity`, `ExpenseCategories`,
  navigation, and all other screens are untouched.
- No Room schema/version/migration change.
- No network/cloud/AI/telemetry dependency; data stays on device.

### Verification
Executed in the audit environment with Gradle 8.9 + AGP 8.6.1 (offline; aapt2
override), not just source review:

```text
gradle :app:compileDebugKotlin   -> BUILD SUCCESSFUL
gradle :app:assembleDebug        -> BUILD SUCCESSFUL
gradle :app:testDebugUnitTest    -> BUILD SUCCESSFUL (81 tests, 0 failures)
gradle :app:lintDebug            -> BUILD SUCCESSFUL (0 errors, 4 pre-existing warnings)
```

The repository still does not ship `gradlew`/wrapper JAR, so a locally-installed
Gradle 8.9 distribution was used. Instrumentation tests were not run (no
emulator/device and no `androidTest` source set).

### Remaining
- Device/emulator UI verification of the two Expenses interactions (sheet
  initial position and category highlight) is still recommended — it was
  verified at compile/test/lint level only.
- `docs/16_KNOWN_ISSUES.md` Issue #1 (missing Gradle wrapper scripts) remains
  open.

---

## 2026-09-20 — Home screen redesign (`feat/expenses-micro-ux`)

Home rebuilt in Compose to match the Google Stitch dashboard design while
keeping the LifeOS purple theme and real Room data throughout.

### Home layout (top to bottom)
- **Header**: LifeOS wordmark + logo dot, Search, ProfileAvatar (unchanged flow).
- **Greeting band**: date line (`SATURDAY, 19 SEPTEMBER 2026`), live greeting
  (`DateTimeUtils.greeting()`), and a day-status label derived from completed
  goals ("Day on track" / "Building momentum" / "Fresh start" / "Day starting").
- **Circadian Velocity card**: `$dayProgress%` (minutes-elapsed / 1440, refreshed
  every 60 s), circular bolt progress ring, 4-segment bar ((pct/25), 0..4), and a
  TASKS / HABITS / SPEND stats pill — all real values from today's DAO flows.
- **Focus Now**: highest-priority incomplete task of today (first from
  `observeForDay`, which already sorts by `PRIORITY_ORDER`); when every task is
  done it shows the most recently completed one with a DONE state and a
  one-tap undo. Empty state when no tasks planned.
- **Quick Actions**: Diary / Expense / Timeline tiles routed to existing screens.
- **Habits**: "n/N Active" head + WEEKLY CONSISTENCY row (scheduled-day aware:
  a day counts only when every active habit scheduled on it passed its goal),
  per-habit rows with icon, `{n}d streak` badge, and one-tap check-in/undo that
  persists via `logProgress`/`clearProgress`.
- **Today's Activity**: real timeline (`BuildTimelineUseCase`, today only) with
  typed dot + time pill; empty state otherwise.
- FAB (camera) → Capture coverage, unchanged.

### Data-layer additions
- `DateTimeUtils.dayProgressPercent()`.
- `HabitCompletionDao.observeAllInRange(start, end)` +
  `HabitRepository.observeAllInRange` (weekly consistency window).
- `GetHomeSummaryUseCase` now also consumes `BuildTimelineUseCase` (4th
  dependency, wired in `ServiceLocator`) and exposes `focusTask`,
  `focusTaskIsDone`, `weeklyConsistency: List<DayCheck>`, `weeklyDoneDays`,
  `recentActivity`, plus per-habit `currentStreak/longestStreak/completionPercent`.
- `HomeViewModel`: `toggleTask` kept, `toggleFocusTask` added, `incrementHabit`
  replaced by `toggleHabit(habitId, isDone, goalCount)` (check → log to goal,
  uncheck → clear today's record).

### Verification
```text
gradle :app:compileDebugKotlin   -> BUILD SUCCESSFUL
gradle :app:assembleDebug        -> BUILD SUCCESSFUL
gradle :app:testDebugUnitTest    -> BUILD SUCCESSFUL (81 tests, 0 failures)
gradle :app:lintDebug            -> BUILD SUCCESSFUL (0 errors; only pre-existing
                                     dependency-version warnings)
```

No Room schema change (DB version stays 1), no new navigation stacks, no novel
dependencies, no cloud/AI/analytics; the Screen callback contract of
`HomeScreen` (and therefore `LifeOSNavHost`) is unchanged.

### Remaining
- Device/emulator visual check of the Stitch-to-LifeOS mapping (e.g. Fast Yet
  Fresh / "14 day streak" sample labels in the reference were intentionally not
  reproduced).
