# 25 — Data Flow

## Current local data flow

```text
User
 ↓
Compose Screen
 ↓
ViewModel / Use Case
 ↓
Repository
 ↓
Room DAO
 ↓
SQLite on device
```

Settings follow:

```text
Compose Screen
 ↓
SettingsStore
 ↓
DataStore Preferences
```

Local Intelligence follows:

```text
ViewModel / Repository data
 ↓
LifeOSIntelligenceEngine
 ↓
Diary / Mood / Task / Habit / Trend / Pattern analyzers
 ↓
Local result
 ↓
Compose UI
```

No external AI endpoint is involved.

## Backup restore flow

```text
Onboarding or Settings
 ↓
ACTION_OPEN_DOCUMENT
 ↓
application/json
 ↓
app-private temporary file
 ↓
BackupRepository.importFromFile()
 ↓
existing repository restore behavior
```

The UI does not access Room directly.

## Timeline

Timeline entries are composed from existing feature repositories and sorted using persisted timestamps. Capture media remains referenced by persisted capture metadata/URI rather than copied into a second timeline database.

## Current-state addendum — 2026-09-16

This document remains part of the LifeOS documentation set. Current UI/UX, motion, responsive and accessibility rules are centralized in [`DESIGN.md`](./DESIGN.md). The current Intelligence implementation is local/offline and requires no external AI provider or API key. Build/test statements are only considered verified when the exact command has been executed in a real Android/Gradle environment.

## Current implementation snapshot — 2026-09-16

Capture continues to flow from CameraX/MediaRecorder through CaptureSheet into CaptureRepository and then Timeline; navigation changes do not bypass that flow.
