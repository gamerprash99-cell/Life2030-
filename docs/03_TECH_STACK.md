# 03 — Technology Stack

| Area | Technology | Current role |
|---|---|---|
| Language | Kotlin | Application source |
| UI | Jetpack Compose | Native UI |
| Design | Material 3 | Components/theme foundation |
| Persistence | Room / SQLite | Local data |
| Preferences | DataStore Preferences | Settings |
| Navigation | Navigation Compose | App navigation |
| Scheduling | WorkManager | Task/habit reminders |
| App Lock | DataStore + salted PIN hashing | Local PIN authentication and recovery |
| Camera | CameraX | Photo/video capture |
| Audio | MediaRecorder | Audio capture |
| Image loading | Coil | Capture/media presentation |
| Serialization | kotlinx.serialization JSON | Backup and local models |
| Local Intelligence | Plain Kotlin analyzers/rules/templates | Offline analysis |
| Testing framework | JUnit + AndroidX/Compose test dependencies | Declared; runtime coverage must be verified separately |

## Android versions

- compileSdk: 35
- minSdk: 26
- targetSdk: 35
- JVM target: 17

## Network/cloud

No `INTERNET` permission is declared. There is no OkHttp/Retrofit cloud-AI integration in the current source. Do not reintroduce one as a requirement for the released app.

## UI design system

See [`DESIGN.md`](./DESIGN.md) for current LifeOS visual tokens, responsive rules and motion specifications.

## Current-state addendum — 2026-09-16

This document remains part of the LifeOS documentation set. Current UI/UX, motion, responsive and accessibility rules are centralized in [`DESIGN.md`](./DESIGN.md). The current Intelligence implementation is local/offline and requires no external AI provider or API key. Build/test statements are only considered verified when the exact command has been executed in a real Android/Gradle environment.

## Current implementation snapshot — 2026-09-16

The implemented reference UI remains Kotlin + Jetpack Compose + Material 3 with the existing Room, DataStore, Navigation Compose, WorkManager, CameraX, MediaRecorder stack without biometric authentication.


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
