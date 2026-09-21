# Changelog

All notable changes are documented here and detailed in
[`docs/17_CHANGELOG.md`](docs/17_CHANGELOG.md) (history organized by
development session, since this repo has no prior commit log).

## [Unreleased] — 2026-09-21

- **Capture sheet redesign** (`ui/capture/CaptureSheet.kt`, `ui/theme/Color.kt`)
  from the Stitch "LifeOS Moment Capture" design: `displayLarge` title,
  borderless quick-thought field, and a 2×2 lavender "Life Capture" tile grid;
  new subtitle/dark-mode tokens. Visual layer only — no navigation, schema,
  or data changes. Verified: 176 unit tests pass, `assembleDebug` and
  `lintDebug` (0 errors) succeed.
- **Diary (LifeOS Journal) redesign** (`ui/diary/`, `core/intelligence/`)
  from the Stitch "LifeOS Diary / Journal" design: paper/ink-violet editorial
  palette, day-strip list screen with mood pills and keyword chips, bottom-
  sheet composer with 5-mood picker, entry detail route, expandable offline
  analytics (narrative, mood chart, themes, patterns, recommendations) and a
  connection radar tying entries to the day's timeline. New
  `LifeOSIntelligenceEngine.diaryInsights()` + `DiaryInsights` model,
  `DiaryConnections` builder. No schema/dependency/navigation-architecture
  change. Verified: 101 unit tests pass, `assembleDebug` and `lintDebug`
  (0 errors) succeed.

## [0.2.0] — Hardening pass (2026-09-18 … 2026-09-20)

Encrypted-database key safety, app lock, profile, search, dates/timeline,
habits, tasks, home dashboard, media capture hardening. See
`docs/17_CHANGELOG.md`.