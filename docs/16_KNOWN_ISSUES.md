# 16 — Known Issues

---

### Issue #1 — Missing Gradle wrapper scripts

- **Severity**: 🟠 High (blocks command-line builds)
- **Description**: `gradle/wrapper/gradle-wrapper.properties` exists but
  `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.jar` do not.
- **Reproduction**: Run `./gradlew assembleDebug` from a fresh clone.
- **Expected behavior**: Gradle wrapper downloads/builds the project.
- **Actual behavior**: `bash: ./gradlew: No such file or directory`
- **Possible cause**: These files were not generated because the project
  was authored without ever running Gradle (no SDK/network access available
  during development — see `docs/13_DEPLOYMENT.md`).
- **Current workaround**: Open in Android Studio (usually self-heals), or
  run `gradle wrapper --gradle-version 8.9` from a machine with Gradle installed.
  The CI workflow (`.github/workflows/android-build.yml`, added after this
  issue was first logged) works around it by installing Gradle directly via
  `gradle/actions/setup-gradle` and running `gradle assembleDebug` instead
  of `./gradlew assembleDebug` — but this is a workaround, not a fix; local
  command-line builds still need one of the two options above.
- **Status**: Open (CI workaround in place; local `gradlew` still missing)

---

### Issue #2 — Production keystore not supplied

- **Severity**: 🟠 High (blocks any real release)
- **Description**: Release signing scaffolding exists, but a production keystore is not part of the repository and must remain outside source control.
- **Reproduction**: Run `./gradlew assembleRelease`.
- **Expected behavior**: A signed, installable release APK.
- **Actual behavior**: Without `keystore.properties`, the release build uses the configured debug signing fallback; this is suitable for build verification, not Play Store distribution.
- **Current workaround**: None needed. `app/build.gradle.kts` now defines a
  `signingConfigs.release` block driven by an optional `keystore.properties`
  file. When the file is absent (as in CI and local debug), release builds fall
  back to debug signing; when present, they produce a properly signed APK.
  Copy `keystore.properties.example` to create one.
- **Status**: Resolved (scaffolding in place). A real production keystore still
  must be supplied via `keystore.properties` before Play Store submission.

---

### Issue #3 — Backup restore behavior needs broader verification

- **Severity**: 🟡 Medium
- **Description**: JSON restore is now reachable from both Onboarding and Settings through Android Storage Access Framework and the existing `BackupRepository`, and `restore()` now runs inside a single `withTransaction { }` so a failed import cannot leave the database half-written.
- **Current state**: UI wiring and transactional restore are present; full restore behavior still requires execution against representative valid/invalid backups in a real Android environment.
- **Status**: Verification pending

---

### Issue #4 — [RESOLVED] Task recurrence (`RepeatRule`) is stored but never acted on

- **Severity**: 🟡 Medium
- **Description**: `TaskEntity.repeatRule` (`NONE/DAILY/WEEKLY/MONTHLY/CUSTOM_DAYS`) and `repeatDaysCsv` are now read on completion: `TaskRepository.setCompleted()` calls `RepeatRuleCalculator.nextOccurrence()` and spawns the next occurrence. Overdue tasks reschedule from today rather than back-filling stale dates, and the new instance deliberately does not inherit the reminder.
- **Status**: Resolved. Covered by `RepeatRuleCalculatorTest`.

---

### Issue #5 — [RESOLVED] No UI to set task priority, category, description, or repeat rule

- **Severity**: 🟢 Low
- **Description**: The "Add task" dialog in `ui/tasks/TasksScreen.kt` now exposes title, description, category, a priority dropdown (`Low/Medium/High`) and a repeat dropdown (`Doesn't repeat/Daily/Weekly/Monthly`), alongside the existing reminder picker. `TasksViewModel.addTask()` persists all of these.
- **Status**: Resolved.

---

### Issue #6 — [RESOLVED] Habit completions are not cleaned up when a habit is deleted

- **Severity**: 🟢 Low
- **Description**: `HabitRepository.delete()` now calls `HabitCompletionDao.deleteForHabit(id)` before deleting the habit, so no orphaned `habit_completions` rows remain.
- **Status**: Resolved.

---

### Issue #7 — AI Assistant chat does not use real app data as context

- **Severity**: 🟢 Low (functional limitation, not a bug)
- **Description**: `AiRepository.chat()` supports an optional `contextBlock`
  parameter, but `ui/ai/AiAssistantScreen.kt` always passes `null`.
- **Reproduction**: Ask the AI Assistant "What tasks do I have today?"
- **Expected behavior** (if this were wired up): A context-aware answer
  referencing real task data.
- **Actual behavior**: The AI has no way to know; it will either say so or
  hallucinate a generic answer.
- **Current workaround**: None — this is a scoped-out feature, not a defect
  in what exists.
- **Status**: Open

---

### Issue #8 — [RESOLVED] No database encryption

- **Severity**: 🟠 High (security)
- **Description**: The Room database is now encrypted at rest with SQLCipher; the passphrase is random per install and wrapped by an Android Keystore AES-GCM key. See `docs/08_SECURITY.md`.
- **Status**: Resolved. There is no cloud AI key to encrypt — the Intelligence engine is fully on-device.

---

### Issue #9 — [RESOLVED] No automated tests exist

- **Severity**: 🟡 Medium (process/quality risk, not a functional bug)
- **Description**: A JVM unit-test suite now covers PIN hashing, habit stats, repeat rules, date math, offline intelligence and backup serialization; CI runs `gradle test`. See `docs/14_TESTING.md`.
- **Status**: Resolved. Device/instrumentation coverage is still a future addition.

---

### Issue #10 — [RESOLVED] Capture gave no confirmation after Photo/Video/Audio capture

- **Severity**: 🟠 High (was — core UX bug)
- **Description**: `CaptureSheet.kt` wrote the file and database row correctly, then called `onDismiss()` immediately — the bottom sheet closed with no visible feedback, making it look like capture had silently failed even though the data was saved correctly.
- **Fix**: `CaptureSheet.kt` now shows a CONFIRM state with a real preview of the captured file (`ui/capture/CaptureMediaPreview.kt`) and a "Done" button before closing. A new `CaptureDetailScreen.kt` also lets the user reopen any captured item later from the Timeline (tap-to-open was added to `TimelineScreen.kt` for capture items).
- **Status**: Resolved in the UI/UX polish pass.

---

### Issue #11 — [RESOLVED] No way to set an AI API key through the UI

- **Severity**: Was 🟠 High
- **Description**: Previously, AI features required a cloud API key with no UI to enter one.
- **Fix**: The entire cloud AI dependency was removed. AI features are now powered by the on-device LifeOS Intelligence Engine (`core/intelligence/`) — no key of any kind is required, ever. See `docs/11_AI_SYSTEM.md` for the full rewrite.
- **Status**: Resolved — this was a bigger fix than closing the gap; the underlying need for a key no longer exists at all.

### Issue #12 — Permanently-denied permissions gave no path forward

- **Severity**: Was 🟠 High (UX — felt broken/stuck)
- **Description**: `core/util/PermissionManager.kt`'s old `rememberPermissionState()` had no way to distinguish "denied, can ask again" from "permanently denied" (Android silently no-ops the system dialog after a second denial). Tapping "Grant permission" after a permanent denial did nothing visible, reading as a repeated/broken request.
- **Fix**: `PermissionManager.kt` now exposes a `PermissionStatus` (GRANTED / NOT_YET_REQUESTED_OR_DENIABLE / PERMANENTLY_DENIED) via `ActivityCompat.shouldShowRequestPermissionRationale()`. All three capture screens now show "Open Settings" instead of a dead-end "Grant permission" button once a permission is permanently denied.
- **Status**: Resolved.

### Issue #13 — [RESOLVED] Biometric App Lock removed

- **Severity**: Was 🟡 Medium.
- **Description**: The former biometric implementation depended on AndroidX `BiometricPrompt` and device-level enrollment.
- **Resolution**: On 2026-09-16 the biometric option, setup flow, runtime prompt, `AppLockManager` and AndroidX Biometric dependency were removed. App Lock is now PIN-only.
- **Compatibility**: A legacy stored `BIOMETRIC` setting is interpreted as `NONE` because a biometric configuration contains no LifeOS PIN secret that can safely be converted.
- **Status**: Resolved.


## Current-state addendum — 2026-09-16

This document remains part of the LifeOS documentation set. Current UI/UX, motion, responsive and accessibility rules are centralized in [`DESIGN.md`](./DESIGN.md). The current Intelligence implementation is local/offline and requires no external AI provider or API key. Build/test statements are only considered verified when the exact command has been executed in a real Android/Gradle environment.

## Current implementation snapshot — 2026-09-16

The remaining verification gap is build/device validation in a complete Android environment. The UI source itself was reviewed for the requested navigation and capture changes.


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
