# 14 — Testing

## Current state

**Zero test files exist in this repository.** Confirmed by search: no files
under any `src/test/` or `src/androidTest/` directory.

However, the test **frameworks are already declared** as dependencies in
`app/build.gradle.kts`, ready to use:

```kotlin
testImplementation("junit:junit:4.13.2")
androidTestImplementation("androidx.test.ext:junit:1.2.1")
androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
androidTestImplementation(platform("androidx.compose:compose-bom:2024.11.00"))
androidTestImplementation("androidx.compose.ui:ui-test-junit4")
```

This means: the project is set up to support JUnit unit tests and Compose
UI tests, but nobody has written any yet.

## Test frameworks available (but unused)

| Framework | Type | Would live in |
|---|---|---|
| JUnit 4 | Unit tests (pure Kotlin logic, no Android framework) | `app/src/test/java/...` |
| AndroidX Test + Espresso | Instrumented tests (run on device/emulator) | `app/src/androidTest/java/...` |
| Compose UI Test (`ui-test-junit4`) | Compose-specific UI tests | `app/src/androidTest/java/...` |

## Manual testing

⚠️ **NOT VERIFIED FROM CODEBASE** — no manual test checklist, test plan, or
QA document exists in the repository. All verification of this app to date
has been by code review (no build environment was available to run it).

## Critical user flows a new developer should manually verify first

Since there is no automated coverage, these are the highest-value manual
checks before trusting any change:

1. **Create → Read → Update → Delete** for each of: Notes, Tasks, Habits,
   Expenses, Diary entries — confirm each persists after closing/reopening
   the app (verifies Room is actually writing/reading correctly)
2. **Habit streak & heatmap correctness** — log a habit for several
   consecutive days, then skip a day, and confirm the streak resets and the
   heatmap in `HabitDetailScreen` reflects it (`HabitRepository.computeAnalytics()`
   / `computeHeatmap()` — the most complex pure logic in the app)
3. **Timeline aggregation** — create one of each item type (note, completed
   task, completed habit, expense, diary entry, capture) on the same day and
   confirm all six appear in `TimelineScreen`, sorted correctly by time
4. **AI features with no API key** — confirm every AI entry point (note
   actions, task extraction, diary draft, insights, chat) shows the
   "Add your AI API key in Settings..." message gracefully rather than
   crashing
5. **AI features with a valid API key** — confirm at least one real round
   trip to Anthropic succeeds end-to-end
6. **Reminder scheduling** — set a task/habit reminder a few minutes out,
   background the app, confirm the notification fires (requires testing on
   Android 13+ to also confirm the `POST_NOTIFICATIONS` permission flow)
7. **App Lock** — enable it in Settings, fully close and reopen the app,
   confirm the biometric/PIN prompt appears and blocks access until passed
8. **Backup export + share** — export a backup, confirm the JSON file is
   well-formed and contains real data, confirm the Share button opens the
   system share sheet
9. **Camera/Video/Audio capture** — confirm each permission prompt appears
   only when that specific capture type is opened, and confirm a captured
   file actually appears under app-private storage afterward

## Known untested areas

- Backup **import** (`BackupRepository.importFromFile()`) — code exists but
  has no UI trigger, so it has never been exercised even manually (see
  `docs/04_FEATURES.md` §14 and `docs/16_KNOWN_ISSUES.md`)
- Database migrations — schema version is `1` with no migrations written;
  untestable until a schema change actually happens
- Any behavior on Android versions below 33 for the notification permission
  branch logic in `core/util/NotificationHelper.kt`
- Configuration changes (screen rotation) — Compose `ViewModel` state should
  survive this by default, but it has not been explicitly verified

## Recommended testing checklist for future developers

- [ ] Add JUnit unit tests for `HabitRepository.computeAnalytics()` and
      `computeHeatmap()` first — this is the most complex, highest-value pure
      logic in the app and is fully unit-testable without Android dependencies
      if `HabitDao`/`HabitCompletionDao` are faked or an in-memory Room DB is used
- [ ] Add a Room in-memory-database test for each DAO's core queries
- [ ] Add a Compose UI test for the Add Task / Add Habit dialogs
- [ ] Add an instrumented test for the `AppLockGate` flow
- [ ] Set up a GitHub Actions workflow to run `./gradlew test` on every PR
      once tests exist (see `docs/12_GITHUB_WORKFLOW.md`)
