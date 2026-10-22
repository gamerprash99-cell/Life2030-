# 00 — Project Overview

## Current state — 2026-09-16

LifeOS is a native Android personal life-management app using Kotlin, Jetpack Compose, Material 3, Room, DataStore, Navigation Compose, WorkManager and local capture/intelligence infrastructure.

### Product areas

- Notes
- Tasks
- Habits
- Diary
- Expenses
- Timeline
- Search
- Photo / Video / Audio capture
- Reminders
- App Lock
- Backup / Restore
- Local Intelligence

### Privacy model

The current source is local-first. `AndroidManifest.xml` does not declare `android.permission.INTERNET`. The Intelligence Engine is implemented in `core/intelligence/` and does not require a cloud provider or API key.

### Current UI direction

The 2026 UI pass uses shared LifeOS design tokens, soft lavender/violet surfaces, rounded cards, grouped settings, animated onboarding, animated bottom navigation and tactile component feedback. See [`DESIGN.md`](./DESIGN.md).

### Important verification boundary

This source snapshot was not compile-verified in the supplied environment because the Gradle wrapper is absent and a usable Android Gradle environment is unavailable. Documentation must not describe a build as passed unless it is actually executed.

## Current-state addendum — 2026-09-16

This document remains part of the LifeOS documentation set. Current UI/UX, motion, responsive and accessibility rules are centralized in [`DESIGN.md`](./DESIGN.md). The current Intelligence implementation is local/offline and requires no external AI provider or API key. Build/test statements are only considered verified when the exact command has been executed in a real Android/Gradle environment.

## Current implementation snapshot — 2026-09-16

The 2026-09-16 UI integration keeps the native Kotlin/Compose architecture and adds the Stitch reference visual language to the Android implementation.
