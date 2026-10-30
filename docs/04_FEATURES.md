# 04 — Features

Full inventory of every implemented feature, with exact file paths.

---

## 1. Notes

**Purpose**: Rich-text note-taking with organization (pin/favorite/archive/trash) and optional AI actions.

- **User flow**: Notes list (`ui/notes/NotesListScreen.kt`) → tap note or "+" → `ui/notes/NoteEditorScreen.kt` → edit title/blocks → back (auto-saves).
- **Files**: `ui/notes/NotesListScreen.kt`, `ui/notes/NoteEditorScreen.kt`, `ui/notes/NotesViewModel.kt` (contains both `NotesListViewModel` and `NoteEditorViewModel`)
- **Domain model**: `domain/model/NoteBlock.kt` — a sealed class (`Paragraph`, `Heading`, `BulletItem`, `NumberedItem`, `ChecklistItem`), serialized to JSON and stored in `NoteEntity.contentJson`
- **Database**: `data/db/entities/NoteEntity.kt`, `data/db/dao/NoteDao.kt`
- **Repository**: `data/repository/NoteRepository.kt`
- **AI integration**: `core/ai/AiModels.kt` (`NoteAiAction` enum: Summarize, Generate title, Organize text, Rewrite, Improve grammar, Make shorter/longer, Extract important points, Create checklist, Generate ideas, Explain content, Create study questions) plus "Extract tasks" — all routed through `AiRepository.runNoteAction()` / `extractTasks()`
- **Auth requirement**: None
- **Error handling**: AI failures surface as an in-dialog message ("AI error: ..."); missing API key surfaces "Add your AI API key in Settings..."
- **Status**: Implemented
- **Known limitations**: No note-to-note linking; search is plain SQL LIKE, not semantic; extracted tasks can only be approved if the note has already been saved once (noteId must be non-null — see `NoteEditorViewModel.approveExtractedTasks`)

## 2. Tasks

**Purpose**: To-do list with priority, due date, reminders, and "keep for tomorrow" rescheduling.

- **User flow**: `ui/tasks/TasksScreen.kt` → "+" → enter title, optionally set a reminder time via `ui/components/ReminderTimePickerDialog.kt` → Add
- **Files**: `ui/tasks/TasksScreen.kt` (contains `TasksViewModel`)
- **Database**: `data/db/entities/TaskEntity.kt`, `data/db/dao/TaskDao.kt`
- **Repository**: `data/repository/TaskRepository.kt`
- **Reminders**: `core/reminders/ReminderScheduler.kt` schedules a WorkManager job when a reminder time is set; cancelled automatically on completion or deletion
- **Status**: Implemented
- **Known limitations**: `RepeatRule` field exists (NONE/DAILY/WEEKLY/MONTHLY/CUSTOM_DAYS) but nothing auto-creates the next recurrence — see `docs/16_KNOWN_ISSUES.md`

## 3. Habits — the permanent tracker

**Purpose**: A complete, private habit tracker that is a permanent root tab
(Home · Notes · ＋ Capture · Habits · Life). It supports Build and Quit
habits, flexible schedules, three tracking modes, real streak math and a
12-week history — all computed from persisted rows.

- **User flow**:
  - First habit — `ui/habits/HabitEditorScreen.kt` guided 10-step wizard:
    intro → name/description → icon → colour → Build/Quit → frequency →
    tracking → reminder → category/Timeline → review/create. The review step
    saves from the wizard's bottom bar.
  - Later adds/edits — the same screen in normal form mode, with per-section
    editing, validation and an unsaved-changes guard.
  - Dashboard — `ui/habits/HabitsScreen.kt`: week date strip (past days
    only), All/Due/Completed filters, live Habit Score (rest days are
    neutral), habit cards with streak, schedule, tracking control, 28-day
    mini heatmap, overflow menu (edit/archive/delete), archived bottom sheet
    with restore, delete confirmation, and a calm celebration + share overlay.
  - Detail — `ui/habits/HabitDetailScreen.kt`: hero + streak, 2×2 stats
    (current/longest streak, this week, this month), day action card
    (tick / count stepper / checklist / Quit "stayed on track" & "I slipped"),
    clickable 12-week heatmap that backfills a past day, schedule/reminder
    card, share, archive and delete.
- **Semantics**: Build habits track doing; Quit habits track avoiding and
  record an explicit slip. An empty day is neither success nor failure and is
  never auto-marked as missed. Unscheduled days never break a streak.
- **Schedules**: every day, selected weekdays, days of month, N× per week,
  N× per month — plus `LEGACY`, which preserves the exact pre-v7
  DAILY/WEEKLY/CUSTOM meaning for existing habits.
- **Tracking modes**: Tick (one tap), Count (daily target with stepper and
  direct entry), Checklist (reusable named items; a day completes only when
  all active items are checked).
- **Files**: `ui/habits/HabitsScreen.kt`, `HabitsViewModel.kt`,
  `HabitDetailScreen.kt`, `HabitEditorScreen.kt`, `HabitVisuals.kt`.
- **Database**: `data/db/entities/HabitEntity.kt` (`HabitEntity`,
  `HabitCompletionEntity`, `HabitChecklistItemEntity`,
  `HabitChecklistCheckEntity`); schema v7 with `MIGRATION_6_7`.
- **Repository**: `data/repository/HabitRepository.kt` — draft validation,
  create/update, template preservation, tick/count/slip/checklist logging,
  analytics, heatmap, backup/restore.
- **Streak engine**: `core/util/HabitStatsCalculator.kt` — schedule-aware
  streaks, per-period targets for N-per-week/month habits, completion
  percentages that cannot exceed 100, and legacy-compatible parsing.
- **Home / Timeline**: Home shows only habits actually due today (a met
  weekly target drops off instead of looking missed) with the same explicit
  completion rule; successful days appear on the Life Timeline only when the
  habit is not opted out, and blank days never do.
- **Privacy**: fully offline, no account, no upload, no telemetry.
- **Status**: Implemented and unit-tested; instrumentation migration tests
  compile but require an emulator/device to execute.

## 4. Expenses

**Purpose**: Personal expense logging with categories and monthly totals.

- **User flow**: `ui/expenses/ExpensesScreen.kt` → "+" → amount, category (from `domain/model/Categories.kt`'s `ExpenseCategories.ALL`), optional merchant → Save
- **Database**: `data/db/entities/ExpenseEntity.kt`, `data/db/dao/ExpenseDao.kt` (includes `getCategoryTotals` for category breakdowns)
- **Repository**: `data/repository/ExpenseRepository.kt`
- **Status**: Implemented
- **Known limitations**: No editing of an existing expense (only add + implicit list); no budget/limit feature

## 5. Diary

**Purpose**: Private journal with mood tagging and an optional AI-drafting assist.

- **User flow**: `ui/diary/DiaryScreen.kt` → "+" → write freely, pick a mood, OR tap "Turn into a diary entry with AI" to have the AI turn rough notes into a polished entry (saved as an unreviewed AI draft)
- **Database**: `data/db/entities/DiaryEntity.kt` — `aiGenerated` and `isReviewed` fields implement the "AI drafts must be approved" rule
- **Repository**: `data/repository/DiaryRepository.kt` (`approveAiDraft()`)
- **AI**: `AiRepository.draftDiaryEntry()`
- **Status**: Implemented — AI drafts show an "AI draft — needs review" label with an Approve button until confirmed

## 6. Timeline

**Purpose**: A single, day-by-day feed merging Notes, completed Tasks, completed Habits, Expenses, Diary entries, and Captures, sorted by time.

- **Files**: `ui/timeline/TimelineScreen.kt` (date navigation with previous/next arrows), `domain/usecase/BuildTimelineUseCase.kt` (the aggregation logic)
- **How it works**: Not a database table — `BuildTimelineUseCase` queries all six repositories for a given day and merges the results into `domain/model/TimelineItem.kt` objects, sorted by `timeMinutes`.
- **Status**: Implemented

## 7. Global Search

**Purpose**: Search across Notes, Tasks, Expenses, and Diary in one screen.

- **Files**: `ui/search/SearchScreen.kt`
- **How it works**: Calls `search(query)` on `NoteRepository`, `TaskRepository`, `ExpenseRepository`, `DiaryRepository` — each of which runs a plain SQL LIKE '%query%' query (see e.g. `NoteDao.search()`). This is not full-text search (no SQLite FTS extension) and not semantic/AI search.
- **Status**: Implemented (basic substring search only)

## 8. Home Dashboard

**Purpose**: At-a-glance view of today's tasks, habits, and spending.

- **Files**: `ui/home/HomeScreen.kt`, `ui/home/HomeViewModel.kt`, `domain/usecase/GetHomeSummaryUseCase.kt`
- **How it works**: `GetHomeSummaryUseCase` combines five live Flows (today's tasks, overdue tasks, all habits, today's habit completions, today's expense total) with Kotlin's combine() operator into one `HomeSummary` object.
- **Status**: Implemented

## 9. Capture (Photo / Video / Audio / Thought)

**Purpose**: Quick, in-the-moment capture of a memory.

- **Files**: `ui/capture/CaptureSheet.kt` (bottom sheet menu), `ui/capture/CameraCaptureScreen.kt` (photo, via CameraX ImageCapture), `ui/capture/VideoCaptureScreen.kt` (video, via CameraX VideoCapture/Recorder), `ui/capture/AudioCaptureScreen.kt` (via android.media.MediaRecorder), `ui/capture/CaptureDetailScreen.kt` (full viewer, added in the UI/UX pass), `ui/capture/CaptureMediaPreview.kt` + `MediaPreviewUtils.kt` (shared preview composables, added in the UI/UX pass)
- **Storage**: `core/util/MediaStorage.kt` — all captured files are written to app-private storage (context.filesDir/captures/), never to shared/public storage or MediaStore
- **Permissions**: Requested at the moment the relevant capture screen opens, via `core/util/PermissionManager.kt`'s `rememberPermissionState()` — never at app launch
- **Database**: `data/db/entities/CaptureEntity.kt`, `data/repository/CaptureRepository.kt` (now includes `getById()`, a minimal additive read method added for the Detail screen — no schema change)
- **Post-capture confirmation** (fixed during the UI/UX pass; see `docs/16_KNOWN_ISSUES.md`): after a Photo/Video/Audio capture, `CaptureSheet` now shows a CONFIRM state with a real preview of the captured file before closing, instead of dismissing silently.
- **Timeline integration**: tapping a capture item in `TimelineScreen.kt` now opens `CaptureDetailScreen`, showing the real preview, date/time, and a Delete action.
- **Status**: Implemented (all four capture types are real, working code, not stubs; post-capture confirmation and a Detail/viewer screen were added)

## 10. AI Assistant (Chat)

**Purpose**: Free-form chat with the AI about the user's day/data.

- **Files**: `ui/ai/AiAssistantScreen.kt`
- **How it works**: Sends the running conversation to `AiRepository.chat()`, which routes the request to `LifeOSIntelligenceEngine.answerQuestion()`. ⚠️ Note: the chat does not currently inject any real app data as context — `AiAssistantScreen`'s call passes `contextBlock = null`, so despite `AiRepository.chat()` supporting a context block, the chat currently only sees the conversation itself, not the user's actual notes/tasks/etc.
- **Status**: Implemented, with the context-injection limitation noted above

## 11. AI Insights / Weekly Review

**Purpose**: AI-generated summary of the week's tasks/spending/diary activity.

- **Files**: `ui/insights/InsightsScreen.kt`
- **How it works**: Computes real stats (tasks completed, total spend, diary entry count) for the current week directly from the repositories, then passes that stats block to `AiRepository.generateReviewSummary()`.
- **Status**: Implemented

## 12. App Lock

**Purpose**: PIN gate on the whole app.

- **Files**: `core/util/SettingsStore.kt`, `ui/security/AppLockScreen.kt`, gated in `MainActivity.kt`'s `AppLockGate` composable
- **How it works**: Uses the existing LifeOS PIN with salted hashing and a recovery-question flow. No biometric or device-credential authentication is used.
- **Toggle**: `ui/settings/SettingsScreen.kt`, persisted via `core/util/SettingsStore.kt`
- **Status**: Implemented

## 13. Reminders / Notifications

**Purpose**: Task and Habit reminders delivered as Android notifications.

- **Files**: `core/reminders/ReminderScheduler.kt`, `core/reminders/ReminderWorker.kt`, `core/util/NotificationHelper.kt`
- **How it works**: One OneTimeWorkRequest per reminder (not a recurring poll), uniquely named per task/habit so re-setting a reminder replaces the old job. ReminderWorker re-checks the item is still open before notifying.
- **Permission**: POST_NOTIFICATIONS (Android 13+) requested only when the user turns on the "Reminders" toggle in Settings (`SettingsScreen.kt`'s `RemindersCard`)
- **Status**: Implemented

## 14. Backup / Export / Share

**Purpose**: Full data export to a local JSON file, shareable via Android's share sheet.

- **Files**: `data/repository/BackupRepository.kt`, wired into `ui/settings/SettingsScreen.kt`
- **How it works**: `buildBackup()` collects every table into one LifeOSBackup object, serialized to pretty-printed JSON via kotlinx.serialization, written to context.filesDir (app-private storage). "Share" uses androidx.core.content.FileProvider (declared in AndroidManifest.xml + res/xml/file_paths.xml) to hand the file to any share target.
- **Import**: `BackupRepository.importFromFile()` exists and is fully implemented, but ⚠️ no Settings UI button currently calls it — restore is code-complete but not user-reachable yet.
- **Status**: Partially implemented (export: complete and reachable; import: complete but not wired to any UI)

## 15. Onboarding

**Purpose**: 4-page first-launch introduction.

- **Files**: `ui/onboarding/OnboardingScreen.kt`, gated in `MainActivity.kt`'s `OnboardingGate`
- **Status**: Implemented — shown once, persisted via `SettingsStore.onboardingComplete`

## 16. Settings

**Purpose**: App Lock toggle, AI feature toggle, Reminders toggle, Backup/Export/Share.

**Current state (2026-09-16):** cloud AI/key configuration is no longer part of the product. The current `AiRepository` delegates to the local `LifeOSIntelligenceEngine`; supported analysis requires no external provider or API key. Historical cloud-AI notes in this document are retained only for traceability.

- **Files**: `ui/settings/SettingsScreen.kt`
- **Status**: Implemented

## Current-state addendum — 2026-09-16

This document remains part of the LifeOS documentation set. Current UI/UX, motion, responsive and accessibility rules are centralized in [`DESIGN.md`](./DESIGN.md). The current Intelligence implementation is local/offline and requires no external AI provider or API key. Build/test statements are only considered verified when the exact command has been executed in a real Android/Gradle environment.

## Current implementation snapshot — 2026-09-16

Home, Habits, Tasks, Insights, Settings and Life Capture now use the updated lavender/violet component language without changing their existing local data responsibilities.

## 2026-09-18 feature update
- Profile now supports selecting a local profile photo.
- Settings is intentionally accessed from Profile rather than the Home header.
- Expenses and Add Expense received the Stitch visual treatment while retaining live repository-backed data.
- App Lock received the keypad-oriented UI treatment while retaining secure PIN/recovery behavior.
- Audio capture/playback lifecycle handling was hardened.
