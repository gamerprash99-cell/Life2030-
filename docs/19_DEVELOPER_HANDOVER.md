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

Biometric authentication is device-level through AndroidX `BiometricPrompt`. PIN mode remains app-specific through the existing hashing/recovery implementation.

## Verification boundary

Do not claim a build, test, emulator flow or CI run passed unless it was actually executed and recorded.

## Current-state addendum — 2026-09-16

This document remains part of the LifeOS documentation set. Current UI/UX, motion, responsive and accessibility rules are centralized in [`DESIGN.md`](./DESIGN.md). The current Intelligence implementation is local/offline and requires no external AI provider or API key. Build/test statements are only considered verified when the exact command has been executed in a real Android/Gradle environment.

