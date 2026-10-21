# Next Session

A handoff for whoever (or whichever agent) picks this up next. It states what is
finished, what is not, and the exact next actions in priority order.

## State at handoff

- Project: `/home/LifeOS` (Git repo, branch `main`).
- Build: single `:app` module; versions pinned in `gradle/libs.versions.toml`.
- Verified in this environment: Kotlin compile, JVM unit tests, lint, release
  build (see `TESTING.md` for outcomes).
- **Not runnable here:** anything needing a device/emulator.

## Next actions, in order

### 1. Run it on a device once (highest value)

No emulator exists in the build environment, so the entire UI and the persistence
layer are unverified at runtime. The single most useful next step is to open the
project in Android Studio and run it on a real device or an emulator, then:

- create a note, a task, a habit with a completion, an expense, and a diary entry;
- move each to Trash and restore it;
- run the Settings export and confirm the JSON is well-formed;
- grant camera/mic/location at point of use and confirm capture records persist.

Record what actually happened; do not assume.

### 2. Implement `core/media`

Capture records and `attachments` rows already persist, but the code that writes
the actual photo/video/audio file into app-private storage does not exist yet.
Implement `MediaStore`-free, private-storage writes plus cleanup on permanent
delete.

### 3. Implement `core/notifications`

The Settings toggle stores a preference only. Add a `WorkManager` worker that
schedules task/habit reminders from `reminderAt`, requests `POST_NOTIFICATIONS` at
the point of enabling, and cancels work when the toggle is turned off.

### 4. Add onboarding

A short first-run flow that explains local-only storage and lets the user set
currency and AI consent before creating anything.

### 5. Add an importer

`DataExporter` is export-only. Add an importer that reads `formatVersion = 1` back
in, with validation and a dry-run preview.

### 6. Consider FTS for search

Search currently uses `LIKE`. If the corpus grows large, migrate to FTS4 with a
Room companion object and a documented migration.

## Invariants to preserve

- No destructive Room migration in release builds.
- Money only as integer minor units + ISO currency code — never `Float`/`Double`.
- No seeded or fake data anywhere; the timeline reflects real user actions only.
- No secrets in the client; AI stays on-device unless a server-held-key design is
  deliberately adopted.
- Keep `PROJECT_AUDIT.md`, `TESTING.md`, and this file honest: distinguish what is
  verified from what is merely written.

## Known limitations carried forward

- Figma: the available integration is read-only and no file key was provided, so
  designs were authored as Compose tokens (documented in `PROJECT_AUDIT.md`).
- No database encryption.
- No cloud sync, by design.
