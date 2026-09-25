# Changelog

All notable changes are documented here and detailed in
[`docs/17_CHANGELOG.md`](docs/17_CHANGELOG.md) (history organized by
development session, since this repo has no prior commit log).

## [Unreleased] — 2026-09-25 (branch `feat/diary-daily-memory-redesign`)

- **Diary redesigned as *Daily Memory*** (`ui/diary/`, `ui/theme/Color.kt`). The Diary is now an editorial memory timeline read one day at a time instead of a card list with a date strip: a day masthead with previous/next day stepping and memory-position ticks (`DiaryDayHeader`), a card-free timeline on a 1dp spine with mood markers and large leading journal type (`MemoryTimeline`), an inline `+ Memory` action instead of a FAB, and a `YOUR STORY STARTS HERE` empty state. The bottom-sheet composer is replaced by a full-screen `DiaryEditor` (back closes the editor, not the screen), and `DiaryDetailScreen` keeps its route and ViewModel while adopting the same spine/mood/leading language. Deliberately *not* a clone of the existing Timeline (which uses paper badges + cards) so the two surfaces stay distinguishable.
- **Eight moods, five stored keys unchanged.** Added Angry, Anxious and Tired to the existing Happy/Calm/Sad/Stressed/Excited. New keys are additive strings; the five original keys are preserved byte-for-byte so existing entries keep their mood, and `DiaryMoods.fromStored` still resolves them. No Room schema change (mood is `TEXT`; the DB is v4 with three explicit migrations, all untouched).
- **Bug fix — writing into a past day.** `DiaryViewModel.saveEntry` previously stamped new entries with `today()` regardless of the day being read, so journaling into a past day landed on today. It now writes to `selectedDay`. Added an in-flight `saving` guard (cleared in a `finally`, so a failed write cannot wedge the editor) to prevent double-submits, and `shiftDay` refuses to move past today.
- **Unchanged on purpose:** navigation routes, bottom bar, DI/`LocalServiceLocator`, repositories, DAOs, entities, migrations, schema JSON, manifest and Gradle dependencies. No network, no logging, no secrets, no AI/analytics. See `UPDATE.md`; verified offline: 111 unit tests (`testDebugUnitTest`), `assembleDebug` and `lintDebug` (0 errors) all pass.
- Docs: corrected the Diary feature row and UI/UX section (they described removed Notes/Capture/Search/AI directories and a non-existent `core/intelligence/DiaryConnections.kt`), fixed the bottom-bar count (3: Home/Tasks/Habits, per `BottomNavItemsTest`), and fixed a "Room v2" reference that should read v4.

## [Unreleased] — 2026-09-24 (branch `feat/stitch-timeline-diary`)

- **Timeline & Diary visual pass from Stitch.** Timeline (`ui/timeline/TimelineScreen.kt`) redrawn as a dated journal: centered dated header with back + day-navigation chevrons, a 2px `#E8E1EA` hairline spine on a fixed 24dp+ left track anchored by 30dp paper node badges, 24dp-radius time-stamped cards, and a themed empty state. Entries are now tappable via a new `onOpenItem` callback (default no-op) — diary items open the existing Diary detail, task/habit/expense items open their existing sections; navigation architecture unchanged. Data layer untouched (`BuildTimelineUseCase`, `TimelineViewModel`).
- **Diary polish from the Stitch mood palette:** pastel mood-pill backgrounds (`DiaryMoods.backgroundOf`, new `DiaryMood*Pastel` tokens + `DiaryHairline`/`DiarySaveDisabled` in `ui/theme/Color.kt`), pastel-filled selected mood chips in the editor, a flat lavender pill Save button (disabled `#F4ECFF`), and 16dp day-strip capsules with hairline borders. Mood keys persisted are unchanged; no schema (Room v4) or repository changes.
- See `UPDATE.md` for details; verified offline: 108 unit tests (`testDebugUnitTest`), `assembleDebug`, and `lintDebug` (0 errors) all pass.

## [Unreleased] — 2026-09-24 (branch `feat/reliable-coalesced-alarms`)

- **Coalesced, reliable alarms** for tasks & habits. The reminders table is projected into one alarm **event per distinct trigger time** (`AlarmEventProjector`), with the full snapshot encoded into each alarm/activity intent's extras (`AlarmEventCodec`) so alarms present instantly with zero DB reads — no caps. New `AlarmEvent` model and `AlarmIntents`, rewritten `ReminderScheduler` with a `setAlarmClock` → `setAndAllowWhileIdle` ladder (Doze-exempt vs permission-free fallback), new `AlarmPlaybackService` (`mediaPlayback` FGS that owns all audible ringing + ONE high-priority notification with FSI/STOP/Snooze), rewritten `AlarmReceiver` + full-screen `AlarmFullScreenActivity` (Compose, grouped TASKS/HABITS sections, per-type snooze), BootReceiver time/zone wall-clock mirror re-derive, habit exact-alarm gate, and `FOREGROUND_SERVICE`/`FOREGROUND_SERVICE_MEDIA_PLAYBACK`/`WAKE_LOCK` manifest permissions. No Room schema change. See `UPDATE.md` for details; verified offline: 108 unit tests (`testDebugUnitTest`), `assembleDebug`, and `lintDebug` (0 errors) all pass.

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