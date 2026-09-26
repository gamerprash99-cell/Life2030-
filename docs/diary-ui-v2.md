# LifeOS Diary UI v2 — Screen Flow

## Screen 1 — Empty Day
- Editorial daily-memory page with date strip and "+ Memory" action.
- Local Compose Canvas illustration: open journal, moon, stars and sprout.
- Copy hierarchy: "YOUR STORY STARTS HERE" → selected-day status → capture prompt → Start a memory.

## Screen 2 — New Memory / Keyboard
- Full-screen composer; Back closes only the composer.
- Mood-first selection with 8 existing stored mood keys.
- Keyboard-safe writing surface; navigation-bar and IME insets use a union so the save row is not double-padded.
- Open-time minute is captured before typing and remains stable while writing.

## Screen 3 — Filled Memory
- Same composer becomes a calm reading/writing page when text exists.
- Save is disabled for blank content and while the local Room write is in flight.
- Mood label and marker animate subtly without moving the writing surface.

## Screen 4 — Saved Confirmation
- Successful local save closes the composer and briefly shows a small "Memory saved" confirmation at the top of the Diary.
- Confirmation is driven by ViewModel state and automatically fades away after a short period.

## Screen 5 — Saved Memory Detail
- Existing DiaryDetailScreen route and ViewModel remain intact.
- Detail uses the same editorial spine/mood language, shows the stored day/time, supports Edit and Delete, and reuses the full-screen editor for editing.

## Screen 6 — Multiple Memories
- Selected day remains the unit of the page.
- Memories appear newest-first on the editorial spine with time gutter, mood marker, text, quiet tags and Edit/Delete actions.
- The date strip marks days containing memories and remains bounded to the existing one-year history window.

## Architecture / Privacy
- Room → repository → ViewModel → Compose remains the data path.
- No new network call, cloud service, telemetry, external AI, Room schema change, permission, or dependency is introduced by this UI pass.
- The empty-state illustration is local Canvas code rather than a downloaded asset.

## Verification
- Source changes are prepared on feature/diary-ui-v2-final.
- No Android emulator/device is exposed through the connected GitHub workflow, so visual device rendering is not claimed as verified.