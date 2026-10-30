# LifeOS — Next Session Handoff

**Prepared:** 2026-10-31  
**Repository:** https://github.com/gamerprash99-cell/Life2030-  
**Target:** main  
**Purpose:** Implement permanent Habits navigation and the full video-inspired habit tracker.

## Read first
1. `docs/HABITS_PERMANENT_REDESIGN_SPEC.md` — complete design, logic, database, testing and Git contract.
2. `README.md` — project overview.
3. `UPDATE.md` — append-only development history.
4. `docs/17_CHANGELOG.md` — history.
5. `docs/CURRENT_PRODUCT_UI_CONTRACT.md` — verify against live code; may be stale.
6. `docs/05_DATABASE.md`, `docs/09_FRONTEND.md`, `docs/14_TESTING.md`, `docs/21_FILE_STRUCTURE.md`.

## User intent
- Bottom navigation: **Home · Notes · ＋ Capture · Habits · Life**.
- Insights is not a user-facing destination. Current inspected main already has no Insights route; verify all references.
- Habits becomes a permanent root tab, not a secondary route.
- Profile, Settings, App Lock and backup remain reachable.
- Reproduce video patterns: onboarding, custom icon/color, schedules, reminders, date strip, filters, score, habit cards, streaks, heatmap, Build/Quit, Tick/Count/Checklist, Timeline milestones and celebration.
- Use LifeOS Home screenshot/design identity, not the reference app's black/yellow colors.
- Preserve Kotlin, Compose, Room, SQLCipher, offline-first privacy and existing features.

## Repository facts observed
- Inspected `main` commit: `8fe220c70c2c7e8cf13f92b011f363ec2084398f`.
- Existing: `HabitsScreen.kt`, `HabitDetailScreen.kt`, `HabitEntity`, `HabitCompletionEntity`, `HabitRepository`, `HabitStatsCalculator`.
- Room version 6, SQLCipher, exported schema, migrations through 5→6.
- Current bottom bar: Home / Notes / Life / You; Capture is a raised centre action.
- Habits route exists but is stacked outside root-tabs; Profile is currently a root tab.
- Current `Screen.kt` and `LifeOSNavHost.kt` contain no Insights route.
- Existing habit model supports DAILY/WEEKLY/CUSTOM weekdays + goalCount, not all video modes.
- Theme discrepancy to resolve: live `Color.kt` says LUMEN warm clay/sage/amber; UI contract says Aurora Noir; supplied screenshot is lavender/purple. Reconcile before changing shared theme.
- Git tree contains Gradle wrapper files, while README contains older text claiming wrapper absence; verify local checkout.

## Status of this handoff
This is a **documentation/specification preparation only**. No app source, Room schema/migration, UI, navigation, tests or runtime behaviour was changed. No build, lint, emulator or instrumentation test was run for this planning pass. Implementation PR must be created only after code changes and verification.

Never paste a GitHub PAT into ChatGPT. Use local `gh auth login`, Git Credential Manager or configured GitHub integration.

## Next-session sequence
1. Audit git status, recent commits, branch, complete relevant source and docs.
2. Confirm nav/profile reachability.
3. Create feature branch; preserve all user work.
4. Promote Habits to root tab and test navigation.
5. Extend model/DAO/repository/calculator with safe migration and backup compatibility.
6. Implement dashboard, creation/edit, detail/history, tracking, reminders and celebrations.
7. Integrate Home and Timeline.
8. Run unit tests, assemble, lint and instrumentation if available.
9. Update affected documentation with verified results.
10. Review diff, push feature branch and open PR against main; do not merge without explicit approval.

Read the full specification. Do not ask the user to repeat this request.
