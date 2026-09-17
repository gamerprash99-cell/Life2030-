# 06 — API Documentation

## External APIs

**None in the current source.** LifeOS does not require an external HTTP service, backend, or cloud AI provider.

The application manifest does not declare `android.permission.INTERNET`, and `core/ai/AiRepository.kt` delegates to the local `LifeOSIntelligenceEngine`.

## Internal application interfaces

The main internal boundaries are:

- repositories in `data/repository/`
- domain use cases in `domain/usecase/`
- `LifeOSIntelligenceEngine` and its analyzer classes
- `SettingsStore` for DataStore-backed preferences
- `BackupRepository` for JSON export/import
- `ReminderScheduler` / `ReminderWorker` for reminders

These are Kotlin application interfaces, not network APIs.

## Backup format

Backup data is serialized as JSON through the existing `BackupRepository`. Restore is initiated through Android Storage Access Framework (`ACTION_OPEN_DOCUMENT`, MIME `application/json`) and then passed to the repository.

## Documentation rule

Historical references to the previously removed cloud AI implementation may remain in changelog/history sections for traceability. They are not part of the current runtime architecture.

## Current-state addendum — 2026-09-16

This document remains part of the LifeOS documentation set. Current UI/UX, motion, responsive and accessibility rules are centralized in [`DESIGN.md`](./DESIGN.md). The current Intelligence implementation is local/offline and requires no external AI provider or API key. Build/test statements are only considered verified when the exact command has been executed in a real Android/Gradle environment.

## Current implementation snapshot — 2026-09-16

No external API or network service was introduced. LifeOS Intelligence remains a local compatibility layer over the on-device intelligence engine.


---

## Current source snapshot — 2026-09-17

This documentation set is aligned to the supplied LifeOS Android source snapshot. The source of truth is the Kotlin/Jetpack Compose implementation under `app/src/main/java/com/lifeos/app/`, together with `app/build.gradle.kts` and `app/src/main/AndroidManifest.xml`.

### Verified architecture facts
- Native Kotlin Android application using Jetpack Compose + Material 3.
- Navigation uses Navigation Compose with a `root_tabs` nested graph for Home, Tasks, Habits and Insights; secondary screens remain stackable routes.
- Local persistence uses Room/SQLite with SQLCipher for database-at-rest encryption, plus DataStore for preferences/settings.
- Repositories and use cases remain the application data boundary; UI does not directly own Room access.
- Offline intelligence is implemented under `core/intelligence/` using deterministic local analyzers, rules, lexicons, statistics and templates.
- Capture uses CameraX for photo/video and Android `MediaRecorder` for audio, with runtime permissions requested only when capture is selected.
- Backup/restore is local and uses Android Storage Access Framework/document picker flows.
- App Lock is PIN-only (`NONE` / `PIN`) in the current source; no cloud identity or biometric App Lock implementation is present.
- The manifest does not declare `android.permission.INTERNET`.

### Verification boundary
The repository snapshot supplied for this documentation pass does not contain the Gradle wrapper scripts/JAR. Therefore this environment does not claim a fresh Gradle build, instrumentation run, or physical-device verification unless an executed command is recorded elsewhere in the project history.

### Maintenance rule
Historical sections are intentionally retained for traceability. When historical documentation conflicts with the current source, the current source and the latest dated current-state section take precedence; historical changelog entries should not be rewritten merely to make history appear current.
