# 10 — Backend / Server Architecture

## Current status

LifeOS has **no backend server**.

All core application data is stored locally using Room/SQLite. Preferences use DataStore. Intelligence is computed locally.

There is no required:

- cloud database
- REST API
- GraphQL service
- authentication server
- Firebase backend
- telemetry service
- external AI provider

## Offline-first behavior

Core features are designed to work without network access. Backup is a local JSON file operation; the user can explicitly export/share a backup using Android system capabilities.

## Architecture consequence

Do not add a server as a prerequisite for Notes, Tasks, Habits, Diary, Expenses, Timeline, Capture, App Lock or Intelligence. Future optional integrations must remain non-mandatory and must not silently upload user data.

## Current-state addendum — 2026-09-16

This document remains part of the LifeOS documentation set. Current UI/UX, motion, responsive and accessibility rules are centralized in [`DESIGN.md`](./DESIGN.md). The current Intelligence implementation is local/offline and requires no external AI provider or API key. Build/test statements are only considered verified when the exact command has been executed in a real Android/Gradle environment.

