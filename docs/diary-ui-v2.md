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