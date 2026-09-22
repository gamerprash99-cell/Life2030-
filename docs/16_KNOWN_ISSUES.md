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

### Issue #2 — No release signing configuration

- **Severity**: 🟠 High (blocks any real release)
- **Description**: `app/build.gradle.kts` has no `signingConfigs` block.
- **Reproduction**: Run `./gradlew assembleRelease`.
- **Expected behavior**: A signed, installable release APK.
- **Actual behavior**: An unsigned APK is produced (or the build may need
  additional configuration depending on Gradle/AGP defaults).
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


### Issue #14 — [RESOLVED] Add-expense sheet opened too low

- **Severity**: Was 🟡 Medium (UX — extra manual step every time)
- **Description**: The "Add expense" `ModalBottomSheet` in `ui/expenses/ExpensesScreen.kt` used the default sheet state and opened at the *partially expanded* position; the user had to drag it upward to see the whole form.
- **Fix**: The existing sheet now uses `rememberModalBottomSheetState(skipPartiallyExpanded = true)` and a scrollable body, so it opens expanded and all fields stay reachable under IME/landscape/font scaling. Design, fields, buttons, drag handle, swipe-to-dismiss and Back behavior are unchanged.
- **Status**: Resolved (compile/test/lint verified; on-device walkthrough still recommended).

---

### Issue #15 — [RESOLVED] Selected expense category had no clear visual state

- **Severity**: Was 🟡 Medium (UX — selection only implied by text)
- **Description**: Category `GlassChip`s in "Add expense" showed selection only through the "Selected: <name>" line; the chips themselves looked identical.
- **Fix**: `GlassChip` (`ui/components/GlassCard.kt`) gained an optional `selected` parameter that uses `MaterialTheme.colorScheme.primary`/`onPrimary`; the Expenses call site drives it from the existing single `selectedCategory` state. Exactly one chip is highlighted at a time.
- **Status**: Resolved (compile/test/lint verified; on-device walkthrough still recommended).


## Current-state addendum — 2026-09-16

This document remains part of the LifeOS documentation set. Current UI/UX, motion, responsive and accessibility rules are centralized in [`DESIGN.md`](./DESIGN.md). The current Intelligence implementation is local/offline and requires no external AI provider or API key. Build/test statements are only considered verified when the exact command has been executed in a real Android/Gradle environment.

## Current implementation snapshot — 2026-09-16

The remaining verification gap is build/device validation in a complete Android environment. The UI source itself was reviewed for the requested navigation and capture changes.

## 2026-09-18 status update
The onboarding second-launch flash and navigation Back-handler path were addressed in source. Audio recording cleanup/playback lifecycle was also hardened. Build-level confirmation remains pending because the supplied archive has no Gradle wrapper and no system Gradle executable is available in this environment.

## 2026-09-19 status update
The two Expenses UX issues (#14 expanded sheet, #15 category highlight) are
fixed. Build/test/lint were actually executed this pass
(`assembleDebug`, `testDebugUnitTest` 81/0, `lintDebug` 0 errors) — see
`docs/14_TESTING.md`. Issue #1 (missing Gradle wrapper scripts) remains open;
the remaining verification gap is device/instrumentation testing, since no
emulator/device and no `androidTest` source set are available.

---

### Issue #16 — [RESOLVED] Home cold-start freeze and overlapping empty states

- **Severity**: Was 🟠 High (felt broken/unresponsive for seconds on every cold launch)
- **Description**:
  1. SQLCipher's one-time key derivation ran on Room's query executor at the
     *first* Home query — after the UI was on screen — stalling Home data (and
     responsiveness to taps/Back) for ~4–5 s.
  2. `GetHomeSummaryUseCase.assemble()` issued one full-history analytics query
     per active habit plus a serial 7-stream `BuildTimelineUseCase`, all on the
     collector thread, compounding the stall.
  3. Home's two empty `LifeOSCard`s passed sibling `Text`s into the card's
     internal `Box`, so "No routines yet" / "No activity recorded today"
     titles overlapped their body text (most visible exactly during the stall).
- **Fix**: `AppDatabase.warmUpOpen()` opens the DB on a background dispatcher
  at app start; the summary flow runs `flowOn(Dispatchers.Default)` and uses
  `HabitRepository.computeAnalyticsBatch` (one completions read); the timeline
  sources are fetched concurrently; the empty states wrap their texts in a
  `Column`. No Room schema or navigation/architecture change.
- **Status**: Resolved (compile + 88 JVM tests + lint verified; on-device
  timing check of the startup freeze still recommended).

---

### Issue #17 — [RESOLVED] Bottom-nav labels truncated on narrow screens

- **Severity**: Was 🟡 Medium (UX)
- **Description**: `LifeOSBottomBar` laid icon and label out side by side in a
  `weight(1f)` cell; on ~360dp-wide displays "Insights" rendered as "Insi",
  "Home" as "Ho", making the primary sections ambiguous.
- **Fix**: `BottomNavEntry` is now a stacked column (icon above label, centered
  pill, 22dp icon) with ellipsizing `labelMedium` text; labels stay fully
  readable on standard 360–420dp phones.
- **Status**: Resolved (compile/test/lint verified; on-device visual check at
  320–360dp recommended).

---

### Issue #18 — [RESOLVED] Predictive-back contract not declared

- **Severity**: Was 🟡 Medium (targetSdk 35 correctness)
- **Description**: The app declared `targetSdk 35` with Compose Navigation
  2.8.4 but never opted the activity into the new `OnBackInvokedDispatcher`
  path, so predictive-back gesture animations did not participate and Back
  could read as broken during the startup main-thread stall (Issue #16).
- **Fix**: `android:enableOnBackInvokedCallback="true"` added to the
  application. Every screen's Back path was audited: none swallow presses; the
  nested `root_tabs` graph plus stacked secondary routes already pop correctly.
- **Status**: Resolved (code-verified; on-device predictive-back gesture check
  is part of the final release checklist).

---

### Issue #19 — [RESOLVED] Recovery answer could be brute-forced without throttling

- **Severity**: Was 🟠 High (security)
- **Description**: The App-Lock *recovery answer* (the PIN's bypass) was verified
  with no rate limiting or lockout, unlike the PIN path — an attacker with the
  device could replay guesses indefinitely.
- **Fix**: Extracted `core/util/LockoutPolicy.kt` (pure, unit-tested escalating
  window capped at 16 min) and reused it for the recovery answer
  (`SettingsStore.attemptRecoveryAnswer`) with a stricter 3-attempt budget.
  `AppLockScreen` renders remaining attempts and the lockout countdown. Recovery
  counters reset on `enablePinLock`/`disableAppLock`.
- **Status**: Resolved (covered by `LockoutPolicyTest`).

### Issue #20 — [RESOLVED] Backup imports were unversioned and could restore garbage

- **Severity**: Was 🟠 High (data integrity)
- **Description**: `LifeOSBackup` had no format version; restoring a file written
  by a future/incompatible version would silently write wrong data, and there
  was no size sanity guard before reading the whole file into memory.
- **Fix**: `formatVersion` field (default = current, so legacy exports still
  import), `validateBackup()` rejecting invalid/newer files, a 100 MB cap, and
  user-facing errors from `importFromFile` instead of silent garbage.
- **Status**: Resolved (covered by `BackupSerializationTest`).

### Issue #21 — [RESOLVED] Insights weekly range treated midnight as UTC

- **Severity**: Was 🟡 Medium (wrong numbers off-UTC / on DST days)
- **Description**: `InsightsViewModel.loadStats()` computed
  `start * 86_400_000L` / `(end + 1) * 86_400_000L`, which is UTC-midnight math —
  wrong for every non-UTC zone and for DST days, exactly what
  `DateTimeUtils.startOfLocalDayMillis`/`endOfLocalDayMillis` exist to avoid.
- **Fix**: Added `DateTimeUtils.dayRangeMillis(startDay, endDay)` and
  `minutesOfDay(epochMillis)`; routed Insights, `BuildTimelineUseCase`,
  `TaskRepository` and `LifeModels` through them. No raw `*86_400_000` or
  `hour*60+minute` date math remains in main sources.
- **Status**: Resolved (covered by `DateTimeUtilsTest`).

### Issue #22 — [RESOLVED] Timeline/Home read full tables every build

- **Severity**: Was 🟡 Medium (perf, grows with the database)
- **Description**: `BuildTimelineUseCase` fetched complete `notes` and `tasks`
  tables then filtered in memory by day range — the largest wasted I/O on every
  timeline/Home build.
- **Fix**: `NoteRepository.getCreatedBetween` (existing DAO query) and new
  `TaskDao.getCompletedBetween` (`isDeleted=0, isCompleted=1,
  COALESCE(completedAtEpochMillis, updatedAt)`, matching the old in-memory
  filter) with `TaskRepository` delegation. Habit reads stay small-table
  `observeAll().first()`.
- **Status**: Resolved.

### Issue #23 — [RESOLVED] Hot day/range queries had no index support

- **Severity**: Was 🟡 Medium (perf)
- **Description**: No entity declared `@Index`; every day/range/filter query
  (`dateEpochDay`, `createdAt`, `updatedAt`, `isCompleted`, `isDeleted`,
  `isArchived`, `completedAtEpochMillis`) was a full scan.
- **Fix**: `AppDatabase` v2 adds 14 single-column indexes via `MIGRATION_1_2`
  (Room default names; exports `2.json`, `1.json` untouched). No destructive
  fallback. See `docs/05_DATABASE.md`.
- **Status**: Resolved (schema-generated `2.json` glance-diffed; migration SQL
  matches Room's names 1:1).

### Issue #24 — [RESOLVED] Otherwise-reachable screens lacked entries (AI/Notes search/back nav)

- **Severity**: Was 🟡 Medium (UX)
- **Description**: The AI assistant, quick-action Notes, diary *entry* search
  navigation and an AI back affordance were all missing surface areas; the AI
  screen's `TopAppBar` had no Back button.
- **Fix**: Home header sparkle opens LIFE; a Notes quick-action tile was added;
  pure `SearchCategory.routeFor(hitId)` routes Diary hits to the matching entry
  (`DiaryDetail`); `AiAssistantScreen` gained an AutoMirrored Back button wired
  to `popBackStack`.
- **Status**: Resolved (covered by `SearchCategoryTest` / `LifeDestinationRoutesTest`).

### Issue #25 — [RESOLVED] Dead code and duplicate navigation/schedule builders

- **Severity**: Was 🟢 Low (maintainability)
- **Description**: Dead `SettingsStore.verifyPin` and `Screen.bottomNavItems`;
  two identical `LifeDestination`→route `when` blocks in the NavHost; two
  identical private `scheduleOf(habit)` builders.
- **Fix**: Removed the dead code (grep-verified zero references; keep
  `TaskEntity.sourceType/sourceId` for AI provenance); extracted pure
  `LifeDestination.route()` and `HabitEntity.toSchedule()` (both unit-tested)
  and collapsed the call sites.
- **Status**: Resolved (covered by `LifeDestinationRoutesTest` /
  `HabitStatsCalculatorTest`).

---

## 2026-09-22 hardening-pass status update

A security/perf/navigation hardening pass was executed and verified on-device
available tooling (Gradle 8.9 + AGP 8.6.1, system Gradle since the repo still
ships no `gradlew`). Exact commands and results:

```text
gradle :app:testDebugUnitTest  -> BUILD SUCCESSFUL (123 tests, 0 failures)
gradle :app:compileDebugKotlin -> BUILD SUCCESSFUL
gradle :app:assembleDebug      -> BUILD SUCCESSFUL
gradle :app:lintDebug          -> BUILD SUCCESSFUL (0 errors, 26 pre-existing warnings)
```

The 26 lint warnings are pre-existing and unrelated (GradleDependency version
bumps, autoboxing state hints, obsolete `-v26` folder, unused round icon and
`tagline` string, missing monochrome icon tag, one ModifierParameter hint).
Issues #1 (missing Gradle wrapper scripts) remains open; device/instrumentation
verification remains the standing gap (`app/src/androidTest/` does not exist).
All other fixes above are committed on the `feat/hardening-pass` branch.
