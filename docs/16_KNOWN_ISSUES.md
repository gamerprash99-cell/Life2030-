# 16 — Known Issues

---

### Issue #1 — Gradle wrapper (resolved)

- **Severity**: 🟢 Resolved
- **Description**: The wrapper now ships in the repository: `gradlew`,
  `gradlew.bat` and `gradle/wrapper/gradle-wrapper.jar` are all present.
- **Note**: In some Linux containers the `gradlew` file is not marked
  executable after checkout; invoke it as `sh ./gradlew …` there.
- **Status**: Resolved (2026-10-30).

---

### Issue #2 — No production release keystore

- **Severity**: 🟠 High (blocks any real release)
- **Description**: `app/build.gradle.kts` defines a `signingConfigs.release`
  block driven by an optional `keystore.properties` file, but the repository
  intentionally contains no real signing key.
- **Reproduction**: Run `./gradlew assembleRelease` with no
  `keystore.properties`.
- **Expected behavior**: A release-signed APK suitable for distribution.
- **Actual behavior**: The release build compiles and falls back to debug
  signing so CI stays green; the APK is not suitable for Play submission.
- **Current workaround**: Copy `keystore.properties.example` to
  `keystore.properties` and point it at a real keystore.
- **Status**: Scaffolding resolved; a real production keystore still must be
  supplied before Play Store submission.

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

## 2026-09-18 status update
The onboarding second-launch flash and navigation Back-handler path were addressed in source. Audio recording cleanup/playback lifecycle was also hardened. Build-level confirmation remains pending because the supplied archive has no Gradle wrapper and no system Gradle executable is available in this environment.


---

### Issue #4 — Instrumentation migration tests not executed in this environment

- **Severity**: 🟡 Medium (release gate)
- **Description**: `AppDatabaseMigrationTest` (including the new populated
  v6→v7 case) compiles and packages into `app-debug-androidTest.apk`, but no
  `adb`, emulator or physical device is available in the build environment.
- **Current workaround**: Run `./gradlew :app:connectedDebugAndroidTest` on a
  device/emulator before release.
- **Status**: Verification pending

---

### Issue #5 — No manual on-device pass for the permanent Habits flow

- **Severity**: 🟡 Medium
- **Description**: The wizard, all three tracking modes, every frequency,
  Quit slips, archive/restore, the celebration overlay and reminder repeat
  changes are covered by unit tests and compile cleanly, but no human/device
  UI pass has been performed.
- **Current workaround**: Use `docs/14_TESTING.md`'s manual acceptance
  checklist and add the Habits-specific cases listed there.
- **Status**: Verification pending

---

### Issue #6 — [RESOLVED] Habit wizard step 1 showed an oversized blank blocker

- **Severity**: 🟠 High (first-run flow)
- **Description**: On the first wizard step the primary tab bar was still
  composed under the wizard's own Continue bar, and the wizard bar added a
  navigation-bar inset the host Scaffold had already reserved. Together they
  produced a 150–190dp band of empty chrome around Continue.
- **Fix (2026-10-31)**: The Habit editor route is excluded from the primary
  bottom bar and marked as owning its bottom inset, so the root `LifeOSNavHost`
  hands it the full window height exactly like the Diary composer; the wizard
  bar's single `navigationBarsPadding()` is then applied once.
- **Files**: `ui/navigation/BottomBarVisibility.kt`,
  `ui/navigation/LifeOSNavHost.kt`, `ui/habits/HabitEditorScreen.kt`.
- **Status**: Resolved; device pass still recommended on small/gesture-nav
  screens (see Issue #5).

### Issue #7 — [RESOLVED] Habit check-in could not be undone clearly

- **Severity**: 🟠 High (reported)
- **Description**: A successful check-in changed the control to "Done" with no
  advertised reversal; a completed Count habit's stepper was disabled at the
  target, so it could not be corrected; and no day-scoped undo existed.
- **Fix (2026-10-31)**: Day-scoped `setTickForDay` / `undoCheckIn` in the
  ViewModel, an "Undo" snackbar after a successful check-in, an accessibility
  "tap to undo" label on Done, and a Count stepper that keeps decrement enabled
  once progress exists. The Room row stays keyed by `(habitId, dateEpochDay)`,
  so repeated taps upsert and undo removes only that day's row.
- **Files**: `ui/habits/HabitsViewModel.kt`, `ui/habits/HabitsScreen.kt`.
- **Status**: Resolved; no database schema or migration change.

### Issue #8 — [RESOLVED] Declining a permission silently cancelled a save

- **Severity**: 🟠 High (looked like data loss)
- **Description**: Habit creation and both task flows ran the save action only
  after notification / exact-alarm access was granted. Choosing "Not now" left
  the form open and created nothing.
- **Fix (2026-10-31)**: Both permission hosts gained
  `runProtectedOrContinue`, so the entity is always persisted and the reminder
  degrades to the scheduler's permission-free path. `ReminderPermissionHost`
  now also re-checks on resume so the "Open Settings" route can release the
  pending action.
- **Files**: `ui/components/ReminderPermissionHost.kt`,
  `ui/components/ExactAlarmPermissionHost.kt`, `ui/habits/HabitEditorScreen.kt`,
  `ui/habits/HabitDetailScreen.kt`, `ui/tasks/TasksScreen.kt`.
- **Status**: Resolved.

### Issue #9 — [RESOLVED] Home recent activity opened the wrong screen

- **Severity**: 🟡 Medium
- **Description**: Every Home recent-activity row called the Snap viewer
  callback, so tapping a diary, task, habit or expense row opened the capture
  viewer with an id that could not exist.
- **Fix (2026-10-31)**: Added `routeForTimelineItem` and routed each
  `TimelineItemType` to its real destination, pinned by
  `TimelineNavigationTest`.
- **Files**: `ui/navigation/Screen.kt`, `ui/navigation/LifeOSNavHost.kt`,
  `ui/home/HomeScreen.kt`.
- **Status**: Resolved.

### Issue #10 — [RESOLVED] Duplicated window insets on several destinations

- **Severity**: 🟡 Medium
- **Description**: The host Scaffold already applied the status/navigation
  insets, but some destinations asked for them again: nested Scaffolds in
  Timeline/Settings/Expenses, and the full-window Note editor, AI assistant,
  Capture hub and Snap camera overlays.
- **Fix (2026-10-31)**: Insets are applied once per destination: the host zeroes
  top/bottom padding for routes that own those insets
  (`ownsTopInset`/`ownsBottomInset`), the three nested Scaffolds were zeroed
  like Tasks/Habits/Profile, and the camera overlays dropped their duplicate
  padding.
- **Files**: `ui/navigation/BottomBarVisibility.kt`,
  `ui/navigation/LifeOSNavHost.kt`, `ui/capture/CameraCaptureScreen.kt`,
  `ui/capture/VideoCaptureScreen.kt`, `ui/timeline/TimelineScreen.kt`,
  `ui/settings/SettingsScreen.kt`, `ui/expenses/ExpensesScreen.kt`.
- **Status**: Resolved; visual confirmation on a device is still listed in
  `docs/14_TESTING.md`.
