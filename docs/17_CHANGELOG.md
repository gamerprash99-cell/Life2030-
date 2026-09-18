# 17 — Changelog

## Important note on how this document was produced

⚠️ **NOT VERIFIED FROM GIT HISTORY** — this repository, as delivered, has no
`.git` directory, so there is no commit log to reconstruct a chronological
history from. The entries below are organized by **development session**
(as the code was authored) rather than by git commits, since that's the
only history that actually exists to draw from. Per the instructions
governing this document, no history has been fabricated beyond what can be
directly inferred from the current state and structure of the code.

Once this repository is pushed to GitHub (`docs/12_GITHUB_WORKFLOW.md`),
all *future* entries in this file should be generated from real `git log`
output.

---

## [0.2.0] — Hardening pass

### Security
- PIN/recovery hashing upgraded from a single SHA-256 to PBKDF2-HMAC-SHA256
  (120,000 iterations, random 16-byte salt); legacy v1 hashes still verify
- Brute-force lockout added to `SettingsStore` (5 attempts, escalating to 16 min)
- Room database encrypted at rest with SQLCipher; passphrase wrapped by an
  Android Keystore AES-GCM key (`DatabasePassphraseProvider`)
- Removed dead biometric code (`AppLockManager`, `androidx.biometric`,
  `USE_BIOMETRIC` permission); App Lock is PIN-only

### Added
- Recurring tasks now roll over on completion (`RepeatRuleCalculator`)
- Full Add Task dialog: description, category, priority and repeat rule
- JVM unit test suite (PIN hashing, habit stats, repeat rules, date math,
  offline intelligence, backup serialization)
- Optional release signing via `keystore.properties` (`keystore.properties.example`)

### Fixed
- Backup restore now runs in a single database transaction
- Deleting a habit removes its `habit_completions` rows
- Version bumped to `0.2.0` (`versionCode` 2)

### Changed
- CI now runs `gradle test assembleDebug assembleRelease`

---

## [0.1.0-phase1-6] — Current state (`app/build.gradle.kts` `versionName`)

### Added — Core data & architecture
- Room database with 7 tables: `notes`, `tasks`, `habits`, `habit_completions`,
  `expenses`, `diary_entries`, `captures` (`data/db/`)
- Repository layer for each feature (`data/repository/`)
- Manual dependency-injection container, `ServiceLocator` (`core/di/`)
- Glassmorphism design system (`ui/theme/`, `ui/components/GlassCard.kt`)

### Added — Features
- Notes with rich-text blocks (paragraph, heading, bullet, numbered, checklist)
- Tasks with priority, due dates, overdue tracking, "keep for tomorrow"
- Habits with real streak calculation and a 12-week GitHub-style heatmap
- Expenses with categories and monthly totals
- Diary with mood tagging
- Unified Timeline aggregating all of the above by date/time (computed live, not stored)
- Global search across Notes/Tasks/Expenses/Diary (plain SQL `LIKE`)
- Home dashboard combining live task/habit/expense data

### Added — AI layer
- Real Anthropic API integration (`core/ai/AiClient.kt`)
- Note AI actions (summarize, rewrite, extract tasks, etc.)
- AI diary drafting with mandatory human-review flow
- AI weekly review summaries
- AI Assistant chat screen

### Added — Capture
- Photo capture via CameraX (`ui/capture/CameraCaptureScreen.kt`)
- Video capture via CameraX `VideoCapture`/`Recorder` (`ui/capture/VideoCaptureScreen.kt`)
- Audio capture via `MediaRecorder` (`ui/capture/AudioCaptureScreen.kt`)
- Text "thought" capture

### Added — Security / access control
- App Lock via biometric/PIN (`core/security/AppLockManager.kt`)
- `android:allowBackup="false"` + backup/data-extraction exclusion rules

### Added — Reminders
- Per-item WorkManager reminder scheduling (`core/reminders/`)
- Notification permission requested only when the user enables Reminders in Settings

### Added — Backup
- Full JSON export of all tables (`data/repository/BackupRepository.kt`)
- Share exported backup via Android's system share sheet (`FileProvider`)

### Added — Onboarding
- 4-page first-launch introduction (`ui/onboarding/OnboardingScreen.kt`)

### Added — Documentation
- Full `/docs` knowledge base (this file and its 24 siblings), created in
  this same working session, grounded in a direct audit of the code above

### Known limitations at this version
See `docs/16_KNOWN_ISSUES.md` for the complete, current list. Headlines:
missing Gradle wrapper scripts, no release signing config, backup restore
has no UI, task recurrence is inert, no automated tests, database/API key
not encrypted at rest.

### Breaking changes
Not applicable — this is the first tracked version.

### Security
See `docs/08_SECURITY.md` for the full classified findings list from this version.

---

## Future entries

Every subsequent version should follow this format:

```
## [x.y.z] — YYYY-MM-DD

### Added
### Changed
### Fixed
### Removed
### Security
### Breaking Changes
```

...and should be generated from real `git log`/PR history, not reconstructed
from reading code after the fact.

## 2026-09-16 — Figma-led UI/UX polish pass

- Created a new Figma design reference file for the LifeOS 2026 visual system: premium lavender/violet surfaces, rounded cards, grouped settings, onboarding, app-lock UX, and motion timings.
- Added shared Compose UI primitives for LifeOS cards, gradient CTAs, badges, section headers, Intelligence cards, and animated press feedback.
- Refined LifeOS theme colors, typography, spacing and shape tokens around the existing purple/lavender direction while preserving dark mode.
- Refined Home with stronger hierarchy, animated task progress, responsive sections and local Intelligence presentation using existing ViewModel data.
- Redesigned onboarding presentation with animated page transitions and Android JSON backup restore entry point.
- Added JSON restore through Android Storage Access Framework to onboarding and Settings, routed through the existing BackupRepository.
- Refined Settings into grouped sections and retained existing App Lock, reminder, Intelligence and backup behavior.
- Refined the existing Navigation Compose bottom bar with animated selected-state pills while keeping the existing NavController/navigation architecture.
- Refined App Lock setup option cards and CTA styling without changing the existing biometric/PIN security model.
- No Room schema changes, cloud services, external AI APIs, analytics, telemetry or INTERNET permission were introduced.

Verification: archive integrity checked after the source update. Full Android Gradle build/test execution remains dependent on a Gradle/Android SDK environment being available.

## 2026-09-16 — Documentation and Figma design-system correction

- Added `docs/DESIGN.md` as the current visual/UI/UX and motion specification.
- Updated the root README to describe the current local Intelligence Engine and current UI architecture accurately.
- Corrected stale documentation that still described the removed Anthropic/cloud AI implementation.
- Corrected stale documentation that still described backup restore as UI-unreachable after the current restore entry points were added.
- Kept historical changelog entries intact rather than rewriting project history.
- Documented the limitation that Android build/test verification is still unavailable in the supplied environment because the project snapshot has no Gradle wrapper and no usable local Android Gradle environment.

## Current implementation snapshot — 2026-09-16

2026-09-16: Integrated the Stitch LifeOS visual direction into native Compose screens, redesigned Life Capture as a full-screen studio, refreshed primary navigation, and added predictable Back handling.


## 2026-09-16 — Stitch Home dashboard and PIN-only App Lock

### Changed
- Reworked the native Compose Home screen to match the supplied Stitch Today dashboard direction while preserving existing Home ViewModel/repository data and actions.
- Added top-right Search, Settings and Profile actions. Settings was removed from the bottom navigation; Profile is a local navigation destination.
- Preserved the existing bottom navigation architecture and feature destinations.

### Fixed
- Hardened system Back handling at the NavController level so secondary routes pop through the same stack used by visible Back actions.

### Security
- Removed the biometric App Lock option and biometric runtime implementation. App Lock is now `NONE` or `PIN` only.
- Removed the AndroidX Biometric dependency and unused `AppLockManager`.
- Legacy stored `BIOMETRIC` values resolve to `NONE` because there is no safe PIN secret to infer from an old biometric-only configuration.

### Verification
- Source archive was inspected and modified locally. Full Android Gradle compilation/tests could not be executed because the supplied snapshot has no Gradle wrapper and this environment has no installed Gradle/Android SDK.

## 2026-09-17 — LIFE Phase 18 Consolidation
- Consolidated LIFE controller, bounded session/context model, local voice bridge, and allow-listed navigation into the supplied latest LifeOS source tree.
- Preserved existing Room/repository/navigation architecture.
- Added final release checklist and Phase 18 documentation.
- Android build verification remains pending because the supplied project has no Gradle wrapper and the verification environment has no system Gradle.

## 2026-09-18
### UI / interaction
- Added local profile-photo selection and persistence.
- Removed the Home Settings action in favor of Profile → Settings.
- Updated Profile, Expenses, Add Expense, and App Lock Compose UI from the supplied Stitch direction.
- Added navigation Back handling and fixed onboarding completion startup gating.
- Hardened audio recording and playback lifecycle.

## [0.2.x] — 2026-09-19 — Expenses micro UX fix

### Changed
- The existing "Add expense" `ModalBottomSheet` (`ui/expenses/ExpensesScreen.kt`)
  now opens expanded by default via
  `rememberModalBottomSheetState(skipPartiallyExpanded = true)`, and its body is
  scrollable so all fields remain reachable under IME/landscape/font scaling.
  Sheet design, fields, buttons, drag handle, swipe-to-dismiss and Back behavior
  are unchanged.
- The selected expense category chip now renders in the theme's
  `colorScheme.primary`/`onPrimary`; `GlassChip` gained an optional `selected`
  parameter and the Expenses call site reuses the existing single selection
  state. Exactly one category is highlighted at a time.

### Unchanged
- No Room schema/entity/DAO/migration change; no repository, navigation,
  architecture, calculation or category-definition change.
- No network/cloud/AI/telemetry dependency introduced; data stays on device.

### Verification
- `gradle :app:compileDebugKotlin` — BUILD SUCCESSFUL
- `gradle :app:assembleDebug` — BUILD SUCCESSFUL
- `gradle :app:testDebugUnitTest` — BUILD SUCCESSFUL (81 tests, 0 failures)
- `gradle :app:lintDebug` — BUILD SUCCESSFUL (0 errors, 5 pre-existing warnings)
