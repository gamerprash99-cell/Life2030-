# LifeOS Diary Redesign — Phase D: Feature Mapping (Capability Parity)

Each audited Diary capability is mapped to: **Existing** (already works), **New** (to be implemented), **Partial** (model/UI exists but needs completion), or **N/A** (documented reason not applicable). No capability is silently dropped.

| # | Capability | Status | Mapping / reason |
|---|---|---|---|
| 1 | Diary entry list and chronological browsing | Existing | `DiaryScreen` + `DiaryViewModel.observeForDay`, newest-first |
| 2 | Create, edit, save, reopen, delete (confirmed) | Existing | `startNewEntry`/`startEdit`/`save`/`delete` + `MemoryDeleteDialog` |
| 3 | Date/time handling and stable ordering | Existing | `dateEpochDay` + `timeMinutes`; editor preserves minute on re-save |
| 4 | Mood/emotion selection and persistence | Existing | `DiaryMoods`, `DiaryMoodSelector`, `mood` column |
| 5 | Title and rich-text body editing | Existing (title/body); rich text New | Title (120 cap) + body (1000 cap); rich blocks New (§inline rendering) |
| 6 | Text formatting and keyboard/toolbar behavior | Existing | Bottom toolbar, IME insets, two-stage Back |
| 7 | Backgrounds/themes per entry | Partial | `ThemeStyle` variant + `DiaryThemePickerSheet` exist; **inline background rendering missing** |
| 8 | Stickers/emojis with position/scale/rotation | Partial | `Sticker` variant + picker exist; **move/resize/delete-after-place UI missing** |
| 9 | List templates | Partial | `DiaryListPickerSheet` exists (appends marker text); caret-aware insertion New |
| 10 | Camera capture | New | Only Photo Picker exists; CameraX capture + `CAMERA` permission to add |
| 11 | Photo/video selection | Existing (photo) / N/A (video) | Photo Picker ImageOnly works; video deferred (no playback/rotation/backup/cleanup yet) |
| 12 | Audio recording, playback | Existing | `DiaryAttachment.VoiceNote`, recorder/player |
| 13 | Import existing audio file | New | `ImportedAudio` variant exists; document picker + copy + playback UI missing |
| 14 | Drawing/signature create/edit/persist | New | `Drawing` variant exists; canvas UI + artifact persistence missing |
| 15 | Entry media preview, open, removal | Existing | Photo strip + voice row with remove; `removeAttachment` in repository |
| 16 | Diary lock/security settings | N/A (not in LifeOS scope) | Reference shows Diary Lock/password/recovery; LifeOS uses existing App Lock (PIN) only if intentionally designed. Not copied; no insecure recovery mechanism. |
| 17 | Search/filter/calendar/insights | Existing | `DiarySearchScreen`, `DiaryCalendarScreen`, `DiaryInsightsScreen` + use cases |
| 18 | Reminder/challenge flows | New (challenge/reminder) | First-entry challenge + reminder selector (non-gating, explicit opt-in, existing notification flow) |
| 19 | Empty, permission-denied, interrupted, failure states | Existing (partial) | Empty state + inline errors exist; permission-denied states exist for mic/location; **interrupted save/draft recovery to document** |
| 20 | Timeline integration | Existing | `BuildLifeTimelineUseCase` maps `TimelineItemType.DIARY` → `DiaryDetail` |

## New items to implement (from stubs/missing)

| Item | File(s) | Notes |
|---|---|---|
| Drawing canvas | `DiaryDrawingEditor.kt` (new) | Stroke model → app-private PNG artifact |
| Camera capture | `DiaryMediaSheet.kt` (new) | CameraX, `CAMERA` on tap |
| Audio import | `DiaryAudioImportPanel.kt` (new) | Document picker, `ImportedAudio` |
| Inline rich rendering | `DiaryEditor.kt`, `DiaryRichContentRenderer.kt` (new) | Theme, stickers normalized coords, drawings, legacy fallback |
| Sticker move/resize/delete | `DiaryEditor.kt` + `DiaryStickerLayer` | Transform handles |
| Caret-aware list insertion | `DiaryEditor.kt` | Insert at caret, preserve adjacent text |
| First-entry celebration | `DiaryEditorViewModel`/`DiaryScreen` | Post-save sheet, non-gating |
| Reminder time selector | New (uses existing notification flow) | Explicit opt-in |

## Items verified as already working (no change)

Entry list, create/edit/save/delete, date/time, mood, title/body, tags, photo (picker), voice record/playback, place, weather, search, calendar, insights, favorite, share/copy, backup/restore of diary rows, timeline integration, permissions on demand, app-private media.

## Items explicitly out of scope (documented reasons)

| Item | Reason |
|---|---|
| Video capture/playback | Reference shows no playback; spec defers until storage/rotation/backup/cleanup complete |
| Diary Lock password/security question | Third-party insecure recovery mechanism; not LifeOS scope |
| Paywall/subscription/Pro screens | No monetization in LifeOS core diary |
| Cloud sync / remote backup / Firebase / analytics / telemetry / INTERNET | Offline-first privacy principle |
| External AI for diary formatting | Local deterministic intelligence only |
