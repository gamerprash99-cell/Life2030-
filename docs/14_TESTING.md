# 14 — Testing

## Current verification status — 2026-09-16

A JVM unit-test suite has been added under `app/src/test/` and CI runs it as
part of `.github/workflows/android-build.yml` (`gradle test`):

- `PinHasherTest` — PBKDF2 round-trip, legacy SHA-256 compatibility, malformed input
- `HabitStatsCalculatorTest` — current/longest streak, completion percent
- `RepeatRuleCalculatorTest` — next-occurrence math for all repeat rules
- `DateTimeUtilsTest` — date/minute conversions
- `MoodAnalyzerTest` / `NoteTextAnalyzerTest` — offline intelligence heuristics
- `BackupSerializationTest` — full backup JSON round-trip for every entity

The repository still does not ship `gradlew`, so the exact local commands are
`gradle test` and `gradle assembleDebug` (fully qualified once the wrapper is
generated). Instrumentation/UI tests should be run when an emulator is available.
Build/test statements are only considered verified when the exact command has
actually executed in a real Android/Gradle environment.

## Manual acceptance checklist

- Fresh install
- Onboarding page navigation
- Skip
- Get started
- JSON restore from Android document picker
- Invalid JSON handling
- Valid backup restore
- Dynamic Home greeting/date
- Tasks
- Habits
- Diary
- Expenses
- Notes
- Timeline
- Capture
- Settings
- App Lock None
- App Lock PIN
- App Lock PIN lockout after five failed attempts
- PIN recovery
- Back navigation
- Bottom navigation
- Small screen
- Large screen
- Font scaling
- Dark mode

## UI-specific checks

- No content under system navigation bars
- Interactive targets at least 48dp
- No clipped text at supported font scales
- No hardcoded current dates/times/statistics
- Animations remain short and non-blocking
- Long lists use lazy containers where appropriate

Never document a test as passed unless the exact check was actually executed.

## Current-state addendum — 2026-09-16

This document remains part of the LifeOS documentation set. Current UI/UX, motion, responsive and accessibility rules are centralized in [`DESIGN.md`](./DESIGN.md). The current Intelligence implementation is local/offline and requires no external AI provider or API key. Build/test statements are only considered verified when the exact command has been executed in a real Android/Gradle environment.

## Current implementation snapshot — 2026-09-16

Static source review was performed after the UI changes. Full Gradle tests/assemble could not be executed because the supplied snapshot has no Gradle wrapper and this environment has no usable Android Gradle setup.

## 2026-09-18 verification note
The changed sources were statically reviewed. Automated Gradle tests/builds were not runnable from the supplied archive because the Gradle wrapper and system Gradle executable are unavailable in the current environment. Do not interpret this as a passing build; CI/local Android verification remains required.

## 2026-09-19 verification note — actually executed

The Expenses micro UX fix was verified by **running** the project's checks in an
environment with Gradle 8.9 + AGP 8.6.1 (a locally-installed Gradle distribution,
since the repo still ships no `gradlew`; offline with the engine's aapt2
override). Exact commands and results:

```text
gradle :app:compileDebugKotlin   -> BUILD SUCCESSFUL
gradle :app:assembleDebug        -> BUILD SUCCESSFUL
gradle :app:testDebugUnitTest    -> BUILD SUCCESSFUL (81 tests, 0 failures)
gradle :app:lintDebug            -> BUILD SUCCESSFUL (0 errors, 4 pre-existing warnings)
```

The 4 lint warnings are pre-existing and unrelated to the fix (obsolete `-v26`
resource folder, unused round launcher icon, unused `tagline` string,
missing monochrome launcher-icon tag; plus 3 informational autoboxing hints).
No new unit test was required for this change — it is pure Compose presentation
and reuses existing state; no domain/repository logic changed.

Still **not** executed (honest gap): instrumentation tests (no emulator/device
and no `app/src/androidTest/` source set) and a manual on-device UI walkthrough
of the two Expenses interactions.
