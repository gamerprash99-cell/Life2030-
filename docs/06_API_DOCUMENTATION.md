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
