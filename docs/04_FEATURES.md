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

## 3. Habits

**Purpose**: Habit tracking with streaks, a GitHub-style heatmap, and goal-count habits (e.g. "drink 8 glasses of water").

- **User flow**: `ui/habits/HabitsScreen.kt` (list + add, with optional reminder) → tap "Details" → `ui/habits/HabitDetailScreen.kt` (streak stats + 12-week heatmap + "Log today's progress" button)
- **Files**: `ui/habits/HabitsScreen.kt`, `ui/habits/HabitDetailScreen.kt`
- **Database**: `data/db/entities/HabitEntity.kt` (habit definition), `HabitCompletionEntity` (one row per habit per day)
- **Repository**: `data/repository/HabitRepository.kt` — contains the actual streak/heatmap math (`computeAnalytics()`, `computeHeatmap()`), computed live from `habit_completions` rows, not hardcoded
- **Domain models**: `domain/model/Categories.kt` (`HabitAnalytics`, `HeatmapCell`, `HeatmapIntensity`)
- **Status**: Implemented

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
- **How it works**: Sends the running conversation to `AiRepository.chat()`, which calls `AiClient.complete()`. ⚠️ Note: the chat does not currently inject any real app data as context — `AiAssistantScreen`'s call passes `contextBlock = null`, so despite `AiRepository.chat()` supporting a context block, the chat currently only sees the conversation itself, not the user's actual notes/tasks/etc.
- **Status**: Implemented, with the context-injection limitation noted above

## 11. AI Insights / Weekly Review

**Purpose**: AI-generated summary of the week's tasks/spending/diary activity.

- **Files**: `ui/insights/InsightsScreen.kt`
- **How it works**: Computes real stats (tasks completed, total spend, diary entry count) for the current week directly from the repositories, then passes that stats block to `AiRepository.generateReviewSummary()`.
- **Status**: Implemented

## 12. App Lock

**Purpose**: Biometric/PIN gate on the whole app.

- **Files**: `core/security/AppLockManager.kt`, gated in `MainActivity.kt`'s `AppLockGate` composable
- **How it works**: Uses `androidx.biometric.BiometricPrompt` with `BIOMETRIC_WEAK or DEVICE_CREDENTIAL` — accepts either a device biometric or the device's PIN/pattern/password, never a LifeOS-specific password.
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

**Update (UI/UX pass)**: the visible "Anthropic API key" text field and "Save API key" button were removed from the AI Features card at the product owner's explicit request — the raw developer-key-paste UI didn't fit the intended production experience. The underlying storage mechanism (`SettingsStore.aiApiKey`/`setAiApiKey()`) was **not** deleted, only its UI entry point. **Consequence**: as of this change, there is currently no way for a user to set an AI API key through the app UI, so AI features will show their existing "Add your AI API key in Settings..." fallback message until a proper configuration mechanism is designed (see `docs/16_KNOWN_ISSUES.md`).

- **Files**: `ui/settings/SettingsScreen.kt`
- **Status**: Implemented
