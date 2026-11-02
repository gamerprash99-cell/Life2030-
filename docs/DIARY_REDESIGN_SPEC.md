# LifeOS Diary Redesign — Product, UI and Backend Specification

**Status:** Implementation specification; not a claim that the redesign is implemented.  
**Reference:** `docs/DIARY_REFERENCE_VIDEO_FRAME_AUDIT.md`  
**Repository:** `gamerprash99-cell/Life2030-`  
**Platform:** Native Android / Kotlin / Jetpack Compose / Material 3.  
**Principles:** Offline-first, privacy-first, Room-backed, existing architecture preserved.

## 1. Goal

Rework Diary so its editor and rich-writing interactions closely follow the supplied MyDiary reference recording. Preserve LifeOS product identity, existing Diary history and all current local capabilities. The reference guides visual composition and interaction; it does not override LifeOS security/privacy or justify copying a third-party paywall/cloud architecture.

“Exact match” means reproduce behaviors and visual states evidenced by the recording as closely as possible. Where the video does not expose exact pixels/assets/behavior, record uncertainty and use a consistent LifeOS-native approximation; do not claim unsupported pixel-perfect fidelity.

## 2. Existing LifeOS baseline — verified from repository

### 2.1 Stack and architecture
- Kotlin 2.0.21, Jetpack Compose and Material 3.
- Single Gradle module `:app`.
- Manual `ServiceLocator` DI and `LambdaViewModelFactory`.
- Compose Navigation with one `NavController`.
- ViewModels expose `StateFlow`.
- UI → ViewModel → Repository → Room DAO.
- Room + SQLCipher encrypted local database.
- Local file storage for media.
- README states INTERNET permission is not declared.
- Offline local intelligence; no cloud AI.
- Preserve these boundaries and patterns.

### 2.2 Existing diary source paths
Verified current files include:
- `app/src/main/java/com/lifeos/app/ui/diary/DiaryScreen.kt`
- `DiaryEditor.kt`
- `DiaryEditorViewModel.kt`
- `DiaryEditorAttachments.kt`
- `DiaryViewModel.kt`
- `DiaryDetailScreen.kt`
- `DiaryDayHeader.kt`
- `DiaryDateStrip.kt`
- `DiaryMoodSelector.kt`
- `DiaryMoodVisuals.kt`
- `DiaryPaper.kt`
- `DiarySearchScreen.kt`, `DiaryCalendarScreen.kt`, `DiaryInsightsScreen.kt`
- `app/src/main/java/com/lifeos/app/data/db/entities/DiaryEntity.kt`
- `app/src/main/java/com/lifeos/app/data/db/dao/DiaryDao.kt`
- `app/src/main/java/com/lifeos/app/data/repository/DiaryRepository.kt`
- `app/src/main/java/com/lifeos/app/domain/model/DiaryAttachment.kt`
- `app/src/main/java/com/lifeos/app/core/util/MediaStorage.kt`
- `app/src/main/java/com/lifeos/app/ui/navigation/LifeOSNavHost.kt`
- `app/src/main/java/com/lifeos/app/core/util/PermissionManager.kt`
- Database: `app/src/main/java/com/lifeos/app/data/db/AppDatabase.kt`
- Backup implementation: `app/src/main/java/com/lifeos/app/data/repository/BackupRepository.kt` (must inspect full current implementation before editing).

The exact action bar implementation path was not confirmed; search the repository before creating or changing it.

### 2.3 Existing diary data model

Current `DiaryEntity` fields:
- `id: String` primary key
- `title: String?`
- `content: String`
- `mood: String?`
- `tagsCsv: String`
- `dateEpochDay: Long`
- `timeMinutes: Int`
- `aiGenerated: Boolean`
- `isReviewed: Boolean`
- `attachmentsJson: String` default `[]`
- `isFavorite: Boolean`
- `createdAt: Long`
- `updatedAt: Long`

Current sealed `DiaryAttachment` format stores Photo (app-private file path), VoiceNote (app-private recording path and duration), and Place (coordinates and display name).

Do not break old `attachmentsJson` values. Unknown/malformed values must not crash list/detail/backup rendering.

### 2.4 Existing diary behavior

- Diary list is day-scoped; selected date is a concrete epoch day and bounded to available history.
- Entries are Room-backed, newest-first within a day.
- Full-screen composer opens from list/detail.
- Date/time are captured and retained; edit does not silently move entry.
- Mood is optional and persisted using existing mood keys.
- Title is optional; body required; current body cap is 1,000 characters. Title/tag limits are enforced by the editor ViewModel.
- Photos are copied into app-private diary media storage.
- Voice notes use platform recorder/player.
- Location is attached from real device location; permission states are handled.
- Weather model honestly represents unavailable data.
- Attachments are serialized into existing `attachmentsJson`.
- Repository removes orphaned photo/audio files when attachments are removed or entry deleted.
- Search, calendar and local insights screens exist.
- Navigation uses existing Compose NavHost. Composer is an in-place branch with full-window chrome/inset ownership.

Do not remove these working behaviors just because the reference clip does not show them.

## 3. Target information architecture

### 3.1 Diary home/list

1. Soft illustrated/gradient diary header area (LifeOS-native; create original artwork/patterns).
2. Header actions: navigation/menu, search, overflow (calendar/insights and existing supported actions).
3. Date/day context.
4. Entry cards/rows: title/body preview, date/time, mood emoji, attachment indicators, favorite indicator.
5. Clear New Entry action.
6. Empty state encouraging first entry.
7. No paywall for diary create/read/search/export/core operation.

Preserve current date strip/day selection and search/calendar/insights entry points unless a review proves a safer integration. Do not silently drop current menu capabilities.

### 3.2 Composer layout

- Full-screen composing surface.
- Top row: Back, optional overflow, Save.
- Date picker control.
- Mood emoji affordance.
- Optional Title.
- Main Body writing surface.
- Inline rich objects rendered in the entry.
- Persistent bottom toolbar above IME/system navigation insets.
- Contextual tool panels/palettes above toolbar.
- Scrollable writing content; active caret always visible.
- Keyboard closed: toolbar above navigation/gesture area.
- Keyboard open: toolbar reachable above keyboard and content viewport shrinks/scrolls safely.
- Use Compose constraints, WindowInsets and IME insets; no fixed absolute offsets.
- Preserve two-stage Back/keyboard behavior and never silently discard saved data.

### 3.3 Mood

- Tap mood icon/control to open compact accessible emoji panel.
- Selection closes panel and reflects in editor header.
- Reopening permits change/clear.
- Reuse `DiaryMoods` and persisted mood keys; do not invent incompatible values.
- Mood remains optional.

### 3.4 Date

- Tapping date opens month calendar grid.
- Previous/next month; selected date visually distinct.
- Cancel discards temporary selection; Done applies.
- Use epoch-day/DateTimeUtils semantics and timezone-safe conversion.
- Preserve current date-range rules.
- Persist selected date/time through existing ViewModel/repository.

### 3.5 Background/theme tool

Reference shows category tabs, scrollable thumbnail grid and apply/check affordance.

- Categories should cover visible intent: Customize, Popular, Dark, Artsy, Cute, Lifestyle and color/pattern group. Complete reference list is not legible.
- Explicit selected state; include Original/default.
- Apply to current entry only. Preview cannot overwrite body.
- Bundle lightweight local assets/vector/drawable patterns; no remote loading.
- Do not copy proprietary illustrations; create original LifeOS-safe assets.
- Theme is presentation metadata, not a separate entry.
- Missing/unknown theme key falls back to default.
- Theme should render consistently in detail/list/export/backup where appropriate.
- Decide storage format in §5 before implementation.

### 3.6 Add media

Reference sheet shows Camera and Photos & Videos.

- Reuse existing photo importer/camera flow where suitable.
- Photos & Videos must use Android Photo Picker/SAF; avoid broad gallery permission.
- Copy imports to app-private storage; do not depend on temporary URI grants.
- Show preview, add/remove and missing-file state.
- Do not add video until playback, rotation/aspect ratio, backup and cleanup are fully implemented.
- For full video support, define typed Video metadata (path, MIME, dimensions/orientation, duration), preserve natural aspect ratio and provide fullscreen playback.
- Run file operations on IO.
- No network/cloud/public storage.

### 3.7 Stickers

- Toolbar opens a locally bundled categorized sticker/emoji palette.
- Insert sticker into page; select, drag/position, resize; delete affordance.
- Must not obscure text/caret or controls.
- Persist stable sticker ID, category, normalized x/y (0..1), scale and optional rotation.
- Use normalized page coordinates, not raw pixels.
- No remote URLs; use bundled assets or Unicode emoji.
- Provide semantic labels and non-drag accessibility actions.
- Missing asset renders safe placeholder.

### 3.8 Lists

- Contextual palette offers bullet, numbered, checkbox/task and decorative list styles.
- Selecting template inserts at current caret/selection.
- Content stays editable; checkboxes only if document model supports item state.
- Reference toast says to insert bullet list into main text.
- Do not paste ornamental Unicode that destroys editability unless documented fallback.
- Plain-text search must find list content.
- Old plain-text entries remain unchanged.

### 3.9 Text formatting/tags

Reference shows expanded toolbar and text-style/tag-like icons; exact full semantics are not legible.

- Preserve existing tags flow/serialization.
- Only implement formatting actions with defined semantics (e.g. heading, emphasis, underline, list style).
- Preserve text selection/caret when using toolbar.
- Rich formatting must serialize predictably and remain searchable.
- Do not assign invented functions to ambiguous icons.

### 3.10 Audio

Reference offers Recording and Add Audio File.

- Keep current voice-note recording/player workflow.
- Add optional import using Android document/audio picker.
- Copy import to app-private storage.
- Store MIME type, display name and duration only when reliably available.
- Support playback, pause/stop, duration, removal and missing-file states.
- Request microphone permission only when recording starts.
- No cloud transcription.
- Include audio bytes in export/restore.
- Metadata/file I/O off main thread.

### 3.11 Drawing

- Dedicated drawing surface from editor toolbar.
- Tools evidenced: pen/marker/eraser-like, color swatches, undo, redo, Done.
- Done inserts drawing object inline in entry.
- Persist strokes or raster/vector artifact in app-private storage.
- Prefer stroke model (points, color, width, tool) for undo/redo and resolution independence; render thumbnail as needed.
- Bound memory/stroke count.
- Survive process restart, upgrade, backup/restore and editing.
- Never keep drawing only in transient Compose state.

### 3.12 Save / first entry

- Persist via existing DiaryRepository → DiaryDao → Room.
- Show progress; block double-submit.
- After success, dismiss editor and resolve persisted row.
- Preserve current LifeOS save confirmation or integrate with reference's first-entry panel.
- First-entry-only panel may show “First Entry Saved!”, optional 3-Day Diary Challenge, reminder selector and dismiss action.
- Challenge must not gate diary features.
- Schedule reminder only after explicit opt-in and existing notification permission flow.
- Never show success before DB write succeeds.
- Subsequent saves use normal confirmation.

## 4. UI design requirements

### 4.1 Visual fidelity

- Soft pink/pastel canvas scoped to Diary; do not re-theme entire app.
- Pale white/pink panels, quiet borders, restrained shadow, rounded surfaces.
- Compact prominent pink Save action.
- Editorial/handwritten date display, with highly readable body.
- Compact toolbar with semantic labels.
- Palette tabs with selected underline/indicator and consistent thumbnail cards.
- Stickers/drawings are editable content, not blocking overlays.

### 4.2 Accessibility and responsiveness

- Minimum 48dp targets where feasible.
- Content descriptions/semantics for actions.
- Color never sole selected-state signal.
- TalkBack and keyboard navigation.
- Test 320dp/360dp/411dp/large display, landscape, font scale 1.0–1.3+.
- No content behind IME, gesture bar or 3-button navigation.
- Adaptive height/scrolling for panels; no absolute placement.
- Maintain contrast in dark mode.
- Preserve editor draft and scroll state while palettes open/close.

## 5. Backend and persistence design

### 5.1 Preserve current entity first

Keep `diary_entries`, current primary key and every existing column. Existing `attachmentsJson` is backward-compatible storage.

### 5.2 Versioned rich-content format

Before adding theme/sticker/list/drawing/imported audio/video, define a versioned payload.

Preferred options (choose after auditing consumers and backup):
- Extend attachment serialization backward-compatibly; or
- Add separate versioned `richContentJson` via explicit additive Room migration.

Do not force all entries through a new parser without fallback. Proposed conceptual document:
- `schemaVersion`
- ordered blocks
- plain text/body fallback
- attachment object IDs
- type discriminator
- stable local asset key/relative app-private path
- normalized visual coordinates
- dimensions/metadata where required

Possible types (adapt existing packages; avoid duplicate models):
- TextBlock (plain text, formatted spans, list)
- StickerBlock (asset key, normalized position, scale, optional rotation)
- DrawingBlock (file reference or strokes)
- PhotoAttachment
- VoiceNoteAttachment
- ImportedAudioAttachment
- VideoAttachment only if full flow is supported
- PlaceAttachment
- ThemeStyle (stable key, never raw remote URL)

Legacy row rules:
- No rich payload: render current `content` and `attachmentsJson` as today.
- Valid rich payload: render rich document.
- Unknown version/malformed payload: show legacy body and safe attachment placeholders; never crash/delete.
- Do not eagerly rewrite old entries on app launch.
- Retain legacy values until new-format save commits successfully.

### 5.3 Migration rules

- Inspect actual AppDatabase schema version and exported schema on branch before selecting migration.
- Never use destructive migration or recreate database.
- Add explicit `Migration(n, n+1)` that adds only required schema elements.
- Update exported Room schema JSON and migration tests from every supported prior version.
- Preserve all tables/rows and diary fields, including attachment JSON and file paths.
- Test old DB migration and assert every original field/attachment is unchanged.
- If extending JSON avoids schema change safely, document trade-off and maintain compatibility.

### 5.4 Repository boundaries and file lifecycle

- ViewModel must not access DAO or filesystem directly.
- Repository owns Room reads/writes and media lifecycle.
- Use injected dispatchers/IO for copy/delete/metadata work.
- Remove files only when no stored object/entry references them.
- Do not delete previous media during draft edit; reclaim only after replacement DB write commits.
- Use temp-file copy + validation + atomic rename where practical.
- Validate deletion paths remain under app-private diary media directory.
- Handle interrupted copy and failed save without dangling rows/files.

### 5.5 Backup / restore / export

- Extend local backup to include theme metadata, rich blocks, stickers, drawings, imported audio and media bytes.
- Version backup format if needed, preserving older imports.
- Restore media to staging; validate; then commit row references and finalize moves; clean only staged files on failure.
- Never silently omit new rich content.
- Keep backups user-initiated/local.
- Sharing/export may flatten rich diary into readable output with media; keep existing text-share behavior.
- Test legacy backup import, new backup round-trip, missing media and malformed rich JSON.

## 6. Privacy, security and offline

- No Firebase, analytics, telemetry, remote DB, cloud file storage, external AI or user-data upload.
- No mandatory network permission/client.
- No remote image/asset URLs; bundle assets locally.
- Never upload text, mood, photos, audio, drawing, location or metadata.
- Do not add large LLM for diary formatting.
- Future writing suggestions use optional local deterministic Intelligence Engine.
- Do not copy reference security questions or plaintext recovery secrets. Reuse existing LifeOS App Lock security only if separate diary lock is intentionally designed.
- No biometric-data access/storage.
- Request camera/microphone/location only on user action.
- Use Photo Picker/document picker instead of broad permissions.
- Third-party paywall cannot block diary core operations.

## 7. Suggested component boundaries

Adapt to existing code; do not over-fragment.

- Existing `DiaryScreen`, `DiaryEditor`, `DiaryEditorViewModel` remain orchestration points unless audit proves a need to split.
- `DiaryEditorTopBar`: Back, overflow, Save.
- `DiaryEditorDateMoodRow`: date/mood controls.
- `DiaryEditorCanvas`: page surface/content blocks.
- `DiaryEditorToolbar`: tool actions.
- `DiaryThemePickerSheet`: categories/local thumbnails.
- `DiaryMediaPickerSheet`: camera and photo/video import.
- `DiaryStickerPickerSheet`: local sticker palette.
- `DiaryListPickerSheet`: templates/insertion.
- `DiaryAudioPanel`: recording/import/playback.
- `DiaryDrawingEditor`: strokes/tools/undo/redo/Done.
- `DiaryRichContentRenderer`: persisted blocks and legacy fallback.
- Pure serializer/domain models remain outside UI; repositories handle persistence.

## 8. QA acceptance criteria

### Functional
- Current list/day/date/mood/title/body/tags/photos/recorded audio/location/search/calendar/insights/favorite/edit/delete continue working.
- Theme persists after close/reopen and app restart.
- Sticker position/scale persist and remain editable.
- List template inserts at caret without deleting adjacent text.
- Recording and imported audio play after restart.
- Drawing reopens/renders after restart.
- Imports survive source item deletion because copies are app-private.
- Double tap Save cannot create duplicate.
- Failed save leaves editor/draft available.
- First-entry celebration appears once only after durable create.
- Backup/restore preserves legacy and new rich entries/media.

### Regression/navigation
- Home → Diary → new entry → Save → detail/list → Back.
- Home → Diary → existing entry → edit → Save → Back.
- Diary → Search/Calendar/Insights → system Back.
- Composer IME visible → Back hides keyboard first; next Back retains existing dismiss behavior.
- Palette open → Back/outside closes palette without discarding draft.
- Preserve existing app navigation and bottom bar.
- Document draft recovery/autosave policy if changed.

### Visual
- Header/date/mood/title/body/toolbars match recorded hierarchy/spacing.
- Theme grid/categories scroll and select.
- Stickers transform without covering controls/text.
- Drawing canvas has reachable tools/colors.
- IME/system bars never overlap writing/toolbar.
- Dark mode and font scaling readable.
- No overflow/clipping in screenshots.

### Build/tests

Run exact project commands and report actual outcomes:
- `sh ./gradlew :app:compileDebugKotlin`
- `sh ./gradlew :app:testDebugUnitTest`
- `sh ./gradlew :app:assembleDebug`
- `sh ./gradlew :app:assembleDebugAndroidTest`
- `sh ./gradlew :app:lintDebug`
- `sh ./gradlew :app:connectedDebugAndroidTest` only with emulator/device.

## 9. Completion definition

Complete only when:
1. Reference states visible in the video are implemented or explicitly excluded with reason.
2. All existing data/features survive.
3. Rich content is durable, versioned, backed up and recoverable.
4. Offline/privacy requirements remain intact.
5. Navigation, keyboard, insets, accessibility and adaptive layout pass.
6. Relevant builds/tests have been executed.
7. UPDATE.md has a dated appended entry; its history is preserved.
