# 07 — Authentication

## Technical explanation

LifeOS is a single-user, single-device, local-only application. There is no account, signup, OAuth, Firebase Auth, cloud session, token or remote identity system.

### App Lock

The one access-control feature is **App Lock**, implemented as an app-level PIN gate:

- **UI**: `ui/security/AppLockSetupScreen.kt` and `ui/security/AppLockScreen.kt`
- **Persistence**: `core/util/SettingsStore.kt` using DataStore Preferences
- **Choices**: `NONE` or `PIN`
- **PIN security**: the PIN and recovery answer are stored only as salted hashes through `core/security/PinHasher.kt`
- **Recovery**: forgetting the PIN requires verification of the stored recovery answer before a replacement PIN can be created
- **Gate**: `MainActivity.kt`'s `AppLockGate` blocks `LifeOSNavHost` until the PIN is verified

The previous biometric option has been removed. LifeOS does not use AndroidX BiometricPrompt, biometric enrollment, fingerprints, face templates or device credentials for App Lock.

### Session management

There is no account session or token. Once the PIN gate is successfully unlocked, the existing process-lifetime session behavior is retained.

### Authorization

There is one local user and no roles or remote authorization. App Lock protects the whole application rather than individual feature routes.

## Simple explanation

LifeOS does not ask you to sign in. If you turn on App Lock, it asks for a **LifeOS PIN** before opening the app. If you forget it, the recovery question must be answered before the PIN can be replaced.

## Current state — 2026-09-16

The authentication documentation now matches the current PIN-only implementation. No biometric dependency or runtime biometric code remains in the application source.


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
