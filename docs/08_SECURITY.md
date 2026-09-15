# 08 — Security Review

**Review date:** 2026-09-16

## Current security architecture

### App Lock

LifeOS supports:

- `NONE`
- `BIOMETRIC`
- `PIN`

Biometric mode uses AndroidX `BiometricPrompt`. LifeOS does not access or store biometric templates and cannot distinguish which enrolled person authenticated.

PIN mode uses the existing `PinHasher` and recovery flow. Plaintext PINs and plaintext recovery answers must never be persisted.

### Permissions

Camera, microphone and notification permissions are requested only when the related feature requires them. JSON backup restore uses Android's document picker and does not require broad storage permission.

### Network boundary

`AndroidManifest.xml` does not declare `android.permission.INTERNET`. There is no cloud AI client in the current source.

### Local data

Room and DataStore remain local persistence layers. The current source does not provide database-at-rest encryption. This is a known security gap and should be addressed before treating LifeOS as hardened for sensitive production data.

## Known risks

| Priority | Finding | Current state |
|---|---|---|
| High | Room database is not encrypted at rest | Open |
| Medium | PIN/recovery implementation requires continued security review | Open / verify with device testing |
| Medium | Biometric result is device-level by Android design | Documented; PIN is the app-specific alternative |
| Medium | No automated security regression suite | Open |

## Biometric UX requirement

When enabling biometric App Lock:

1. explain Android device-level biometric behavior;
2. check availability;
3. guide enrollment through supported Android settings when required;
4. re-check after return;
5. authenticate successfully before saving the preference.

Do not implement a custom fingerprint database or fake biometric prompt.

## Current-state addendum — 2026-09-16

This document remains part of the LifeOS documentation set. Current UI/UX, motion, responsive and accessibility rules are centralized in [`DESIGN.md`](./DESIGN.md). The current Intelligence implementation is local/offline and requires no external AI provider or API key. Build/test statements are only considered verified when the exact command has been executed in a real Android/Gradle environment.

## Current implementation snapshot — 2026-09-16

The capture redesign does not add storage or network permissions. Camera and microphone access are still requested only when the relevant capture mode is opened.
