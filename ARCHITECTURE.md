# Architecture

## 1. Shape

Single-module Android app (`:app`), package-per-layer, MVVM with repositories and
a **manual dependency container** (no Hilt). One app, one graph, assembled once in
`LifeOsApplication` and passed explicitly. This is a deliberate choice: an
annotation-processor DI framework would add build time and indirection for a graph
this small.

```
com.lifeos.app
├── LifeOsApplication        app entry, builds AppContainer
├── MainActivity             single activity, sets Compose content
├── core
│   ├── designsystem/theme   Color, Theme, Type, Tokens (exact palette)
│   ├── database             Entities, DAOs, LifeOsDatabase (Room v1)
│   ├── domain               pure Kotlin: Money, Dates, Recurrence, HabitProgress, Tags
│   ├── data                 repositories + AppContainer (manual DI)
│   ├── ai                   AiProvider + AiConsent + LocalRuleBasedProvider
│   └── backup               DataExporter (dependency-free JSON/text)
├── feature
│   ├── home                 dashboard
│   ├── notes                notes + folders + tags
│   ├── capture              photo/video/audio/thought/mood/location
│   ├── tracking             tasks, habits, expenses, diary
│   ├── insights             on-device summaries (AI-gated)
│   ├── life                 timeline, calendar, trash
│   ├── search               global search, grouped by type
│   └── settings             appearance, money, AI consent, data
└── ui                       LifeOsApp (nav graph), AppViewModelFactory, components
```

## 2. Data flow

```
UI (Composable)
  → ViewModel (StateFlow)         holds state, exposes intent functions
    → Repository                  maps entities ↔ domain, owns transactions
      → DAO (Room)                Flow queries, suspend writes
        → SQLite                  app-private database
```

Compose reads state with `collectAsStateWithLifecycle()`; ViewModels are created by
`AppViewModelFactory`, which reads the single `AppContainer`.

## 3. Persistence rules

1. **Money** — integer minor units (`Long`) + ISO currency code. Never
   `Float`/`Double`. Formatting is centralised in `core/domain/Money.kt`.
2. **Dates** — local calendar dates are ISO `yyyy-MM-dd` strings so that grouping
   and range queries are lexicographic and timezone-stable. Instants are epoch
   milliseconds.
3. **Deletion** — soft delete via `deletedAt` (0 = live). Restore clears it.
   Permanent delete is explicit and separate.
4. **Migrations** — schema v1. Destructive fallback is disabled on purpose.

## 4. AI architecture

`AiProvider` is the only seam. `AiConsent` defines four graded levels:

| Level | Name | Effect |
| --- | --- | --- |
| 0 | Off | nothing is computed |
| 1 | Summaries | extractive summaries, on-device |
| 2 | Tagging | summaries + keyword tags |
| 3 | Suggestions | summaries + tags + suggestions |

The only wired implementation is `LocalRuleBasedProvider`: deterministic,
offline, no network, no key. A remote provider is intentionally absent. Every AI
surface in the UI reads the consent level and disables itself when the level is
lowered; there is no path that computes AI output at consent level 0.

## 5. Export

`DataExporter` produces a versioned (`FORMAT_VERSION = 1`) JSON document plus a
plain-text summary, using a hand-written escaper instead of pulling a
serialization dependency. It returns a `String`; the UI decides where it goes
(system share sheet). Nothing in this path touches the network.

## 6. Navigation

Single `NavHost` in `ui/LifeOsApp.kt`. Five primary destinations (Home, Notes,
+Capture, Insights, Life) with secondary destinations (Timeline, Calendar, Trash,
Search, Settings) reachable from them.

## 7. Testing strategy

- **JVM unit tests** cover the pure domain layer (`Money`, `Dates`, `Recurrence`,
  `HabitProgress`, `Tags`) and the local AI provider. These run anywhere.
- **Instrumented tests** are declared for Room and UI but cannot run in this
  environment (no emulator). They are never reported as passing.
