# LifeOS Diary Redesign — Phase D: Implementation Plan

**Baseline verified state (from `00-current-diary-audit.md`):**
- The Diary data layer is complete and production-quality: `DiaryEntity`, `DiaryDao`, `DiaryRepository`, `DiaryAttachment` (with ThemeStyle/Sticker/Drawing/ImportedAudio variants), `DiaryAttachments` total codec, `MediaStorage`, backup/restore, search/calendar/insights, Timeline integration.
- The UI layer is substantially implemented but has **stubs**: `DiaryEditorPalette.DRAWING`, `MEDIA`, `AUDIO` are non-functional; inline rich-content rendering (stickers at normalized positions, drawings, theme background) is missing; camera capture and audio import are missing; first-entry celebration is missing.
- No Room schema change is required (rich content lives in `attachmentsJson`).
- Working tree has one preserved uncommitted change: `LifeOSNavHost.kt` migration `diary.new` → `diary`.

**Goal:** Complete the stubbed editor capabilities so every approved Figma screen/interaction is functional, following the Figma design and reference video, without dropping any existing capability.

---

## Plan — complete stubs and missing UI (no data-layer change needed)

### Step 1 — Drawing canvas (`DiaryDrawingEditor.kt`, new)
- Full-screen canvas from the editor toolbar (replace the `DRAWING` stub).
- Pen/marker/eraser-like tools, color swatches, undo/redo, Done.
- Stroke model (points, color, width, tool) persisted as an app-private PNG artifact (`MediaStorage.diaryDrawingsDir`/`newDiaryDrawingFile`).
- Done inserts a `DiaryAttachment.Drawing` (filePath, width, height) inline; survives restart/backup/edit.
- Never keep drawing only in transient Compose state.

### Step 2 — Media sheet (`DiaryMediaSheet.kt`, new)
- Replace `MEDIA` stub. Bottom sheet: Camera (CameraX `ImageCapture`, `CAMERA` permission on tap) and Photos & Videos (Android Photo Picker `PickVisualMedia`, ImageOnly, copy to app-private `diary-photos`).
- Preview, add/remove, missing-file state. No video in v1 (reference shows no playback; spec defers video until playback/rotation/backup/cleanup complete).

### Step 3 — Audio import panel (`DiaryAudioImportPanel.kt`, new)
- Replace `AUDIO` stub. Recording + Add Audio File (document/audio picker, copy to `diary-imported-audio`).
- Playback/pause/stop/duration, removal, missing-file state; `ImportedAudio` metadata (MIME, display name, duration).
- Microphone permission only on record start; no cloud transcription.

### Step 4 — Inline rich-content rendering (`DiaryEditor.kt` + `DiaryRichContentRenderer.kt`)
- Render theme background (per-entry `ThemeStyle`), stickers at normalized x/y/scale/rotation, drawings inline, photos, voice notes, imported audio, place.
- Legacy fallback: rows without rich payload render `content` + `attachmentsJson` unchanged; unknown/malformed payload shows legacy body + safe placeholders, never crashes.
- Do not eagerly rewrite old entries.

### Step 5 — Sticker placement interactivity
- Inserted sticker is movable/resizable with a delete handle (currently only insert-at-center).
- Persist stable assetKey, normalized x/y, scale, rotation.

### Step 6 — First-entry celebration
- After first durable save: "First Entry Saved!", optional 3-Day Diary Challenge (non-gating), reminder time selector (existing notification permission flow), dismiss. Subsequent saves use ordinary confirmation.

### Step 7 — Camera photo path
- `CAMERA` permission requested when user picks Camera; real capture via CameraX; copy to app-private storage.

### Step 8 — Tests + verification
- Unit tests for new UI/view-model methods; run `:app:compileDebugKotlin`, `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, `:app:lintDebug`.
- Connected instrumentation tests only with device/emulator (not available here).

**Architecture preserved:** UI → ViewModel → Repository → DAO → Room. Manual `ServiceLocator` DI. ViewModels expose `StateFlow`. File I/O on `Dispatchers.IO`. No Hilt, no new module, no network, no cloud AI.

---

## Data contract (no schema change)

| Concern | Decision |
|---|---|
| Rich-content storage | Existing `DiaryEntity.attachmentsJson` String column (default `"[]"`), extended codec |
| New attachment variants | ThemeStyle, Sticker, Drawing, ImportedAudio — already added to `DiaryAttachment.kt` |
| Codec | `DiaryAttachments.encode/decode` — total decode, `ignoreUnknownKeys`, `classDiscriminator = "kind"` |
| Schema version | Room v7 — unchanged |
| Migration | None required |
| Legacy rows | Decode totally; render `content` + `attachmentsJson` unchanged |
| Backup | `LifeOSBackup` includes `diaryEntries` (rows with attachment JSON); media files referenced by path, not bundled |
