# LifeOS Diary Redesign — Todo List

Source specs: docs/DIARY_REDESIGN_SPEC.md, docs/DIARY_REFERENCE_VIDEO_FRAME_AUDIT.md, docs/DIARY_CLAUDE_IMPLEMENTATION_PLAN.md
Repository: /home/Life2030, branch main (a62d7ca)
This is a planning document only — do not modify any source code while creating this list.

## Constraints (apply to every phase)

- [ ] Build replacement alongside existing implementation; do not delete old diary code until parity is proven
- [ ] Preserve Room DB (diary_entries table, every column, all IDs/content/mood/tags/date/time/favorites/attachments/media paths)
- [ ] Preserve existing attachmentsJson column; new variants serialized into the same column, backward compatible
- [ ] No schema change unless an explicit additive Migration is audited and required
- [ ] No cloud APIs, Firebase, analytics, telemetry, network endpoints, or user-data upload
- [ ] Do not remove: search, calendar, insights, location, weather, photo, voice-note support
- [ ] Do not replace Navigation Compose or alter global bottom navigation
- [ ] Request permissions only at feature use (camera/microphone on action)
- [ ] Use Photo Picker/SAF for media import; copy to app-private storage
- [ ] Offline-first, privacy-first; bundled local assets only; no remote URLs
- [ ] Responsive UI: keyboard-safe, IME insets, 320/360/411dp widths, landscape, font scale 1.0–1.3+
- [ ] Never claim visual exactness or test success without verification

## Phase 1 — Data model: extend DiaryAttachment sealed class

Files: app/src/main/java/com/lifeos/app/domain/model/DiaryAttachment.kt
(Existing verified structure: sealed class DiaryAttachment with Photo, VoiceNote, Place; kotlinx.serialization with @SerialName discriminators; stored in DiaryEntity.attachmentsJson)

- [ ] Audit all call sites of DiaryAttachment and DiaryAttachments before editing
- [ ] Add ThemeStyle variant (stable key, never a raw remote URL; falls back to default on unknown key)
- [ ] Add Sticker variant (stable asset id, category, normalized x/y 0..1, scale, optional rotation)
- [ ] Add Drawing variant (reference to persisted app-private drawing artifact or strokes; metadata)
- [ ] Add ImportedAudio variant (app-private file path, MIME type, display name, duration when reliably available)
- [ ] Keep existing Photo, VoiceNote, Place variants unchanged
- [ ] Ensure kotlinx.serialization @SerialName discriminators are stable and additive
- [ ] Verify decode is total: unknown/malformed attachmentsJson yields empty list, never crashes list/detail/backup rendering
- [ ] Confirm no Room entity/DAO change needed (attachmentsJson column already exists, default "[]")

## Phase 2 — DiaryAttachments helpers for new types

Files: app/src/main/java/com/lifeos/app/domain/model/DiaryAttachment.kt (DiaryAttachments object)
(Existing verified helpers: encode/decode, photos(), voiceNote(), place())

- [ ] Add theme(attachments): ThemeStyle? helper (default fallback on missing/unknown key)
- [ ] Add stickers(attachments): List<Sticker> helper
- [ ] Add drawings(attachments): List<Drawing> helper
- [ ] Add importedAudio(attachments): List<ImportedAudio> helper
- [ ] Keep existing photos()/voiceNote()/place() helpers unchanged
- [ ] Add unit-testable encode/decode round-trip for each new variant
- [ ] Keep decode total (runCatching → emptyList) for old and new rows

## Phase 3 — DiaryEditorViewModel: new methods

Files: app/src/main/java/com/lifeos/app/ui/diary/DiaryEditorViewModel.kt
(Existing verified state: entryId, title, content, mood, tags, dateEpochDay, timeMinutes, photos, voiceNote, place, locationStatus, weather, recording, playback, isSaving, isLoading, savedEntryId, saveCount, errorMessage)

- [ ] Extend DiaryEditorState with theme, stickers, drawings, importedAudio fields (preserve all existing fields exactly)
- [ ] Add onThemeChange(ThemeStyle) method
- [ ] Add addSticker / updateSticker / removeSticker methods (normalized position/scale/rotation)
- [ ] Add setDrawing / removeDrawing methods
- [ ] Add attachImportedAudio(uri) via document/audio picker (copy to app-private storage on Dispatchers.IO)
- [ ] Add removeImportedAudio method (playback stop, delete file, reclaim only after replacement DB write commits)
- [ ] Add insertList(template) method at current caret/selection (bullet, numbered, checkbox/task, decorative)
- [ ] Add text formatting/tag actions with defined semantics only (heading, emphasis, underline, list style); preserve existing tags flow/serialization
- [ ] Wire new variants into save() attachments list (photos + voiceNote + place + theme + stickers + drawings + imported audio)
- [ ] Preserve existing canSave, isSaving, saveCount monotonic counter, double-submit guard, error handling
- [ ] Keep ViewModel → Repository → DAO boundary; no DAO/filesystem access from ViewModel
- [ ] Run file I/O (copy/delete/metadata) on Dispatchers.IO
- [ ] Preserve two-stage save semantics: success only after DB write completes; failed save leaves editor/draft available

## Phase 4 — UI components

### DiaryThemePickerSheet
Files: app/src/main/java/com/lifeos/app/ui/diary/ (new file DiaryThemePickerSheet.kt)

- [ ] Category tabs: Customize, Popular, Dark, Artsy, Cute, Lifestyle (+ color/pattern group)
- [ ] Scrollable thumbnail grid of bundled local assets/vector patterns (original LifeOS artwork, no proprietary copies)
- [ ] Explicit selected state with underline/indicator; include Original/default
- [ ] Apply/check affordance; apply to current entry only; preview cannot overwrite body
- [ ] Missing/unknown theme key falls back to default; no crash

### DiaryStickerPickerSheet
Files: app/src/main/java/com/lifeos/app/ui/diary/ (new file DiaryStickerPickerSheet.kt)

- [ ] Locally bundled categorized sticker/emoji palette (category strip + collection grid)
- [ ] Insert sticker as independently movable/resizable object with delete/transform handles
- [ ] Persist stable sticker id, category, normalized x/y (0..1), scale, optional rotation
- [ ] Must not obscure text/caret or controls; semantic labels and non-drag accessibility actions
- [ ] Missing asset renders safe placeholder; no remote URLs (bundled assets or Unicode emoji)

### DiaryDrawingEditor
Files: app/src/main/java/com/lifeos/app/ui/diary/ (new file DiaryDrawingEditor.kt)

- [ ] Full-screen drawing canvas from editor toolbar
- [ ] Pen/marker/eraser-like tools, color swatches, undo, redo, Done
- [ ] Stroke model (points, color, width, tool) for undo/redo and resolution independence
- [ ] Bound memory/stroke count; persist strokes or raster/vector artifact to app-private storage
- [ ] Done inserts drawing object inline in entry; survives process restart, upgrade, backup/restore, editing
- [ ] Never keep drawing only in transient Compose state

### DiaryAudioPanel
Files: app/src/main/java/com/lifeos/app/ui/diary/ (new file DiaryAudioPanel.kt)

- [ ] Preserve current voice-note recording/player workflow
- [ ] Recording section + Add Audio File (document/audio picker) actions
- [ ] Copy import to app-private storage; store MIME type, display name, duration when available
- [ ] Playback, pause/stop, duration, removal, missing-file states
- [ ] Microphone permission only when recording starts; no cloud transcription
- [ ] Include audio bytes in export/restore; metadata/file I/O off main thread

### DiaryListPickerSheet
Files: app/src/main/java/com/lifeos/app/ui/diary/ (new file DiaryListPickerSheet.kt)

- [ ] Contextual palette: bullet, numbered, checkbox/task, decorative list styles
- [ ] Selecting template inserts at current caret/selection without deleting adjacent text
- [ ] Content stays editable; checkboxes only if document model supports item state
- [ ] Plain-text search must find list content; no ornamental Unicode that destroys editability

### Updated DiaryEditor
Files: app/src/main/java/com/lifeos/app/ui/diary/DiaryEditor.kt

- [ ] Top row: Back, overflow, prominent pink Save action (compact pill, 48dp min touch target)
- [ ] Date/mood row; optional title; body writing surface
- [ ] Persistent bottom toolbar above IME/system navigation insets
- [ ] Contextual tool palettes/picker sheets above toolbar (theme, media, stickers, lists, audio, drawing)
- [ ] Inline rich objects rendered in the entry (theme, stickers, drawings, media, lists, formatting)
- [ ] Scrollable writing content; active caret always visible
- [ ] WindowInsets + IME insets; no fixed absolute offsets; no content behind IME/gesture bar/3-button nav
- [ ] Preserve two-stage Back behavior (keyboard hides first, then dismiss); never silently discard saved data
- [ ] Preserve existing attachments() and editorActions() slots and both host screens
- [ ] Soft pink/pastel canvas scoped to Diary; pale white/pink panels, quiet borders, rounded surfaces; dark-mode contrast

## Phase 5 — Rich content rendering on page canvas

- [ ] DiaryRichContentRenderer: render persisted blocks and legacy fallback
- [ ] Render text blocks, stickers (normalized coords), drawings, photos, voice notes, imported audio, place
- [ ] Legacy row (no rich payload): render current content + attachmentsJson unchanged
- [ ] Valid rich payload: render rich document
- [ ] Unknown version/malformed payload: show legacy body + safe attachment placeholders; never crash/delete
- [ ] Do not eagerly rewrite old entries on app launch; retain legacy values until new-format save commits
- [ ] Rich formatting serializes predictably and remains searchable
- [ ] Theme renders consistently in detail/list/export/backup where appropriate

## Phase 6 — Backup/restore via attachmentsJson serialization

Files: app/src/main/java/com/lifeos/app/data/repository/BackupRepository.kt (+ DiaryRepository)
(Existing verified: LifeOSBackup with diaryEntries: List<DiaryEntity>, formatVersion=2, exportToFile/importFromFile/restore; DiaryRepository.restoreFromBackup upserts by primary key)

- [ ] Audit actual backup format and restore behavior before editing
- [ ] Extend local backup to include theme metadata, rich blocks, stickers, drawings, imported audio and media bytes
- [ ] Version backup format if needed, preserving older imports (formatVersion bump only if incompatible)
- [ ] Restore media to staging; validate; commit row references; finalize moves; clean staging on failure
- [ ] Never silently omit new rich content; keep backups user-initiated/local
- [ ] Keep old backup import working (legacy backups restore unchanged)
- [ ] Include audio bytes/drawing artifacts in export/restore
- [ ] Test legacy backup import, new backup round-trip, missing media, malformed rich JSON

## Phase 7 — Unit tests for new code

- [ ] Unit tests for DiaryAttachment encode/decode round-trip (all variants incl. new)
- [ ] Unit tests for DiaryAttachments helpers (theme/stickers/drawings/importedAudio grouping)
- [ ] Unit tests for new DiaryEditorViewModel methods (theme, stickers, drawings, imported audio, list insertion)
- [ ] Unit tests for serializer: backward-compat decode of old rows, total on malformed input
- [ ] Unit tests for backup/restore round-trip (legacy + new rich entries/media)
- [ ] Unit tests for migration (if any Room migration added): old DB → new, every original field/attachment unchanged
- [ ] Run :app:testDebugUnitTest and confirm all pass

## Phase 8 — Build verification

Run exact project commands from docs/DIARY_REDESIGN_SPEC.md §8 and report actual outcomes:

- [ ] sh ./gradlew :app:compileDebugKotlin
- [ ] sh ./gradlew :app:testDebugUnitTest
- [ ] sh ./gradlew :app:assembleDebug
- [ ] sh ./gradlew :app:assembleDebugAndroidTest
- [ ] sh ./gradlew :app:lintDebug
- [ ] (Optional, only with emulator/device) sh ./gradlew :app:connectedDebugAndroidTest
- [ ] Manual QA: 320/360/411dp widths; large display; landscape; 1.3+ font size; gesture and 3-button nav; keyboard shown/hidden during editor and palette use; existing entry edit/save; restart/migration/backup/restore; no network requests/permission/API key

## Phase 9 — UPDATE.md entry

Files: /home/Life2030/UPDATE.md

- [ ] Read complete UPDATE.md history before editing
- [ ] Append a dated entry describing the Diary redesign (scope, data model, components, build results, known issues)
- [ ] Preserve all existing history; never overwrite or delete previous entries

## Phase 10 — PR generation

- [ ] Review final diff; keep changes small and reviewable per phase
- [ ] Confirm no source files deleted; old implementation preserved alongside new
- [ ] Generate PR from feature branch against main
- [ ] Include spec references, build/test results, and completion definition in PR description

## Completion definition (from docs/DIARY_REDESIGN_SPEC.md §9)

- [ ] Reference states visible in the video are implemented or explicitly excluded with reason
- [ ] All existing data/features survive
- [ ] Rich content is durable, versioned, backed up and recoverable
- [ ] Offline/privacy requirements remain intact
- [ ] Navigation, keyboard, insets, accessibility and adaptive layout pass
- [ ] Relevant builds/tests have been executed
- [ ] UPDATE.md has a dated appended entry; its history is preserved