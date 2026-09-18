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