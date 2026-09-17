# 02 — Architecture

## Current architecture

LifeOS is a single native Android application. The intended flow remains:

```text
Compose UI
   ↓
ViewModel / UI state
   ↓
Use cases / repositories
   ↓
Room DAOs / DataStore / local services
```

Cross-feature timeline and Intelligence composition use existing domain/repository layers rather than duplicating persistence in UI.

## Major layers

- `ui/` — Compose screens, navigation and reusable visual components
- `domain/` — domain models and use cases
- `data/` — Room database and repositories
- `core/intelligence/` — deterministic local analysis
- `core/reminders/` — WorkManager scheduling
- `core/security/` — App Lock and PIN support
- `core/util/` — dates, permissions, settings, media and notifications

## Navigation

Navigation remains Navigation Compose with one `NavController` owned by `LifeOSNavHost`. Existing routes and back-stack behavior are preserved.

## Offline boundary

There is no backend service and no external AI endpoint in the current source. `core/ai/AiRepository.kt` is a compatibility facade over `LifeOSIntelligenceEngine`.

## UI architecture

The visual system is centralized in `ui/theme/` and reusable components in `ui/components/`. See [`DESIGN.md`](./DESIGN.md) for the current specification.

## Database

Room remains the persistence layer. No database schema change was required for the UI/UX work described in the current snapshot.

## Security

App Lock uses the existing local PIN hashing/recovery path. The lock choices are `NONE` and `PIN`; no biometric subsystem is part of the current architecture.

## Current-state addendum — 2026-09-16

This document remains part of the LifeOS documentation set. Current UI/UX, motion, responsive and accessibility rules are centralized in [`DESIGN.md`](./DESIGN.md). The current Intelligence implementation is local/offline and requires no external AI provider or API key. Build/test statements are only considered verified when the exact command has been executed in a real Android/Gradle environment.

## Current implementation snapshot — 2026-09-16

UI changes continue to consume repositories, use cases and ViewModels; no UI path was added that accesses Room directly.


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
