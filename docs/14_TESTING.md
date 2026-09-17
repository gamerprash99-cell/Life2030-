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


---

## Current source snapshot — 2026-09-17

This documentation set is aligned to the supplied LifeOS Android source snapshot. The source of truth is the Kotlin/Jetpack Compose implementation under `app/src/main/java/com/lifeos/app/`, together with `app/build.gradle.kts` and `app/src/main/AndroidManifest.xml`.

### Verified architecture facts
- Native Kotlin Android application using Jetpack Compose + Material 3.
- Navigation uses Navigation Compose with a `root_tabs` nested graph for Home, Tasks, Habits and Insights; secondary screens remain stackable routes.
- Local persistence uses Room/SQLite with SQLCipher for database-at-rest encryption, plus DataStore for preferences/settings.
- Repositories and use cases remain the application data boundary; UI does not directly own Room access.
- Offline intelligence is implemented under `core/intelligence/` using deterministic local analyzers, rules, lexicons, statistics and templates.
- Capture uses CameraX for photo/video and Android `MediaRecorder` for audio, with runtime permissions requested only when capture is selected.
- Backup/restore is local and uses Android Storage Access Framework/document picker flows.
- App Lock is PIN-only (`NONE` / `PIN`) in the current source; no cloud identity or biometric App Lock implementation is present.
- The manifest does not declare `android.permission.INTERNET`.

### Verification boundary
The repository snapshot supplied for this documentation pass does not contain the Gradle wrapper scripts/JAR. Therefore this environment does not claim a fresh Gradle build, instrumentation run, or physical-device verification unless an executed command is recorded elsewhere in the project history.

### Maintenance rule
Historical sections are intentionally retained for traceability. When historical documentation conflicts with the current source, the current source and the latest dated current-state section take precedence; historical changelog entries should not be rewritten merely to make history appear current.
