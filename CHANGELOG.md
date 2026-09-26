# Changelog

All notable changes are documented here and detailed in
[`docs/17_CHANGELOG.md`](docs/17_CHANGELOG.md) (history organized by
development session, since this repo has no prior commit log).

## [Unreleased] — 2026-09-26 (branch `feat/stitch-timeline-diary`)

- **Diary rebuilt on real data.** The Diary feature now stores and reads real content instead of a visual mock-up. Room **v4 → v5** adds one column, `diary_entries.isFavorite`, via an additive, data-preserving migration. Photos, voice notes and the captured place are persisted in the `attachmentsJson` column that already existed since v1 and was never written to, using a new sealed `kotlinx.serialization` `DiaryAttachment` model (`Photo`/`VoiceNote`/`Place`) with a stable on-disk `kind` tag and total, non-throwing decoding. The bottom-sheet composer is deleted and replaced by a full-screen `DiaryEditorScreen` with real photo picking via the Android Photo Picker (no storage permission), real `MediaRecorder`/`MediaPlayer` voice notes, platform-only `LocationManager`/`Geocoder` place capture, and an honest `Unavailable` weather state (this build has no permitted weather source — `INTERNET` is still absent, so none is possible). The day list and the rewritten `DiaryDetailScreen` now derive their content and metadata from stored state, with Share, Copy, Edit, Delete and Favourite actions, and a FileProvider one-shot read grant for sharing a photo. Adds `RECORD_AUDIO` + coarse/fine location (all user-initiated, with a Settings route after two denials); `CAMERA` deliberately not declared. See `UPDATE.md` for details; verified offline: 119 unit tests (`testDebugUnitTest`), `assembleDebug`, `assembleDebugAndroidTest` and `lintDebug` (0 errors) all pass. The new v4→v5 migration test is instrumentation-only and still needs a device to run.

## [Unreleased] — 2026-09-24 (branch `feat/stitch-timeline-diary`)

- **Timeline & Diary visual pass from Stitch.** Timeline (`ui/timeline/TimelineScreen.kt`) redrawn as a dated journal: centered dated header with back + day-navigation chevrons, a 2px `#E8E1EA` hairline spine on a fixed 24dp+ left track anchored by 30dp paper node badges, 24dp-radius time-stamped cards, and a themed empty state. Entries are now tappable via a new `onOpenItem` callback (default no-op) — diary items open the existing Diary detail, task/habit/expense items open their existing sections; navigation architecture unchanged. Data layer untouched (`BuildTimelineUseCase`, `TimelineViewModel`).
- **Diary polish from the Stitch mood palette:** pastel mood-pill backgrounds (`DiaryMoods.backgroundOf`, new `DiaryMood*Pastel` tokens + `DiaryHairline`/`DiarySaveDisabled` in `ui/theme/Color.kt`), pastel-filled selected mood chips in the editor, a flat lavender pill Save button (disabled `#F4ECFF`), and 16dp day-strip capsules with hairline borders. Mood keys persisted are unchanged; no schema (Room v2) or repository changes.
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