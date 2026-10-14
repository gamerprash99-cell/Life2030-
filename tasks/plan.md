# Implementation Plan — Remove Notes, Capture, LIFE AI, Intelligence, Insights, Search, Weekly Rhythm & AI Settings

**Repo:** https://github.com/gamerprash99-cell/Life2030-.git — clone at `/home/Life2030`
**Branch:** `feat/remove-notes-capture-life-intelligence` (base `f36cfe2`; up to date with `origin/main`)
**Objective:** Delete the Notes, Capture (Photo/Video/Audio), LIFE/AI Assistant, shared offline Intelligence (`core/intelligence`, `core/ai`, `core/life`), Insights/Statistics/Search, and "Weekly Rhythm" screens plus AI settings from LifeOS, refactor every retained feature off the deleted systems, ship a Room DB migration v2→v3 that drops **only** the `notes` and `captures` tables, and fully verify before committing.

## Hard constraints (agreed with user)
- No destructive git: no `reset --hard`, `clean -fd`, force-push, or branch deletion.
- **No** `fallbackToDestructiveMigration`. The migration must be explicit and must not drop unrelated retained tables (tasks, habits, habit_completions, expenses, diary_entries) nor their rows.
- No `@Suppress` / `// noinspection` / disabled or skipped tests to force green. Report only verified results.
- Never write the GitHub PAT to disk. Push **only after** the user provides it following the exact request sentence (Phase 6).

## Scoped-in deletions
- **Features/screens:** Notes, Note Editor, Capture Detail, Capture sheet in Home (FAB), Photo/Video/Audio capture, Diary Insights & Statistics, Diary Connections, Search, AI Assistant, Insights bottom-nav tab, Weekly Rhythm card, AI Features toggle + "Privacy & Local Intelligence" settings section.
- **Code:** `core/ai`, `core/intelligence`, `core/life` (incl. `LifeModels`, `LifeVoiceInputController`), `ui/ai`, `ui/capture`, `ui/notes`, `ui/insights`, `ui/search`, `LifeDestinationRoutes.kt`, `NoteBlock.kt`, `NoteEntity`, `CaptureEntity`, `NoteDao`, `CaptureDao`, `NoteRepository`, `CaptureRepository`, `DiaryAnalyticsSection`, `DiaryConnectionsView`.
- **Tests:** `NoteTextAnalyzerTest`, `MoodAnalyzerTest`, `DiaryConnectionsTest`, `SearchCategoryTest`, `NarrativeScoreTest`, `LifeDestinationRoutesTest`.
- **Permissions/resources:** CAMERA, RECORD_AUDIO, camera `uses-feature`, captures `file_paths` entry; CameraX gradle block (exifinterface kept — still used by… verify or remove).

## Scoped-in refactors (retained code must not reference deleted systems)
- `core/di/ServiceLocator` — drop note/capture/intelligence/ai/life deps; rewire Backup + BuildTimeline.
- Domain/usecase — `BuildTimelineUseCase`, `TimelineItem` (enum: TASK_COMPLETED, HABIT_COMPLETED, EXPENSE, DIARY).
- `ui/timeline/TimelineScreen` (no capture thumbnails/clicks), `ui/home/HomeScreen` (FAB header buttons, Notes tile, QuickActions, copy, `iconFor`), `ui/diary` (ViewModel, Screen, Detail, Moods — remove intelligence/analytics/connections)/keywords/MoodAnalyzer fallback; tag chips from `entry.tagsCsv`), `ui/habits/HabitsScreen` (Weekly Rhythm, `completionPercent`, `todayDone`), `core/util/SettingsStore` + `ui/settings/SettingsScreen` (AI toggle + section), `ui/onboarding/OnboardingScreen` (3 pages: Bolt/Lock/Timeline), `ui/navigation` (Screen, NavHost, BottomBar).
- Repos/DAOs: `TaskRepository`/`TaskDao` (remove `search`, `createFromAiExtraction`, `countCompletedBetween`, `getCompletedBetween`, `getCreatedBetween`), `ExpenseRepository`/`ExpenseDao` (remove `search`, `getInRange`, `getCategoryTotals`, `CategoryTotal`), `DiaryRepository`/`DiaryDao` (remove `search`, `getInRange`, `countInRange`, `daysSinceLastEntry`, ai-draft + review methods, `getLastEntryEpochDay`).
- `core/util/PermissionManager` (drop `LifeOSPermissions` import dead code), `core/util/MediaStorage` (keep profile-photo path helpers, drop captures), `data/db/Converters` (drop CaptureType converters).
- Resources/gradle: `AndroidManifest.xml`, `res/xml/file_paths.xml`, `res/values/strings.xml`, `app/build.gradle.kts`.

## Scoped-out (keep unchanged)
- Home Summary & DailyUpdatePercent (no AI dependency), App Lock / PIN / passphrase (`core/security`), SQLCipher encryption, Biometric unlock (keep `biometric` / `biometric` dep), Backup/restore core, `versionCode`/`versionName` (unchanged: 2 / 0.2.0), captured intent for future reference.
- **Separate Notes/Capture exports are NOT in scope** — their local data is dropped only via the Room migration.

## Verification
Deterministic bare-metal commands (no jq/pip/npm/conda/Gradle auto-download, unless the terminal's tooling provides them; else manual review). Ensure no leftover references or orphaned `LifeOS` dependencies; run: `./gradlew :app:assembleDebug`, `:app:testDebugUnitTest`, `:app:lintDebug`, and `:app:assembleDebugAndroidTest` (discover availability) — all green. Provide the resulting APK for review if possible.

## Migration
No destructive fallback, no `DROP` of unrelated tables. v2→v3 drops `notes` + `captures` only (adding `MIGRATION_2_3` and keeping it in `addMigrations`). Requires Room schema export for testing (KSP exportSchema=true; add schema-location tests + instrumentation MigrationTestHelper invocation). Current DB version = 2; bump to 3. Journal v2 schema identity-hash matches `2a800b15343eba2a` family.

## Backup format
Keep `CURRENT_FORMAT_VERSION = 1` and the `LifeOSBackup` shape minus `notes`/`captures` arrays. `ignoreUnknownKeys` must stay so old v1 backups (that include those keys) still import. Add a legacy-decode unit test.

## Docs
Append an UPDATE.md entry covering: scope removed, migration, data preservation, testing notes, known issues. (This signals PAT dependency.) Provide a final diff for review before applying the PR.

## Phase 6 — Finalize
- Append UPDATE.md entry.
- Stage **only** intended files; commit on `feat/remove-notes-capture-life-intelligence`.
- Prepare PR (title + description) against `origin/main`; do not push — stop and ask for PAT.
- **PAT request must be the exact sentence:** «Local implementation, tests, lint, diff review and documentation are complete. The PR is ready to push. Please provide the GitHub Personal Access Token.»
- Push only after the user provides the PAT.

## Phase 7 — Follow-ups (when allowed)
- Verify git-base cleanliness, branches, PR diff, and live app flow (Home→Tasks→Habits→Diary→Expenses→Timeline→App Lock; Backup export/restore round trip).