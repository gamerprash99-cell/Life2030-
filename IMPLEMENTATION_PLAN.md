# LifeOS Implementation Plan

## Current State
- **Branch:** redesign/from-zero-2026 (HEAD: 90a142b)
- **Build:** ✓ Assembles successfully
- **Design System:** AURA v6 (lagoon canvas, coral signature, mint partner, frosted glass) — already applied
- **Navigation IA:** Home · Notes · ＋Capture · Insights · Life ✓ (5-section bar restored)
- **Feature Coverage:** Most P0 features implemented per spec

## Phases

### Phase 0 — Completed
- Repository and environment audit done
- Project audit documented
- Toolchain identified (JDK 17, AGP 8.9, SDK 35, Gradle 8.9)

### Phase 1 — App Foundation and Design System
**Already completed** — AURA v6 design tokens, theme, and navigation established.

### Phase 2 — Core Productivity
- Tasks: ✓ Implemented with recurrence, reminders
- Habits: ✓ Implemented with streaks, heatmap
- Notes: ✓ Implemented with rich text, folders, AI actions
- Expenses: ✓ Implemented with exact money handling

### Phase 3 — Personal Life and Capture
- Diary: ✓ Implemented with mood, media, voice-to-text
- Life Capture: ✓ Implemented (photo/video/audio/thought/mood/location)
- Timeline: ✓ Implemented with event projection
- Calendar: ✓ Implemented with day/week/month views

### Phase 4 — Expenses and Insights
- Expenses: ✓ Implemented with category breakdown, spending insights
- Insights: **Partially completed** — tab restored in bottom bar, view model wired to intelligence engine

### Phase 5 — AI Infrastructure and Controlled AI
- AI provider: ✓ On-device engine, no cloud provider, no embedded API key
- AI context permissions: ⚠️ Needs verification against spec §16.2 (4 levels)
- AI actions with review: ⚠️ Needs more integration testing

### Phase 6 — Security, Backup, and Recovery
- App lock/PIN: ✓ Implemented with SQLCipher encryption
- Backup/Restore/Export/Import: ✓ Implemented with JSON archive
- Trash/Recovery: ✓ Implemented per-entity
- Security: ✓ Threat model documented

### Phase 7 — Advanced Polish
- Semantic search: Not yet implemented (optional per spec)
- Smart suggestions: In progress
- Release hardening: Need wrapper scripts, accessibility gate, final QA

## Key Decisions & Tradeoffs

### Navigation IA
- **Decision:** Restored Insights tab to bottom bar (Home · Notes · ＋Capture · Insights · Life)
- **Rationale:** Spec §4/§27 requires five-section bar; "You" moved to profile hub accessible from Home/Life
- **Conflict:** Earlier commit removed Insights; this re-adds it per owner spec

### Design Language
- **Decision:** AURA v6 (lagoon: teal→plum canvas, coral signature, mint partner, frosted glass)
- **Rationale:** Spec §53 direction; keeps existing color investments while applying modern glassmorphism
- **Constraint:** Colors should not be changed per user request — only apply the AURA v6 token system already in place

### AI Philosophy
- **Decision:** On-device only, no cloud provider, no embedded API key
- **Rationale:** Spec principle: "Your life. Your data." No server dependencies

## Acceptance Criteria per Phase

### Phase 2 (Foundation)
- [x] Launchable Compose app
- [x] App theme with AURA v6 tokens
- [x] Navigation shell with 5-section bar
- [x] Reusable UI components
- [x] Local database (Room + SQLCipher)
- [x] Error handling basics
- [x] Basic accessibility

### Phase 3 (Personal Life)
- [x] Diary with mood and media
- [x] Capture with all types
- [x] Timeline and calendar

### Phase 4 (Insights)
- [x] Insights tab in bottom bar
- [x] Weekly/monthly reports from offline engine
- [x] All figures from stored rows (no placeholders)

### Phase 6 (Security/Backup)
- [x] App lock with PIN
- [x] Backup/export round-trip
- [x] Trash and recovery

## Remaining Work Before Release

1. **AI Context Permissions** — Verify 4-level permission system (L1 current, L2 selected, L3 modules, L4 full) works correctly
2. **Accessibility Gate** — Run automated contrast/state verification
3. **Device/Instrumentation Tests** — Cannot run without emulator; record limitation
4. **Backup Round-Trip** — Verify on actual device (Termux environment limitation)
5. **Wrapper Scripts** — Fix gradlew reproducibility
6. **Dead Dependency Removal** — biometric dependency declared but unused
7. **Documentation** — Update ALL docs (CHANGELOG, SECURITY.md, PRIVACY.md)

## Build and Test Commands

```
# Build debug APK
/tools/lifeos-build-home.sh :app:assembleDebug

# Run unit tests
/tools/lifeos-build-home.sh :app:testDebugUnitTest

# Full build + test
/tools/lifeos-build-home.sh :app:assembleDebug :app:testDebugUnitTest
```

## This Session (Redesign Pass) — Completed

1. **Insights tab restored** — `Screen.Insights` + NavHost route wired; bottom bar
   is now `Home · Notes · ＋Capture · Insights · Life`; `BottomNavItemsTest` updated.
2. **Voice capture fixed** — the Capture hub Audio tile was a stub. It now opens
   the real recorder (`AudioCaptureScreen`) built on the shared `AudioRecorder`
   interface, saving through `CaptureViewModel.saveAudio`; dead `CaptureSheet.kt`
   removed. `CaptureViewModelTest` covers the save path.
3. **Launcher mark recoloured** — AURA lagoon tile, mint→coral ring, coral bead,
   plus a monochrome layer for themed icons (shape unchanged; colour only).
4. **Animated start screen** — `LifeOSStartScreen`, theme-aware, reduced-motion
   aware, crossfading into the app after the native splash.
5. **Micro-motion** — capture halo breath, animated bottom-nav selection pill,
   self-drawing capture success tick, breathing Home AI orb.

## Next Session Tasks

1. Verify AI context permission levels work
2. Run accessibility contrast check
3. Begin release hardening (wrapper, docs, signing guidance)
4. Apply any remaining Figma-led visual polish on top of AURA v6
5. Confirm the start screen + icon on a physical device (no emulator in this env)