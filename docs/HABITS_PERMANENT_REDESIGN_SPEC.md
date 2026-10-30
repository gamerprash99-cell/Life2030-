# LifeOS — Permanent Habits Experience Specification

**Status:** Implementation brief; not yet implemented by this document.
**Reference assets:** user-provided Home screenshot `1000275963.jpg` and habit-tracker video `1000275962.mp4` (72.7 seconds).
**Inspected baseline:** `main` at `8fe220c70c2c7e8cf13f92b011f363ec2084398f`, 2026-10-31.

## 1. Goal

Make **Habits a permanent primary destination** with a complete, polished experience inspired by the supplied video. Preserve the existing Kotlin + Jetpack Compose + Material 3 + Room + SQLCipher architecture. Extend the current Habit feature; do not create a second habit subsystem.

Required bottom navigation:

**Home · Notes · ＋ Capture · Habits · Life**

- Capture remains the raised centre action, not a selectable tab.
- Habits becomes a stateful root tab and is one tap away.
- Profile, Settings, App Lock and backup/restore must remain reachable (e.g. Home avatar/Profile and existing Settings routes) after Profile leaves the bottom bar.
- Insights must not be a user-facing tab, screen, route or stale navigation destination. In the inspected main snapshot, `Screen.kt` and `LifeOSNavHost.kt` already have no Insights route, and the current bottom bar is Home / Notes / Life / You. Verify the complete checkout before changing anything.
- Do not delete local Intelligence Engine/analyzers/statistics that are still used by Home, LifeOS AI, reports, etc. Remove only obsolete Insights UI/navigation references after reachability checks.

## 2. Visual reference contract

### LifeOS identity (from the user's Home screenshot)
Preserve LifeOS's calm, premium, lavender/purple visual identity: pale lavender canvas, iris-purple gradient hero, dark readable typography, pastel quick-action surfaces, generous spacing, large rounded cards and a prominent raised Capture action. The project direction supplied by the user also references `#FDF8FF`, `#EADDFF`, `#21005D`, and `#F3EDF7`.

The video is the **interaction/layout reference**, not permission to copy its black/yellow theme. Reproduce its hierarchy, setup flow, date selector, filters, score, cards, activity grid, tracking modes and celebration using LifeOS's own live design tokens. Before touching shared theme code, inspect the actual checked-out `Color.kt` and reconcile it with the screenshot and project tokens. The inspected main code labels Color.kt “LUMEN” (warm clay/sage/amber), while `docs/CURRENT_PRODUCT_UI_CONTRACT.md` describes Aurora Noir. Do not silently re-theme the entire app; record and resolve this discrepancy.

### Video observations
The 72.7-second video shows:
1. First-habit intro/onboarding, habit title, theme customization (color palette + icon grid), frequency selection, optional reminder/time, review/continue.
2. Dashboard header with selected date, achievement/share/settings affordances.
3. Horizontal seven-day date strip; selected day gets a strong ring.
4. All / Due filters and a Habit Score.
5. Rounded habit cards with icon, title, streak, detail/history action, completion action and compact activity grid.
6. Completion celebration: “Well done”, confetti, medal/achievement, habit name, streak/date and Share.
7. Full Add/Edit form: title, optional description, color, icon, Build a Habit / Quit a Habit, frequency, Tick / Count / Checklist, reminder, category and “Show Streaks on Timeline”.
8. Monthly chronological history with streak-unlocked / ongoing-streak events.

## 3. Permanent Habits dashboard

### Header and date selector
- Clear title (“Habits” / “Today's Habits”) and selected human-readable date.
- Horizontally scrollable seven-day strip; weekday + date, selected ring, accessible selected state and >=48dp touch target.
- Default to device-local Today. Past dates can be inspected and logged if supported; future dates cannot be completed early.
- Date selection updates all cards, due counts, score and progress from Room.
- Use LocalDate/epochDay consistently; avoid UTC-midnight date shifts.

### Summary and filters
- Show animated Habit Score: completed scheduled habits / habits scheduled on selected day.
- Rest days are neutral, not misses. If no habits are scheduled, show a truthful empty state, not 0% as a misleading score.
- Filters: All, Due, Completed; Archived in a secondary menu/filter.
- Counts must be calculated for the selected date from persisted data.
- Helpful empty state with one clear primary action: Add habit.

### Habit card
Reusable Compose component, showing:
- Selected icon in a tinted container; title and optional category.
- Current streak; optional best-streak cue.
- Mode-aware progress: Tick complete state, Count current/goal, Checklist checked/total.
- One obvious completion/control affordance.
- Compact recent-history grid/mini heatmap.
- Card body opens detail; completion controls must not also trigger navigation.
- Edit/Archive/Delete in overflow or contextual menu; destructive actions require confirmation.
- Stable LazyColumn keys (habit ID); preserve scroll and selected date after logging.

### Responsive sizing starting points
Use shared tokens, not fixed coordinates:
- Horizontal screen padding: shared token (typically 20–24dp).
- Section spacing: shared token (typically 16–24dp).
- Card radius: shared large radius (about 20–28dp where consistent).
- Card inner padding: about 16dp.
- Icon tile: about 40–48dp.
- Primary controls: >=48dp touch target.
- Date circles: about 40–48dp; allow horizontal scroll on narrow phones.
- Heatmap cells: about 8–10dp with consistent gaps.
- Typography follows MaterialTheme/shared LifeOS type tokens.
- Respect font scale; nothing clips or sits behind bottom/system bars.

## 4. Create/Edit experience

Support both a guided first-habit flow and a full reusable Add/Edit form. Do not force the wizard for every habit.

### Identity
- Required title with validation; optional description.
- Custom color and icon picker; curated icon grid and optional “More icons” if available locally.
- Live preview of the resulting habit card.

### Type
- **Build a Habit:** success means the desired action was completed.
- **Quit a Habit:** success means the user avoided the undesired action on a scheduled day. Provide a clear “Stayed on track” action and a separate, non-shaming “I slipped” outcome. Never silently treat an unrecorded day as success.
- Persist type and include it in analytics and backup/restore.

### Frequency
Choices:
- Everyday.
- Specific Days of Week (multi-select Monday–Sunday).
- Specific Days of Month (multi-select 1–31; handle shorter months gracefully).
- Some Days Per Period (target count + period unit; sensible default days/week, with monthly option if supported).

Rules:
- Show only selectors relevant to the selected frequency.
- Persist a deterministic schedule representation, not UI labels alone.
- Existing DAILY/WEEKLY/CUSTOM rows retain their exact meaning; do not reinterpret existing WEEKLY records as arbitrary weekdays.
- Unscheduled days are neutral.
- Period denominator counts scheduled occurrences, not all calendar days.

### Tracking
**Tick:** one tap completes selected scheduled day; a second tap may undo with immediate persisted state; one row per habit/date.

**Count:** positive target; increment/decrement and optional direct entry; no negative values; show current/goal; completion at or above target. Do not discard legitimate over-goal progress.

**Checklist:** reusable template items + per-date checked state; complete only when all required items are checked; partial stays partial. Stable item IDs. Editing the template must not rewrite historical checklist results. Add-item flow and empty state required.

### Reminders
- Optional, off by default; local time picker follows device 12/24-hour preference.
- Reuse existing reminder/notification/exact-alarm infrastructure; no parallel scheduler.
- Request permission only when enabling reminders and only if required.
- Denial/restricted/exact-alarm unavailable must have clear fallback; habit saving must work without a reminder.

### Category and Timeline
- Reuse existing category conventions.
- Persist “Show Streaks on Timeline” if not already present.
- Timeline milestones/events derive from real persisted completion transitions; avoid duplicate events on repeated taps.

### Save/edit
- Explicit Save and validation.
- Unsaved Back prompts Keep editing / Discard.
- Edit preserves ID, created timestamp and all historical completions.
- Archive is reversible and preferred to delete.
- Delete confirms and explains history impact. Never silently erase history.

## 5. Detail, streaks and analytics

Habit detail should include:
- Identity, current streak, longest streak.
- Weekly and monthly completion percentage.
- Total successful scheduled days.
- 12-week heatmap and month/year history where practical.
- Legend for missed / partial / completed / exceptional / unscheduled or future.
- Tapping a heatmap day opens its date/progress detail.
- Schedule/reminder controls; Edit, Archive and Delete.
- Chronological milestones if Timeline supports them.

All numbers come from Room. Respect start date and schedule. A scheduled Today still in progress must not prematurely break the previous streak; unscheduled days never break streaks. Current code already contains useful schedule-aware logic in `HabitStatsCalculator`, `HabitRepository.computeAnalytics()` and `computeHeatmap()`; extend and test it.

## 6. Completion celebration and motion

- Completion action animates check/progress immediately and persists through ViewModel/repository.
- Celebrate meaningful transitions (first completion, 7-day streak, new personal best), not every count increment.
- Celebration: “Well done”, habit name, streak/milestone, date, Continue/Close and optional Android Sharesheet.
- Use existing LifeOS motion tokens/spring family; starting target 180–300ms for touch/state transitions, 300–450ms for entrances, short one-shot confetti.
- Animate state changes, progress, selection and card insertion/removal. Avoid continuous animations on every card.
- Respect system animator scale/reduced motion. Haptics only for meaningful events.
- Never block main thread.

## 7. Home integration
- Keep Home hierarchy: Today → Tasks → Habits → Spending → Recent Activity → Intelligence.
- Home can show compact Today's Habits preview with scheduled items, status, streak and See all.
- “See all” opens permanent Habits tab.
- Home and Habits use the same schedule/progress calculation so values never disagree.

## 8. Navigation target and observed baseline

Inspected baseline:
- `LifeOSBottomBar.kt`: Home, Notes, Life, You; Capture is a raised centre action.
- `Screen.Habits` exists but is registered as a stacked module, not a root tab.
- `Screen.Profile` is currently a root tab.
- Inspected `Screen.kt` / `LifeOSNavHost.kt` has no Insights destination.

Target:
- Root tab graph: Home, Notes, Habits, Life.
- Capture remains separate centre action.
- Move Habits into root-tabs graph with save/restore/launchSingleTop semantics.
- Move Profile out of root tabs but keep route and reachable Home/avatar/Settings path.
- Habit Detail remains stacked above Habits; Back returns to Habits with selected date/scroll state intact.
- Preserve all other routes and back-stack behaviour.
- Update `BottomNavItemsTest` to assert Home, Notes, Habits, Life in order; Capture is not a tab.
- Test Home → Habits → Detail → Back and Profile/Settings reachability.

## 9. Existing files to audit before edits

| Path | Responsibility |
|---|---|
| `ui/components/LifeOSBottomBar.kt` | Bottom bar ordering, icon, label, semantics, Capture centre action |
| `ui/navigation/Screen.kt` | Routes |
| `ui/navigation/LifeOSNavHost.kt` | Root graph and overlays |
| `ui/habits/HabitsScreen.kt` | Dashboard + current ViewModel |
| `ui/habits/HabitDetailScreen.kt` | Analytics/history |
| `data/db/entities/HabitEntity.kt` | Persisted habit fields |
| `data/db/dao/HabitDao.kt` | Room queries |
| `data/repository/HabitRepository.kt` | Room-backed operations/analytics |
| `core/util/HabitStatsCalculator.kt` | Pure schedule/streak math |
| `data/db/AppDatabase.kt` | Schema + forward migrations |
| Converter, reminder, backup/restore, Home, Timeline and Intelligence call sites | Compatibility/integration |

Search all references before changing the model. Do not assume this list is exhaustive.

## 10. Database and migration safety

Inspected `AppDatabase.kt` on main declares Room version **6**, SQLCipher, exported schema, and explicit migrations through 5→6. Current `HabitEntity` stores ID, name, icon, category, frequency (DAILY/WEEKLY/CUSTOM), custom weekday CSV, goalCount, reminder time, start date, archive flag and timestamps. `HabitCompletionEntity` stores habit ID, epoch day, progress count and completion timestamp.

The requested modes exceed the current model. Before implementation:
1. Inspect all migrations, schema v6, converters and backup payload/version.
2. Design the smallest normalized extension for type, color, description, tracking mode, month-day/period schedule and checklist templates + daily checklist state.
3. If schema changes are required, increment from the actual checked-out version (expected 6→7 only if still v6) and add a forward-only migration.
4. Never delete/recreate Room DB, use destructive fallback or wipe user data.
5. Preserve every existing habit/completion row. Existing rows get safe defaults (e.g. Build + Tick) without changing history/streaks.
6. Export new schema and add populated-v6 migration tests.
7. Update backup/restore with old-backup compatibility and new-field round-trip.
8. If checklist uses tables, define stable IDs, foreign keys/indices and safe archive/delete semantics.

## 11. Offline, privacy and performance
- Fully offline; no Firebase, remote DB, cloud API/AI, telemetry, analytics SDK or INTERNET permission.
- Room remains source of truth; do not bypass repositories/use cases.
- Share only user-approved minimal summary through Android Sharesheet; never upload habit data.
- Reuse existing permission/reminder helpers.
- Avoid one full-history Room query per habit; batch/range queries.
- Use lifecycle-aware collection consistent with project patterns, stable list keys and cached calculations.
- Avoid heavy dependencies for icons/charts/confetti when Compose APIs suffice.

## 12. Accessibility and device QA
Verify small/large phones, portrait and relevant landscape, 200% font scale, TalkBack labels, >=48dp targets, light/dark contrast, gesture/three-button navigation, IME/keyboard handling, narrow date strip, loading/empty/error/archived/no-scheduled states, reduced motion, local-midnight and time-zone changes.

## 13. Tests and verification

Unit tests:
- Daily, weekday, month-day and N-days-per-period schedules.
- Existing DAILY/WEEKLY/CUSTOM compatibility.
- Streaks across scheduled/unscheduled days; incomplete Today.
- Period/month denominator.
- Count/checklist partial vs complete.
- Build/Quit semantics.
- Duplicate logging/undo idempotency.
- Month lengths/leap years/time zones.
- Empty score; migration defaults; backup round-trip.

Compose/navigation tests:
- Home, Notes, Habits, Life order; Capture remains centre action.
- Home → Habits → Detail → Back.
- Tab state/back-stack preservation.
- Profile/Settings still reachable.
- Add/Edit validation, unsaved prompt, all tracking modes.
- Date/filter/completion/celebration behaviour.
- Large font and scrollable form.

Run (after verifying wrapper in local checkout):
```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:connectedDebugAndroidTest   # only with configured device/emulator
```
The inspected Git tree contains `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` and properties, although README text says the wrapper is absent. Verify actual local checkout. Report exact commands/results; never claim unrun tests passed. Inspect the first real build error.

## 14. Documentation and Git
Before edits: `git status --short`, `git log -5 --oneline`, inspect branch and relevant Kotlin/docs. Preserve uncommitted user work; no reset/clean/force push/direct main edits.

After implementation, update only affected docs with verified facts:
- `README.md`: current navigation, habit capabilities, schema and verified build status.
- `UPDATE.md`: append dated entry; preserve history.
- `docs/17_CHANGELOG.md`: actual changes only.
- `docs/CURRENT_PRODUCT_UI_CONTRACT.md`: update authoritative nav/design only after implementation.
- `docs/04_FEATURES.md`, `docs/05_DATABASE.md`, `docs/09_FRONTEND.md`, `docs/14_TESTING.md`, `docs/21_FILE_STRUCTURE.md`: affected sections.
- `NEXT_SESSION.md`: actual completed files, migration version, tests, PR URL and remaining work.

Then review `git diff --check`, full diff, changed-file list, secrets/network/dependency changes; commit on a feature branch, push, open PR to main and do not merge unless asked. Never ask the user to paste a PAT into chat; use configured GitHub auth or local credential manager.

## 15. Definition of done
Habits is a permanent primary tab; Capture remains the centre action; Profile/Settings/App Lock/backup remain accessible; Insights UI/routes are absent; local Intelligence code remains where used; dashboard/setup/edit/tracking/reminders/detail/heatmap/celebration are functional; all metrics are real and schedule-aware; existing data survives migration; backup compatibility works; Home/Timeline/navigation remain correct; tests/build/lint are honestly reported; relevant docs and NEXT_SESSION are current; reviewable PR exists and main is untouched.

**Claude Code:** Work in small verified phases. Audit first, report the actual checkout and file plan, implement the smallest safe vertical slice, test, then continue. Do not rewrite architecture or create a second habit subsystem.
