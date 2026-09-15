# 01 — Product Requirements

## Source of this document

⚠️ **NOT VERIFIED FROM CODEBASE**: There is no standalone Product Requirements
Document (PRD), user story file, or specification file inside this
repository as delivered. This document is reconstructed by reading the
*implemented code and its inline comments*, which frequently reference a
"spec" (e.g. `// Section 8/10`, `// Rule #9`) that is **not itself present
in the repo**. Where a requirement is stated below, it is because the code
directly implements it — not because a source requirements document was
read.

## Product vision (inferred from code)

A single, local-first Android app that unifies personal notes, tasks,
habits, expenses, and diary entries into one connected experience, with
optional, user-controlled AI assistance, and strict user control over data
(no forced cloud sync, no silent AI writes).

## User types

Only one user type exists in the implementation: a single, anonymous,
on-device user. There is no concept of admin, guest, or multi-user account
in the code (see `docs/07_AUTHENTICATION.md`).

## Functional requirements — status

Legend: **IMPLEMENTED** / **PARTIALLY IMPLEMENTED** / **PLANNED (referenced in code comments only)** / **NOT IMPLEMENTED**

| Requirement | Status | Evidence (file) |
|---|---|---|
| Create/edit/delete rich-text notes (headings, bullets, checklists) | **IMPLEMENTED** | `ui/notes/NoteEditorScreen.kt`, `domain/model/NoteBlock.kt` |
| Pin / favorite / archive / trash notes with restore | **IMPLEMENTED** | `data/repository/NoteRepository.kt` |
| AI note actions (summarize, rewrite, extract tasks, etc.) | **IMPLEMENTED** (requires user-provided API key) | `core/ai/AiModels.kt` (`NoteAiAction`), `ui/notes/NotesViewModel.kt` |
| Create/complete/reschedule tasks with priority & due date | **IMPLEMENTED** | `ui/tasks/TasksScreen.kt`, `data/repository/TaskRepository.kt` |
| Task reminders (notifications) | **IMPLEMENTED** | `core/reminders/ReminderScheduler.kt`, `core/reminders/ReminderWorker.kt` |
| Recurring tasks (daily/weekly/monthly/custom) | **PARTIALLY IMPLEMENTED** — `RepeatRule` field and enum exist on `TaskEntity`, but no scheduled job auto-creates the next occurrence | `data/db/entities/TaskEntity.kt` |
| Habit tracking with streaks | **IMPLEMENTED** | `data/repository/HabitRepository.kt` (`computeAnalytics`) |
| Habit heatmap (GitHub-style calendar) | **IMPLEMENTED** | `data/repository/HabitRepository.kt` (`computeHeatmap`), `ui/habits/HabitDetailScreen.kt` |
| Goal-count habits (e.g. "drink 8 glasses of water") | **IMPLEMENTED** | `HabitEntity.goalCount` |
| Habit reminders | **IMPLEMENTED** | Same reminder infrastructure as tasks |
| Expense logging with categories | **IMPLEMENTED** | `ui/expenses/ExpensesScreen.kt`, `domain/model/Categories.kt` |
| Monthly expense totals / category breakdown | **IMPLEMENTED** | `data/db/dao/ExpenseDao.kt` (`getCategoryTotals`) |
| Diary entries with mood tags | **IMPLEMENTED** | `ui/diary/DiaryScreen.kt` |
| AI-drafted diary entries with mandatory human review | **IMPLEMENTED** | `DiaryEntity.aiGenerated` / `isReviewed`, `ui/diary/DiaryScreen.kt` (`approveDraft`) |
| Unified Timeline (all features merged by date/time) | **IMPLEMENTED** | `domain/usecase/BuildTimelineUseCase.kt` |
| Global search across notes/tasks/expenses/diary | **IMPLEMENTED** | `ui/search/SearchScreen.kt` — simple `LIKE` queries, not full-text search |
| Home dashboard (today's tasks/habits/spend) | **IMPLEMENTED** | `domain/usecase/GetHomeSummaryUseCase.kt`, `ui/home/HomeScreen.kt` |
| Photo capture | **IMPLEMENTED** | `ui/capture/CameraCaptureScreen.kt` (CameraX) |
| Video capture | **IMPLEMENTED** | `ui/capture/VideoCaptureScreen.kt` (CameraX) |
| Audio capture | **IMPLEMENTED** | `ui/capture/AudioCaptureScreen.kt` (MediaRecorder) |
| Quick text "thought" capture | **IMPLEMENTED** | `ui/capture/CaptureSheet.kt` |
| AI Assistant chat | **IMPLEMENTED** | `ui/ai/AiAssistantScreen.kt` |
| AI weekly review summary | **IMPLEMENTED** | `ui/insights/InsightsScreen.kt` |
| App Lock (biometric/PIN) | **IMPLEMENTED** | `core/security/AppLockManager.kt`, gated in `MainActivity.kt` |
| Full backup export to local JSON | **IMPLEMENTED** | `data/repository/BackupRepository.kt` |
| Backup restore from JSON | **IMPLEMENTED** | `data/repository/BackupRepository.kt` (`importFromFile`) — ⚠️ no UI screen calls this import function; only export is wired to a button |
| Share exported backup via system share sheet | **IMPLEMENTED** | `ui/settings/SettingsScreen.kt` |
| Onboarding (first-launch intro) | **IMPLEMENTED** | `ui/onboarding/OnboardingScreen.kt` |
| User accounts / login / signup | **NOT IMPLEMENTED** | No such code exists anywhere in the repo |
| Cloud sync / multi-device | **NOT IMPLEMENTED** | No networking code for sync exists |
| Semantic / AI-powered search | **NOT IMPLEMENTED** | `SearchScreen.kt` uses plain SQL `LIKE`, no embeddings/vector search |
| Note-to-note linking | **NOT IMPLEMENTED** | No wiki-link or reference code exists |
| Calendar sync (Google Calendar etc.) | **NOT IMPLEMENTED** | No calendar API integration exists |
| Home-screen widgets | **NOT IMPLEMENTED** | No `AppWidgetProvider` or glance widget code exists |
| Encryption at rest | **NOT IMPLEMENTED** | Room database (`AppDatabase.kt`) uses plain SQLite, not SQLCipher |
| Automated tests | **NOT IMPLEMENTED** | Test dependencies declared in `app/build.gradle.kts`; zero test files exist |

## Non-functional requirements (inferred from code comments)

- **Local-data-first / privacy**: `android:allowBackup="false"` in
  `AndroidManifest.xml`; AI calls only fire on explicit user action and only
  send the minimum text needed (see `core/ai/AiRepository.kt` comments).
- **Minimal-permission**: Camera/Mic/Notification permissions are requested
  at the point of use, not at launch (see `core/util/PermissionManager.kt`
  and its usage in `ui/capture/*` and `ui/settings/SettingsScreen.kt`).
- **Reviewable AI**: every AI-derived write (extracted tasks, AI diary
  drafts) requires an explicit user confirmation step before it's persisted
  (`ui/notes/NoteEditorScreen.kt` extracted-tasks dialog; `DiaryEntity.isReviewed`).

These are design principles that are consistently followed in the code, but
⚠️ **NOT VERIFIED FROM CODEBASE** as *documented, ratified* NFRs — no formal
NFR document exists; they are reconstructed from repeated patterns and
comments across the source files cited above.

## User journeys (as implemented)

1. **First launch** → `OnboardingScreen` (4 pages) → optional `AppLockGate`
   (only if enabled in Settings) → `HomeScreen`.
2. **Daily use** → `HomeScreen` shows today's tasks/habits/spend → user taps
   into Tasks/Habits/Notes/Diary/Expenses/Timeline/Search/AI Assistant via
   bottom nav or Home's quick-link chips (`ui/navigation/LifeOSNavHost.kt`).
3. **Capture in the moment** → Home FAB → `CaptureSheet` → Photo / Video /
   Audio / Thought.
4. **AI usage** (opt-in) → Settings → enter API key → AI actions become
   functional across Notes, Diary, Insights, and the AI Assistant chat.
5. **Backup** → Settings → Export backup → optional Share via system share sheet.

## Feature priorities

⚠️ **NOT VERIFIED FROM CODEBASE** — no prioritization/backlog document
exists in the repo. All implemented features currently ship at equal
priority (i.e., all are reachable from the main navigation).
