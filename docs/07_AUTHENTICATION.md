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
