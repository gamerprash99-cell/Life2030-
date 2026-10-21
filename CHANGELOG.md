# Changelog

All notable changes to LifeOS are recorded here. The project follows
[Keep a Changelog](https://keepachangelog.com/) and intends to follow
[Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- **Design system** — `core/designsystem/theme/` with the exact LifeOS palette
  (dark and light), typography, spacing, radii, and control tokens. Documented
  contrast ratios for the semantic colour roles.
- **Persistence** — Room schema v1 with 12 entities: folders, tags, entity_tags,
  notes, tasks, habits, habit_completions, expenses, diary_entries, captures,
  attachments, life_events. Soft-delete (`deletedAt`) columns throughout for
  trash/restore.
- **Domain layer** — `Money` (exact minor units, never float), `Dates` (ISO local
  dates), `Recurrence` + `RecurrenceRules`, `HabitProgress`, `Tags`, `Ids`, shared
  enums.
- **Data layer** — repositories for notes, tracking (tasks/habits/expenses/diary),
  captures, timeline + search, and settings; `AppContainer` manual DI graph.
- **AI layer** — `AiProvider` interface, graded `AiConsent` levels (0–3), and a
  local, deterministic, offline `LocalRuleBasedProvider`. No remote provider and
  no embedded keys.
- **Export** — `core/backup/DataExporter`: dependency-free, versioned
  (`formatVersion = 1`) JSON plus a text summary; exact `amountMinor` preserved.
- **Features** — Home, Notes, Capture, Tracking, Insights, Life (Timeline /
  Calendar / Trash), Search, and Settings screens wired through a single Compose
  `NavHost`. Settings includes a working export action that hands the payload to
  the system share sheet.
- **Tests** — JVM unit tests for `Money`, `Dates`, `RecurrenceRules`,
  `HabitProgress`, `Tags`, and the local AI provider.
- **Docs** — `PROJECT_AUDIT.md`, `IMPLEMENTATION_PLAN.md`, `ARCHITECTURE.md`,
  `SECURITY.md`, `PRIVACY.md`, `TESTING.md`, `CHANGELOG.md`, `NEXT_SESSION.md`.

### Notes

- Release builds intentionally do **not** enable destructive Room migration.
- Money is stored only as integer minor units + ISO currency code.

### Known gaps

- `core/media` (writing media files to private storage) is not implemented yet.
- `core/notifications` (WorkManager reminder worker) is not implemented yet.
- No onboarding flow.
- Search uses `LIKE`, not FTS.
