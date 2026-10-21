# Implementation Plan

This is the plan the codebase was built against, with the **actual status** of each
phase. Status values: `done` (implemented), `written-unverified` (code exists but
cannot run in this environment), `pending` (not implemented).

## Phase 0 — Foundation

| Item | Status |
| --- | --- |
| Gradle 8.9 / AGP 8.6.1 / Kotlin 2.0.21 / KSP pinning | done |
| Dependency version catalog (`libs.versions.toml`) | done |
| `local.properties` (SDK path), gitignore | done |
| Manifest with point-of-use permissions | done |
| Launcher icon + themes + strings | done |

## Phase 1 — Design system

| Item | Status |
| --- | --- |
| Exact palette (dark + light) as tokens | done |
| Semantic colour roles + documented contrast | done |
| Typography, spacing, radii, control-height tokens | done |
| `LifeOSTheme` + `LocalLifeOsTokens` | done |

## Phase 2 — Persistence

| Item | Status |
| --- | --- |
| 12 Room entities incl. trash columns | done |
| DAOs (notes, tracking, capture, search, life events) | done |
| `LifeOsDatabase` v1, converters, schema export | done |
| No destructive migration in release | done |

## Phase 3 — Domain + data

| Item | Status |
| --- | --- |
| `Money` (minor units only) | done |
| `Dates` (ISO local dates) | done |
| `Recurrence` + `RecurrenceRules` | done |
| `HabitProgress` (streaks) | done |
| Repositories + manual `AppContainer` | done |

## Phase 4 — AI (consent-gated, on-device)

| Item | Status |
| --- | --- |
| `AiProvider` seam + graded `AiConsent` | done |
| `LocalRuleBasedProvider` (deterministic, offline) | done |
| Remote provider | intentionally absent (no client keys) |

## Phase 5 — Features

| Screen | Status |
| --- | --- |
| Home | written-unverified |
| Notes (folders, tags) | written-unverified |
| Capture (photo/video/audio/thought/mood/location) | written-unverified — media write pending |
| Tracking (tasks, habits, expenses, diary) | written-unverified |
| Insights | written-unverified |
| Life (timeline, calendar, trash) | written-unverified |
| Search | written-unverified |
| Settings (+ export) | written-unverified |

## Phase 6 — Backup / export

| Item | Status |
| --- | --- |
| Versioned JSON exporter | done |
| Text summary exporter | done |
| Wired into Settings share sheet | done |
| Importer (restore) | pending |

## Phase 7 — Quality gates

| Gate | Status |
| --- | --- |
| `:app:compileDebugKotlin` | run in this environment |
| `:app:testDebugUnitTest` | run in this environment |
| `:app:lintDebug` | run in this environment |
| `:app:assembleRelease` | run in this environment |
| Instrumented / migration tests | declared; **cannot run** (no emulator) |

## Phase 8 — Docs + delivery

| Item | Status |
| --- | --- |
| `PROJECT_AUDIT.md` … `NEXT_SESSION.md` | done |
| Commit history in logical commits | done |
| Push to GitHub | pending (needs a PAT) |

## Explicitly out of scope for v1

- Cloud sync / multi-device.
- Remote AI.
- Database encryption (SQLCipher).
- Widgets, wearables, tablet-specific layouts.
