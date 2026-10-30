# Permanent Habits experience — tracker, wizard, dashboard, detail, Room v7

Implements the full permanent Habits experience described in
`docs/HABITS_PERMANENT_REDESIGN_SPEC.md`: Habits becomes a permanent root tab,
the app gets a complete habit tracker (dashboard → wizard/form → detail), and
the database moves v6 → v7 with an additive migration. No internet permission,
cloud, account or telemetry was added; everything remains SQLCipher-backed and
offline.

## Navigation

- Bottom bar is exactly **Home · Notes · ＋ Capture · Habits · Life**
  (`bottomNavItems`, `BottomNavItemsTest` locks the order and proves Profile
  and Insights are not tabs).
- Habits moved into the `root_tabs` graph with saved state; Capture stays the
  raised centre action; Profile remains a first-class stacked destination
  reachable from the Home avatar and the new "Profile & settings" tile on the
  Life hub, with Settings, App Lock and backup unchanged.
- New `habits/edit?habitId={id}&wizard={bool}` route (declared before
  `habits/{habitId}`) powers the guided wizard and the normal add/edit form;
  the detail screen's Edit action navigates there.

## Habits feature

- **Dashboard** (`HabitsScreen.kt`, `HabitsViewModel.kt`): week date strip
  (past days only, "Today" shortcut), All/Due/Completed filters with counts,
  live Habit Score (rest days are explicitly neutral), habit cards with
  streak + streak unit, schedule, Tick/Count/Checklist control, 28-day mini
  heatmap, overflow actions, archived bottom sheet with restore, delete
  confirmation, and a calm celebration overlay with confetti, milestone copy
  and local share.
- **Wizard / editor** (`HabitEditorScreen.kt`): 10-step first-habit wizard
  (name → icon → colour → Build/Quit → frequency → tracking → reminder →
  category/Timeline → review → create) plus the full form for later edits,
  with per-step validation, an unsaved-changes guard and explicit save errors.
- **Detail** (`HabitDetailScreen.kt`): hero + streak, 2×2 stats, day action
  card (tick, count stepper/direct entry, checklist items, Quit "stayed on
  track" and "I slipped"), clickable 12-week heatmap that selects/backfills a
  past day, schedule/reminder card (set/change/remove, repeat type), share,
  archive and delete.
- **Semantics**: Build vs Quit are first-class; a blank day is never silently
  treated as success or failure; unscheduled days never break a streak; N×
  per week/month habits stop being due once their period target is met.
- **Schedules** (`HabitStatsCalculator.kt`): every day, selected weekdays,
  days of month (short months safe), N× per week, N× per month — and
  `LEGACY`, which preserves the exact pre-v7 DAILY/WEEKLY/CUSTOM meaning.
- **Home / Timeline**: Home lists only habits actually due today (per-habit
  period targets, no cross-habit leakage), routes checklist habits to the
  Habits tab instead of faking a tick, and Timeline milestones respect the
  per-habit toggle with honest subtitles.

## Database & data safety

- Room **v6 → v7** (`MIGRATION_6_7`), purely additive over populated data:
  `habits.description/colorArgb/habitType/trackingMode/scheduleType/
  daysOfWeekCsv/daysOfMonthCsv/timesPerPeriod/showOnTimeline`,
  `habit_completions.outcome/targetCount`, and the new
  `habit_checklist_items` / `habit_checklist_checks` tables + indexes.
  `app/schemas/.../7.json` is committed and matches the migration SQL.
- Existing rows keep their meaning: `LEGACY` schedules, `DONE` outcomes,
  `targetCount = 0` ("use current goal"); editing an old CUSTOM habit keeps
  its frequency and weekdays instead of silently becoming DAILY.
- Checklist edits archive template items instead of rewriting history;
  `targetCount` snapshots keep old days honest when a goal later changes.
- `HabitRepository.delete()` now removes completions, checklist rows and the
  reminder together; backup format **v2** adds the checklist tables while v1
  files still restore with empty defaults.
- Reminder reconciliation now honours an explicit repeat change (daily →
  weekdays) instead of silently keeping the stored repeat type.

## Verification (executed)

```text
sh ./gradlew :app:compileDebugKotlin   → BUILD SUCCESSFUL
sh ./gradlew :app:testDebugUnitTest    → BUILD SUCCESSFUL — 486 tests, 0 failures
sh ./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
                                       → BUILD SUCCESSFUL — lint 0 errors / 32 pre-existing warnings
```

AAPT2 is supplied via `-Pandroid.aapt2FromMavenOverride=/opt/aapt2-wrapper/aapt2`
in this environment because the Maven daemon cannot start in the container;
the wrapper is invoked as `sh ./gradlew` (not executable after checkout here).

New/updated tests: `HabitStatsCalculatorTest` (all schedule modes, quota
periods, outcome/target snapshots, 100% ceiling), `AppDatabaseSchemaTest`
(v7, exact 11-table set, new columns), `AppDatabaseMigrationTest`
(`migrate6To7_*` populated-data test — compiled, not executed: no
emulator/device), `BackupSerializationTest` (v2 checklist round-trip),
`BottomNavItemsTest`.

Not executed: `:app:connectedDebugAndroidTest` (no `adb`/emulator/device in
this environment). No instrumentation pass is claimed.

## Manual review still recommended

1. Device pass over the wizard, the three tracking modes, each frequency,
   Quit slip, archive/restore, celebration and reminder repeat change.
2. `connectedDebugAndroidTest` for the populated v6→v7 migration.
3. Reconcile `docs/CURRENT_PRODUCT_UI_CONTRACT.md` §3 with the live Lumen
   palette (the Habits iris accent is intentionally scoped to `ui/habits/`).

## Privacy

No internet permission, account, cloud AI, analytics or telemetry was added.
All habit data stays in the encrypted local Room database, and sharing is an
explicit user action through the Android share sheet.
