# Task List — LifeOS Diary Stitch Redesign

## Phase 1: Intelligence foundations
- [ ] Task 1: `DiaryInsights` model + `diaryInsights()` on `LifeOSIntelligenceEngine`; expose engine from `ServiceLocator`.
- [ ] Task 2: Pure `DiaryConnections` builder (nodes/edges from real data via `BuildTimelineUseCase`).

## Checkpoint: Foundations
- [ ] Existing unit tests still green (`gradle :app:testDebugUnitTest`).

## Phase 2: Diary screen redesign
- [ ] Task 3: `DiaryViewModel` (entries, day filter, composer, insights, connections, edit/delete).
- [ ] Task 4: `DiaryScreen` redesign — header, date strip, journal cards, empty/loading/error, FAB, analytics, connections view.
- [ ] Task 5: `DiaryEditorSheet` — new/edit bottom sheet with tinted mood chips.
- [ ] Task 6: `DiaryDetailScreen` + route wiring (`Screen.kt`, `LifeOSNavHost.kt`).
- [ ] Task 7: Mood/editorial palette tokens in `Color.kt`.

## Checkpoint: UI compiles
- [ ] `gradle :app:compileDebugKotlin` passes.

## Phase 3: Verification & docs
- [ ] Task 8: Unit tests for new pure logic (DiaryConnections, mood pill mapping).
- [ ] Task 9: `gradle :app:testDebugUnitTest`, `:app:assembleDebug`, `:app:lintDebug` all pass; findings recorded.
- [ ] Task 10: Update README.md, UPDATE.md, CHANGELOG.md, docs/17_CHANGELOG.md.
- [ ] Task 11: Diff review; create branch `feat/diary-stitch-redesign`; commit; final report.
- [ ] STOP — ask user for GitHub PAT before pushing. Never write PAT to disk.