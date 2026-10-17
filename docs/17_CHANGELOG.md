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

## [Unreleased] — Lavender Dream redesign + Automatic Expense Capture & Insights removal (2026-10-16)

Branch `feat/lavender-dream-redesign`. A full light+dark retheme to the
"Lavender Dream" direction, a bottom-navigation correction, two feature
removals and documentation reconciliation. The data layer, Room schema,
migrations, ViewModels and user data are unchanged; no permission or network
path was added.

### Added
- **`Screen.Backup` route** — opens Settings directly at the Backup & Local
  Storage section (scroll-to-section via `boundsInRoot` anchors); wired from
  Profile's "Local Backup" row.
- **New color tokens** — `LifeOSInkOnBrand` (AA ink on primary fills),
  `LifeOSPrimaryTextLight` / `LifeOSPrimaryTextDark`.

### Changed
- **Theme** (`Color.kt`, `Theme.kt`) — every existing token name preserved but
  remapped to the lavender palette. Light: bg `#F1EDF7`, surface `#FCFAFF`,
  primary `#A995D1`, soft `#D8C9EF`, secondary `#8B78B7`, text `#3F3457`,
  secondary text `#716781`, border `#E4DDF0`. Dark: bg `#17141F`, surface
  `#24202F`, elevated `#302A3E`, primary `#BBA7E6`, soft `#463956`, secondary
  `#C8B7EB`, text `#F6F1FC`, secondary text `#C8BED8`, border `#494055`.
  Functional state colors (success/danger/warning/info) keep their semantics;
  mint is no longer the product identity anywhere.
- **Bottom bar** (`LifeOSBottomBar.kt`) — now Home · Notes · Capture (centre
  action) · Life. ~24dp icons, 44×32 selected pill, ≥48dp touch targets, proper
  `WindowInsets` padding. Capture remains an action, not a tab.
- **Resources** — light/dark splash and window backgrounds, launcher vectors and
  the Today-widget background recolored to lavender.
- **Diary paper palette** (`DiaryColors.kt`) — recolored to lavender-tinted
  paper; token names kept.
- **Headings** — the "You" (Profile) and "Life" (Life) page headings removed;
  `LifeOSPageHeader.title` is now nullable and the contextual eyebrow remains.
- **Appearance settings copy** — now says "Lavender Dream".

### Removed
- **Automatic Expense Capture** — `TransactionCaptureService`,
  `TransactionParser`, `ExpenseCategorizer`, `CaptureTransactionUseCase`,
  `NotificationAccess`, `ExpenseOrigin`, `Transaction`, the notification-listener
  manifest service and `<queries>` block, the Settings toggle/card, the
  "Automatically detected" expense badge, the `SettingsStore` key/flow, the
  dedup DAO queries (`existsById`, `findSameAmountOnDay`) and the `id`
  parameter of `addExpense`. Manual expense entry untouched. No migration needed
  (the capture marker was a `tagsCsv` tag).
- **Insights** — `InsightsScreen`, `InsightsViewModel`, `Screen.Insights` and
  its NavHost entry. The shared intelligence engine was **deliberately kept**:
  Diary insights/weekly review and the on-device AI assistant consume it.
- Per-feature unit tests for both removed features; `BottomNavItemsTest` updated
  for the new tab order and route uniqueness.

## [Unreleased — earlier pass] — Figma fidelity rebuild

Branch `redesign/from-zero-2026`. Every screen was rebuilt from zero against the
supplied Figma file (`ehSpxDfVot6SgPIdd2Tvf4`). The result is a new theme, a new
component kit and one canvas/page-header layout shared by the whole app. The
data layer, ViewModels, navigation routes and the Diary's paper module are
unchanged, so no feature lost behaviour: the work is presentation only.

### Added
- **New component kit** (`ui/components/LifeOSCore.kt`) — `LifeOSCard`,
  `LifeOSPageHeader`, `LifeOSEyebrow`, `LifeOSSectionHeader`, `LifeOSPrimaryButton`,
  `LifeOSProgressTrack`, `LifeOSEmptyState`, `LifeOSFilterPill`, `LifeOSLinkText`,
  `LifeOSSearchField`, `LifeOSHeaderButton`, plus `lifeOSCanvas()` /
  `lifeOSCanvasBrush()` / `lifeOSCardShape()` / `lifeOSIsDark()`.
- **The canvas** — the Figma frame's background gradient (a single brush that
  runs from the lighter top of the frame to the near-black bottom, with a
  dark-mode variant), so every page sits in the same light instead of flattening
  to one hue.
- **`DiaryColors.kt`** — the Diary module's paper palette, split out of `Color.kt`
  so the app palette and the paper palette stay legible as separate things.

### Changed
- **Every screen rebuilt to the Figma recipe**: an 86dp page header (9sp mint
  uppercase eyebrow + 24sp SemiBold title + 42×42 r15 action), 24dp content
  padding with an 18dp gap, the `#162630` r16 search well, an r22 translucent
  card (18dp padding, 12dp inner gap), a mint section-action label and an r22
  selected pill. Screens rebuilt: Home, Notes, Note editor, Tasks, Habits,
  Habit detail, Expenses, Insights, Life, Capture hub, Capture media preview,
  Global search, AI assistant, Profile, Settings, App-Lock setup, Onboarding,
  Snap, Snap viewer and the start screen.
- **Theme** (`Color.kt`, `Theme.kt`, `Type.kt`) — the app's tokens, schemes and
  type scale now come from the Figma file; the type is single-voice **Inter**
  (one variable font) instead of three families across eight files.
- **One status-bar rule** — destinations no longer apply their own
  `statusBarsPadding()` / `navigationBarsPadding()`; the `LifeOSNavHost`
  `Scaffold` owns the insets for every destination.
- The shared radius default for `LifeOSSurface` / `GlassCard` / `LiquidGlass` is
  the card radius (22dp), so no screen has to pass its own shape.

### Fixed
- `SettingsScreen` failed to compile: a missing `fillMaxSize` import and a
  stray top-level brace left by the rebuild.

### Removed
- Dead components: `LifeOSTopBar.kt`, `LifeOSDesignComponents.kt`, and the
  superseded `LifeOSScreenHeader` / `LifeOSActionTile` / `LifeOSHeroCard` /
  `Legacy*` / `LiquidGlassFab` definitions they left behind.
- Superseded screens: `ui/notes/NotesListScreen.kt` and `ui/search/SearchScreen.kt`
  (both unreachable — `NotesScreen` and `GlobalSearchScreen` do their jobs).
- The bundled Fraunces / Manrope / Kalam font files, replaced by the single
  Inter variable font the design uses.

### Kept deliberately
- **The Diary and Timeline keep their paper aesthetic.** The design's Diary
  frame is a page, not a card, so `GlassCard`, the paper palette and the diary's
  insets stay as they are — this pass is a rebuild of the *app* chrome, not the
  notebook inside it.

---

## [Unreleased] — Redesign pass: capture audio, identity, motion

Branch `redesign/from-zero-2026`.

### Fixed
- **Voice capture was dead.** The Capture hub's Audio tile was a stub that only
  printed "Voice notes are coming next"; the one working recorder
  (`AudioCaptureScreen`) lived behind an unreachable `CaptureSheet`. The tile now
  opens the real recorder, which records through the shared `AudioRecorder`
  interface (live timer + real amplitude), saves via
  `CaptureViewModel.saveAudio`, and reports honest failures
  (`START_FAILED` / `TOO_SHORT` / `STOP_FAILED`). The dead `CaptureSheet.kt` is
  removed.

### Changed
- **Launcher mark recoloured to AURA.** The adaptive icon dropped the generic
  indigo `#6C63FF` for the app's own lagoon tile with a mint→coral ring and a
  coral "captured moment" bead. The shape is unchanged — only the colour moved
  onto the real palette. Added an Android 13+ monochrome layer for themed icons.
- **Insights restored to the bottom bar.** IA is now
  `Home · Notes · ＋Capture · Insights · Life` as the spec requires (the old
  "You" slot becomes Insights); `Screen.Insights` and its NavHost route are
  wired, and `BottomNavItemsTest` asserts the new order.

### Added
- **Animated start screen.** After Android's native splash, `LifeOSStartScreen`
  traces the ring in, springs the bead into its opening, then lifts the wordmark
  and promise over a progress shimmer (~1.6s) before crossfading into the app.
  It is theme-aware (bright mint→coral on the dark lagoon, deeper brand steps in
  the light polarity) and honours the system reduced-motion setting.
- **Micro-motion.** The centre Capture action now carries a slow breathing halo;
  the bottom-bar selection pill fades in with a small icon lift; the capture
  confirmation draws a real animated tick; and the Home AI orb breathes gently.

### Fixed (build)
- Widened `CaptureViewModel`'s dependency from `DeviceLocationProvider` to the
  `LocationProvider` interface so it can be tested on the JVM.

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
