## Diary Redesign

Implements the LifeOS Diary rich-content editor redesign per docs/DIARY_REDESIGN_SPEC.md, DIARY_REFERENCE_VIDEO_FRAME_AUDIT.md, and DIARY_CLAUDE_IMPLEMENTATION_PLAN.md.

### What changed

**Data model** (`DiaryAttachment.kt`): Added `ThemeStyle`, `Sticker`, `Drawing`, and `ImportedAudio` variants to the sealed `DiaryAttachment` class. All stored in the existing `attachmentsJson` column — **no Room schema change**, fully backward compatible.

**DiaryAttachments helpers**: Added `themeStyle()`, `stickers()`, `drawing()`, `importedAudio()` accessors.

**DiaryEditorViewModel**: Added state fields + methods for theme, stickers (add/move/remove), drawing (set/remove), imported audio (add/remove), audio playback toggle (`playingFilePath`). Save persists all new attachment types.

**DiaryRepository**: Media lifecycle extended to delete drawing/imported audio files on entry deletion and orphaned-media cleanup.

**MediaStorage**: Added `diaryDrawingsDir()`/`newDiaryDrawingFile()` and `diaryImportedAudioDir()`/`newDiaryImportedAudioFile()` for app-private storage.

**UI**: `DiaryThemePickerSheet` (category tabs + local Canvas-drawn theme thumbnails), `DiaryStickerPickerSheet` (category strip + glyph grid), `DiaryListPickerSheet` (bullet/numbered/checkbox/dash templates), wired into `DiaryEditor` via tool palette buttons and a prominent Save action.

### What was preserved
- All existing diary data/features (Room DB, attachmentsJson, photos, voice notes, location, tags, mood, search/calendar/insights)
- Architecture (UI → ViewModel → Repository → DAO → Room)
- Offline-first, privacy-first (no network, no cloud, no INTERNET permission)
- Existing navigation and bottom bar

### Verification (executed)
- `sh ./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL
- `sh ./gradlew :app:testDebugUnitTest` → BUILD SUCCESSFUL
- `sh ./gradlew :app:assembleDebug` → BUILD SUCCESSFUL
- `sh ./gradlew :app:assembleDebugAndroidTest` → BUILD SUCCESSFUL
- `sh ./gradlew :app:lintDebug` → BUILD SUCCESSFUL (0 errors)

Instrumentation tests (`connectedDebugAndroidTest`) require a device/emulator and were not executed here.

UPDATE.md has a dated appended entry; history preserved.
