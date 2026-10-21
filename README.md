# LifeOS

A private, local-first Android app for managing your own life data — notes, tasks,
habits, expenses, diary, captures, a timeline of real events, insights, search, and
trash/restore.

**There is no account, no server, and no analytics. Your data stays on your
device unless you export it yourself.**

## What it is

LifeOS is built around a simple promise: your personal data is yours. Everything is
stored in a local database on the device. The "LifeOS AI" features are an
on-device, deterministic, offline provider gated behind explicit, revocable
consent — not a cloud call.

## Highlights

- **Local-first** — Room/SQLite in app-private storage; no network calls.
- **Money done right** — amounts are stored as exact integer minor units plus an
  ISO currency code, never floating point.
- **Trash, not terror** — deleting moves items to Trash; permanent delete is a
  separate, confirmed step.
- **Consent-gated AI** — four graded levels, off by default, on-device only.
- **Point-of-use permissions** — camera/mic/location/notifications are requested
  only when the matching feature is used.
- **Portable** — a versioned JSON export you control.

## Tech stack

| Layer | Choice |
| --- | --- |
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose (BOM 2024.11.00), Material 3 |
| Architecture | MVVM, repositories, manual DI (`AppContainer`) |
| Persistence | Room 2.6.1 (schema v1) |
| Settings | DataStore Preferences |
| Build | Gradle 8.9, AGP 8.6.1, KSP, JDK 17 |

## Project layout

```
app/src/main/java/com/lifeos/app
├── core/designsystem   exact-palette tokens, theme, typography
├── core/database       Room entities, DAOs, database
├── core/domain         Money, Dates, Recurrence, HabitProgress, Tags
├── core/data           repositories + AppContainer (manual DI)
├── core/ai             AiProvider, AiConsent, LocalRuleBasedProvider
├── core/backup         DataExporter (versioned JSON + text)
├── feature/*           home, notes, capture, tracking, insights, life, search, settings
└── ui                  LifeOsApp nav graph, components, view-model factory
```

## Build

```bash
# Debug compile
./gradlew :app:compileDebugKotlin --offline

# JVM unit tests
./gradlew :app:testDebugUnitTest --offline

# Lint
./gradlew :app:lintDebug --offline

# Release (unsigned)
./gradlew :app:assembleRelease --offline
```

Set the Android SDK path in `local.properties` (`sdk.dir=...`) before building.

## Documentation

- [`PROJECT_AUDIT.md`](PROJECT_AUDIT.md) — honest status of what is implemented and
  what is verified.
- [`ARCHITECTURE.md`](ARCHITECTURE.md) — structure and data flow.
- [`IMPLEMENTATION_PLAN.md`](IMPLEMENTATION_PLAN.md) — the plan vs. actual status.
- [`SECURITY.md`](SECURITY.md) — threat model and guarantees (and non-guarantees).
- [`PRIVACY.md`](PRIVACY.md) — what is stored, where, and what leaves the device.
- [`TESTING.md`](TESTING.md) — exact verification commands and their limits.
- [`CHANGELOG.md`](CHANGELOG.md) — change history.
- [`NEXT_SESSION.md`](NEXT_SESSION.md) — handoff and next actions.

## Status

See [`PROJECT_AUDIT.md`](PROJECT_AUDIT.md). In short: the codebase is complete for
its v1 scope and compiles, with JVM unit tests for the domain and AI layers;
UI/persistence runtime behaviour has **not** been verified because no
device/emulator was available in the build environment.
