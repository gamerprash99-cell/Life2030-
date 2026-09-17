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
