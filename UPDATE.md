# LifeOS — UPDATE

Change log for the `fix/audit-hardening` branch (UI/UX + navigation audit and redesign, 2026-09-17).

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
- Added a navigation-level Compose `BackHandler` so secondary destinations consistently return to the previous destination.
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


## 2026-10-12 — Latest GitHub Actions compilation repair

- Audited failed workflow run [38227803179](https://github.com/gamerprash99-cell/Life2030-/actions/runs/38227803179) on `main`, commit `47f1d40`. The failing job was **Assemble debug APK**; the first real task failure was Kotlin compilation.
- Root causes from the job log:
  - `:app:compileDebugKotlin` could not resolve `androidx.work` types because the WorkManager KTX dependency had been dropped, and the existing App Lock implementation could not resolve AndroidX Biometric / FragmentActivity APIs because the Biometric dependency was missing.
  - Legacy Capture UI files still called the former capture repository/entity API (enum types, `addCapture`, separate date/time and caption properties) after the repository changed to string capture types and timestamped `CaptureEntity` records.
  - Legacy Notes list code referenced a missing `NotesListViewModel` and removed `plainTextForSearch` property.
  - Legacy Search code called task/expense repository search methods that are not present in the current data layer.
- Fix branch: `fix/ci-latest-kotlin-compilation`. Restored explicit WorkManager and Biometric dependencies; kept the central permission helper and added compatibility file-factory names backed by the existing app-private capture directories; aligned legacy Capture and Notes screens with current Room-backed repository/entity APIs; restricted the legacy Search screen to real Notes and Diary search results.
- No database entity, Room schema, migration, navigation destination, user data, external service, or network permission was changed.
- Verification status at documentation update time: GitHub Actions rerun is still required after the fix commits. No passing build/test result is claimed until a fresh CI run reports success.


## 2026-10-12 — CI follow-up: Intelligence APIs and stale Insights test

- The next CI attempt confirmed app production Kotlin compilation now succeeds for both debug and release variants. Its remaining errors were confined to six unresolved `narrativeScore` calls in `NarrativeScoreTest.kt`.
- Inspection confirmed the current `InsightsScreen`/`InsightsViewModel` no longer expose or display that old narrative score. Replaced the stale test with unit tests covering the current `InsightsUiState` defaults and `InsightsPeriod` labels instead of adding dead production code or disabling the test.
- Reconciled retained offline Intelligence and LIFE command code with the current repository contracts: task reads use existing backup/read APIs, expense/date-range and search helpers use existing Room repositories/queries, diary range access delegates to the existing range query, and weekly boundaries are calculated as ISO Monday–Sunday local dates.
- The current test/build workflow was last observed as `in_progress` in the build/test step with no fresh run conclusion or downloadable live logs. Therefore the final build/test result remains **unverified** until a completed Actions run reports its conclusion.
- All changes remain on `fix/ci-latest-kotlin-compilation` and PR #48; `main` was not modified or merged. No Room schema, migration, user data, network permission, or external service was added or changed.


## 2026-10-12 — Room v6 schema-test repair and preservation review

- CI reached unit tests after production Kotlin compilation and debug APK packaging. Seven `AppDatabaseSchemaTest` cases failed because the schema fixture was not available at the expected path; the test still asserted obsolete v3–v5 tables while the current `AppDatabase` is v6 and intentionally contains nine entity tables.
- Moved `room.schemaLocation` / `room.incremental` arguments to the project-level KSP extension so Room exports its schema at `app/schemas/com.lifeos.app.data.db.AppDatabase/6.json`.
- Reworked `AppDatabaseSchemaTest` to parse the exported schema JSON with kotlinx.serialization and verify version 6, the exact current entity table set, and persisted columns for reminders, diary entries, Notes, Note folders, and Captures. No entity definitions, Room version, or migration SQL was changed.
- Replaced obsolete `narrativeScore` unit checks with tests for the current Insights state model, which no longer exposes that old score.
- Final preservation review restored the existing permission helper behavior verbatim and added only `LifeOSPermissions.CAMERA` / `RECORD_AUDIO` constants; restored the existing four-category local Search UI, backed by added on-device repository search methods.
- Verification at this point: production Kotlin compilation and debug APK assembly succeeded on a prior branch run, but that run failed in unit tests due to the stale/missing schema fixture. A fresh full CI result for the final source revision is still pending; no green build is claimed until a completed run reports success.
- All fixes remain on `fix/ci-latest-kotlin-compilation` / PR #48. `main` has not been modified or merged. Changes remain offline-only, and no user data or database migration was changed.


## 2026-10-12 — UI/UX and codebase audit report

- Added `docs/LIFEOS_UI_UX_CODEBASE_AUDIT_2026-10-12.md`, a repository-based audit covering startup, design-system consistency, navigation, Home, Capture, Notes, Tasks, Habits, Expenses, Diary, Timeline, offline intelligence, security/privacy, accessibility, dead-code cleanup, CI, and a phased implementation roadmap.
- Confirmed from GitHub metadata that the latest visible Android CI run #283 on commit `dd36cca5807d8cf04addaeb560052c57728c0d6b` completed successfully. The report does not treat this as proof of runtime UX or device correctness.
- Identified a documentation consistency issue: `REDESIGN_PLAN.md` specifies Aurora Noir and a five-section navigation model, while `UI_UX_FLOW_NAVIGATION.md` and historical `UPDATE.md` sections retain older token/navigation assumptions. The report recommends reconciling these without erasing history.
- Recorded that the broader dead-code sweep is still marked pending in `REDESIGN_PLAN.md`; no component is declared dead without call-site/reachability verification.
- Verification performed: inspected repository tree, selected source/configuration/documentation files, test inventory, recent commit metadata, and latest visible CI job result. No local build, emulator/device test, screenshot capture, profiling, or full unused-symbol analysis was performed as part of this report.
- No application source, database schema, migration, feature, or user data was changed. The report is on branch `audit/ui-ux-codebase-review-2026-10-12` for review.

## 2026-10-12 — P0 UX baseline: contract, note-editor safety, Home hierarchy

Scope: a first, deliberately narrow slice of the P0 work from the 2026-10-12
static audit. No database schema, migration, entity, repository contract or user
data was touched; no permission or network path was added.

### Canonical product/UI contract (new)
- Added `docs/CURRENT_PRODUCT_UI_CONTRACT.md` — the single authoritative
  statement of the five destinations (Home · Notes · Capture · Insights · Life),
  Capture as a centre **action** rather than a tab, every track module's entry
  points, the Aurora Noir token values as they actually are in `Color.kt`, the
  Diary paper-aesthetic exception, and the offline/data-honesty/privacy
  contract.
- Corrected `UI_UX_FLOW_NAVIGATION.md` rather than rewriting it: a clearly marked
  "Corrections — 2026-10-12" note flags the historical palette
  (`#7C4DFF`/`#D946EF` → `#5A4BE8`/`#8B3DDB`), the non-existent `life_calendar`
  route, and the Notes-editor/AI details that do not match the build. History is
  preserved; the active spec is now unambiguous.

### Note editor — unsaved changes are no longer silently discarded
- **Root cause:** both the header Back arrow and system Back called `onBack()`
  directly, so an accidental Back or gesture threw away an unsaved note with no
  prompt.
- **Fix (`NoteEditorScreen.kt`):** a draft-vs-baseline check (`isNoteDirty` +
  `NoteBaseline`) gates leaving. Back now runs a two-stage rule — the first press
  only dismisses the keyboard, the second opens a "Discard changes?" dialog with
  **Keep editing** / **Discard**. The same guard covers the arrow and system Back.
- New unit tests (`NoteEditorDirtyTest`, 9 cases) pin the rule: an untouched
  existing note is clean, any edit (including clearing a field) is dirty, an
  untouched new note is clean, and a new note typed-then-cleared is clean.

### Home — content leads
- Moved the LifeOS AI entry from above today's tasks to the **last** section of
  Home. The screen now reads: today's progress → tasks → habits → weekly rhythm →
  spend/capture → recent activity → LifeOS AI, so the user's real day ranks above
  the assistant rather than competing with it.

### Verification
- Ran `./gradlew testDebugUnitTest` on this branch: **BUILD SUCCESSFUL**, 471
  tests, 0 failures / 0 errors / 0 skipped, including the new
  `NoteEditorDirtyTest` (9 cases) and the pre-existing `AppDatabaseSchemaTest`.
- No device/emulator was available, so contrast at 200% font scale, touch-target
  checks, TalkBack order and cold/warm startup timings remain **unverified** and
  are listed below.

### Remaining (unchanged by this pass)
- Device measurements: cold/warm startup, contrast at 200% font scale, TalkBack
  order, per-screen interaction review.
- The navigation-graph question of whether tapping an *unvisited* root tab from a
  stacked module should clear the module (currently it stacks the tab above it).
- Dead-code reachability inventory (audit P2).


## 2026-10-31 — Permanent Habits redesign specification and handoff

- Inspected the GitHub `main` snapshot and the user-provided LifeOS Home screenshot plus 72.7-second habit-tracker video.
- Added `docs/HABITS_PERMANENT_REDESIGN_SPEC.md` covering target navigation, observed reference interactions, dashboard, creation/edit, tracking modes, streaks, heatmap, reminders, celebration, accessibility, migration, tests and PR workflow.
- Added `NEXT_SESSION.md` so the next Claude Code session can continue without asking the user to repeat requirements.
- Added README links to the specification and handoff.
- Baseline: existing Habit screens/repository/streak logic are present; Habits is a stacked route, not a root tab. Current bottom bar is Home / Notes / Life / You with Capture as centre action. Inspected Room database is version 6; current habit model does not represent every requested tracking mode/schedule.
- Insights is already absent from inspected `Screen.kt` / `LifeOSNavHost.kt` navigation. Preserve local Intelligence Engine code still used elsewhere.
- **Documentation-only preparation:** no application source, database schema, migration, UI or tests changed. No build or emulator test was run.


## 2026-10-30 — Permanent Habits experience (implementation)

### Navigation
- Bottom bar is exactly **Home · Notes · ＋ Capture · Habits · Life**. Habits
  is a permanent root tab in the `root_tabs` graph with saved state; Capture
  remains the raised centre action.
- Profile left the bottom bar but is still a stacked destination from the Home
  avatar and the Life hub, with Settings, App Lock and backup unchanged.
- New `habits/edit?habitId=&wizard=` route for the guided wizard and the full
  add/edit form; detail's Edit action navigates to it.

### Habits
- Dashboard (`HabitsScreen.kt` + `HabitsViewModel.kt`): date strip (past days
  only), All/Due/Completed filters, live Habit Score with neutral rest days,
  habit cards with streak, schedule, tracking control and 28-day mini heatmap,
  archived sheet/restore, delete confirmation, celebration + share.
- Editor (`HabitEditorScreen.kt`): 10-step first-habit wizard and normal form
  with Build/Quit, Tick/Count/Checklist, every frequency mode, icon/colour
  pickers, reminder time + repeat, category, Timeline toggle, review,
  per-step validation and an unsaved-changes guard.
- Detail (`HabitDetailScreen.kt`): hero/streak, 2×2 stats, day action card
  (tick, count, checklist, Quit slip/"stayed on track"), clickable 12-week
  heatmap with day backfill, schedule/reminder card, share/archive/delete.
- `HabitVisuals.kt` carries the Habits-scoped iris/lavender accent, score ring,
  mini heatmap, celebration/confetti, share helpers and reduced-motion
  detection. The rest of the app intentionally keeps the warm Lumen theme.
- `HabitStatsCalculator.kt` now handles schedule modes, per-habit period
  quota rules for N×/week and N×/month, legacy meaning, streak units and
  honest completion percentages (100% ceiling).
- Home lists only habits due today using per-habit period targets and routes
  checklist habits to the Habits tab; Timeline milestones respect the
  per-habit toggle and use honest subtitles.

### Database and backups
- Room **v6 → v7**, additive (`MIGRATION_6_7`); exported `7.json` committed.
- New fields/tables as documented in `docs/05_DATABASE.md`; old rows keep
  their meaning and get valid defaults.
- Backup format **v2** with checklist tables; v1 restores still work.
- `HabitRepository.delete()` now removes completions, checklist rows and the
  reminder together; template edits archive items instead of rewriting
  history; editing a checklist habit reconciles only that day.
- Existing reminder rows now accept an explicit repeat change instead of
  silently keeping the old repeat type.

### Verification (executed in this environment)
```text
sh ./gradlew :app:compileDebugKotlin   → BUILD SUCCESSFUL
sh ./gradlew :app:testDebugUnitTest    → BUILD SUCCESSFUL, 486 tests, 0 failures
sh ./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
                                       → BUILD SUCCESSFUL, lint 0 errors / 32 warnings (pre-existing)
```
Instrumentation migration tests compile (`assembleDebugAndroidTest`) but were
not executed: no emulator/device or `adb` is available here.

### Not changed / preserved
- No internet permission, cloud, telemetry or new network dependency.
- SQLCipher encryption, PIN App Lock, offline-first behaviour and all
  pre-existing feature data paths are preserved.
- The legacy pre-v7 habit schedules retain their exact DAILY/WEEKLY/CUSTOM
  semantics.

## 2026-10-31 — Habits undo, wizard chrome and project-wide minor bug audit

Scope: the two reported Habits defects plus a read-only audit of navigation,
window insets, save/permission flows and Home routing. No Room entity, schema,
migration, repository contract or user data was changed; no permission,
network path, dependency or cloud service was added.

### Reported bug 1 — habit creation step 1: oversized blank blocker

- **Problem**: on the first wizard step a large empty/white band surrounded the
  Continue button.
- **Root cause**: the Habit editor is composed inside the root `LifeOSNavHost`
  Scaffold, which still reserved the primary bottom-navigation bar height for
  it, while the wizard painted its own Back/Continue bar with
  `navigationBarsPadding()`. The tab bar, the wizard bar and a duplicated
  navigation-bar inset stacked into roughly 150–190dp of empty chrome.
- **Fix**: `BottomBarVisibility.kt` now excludes the Habit editor route from
  the primary bottom bar and marks it as owning the bottom inset, so the root
  hands it the full window height exactly like the Diary composer; the wizard
  bar's single `navigationBarsPadding()` is then applied once.
- **Preserved**: scrolling, all 10 steps, per-step validation, the top Save
  action and the unsaved-changes guard.
- **Files**: `ui/navigation/BottomBarVisibility.kt`,
  `ui/navigation/LifeOSNavHost.kt`, `ui/habits/HabitEditorScreen.kt`.

### Reported bug 2 — habit check-in undo

- **Problem**: checking in changed the control to "Done" but the reversal was
  not discoverable, a completed Count habit could not be corrected, and no
  day-scoped undo existed.
- **Root cause**: the dashboard toggled the completion row, but the Count
  stepper was disabled at the target, nothing advertised undo, and the tap
  handler used the currently selected day rather than the day that was marked.
- **Fix**: `HabitsViewModel` gained day-scoped `setTickForDay`/`undoCheckIn`
  and a per-habit/day in-flight guard so repeated taps cannot fire duplicate
  check-ins or celebrations. The dashboard raises a short **Undo** snackbar
  after a successful check-in; the Done control announces "tap to undo" for
  screen readers; the Count stepper keeps decrement enabled once there is
  progress to reverse (increment stays disabled at the target).
- **Database**: no schema or migration change. Completions remain keyed by
  `(habitId, dateEpochDay)`, so repeated taps upsert, undo clears only the day
  that was marked, history is preserved and the state survives app restart
  because it is written to Room.
- **Files**: `ui/habits/HabitsViewModel.kt`, `ui/habits/HabitsScreen.kt`.

### Additional verified fixes found by the audit

- **Home recent activity opened the wrong screen** — every row used the Snap
  viewer callback, so diary/task/habit/expense rows opened an empty capture
  viewer. Added `routeForTimelineItem` (pinned by `TimelineNavigationTest`) and
  routed each type to its real destination.
  `ui/navigation/Screen.kt`, `ui/navigation/LifeOSNavHost.kt`,
  `ui/home/HomeScreen.kt`.
- **Duplicated window insets** — Timeline, Settings and Expenses asked for
  `systemBars` inside the host Scaffold that had already inset them; the Note
  editor, AI assistant, Capture hub and Snap camera overlays also added their
  own status/navigation padding on top of the host's. Insets are now owned once
  per destination: the host zeroes the edges a route owns
  (`ownsTopInset`/`ownsBottomInset`), the three nested Scaffolds were zeroed
  like Tasks/Habits/Profile, and the camera overlays dropped their duplicate
  padding. `ui/navigation/BottomBarVisibility.kt`,
  `ui/navigation/LifeOSNavHost.kt`, `ui/capture/CameraCaptureScreen.kt`,
  `ui/capture/VideoCaptureScreen.kt`, `ui/timeline/TimelineScreen.kt`,
  `ui/settings/SettingsScreen.kt`, `ui/expenses/ExpensesScreen.kt`.
- **A declined permission silently cancelled the save** — habit creation and
  both task flows ran the save only after notification / exact-alarm access was
  granted, so "Not now" created nothing. Both permission hosts now expose
  `runProtectedOrContinue`: the entity is always persisted and the reminder
  degrades to the scheduler's permission-free path. `ReminderPermissionHost`
  also re-checks on resume so the "Open Settings" route releases the pending
  action. `ui/components/ReminderPermissionHost.kt`,
  `ui/components/ExactAlarmPermissionHost.kt`, `ui/habits/HabitEditorScreen.kt`,
  `ui/habits/HabitDetailScreen.kt`, `ui/tasks/TasksScreen.kt`.
- **Habit editor Back was inconsistent** — system Back moved one wizard step
  back while the header arrow exited the flow; both now share one step-aware
  handler. `ui/habits/HabitEditorScreen.kt`.

### Verification (executed in this environment)

Environment: JDK `17.0.21`, Android SDK at `/opt/android-sdk`, AAPT2 supplied
via `-Pandroid.aapt2FromMavenOverride=/opt/aapt2-wrapper/aapt2`; wrapper
invoked as `sh ./gradlew` (not executable in this container).

```text
sh ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug \
  -Pandroid.aapt2FromMavenOverride=/opt/aapt2-wrapper/aapt2
  → BUILD SUCCESSFUL in 13m 28s
    :app:testDebugUnitTest          497 tests, 0 failures, 0 errors, 0 skipped (39 XML result files)
    :app:assembleDebug              app/build/outputs/apk/debug/app-debug.apk
    :app:assembleDebugAndroidTest   app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
    :app:lintDebug                  0 errors, 32 warnings (all pre-existing), 7 informational
```

- New tests: `TimelineNavigationTest` (5 cases) and the inset-ownership /
  bottom-bar cases in `BottomBarVisibilityTest`.
- Not executed: `:app:connectedDebugAndroidTest` — no `adb`, emulator or
  device is available here. No instrumentation pass is claimed.

### Not changed / preserved

- No Room entity, schema, migration, DAO or repository-contract change; the v7
  schema and every stored row are untouched.
- No internet permission, telemetry, cloud call, new dependency or credential.
- SQLCipher encryption, PIN App Lock, the navigation graph, offline-first
  behaviour and all pre-existing user data paths are preserved.

### Remaining

- Device/emulator pass for the wizard on small screens and gesture/3-button
  navigation, the Undo snackbar timing, and visual confirmation of the inset
  changes (see `docs/14_TESTING.md`).
- `:app:connectedDebugAndroidTest` (v6→v7 migration and expense-capture query
  tests) still needs a real device/emulator.
