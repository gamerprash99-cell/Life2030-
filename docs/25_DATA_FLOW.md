# 25 — Data Flow

## Standard flow for any feature (Notes/Tasks/Habits/Expenses/Diary/Captures)

```
USER
 -> taps a UI element
COMPOSE SCREEN (ui/feature/*.kt)
 -> calls a function on
VIEWMODEL (StateFlow-based, one per screen)
 -> calls a suspend function on
REPOSITORY (data/repository/*.kt)
 -> calls a suspend fun / observes a Flow from
ROOM DAO (data/db/dao/*.kt)
 -> reads/writes
SQLITE DATABASE (lifeos.db, on-device)
 -> Flow emits new data automatically on change, back up through
ROOM DAO -> REPOSITORY -> VIEWMODEL (StateFlow updates) -> COMPOSE SCREEN recomposes automatically
 -> USER sees the updated UI
```

There is no network step in this flow for any of these six features —
everything above is 100% on-device.

## AI-touching flow (Notes AI actions, Diary AI draft, Insights, AI Assistant)

```
USER taps an AI action button
 -> COMPOSE SCREEN -> VIEWMODEL
 -> AiRepository (core/ai/AiRepository.kt) assembles the prompt
 -> AiClient (core/ai/AiClient.kt) reads the API key from SettingsStore
    - if no key: returns AiResult.NoApiKey immediately, no network call made
    - if key present: HTTPS POST
 -> ANTHROPIC API (api.anthropic.com/v1/messages)
 -> JSON response back to AiClient, parsed into AiResult.Success or AiResult.Error
 -> AiRepository -> VIEWMODEL (result shown in UI, a dialog or inline card)
 -> USER reviews the result
 -> only if the user explicitly approves:
    REPOSITORY write (e.g. TaskRepository.createFromAiExtraction(),
                       DiaryRepository.approveAiDraft())
    -> ROOM DAO -> SQLITE DATABASE
```

Critical detail: the final "REPOSITORY write" step only happens after
explicit user approval — see docs/24_ARCHITECTURAL_DECISIONS.md ADR-004.
There is no path in the code where an AI response is written to the
database without this approval step.

## Timeline flow (aggregation, not a direct CRUD flow)

```
USER opens Timeline screen, or changes the selected date
 -> TimelineScreen.kt -> TimelineViewModel.loadFor(epochDay)
 -> BuildTimelineUseCase (domain/usecase/BuildTimelineUseCase.kt)
    queries six repositories for the same day:
      - NoteRepository.getCreatedBetween(...)
      - TaskRepository.getCreatedBetween(...) filtered to completed
      - HabitRepository.observeAllForDay(...) + observeAll()
      - ExpenseRepository.observeForDay(...)
      - DiaryRepository.observeForDay(...)
      - CaptureRepository.observeForDay(...)
    merges results into TimelineItem objects, sorted by timeMinutes
 -> TimelineViewModel._items (StateFlow)
 -> TimelineScreen recomposes with the merged, sorted list
 -> USER sees one unified feed
```

## Reminder flow (the one flow that involves Android OS scheduling, not just the app)

```
USER sets a reminder time in the Add Task / Add Habit dialog
 -> TasksScreen.kt / HabitsScreen.kt
 -> TaskRepository.createTask(...) / HabitRepository.createHabit(...)
 -> if reminderEpochMillis is in the future:
    ReminderScheduler.scheduleTaskReminder() / scheduleHabitReminder()
 -> ANDROID WORKMANAGER (OS-level job scheduler)
 -> at the scheduled time, regardless of whether the app is open:
    ReminderWorker.doWork() re-checks the item is still open via the repository
 -> if still relevant: NotificationHelper.showReminder()
 -> ANDROID NOTIFICATION SYSTEM
 -> USER sees a system notification
```

## Backup export flow

```
USER taps "Export backup" in Settings
 -> SettingsScreen.kt -> SettingsViewModel.exportBackup(context.filesDir)
 -> BackupRepository.exportToFile()
    calls all six repositories' getAllForBackup() functions
    serializes everything into one LifeOSBackup object via kotlinx.serialization
    writes pretty-printed JSON to context.filesDir
 -> LOCAL FILE (lifeos-backup-<timestamp>.json)
 -> user optionally taps "Share"
 -> FileProvider -> Android system share sheet
 -> USER chooses a destination app (email, cloud drive, etc.) — entirely the user's choice, not automatic
```

## What does NOT happen anywhere in this app (explicitly, to prevent assumptions)

- No data is ever sent to a server automatically or in the background.
- No analytics events are captured or sent anywhere (no analytics SDK exists).
- No data leaves the device except: (a) the specific text sent per AI call
  as documented in docs/11_AI_SYSTEM.md, and (b) a backup file the user
  explicitly chooses to share via the system share sheet.
