# Fix Habit Check-in Undo, Setup UI and Minor Application Bugs

Base: `main` · Head: `fix/habits-undo-and-autonomous-bug-audit`

## Summary

This PR fixes the two reported Habits defects — the oversized blank blocker on
the first habit-wizard step and the inability to reverse an accidental
check-in — and then fixes the additional, evidence-backed defects found by a
project-wide audit of navigation, window insets, save/permission flows and Home
routing.

- Reported bug 1: the wizard no longer stacks the primary tab bar, its own
  Continue bar and a duplicated navigation-bar inset.
- Reported bug 2: check-in undo is explicit (Undo snackbar), day-scoped and
  persisted; Count habits stay correctable after reaching their target.
- Audit fixes: Home recent activity routes to the correct screen; duplicated
  window insets removed from several destinations; declining a notification or
  exact-alarm prompt no longer cancels habit/task creation; the habit editor's
  Back affordances now agree.
- No Room schema, migration, repository contract, user data, dependency,
  permission or network path was changed.

## Problems Fixed

### 1. Habit creation wizard — oversized blank blocker (reported)

- **Before:** step 1 of 10 showed a 150–190dp band of empty chrome around the
  Continue button.
- **Root cause:** `HabitEditorScreen` is composed inside the root
  `LifeOSNavHost` Scaffold, which still reserved the primary bottom-navigation
  bar height for the editor, while the wizard painted its own bar with
  `navigationBarsPadding()`. The tab bar, the wizard bar and the duplicated
  navigation-bar inset stacked.
- **After:** the editor route is excluded from the primary bottom bar and owns
  its bottom inset (like the Diary composer), so the root hands it the full
  window height. The single `navigationBarsPadding()` on the wizard bar is the
  only bottom inset. All 10 steps, scrolling and the unsaved-changes guard are
  unchanged.

### 2. Habit check-in undo (reported)

- **Before:** checking in changed the control to "Done" with no discoverable
  way back; a Count habit's stepper was disabled at the target, so a completed
  count could not be corrected; undo was not day-scoped.
- **After:** `HabitsViewModel` exposes day-scoped `setTickForDay` and
  `undoCheckIn`, with a per-habit/day in-flight guard so repeated taps cannot
  duplicate check-ins or celebrations. The dashboard shows an **Undo** snackbar
  after a successful check-in; the Done control announces "tap to undo"; the
  Count stepper keeps decrement enabled once there is progress (increment stays
  capped at the target).
- **Persistence:** completions remain keyed by `(habitId, dateEpochDay)` in
  the existing Room table; undo deletes only that day's row, every statistic
  recomputes from the existing flows, and the state survives restart.

### 3. Home recent activity opened the wrong screen

Every Home activity row used the Snap-viewer callback, so diary, task, habit
and expense rows opened an empty capture viewer. Added `routeForTimelineItem`
and routed each `TimelineItemType` to its real destination (pinned by
`TimelineNavigationTest`).

### 4. Duplicated window insets

The host Scaffold had already applied the status/navigation insets, but some
destinations applied them again:

- nested Scaffolds in Timeline, Settings and Expenses (now zeroed, matching
  Tasks/Habits/Profile);
- the full-window Note editor, AI assistant and Capture hub (now own their
  insets; the host passes the full window);
- the Snap camera overlays (duplicated padding removed).

### 5. Declining a permission silently cancelled the save

Habit creation and both task flows ran the save action only after notification
/ exact-alarm access was granted, so choosing "Not now" left the form open and
created nothing. Both permission hosts now expose `runProtectedOrContinue`:
the entity is always persisted and the reminder degrades to the scheduler's
permission-free path. `ReminderPermissionHost` also re-checks on resume so the
"Open Settings" route releases the pending action.

### 6. Habit editor Back was inconsistent

System Back moved one wizard step back while the header arrow exited the flow.
Both now share one step-aware handler.

## Files Changed

| File | Change | Reason |
|---|---|---|
| `ui/habits/HabitsViewModel.kt` | Day-scoped tick/undo + in-flight guard | Reported undo bug, repeat-tap safety |
| `ui/habits/HabitsScreen.kt` | Undo snackbar, a11y label, Count stepper | Reported undo bug |
| `ui/habits/HabitEditorScreen.kt` | Own inset, one Back handler, permission-or-continue | Reported wizard bug, save loss, Back |
| `ui/habits/HabitDetailScreen.kt` | Permission-or-continue on reminder saves | Save loss on declined permission |
| `ui/navigation/BottomBarVisibility.kt` | Editor excluded from tab bar; inset ownership rules | Wizard chrome, duplicated insets |
| `ui/navigation/LifeOSNavHost.kt` | Zero the edge a route owns; route activity items | Wizard chrome, Home routing |
| `ui/navigation/Screen.kt` | `routeForTimelineItem` | Home routing |
| `ui/home/HomeScreen.kt` | Per-type activity click callback | Home routing |
| `ui/components/ReminderPermissionHost.kt` | `runProtectedOrContinue` + resume re-check | Save loss on declined permission |
| `ui/components/ExactAlarmPermissionHost.kt` | `runProtectedOrContinue` | Save loss on declined permission |
| `ui/tasks/TasksScreen.kt` | Permission-or-continue on add/save | Save loss on declined permission |
| `ui/timeline/TimelineScreen.kt` | Zero nested Scaffold insets | Duplicated insets |
| `ui/settings/SettingsScreen.kt` | Zero nested Scaffold insets | Duplicated insets |
| `ui/expenses/ExpensesScreen.kt` | Zero nested Scaffold insets | Duplicated insets |
| `ui/capture/CameraCaptureScreen.kt` | Remove duplicate camera-overlay insets | Duplicated insets |
| `ui/capture/VideoCaptureScreen.kt` | Remove duplicate camera-overlay insets | Duplicated insets |
| `test/.../TimelineNavigationTest.kt` | New (5 cases) | Pin activity routing |
| `test/.../BottomBarVisibilityTest.kt` | New ownership cases | Pin bar/inset policy |
| `README.md`, `UPDATE.md`, `docs/17_CHANGELOG.md`, `docs/14_TESTING.md`, `docs/16_KNOWN_ISSUES.md`, `docs/04_FEATURES.md`, `NEXT_SESSION.md` | Documentation | Record fixes, results and remaining work |

## Documentation

- **README.md** — corrected build/test instructions (committed wrapper,
  instrumentation suite), updated build status, known limitations, repository
  structure, feature description and a 2026-10-31 recent-changes entry.
- **UPDATE.md** — dated entry recording each problem, root cause, fix, files,
  executed commands and actual results.
- **docs/17_CHANGELOG.md** — new Unreleased section for this pass.
- **docs/14_TESTING.md** — executed verification plus new manual device cases.
- **docs/16_KNOWN_ISSUES.md** — resolved entries for the wizard, undo, save
  cancellation, Home routing and insets; corrected the signing issue text.
- **docs/04_FEATURES.md** — Habits dashboard now documents the Undo snackbar
  and Count correction.
- **NEXT_SESSION.md** — follow-up status; the unrelated open PR #60 is left
  untouched.

## Testing

```text
sh ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug \
  -Pandroid.aapt2FromMavenOverride=/opt/aapt2-wrapper/aapt2
```

Actual results (JDK 17.0.21, Android SDK 35, container without a device):

- `:app:testDebugUnitTest` — **BUILD SUCCESSFUL**, 497 tests, 0 failures,
  0 errors, 0 skipped (39 XML result files).
- `:app:assembleDebug` — **BUILD SUCCESSFUL**.
- `:app:assembleDebugAndroidTest` — **BUILD SUCCESSFUL**.
- `:app:lintDebug` — **BUILD SUCCESSFUL**, 0 errors, 32 pre-existing warnings,
  7 informational.

Known gap: `:app:connectedDebugAndroidTest` was **not** run (no adb, emulator or
device). No instrumentation or on-device pass is claimed.

### Testing checklist

- [ ] First habit wizard step 1 on a small phone (360×640) and a tablet: no
      tab bar under Continue, no blank band, content scrolls.
- [ ] Wizard Back: system Back and the header arrow both move one step back; at
      step 1 both show the discard guard.
- [ ] Tick habit: check in → Done + Undo; tap Undo → Check in + "Not logged
      yet", score/streak/heatmap/percentage recompute; restart → still pending.
- [ ] Undo after browsing to another date reverses only the marked day.
- [ ] Count habit: reach target, decrement, re-increment stops at target.
- [ ] Quit habit: "On track" → Undo; "I slipped" records a slip.
- [ ] Declined notification and exact-alarm prompts still create the habit/task.
- [ ] Home recent activity opens diary/task/habit/expense/snap correctly.
- [ ] Timeline, Settings, Expenses, Note editor, AI assistant, Capture and Snap
      show no extra blank edge or overlapped controls.
- [ ] `./gradlew connectedDebugAndroidTest` passes the v6→v7 migration test on a
      device/emulator.

## Database and Security

- **Migration impact:** none. Room stays at v7; no entity, DAO or migration was
  added or removed, and `6.json`/`7.json` are untouched.
- **Data preservation:** check-in undo deletes only the current day's
  completion row for the affected habit; history for other days, checklist
  checks and archived habits are untouched.
- **Offline/privacy:** no `INTERNET` permission, no telemetry, no cloud service
  or dependency was added; SQLCipher encryption and PIN App Lock are unchanged.
- No credential, token or secret is included in this PR.

## Remaining Issues

- `connectedDebugAndroidTest` still needs a device/emulator; the v6→v7
  migration test compiles only.
- No manual on-device pass yet for the wizard, undo snackbar timing or the
  inset changes (checklist above).
- Count-habit steppers remain optimistic for very fast repeated taps (the
  persisted value stays valid; only the intermediate count may coalesce).
- Issue #7 from `docs/16_KNOWN_ISSUES.md` (AI assistant context) remains open.
