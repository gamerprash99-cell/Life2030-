# LifeOS Diary Redesign — Phase A: Current Diary Audit

**Source repository:** `gamerprash99-cell/Life2030-` at `/home/Life2030`  
**Branch / commit:** `main`, HEAD `9947211` ("Merge pull request #64 ...")  
**Working tree:** one uncommitted change — `app/src/main/java/com/lifeos/app/ui/navigation/LifeOSNavHost.kt` (a partial migration from the `diary.new` package back to `diary`; see §Uncommitted work).  
**Audited:** 2026-11-06 — all claims below verified against checked-out source, not assumed from README/docs lag.

---

## 1. Existing Diary specs/documents — currency status

| Document | Status | Notes |
|---|---|---|
| `docs/DIARY_REDESIGN_SPEC.md` | Current (design target) | Describes the redesign target; rich-content contract §5 matches what was already implemented in the attachment model. |
| `docs/DIARY_REFERENCE_VIDEO_FRAME_AUDIT.md` | Current | Frame-by-frame audit of `1000276437.mp4` (~85s). Lists states/screens observed; flags paywall, subscription, cloud, Pro features as out of scope. |
| `docs/DIARY_CLAUDE_IMPLEMENTATION_PLAN.md` | Current | Phased plan; no code executed yet per this plan (predates 2026-11-02 work). |
| `TODO_DIARY_REDESIGN.md` | Current | 10-phase todo; phases 1–9 largely reflected in working tree already. |
| `UPDATE.md` — 2026-11-02 entry | Current (describes working tree) | Documents the Diary attachment-model + UI work already in the tree (ThemeStyle, Sticker, Drawing, ImportedAudio variants; Theme/Sticker/List picker sheets; no schema change). History preserved. |
| `README.md` | Current | Describes the single-module `:app`, Room+SQLCipher v7, manual DI, Compose+M3. Diary section matches verified baseline. |
| `docs/05_DATABASE.md` | Current | Full schema; `diary_entries` table documented. |
| `docs/09_FRONTEND.md` | Current | Frontend contract; matches working tree. |
| `docs/DESIGN.md` | Current | Design-system tokens. |
| `docs/04_FEATURES.md` | Current | Feature inventory. |

No documentation was deleted or overwritten during this audit.

---

## 2. Existing Diary screens and navigation

All screens live in package `com.lifeos.app.ui.diary` (main source). Navigation Compose single `NavHost` in `LifeOSNavHost.kt`; routes in `Screen.kt`.

| Screen/File | Route | Role |
|---|---|---|
| `DiaryScreen.kt` | `diary` | Day-scoped list/home. Date header, entry cards, empty state, save-confirmation sheet, delete dialog, opens inline editor. |
| `DiaryEditor.kt` | (in-place branch inside `diary` / `diary/{entryId}`) | Full-screen composer: top bar Back/Save, date/time strip, title + ruled body surface, character counter, mood, attachments footer, contextual tool palettes (Theme, Sticker, List wired; Drawing/Media/Audio are **stubs**), date/time picker dialogs. |
| `DiaryDetailScreen.kt` | `diary/{entryId}` | Full-page entry view: date/mood, body, photos, place/weather, tags, meta rows, share/copy/delete/favorite, opens inline editor for edit, inline editor overlay for rich actions. |
| `DiarySearchScreen.kt` | `diary/search` | Query + mood-filter search over title/content/tags. |
| `DiaryCalendarScreen.kt` | `diary/calendar` | Month grid with entries-per-day dots, prev/next month. |
| `DiaryInsightsScreen.kt` | `diary/insights` | Streak, stats, mood mix, 7-day mood chart, recurring words, reflection prompt. |

Navigation destinations (`Screen.kt`): `Diary`, `DiarySearch`, `DiaryCalendar`, `DiaryInsights`, `DiaryDetail`. Diary composer is an **in-place branch** inside `DiaryScreen`/`DiaryDetailScreen` (not a separate route), owned by `ComposerWindowOwner` so the bottom bar is hidden and the composer takes the full window.

Diary entry points: Home `onOpenDiary`, AI Assistant `AiAction.DIARY`, Global Search `onOpenDiary`, Life/Timeline item routing (`TimelineItemType.DIARY` → `DiaryDetail`).

---

## 3. Existing capabilities and feature inventory (verified in source)

| Capability | Implementation | Status |
|---|---|---|
| Entry list, day-scoped browsing, newest-first | `DiaryViewModel` + `observeForDay` | Present |
| Create / edit / save / reopen / delete | `DiaryEditorViewModel.start()` / `save()` / `delete()` | Present |
| Date/time handling, stable ordering | `dateEpochDay` + `timeMinutes`, ordered `DESC` | Present |
| Mood selection + persistence | `DiaryMoods`, `DiaryMoodSelector`, `mood` column | Present |
| Title + body editing | `title` column (120 char cap), `content` (1000 char cap) | Present |
| Tags | `tagsCsv`, `DiaryTagEditor` | Present |
| Photo attachment | `DiaryAttachment.Photo`, `DevicePhotoImporter`, Photo Picker | Present |
| Voice note record/playback | `DiaryAttachment.VoiceNote`, `DiaryAudioRecorder`/`DiaryAudioPlayer` | Present |
| Place attachment | `DiaryAttachment.Place`, `LocationProvider`, `Geocoder` | Present |
| Weather | `WeatherRepository`, honest `Unavailable` state | Present |
| Search | `DiaryRepository.search()` (LIKE), `DiarySearchScreen` | Present |
| Calendar month grid | `getAllInRange`, `DiaryCalendarScreen` | Present |
| Local insights | `GetDiaryInsightsUseCase`, `DiaryInsightsScreen` | Present |
| Favorite | `isFavorite`, `toggleFavorite` | Present |
| Share / copy | `shareEntry`, `copyEntry` | Present |
| Backup/restore (diary rows) | `BackupRepository` includes `diaryEntries`, upsert by PK | Present |
| Timeline integration | `BuildLifeTimelineUseCase` maps `TimelineItemType.DIARY` | Present |
| App-private media | `MediaStorage` (`diary-photos`, `diary-audio`, …) | Present |
| Permissions on demand | `PermissionManager`, camera/mic/location at feature use | Present |
| Rich-content attachment variants (ThemeStyle, Sticker, Drawing, ImportedAudio) | `DiaryAttachment.kt` extended, `attachmentsJson` codec | **Present in model only; UI stubbed** (see §Stubs) |
| Theme picker sheet | `DiaryThemePickerSheet` | Present (wired) |
| Sticker picker sheet | `DiaryStickerPickerSheet` + `DiaryStickerLayer` | Present (wired; insert only — no move/resize UI) |
| List picker sheet | `DiaryListPickerSheet` | Present (wired; appends marker text) |
| Drawing canvas | **None** — `DiaryEditorPalette.DRAWING` is a stub | **Missing** |
| Camera capture | **None** — only Photo Picker (ImageOnly) | **Partial** |
| Imported audio (document picker) | **None** — model exists, UI missing | **Missing** |
| Inline rich-content rendering | **None** — attachments render in legacy rows only | **Missing** |
| First-entry celebration / challenge / reminder | **None** in current UI | **Missing** |

### Stubbed palettes in `DiaryEditor.kt`
```kotlin
enum class DiaryEditorPalette { NONE, THEME, STICKER, DRAWING, LIST, MEDIA, AUDIO }
```
- THEME → `DiaryThemePickerSheet` (wired)
- STICKER → `DiaryStickerPickerSheet` (wired, insert-at-center only; no move/resize/delete-after-place)
- DRAWING → `openPalette = NONE` (stub)
- LIST → `DiaryListPickerSheet` (wired, appends text)
- MEDIA / AUDIO → `Unit` (stub)

---

## 4. Relevant source files and responsibilities

**UI (`app/src/main/java/com/lifeos/app/ui/diary/`):**
- `DiaryScreen.kt` — list/home, save sheet, delete dialog, editor orchestration
- `DiaryEditor.kt` — composer, palettes, date/time pickers, save action
- `DiaryDetailScreen.kt` (+ inner `DiaryDetailViewModel`) — detail view, edit entry, remove attachment
- `DiaryEditorViewModel.kt` — draft state, save, audio, location, rich-content (theme/stickers/drawing/imported audio) methods
- `DiaryEditorAttachments.kt` — permission triggers (photo/mic/location), action bar, attachments rows
- `DiaryThemePickerSheet.kt`, `DiaryStickerPickerSheet.kt`, `DiaryListPickerSheet.kt` — picker sheets
- `DiaryComponents.kt` — `DiaryPanel`, `DiarySectionLabel`, `DiaryPhotoStrip`, `DiaryVoiceNoteRow`, `DiaryLocationRow`, `DiaryWeatherRow`, `DiaryTagEditor`
- `DiaryDateStrip.kt`, `DiaryDayHeader.kt`, `DiaryEmptyState.kt`, `DiaryPaper.kt`, `DiaryMoodSelector.kt`, `DiaryMoodVisuals.kt`, `DiaryMotion.kt`, `DiaryTitleField.kt`, `MemoryDeleteDialog.kt`, `MemoryTimeline.kt`, `MoodDot.kt`
- `DiarySearchScreen.kt` + `DiarySearchViewModel.kt`, `DiaryCalendarScreen.kt` + `DiaryCalendarViewModel.kt`, `DiaryInsightsScreen.kt` + `DiaryInsightsViewModel.kt`

**Data layer:**
- `data/db/entities/DiaryEntity.kt` — `diary_entries` table (v1 columns + `attachmentsJson`)
- `data/db/dao/DiaryDao.kt` — upsert/delete/observe/search/range queries
- `data/repository/DiaryRepository.kt` — persistence boundary, media lifecycle, orphaned-media cleanup
- `domain/model/DiaryAttachment.kt` — sealed class + `DiaryAttachments` codec (total decode)
- `domain/model/DiaryMood.kt`, `DiaryWeather.kt`, `DiaryTextStats.kt`
- `domain/usecase/DiarySearchUseCases.kt`, `GetDiaryInsightsUseCase.kt`
- `domain/intelligence/DiaryAnalyzer.kt`, `DiaryInsights.kt`, `DiarySnapshot.kt`

**Infrastructure:**
- `data/db/AppDatabase.kt` — Room v7, SQLCipher, migrations 1→2 … 6→7
- `data/repository/BackupRepository.kt` — `LifeOSBackup` v2, export/import/restore
- `core/util/MediaStorage.kt` — app-private dirs + `deleteIfExists`
- `core/util/PermissionManager.kt` — permission helpers + settings intents
- `core/di/ServiceLocator.kt` — manual DI, `diaryRepository`, diary use cases
- `ui/navigation/LifeOSNavHost.kt` + `ui/navigation/Screen.kt` — Navigation Compose
- `ui/theme/` — design tokens incl. Diary tokens (`DiaryActionViolet`, `DiaryPaperBase`, …)

**Tests (`app/src/test/java/com/lifeos/app/ui/diary/`):**
All declare package `com.lifeos.app.ui.diary.new` (stale package name, pre-migration): `DiaryViewModelTest`, `DiaryEditorViewModelTest` (implied), `DiaryDetailViewModelTest`, `DiarySearchViewModelTest`, `DiaryCalendarViewModelTest`, `DiaryInsightsViewModelTest`, `DiaryEditorAccessibilityTest`, `DiaryEditorLayoutTest`, `DiaryLayoutClearanceTest`, `DiaryLocationOfferTest`, `DayStripRangeTest`. Need to verify they compile against `diary` package sources.

---

## 5. Room schema, database version, migrations

- **Database:** `app/src/main/java/com/lifeos/app/data/db/AppDatabase.kt`, Room 2.6.1 on SQLCipher (`net.zetetic:sqlcipher-android:4.6.1`)
- **Schema version:** `7` (`version = 7`, `exportSchema = true`)
- **Schema exports committed:** `app/schemas/com.lifeos.app.data.db.AppDatabase/6.json` and `7.json`
- **Migrations:** `MIGRATION_1_2` … `MIGRATION_6_7` (additive indexes + v6→v7 habit tables). No diary-specific migration needed for the attachment-model work because rich content lives in the existing `attachmentsJson` String column (default `"[]"`).
- **Destructive fallback:** none. Any future schema change requires an explicit `Migration`.

The `diary_entries` table columns: `id` (PK), `title`, `content`, `mood`, `tagsCsv`, `dateEpochDay`, `timeMinutes`, `aiGenerated`, `isReviewed`, `attachmentsJson` (default `"[]"`), `isFavorite`, `createdAt`, `updatedAt`.

**No schema change is required** for the redesign because all new rich types serialize into `attachmentsJson`.

---

## 6. Current persistence/media/permission flows

- **Persistence:** UI → ViewModel → `DiaryRepository` → `DiaryDao` → Room. All writes on `Dispatchers.IO` (repository-level `withContext(io)`). Reads are `Flow`.
- **Media:** Photos copied to app-private `filesDir/diary-photos/` via `DevicePhotoImporter` (Photo Picker `PickVisualMedia`, ImageOnly). Voice notes recorded to `filesDir/diary-audio/` via `MediaRecorder`. Drawings/imported audio dirs exist in `MediaStorage` but have no writers yet.
- **Permissions:** `CAMERA` (not declared for diary photo import — uses Photo Picker), `RECORD_AUDIO` requested on record tap, `ACCESS_COARSE/FINE_LOCATION` on add location. Handled granted/denied/permanently denied via `PermissionManager` + `openAppSettings`/`openLocationSettings`. No `INTERNET` declared.
- **Backup/restore:** `LifeOSBackup` JSON (formatVersion 2) includes `diaryEntries: List<DiaryEntity>`. Restore upserts by primary key. Media files are NOT included in backup (only paths) — a known limitation for photos/voice/drawings/imported audio.

---

## 7. Existing tests and known issues

- Unit tests exist for Diary (package `diary.new`, pre-migration). Last full run (2026-10-31): 497 tests, 0 failures.
- Instrumentation tests: `AppDatabaseMigrationTest` (v6→v7), `ExpenseCaptureQueriesTest` — compile but require device/emulator; not executed in this environment.
- Known issues (README §19 / `docs/16_KNOWN_ISSUES.md`): no release keystore; backup restore verification needs device; AI Assistant lacks real context; instrumentation tests not executed; no manual on-device pass for several flows.
- AAPT2 daemon startup failure blocks `:app:processDebugResources` in this container (workaround: `-Pandroid.aapt2FromMavenOverride=/home/Android/build-tools/35.0.0/aapt2`).

---

## 8. Uncommitted work (must be preserved)

`LifeOSNavHost.kt` is modified in the working tree but **not committed**. The diff migrates Diary destinations from `com.lifeos.app.ui.diary.new.*` (which does NOT exist) to `com.lifeos.app.ui.diary.*` (which exists and matches main sources), and wires ViewModels via `LocalServiceLocator` + `LambdaViewModelFactory`. This is a **partial, correct migration** that must be kept — it makes the working tree consistent with the actual `diary` package. Do not discard it.

---

## 9. Proposed files to add/change/remove, with reasons

| File | Action | Reason |
|---|---|---|
| `docs/diary-redesign/00-current-diary-audit.md` | Add | This audit (Phase A deliverable). |
| `docs/diary-redesign/01-figma-design-spec.md` | Add | Figma file URL, screen inventory, tokens, interactions (Phase C). |
| `docs/diary-redesign/02-implementation-plan.md` | Add | Implementation plan & stubs-to-complete mapping. |
| `docs/diary-redesign/03-feature-mapping.md` | Add | Capability parity checklist mapping each item to existing/new/N/A. |
| `docs/diary-redesign/04-testing-report.md` | Add | Exact build/test commands and results (Phase F). |
| `docs/diary-redesign/05-final-review.md` | Add | Final review confirmation (Phase G). |
| `docs/diary-redesign/CLAUDE_HANDOFF.md` | Add | Handoff for next session. |
| `UPDATE.md` | Append | Dated entry preserving history. |
| `DiaryEditor.kt` | Change | Complete stubs: Drawing, Media(camera), Audio(import) palettes; inline rich-content rendering. |
| New: `DiaryDrawingEditor.kt` | Add | Stroke-based drawing canvas + Done → persisted artifact. |
| New: `DiaryMediaSheet.kt` | Add | Camera + Photos&Videos sheet (Photo Picker). |
| New: `DiaryAudioImportPanel.kt` | Add | Document/audio picker import + playback. |
| `DiaryEditor.kt` rendering | Change | Render stickers (normalized coords), drawings, theme background inline; legacy fallback. |
| `DiaryAttachment.kt` | No change | Model already complete. |
| `DiaryEditorViewModel.kt` | Possibly add | Camera photo flow, audio import, drawing artifact methods if not already present. |
| `DiaryRepository.kt` | Possibly add | Media lifecycle for drawing/imported audio/files (already partially present). |
| `BackupRepository.kt` | Change | Include rich media bytes if backup format extended. |
| Nothing | Delete | No files deleted (data-safety rule). |

---

## 10. Risks and data-compatibility plan

**Data safety (critical):** No Room table/drop/recreate. No `INTERNET`. All new rich content serializes into existing `attachmentsJson` (no schema change). Old rows decode totally (`runCatching → emptyList`) and render legacy body + attachments unchanged. No eager rewrite of old entries.

**Risk — stubs:** Drawing/Media/Audio palettes and inline rich rendering are missing; without them the reference-video capabilities (camera, audio import, drawing canvas, inline stickers/drawings) are not functional.

**Risk — test package mismatch:** Tests use package `diary.new`; main sources use `diary`. Verify tests still compile/reference correct symbols.

**Risk — AAPT2:** Build blocked in container without aapt2 override flag.

**Backup media:** Photos/voice/drawings/imported audio are referenced by path only; backup does not bundle media bytes. Restore on a device without the files shows missing-file states. Documented limitation; acceptable for v1 (no destructive change).

**Compatibility:** `DiaryAttachments.decode` tolerates unknown keys (`ignoreUnknownKeys`) and malformed input (total decode). `DiaryThemes.byKey` falls back to `ORIGINAL`. `DiaryStickers.byKey` falls back to placeholder. Safe rollback path exists.
