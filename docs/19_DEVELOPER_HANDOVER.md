# 19 — Developer Handover

## First 60 minutes

1. Read `README.md`.
2. Read `docs/00_PROJECT_OVERVIEW.md`.
3. Read `docs/02_ARCHITECTURE.md`.
4. Read `docs/DESIGN.md` before changing UI.
5. Inspect `MainActivity`, `LifeOSNavHost`, `ServiceLocator`, Room entities/DAOs and repositories.
6. Inspect `core/intelligence/`, `core/security/`, `core/reminders/` and capture flows.
7. Check Git state if the real repository is available.
8. Check whether `gradlew` and a working Android SDK/JDK are present.

## UI implementation rule

The Figma design direction is a reference. Convert it to the existing Compose architecture; do not paste React/Tailwind output into the Android project.

Reuse existing tokens/components and preserve ViewModel/repository ownership of state.

## Intelligence

The current Intelligence Engine is local/offline. There is no external provider or API key requirement.

## Backup

Onboarding and Settings expose JSON restore through Android's document picker and the existing `BackupRepository`.

## Security

App Lock is PIN-only. PIN mode remains app-specific through the existing salted hashing/recovery implementation.

## Verification boundary

Do not claim a build, test, emulator flow or CI run passed unless it was actually executed and recorded.

## Current-state addendum — 2026-09-16

This document remains part of the LifeOS documentation set. Current UI/UX, motion, responsive and accessibility rules are centralized in [`DESIGN.md`](./DESIGN.md). The current Intelligence implementation is local/offline and requires no external AI provider or API key. Build/test statements are only considered verified when the exact command has been executed in a real Android/Gradle environment.

## Current implementation snapshot — 2026-09-16

The latest implementation keeps feature/domain/data layers intact. Primary UI changes are concentrated in ui/components, ui/home, ui/habits, ui/tasks, ui/insights, ui/settings and ui/capture.


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
