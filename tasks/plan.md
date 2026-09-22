# Implementation Plan: LifeOS Diary Stitch Redesign

## Overview

Reproduce the Stitch "LifeOS Diary Redesign" project
(`https://stitch.withgoogle.com/projects/11028282657733913812` — 3 screens:
journal list, empty state, "New diary entry" composer sheet) as native Kotlin +
Jetpack Compose inside the existing LifeOS architecture. No demo, no HTML, no
fake data. Every visible value must come from the real Room-backed repositories,
and every smart surface must be powered by the existing on-device Intelligence
Engine.

## Design system extracted from Stitch ("Tactile Editorial Journal")

- Canvas `#FDF8FF` (parchment), cards `#FFFCFF`/`#FFFFFF`, hairline borders
  `rgba(73,69,78,0.08)`, warm diffused shadows. Primary ink violet `#21005D`,
  lavender secondary `#EADDFF`, dried-rose tertiary `#7D5260`.
- Mood palette: Goldenrod `#F59E0B`, Sage `#10B981`, Indigo `#6366F1`,
  Terracotta `#F43F5E`.
- Editorial serif (Playfair Display) for titles/dates/body; sans (Plus Jakarta
  Sans) for UI. Cards `24dp` radius, inputs `16dp`, pills full-round.
- Mobile margins `20dp`, card gap `16dp`, bottom inset `16dp`.
- FAB: 60dp circular lavender pill with deep-purple plus icon, floating bottom
  right. Mood chips: tinted selected state matching the emotion hue.
- Bottom nav: floating rounded island, active tab = elliptical lavender pill.

Mapping to the existing LifeOS theme: the app's established Material 3 theme and
font stack are kept (Phase 0 requires respecting existing architecture/theme);
the Diary screen reproduces the structure, spacing, elevation language, mood
palette and interactions of the Stitch design using LifeOS tokens.

## Architecture Decisions

1. **No Room schema change.** `diary_entries` already stores `mood`, `tagsCsv`,
   `dateEpochDay`, `timeMinutes`, `title`, `content`, `createdAt`, `updatedAt`.
   DB version stays `1`; no migration.
2. **No new dependencies** (no chart lib, no network, no font file). Charts are
   Compose `Canvas`; all intelligence reuses `LifeOSIntelligenceEngine`.
3. **Reuse patterns already proven in this repo**: `Dialog`+`Surface` based
   sheet (mirrors `CaptureSheet.kt` Stitch precedent), `GlassCard`/`LifeOSCard`
   surface language, `LifeOSTopBar`, `LifeOSSpacing`, existing navigation.
4. **New deterministic, modular pieces** (all offline, tested):
   - `DiaryInsights` model + builder inside the Intelligence Engine facade
     (`diaryInsights()`) — composes DiaryAnalyzer/TrendAnalyzer/PatternDetector/
     CorrelationAnalyzer/KeywordExtractor/StatisticsEngine.
   - `DiaryConnections` (pure) — builds a real relationship graph from persisted
     data via `BuildTimelineUseCase` (no fabrication).
   - `DiaryViewModel` extracted from the screen file.
5. **Graphify**: no reusable in-app graph exists (`graphify-out/` is a developer
   tool artifact). The Diary "connections" is rendered with Compose `Canvas`
   from real data only.
6. **Navigation**: existing Compose navigation preserved. Add one secondary
   route `diary/{entryId}` for entry detail (system back / predictive back).

## Task List

### Phase 1: Intelligence foundations
- [ ] Task 1: Add `DiaryInsights` model + `diaryInsights()` to the Intelligence
      Engine (expose engine from ServiceLocator).
- [ ] Task 2: Add pure `DiaryConnections` builder + unit tests.

### Phase 2: Diary screen redesign
- [ ] Task 3: `DiaryViewModel` — entries, day filter, composer, insights,
      connections, edit/delete.
- [ ] Task 4: `DiaryScreen` — editorial header, date-strip chips, journal cards,
      empty state, loading/error, FAB, analytics (Canvas mood chart, keywords,
      trends, patterns, recommendations, local "Ask"), connections view.
- [ ] Task 5: `DiaryEditorSheet` — new/edit bottom sheet with tinted mood chips
      (persists via DiaryRepository).
- [ ] Task 6: `DiaryDetailScreen` + route wiring (`Screen.kt`, `LifeOSNavHost.kt`).
- [ ] Task 7: Theme tokens (`Color.kt`) for mood palette + editorial accents.

### Phase 3: Verification & docs
- [ ] Task 8: Unit tests for new pure logic.
- [ ] Task 9: Build + lint + tests via system gradle.
- [ ] Task 10: Update README/UPDATE/CHANGELOG/docs.
- [ ] Task 11: Full diff review, branch `feat/diary-stitch-redesign`, report.
      STOP — ask for PAT before any push.

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Compose API drift on older BOM | Med | Stick to APIs already used in repo (LazyColumn, Canvas, AnimatedVisibility, spring); compile early |
| Mood storage is free text | Med | Reuse existing mood strings; MoodAnalyzer for null moods; keep stored value on save |
| No in-app graph lib | Med | Pure Canvas graph, deterministic layout from counts, small node set |
| RoomFlow identity for filtered list | Low | filter in ViewModel via combine; keys on entry.id |
| Runtime cost of per-card analysis | Med | Compute keywords/mood once per card via remember(entry.id, refresher) |

## Open Questions

- None blocking. On-device visual confirmation requires an emulator which is not
  available in this environment; verified via compile, JVM tests, lint and pixel
  measurement of the Stitch source.