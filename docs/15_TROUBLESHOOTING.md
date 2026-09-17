# 15 — Troubleshooting

## Build cannot start with `./gradlew`

The supplied project snapshot may not contain the Gradle wrapper scripts/JAR. Verify the repository contents before assuming the wrapper exists.

Preferred recovery on a development machine is to open the project in Android Studio with a compatible JDK/Android SDK or regenerate the wrapper using an installed compatible Gradle version. Do not hide build failures by disabling tasks or suppressing compiler errors.

## Backup restore fails

Check:

1. the selected document is JSON;
2. the file is a LifeOS backup;
3. required backup fields are present;
4. the existing `BackupRepository` reports a compatible format;
5. the current restore semantics are understood before retrying against real data.

The UI uses Android Storage Access Framework and `application/json`.

## Biometric setup unavailable

LifeOS uses Android's biometric enrollment. If the device has no suitable enrolled biometric, direct the user to Android security/biometric settings and re-check availability after returning to LifeOS.

LifeOS cannot identify which enrolled fingerprint/face belongs to a particular person.

## Intelligence

LifeOS Intelligence is local and does not require an API key or internet connection. If a local analysis fails, inspect the corresponding analyzer and repository data flow rather than looking for a cloud credential.

## UI problems

For spacing, colors, typography, animation and responsive behavior, use [`DESIGN.md`](./DESIGN.md) as the current design reference.

## Current-state addendum — 2026-09-16

This document remains part of the LifeOS documentation set. Current UI/UX, motion, responsive and accessibility rules are centralized in [`DESIGN.md`](./DESIGN.md). The current Intelligence implementation is local/offline and requires no external AI provider or API key. Build/test statements are only considered verified when the exact command has been executed in a real Android/Gradle environment.

## Current implementation snapshot — 2026-09-16

For navigation issues, use the persistent bottom navigation to switch primary sections and system Back to unwind secondary routes; a NavHost BackHandler now provides a consistent fallback.


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
