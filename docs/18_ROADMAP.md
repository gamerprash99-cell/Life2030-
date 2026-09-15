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
- **Technical work**: Generate a keystore, add a `signingConfigs` block to `app/build.gradle.kts`
- **Dependencies**: None
- **Complexity**: Small
- **Risks**: Keystore must be backed up securely — losing it means losing the ability to update a published app under the same identity
- **Status**: Not started

### Encrypt the database and the stored AI API key
- **Goal**: Close the two 🟠 HIGH findings in `docs/08_SECURITY.md`
- **User benefit**: Personal diary/notes data and the user's AI key are protected even if the device is compromised
- **Technical work**: Adopt SQLCipher for Room; use Jetpack Security's `EncryptedFile`/encrypted preferences for the API key
- **Dependencies**: None
- **Complexity**: Medium
- **Risks**: SQLCipher migration on an existing (unencrypted) install needs careful handling to avoid data loss
- **Status**: Not started

---

## NEXT (high-value, moderate effort)

### Wire up backup restore in the UI
- **Goal**: Close Issue #3 in `docs/16_KNOWN_ISSUES.md`
- **User benefit**: Users can actually recover their data or move it to a new device
- **Technical work**: Add a file picker + "Restore backup" button in `SettingsScreen.kt` calling the already-implemented `BackupRepository.importFromFile()`
- **Dependencies**: None — the backend logic already exists
- **Complexity**: Small
- **Risks**: Need a clear UX for merge-vs-overwrite behavior (current `restore()` upserts by primary key, so it merges)
- **Status**: Not started

### Build recurring task auto-rollover
- **Goal**: Make `TaskEntity.repeatRule` actually do something
- **User benefit**: Recurring commitments don't need to be manually re-created
- **Technical work**: A WorkManager job (or logic on task completion) that reads `repeatRule`/`repeatDaysCsv` and creates the next `TaskEntity`
- **Dependencies**: Existing `ReminderScheduler`/WorkManager infrastructure can likely be extended
- **Complexity**: Medium
- **Risks**: Needs careful date-math to avoid duplicate or skipped occurrences
- **Status**: Not started

### Expand the Add Task dialog to expose priority/category/description/repeat
- **Goal**: Close Issue #5
- **User benefit**: Users can actually use the fields the data model already supports
- **Technical work**: UI-only — extend `TasksScreen.kt`'s dialog
- **Dependencies**: Pairs well with the recurring-task work above
- **Complexity**: Small
- **Risks**: None
- **Status**: Not started

### Add automated tests, starting with `HabitRepository`
- **Goal**: Close Issue #9; protect the most complex logic in the app (streak/heatmap math) from regression
- **User benefit**: Indirect — fewer bugs over time
- **Technical work**: See the checklist in `docs/14_TESTING.md`
- **Dependencies**: None — test frameworks are already declared as dependencies
- **Complexity**: Medium (ongoing)
- **Risks**: None
- **Status**: Not started

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

### Set up CI/CD (GitHub Actions)
- **Goal**: Automated build + (eventually) test-on-PR
- **User benefit**: Indirect — faster, safer iteration
- **Technical work**: ✅ **Done for the build step** —
  `.github/workflows/android-build.yml` runs `gradle assembleDebug` on every
  push/PR to `main` (works around the missing `gradlew` wrapper — see
  `docs/16_KNOWN_ISSUES.md` Issue #1). **Remaining**: add a test step once
  automated tests exist, add a release-build step once signing is configured
- **Dependencies**: Test step depends on the "Add automated tests" item above; release step depends on the "Add release signing configuration" item in NOW
- **Complexity**: Small
- **Risks**: None
- **Status**: Partially implemented (build automation done; test/release automation not started)

### Clean up orphaned habit completions on delete
- **Goal**: Close Issue #6
- **Technical work**: Either add `@ForeignKey(onDelete = CASCADE)` (requires a Room migration since it changes schema) or a manual cleanup query in `HabitRepository.delete()`
- **Complexity**: Small–Medium (migration required if using the FK approach)
- **Status**: Not started

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
