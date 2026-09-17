# 18 — Roadmap

⚠️ **NOT VERIFIED FROM CODEBASE as a formal product roadmap** — no roadmap
document exists in the repo. Everything below is derived from: (a) code
comments that explicitly defer work ("Phase 6 hardening item", "left as an
extension point"), and (b) the gaps documented in `docs/16_KNOWN_ISSUES.md`.
Items are sequenced by how blocking they are to shipping anything real.

---

## NOW (blocking a first real release)

### Fix the Gradle wrapper
- **Goal**: Make the project buildable from a clean clone without Android Studio's auto-heal
- **User benefit**: None directly — pure developer-experience/CI enablement
- **Technical work**: Run `gradle wrapper --gradle-version 8.9` once and commit the generated files
- **Dependencies**: None
- **Complexity**: Trivial
- **Risks**: None
- **Status**: Not started

### Add release signing configuration
- **Goal**: Produce an installable, distributable release build
- **User benefit**: The app can actually be installed by someone outside development
- **Technical work**: `signingConfigs.release` added to `app/build.gradle.kts`, driven by an optional `keystore.properties`; `assembleRelease` is wired into CI and falls back to debug signing when no keystore is present
- **Dependencies**: None
- **Complexity**: Small
- **Risks**: Keystore must be backed up securely — losing it means losing the ability to update a published app under the same identity
- **Status**: Scaffolding done (see `keystore.properties.example`); a real production keystore still needs to be supplied

### Complete release-environment verification
- **Goal**: Close the 🟠 HIGH findings in `docs/08_SECURITY.md`
- **User benefit**: Personal diary/notes data is protected even if the device is compromised
- **Technical work**: SQLCipher adopted for Room; passphrase generated per install and wrapped with an Android Keystore AES-GCM key (`DatabasePassphraseProvider`). There is no stored AI API key — the Intelligence engine is fully on-device
- **Dependencies**: None
- **Complexity**: Medium
- **Risks**: SQLCipher migration on an existing (unencrypted) install needs careful handling to avoid data loss
- **Status**: Done for new installs. Existing unencrypted databases need an explicit migration path before wide rollout.

---

## NEXT (high-value, moderate effort)

### Wire up backup restore in the UI
- **Goal**: Close Issue #3 in `docs/16_KNOWN_ISSUES.md`
- **User benefit**: Users can actually recover their data or move it to a new device
- **Technical work**: File picker + restore entry already present in Onboarding and Settings; `BackupRepository.restore()` now runs inside a single `withTransaction { }`
- **Dependencies**: None
- **Complexity**: Small
- **Risks**: Restore upserts by primary key (merge semantics); needs device testing with representative backups
- **Status**: Implemented + transactional; device verification pending

### Build recurring task auto-rollover
- **Goal**: Make `TaskEntity.repeatRule` actually do something
- **User benefit**: Recurring commitments don't need to be manually re-created
- **Technical work**: `RepeatRuleCalculator` (pure date math) + `TaskRepository.setCompleted()` spawns the next occurrence; overdue tasks roll forward from today
- **Dependencies**: Existing task repository/reminders
- **Complexity**: Medium
- **Risks**: Needs careful date-math to avoid duplicate or skipped occurrences
- **Status**: Done, covered by `RepeatRuleCalculatorTest`

### Expand the Add Task dialog to expose priority/category/description/repeat
- **Goal**: Close Issue #5
- **User benefit**: Users can actually use the fields the data model already supports
- **Technical work**: `TasksScreen.kt` dialog extended with description, category, priority and repeat dropdowns
- **Dependencies**: Pairs well with the recurring-task work above
- **Complexity**: Small
- **Risks**: None
- **Status**: Done

### Add automated tests, starting with `HabitRepository`
- **Goal**: Close Issue #9; protect the most complex logic in the app (streak/heatmap math) from regression
- **User benefit**: Indirect — fewer bugs over time
- **Technical work**: JVM unit tests added; streak math extracted to pure `HabitStatsCalculator`; CI runs `gradle test`
- **Dependencies**: None
- **Complexity**: Medium (ongoing)
- **Risks**: None
- **Status**: Done for unit tests; instrumentation coverage still to add

---

## LATER (real value, larger scope)

### Feed real app data into the AI Assistant chat
- **Goal**: Close Issue #7 — make the chat actually useful
- **User benefit**: "What did I spend on food this week?" gets a real, grounded answer
- **Technical work**: Build a context-assembly step (similar to `InsightsScreen.kt`'s stats string) and pass it as `AiRepository.chat()`'s `contextBlock`
- **Dependencies**: None — the parameter already exists
- **Complexity**: Medium (needs thought on what's safe/useful to include per query)
- **Risks**: Sending too much context increases cost and privacy surface per the "send only what's needed" principle already established elsewhere in the app
- **Status**: Not started

### CI/CD hardening and verification
- **Goal**: Automated build + (eventually) test-on-PR
- **User benefit**: Indirect — faster, safer iteration
- **Technical work**: `.github/workflows/android-build.yml` runs `gradle test assembleDebug assembleRelease` on every push/PR to `main` (works around the missing `gradlew` wrapper — see `docs/16_KNOWN_ISSUES.md` Issue #1). Test and release steps are now included; release falls back to debug signing without a keystore
- **Dependencies**: None
- **Complexity**: Small
- **Risks**: None
- **Status**: Implemented (build + test + release automation done)

### Expand automated/device coverage
- **Goal**: Close Issue #6
- **Technical work**: `HabitCompletionDao.deleteForHabit()` called from `HabitRepository.delete()` (manual cleanup query, no schema migration needed)
- **Complexity**: Small
- **Status**: Done

---

## FUTURE (large, not scoped in detail)

These are directions implied by the product concept but with no code
groundwork laid yet:

- **Semantic/AI-powered search** — would require an embeddings pipeline and
  likely a local vector index; current search is plain SQL `LIKE`
- **Note-to-note linking** (wiki-style references)
- **Calendar sync** (Google Calendar or device calendar integration)
- **Home-screen widgets**
- **Cloud sync / multi-device** — would require designing and building an
  actual backend for the first time; today's local-first architecture was
  explicitly chosen to avoid this (see `docs/24_ARCHITECTURAL_DECISIONS.md`,
  ADR-001), so this would be a significant architectural shift, not an add-on
- **User accounts** — a prerequisite for cloud sync; see `docs/07_AUTHENTICATION.md`

Each of these is a substantial project in its own right and should get its
own ADR (`docs/24_ARCHITECTURAL_DECISIONS.md`) and feature spec before work starts.

## Current-state addendum — 2026-09-16

This document remains part of the LifeOS documentation set. Current UI/UX, motion, responsive and accessibility rules are centralized in [`DESIGN.md`](./DESIGN.md). The current Intelligence implementation is local/offline and requires no external AI provider or API key. Build/test statements are only considered verified when the exact command has been executed in a real Android/Gradle environment.

## Current implementation snapshot — 2026-09-16

The next UI validation step is real-device comparison against the supplied Stitch screens across small/large phones, font scaling, gesture navigation and landscape where applicable.

## LIFE Phase 18 — Consolidation
The LIFE integration is consolidated into the latest supplied LifeOS source. Release verification still requires Android build tooling, APK size measurement, and a real trained LIFE checkpoint before production release.


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
