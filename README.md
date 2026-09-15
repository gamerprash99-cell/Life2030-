# LifeOS — Android

LifeOS is a native Kotlin + Jetpack Compose + Material 3 personal life-management app built around an **offline-first, privacy-first, local-first** model.

## Current 2026 UI/UX snapshot

The Android implementation now applies the supplied Stitch/Figma visual direction to the primary LifeOS experience: calm lavender/violet surfaces, rounded cards, quick-action chips, animated progress, a floating five-section navigation bar, and a full-screen Life Capture studio for photo/video/audio. The HTML reference was converted into native Compose patterns rather than embedded as a web runtime.

Navigation now has a consistent system-Back fallback for secondary routes, while bottom navigation always returns to the selected primary section. Existing repositories, use cases, ViewModels, DAOs, entities and Room schema remain the data authority.

The design source of truth remains [`docs/DESIGN.md`](./docs/DESIGN.md).

## Architecture

- Kotlin
- Jetpack Compose + Material 3
- Room / SQLite for local data
- DataStore for preferences
- Navigation Compose
- WorkManager for reminders
- CameraX + MediaRecorder for capture
- AndroidX BiometricPrompt for device-level biometric authentication
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
| App Lock | None / Biometric / PIN paths using existing security architecture |
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
- bottom navigation and FAB
- onboarding
- App Lock UX
- motion timings
- accessibility
- dark mode
- responsive Compose guidance
- Figma design reference notes

## Build status

The supplied project snapshot does **not include the Gradle wrapper scripts/JAR**, and this environment does not have a usable Android Gradle build environment. Therefore this source snapshot must not be described as compile-verified here.

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
