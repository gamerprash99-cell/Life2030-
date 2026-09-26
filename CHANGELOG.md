# Changelog

All notable changes are documented here and detailed in
[`docs/17_CHANGELOG.md`](docs/17_CHANGELOG.md) (history organized by
development session, since this repo has no prior commit log).

## [Unreleased] — 2026-09-26 (branch `feature/diary-date-strip-and-startup`)

- **Diary date strip** (`ui/diary/DiaryDateStrip.kt`, new; `DiaryDayHeader.kt`, `DiaryScreen.kt`, `DiaryViewModel.kt`). Replaces the seven ambiguous day ticks and the prev/next chevrons with a `LazyRow` of weekday-over-numeral cells that keeps the selected day centred. Cell width is *derived* as one fifth of the measured width, so the strip is correct at any width, aspect ratio, font scale and navigation mode and re-derives on configuration change. The range is bounded to `today-365 .. today` by construction (366 fixed cells, ~5 ever realised, no future day), centred flings use `rememberSnapFlingBehavior(state, SnapPosition.Center)` from the compose-foundation `snapping` API already in the resolved 1.7.5 — no new dependency — and one haptic fires per *settled* day change rather than per tap or per drag, so it cannot double-fire. The "days with memories" signal the ticks carried is preserved as a dot under the numeral, now unambiguous because selection is a tinted pill. The masthead is compacted to a `TODAY` eyebrow over `26 September · Saturday`, and the oversized `displayLarge` numeral that outweighed the memories is gone. `DiaryDayHeader`'s `canGoForward`/`onPrevious`/`onNext` and `DiaryViewModel.shiftDay()` are removed as dead; `selectDay()` clamps to the same bounds in the ViewModel.
- **Bug fix — diary timestamps were written at save time.** `DiaryViewModel.saveEntry` called `nowMinutesOfDay()` when saving, so opening the composer at 8:04, writing for twenty minutes and saving filed the memory at 8:24. The minute is now captured in `startNewEntry()`/`startEdit()` and reused on save; `startEdit` captures the entry's existing `timeMinutes` so re-saving an edit can never move a memory in the day's timeline; and the new `editorTimeMinutes` state is rendered in `DiaryEditor` (`Written at 8:04 AM` when editing) so the stored value is visible before the save and cannot drift while typing. No schema change — `DiaryEntity.timeMinutes`/`createdAt` already existed and Room stays v4 with its three migrations untouched.
- **Bug fix — the editor double-padded the navigation bar.** `DiaryEditor` combined `windowInsetsPadding(systemBars)` with `imePadding()`, so the bottom inset was counted twice once the keyboard was up. The bottom inset is now the `union` of `navigationBars` and `ime`, with `safeDrawing` minus that union applied to the top.
- **Startup: the SQLCipher open was happening on the main thread.** Every `ServiceLocator` member was an eager `val` that dereferences `database` — directly or through a repository already holding a DAO — so the first assignment in `LifeOSApplication.onCreate()` forced `AppDatabase.getInstance()` before `setContent` (native load, Keystore + AES/GCM unwrap, SharedPreferences, Room build). All members are now `by lazy`, so an unopened feature also never builds its repository. `AppDatabase.warmUpOpen()` no longer runs `habitDao().getAllForBackup()` — which deserialised every habit row on the startup path, cost that grew with the user's data — but `SELECT 1` via `openHelper.writableDatabase`, inside `withContext(Dispatchers.IO)`. `MainActivity` also *composed* `LifeOSNavHost` behind the splash behind a background-coloured overlay, which built Home's ViewModel and would have put the open straight back on the main thread; content is now genuinely gated on `DatabaseInit.Ready`. New `core/util/StartupTrace.kt` adds `android.os.Trace` sections (`Application.onCreate`, `di.build`, `db.open`, `db.passphrase`, `MainActivity.setContent`, `home.firstFrame`) that compile to no-ops unless a Perfetto session is attached — no logging, no production cost, API 29+.
- **Animation polish.** Strip selection uses non-bouncy springs on the pill fill and numeral scale; the mood label cross-fades over 160/110ms instead of swapping, since it changes on every tap and an instant replacement reads as a flicker exactly when the user is looking for confirmation; new `Modifier.fadeInAsContent()` fades the empty state's open spine in with no translation, because the state is already vertically centred and a slide would read as the layout moving. All Compose animation coroutines already honour the system "Remove animations" setting.
- **Tests.** New `DayStripRangeTest` (8 cases) pins the strip's bounds; new `DiaryViewModelTest` (13 cases) covers open-time-vs-save-time, per-entry stamps, edit preservation of `timeMinutes`/`createdAt`, editor-minute lifecycle, past-day filing, day clamps and day-scoped content. `DiaryViewModel` gained two defaulted clock seams so these are deterministic, with production call sites unchanged. `kotlinx-coroutines-test:1.9.0` added **test-only** and version-matched to the existing coroutines dependency. Verified offline: 132 unit tests (up from 111), 0 failures; `assembleDebug`, `assembleDebugAndroidTest` and `lintDebug` (0 errors, only pre-existing warnings) all pass. See `UPDATE.md`.
- **Not verified:** no emulator/device/AVD is available in this sandbox, so there are no measured before/after cold-start timings and the UI has not been viewed on a screen. The trace sections are the instrumentation that makes the measurement possible on real hardware, not a substitute for it.

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
## [Unreleased] — 2026-09-26 — Diary UI v2 complete implementation

- Redesigned the Diary empty state with a local Compose illustration and clearer editorial capture hierarchy.
- Made the full-screen Diary composer keyboard-first with explicit focus/keyboard opening and retained the existing keyboard/navigation inset union.
- Added singular/plural character count presentation.
- Added a post-save `Memory saved` confirmation driven by successful local Room write state.
- Added unit coverage for the save-confirmation event and blank-save rejection.
- Preserved Room v4, existing navigation, Repository/ViewModel boundaries, mood vocabulary, date-strip bounds, delete flow and offline-first/privacy-first behavior.
- Build/device verification remains pending because no Android runner or outbound GitHub network is available in this environment.


## [Unreleased] — 2026-09-26 — Diary UI reference alignment pass

- Aligned the Diary main surface to the approved responsive reference direction, including lower-right responsive add action and adaptive content flow.
- Preserved the existing Room/ViewModel/Repository and Timeline aggregation architecture.
- No Room schema, migration, network, external AI, telemetry or new dependency changes.
- Device/emulator validation remains pending because no Android runner is available in the connected environment.
