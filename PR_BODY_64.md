## Diary Redesign v2 — New UI Components

Complete rewrite of the Diary editor screens in the `new/` package, implementing the rich-content redesign per docs/DIARY_REDESIGN_SPEC.md, DIARY_REFERENCE_VIDEO_FRAME_AUDIT.md, and DIARY_CLAUDE_IMPLEMENTATION_PLAN.md.

### What changed

**New screens (all in `ui/diary/new/`):**
- `DiaryEditorScreen` — full-screen composer with themes, stickers, lists, media attachments
- `DiaryHomeScreen` — diary list view with entry cards
- `DiaryDetailScreen` — full-page entry detail view
- `DiaryNewThemePickerSheet` — category tabs + local Canvas-drawn theme thumbnails
- `DiaryNewStickerPickerSheet` — categorized sticker/emoji grid
- `DiaryNewListPickerSheet` — bullet/numbered/checkbox/dash templates
- `DiaryNewDatePicker` / `DiaryNewTimePicker` — date/time selection
- `DiaryNewCalendarScreen` / `DiaryNewInsightsScreen` — calendar + insights
- `DiaryNewDeleteDialog` / `DiaryNewFirstEntrySuccess` — confirmation flows

**Design system:** `DiaryTheme`, `DiaryThemes`, `DiaryStickers`, `DiaryListTemplates`

**State management:** `DiaryEditorOverlay`, `DiaryEditorViewModel`, `DiaryViewModel`

**Navigation:** Updated `LifeOSNavHost` to wire in the new screens

### What was preserved
- All existing diary data/features (Room DB, `attachmentsJson`, photos, voice notes, location, tags, mood, search/calendar/insights)
- Architecture (UI → ViewModel → Repository → DAO → Room)
- Offline-first, privacy-first (no network, no cloud, no INTERNET permission)
- Room schema unchanged — rich content stored in existing `attachmentsJson` column

### Verification
- All Kotlin compilation errors resolved (icon references, imports, alignment, double-prefix fixes)
- Build killed by SIGKILL due to container memory constraints — not code errors
- `connectedDebugAndroidTest` not executed (no device/emulator)

UPDATE.md has a dated appended entry; history preserved.

PR #63 contains the data-model phase (ThemeStyle, Sticker, Drawing, ImportedAudio variants + repository/media lifecycle). PR #64 contains the complete UI rewrite building on top.