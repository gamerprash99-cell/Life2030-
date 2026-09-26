# LifeOS Diary UI v2

## Screen flow
1. Empty day — editorial empty state with local journal illustration and + Memory action.
2. New memory — full-screen composer, mood-first input, captured open-time stamp.
3. Keyboard writing — automatic focus/keyboard, IME-safe save action, character count.
4. Filled memory — same writing surface with selected mood and stable timestamp.
5. Save confirmation — local `Memory saved` feedback after successful repository write.
6. Saved detail — editorial reading page with Edit/Delete and preserved memory timestamp.
7. Multiple memories — selected-day chronological spine with time gutter, mood markers and inline actions.

## Working logic
- DiaryScreen reads selected-day entries from DiaryViewModel.
- DiaryViewModel reads/writes through DiaryRepository; no UI layer bypasses Room.
- New entries keep the minute captured when the composer opens.
- Editing preserves the existing entry id, day, original time and createdAt.
- Blank saves are rejected and in-flight saves are guarded against double submission.
- Delete is confirmed through the shared MemoryDeleteDialog.
- Date navigation is bounded to today and the previous 365 days.

## Privacy / architecture
- No cloud service, external AI, telemetry, network endpoint or new dependency is introduced by this UI pass.
- No Room schema or migration change is required.
- Empty-state artwork is drawn locally with Compose Canvas.

## Verification
- Source changes are on `feature/diary-ui-v2-final-complete`.
- Existing DiaryViewModelTest and DayStripRangeTest remain part of the project; one save-confirmation test was added.
- Android build/emulator verification is not claimed because no Android runner is available and outbound GitHub DNS/network access is unavailable in this environment.

### 2026-09-26 — Diary UI reference implementation alignment

- This branch aligns the existing Diary surfaces with the seven approved visual references while preserving the app's current architecture and Room-backed behavior.
- Layout rule: no absolute screen coordinates are introduced. All placement should derive from Compose measurement, intrinsic content, `WindowInsets`, weights/fill constraints, and adaptive containers.
- Screen 1 keeps the `+ Memory` affordance on the lower-right above the existing bottom navigation using app content flow rather than a fixed pixel offset.
- Screen 2 keeps keyboard-safe full-screen writing, automatic focus, mood selection, timestamp capture, and a visible save affordance.
- Screen 3 keeps filled writing with media/metadata sections represented only where existing local data/features support them; unsupported fields are not fabricated.
- Screen 4 uses successful local save state for the confirmation flow.
- Screen 5/7 keep detail/edit/delete behavior through the existing Diary detail ViewModel and Repository.
- Screen 6 keeps Diary connected to the unified Timeline via `BuildTimelineUseCase`; Diary remains one of the Timeline's real-time aggregated sources.
- Today remains the default selected day through `DiaryViewModel` and date selection is bounded to the existing history window.
- Full permissions for edit behavior are preserved through the current navigation and permission architecture; no permission is requested merely to open or edit a Diary entry.

## Scope guard
- Do not add fake location, weather, photos, voice notes, tags, favorites, share/copy, or attachment data if the existing repository/domain layer does not supply them. Visual affordances must map to real features or remain omitted until their complete local implementation exists.
- No hardcoded phone-specific coordinates, no database recreation, no architecture rewrite, no external service dependency.
