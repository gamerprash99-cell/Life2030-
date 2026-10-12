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
