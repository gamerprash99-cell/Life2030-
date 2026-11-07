# LifeOS Diary Redesign — CLAUDE_HANDOFF.md

**Purpose:** Let a future Claude session continue this work without re-auditing or guessing.  
**Session date:** 2026-11-06 | **Branch:** `main` (HEAD `9947211`) | **Repo:** `/home/Life2030`

---

## 1. Where things stand

- Build (`compileDebugKotlin`, `assembleDebug`, `lintDebug`, `assembleDebugAndroidTest`) — **PASS**.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk` (~46 MB).
- Unit tests (`testDebugUnitTest`) — **FAIL TO COMPILE** due to pre-existing test-package mismatch (tests use `package com.lifeos.app.ui.diary.new`; no `diary.new` sources in main). **Not caused by this work.**
- Instrumentation tests — compile, not executed (no device/emulator/adb).
- Figma file created (`EcERw5RozoOh8fhf84Kafj`) + session opened, but **MCP rate-limited (Starter plan)** — no canvas content written.

## 2. Uncommitted working-tree change (PRESERVE)

`app/src/main/java/com/lifeos/app/ui/navigation/LifeOSNavHost.kt` is modified but **not committed**. It migrates Diary destinations from `diary.new` → `diary` and wires ViewModels via `LocalServiceLocator`/`LambdaViewModelFactory`. **Do not discard.** Either commit it intentionally or leave it uncommitted — but do not `git checkout`/`git restore` it.

## 3. Files added by this session (Diary redesign)

| File | State |
|---|---|
| `docs/diary-redesign/00-current-diary-audit.md` | Complete |
| `docs/diary-redesign/01-figma-design-spec.md` | Complete (documents Figma blocker) |
| `docs/diary-redesign/02-implementation-plan.md` | Complete |
| `docs/diary-redesign/03-feature-mapping.md` | Complete |
| `docs/diary-redesign/04-testing-report.md` | Complete |
| `docs/diary-redesign/05-final-review.md` | Complete |
| `docs/diary-redesign/CLAUDE_HANDOFF.md` | This file |
| `UPDATE.md` | Appended dated entry |
| `app/.../ui/diary/DiaryMediaSheet.kt` | Added — Camera + Photos & Videos sheet (currently a palette stub in `DiaryEditor.kt`) |
| `app/.../ui/diary/DiaryDrawingEditor.kt` | **Deleted** (failed compilation; do not re-add without fixing) |
| `app/.../ui/diary/DiaryAudioPanel.kt` | **Deleted** (failed compilation; do not re-add without fixing) |
| `app/.../ui/diary/DiaryEditor.kt` | Modified — DRAWING/MEDIA/AUDIO palettes are explicit stubs; `RichInlineLayer` + inline sticker/drawing rendering composables present but gated behind stubs |

## 4. Files deleted by this session (and why)

- `DiaryDrawingEditor.kt` — compile errors (`DrawScope`, `detectDragGestures` import path, `plus` operator on `List`, `width`/`height` function confusion). Rebuild carefully if needed.
- `DiaryAudioPanel.kt` — compile errors (`AudioPicker.launch(Intent)` receiver mismatch). Rebuild carefully if needed.

Both were created in this session and deleted before commit, so no history loss.

## 5. Data contract (no schema change)

- Room v7, unchanged. No migration.
- Rich content → existing `DiaryEntity.attachmentsJson` (default `"[]"`).
- `DiaryAttachment` sealed class already has ThemeStyle/Sticker/Drawing/ImportedAudio variants.
- `DiaryAttachments.decode()` is total (`runCatching → emptyList`).
- Existing entries byte-identical; no table dropped, no media deleted.

## 6. Remaining work (in order)

1. **Figma:** Retry MCP calls; build screens/tokens/components/prototype in `EcERw5RozoOh8fhf84Kafj` per `01-figma-design-spec.md`. Update the spec with frame/node IDs.
2. **Complete stubs:** Wire `DiaryEditorPalette.DRAWING` → drawing canvas, `MEDIA` → `DiaryMediaSheet` (camera + photo picker), `AUDIO` → `DiaryAudioPanel` (audio import). Make every visible control functional.
3. **Inline rich rendering:** Wire `RichInlineLayer` (stickers at normalized coords, drawing inline, theme background) into `DiaryEditor.kt` WritingSurface with legacy fallback.
4. **First-entry celebration:** Post-first-save sheet ("First Entry Saved!", optional 3-day challenge, reminder selector, dismiss); non-gating.
5. **Fix unit tests:** Re-parent test package `diary.new` → `diary`; update imports/call sites; run `:app:testDebugUnitTest`.
6. **On-device QA:** 14-step journey (create → persist → reopen/edit → theme/stickers → media → audio → drawing → permission denial → Back → Timeline → legacy entries). Requires device/emulator.
7. **Update UPDATE.md** with final results; complete `04-testing-report.md`/`05-final-review.md`.

## 7. Build/test commands (use the aapt2 override)

```sh
cd /home/Life2030
sh ./gradlew :app:compileDebugKotlin -Pandroid.aapt2FromMavenOverride=/home/Android/build-tools/35.0.0/aapt2
sh ./gradlew :app:assembleDebug -Pandroid.aapt2FromMavenOverride=/home/Android/build-tools/35.0.0/aapt2
sh ./gradlew :app:lintDebug -Pandroid.aapt2FromMavenOverride=/home/Android/build-tools/35.0.0/aapt2
sh ./gradlew :app:testDebugUnitTest -Pandroid.aapt2FromMavenOverride=/home/Android/build-tools/35.0.0/aapt2
# connectedDebugAndroidTest requires a device/emulator:
# sh ./gradlew :app:connectedDebugAndroidTest -Pandroid.aapt2FromMavenOverride=/home/Android/build-tools/35.0.0/aapt2
```

## 8. Key constraints (do not violate)

- No `INTERNET`, no cloud, no Firebase, no telemetry, no external AI, no ads, no paywall.
- No destructive Room migration; no DB delete/recreate; preserve existing entries/media.
- No Hilt/new module architecture; keep manual `ServiceLocator`.
- Permissions only at feature use; handle granted/denied/permanently denied.
- All media app-private; no MediaStore/public storage dependence.
