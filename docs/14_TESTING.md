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

The repository ships the Gradle wrapper (`gradlew`, `gradle/wrapper/`), so the
local commands are `./gradlew test` / `./gradlew assembleDebug` (in this
environment the wrapper file is not executable, so it was invoked as
`sh ./gradlew ...`). Instrumentation/UI tests still require an emulator or
device. Build/test statements are only considered verified when the exact
command has actually executed in a real Android/Gradle environment.

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

---

## Permanent Habits verification — 2026-10-30 (executed)

Environment: JDK `17.0.21`, Android SDK at `/opt/android-sdk`, AAPT2 supplied
via `-Pandroid.aapt2FromMavenOverride=/opt/aapt2-wrapper/aapt2` because the
Maven AAPT2 daemon fails to start in this container. Wrapper invoked as
`sh ./gradlew` (the file is not executable here).

```text
sh ./gradlew :app:compileDebugKotlin -Pandroid.aapt2FromMavenOverride=/opt/aapt2-wrapper/aapt2
  → BUILD SUCCESSFUL

sh ./gradlew :app:testDebugUnitTest -Pandroid.aapt2FromMavenOverride=/opt/aapt2-wrapper/aapt2
  → BUILD SUCCESSFUL — 486 tests, 0 failures, 0 errors, 0 skipped
    (38 XML result files parsed from app/build/test-results/testDebugUnitTest)

sh ./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug -Pandroid.aapt2FromMavenOverride=/opt/aapt2-wrapper/aapt2
  → BUILD SUCCESSFUL
    :app:assembleDebug               (app/build/outputs/apk/debug/app-debug.apk)
    :app:assembleDebugAndroidTest    (app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk)
    :app:lintDebug                   0 errors, 32 warnings (all pre-existing:
                                     GradleDependency version hints, launcher
                                     resources, one PluralsCandidate, legacy
                                     ModifierParameter/ObsoleteSdkInt warnings)
```

Permanent-Habits-specific coverage added/updated:

- `HabitStatsCalculatorTest` — every schedule type, month-day short-month
  handling, N-per-week/month due rules and period streaks, "100% is a ceiling"
  for flexible schedules, completion outcome/target snapshots, legacy mapping.
- `AppDatabaseSchemaTest` — now parses exported **v7** schema; asserts the
  exact 11-table set and the new habit/completion/checklist columns.
- `AppDatabaseMigrationTest` — new `migrate6To7_*` populated-data test:
  existing habit + completion survive byte-for-byte, new columns get valid
  defaults, checklist tables exist/writable. **Compiled but not executed**
  (no emulator/device in this environment).
- `BackupSerializationTest` — checklist template/check round-trip, v1 file
  decodes with empty checklist defaults, format version is 2.
- `BottomNavItemsTest` — bar is exactly Home · Notes · Habits · Life and
  Profile/Insights are not tabs.

Not executed here: `:app:connectedDebugAndroidTest` (no `adb`, no emulator or
device). No pass is claimed for instrumentation tests.

### Permanent Habits manual cases (run on device)

- First launch with no habits -> invitation card -> guided wizard, all 10 steps,
  back/forward, discard guard, create and see the card.
- Second habit via FAB opens the full form (no wizard) and saves.
- Edit an existing **legacy CUSTOM** habit and confirm its weekdays/frequency
  survive.
- Tick a habit, undo it; count up/down; check all checklist items and undo one.
- Quit habit: "stayed on track", "I slipped", undo, and the detail note.
- Every frequency: every day, selected weekdays, month days (pick 31, view a
  30-day month), Nx/week and Nx/month rest-day behaviour.
- Date strip: select a past day, backfill it, return to Today.
- Filters All/Due/Completed counts; score on a rest day.
- Heatmap day tap -> dialog -> mark done/undo; future day is blocked.
- Reminder set/change time/change repeat (daily -> weekdays)/remove.
- Archive -> archived sheet -> restore; delete with confirmation.
- Celebration appears only on a real transition to done; Share opens the
  system sheet; system "remove animations" still shows the static overlay.
- Rotation/process death: selected date and unsaved editor state behave per
  the guard.

