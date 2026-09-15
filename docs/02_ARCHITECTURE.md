# 02 — Architecture

## Current architecture

LifeOS is a single native Android application. The intended flow remains:

```text
Compose UI
   ↓
ViewModel / UI state
   ↓
Use cases / repositories
   ↓
Room DAOs / DataStore / local services
```

Cross-feature timeline and Intelligence composition use existing domain/repository layers rather than duplicating persistence in UI.

## Major layers

- `ui/` — Compose screens, navigation and reusable visual components
- `domain/` — domain models and use cases
- `data/` — Room database and repositories
- `core/intelligence/` — deterministic local analysis
- `core/reminders/` — WorkManager scheduling
- `core/security/` — App Lock and PIN support
- `core/util/` — dates, permissions, settings, media and notifications

## Navigation

Navigation remains Navigation Compose with one `NavController` owned by `LifeOSNavHost`. Existing routes and back-stack behavior are preserved.

## Offline boundary

There is no backend service and no external AI endpoint in the current source. `core/ai/AiRepository.kt` is a compatibility facade over `LifeOSIntelligenceEngine`.

## UI architecture

The visual system is centralized in `ui/theme/` and reusable components in `ui/components/`. See [`DESIGN.md`](./DESIGN.md) for the current specification.

## Database

Room remains the persistence layer. No database schema change was required for the UI/UX work described in the current snapshot.

## Security

App Lock uses AndroidX BiometricPrompt for device-level biometrics and the existing local PIN hashing/recovery path for PIN mode. LifeOS never stores biometric templates.

## Current-state addendum — 2026-09-16

This document remains part of the LifeOS documentation set. Current UI/UX, motion, responsive and accessibility rules are centralized in [`DESIGN.md`](./DESIGN.md). The current Intelligence implementation is local/offline and requires no external AI provider or API key. Build/test statements are only considered verified when the exact command has been executed in a real Android/Gradle environment.

## Current implementation snapshot — 2026-09-16

UI changes continue to consume repositories, use cases and ViewModels; no UI path was added that accesses Room directly.
