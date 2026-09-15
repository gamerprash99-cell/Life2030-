# 10 — Backend

## There is no backend server

Confirmed by direct repository inspection: no server directory, no
Node.js/Python/Go/Java server project, no `Dockerfile`, no
`docker-compose.yml`, no Firebase Cloud Functions, no serverless
configuration (Vercel/Netlify/AWS Lambda config files), and no deployment
target for a backend anywhere in this repository.

**What plays the role of "backend" here is entirely on-device code**, split
into two layers inside the single Android app module:

## 1. Business logic layer — `domain/`

- `domain/usecase/GetHomeSummaryUseCase.kt` — combines live data from three
  repositories into the Home dashboard's summary object
- `domain/usecase/BuildTimelineUseCase.kt` — the core "connect everything"
  logic; aggregates six repositories into one time-sorted feed for a given day
- `domain/model/` — pure data classes with no Android/database dependencies
  (`NoteBlock`, `TimelineItem`, `HabitAnalytics`, `HeatmapCell`, `ExpenseCategories`)

## 2. Data access layer — `data/`

- `data/repository/*.kt` — one repository per feature area (Note, Task,
  Habit, Expense, Diary, Capture) plus `BackupRepository`. These are the
  closest equivalent to a traditional backend's "service layer" — they
  contain validation, ID generation (`core/util/IdGenerator.kt`), timestamp
  handling, and orchestration logic (e.g. `HabitRepository.computeAnalytics()`
  computing real streaks from raw completion rows).
- `data/db/` — Room entities and DAOs, the closest equivalent to a
  traditional backend's database layer.

## Validation

Validation is minimal and done inline at the point of use — mostly
"is this blank?" checks before writing (e.g.
`TaskRepository`/`HabitRepository`/`ExpenseRepository`'s `create*()`
functions each guard against blank required fields via the calling
ViewModel, e.g. `TasksViewModel.addQuickTask()`'s `if (title.isBlank()) return`).
There is no schema-validation library or centralized validation layer.

## Background processes

The only background processing in the app is **WorkManager jobs** for
reminders:
- `core/reminders/ReminderWorker.kt` — a `CoroutineWorker` that fires a
  single notification, then exits (not a recurring/periodic worker)
- `core/reminders/ReminderScheduler.kt` — schedules/cancels these per-item

There is no server-side cron job, queue, or scheduled task of any kind —
everything runs within Android's own WorkManager on the user's device.

## External integrations

Only one: the Anthropic AI API call, documented fully in
`docs/06_API_DOCUMENTATION.md`. No other integration (payment processor,
email service, SMS service, push notification service, analytics pipeline)
exists.

## If a backend is added in the future

⚠️ **NOT VERIFIED FROM CODEBASE** — this section is forward-looking guidance
only, not a description of existing code. If cloud sync/multi-device/backend
features are ever added, the natural seam is the `data/repository/*.kt`
layer: today each repository talks only to a local Room DAO; a future
version could have these same repositories talk to a remote API instead (or
in addition), without the `ui/` layer needing to change, since screens
already depend only on repository interfaces via `ServiceLocator`.
