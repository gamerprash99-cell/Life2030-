# 20 — Founder Guide

## What LifeOS is

LifeOS is a native Android personal life-management app that brings Notes, Tasks, Habits, Diary, Expenses, Timeline and Life Capture together on one device.

## Privacy model

The current product direction is offline-first and privacy-first. Core data is stored locally. LifeOS Intelligence runs locally using deterministic Kotlin analysis; no cloud AI provider or API key is required.

## UI direction

The 2026 visual direction is premium, calm and modern: lavender/violet identity, soft surfaces, rounded cards, grouped settings, clear typography and restrained motion. See `docs/DESIGN.md`.

## Backup

Users can export a local JSON backup and restore it through Android's native document picker. Restore is routed through `BackupRepository`.

## App Lock

Users can choose None, Biometric or PIN. Biometric authentication is provided by Android and is based on device-enrolled biometrics. A PIN is the independent app-specific option.

## Current limitations

- Build verification depends on a proper Android/Gradle environment.
- Automated test coverage must be expanded and executed.
- Database-at-rest encryption remains a security hardening item.
- Some existing feature-model fields remain outside their creation UI.

## Current-state addendum — 2026-09-16

This document remains part of the LifeOS documentation set. Current UI/UX, motion, responsive and accessibility rules are centralized in [`DESIGN.md`](./DESIGN.md). The current Intelligence implementation is local/offline and requires no external AI provider or API key. Build/test statements are only considered verified when the exact command has been executed in a real Android/Gradle environment.

## Current implementation snapshot — 2026-09-16

The current product presentation emphasizes calm lavender surfaces, local-first intelligence, quick capture and a persistent five-section navigation model while retaining existing functionality.


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
