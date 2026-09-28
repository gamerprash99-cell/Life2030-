# Diary composer keyboard and date-strip cleanup — 2026-09-28

## User-visible goals
- Fix the Diary empty-state floating add button so it does not overlap the story copy.
- Give the day strip a stable, readable five-day presentation with the selected day clearly separated from adjacent dates; keep today included and the strip bounded to the existing history range.
- Make the editor behave like a chat-style writing surface when the IME opens: the content area should resize/scroll with the keyboard instead of leaving a large blank band.
- Remove the large standalone mood row from the editor.
- Move photo, voice and location actions into a compact left-side action rail inside the writing card, inspired by the supplied filled-editor reference.
- Keep all existing attachment behavior and ViewModel/Repository/Room contracts.
- Do not add permissions, network, AI, dependencies or schema changes.

## Files
- `app/src/main/java/com/lifeos/app/ui/diary/DiaryScreen.kt`
- `app/src/main/java/com/lifeos/app/ui/diary/DiaryDayHeader.kt`
- `app/src/main/java/com/lifeos/app/ui/diary/DiaryDateStrip.kt`
- `app/src/main/java/com/lifeos/app/ui/diary/DiaryEditor.kt`
- `app/src/main/java/com/lifeos/app/ui/diary/DiaryEditorAttachments.kt`
- `app/src/main/java/com/lifeos/app/ui/diary/DiaryComponents.kt`
- `app/src/main/java/com/lifeos/app/ui/diary/MoodSelector.kt`
- `UPDATE.md`

## Implementation constraints
- Preserve Compose Navigation, ViewModels, Repository boundaries and Room v5.
- Reuse existing attachment launchers and ViewModel methods; do not bypass Room.
- Keep existing permission timing and app-private media storage.
- Use IME/window inset APIs already available in the resolved Compose stack.
- Keep touch targets accessible and avoid absolute coordinates.
- Do not delete the persisted mood field; only change editor presentation.
