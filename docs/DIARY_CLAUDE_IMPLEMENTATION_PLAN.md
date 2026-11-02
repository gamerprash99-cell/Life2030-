# Claude Implementation Plan — LifeOS Diary Redesign

> Read this plan, `DIARY_REDESIGN_SPEC.md` and `DIARY_REFERENCE_VIDEO_FRAME_AUDIT.md` before touching code.

## Mission

Implement the diary redesign guided by the reference recording while preserving LifeOS capabilities, architecture, user data, offline-first privacy and Git history. Target: richer full-screen editor with pastel styling, contextual tool palettes and durable rich content.

**Do not delete the old Diary module first.** Replace/retire individual pieces only after the new equivalent works, is tested and preserves behavior. Keep Room history and old attachment parsing backward compatible.

## Mandatory repository rules

1. Audit Git status, recent commits and current branch before editing.
2. Read README.md, complete UPDATE.md history, diary source, Room schema/migrations, backup/restore, navigation, permissions and tests.
3. Preserve Kotlin, Compose, Material 3, packages, manual DI, StateFlow ViewModels, repositories/use cases and Room.
4. No cloud AI, network endpoints, Firebase, analytics, telemetry or uploads.
5. Never use destructive Room migration or replace the database.
6. Preserve diary IDs, content, mood, tags, date/time, favorites, AI flags, attachments, media paths and backup compatibility.
7. Do not remove search/calendar/insights/location/weather/current photo and voice-note support.
8. Do not replace Navigation Compose or alter global bottom navigation.
9. Request permissions only at feature use.
10. Never claim visual exactness or test success without verification.

## Phase A — Audit (no edits)

Read and map call sites/dependencies:
- `ui/diary/DiaryScreen.kt`
- `ui/diary/DiaryEditor.kt`
- `ui/diary/DiaryEditorViewModel.kt`
- `ui/diary/DiaryEditorAttachments.kt`
- every other `ui/diary/*`
- `domain/model/DiaryAttachment.kt`, mood/date/weather models
- `data/db/entities/DiaryEntity.kt`
- `data/db/dao/DiaryDao.kt`
- `data/repository/DiaryRepository.kt`
- `data/db/AppDatabase.kt` and exported schemas
- `data/repository/BackupRepository.kt` and backup serialization
- `core/util/MediaStorage.kt`, `PermissionManager.kt`
- `core/media/*`, `core/location/*`
- `ui/navigation/LifeOSNavHost.kt`, route declarations and chrome/inset helpers
- Diary unit/UI tests
- README.md, UPDATE.md, database/frontend/design docs

Create path-level dependency map. Identify ownership for theme, mood, photos, voice recording/playback, location, save, detail, list/calendar/search/insights, backup and navigation.

## Phase B — UI foundation

Implement reference composition in Compose:
- Editor top bar Back / overflow / Save.
- Date and mood row/panels.
- Title/body page canvas.
- Compact anchored toolbar above IME/system navigation.
- Reusable contextual palette-sheet pattern.
- Correct keyboard-safe responsive/scroll behavior.

Use current diary theme/components and LifeOS design tokens; avoid one-off magic coordinates. Keep Save/Back behavior and editor callbacks. Add UI tests and screenshot review before proceeding.

## Phase C — Rich content data contract

Design and test before tool UI:
1. Define versioned rich diary document model + serializer, or additive Room field with explicit migration.
2. Stable discriminator and schema version; safely preserve unknown block payloads where possible.
3. Legacy fallback: old content + attachmentsJson render unchanged if rich payload null/empty/malformed/unsupported.
4. Persist ordered content blocks; do not keep stickers/drawings only in Compose state.
5. Store imports under app-private directories with collision-safe names and atomic writes.
6. Repository remains persistence/file lifecycle boundary.
7. Add migration, serializer, repository and backup round-trip tests before rich formats ship.

Do not add migration until current schema and migration chain are fully audited. If extending existing JSON envelope can avoid schema safely, document the choice first.

## Phase D — Implement tools one at a time

1. Themes/background picker using local bundled resources; persist per-entry theme.
2. Media sheet: camera and Android Photo Picker; app-private photo copies.
3. Sticker palette: local assets, normalized placement/scale, accessible transform/delete.
4. List templates: insertion at current caret/selection and editable text.
5. Audio panel: preserve recorder/player; add document-picker import and private copy.
6. Drawing: vector/stroke model with color/tool/undo/redo; Done inserts persisted drawing.
7. Optional video only after storage, rotation/aspect ratio, playback/fullscreen, backup and cleanup are complete.

Each tool must support add, render, edit, remove, save, reopen, process restart, export/restore and missing-file behavior.

## Phase E — Save and first entry

- Existing ViewModel → DiaryRepository → DiaryDao flow.
- Progress state; prevent duplicate save.
- Show success only after DB write completes.
- First-entry celebration only after first durable save; later saves use ordinary confirmation.
- Challenge/reminders optional and local, not a paywall.
- Reminder requires explicit opt-in and current notification permission flow.

## Phase F — Backup and restore

- Audit actual backup format and restore behavior.
- Backward-compatible backup version only if required.
- Include rich metadata/media bytes, particularly drawings/imported audio/custom assets.
- Restore into staging, validate, commit safely; clean staging on failure.
- Keep old backup import working.
- Test old/new round-trip.

## Phase G — Verification

Build/test each meaningful increment:
- `sh ./gradlew :app:compileDebugKotlin`
- `sh ./gradlew :app:testDebugUnitTest`
- `sh ./gradlew :app:assembleDebug`
- `sh ./gradlew :app:assembleDebugAndroidTest`
- `sh ./gradlew :app:lintDebug`
- Connected tests only with device/emulator.

Manual QA:
- 320/360/411dp widths; large display; landscape; 1.3+ font size.
- Gesture and 3-button navigation.
- Keyboard shown/hidden during editor and palette use.
- Existing entry edit/save; historic date; optional mood; tags.
- Photo import/remove; record/play voice; location denied/permanently denied.
- Theme/sticker/list/audio import/drawing persistence.
- Restart, migration, backup/restore.
- Home → Diary → editor → save → detail/list → Back.
- Diary → search/calendar/insights → system Back.
- No network requests/permission/API key.

## Phase H — Documentation and final report

Append a dated entry to UPDATE.md; never overwrite history. Update feature/database/frontend/design docs only where verified. Report files, migration/data design, security/offline, exact commands/results, remaining issues and readiness based only on verified results.

## Stop conditions

Stop and fix before proceeding if:
- legacy diary rows/media paths cannot be proven safe;
- destructive migration is proposed;
- UI/ViewModel bypasses repository;
- palette interactions can discard draft;
- files are deleted before successful DB commit;
- backup drops rich content;
- nav stack/bottom tabs change unexpectedly;
- core feature needs cloud/API/network.

## Execution instruction for Claude

Do not start by deleting the old Diary module. Produce audit and file plan, implement new components alongside existing flows, route Diary through replacement only after parity/tests, then retire obsolete code only after proving no call sites/data formats/tests depend on it. Keep a small, reviewable diff per phase.
