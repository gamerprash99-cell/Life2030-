# LifeOS — UPDATE

Change log for the `fix/audit-hardening` branch (UI/UX + navigation audit and redesign, 2026-09-17).
---

---

## 2026-09-26 — Diary date strip, honest timestamps, startup fix (branch `feature/diary-date-strip-and-startup`)

Three problems, one pass. The Diary gains a real date picker, diary timestamps stop
lying, and the multi-second cold start gets an actual root cause. Room stays v4 with
its three migrations untouched, navigation is unchanged, and the app stays offline.

### 1. The date strip replaces the ticks and chevrons

The 2026-09-25 redesign gave day navigation seven ambiguous dots — a filled tick
meant either "selected" or "has memories" depending only on its size — plus two
chevrons, and gave the `displayLarge` day numeral more visual weight than the
memories themselves. Both are gone.

- `ui/diary/DiaryDateStrip.kt` *(new)* — a `LazyRow` of weekday-over-numeral cells
  that keeps the selected day centred. Everything is **derived, not hard-coded**: a
  cell is exactly one `VISIBLE_DATES`(=5)th of the measured width, so five dates fit
  a small phone, a large phone and a landscape window with no magic dp value, and it
  re-derives on configuration change instead of caching a stale pixel width.
- **Bounded by construction.** The range is `today-365 .. today` — 366 fixed cells,
  only ~5 ever realised, and no unbounded or ever-growing list. There is no tomorrow
  to journal, so the upper bound is `today` itself. `dayStripRange()` is a pure
  function precisely so the bound is unit-testable.
- **Centred flings** via `rememberSnapFlingBehavior(state, SnapPosition.Center)` from
  the `snapping` API already present in the resolved compose-foundation 1.7.5. No new
  dependency and no reaching into `LazyRow` internals. (There is deliberately no
  manual fling hook: the built-in provider is the version-correct path.)
- **One haptic per real day change**, keyed on the *selected* value rather than the
  tap or the drag. A tap that also re-centres the strip therefore cannot double-fire,
  and holding a finger down produces nothing. The day-swap is likewise driven by the
  index the strip has *settled* on, not by intermediate scroll positions.
- The "which days have memories" signal the ticks carried is preserved as a small
  dot under the numeral — now unambiguous, because selection is a tinted pill.
- `ui/diary/DiaryDayHeader.kt` — back + `+ Memory` row, then a compact hierarchy:
  `TODAY` eyebrow over `26 September · Saturday`. The strip is full-bleed so a
  centred cell is genuinely centred; the label above keeps its screen padding.
- `DiaryDayHeader`'s `canGoForward` / `onPrevious` / `onNext` parameters and
  `DiaryViewModel.shiftDay()` are **removed** — the strip is now the only day-navigation
  control, so they were dead weight. `selectDay()` clamps to the same range, in the
  ViewModel rather than the composable, so no caller can park the screen on a day the
  strip cannot render.

### 2. Timestamps stop lying

`DiaryViewModel.saveEntry` called `DateTimeUtils.nowMinutesOfDay()` **at save time**.
Open the composer at 8:04, write for twenty minutes, save, and the memory was filed
at 8:24 — the time silently moved, and there was no way to see it before committing.

- The minute is now captured in `startNewEntry()` / `startEdit()` and reused on save.
  `startEdit` captures the entry's *existing* `timeMinutes`, so re-saving an edited
  memory can never move it in the day's timeline.
- `editorTimeMinutes` is exposed as state and rendered in `DiaryEditor` under the date
  with a small clock glyph (`Written at 8:04 AM` when editing), so the value that will
  be stored is visible before the save, and it does not drift while the user types.
- No schema change: `DiaryEntity.timeMinutes` and `createdAt` already existed.
  `DiaryDetailScreen` passes the stored `timeMinutes` through; it only ever edits.

### 3. Startup: the actual root cause

- **`ServiceLocator` was forcing SQLCipher open on the main thread.** Every member was
  an eager `val`, and each one dereferences `database` — directly, or through a
  repository that already holds a DAO — so the first assignment in
  `LifeOSApplication.onCreate()` called `AppDatabase.getInstance()` before `setContent`:
  native library load, Keystore load + AES/GCM unwrap, SharedPreferences read and the
  Room build. All members are now `by lazy`, which also means a feature nobody opens
  never pays for its repository.
- **The warm-up query was reading a whole table.** `AppDatabase.warmUpOpen()` used
  `habitDao().getAllForBackup()`, deserialising every habit row on the startup path —
  waste that grew with the user's data, paid for while the splash was still up. It is
  now `openHelper.writableDatabase.query("SELECT 1")`, which reaches the same code path
  at constant cost, inside `withContext(Dispatchers.IO)` so the blocking open can never
  land on the main thread regardless of caller.
- **`MainActivity` composed the app behind the splash.** It drew a background-coloured
  `Surface` over `LifeOSNavHost()` while the database was still opening — but
  *composing* the nav host builds Home's ViewModel, whose repository chain is exactly
  what forces `AppDatabase.getInstance()`. The overlay would have quietly put the
  SQLCipher open straight back on the main thread. The content is now genuinely gated
  on `DatabaseInit.Ready`; the native splash covers the wait, so nothing is lost.
- **`core/util/StartupTrace.kt` *(new)*** — `android.os.Trace` only, so the framework
  compiles it to a no-op unless a Perfetto/systrace session is attached: production
  cost is one boolean check per call site and nothing reaches logcat. Sections:
  `lifeos:Application.onCreate`, `lifeos:di.build`, `lifeos:db.open` (async),
  `lifeos:db.passphrase`, `lifeos:MainActivity.setContent`, `lifeos:home.firstFrame`.
  Supported from API 29, where `Trace.isEnabled()` was added; below that every entry
  point collapses to a bare call of the wrapped block.
- No logging, no `INTERNET` permission, no new runtime dependency.

### Animation
Restrained and system-setting-aware throughout (Compose animation coroutines already
honour "Remove animations", so no duration-scale plumbing was needed). The strip's
selected pill and numeral use non-bouncy springs; the mood label cross-fades over
160/110ms instead of snapping, since it changes on every tap and an instant swap
reads as a flicker exactly when the user is looking for confirmation;
`Modifier.fadeInAsContent()` (new, in `DiaryMotion.kt`) fades the empty state's open
spine in with no translation, because the state is already vertically centred and a
slide would read as the layout moving.

### Tests
New `DayStripRangeTest` (8 cases) pins the strip's bounds: ends on today, starts
exactly one window back, ascending and contiguous, no repeats, never a future day,
honours a narrower window, and asserts the year-long bound on purpose.
New `DiaryViewModelTest` (13 cases) covers the open-time-vs-save-time distinction,
per-entry stamps, edit preservation of both `timeMinutes` and `createdAt`, editor
minute lifecycle, past-day filing, the day clamps, day-scoped content and
`daysWithMemories`. The ViewModel gained two defaulted clock seams
(`nowMinutes`, `todayEpochDay`) purely so these are deterministic; production call
sites are unchanged. `kotlinx-coroutines-test:1.9.0` was added **test-only**,
version-matched to the existing `kotlinx-coroutines-android` so no second coroutines
is pulled in.

### Verification (offline, no network)
`/opt/gradle-8.9/bin/gradle --offline :app:testDebugUnitTest :app:assembleDebug
:app:assembleDebugAndroidTest :app:lintDebug` → **BUILD SUCCESSFUL**;
**132 unit tests, 0 failures/errors** (up from 111); lint **0 errors**, and every
remaining warning is pre-existing in files this change does not touch. Also
confirmed by diff review: no `Log.`/`println`/TODO, no secrets, no `INTERNET`
permission, and no change under `data/db/` migrations, `schemas/`, `ui/navigation/`,
`androidTest/` or the manifest.

### Not verified here
No emulator, device or AVD is available in this sandbox, so there are **no measured
before/after cold-start timings** — the trace sections above are the instrumentation
that makes that measurement possible on real hardware, not a substitute for it. The
UI has likewise not been viewed on a screen. Both remain open.

## 2026-09-25 — Diary redesigned as *Daily Memory* (branch `feat/diary-daily-memory-redesign`)

The Diary stops being a list of paper cards with a date strip and becomes an
editorial memory timeline you read one day at a time. UI layer only — the Room
schema (v4), entities, DAOs, migrations, repositories, navigation routes, DI
and Gradle dependencies are all untouched, and the app stays fully offline.

### Files
- `ui/diary/DiaryScreen.kt` — rewritten around `DiaryDayHeader` +
  `MemoryTimeline`; inline `+ Memory`; day-scoped `YOUR STORY STARTS HERE`
  empty state; `BackHandler` layered so back closes the editor, then the screen.
- `ui/diary/DiaryDayHeader.kt` *(new)* — back, prev/next chevrons, day
  eyebrow, `1 January · Tuesday` headline and a memory-position tick row.
- `ui/diary/MemoryTimeline.kt` *(new)* — `MemoryMoment`: 44dp time gutter,
  1dp spine with tapered first/last ends, mood dot, uppercase mood, journal
  text at 28sp leading.
- `ui/diary/DiaryEditor.kt` *(new)* — full-screen composer replacing
  `DiaryEditorSheet.kt` (deleted): mood-first, 8 moods, saving-aware action.
- `ui/diary/DiaryEmptyState.kt`, `MoodSelector.kt`, `MemoryDeleteDialog.kt`,
  `DiaryMotion.kt` *(new)*.
- `ui/diary/DiaryDetailScreen.kt` — route + ViewModel kept; adopts the same
  spine/mood/leading so a memory opened alone still reads as part of its day.
- `ui/diary/DiaryMoods.kt` — data-driven, 8 moods; `ui/theme/Color.kt` —
  mood/neutral tokens for the new palette.

### Deliberately not a Timeline clone
Timeline uses a 2px spine with 30dp paper badges and 24dp-radius cards. Diary
uses a 1dp spine, **no cards**, mood dots and type as the loudest element — the
two surfaces stay visibly different.

### Mood storage — additive, no migration
Mood is a `TEXT` column, so new moods need no schema work. Angry / Anxious /
Tired are added; the five original keys are preserved byte-for-byte and
`fromStored` still resolves them, so existing entries keep their mood.
`DiaryMoodsTest` now asserts all 8, plus unique labels/keys and legacy-key
compatibility.

### Bug fixed
`DiaryViewModel.saveEntry` stamped every new entry with `today()`, so writing
while reading a past day saved to today. It now writes to `selectedDay`. Added
an in-flight `saving` guard (reset in a `finally`, so a failed write cannot
wedge the composer) and a `shiftDay` guard refusing to move past today.

### Verification (offline, no network)
`/home/gradle-8.9/bin/gradle --no-daemon testDebugUnitTest assembleDebug lintDebug`
→ **BUILD SUCCESSFUL**; 111 unit tests, 0 failures/errors; lint 0 errors and 0
diary-related issues. Also confirmed: no secrets/logging/network in `ui/diary/`,
`INTERNET` permission still absent, and no diff under `data/`, `core/`,
`ui/navigation/`, `ui/components/`, `androidTest/` or `schemas/`.

### Docs corrected (pre-existing drift, unrelated to this change)
- Diary feature row + UI/UX section described removed Notes/Capture/Search/AI
  directories and a non-existent `core/intelligence/DiaryConnections.kt`.
- Bottom bar documented as four destinations including Insights; it is three
  (Home/Tasks/Habits), locked by `BottomNavItemsTest`.
- Two "Room v2" references should read v4.
- Not fixed (out of scope, still stale): README's Capture/Timeline/Expenses
  sections and the `docs/` historical entries.

## 2026-09-24 — Timeline & Diary visual pass from Stitch (branch `feat/stitch-timeline-diary`)

Follows the "LifeOS Timeline Overview" + "LifeOS Diary / Journal" Stitch
reference screens. Visual layer only — no navigation architecture, Room schema
(v4), repository or use-case changes; the app remains fully offline-first.

### Timeline (`ui/timeline/TimelineScreen.kt`)
- Header replaced with a centered dated headline and back/previous/next
  chevrons; the same Single-day navigation semantics (`selectedDate`,
  `LaunchEffect` reload, `canGoForward` guard) are preserved.
- Entries now render as a dated journal: a 2px `#E8E1EA` hairline spine on a
  fixed 32dp left track, 30dp paper node badges (`DiaryPaperCard`,
  1dp hairline border) carrying each entry's emoji, and 24dp-radius cards with
  title, subtitle and a time pill. First/last entries get tapered spine ends.
- Empty state upgraded to mirror the Diary's (lavender circle + timeline icon
  + title + caption).
- New `onOpenItem: (TimelineItem) -> Unit` callback (default no-op). The Nav
  host routes DIARY → Diary detail, TASK/HABIT/EXPENSE → their existing
  sections; system Back returns to the Timeline.
- `TimelineViewModel` and `BuildTimelineUseCase` untouched.

### Diary (`ui/diary/`)
- New pastel mood tokens in `ui/theme/Color.kt` (`DiaryMood*Pastel`,
  `DiaryHairline` `#E8E1EA`, `DiarySaveDisabled` `#F4ECFF`).
- `DiaryMoods.backgroundOf()` maps the five stored moods to their pastel fill;
  `EntryMoodPill` (list + detail) and the editor's selected mood chips now use
  pastel backgrounds with the saturated accent text. Mood keys persisted are
  unchanged, so existing entries keep their mood.
- Save button restyled as a flat lavender pill (`DiaryLavender` fill,
  `DiaryInkViolet` label, disabled `DiarySaveDisabled`, zero elevation).
- Day-strip chips: 16dp capsules, unselected bordered by the `#E8E1EA` hairline;
  selected still fills `DiaryLavender`.

### Database
- None. Room schema stays v2; `diary_entries` and all readers untouched.

### Verification (offline, deps cached)
- `gradle :app:testDebugUnitTest` — 108 tests, 0 failures.
- `gradle :app:assembleDebug` — BUILD SUCCESSFUL.
- `gradle :app:lintDebug` — 0 errors; 23 pre-existing warnings, none in changed
  files.

---

## 2026-09-24 — Coalesced, reliable alarms: one intelligent alarm per trigger time (branch `feat/reliable-coalesced-alarms`)

### Root causes fixed
1. **Doze per-app alarm limit.** The old scheduler armed one `AlarmManager`
   alarm per reminder (`lifeos://reminder/<id>`). Doze allows each app only
   ~once per 9 minutes of exact delivery across *all* its alarms, so beyond a
   handful of reminders the extras stopped firing — matching the "stops after
   5–6 reminders" symptoms.
2. **Delivery depended on the app being open.** A reminder delivered only a
   notification + a best-effort `startActivity`; in the background/locked/Doze
   states the screen could never show and the cold SQLCipher open inside
   `goAsync()` could exceed the broadcast window.
3. **No coalescing.** Two reminders at the same time produced two alarms →
   overlapping alarm UIs + double audio.

### What changed
- **`AlarmEvent` + `AlarmEventProjector` (new, pure).** The reminders table is
  now projected into one *event per distinct trigger time* (snooze returns
  override the real trigger; future-only; events sorted by time, items by id;
  **no caps** — the number of real alarms equals distinct trigger times, never
  the number of reminders). `id` = `event:<triggerAt>`.
- **`AlarmEventCodec` + `AlarmIntents` (new).** The full event snapshot travels
  inside each PendingIntent/Activity intent (trigger time, item ids/titles), so
  the alarm presents **instantly from extras with zero DB reads**. Stable
  identities make re-projection cancel/refresh idempotent (`FLAG_UPDATE_CURRENT`).
- **`ReminderScheduler` (rewritten).** One alarm per event via the
  exact-when-permitted ladder: `setAlarmClock` (Doze-exempt, no 9-minute limit,
  FGS-start allowed) when exact access is granted or API < 31, else
  `setAndAllowWhileIdle` (permission-free, never throws). `reproject()` diffs
  against a `scheduled_event_keys` preference set, cancels what left the set and
  re-arms the rest.
- **`AlarmPlaybackService` (new).** A `mediaPlayback` foreground service that
  *owns the audible alarm*: looping `TYPE_ALARM` tone + vibration + partial
  wakelock, plus ONE high-priority notification with a full-screen intent and
  STOP/Snooze actions. Started from the alarm broadcast (platform-permitted for
  `setAlarmClock`, and `mediaPlayback` is a background-startable FGS type), it
  rings even in deep Doze / locked / app-in-background. If `startForeground` is
  refused, it degrades to a single HIGH-importance notification carrying the
  event's own sound/flags. The full-screen activity plays **no sound** — no
  double-ring by construction.
- **`AlarmReceiver` (rewritten).** Decodes the event from extras, starts the
  FGS (fallback notification if refused), then `goAsync()`+IO runs
  `handleEventFired(ids)` and re-projects. Presentation never waits on the DB.
- **`AlarmFullScreenActivity` (rewritten, Compose/Material 3).** Dark LifeOS
  alarm screen: "TASK TIME"/"HABIT TIME"/"IT'S TIME" (existing string keys
  re-valued), scheduled time, grouped TASKS/HABITS sections, prominent STOP,
  snooze 5/10/15/20/60 with a Tasks/Habits/Both target selector when an event
  mixes types (per-type snooze). `setShowWhenLocked`/`turnScreenOn`/keep-screen-
  on preserved; `noHistory` + `singleTop` + CLEAR_TOP prevents stacked alarm UIs.
- **`ReminderRepository` (single `reprojectAll()` funnel).** Every mutation —
  create/edit/delete/fire/snooze/rebuild/toggle — ends in one projection pass;
  `normalizePastDue()` fast-forwards recurring reminders past missed windows and
  disables dead one-shots, `handleEventFired` advances the per-fire state, and
  `snoozeMany` supports per-type snooze.
- **`BootReceiver`:** TIME_SET / TIMEZONE_CHANGED now rebuilds the reminders
  table from the task/habit `reminderEpochMillis` wall-clock mirrors
  (`rebuildFromMirrors`) so recurring reminders stay pinned to local time.
- **Habits parity:** the habit add-dialog and habit-detail Reminder card now
  gate timed alarms behind `ExactAlarmPermissionHost` (SCHEDULE_EXACT_ALARM)
  chained after the notification-permission host — same care as tasks.
- **Manifest:** added `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`,
  `WAKE_LOCK` and the `mediaPlayback` FGS service declaration.
- **strings.xml:** kept the existing alarm keys (re-valued titles), added
  snooze-target, section, snooze-action and multi-item strings.

### Database
No Room schema/migration change. DB stays at version 4; `reminders` table and
all retained tables are untouched.

### Verification (offline, deps cached)
```text
gradle :app:testDebugUnitTest  -> BUILD SUCCESSFUL (108 tests, 0 failures
                                     incl. 9 new AlarmEventProjectorTest)
gradle :app:assembleDebug      -> BUILD SUCCESSFUL
gradle :app:lintDebug          -> BUILD SUCCESSFUL (0 errors; only pre-existing
                                     dependency/Info warnings)
```
On-device checks still recommended (no emulator here): an actual alarm firing
in Doze, lock-screen FSI/heads-up, per-type snooze, reboot/time-zone re-arm.

---
---

## 2026-09-23 — Exact-alarm permission flow (SCHEDULE_EXACT_ALARM), shared & permission-safe scheduler

Task and habit reminders have always used the permission-free, Doze-aware
inexact `AlarmManager` API, which is reliable but delivers reminders only when
the system batches them. This pass adds Android 12+ special access so timed
task reminders can fire at the exact scheduled minute.

### What changed
- **Manifest** (`AndroidManifest.xml`): declared `SCHEDULE_EXACT_ALARM`. This is
  a *special access* the user must enable on the system "Alarms & reminders"
  page — never requested at launch, and its absence can never crash or throw.
- **`ReminderScheduler`** (shared by tasks and habits): when exact scheduling
  is granted it uses `setExactAndAllowWhileIdle`; otherwise it falls back to the
  existing permission-free `setAndAllowWhileIdle`. Both are Doze-aware; a
  `SecurityException` can never propagate, so nothing throws and no reminder is
  left as a silent zombie. Habits keep their existing ungated flow unchanged.
- **`PermissionManager`**: added `openExactAlarmSettings(context)` → system
  `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` page (fallback to app details on old
  Android).
- **`ExactAlarmPermissionHost`** (new, `ui/components`): mirrors
  `ReminderPermissionHost`. Shows an explanatory dialog with "Open Settings" /
  "Not now" and re-checks the grant on every `ON_RESUME` (returning from the
  system page), releasing the held action only once the grant is present — it
  never claims "reminder scheduled" while access is still denied.
- **`SettingsScreen`**: added an "Alarms & reminders" row that reads the *live*
  system grant (allowed / not allowed / not required on API < 31) and deep-links
  to the exact-alarm system page. No fake in-app switch.
- **`TasksScreen`**: the task-reminder save is gated behind exact-alarm access
  (via `ExactAlarmPermissionHost`); habits call it untouched. `POST_NOTIFICATIONS`
  permission remains required for notifications.
- **`strings.xml`**: added the settings-row and `exact_alarm_dialog_*` strings.

### Database
No Room schema/migration change. DB stays at version 4; `reminders` table
unchanged; no columns dropped or added.

### Verification (offline, deps cached)
```text
gradle :app:compileDebugKotlin -> BUILD SUCCESSFUL
:app:testDebugUnitTest        -> BUILD SUCCESSFUL (12 suites, 99 tests, 0 failures)
:app:assembleDebug            -> BUILD SUCCESSFUL (APK + dex packaged)
:app:lintDebug                -> 0 errors, 0 warnings
```
Exact-alarm scheduling is Android-version-conditional (only lives on API 31+, is
granted by the user on the system page, and only ever *armed* on-task-save), so
it is not exercised by the current on-device scenario tests.

## 2026-09-18 — Audit hardening pass (P0 data safety, correctness, polish)

### P0: encrypted-database key handling is now non-destructive (`DatabasePassphraseProvider.kt`)
- **Before:** if the Android Keystore key/wrapped passphrase was missing or unreadable while a database file already existed, startup could generate a *new* passphrase. That made the existing encrypted database permanently unreadable — silent data loss.
- **After:** a pure decision table (`resolveAction`) encodes the contract: healthy install → **REUSE**; fresh install (no wrapped key, no DB file) → **CREATE_FRESH**; anything ambiguous (unreadable wrapped key, or existing DB with no wrapped key) → **FAIL_KEY_UNAVAILABLE**. The passphrase is never rotated and the database is never deleted. Failures throw `DatabaseKeyUnavailableException`, surfaced by a dedicated, non-destructive recovery screen.
- `AppDatabase` now opens `DatabasePassphraseProvider.DATABASE_NAME` so the provider and Room agree on one file name.

### Startup resilience
- `LifeOSApplication` now exposes a nullable `serviceLocator` plus `initializationError` and `retryInitialization()`; `ServiceLocator.get` is wrapped in `runCatching`.
- `MainActivity` gates on this: a locked/failed DB key shows `DataKeyErrorScreen` (Retry / Exit) instead of crashing or wiping data.

### App Lock (4-digit PIN + auto-lock)
- New `PinComponents` (`PinDots`, `PinKeypad`, `PinKeypadPanel`) with haptics and accessibility semantics.
- `AppLockSetupScreen` / `AppLockScreen` rewritten for an exactly-4-digit keypad with auto-advance and auto-submit; lockout countdown and Forgot-PIN recovery preserved.
- Auto-lock: `AUTO_LOCK_GRACE_MILLIS = 30s`; locking on `ON_STOP` and re-prompting after the grace period, with an "Auto-lock" toggle in Settings.

### Profile
- New `ProfileAvatar` (Coil `AsyncImage` with Person fallback). `ProfileScreen` supports a persisted profile photo (`OpenDocument` + `takePersistableUriPermission`), editable display name, App Lock/Backup/Settings rows, and an About dialog. Home header uses the avatar.

### Search
- `SearchScreen` rewritten with `SearchCategory`, `SearchHit`, debounced (300ms) querying, tappable results, and category-aware navigation. Fixes a compile error by using `NoteEntity.plainTextForSearch`.

### Dates & Timeline
- `DateTimeUtils.startOfLocalDayMillis` / `endOfLocalDayMillis` / `nowMinutesOfDay` added. `BuildTimelineUseCase` now uses **local** day boundaries (previously `epochDay * 86_400_000L`, which shifted users off UTC into the wrong calendar day). Timeline caps navigation at today and handles system Back.

### Habits
- Streaks are now schedule-aware (`HabitSchedule`, `isScheduled`, `parseCustomDays`): a Mon/Wed/Fri habit is no longer penalised for missing Tuesday, and an in-progress today does not break a run. `HabitRepository.computeAnalytics`/`computeHeatmap` use the schedule (off-days show as no-data). Legacy consecutive-day overloads retained.

### Tasks
- **Bug:** enum priorities are persisted as strings, so `ORDER BY priority` sorted alphabetically (HIGH < LOW < MEDIUM). Added an explicit `PRIORITY_ORDER` CASE expression for `observeForDay`/`observeAll`, and `observeOverdue(today, nowMinutes)` now includes due-today-past-time items.

### Notes
- `NotesListViewModel` with filters (All/Favorites/Archived/Trash) and folder chips; per-row actions (pin, favorite, archive, trash/restore, delete-forever).
- `NoteEditorViewModel.applyAiResult()` makes AI suggestions **explicit**: Generate title / convert to checklist / append prose, applied only when the user taps Apply (Copy and Close also available). This removes the earlier auto-apply behavior.

### Media capture
- `AudioCaptureScreen` (15-min cap), `VideoCaptureScreen` (5-min cap), and `CameraCaptureScreen` now handle system Back, bound/unbind the camera provider safely, cap duration, clean up partial files, and expose a discard action.

### Diary
- Removed the dedicated AI drafting path entirely (Section 23): entries are user-written and saved only on explicit Save; added delete confirmation and Back handling.

### Onboarding
- Replaced hardcoded colors with `MaterialTheme.colorScheme` tokens so onboarding follows light/dark theme.

### Reminders
- Global "Task & habit reminders" toggle in Settings is now persisted and enforced: turning it off cancels every scheduled WorkManager job (`ReminderScheduler.cancelAllReminders`, tagged work), turning it on re-registers all future task/habit reminders. The scheduler also self-defers when the flag is off, so a stray call can't re-arm notifications.

### Build hygiene & tests
- `app/schemas/` is no longer gitignored so the exported Room schema (`1.json`) is version-controlled.
- New/expanded unit tests: `DatabasePassphraseProviderTest` (data-safety decision table), schedule-aware `HabitStatsCalculatorTest`, and local-day-boundary `DateTimeUtilsTest`. **81 tests pass.**

### Verification
```text
gradle :app:compileDebugKotlin
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
```

## What changed

### Navigation (root cause fix)
- **Before:** a global `BackHandler` in `LifeOSNavHost` intercepted every back press and popped to whatever the single NavController happened to be on. Combined with a flat route list, going Home → Notes and back could leave stale entries on the stack. Home → Notes → Home did not work via the UI.
- **After:** the global `BackHandler` is gone. The four primary destinations (Home, Tasks, Habits, Insights) live in one nested graph (`root_tabs`) with `saveState`/`restoreState`/`launchSingleTop` on bottom-bar taps. Child screens (Notes, Note Editor, Habit Detail, Expenses, Diary, Timeline, Capture Detail, Search, AI Assistant, Profile, Settings, App Lock Setup) are registered outside the graph, so they stack on top of the active tab and system Back dismisses them one level at a time.

### Bottom navigation (`LifeOSBottomBar.kt`)
- Order is now **Home, Tasks, Habits, Insights** (was Home, Habits, Tasks, Insights).
- Labels are **always visible** (previously the label only appeared for the selected item).
- Selected state: animated lavender pill (`secondaryContainer`) with primary icon+text; unselected: neutral `onSurfaceVariant` icon+label.
- 48dp minimum touch target, press-scale animation, tab semantics (`Role.Tab` + `selected`).
- Removed the dead legacy `LifeOSBottomBar(selected, onSelect)` overload from `LifeOSDesignComponents.kt`.

### Home screen (`HomeScreen.kt`)
- **Root-cause fix for the large empty gap:** the decorative gradient was a `Box(Modifier.size(210.dp))` that forced the greeting block to 210dp+; it now uses `matchParentSize()` behind real content.
- Rebuilt header (LifeOS • Today + date, search/settings/profile actions), greeting, Daily Momentum with animated progress, quick-action row, Today's Priorities, habits streak, Recent Activity, LifeOS Intelligence, and a Capture entry point.

### Insights screen (`InsightsScreen.kt`)
- Cognitive Vitality circular indicator is properly sized (was 104dp before, text overlapped the dial), with typography hierarchy and an honest empty state.
- Insight stat cards use a Column (icon/value/label) so values never overlap.
- Pattern Intelligence is clearly presented as the **local** engine; Weekly Narrative now renders its extracted score in a dedicated row ("N / 100").
- Added `internal fun narrativeScore(narrative): Int` that parses both `score N/100` and `N/100` formats (falls back to 0).

### Build hygiene
- `assembleDebug` passes; `testDebugUnitTest` passes (64 tests); `lintDebug` reports **0 errors**.
- Fixed pre-existing lint errors outside the redesign scope (no behavior change):
  - `NotificationHelper`: wrap `notify()` in `SecurityException` handling (Android 13+ permission race) and drop an obsolete `SDK_INT >= O` branch (minSdk is 26).
  - `AndroidManifest.xml`: declared `android.hardware.camera` as `required="false"` (camera works on camera-less/ChromeOS devices).
  - `VideoCaptureScreen`, `MediaPreviewUtils`, `HabitsScreen`, `TimelineScreen`: suppressed the `ProduceStateDoesNotAssignValue` false positives (the lambdas do assign `value`).
- New tests: `BottomNavItemsTest` (order/labels/route uniqueness) and `NarrativeScoreTest` (both score formats + fallbacks).

## Not changed
- All repositories, use cases, ViewModels, DAOs, entities and the Room schema.
- No internet permission, no cloud/AI/telemetry calls; everything remains offline-first.
- README.md and docs/ are preserved (README build-status and snapshot sections were updated).

## Verification
```text
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

## 2026-09-18 — Stitch UI integration + interaction fixes
### Changed
- Integrated the supplied Stitch visual direction into the existing Kotlin/Jetpack Compose implementation for Profile, Expenses, Add Expense, and App Lock.
- Added local profile-photo selection and persistence through `SettingsStore`.
- Removed the Home-screen Settings action; Settings is now intentionally exposed from Profile.
- Kept system Back handling per-screen so secondary destinations consistently return to the previous destination (no global interceptor).
- Changed onboarding startup gating to wait for the persisted completion value before rendering, preventing the onboarding pages from flashing after the first launch.
- Hardened audio capture cleanup, recorder error handling, and local MediaPlayer preparation/playback.

### Preserved
- Existing Room database, repositories, use cases, navigation architecture, offline-first behavior, and app-lock hashing/recovery.
- No Room schema change was required for profile photo storage because the URI is a local DataStore preference.
- No cloud upload, telemetry, remote AI, or new network dependency was introduced.

### Verification
- Source-level audit completed against the supplied latest repository and Stitch HTML/screens.
- Full Gradle verification could not be executed because the supplied repository archive does not contain `gradlew`/`gradle-wrapper.jar`, and no system Gradle executable is available in the execution environment.

### Remaining
- Run the project's normal Android CI/build locally or in GitHub Actions once the wrapper is present to perform the final compiler, unit-test, instrumentation, and lint verification.

---

## 2026-09-19 — Expenses micro UX fix + documentation audit

### Change summary
Two small, additive UX improvements to the existing Expenses screen. No
redesign, no architecture, navigation, schema or business-logic changes.

### Affected files
- `app/src/main/java/com/lifeos/app/ui/expenses/ExpensesScreen.kt`
- `app/src/main/java/com/lifeos/app/ui/components/GlassCard.kt`
- Documentation: `README.md`, `UPDATE.md`, `docs/04_FEATURES.md`,
  `docs/05_DATABASE.md`, `docs/09_FRONTEND.md`, `docs/14_TESTING.md`,
  `docs/16_KNOWN_ISSUES.md`, `docs/17_CHANGELOG.md`, `docs/21_FILE_STRUCTURE.md`,
  `docs/DOCUMENTATION_AUDIT.md`, `docs/00_PROJECT_OVERVIEW.md`,
  `FINAL_RELEASE_CHECKLIST.md`

### Root cause / problem
1. **Sheet opened too low.** `AddExpenseSheet` used a default
   `ModalBottomSheet`, which opens at the *partially expanded* state; users had
   to drag the sheet upward to see the whole Add Expense form.
2. **Unclear category selection.** Category `GlassChip`s had no selected
   visual; selection was only indicated by the "Selected: <name>" text line.

### Implementation
1. **CHANGE #1 — expanded-by-default sheet.** In `AddExpenseSheet`, the
   existing `ModalBottomSheet` now receives
   `sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)`
   (existing Material 3 API) so it opens fully expanded. The form `Column` gained
   `verticalScroll(rememberScrollState())` so every field/button stays reachable
   when available height shrinks (IME, landscape, font scaling). Shape, drag
   handle, background, fields, category UI, buttons, swipe-to-dismiss and Back
   behavior are unchanged. No dialog, new screen, `Box` or hardcoded offsets
   were introduced.
2. **CHANGE #2 — selected category visual state.** The reusable `GlassChip`
   (`ui/components/GlassCard.kt`) gained an optional `selected: Boolean = false`
   parameter. When selected it uses `MaterialTheme.colorScheme.primary` as the
   container/border with `MaterialTheme.colorScheme.onPrimary` content color;
   otherwise it keeps its original glass surface/border. The Expenses call site
   passes `selected = selectedCategory == cat.name`, reusing the **existing**
   `selectedCategory` state (no second state introduced). Only one chip can be
   selected; selecting another moves the highlight immediately. The existing
   "Selected: <name>" line is retained.

### Preserved
- The entire Expenses screen layout, monthly card, daily average, budget/left
  math, recent transactions, empty state and FAB are untouched.
- `ExpenseRepository`, `ExpenseDao`, `ExpenseEntity`, `ExpenseCategories`,
  navigation, and all other screens are untouched.
- No Room schema/version/migration change.
- No network/cloud/AI/telemetry dependency; data stays on device.

### Verification
Executed in the audit environment with Gradle 8.9 + AGP 8.6.1 (offline; aapt2
override), not just source review:

```text
gradle :app:compileDebugKotlin   -> BUILD SUCCESSFUL
gradle :app:assembleDebug        -> BUILD SUCCESSFUL
gradle :app:testDebugUnitTest    -> BUILD SUCCESSFUL (81 tests, 0 failures)
gradle :app:lintDebug            -> BUILD SUCCESSFUL (0 errors, 4 pre-existing warnings)
```

The repository still does not ship `gradlew`/wrapper JAR, so a locally-installed
Gradle 8.9 distribution was used. Instrumentation tests were not run (no
emulator/device and no `androidTest` source set).

### Remaining
- Device/emulator UI verification of the two Expenses interactions (sheet
  initial position and category highlight) is still recommended — it was
  verified at compile/test/lint level only.
- `docs/16_KNOWN_ISSUES.md` Issue #1 (missing Gradle wrapper scripts) remains
  open.

---

## 2026-09-20 — Home screen redesign (`feat/expenses-micro-ux`)

Home rebuilt in Compose to match the Google Stitch dashboard design while
keeping the LifeOS purple theme and real Room data throughout.

### Home layout (top to bottom)
- **Header**: LifeOS wordmark + logo dot, Search, ProfileAvatar (unchanged flow).
- **Greeting band**: date line (`SATURDAY, 19 SEPTEMBER 2026`), live greeting
  (`DateTimeUtils.greeting()`), and a day-status label derived from completed
  goals ("Day on track" / "Building momentum" / "Fresh start" / "Day starting").
- **Circadian Velocity card**: `$dayProgress%` (minutes-elapsed / 1440, refreshed
  every 60 s), circular bolt progress ring, 4-segment bar ((pct/25), 0..4), and a
  TASKS / HABITS / SPEND stats pill — all real values from today's DAO flows.
- **Focus Now**: highest-priority incomplete task of today (first from
  `observeForDay`, which already sorts by `PRIORITY_ORDER`); when every task is
  done it shows the most recently completed one with a DONE state and a
  one-tap undo. Empty state when no tasks planned.
- **Quick Actions**: Diary / Expense / Timeline tiles routed to existing screens.
- **Habits**: "n/N Active" head + WEEKLY CONSISTENCY row (scheduled-day aware:
  a day counts only when every active habit scheduled on it passed its goal),
  per-habit rows with icon, `{n}d streak` badge, and one-tap check-in/undo that
  persists via `logProgress`/`clearProgress`.
- **Today's Activity**: real timeline (`BuildTimelineUseCase`, today only) with
  typed dot + time pill; empty state otherwise.
- FAB (camera) → Capture coverage, unchanged.

### Data-layer additions
- `DateTimeUtils.dayProgressPercent()`.
- `HabitCompletionDao.observeAllInRange(start, end)` +
  `HabitRepository.observeAllInRange` (weekly consistency window).
- `GetHomeSummaryUseCase` now also consumes `BuildTimelineUseCase` (4th
  dependency, wired in `ServiceLocator`) and exposes `focusTask`,
  `focusTaskIsDone`, `weeklyConsistency: List<DayCheck>`, `weeklyDoneDays`,
  `recentActivity`, plus per-habit `currentStreak/longestStreak/completionPercent`.
- `HomeViewModel`: `toggleTask` kept, `toggleFocusTask` added, `incrementHabit`
  replaced by `toggleHabit(habitId, isDone, goalCount)` (check → log to goal,
  uncheck → clear today's record).

### Verification
```text
gradle :app:compileDebugKotlin   -> BUILD SUCCESSFUL
gradle :app:assembleDebug        -> BUILD SUCCESSFUL
gradle :app:testDebugUnitTest    -> BUILD SUCCESSFUL (81 tests, 0 failures)
gradle :app:lintDebug            -> BUILD SUCCESSFUL (0 errors; only pre-existing
                                     dependency-version warnings)
```

No Room schema change (DB version stays 1), no new navigation stacks, no novel
dependencies, no cloud/AI/analytics; the Screen callback contract of
`HomeScreen` (and therefore `LifeOSNavHost`) is unchanged.

### Remaining
- Device/emulator visual check of the Stitch-to-LifeOS mapping (e.g. Fast Yet
  Fresh / "14 day streak" sample labels in the reference were intentionally not
  reproduced).

---

## 2026-09-20 — Home dashboard, profile photo, capture & navigation hardening

### DAILY UPDATE card (real data, no gestures)
- The speculative 4-segment "CIRCADIAN VELOCITY" card is gone. The reference
  spec shows a single **"TODAY'S PROGRESS" title + one percentage + one bar**,
  so the Home header card is now **DAILY UPDATE**: current percentage
  (`headlineLarge`), one `LinearProgressIndicator`, and the TASKS / HABITS /
  SPEND stat surface (real per-module counts from Room).
- The percentage is real and deterministic: completed tasks + completed habits
  over every task/habit scheduled today (`computeDailyUpdatePercent`), with
  `0` when nothing is planned so a fresh day never claims clock-driven
  progress. It no longer uses the device wall-clock / gesture math
  (`rememberDayProgress` + `DateTimeUtils.dayProgressPercent` removed from
  Home).
- `HomeSummary` swaps `focusTask`/`focusTaskIsDone` for `dailyUpdatePercent`;
  `toggleFocusTask()` removed (the task's real checkbox toggle in TODAY'S TASKS
  replaces it).
- **Fix 11** (focus-empty-state overlap): the Focus-Now card rendered even when
  empty, producing the overlapping text ghost. TODAY'S TASKS omits its section
  entirely when no tasks are scheduled, so there is nothing to overlap; the
  FAB/labels were already clear of the extended FAB. LifeOSSpacing reviewed.

### TODAY'S TASKS (replaces FOCUS NOW)
- Real, live list of today's tasks (checkbox toggles the Room state directly,
  struck-through when done, priority chip + time subtitles). Empty list → no
  badge, no ghost card. Follows the spec's TODAY'S TASKS block.

### Prominent Habits heading
- NEW-section heading now `titleMedium` SemiBold with an "n/N Active" count and
  an "Open all" affordance, matching the spec's TODAY'S HABITS block.

### Profile photo — persistent, cropped, resilient
- **Root cause of the disappearing photo:** the previous flow stored the picked
  image as a *content:// URI* backed by a temporary read grant. Grants expire
  at day change / process kill, and `takePersistableUriPermission` is rejected
  by many providers — so the avatar silently failed.
- **Fix:** picking now runs the photo through an **EXIF-correct downscale**
  (`androidx.exifinterface`), a new **square crop dialog** (Compose-only:
  Canvas preview, scrimmed bands, drag-to-position, corner-handle resize,
  `detectDragGestures`), then saves a JPEG to app-private storage
  `filesDir/profile-photos/profile_<timestamp>.jpg`. `SettingsStore` stores
  that absolute path — it survives day changes, restarts and process death. Old
  `content://` values still render when Coil can load them; on any load failure
  `ProfileAvatar` falls back to the letter avatar (first letter of display
  name) or a Person icon, never a broken image.
- `androidx.exifinterface:1.3.7` added (already on the runtime classpath via
  Coil — no APK growth); this also clears the corresponding lint warning.

### Fix 12 — Voice Capture crash
- `MediaStorage.newAudioFile()` and the `MediaRecorder` constructor ran on the
  UI thread **outside** the try/catch, so a failure (e.g. no space, corrupt
  state) crashed the app instead of showing an error. Both now live inside the
  try; the mic button re-requests permission if it is missing, `start()`/
  `stop()` are re-entrancy-guarded, and every failure path cleans up file +
  recorder and shows the existing error surface. Too-short recording discard
  preserved.

### Fix 13 — Bottom navigation reliability
- Re-tapping the already-visible tab previously *navigated again*, so repeated
  taps could leave duplicate stacks that made the tab appear "dead" (the Home
  case). Re-tap of the visible tab now pops back to the start destination
  instead of duplicating; other tabs keep the canonical
  `popUpTo<saveState> + launchSingleTop + restoreState` pattern. Navigation
  stays on `navigation-compose:2.8.4`.

### Verification
```text
gradle :app:compileDebugKotlin   -> BUILD SUCCESSFUL
gradle :app:assembleDebug        -> BUILD SUCCESSFUL (app-debug.apk built)
gradle test                      -> BUILD SUCCESSFUL (88 unit tests, 0 failures:
                                     81 prior + 7 new DailyUpdatePercentTest)
gradle :app:lintDebug            -> BUILD SUCCESSFUL (0 errors; ExifInterface
                                     warning cleared; only pre-existing Info
                                     autoboxing/Icon/unused-resource items)
```

No Room schema change (DB version stays 1), no nav-dependency change, no
cloud/AI/analytics, no gesture-based progress. `HomeScreen`'s callback contract
(and `LifeOSNavHost`) is unchanged: only the removed `toggleFocusTask` and the
`HomeSummary` field swap touched anything outside Home.

### Remaining
- On-device check of the crop dialog, the avatar persistence after a forced
  stop / device restart, the audio-capture error path, and the re-tap Home
  behavior (no emulator in this environment — verified via compile + JVM tests
  + code reasoning).

---

## 2026-09-20 — Home startup, Home empty states, bottom-nav labels & predictive back

### Fix 14 — Home startup freeze (4–5 s of blank/overlapping Home on cold launch)

Root cause was two-fold, both now fixed without touching architecture, schema
or Room:

1. **SQLCipher cold-open cost landed on the first render.** The encrypted
   database was opened lazily on Room's query executor at the *first* Home
   query — after the UI was already showing. SQLCipher's one-time key
   derivation (PBKDF2) and file open ran right when Home began collecting the
   summary flows, stalling data for several seconds (and making the system
   feel unresponsive to taps/Back during that window).
   - **Fix:** `AppDatabase.warmUpOpen(context, scope)` forces the database open
     on a background dispatcher from `LifeOSApplication.onCreate`, so the
     one-time key derivation overlaps UI setup instead of Home's first query.
2. **`GetHomeSummaryUseCase.assemble()` did full-history analytics per habit.**
   `habitRepo.computeAnalytics(habit, today)` issued one full-history Room query
   per active habit, and `BuildTimelineUseCase` pulled its 7 source streams one
   at a time — all serial, on the collector (main) thread.
   - **Fix:** new `HabitRepository.computeAnalyticsBatch(habits, today)` loads
     the completions table **once** and computes every habit's analytics
     in-memory (same `analyticsFor` math reused by `computeAnalytics`, so detail
     and Home stay consistent). `BuildTimelineUseCase` now fires its source
     queries concurrently (`coroutineScope { async { … } }`). The summary flow
     is `flowOn(Dispatchers.Default)`, so assembly never runs on the main thread.

### Fix 15 — Home empty-state cards overlapped their text (`LifeOSCard` Box root cause)
- `LifeOSCard` renders card children inside a `Box` (press-scale surface); two
  sibling `Text`s passed straight into it stack at the same top-start corner.
  The "No routines yet" (Habits) and "No activity recorded today" (Today's
  Activity) empty states did exactly that, so the title and body overlapped.
  (This was the most visible state right after launch, compounding Fix 14.)
- **Fix:** both Home empty states now wrap their texts in a
  `Column(verticalArrangement = Arrangement.spacedBy(10.dp))` — the same
  pattern `HabitsScreen` already used. The `LifeOSCard` container itself is
  unchanged (other callers rely on `Row`/single-child content).

### Fix 16 — Bottom-nav labels truncated on narrow screens
- The bottom bar rendered icon and label side by side inside a `weight(1f)`
  cell; on ~360dp displays "Insights" clipped to "Insi", "Home" to "Ho", etc.
- **Fix:** each `BottomNavEntry` is now a stacked column (icon above label,
  Material 3 `NavigationBar` direction), centered pill, 22dp icon, ellipsizing
  `labelMedium` text. Labels remain always visible and fully readable on
  standard 360–420dp phones.

### Fix 17 — System Back: verified + predictive-back enabled
- Code audit confirmed every screen's Back path is correct and none swallows
  presses (no global interceptor; nested `root_tabs` graph; per-screen
  `BackHandler`s call their dismiss/save). The perceived unresponsiveness
  traced to the Fix-14 main-thread stall during startup.
- `AndroidManifest.xml` now declares
  `android:enableOnBackInvokedCallback="true"` so Compose Navigation's
  `OnBackInvokedDispatcher` path is active (it was required for the predictive
  back contract at targetSdk 35).

### Audio / capture
- Full pipeline re-audited (`AudioCaptureScreen`, `CaptureSheet` confirmation,
  `CameraCaptureScreen`, `VideoCaptureScreen`, `CaptureMediaPreview`,
  `MediaStorage`, `PermissionManager`, Timeline audio rows, AI voice input).
  No concrete defect found in the current code; correct-by-construction paths
  were left untouched rather than fabricated (Fix 12 on-device check remains
  outstanding).

### Verification
```text
gradle :app:compileDebugKotlin   -> BUILD SUCCESSFUL
gradle :app:testDebugUnitTest    -> BUILD SUCCESSFUL (88 tests, 0 failures)
gradle :app:lintDebug            -> BUILD SUCCESSFUL (0 errors; only pre-existing warnings)
```

No Room schema change (DB version stays 1), no navigation architecture change,
no new dependencies, no cloud/AI/analytics. `HomeScreen`'s callback contract
and `LifeOSNavHost` are unchanged.

### Remaining
- On-device confirmation of the startup freeze elimination (timing), predictive
  back gesture animation, and the bottom-bar label rendering at 320–360dp (no
  emulator/device in this environment — verified via compile + 88 JVM tests +
  lint + code reasoning).

---

## 2026-09-21 — Capture sheet redesign (LifeOS Moment Capture, from the Stitch design project)

Implemented from the single Stitch design screen into `CaptureSheet.kt`'s
capture menu. **Visual-layer only**: navigation, data, persistence, permissions
and the photo/video/audio sub-screens are unchanged.

- **Header:** removed the top-right Close `IconButton` (dismiss now flows
  through system Back, tap-outside, and the "Close → Return to LifeOS" tile).
  Title `headlineMedium` → `displayLarge` (40/48, per measured 82px cap ≈
  27.3dp at 3x); subtitle `bodySmall` → `bodyMedium`, still `onSurfaceVariant`
  (measured ~`#494455`).
- **Quick-thought card:** surface tint `#F7F2F9` (`LifeOSCaptureCardLight`),
  20dp radius, no elevation; "Quick thought" label `titleMedium` →
  `headlineSmall`; input switched from `OutlinedTextField` (visible border) to a
  borderless M3 `TextField` — transparent container, single bottom indicator in
  `outline`, placeholder in `outline`, `bodyMedium`, `minLines = 3` — matching
  the design's borderless entry; "Save thought" `Button` unchanged.
- **Life Capture grid:** section label `titleMedium` → `titleLarge`; tiles use
  the design's lavender `#F1ECFF` (`LifeOSCaptureTileLight`) instead of white
  `surface`, no elevation; labels `titleMedium` → `titleLarge`; subtitles
  `labelSmall` → `bodyMedium`; icon container stays `primaryContainer` 44dp /
  15dp radius with 24dp branded icon.
- **Spacing:** outer side padding 22 → 30dp; card→header gap 28dp (top padding
  12 + 16 spacedBy); tile gap 12dp; title→subtitle 2dp — all from measured
  pixel geometry.
- Content now scrolls (`verticalScroll`) with `imePadding` so the sheet fits
  ~360×640 screens and the keyboard never covers the field.
- **Dark mode:** paired tone tokens added to `Color.kt`
  (`LifeOSCaptureCardDark` `#37323D`, `LifeOSCaptureTileDark` `#2C2844`).
- The punch-hole-era top-right decorative cluster (paper-plane doodle, floating
  "audio" label, ✕ shapes) was judged a Stitch ambient decoration and was
  deliberately **not** reproduced.

### Verification
```text
gradle :app:compileDebugKotlin   -> BUILD SUCCESSFUL
gradle :app:testDebugUnitTest    -> BUILD SUCCESSFUL (176 tests, 0 failures, 0 errors)
gradle :app:assembleDebug        -> BUILD SUCCESSFUL
gradle :app:lintDebug            -> BUILD SUCCESSFUL (0 errors; only pre-existing warnings)
```

No Room schema change (DB version stays 1), no navigation architecture change,
no new dependencies, no cloud/AI/analytics. `LifeOSNavHost` and the sheet's
callback contract are unchanged.

### Remaining
- On-device visual confirmation (no emulator/device in this environment —
  verified via compile + 176 JVM tests + lint + pixel-measurement spec match +
  code reasoning).

---

## 2026-09-21 — Diary (LifeOS Journal) redesign, from the Stitch design project

Implemented natively (Kotlin + Compose) from the Stitch "LifeOS Diary /
Journal" screens into `ui/diary/`. The plan (`tasks/plan.md`) and task list
(`tasks/todo.md`) live under `tasks/`. **Visual and informational layer only**:
Room schema, repositories, analyzers and navigation architecture are
unchanged.

- **Palette & tokens:** paper `#FDF8FF`, card `#FFFCFF`, ink violet `#21005D`,
  lavender `#EADDFF` (with dark-mode pairs) and a per-mood editorial palette —
  Goldenrod (happy), Sage (calm), Indigo (sad), Terracotta (stressed),
  dried-rose (excited) — added to `Color.kt` (`DiaryPaper`, `DiaryPaperCard`,
  `DiaryInkViolet`, `DiaryLavender`, `DiaryMood*`).
- **List screen (`DiaryScreen.kt` rewrite):** editorial header (title
  `headlineSmall`, subtitle `bodySmall`), 60dp round lavender FAB with ink
  plus, day strip (last 14 days, newest-first, All option), mood entry cards
  (24dp radius, date + mood pill, inline Edit/Delete, keyword chips), friendly
  empty state, all analytics below the entries.
- **Composer (`DiaryEditorSheet.kt`):** Dialog + Surface bottom sheet — grab
  handle, mood chip row (5 moods, tapped chip tinted), borderless
  `TextField` ("What happened today?"), Cancel / Save (enabled once non-blank),
  and Delete + date header when editing an existing entry.
- **Details (`DiaryDetailScreen.kt`):** entry-detail route `diary/{entryId}`
  (new `Screen.DiaryDetail` + `LifeOSNavHost` wiring) with editorial
  typography, mood pill, theme keyword chips, the day's connection radar and
  full editing/deleting from the sheet.
- **Local intelligence:** new `LifeOSIntelligenceEngine.diaryInsights(today)`
  (week/month counts, streak, average mood + trend via `TrendAnalyzer`,
  themes via `KeywordExtractor`, patterns via `PatternDetector`, narrative via
  `ReportGenerator.weekly`, recommendations via `CorrelationAnalyzer`) returned
  as a new `DiaryInsights` model with a ready-to-use all-in-one summary string.
  The engine is now exposed by `ServiceLocator` (`val intelligenceEngine`).
- **Connection radar (`DiaryConnections.kt` + `DiaryConnectionsView.kt`):** a
  pure builder turns the selected day's diaries + timeline (via
  `BuildTimelineUseCase`) into a node/edge graph — entry (center), mood and
  keywords (left rail), timeline items (right rail), capped for legibility —
  rendered on `Canvas` with `RadarNodeChip` overlays (DP-based layout). No
  entry on that day shows a friendly note instead of a graph.
- **Analytics section (`DiaryAnalyticsSection.kt`):** expandable cards —
  Summary (week/month/streak), mood bar chart (−2..+2 scale, last 14 days),
  top themes, repeating patterns, weekly narrative, recommendations, and an
  **Ask LifeOS** card that runs the existing offline `LocalQuestionEngine`
  against last week's diary (with follow-up suggestions chips).
- **Tests:** `DiaryConnectionsTest` (8) covering graph shape, mood
  preservation/detection, shared-keyword edges, same-day timeline wiring and
  capping; `DiaryMoodsTest` (5) covering stored-mood equivalence,
  fallback labels and analyzer mapping.

### Verification
```text
gradle :app:compileDebugKotlin   -> BUILD SUCCESSFUL
gradle :app:testDebugUnitTest    -> BUILD SUCCESSFUL (101 tests, 0 failures, 0 errors)
gradle :app:assembleDebug        -> BUILD SUCCESSFUL
gradle :app:lintDebug            -> BUILD SUCCESSFUL (0 errors; only pre-existing warnings)
```

No Room schema change (DB version stays 1), no new dependencies, no
cloud/AI/analytics. Mood values are the existing persisted strings; stored
mood wins, analyzer detection is the fallback (matching previous behaviour).

### Remaining
- On-device visual confirmation against the Stitch screens (no
  emulator/device in this environment — verified via compile + 101 JVM tests +
  lint + code reasoning); visual QA can't render screenshots here.

---

## 2026-09-23 — Remove Notes, Capture, LIFE AI, Intelligence, Insights & Search (branch `feat/remove-notes-capture-life-intelligence`)

### Scope removed
- **Notes & Capture:** Notes list/editor, Photo/Video/Audio capture (CameraX), Capture detail, and their sheets (FAB from Home) are gone. `core/ai`, `core/intelligence`, `core/life` (incl. `LifeModels`, `LifeVoiceInputController`) and the `ui/ai`, `ui/capture`, `ui/notes`, `ui/search`, `ui/insights` trees were deleted.
- **Insights / Statistics / Connections / Search:** the Insights bottom-nav tab, Diary analytics & connection-radar sections, the Search screen, and the "Weekly Rhythm" home/insights card were removed.
- **LIFE AI Assistant & settings:** the AI assistant screen, offline intelligence engine wiring, AI features toggle and "Privacy & Local Intelligence" settings section were removed.
- **Permissions/resources:** CAMERA / RECORD_AUDIO permissions, camera `uses-feature`, the captures FileProvider path, and the CameraX Gradle dependency were removed. Tagline now reads "Organize your life. Understand your day."

### Room migration v2 → v3 (non-destructive)
- `AppDatabase` is now version **3** with `MIGRATION_2_3`, registered alongside `MIGRATION_1_2`. No `fallbackToDestructiveMigration`.
- The migration drops **only** the removed features' tables — `notes` and `captures` — and touches nothing else. All retained tables (`tasks`, `habits`, `habit_completions`, `expenses`, `diary_entries`) keep every column and row.
- Exported schema `app/schemas/.../3.json` now contains exactly the five retained tables (KSP `exportSchema=true`).
- **Tests:** a JVM `AppDatabaseSchemaTest` asserts the exported 3.json keeps the five retained tables and drops `notes`/`captures` (runs on every unit-test run); an instrumentation `AppDatabaseMigrationTest` (in `app/src/androidTest`) seeds a v2 database with rows in both dropped tables, runs `MIGRATION_2_3` via `MigrationTestHelper` with `validateDroppedTables=true`, and asserts the tables are gone while retained tables survive. The instrumentation test uses the `FrameworkSQLiteOpenHelperFactory` (plain SQLite) so the helper can validate the migration; it compiles via `:app:assembleDebugAndroidTest` but must run on a device/emulator.

### Data preservation
- **Timeline data:** `TaskDao.getCompletedBetween` was re-introduced as a retained-feature query so the Timeline screen still lists completed tasks; `BuildTimelineUseCase` reads from it unchanged.
- **Backups:** `LifeOSBackup` no longer carries `notes`/`captures` arrays, but `CURRENT_FORMAT_VERSION` stays **1** and `ignoreUnknownKeys=true` remains, so older v1 backup files that include those keys still import (legacy keys are simply dropped). `BackupSerializationTest` gained a legacy-decode case.
- Repeated/off-device verification: `assembleDebug`, `testDebugUnitTest`, `lintDebug`, `assembleDebugAndroidTest` all build green (0 lint errors).

### Known issues / notes
- Deleted-feature data (`notes`, `captures`) is dropped on upgrade by design; no export path exists since the features are removed.
- The instrumentation migration test cannot be executed in this environment (no device/emulator); it is compiled and documented, and must be run via `:app:connectedDebugAndroidTest`.

---

## 2026-09-23 — Reliable reminders, startup unlock gate & back-navigation hardening (branch `feat/remove-notes-capture-life-intelligence`)

Three production-fix problems shipped together after the Notes/Capture/LIFE AI removal batch. No architecture rewrite: Compose Navigation + Room stays, everything stays offline-first.

### 1. Reliable task/habit reminders — WorkManager → AlarmManager exact alarms (Room v4)
The old reminder path ran WorkManager jobs, which Doze / app-standby bucketing can defer for minutes (or drop) while the app is backgrounded, so a "now + 2h" reminder could arrive late or never.

- **`reminders` table (Room v4, pure additive).** `ReminderEntity` (`id` = `"task:<id>"`/`"habit:<id>"`, entity type/id, title, `nextTriggerAtEpochMillis`, `repeatType` ONCE/DAILY/WEEKDAYS, `repeatDaysCsv`, `enabled`, sound/vibration flags, `snoozeMinutes`, `snoozeReturnAtEpochMillis`, `updatedAt`) + `ReminderDao`. `MIGRATION_3_4` creates the table and four `index_reminders_*` indexes; no retained table is touched and `fallbackToDestructiveMigration` remains off. Schema `app/schemas/.../4.json` is exported (KSP).
- **`ReminderScheduler` (AlarmManager).** One exact alarm per enabled reminder via `setAlarmClock` (`AlarmManager.AlarmClockInfo`) — Doze-exempt and the strongest priority the platform offers — with a Doze-aware `setAndAllowWhileIdle(RTC_WAKEUP)` fallback when exact-alarm permission is missing on API 31+. Each PendingIntent is an explicit broadcast to `AlarmReceiver` (no intent filter; cannot be triggered by other apps) with a `lifeos://reminder/<id>` data URI + `FLAG_UPDATE_CURRENT|FLAG_IMMUTABLE`, so ids never collide.
- **`ReminderScheduleCalculator`** — pure JVM recurrence: ONCE → no next; DAILY → +24h; WEEKDAYS → next Mon–Fri at the same wall-clock time (custom day CSV honored, defaults `1,2,3,4,5`). Covered by 7 unit tests.
- **State machine (`ReminderRepository`)** owns every transition in one place: a real fire advances DAILY/WEEKDAYS and re-arms or disables ONCE; a snooze echoes the current notification after N minutes *without* moving the next real occurrence; completing/archiving/deleting an entity cancels+removes its row; a global toggle cancels everything or re-arms all still-future reminders; `rebuildFromMirrors` rebuilds the table from the retained `reminderEpochMillis` mirrors on `tasks`/`habits` after backup restore.
- **Delivery.** `AlarmReceiver` runs the fire handler on `goAsync()`+IO, posts a HIGH-importance, CATEGORY_ALARM notification (per-notification sound/vibration flags; channel is silent by default so the sustained in-app ring doesn't double-beep) with a full-screen intent to `AlarmFullScreenActivity` when permitted, and launches that activity when the app is foregrounded. `AlarmFullScreenActivity` (showWhenLocked/turnScreenOn on target-API 27+) plays a looping `TYPE_ALARM` sound, vibes a repeating waveform, and offers STOP + Snooze 5/10/15/30 (slept back to the repository). `BootReceiver` (exported=true) re-arms on BOOT_COMPLETED / MY_PACKAGE_REPLACED / TIME_SET / TIMEZONE_CHANGED; `LifeOSApplication` re-arms at startup as an extra safety net.
- **Wiring.** `TaskRepository`/`HabitRepository` delegate reminder reconciliation to `ReminderRepository` (their `appContext` params and old `rescheduleAllReminders`/`ReminderScheduler.*Reminder` APIs are gone). `BackupRepository.restore` rebuilds reminders after restore. Task add-dialog → "Repeat reminder" selector (Once/Daily/Mon–Fri); Habit add-dialog and Habit-detail Reminder card get the same selector + time picker. Settings reminders toggle now arms/cancels exact alarms and surfaces an "Allow exact alarms" button (API 31+, `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM`).
- **Removed:** `ReminderWorker.kt` and the `androidx.work:work-runtime-ktx` dependency (WorkManager was used nowhere else).

### 2. Startup: encrypted-DB open never blocks the first frame
Previously the SQLCipher cold open ran on Home's first query *after* the UI was shown. Now `LifeOSApplication.onCreate` builds the DI container cheaply (lazy DB), then opens the database on a background coroutine; `databaseState` gates the UI on a lightweight static `BrandSplash` ("Unlocking your data…") until `Ready`, with a non-destructive `DataKeyErrorScreen` + retry path on `Error`. `SettingsStore.warmUp()` pre-caches the DataStore file and notification channel creation is moved off the main thread.

### 3. Profile → Settings system-Back gap closed
`SettingsScreen` was pushed without a Back affordance and the stack could lose its parent. It now renders a back arrow + `onBack` (popping back to Profile), and every secondary navigation in `LifeOSNavHost` uses `launchSingleTop` so repeated taps / back-chain navigation never duplicate destinations.

### Tests
- `ReminderScheduleCalculatorTest` (7: ONCE/DAILY/WEEKDAYS/custom-days/invalid-CSV-defaults/weekend-only).
- `AppDatabaseSchemaTest`: now asserts the exported 4.json — exactly the six retained tables (tasks, habits, habit_completions, expenses, diary_entries, **reminders**), notes/captures still gone, and the full reminders column set via the createSql.
- `AppDatabaseMigrationTest`: added a v3→v4 case (seeds v3, runs `MIGRATION_3_4` with validation, asserts `reminders` + its four indexes are SELECTable).
- `grep` verification (WorkManager, old scheduler APIs, COLUMN_NAME_* in NotificationHelper): no stale callers or hard-coded SQL remain.

### Verification
```text
gradle :app:clean :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
  -> BUILD SUCCESSFUL in ~4m23s
  -> 93 JVM unit tests, 0 failures         (app-debug.apk 41.9 MB)
  -> lintDebug: 0 errors, 22 warnings (pre-existing dependency/Info items only)
  -> assembleDebugAndroidTest: BUILD SUCCESSFUL (compiles; must run on a device)
```

### Remaining
- Rescue path verification is on-device only in this sandbox (`pm install/uninstall`, `am start`/`input`/`settings`/`dumpsys --` all throw `SecurityException: Permission Denial`; only `pm list`/`cmd package list` work), so the following could not be executed here: cold-start timing, the v3→v4 migration running against a real SQLCipher file, an actual alarm firing (Doze, reboot, permission-revoked fallback), full-screen-intent launch, and the Settings exact-alarm system screen. Verified here via compile + 93 JVM tests (incl. schema JSON) + lint + code reasoning; `:app:connectedDebugAndroidTest` covers the injectable parts on a device.

---

## 2026-09-23 — Startup splash, nav re-tap, permission-free reminders (branch `fix/startup-navigation-reminders`)

Follow-up hardening on the same feature branch base. No architecture, Room-schema, repository, navigation-structure or dependency-catalog changes beyond one additive AndroidX module.

### 1. "Unlocking your data…" replaced by the Android native splash
- **Root cause:** `MainActivity.BrandSplash()` was a custom full-screen Compose screen gated on `databaseState != Ready`; on first launch it could stay visible during the SQLCipher open and looked like a hand-rolled loading screen.
- **Fix:** `MainActivity` now calls `installSplashScreen()` before `super.onCreate` and keeps the system splash on screen (`setKeepOnScreenCondition`) only while `DatabaseInit.Initializing`; it dismisses on **Ready or Error**, straight into the real destination (Home / App Lock / Onboarding / DataKeyErrorScreen). `Theme.LifeOS.Splash` (parent `Theme.SplashScreen`, white background, launcher icon, `postSplashScreenTheme` → `Theme.LifeOS`) is applied to MainActivity; the application and `AlarmFullScreenActivity` keep `Theme.LifeOS`. The `BrandSplash` composable and its `Text("Unlocking your data…")` are deleted; a bare background `Surface` safety net (no label) backs the splash dismiss. New dependency: `androidx.core:core-splashscreen:1.2.0`.

### 2. First content no longer waits on reminder re-arming
- **Root cause:** `LifeOSApplication.launchDatabaseOpen` ran `reminderRepository.rebuildAllActive()` *before* setting `databaseState = Ready`, so the UI waited on re-scheduling every alarm.
- **Fix:** `Ready` now flips immediately after `AppDatabase.warmUpOpen`; `rebuildAllActive()` runs after in a non-blocking coroutine (`runCatching`). `BootReceiver` still covers reboot / package-update / time-change re-arming.

### 3. Bottom-bar re-tap no longer redirects to Home
- **Root cause:** the re-tap branch called `popBackStack(findStartDestination().id, false)`, which pops **to Home** no matter which tab was tapped (Habits→Habits landed on Home; same for Tasks), and cascaded into the intermittent Home-icon / Profile→Back→Tasks weirdness.
- **Fix:** every tap now uses one canonical pattern — `navigate(route) { popUpTo(startDest) { saveState = true }; launchSingleTop = true; restoreState = true }`. Re-tapping the visible tab stays on it; Profile → Back returns to the prior tab with the correct item selected. `LifeOSNavHost` structure (single NavHost, nested `root_tabs`) untouched.

### 4. Reminders drop the exact-alarm requirement
- **Root cause:** `ReminderScheduler.schedule()` preferred `setAlarmClock` and branched on `canScheduleExactAlarms()`, demanding `SCHEDULE_EXACT_ALARM` (API 31+ Settings nudge; denied by default on API 34+); Settings surfaced an "Alarms may be delayed · Allow exact alarms" button.
- **Fix:** LifeOS task/habit reminders don't need exact-to-the-minute delivery, so `schedule()` now always uses `alarmManager.setAndAllowWhileIdle(RTC_WAKEUP, triggerAt, pendingIntent)` — Doze-aware, permission-free, never throws for a missing exact-alarm grant, and the existing PendingIntent identity (`lifeos://reminder/<id>`, `FLAG_UPDATE_CURRENT|FLAG_IMMUTABLE`) still guarantees cancel/replace and no duplicates. The Settings exact-alarm launcher + button are removed (POST_NOTIFICATIONS request path and the global reminders toggle are unchanged), and `SCHEDULE_EXACT_ALARM` is gone from the manifest (POST_NOTIFICATIONS, VIBRATE, USE_FULL_SCREEN_INTENT remain). Past-skip, BootReceiver, Room source of truth and `cancelAll` untouched, verified by grep.

### 5. Unrelated UI
- No screen visuals, layouts, navigation structure, permissions flow or repository logic were touched beyond the four root causes above. The floating call/PiP overlay seen in screenshots is external (system) UI and was ignored.

### Files changed
- `app/build.gradle.kts` (add `core-splashscreen` 1.2.0)
- `app/src/main/AndroidManifest.xml` (MainActivity splash theme; drop `SCHEDULE_EXACT_ALARM`)
- `app/src/main/res/values/themes.xml` (add `Theme.LifeOS.Splash`)
- `app/src/main/java/com/lifeos/app/MainActivity.kt` (installSplashScreen + keep-on-screen; remove `BrandSplash`)
- `app/src/main/java/com/lifeos/app/LifeOSApplication.kt` (Ready-first, async re-arm)
- `app/src/main/java/com/lifeos/app/ui/components/LifeOSBottomBar.kt` (canonical navigate)
- `app/src/main/java/com/lifeos/app/core/reminders/ReminderScheduler.kt` (permission-free `setAndAllowWhileIdle`)
- `app/src/main/java/com/lifeos/app/ui/settings/SettingsScreen.kt` (remove exact-alarm launcher/button)
- `UPDATE.md`, plan `docs/superpowers/plans/2026-09-23-startup-navigation-reminders.md`

### Verification
```text
gradle :app:compileDebugKotlin   -> BUILD SUCCESSFUL
gradle :app:testDebugUnitTest    -> BUILD SUCCESSFUL (93 tests, 0 failures)
gradle :app:assembleDebug        -> BUILD SUCCESSFUL
gradle :app:lintDebug            -> BUILD SUCCESSFUL (0 errors; only pre-existing
                                     dependency-version / Info warnings)
```

### Remaining
- On-device: native-splash→Home transition timing, re-tap stays-on-tab behaviour, Profile→Back tab sync, and an actual reminder firing without exact-alarm access (no emulator/device here — verified via compile + 93 JVM tests + lint + code reasoning).

## 2026-09-23 — Back-nav fix on overlays, POST_NOTIFICATIONS gate, task-reminder parity & zombie-reminder normalization (branch `fix/startup-navigation-reminders`)

Completes this branch's hardening. No Room-schema (v4), entity, migration, repository-API or dependency-catalog changes.

### 1. Back navigation from a tab tapped on an overlay (Home → Profile → Habits → Back → Profile → Back → Home)
- **Root cause:** the previous fix made every bottom-bar tap the one canonical pattern — `navigate(route) { popUpTo(findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true }`, which is *always wrong when the current destination lives outside the tabs graph*. Tapping a tab while Profile/Habit Detail/Expenses is stacked on top **pops the overlay off the stack** (because `popUpTo` reaches past it to the tabs' start destination), so system Back from the tab returned to Home instead of Profile, and the overlay's saved state could ghost a duplicate.
- **Fix:** `LifeOSBottomBar` is now overlay-aware. `isOnTab` checks the visible destination's `hierarchy` for the nested tabs graph (`ROOT_TABS_GRAPH`, promoted from a private const to `internal const val` in `Screen.kt`, same package as the NavHost):
  - inside the tabs → the canonical switch is preserved unchanged;
  - on an overlay → the tab is **pushed on top** (no `popUpTo`), deduping first via `popBackStack(tabRoute, false)` so an existing tab below the overlay is returned to instead of duplicated; if none exists, `navigate { launchSingleTop = true }`. System Back now unwinds exactly Profile → tab → Profile → Home.

### 2. POST_NOTIFICATIONS is requested when a timed reminder is created — never at startup
- **Root cause:** the permission was only ever requested from the Settings toggle; creating a habit/task reminder on a fresh install silently scheduled a full-screen notification the OS would never deliver — a "broken reminder" the user had to debug via Settings.
- **Fix:** new `ReminderPermissionHost` composable (`ui/components/ReminderPermissionHost.kt`) owns its own request launcher (mirroring `PermissionManager`/`rememberPermissionState` semantics) and only runs the held action on actual OS grant. It is composed inline in the reminder creation/edit paths — `NewTaskDialog` Add, Habits add-habit confirm, `HabitDetailScreen.ReminderCard` (time + repeat-change arms), and the new task reminder editor — never at startup. API<33 or already granted → runs immediately; denied → an explanation AlertDialog ("Allow notifications?") holds the action until grant (or drops it); permanently denied → "Open Settings" via `PermissionManager.openAppSettings`. Clearing a reminder stays ungated. The Settings RemindersCard now routes a permanently-denied toggle to system Settings instead of a silent `request()`.

### 3. Task reminders reach full parity with habit reminders
- **Root cause:** habits get a dedicated reminder card in their detail screen; tasks could only set/remove a reminder at creation time — no edit, no repeat change, no way to see the alarm cadence from the task itself.
- **Fix:** `TaskRepository.setReminder(taskId, epochMillis, repeatType)` + `getReminderFor(taskId)` (mirrors `HabitRepository.setReminder`, sharing `syncReminderForTask` — same stable row id, so edit/clear replaces rather than duplicates), `TasksViewModel.updateReminder`/`reminderRepeatFor`, a tappable "Remind at …" chip on any task with a reminder, and `TaskReminderEditorDialog` (time picker + repeat selector + Save/Clear, insets-safe like `NewTaskDialog`), gated by the permission host when arming.

### 4. Past-due ("zombie") reminders normalize instead of silently never firing
- **Root cause:** `syncReminderForEntity` / `rebuildAllActive` / `rebuildFromMirrors` silently skipped rows whose trigger was already in the past, leaving them *enabled forever* with an alarm that could never fire — e.g. a DAILY reminder set after today's time would never trigger today's notification and stayed armed-forever-dormant rows in the DB.
- **Fix:** one arming path, `ReminderRepository.armNextOccurrence`, with a pure helper `ReminderScheduleCalculator.nextFutureOccurrenceMillis` (already-future → as-is; past DAILY/WEEKDAYS → fast-forward to the next future occurrence, persisted via `advanceTrigger`; past ONCE → disabled + alarm cancelled). All lifecycle arming now flows through it: create/edit sync, `rebuildAllActive` (boot/toggle/time-change), `rebuildFromMirrors` (backup restore / v4 upgrade), and `handleFired`'s recurring branch (which previously did a bare `rearmIfFuture`).

### Files changed
- `app/src/main/java/com/lifeos/app/ui/navigation/Screen.kt` (`ROOT_TABS_GRAPH` internal const)
- `app/src/main/java/com/lifeos/app/ui/navigation/LifeOSNavHost.kt` (reference shared const)
- `app/src/main/java/com/lifeos/app/ui/components/LifeOSBottomBar.kt` (overlay-aware tab tap)
- `app/src/main/java/com/lifeos/app/ui/components/ReminderPermissionHost.kt` (new)
- `app/src/main/java/com/lifeos/app/ui/tasks/TasksScreen.kt` (NewTaskDialog gate, TaskCard reminder chip, `TaskReminderEditorDialog`, VM `updateReminder`/`reminderRepeatFor`)
- `app/src/main/java/com/lifeos/app/data/repository/TaskRepository.kt` (`setReminder`/`getReminderFor`)
- `app/src/main/java/com/lifeos/app/ui/habits/HabitsScreen.kt` (add-habit gate)
- `app/src/main/java/com/lifeos/app/ui/habits/HabitDetailScreen.kt` (ReminderCard arms gated)
- `app/src/main/java/com/lifeos/app/ui/settings/SettingsScreen.kt` (permanently-denied → open Settings)
- `app/src/main/java/com/lifeos/app/core/reminders/ReminderScheduleCalculator.kt` (`nextFutureOccurrenceMillis`)
- `app/src/main/java/com/lifeos/app/data/repository/ReminderRepository.kt` (`armNextOccurrence`; rewire all arming)
- `app/src/test/java/com/lifeos/app/core/reminders/ReminderScheduleCalculatorTest.kt` (+6 fast-forward cases)
- `UPDATE.md`, plan `docs/superpowers/plans/2026-09-23-task-reminder-navigation-permissions.md`

### Verification
```text
gradle :app:compileDebugKotlin   -> BUILD SUCCESSFUL
gradle :app:assembleDebug        -> BUILD SUCCESSFUL
gradle :app:testDebugUnitTest    -> BUILD SUCCESSFUL (99 tests, 0 failures)
gradle :app:lintDebug            -> BUILD SUCCESSFUL (0 errors; only pre-existing warnings)
```

### Remaining
- On-device: overlay back-stack behaviour, the permission dialog lifecycle, and a real notification firing at the set time (no emulator/device in this sandbox — verified via compile + 99 JVM tests + lint + code reasoning).
 

## 2026-09-26 — Diary UI v2 reference pass

- Refined the full-screen Diary composer writing surface in `ui/diary/DiaryEditor.kt` to make the empty writing state calmer and more intentional: headline-style placeholder copy plus a quieter supporting prompt, while preserving the existing Room/ViewModel flow, keyboard-safe insets, mood selector, captured timestamp, save guard and Back behavior.
- No Room schema, navigation, repositories, permissions, network, AI, or dependency changes.
- Verification pending: UI screenshot/emulator validation is still required because the connected GitHub environment does not expose an Android emulator/device runner.
