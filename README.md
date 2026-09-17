# LifeOS — Android

LifeOS is a native Kotlin + Jetpack Compose + Material 3 personal life-management app built around an **offline-first, privacy-first, local-first** model.

## Current 2026 UI/UX snapshot

The Android implementation applies the supplied Stitch/Figma visual direction to the primary LifeOS experience: calm lavender/violet surfaces, rounded cards, quick-action chips, animated progress, a four-destination bottom navigation bar (Home, Tasks, Habits, Insights) with always-visible labels and a soft lavender selected pill, and a full-screen Life Capture studio for photo/video/audio. The HTML reference was converted into native Compose patterns rather than embedded as a web runtime.

The four primary sections live in a single nested navigation graph so the bottom bar always returns to the selected section, while secondary screens (Notes/Note Editor, Habit Detail, Expenses, Diary, Timeline, Capture Detail, Search, AI Assistant, Profile, Settings, App Lock Setup) stack naturally on top and dismiss one level at a time with Android's system Back. Existing repositories, use cases, ViewModels, DAOs, entities and Room schema remain the data authority.

The design source of truth remains [`docs/DESIGN.md`](./docs/DESIGN.md).

## Architecture

- Kotlin
- Jetpack Compose + Material 3
- Room / SQLite (SQLCipher-encrypted at rest) for local data
- DataStore for preferences
- Navigation Compose
- WorkManager for reminders
- CameraX + MediaRecorder for capture
- DataStore-backed LifeOS PIN authentication
- Local LifeOS Intelligence Engine for offline analysis

The existing repositories, use cases, ViewModels, DAOs, entities, navigation and database are preserved. UI code does not bypass the repository/domain layers to access Room.

## Features

| Area | Current state |
|---|---|
| Notes | Room-backed notes, editor, search/list flows |
| Tasks | Room-backed tasks, completion and reminders |
| Habits | Completion, streaks and analytics |
| Expenses | Categories, entries and totals |
| Diary | Entries, mood tagging and local intelligence assistance |
| Timeline | Chronological cross-feature timeline including captures |
| Search | Existing cross-feature search screen |
| Capture | Photo, video and audio using on-demand permissions |
| Reminders | WorkManager-based task/habit reminders |
| App Lock | None / PIN paths using existing security architecture |
| Backup | Local JSON export and restore through Android document picker |
| Intelligence | Deterministic local analysis; no cloud model required |
| Onboarding | Four-page onboarding with animated transitions and JSON restore entry |
| UI system | Shared LifeOS cards, buttons, badges, section headers and bottom navigation |

## Privacy / Offline

LifeOS does not require a backend, Firebase, telemetry, analytics service, cloud database, or external AI provider. The manifest does not declare `android.permission.INTERNET`.

The Intelligence Engine runs on-device using deterministic Kotlin analyzers, lexicons, statistics, rules and templates. No API key is required.

## Backup / Restore

Backup data is handled by the existing `BackupRepository`. Restore uses Android Storage Access Framework / `ACTION_OPEN_DOCUMENT` with `application/json`; broad storage permission is not required.

Before relying on restore for important data, verify the exact current repository restore semantics and test with a disposable backup.

## UI / UX

See [`docs/DESIGN.md`](./docs/DESIGN.md) for:

- colors and gradients
- typography
- spacing and shapes
- card/button/chip rules
- bottom navigation and Home top actions
- onboarding
- App Lock UX
- motion timings
- accessibility
- dark mode
- responsive Compose guidance
- Figma design reference notes

## Current UI changes — 2026-09-17

- Bottom navigation was rebuilt: four destinations ordered **Home, Tasks, Habits, Insights**; each item shows its label at all times and the selected item gets an animated lavender pill with primary icon/text color (unselected items use the neutral `onSurfaceVariant`). Every item keeps a 48dp minimum touch target and animated press feedback, and re-selecting the current section restores its saved state.
- Navigation was fixed: the global `BackHandler` was removed and the four primary destinations now live in a nested `root_tabs` graph. Home → Notes → Home (and every other child route) now works through the UI, and Android's system Back behaves naturally instead of being intercepted app-wide.
- Home screen was reworked: the oversized decorative block that forced a huge empty gap was replaced with a match-parent background glow, and the screen now has a LifeOS / Today header, greeting, Daily Momentum with an animated progress bar, quick actions, Today's Priorities, habits streak, Recent Activity and LifeOS Intelligence sections.
- Insights screen was reworked: the Cognitive Vitality card uses a properly sized animated circular indicator with a real empty state, Insight stat cards no longer overlap text, Pattern Intelligence is presented as a local-only engine, and Weekly Narrative now surfaces its score with a dedicated row; a `narrativeScore` parser handles both "score N/100" and "N/100" formats.
- Added pure-JVM tests for bottom-navigation ordering/labels and narrative scoring, and cleaned up pre-existing lint errors (notification permission handling on Android 13+, camera hardware declaration, `ProduceState`/aapt2-adjacent warnings).

## Current UI changes — 2026-09-16

- Home now follows the supplied Stitch dashboard direction in native Jetpack Compose, including the LifeOS/Today header, search, Settings and profile actions at the top, Daily Momentum, quick actions and existing live dashboard sections.
- Settings was removed from the bottom navigation and is opened from the Home header. A local Profile screen is also available from the Home header.
- Android system Back uses the existing single NavController stack for secondary routes.
- App Lock now supports only `NONE` and `PIN`; biometric UI, storage path and dependency were removed. Existing legacy `BIOMETRIC` DataStore values resolve to `NONE` rather than becoming an invalid lock state.

## Build status

Verified on this snapshot with Gradle 8.9 + Android Gradle Plugin 8.6.1: `assembleDebug` builds cleanly, `testDebugUnitTest` passes (64 tests across bottom-nav ordering, narrative scoring, mood/note intelligence, PIN hashing, habit stats, repeat rules, date-time utilities and backup serialization), and `lintDebug` reports zero errors. The repository does not ship the Gradle wrapper JAR/scripts, so builds use a locally-installed Gradle distribution; resource processing on arm64 hosts uses the `android.aapt2FromMavenOverride` override with a qemu-compatible aapt2.

When a proper Android environment is available, run:

```text
./gradlew test
./gradlew assembleDebug
```

and run the configured Android/instrumentation checks as applicable.

## Documentation

The detailed project documentation remains in [`docs/`](./docs). Historical implementation notes are preserved where relevant; current-state corrections are documented in the affected files rather than silently rewriting project history.

Start with:

1. [`docs/00_PROJECT_OVERVIEW.md`](./docs/00_PROJECT_OVERVIEW.md)
2. [`docs/02_ARCHITECTURE.md`](./docs/02_ARCHITECTURE.md)
3. [`docs/DESIGN.md`](./docs/DESIGN.md)
4. [`docs/19_DEVELOPER_HANDOVER.md`](./docs/19_DEVELOPER_HANDOVER.md)
5. [`docs/16_KNOWN_ISSUES.md`](./docs/16_KNOWN_ISSUES.md)

## LIFE Phase 18 Consolidation
LIFE is integrated into the supplied LifeOS source as a local, allow-listed controller with repository-only data access and optional Android on-device voice input. No cloud AI or network fallback is part of the LIFE execution path. Production neural inference remains gated on a real validated LIFE checkpoint. See `FINAL_RELEASE_CHECKLIST.md` and `docs/33_LIFE_INTEGRATION_PHASE18.md`.
