# Testing

This document lists the exact commands used to verify LifeOS and what they can and
cannot prove **in this environment**. Nothing here is claimed to have run unless a
result is recorded.

## Environment

| Tool | Version / path |
| --- | --- |
| JDK | 17 |
| Gradle | 8.9 (wrapper) |
| Android SDK | `/home/android-sdk` (platform 35) |
| Device / emulator | **none** |

Because there is no device or emulator, **instrumented (`androidTest`) tests and
Room migration runtime tests cannot be executed here.** They are declared in the
source tree but must be treated as *not run* until executed on real hardware or an
emulator.

## Commands

All commands run from the project root with `GRADLE_USER_HOME=/home/.gradle`.

### 1. Compile (Kotlin + KSP/Room)

```bash
./gradlew :app:compileDebugKotlin --offline
```

Proves: the Kotlin sources compile, the Compose compiler accepts the UI code, and
Room's annotation processor (KSP) validates every `@Dao`/`@Query`/entity mapping.
Does **not** prove: anything about runtime behaviour.

### 2. JVM unit tests

```bash
./gradlew :app:testDebugUnitTest --offline
```

Covers the pure, device-independent logic:

| Test | What it pins down |
| --- | --- |
| `MoneyTest` | parsing/formatting of exact minor units; no float drift |
| `DatesTest` | ISO date round-trips, relative labels |
| `RecurrenceRulesTest` | daily/weekly/monthly/yearly/interval/days-of-week occurrence logic |
| `HabitProgressTest` | streak and completion-rate computation |
| `TagsTest` | tag normalisation/deduplication |
| `LocalRuleBasedProviderTest` | the on-device AI provider is deterministic and offline |

### 3. Android lint

```bash
./gradlew :app:lintDebug --offline
```

Static analysis over resources, manifest, and Kotlin. Produces an HTML report at
`app/build/reports/lint-results-debug.html`.

### 4. Release build

```bash
./gradlew :app:assembleRelease
```

Proves the release variant compiles and R8/minification config is valid. The
unsigned release APK is produced for inspection only.

## Recorded results

These are the literal outcomes observed in the build environment (Gradle 8.9,
JDK 17, no device/emulator):

| Command | Outcome |
| --- | --- |
| `./gradlew :app:compileDebugKotlin --offline` | BUILD SUCCESSFUL — Kotlin + Compose + KSP/Room validated |
| `./gradlew :app:testDebugUnitTest --offline` | BUILD SUCCESSFUL — 79 tests, 0 failures, 0 errors, 0 skipped |
| `./gradlew :app:lintDebug` | BUILD SUCCESSFUL — 0 errors, 72 warnings |
| `./gradlew :app:assembleRelease` | BUILD SUCCESSFUL (19m 20s) — R8 minify, resource shrink, lintVital all passed |

Test breakdown (from `app/build/test-results/testDebugUnitTest/`):

| Test class | Tests | Failures | Errors |
| --- | --- | --- | --- |
| `MoneyTest` | 18 | 0 | 0 |
| `LocalRuleBasedProviderTest` | 17 | 0 | 0 |
| `RecurrenceRulesTest` | 14 | 0 | 0 |
| `HabitProgressTest` | 13 | 0 | 0 |
| `TagsTest` | 9 | 0 | 0 |
| `DatesTest` | 8 | 0 | 0 |

Note: `lintDebug` required network access to resolve an `androidTest`-variant
dependency (`androidx.collection:1.4.0`) that is not in the offline cache; it was
run without `--offline`. This does not affect the app's runtime dependencies.

## What is NOT tested here

- UI rendering, navigation, and Compose behaviour (needs a device).
- Room persistence at runtime, including trash/restore round-trips (needs a
  device).
- Camera, microphone, and location capture (needs real hardware and runtime
  permission grants).
- The Android share sheet (needs a device).
- Notifications (worker not implemented yet — see `PROJECT_AUDIT.md`).
- Any performance or battery measurement.

## Honesty rule

If a result is not in this file, it did not happen. Test output is recorded as the
literal command result, never paraphrased into a stronger claim.
