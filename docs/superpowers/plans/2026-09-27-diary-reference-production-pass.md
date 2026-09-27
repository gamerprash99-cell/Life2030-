# Diary reference production pass — 2026-09-27

## Goal
Bring the Diary implementation materially closer to the supplied four reference screens while preserving the current Compose → ViewModel → Repository → Room architecture and all existing offline/data contracts.

## Scope
- Day/timeline screen
- Memory editor
- Save confirmation
- Memory detail
- Diary visual tokens and reusable diary components
- Diary tests and UPDATE.md
- No app-wide navigation rewrite
- No Room schema changes
- No new network/cloud/AI dependencies or permissions

## Reference requirements
1. Day list: TODAY/date masthead, 5-day strip, timeline spine, mood labels, attachment thumbnails, bottom navigation + floating add.
2. Editor: explicit Date + Time row, mood selector, large writing area, photos, tags, save action above IME.
3. Confirmation: saved-memory illustration/card with View memory and Add another memory.
4. Detail: mood/date/time header, content card, photo gallery, location/weather section, tags, metadata, Share/Copy/Delete/Favorite actions.

## Existing production contracts to preserve
- DiaryEntity and Room v5 remain unchanged.
- Attachments remain in attachmentsJson and app-private storage.
- Existing photo picker, recorder/player, platform location provider and honest offline weather state remain.
- Existing day selection/history bounds remain.
- Existing delete/storage race fixes remain.
- Existing navigation route diary/{entryId} remains.

## Verification
Because the connected GitHub environment does not expose a local Android build runtime, source-level verification is required here and any build claim must come from an actual CI/device run.