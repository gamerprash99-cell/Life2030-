# Diary Reference Video — Frame-by-Frame Audit

**Source:** user-supplied `1000276437.mp4`  
**Duration:** approximately 84.93 seconds (4,363 frames at 51.37 fps).  
**Inspection method:** sampled the complete clip at one-second intervals. Timecodes below describe the visible states in the recording; they are not claims about hidden app internals.

## 1. Visual direction

- Full-screen, very light pink diary canvas; the editor fills the usable screen.
- Compact top row: Back, overflow/more menu, prominent pink Save action.
- Date row in a handwritten/serif-like display treatment; mood emoji control on the right.
- A quiet, minimal title field and body placeholder (“Write more here...”).
- A slim floating/anchored editor toolbar near the lower edge.
- Tool selection opens contextual palettes/sheets rather than navigating away from the entry.
- Reference visual assets include pink/soft pastel surfaces, pale borders, emoji, sticker illustrations and thumbnail grids.
- Toolbar must remain reachable with keyboard open; content remains editable.

## 2. Timeline of observed states

| Time | Observed screen/state | Interaction or requirement evidenced |
|---|---|---|
| 00–04s | App splash / diary brand screen | Launch transition only; not part of diary editor scope. |
| 04–06s | “Let's Go Pro!” subscription paywall | Promotional gate appears before the diary; LifeOS must not add a paywall to core offline diary capability. |
| 07–08s | Diary landing/list | Pink illustrated header, top menu/search controls, date-based entries, mood emoji on rows, add action at bottom centre. |
| 09–10s | Side navigation drawer | Brand header; Upgrade to PRO, Theme, Tags, Diary Lock, Backup & Restore, Export & Import, Donate, Share App, Follow Us, More Apps, Help Center, Settings. |
| 11–12s | Set Diary Lock | Diary Lock toggle, set password, security question, recovery email, fingerprint toggle. Exact security behavior is not visible; do not copy insecure recovery mechanisms. |
| 13–14s | Drawer reopened | Menu entries repeated; no additional behavior observed. |
| 15s | Diary list again | Entry list with mood indicators and bottom actions. |
| 16–18s | Mood chooser | “How's your day?” panel with emoji moods; selection before returning to editor. |
| 19s | New diary editor | Back, overflow, Save; selected date, mood icon, Title and body placeholder; toolbar anchored near bottom. |
| 20–22s | Date picker | Calendar month grid, selected day, Cancel and Done; selection can be changed. |
| 23s | Keyboard open in editor | Date/title/body editable; keyboard does not obscure active writing area. |
| 24–28s | Theme/background palette | Categories such as Customize, Popular, Dark, Artsy, Cute and other tabs; scrollable grid of background thumbnails; explicit selected/apply affordance. |
| 29–31s | Theme palette | Further categories and background choices, including dark/night variants. |
| 32–33s | Dark category | Dark paper/background thumbnails. |
| 34–35s | Artsy category | Illustrated/patterned choices. |
| 36–37s | Cute category | Pastel and illustrated choices. |
| 38–39s | Lifestyle category | Lifestyle/seasonal thumbnail grid. |
| 40–41s | Theme apply/return | Visible apply/check affordance; returns to editor. |
| 42s | Editor with toolbar | Palette closed; editor remains on same entry. |
| 43–46s | Add Media bottom sheet | Camera and Photos & Videos options; sheet can be dismissed. |
| 47s | Editor toolbar | Media sheet closed; editor remains intact. |
| 48–51s | Sticker palette | Category strip and sticker/emoji collection; sticker inserted as independently movable/resizable object with delete/transform handles. |
| 52–53s | Editor with sticker | Sticker remains on page after palette closes. |
| 54–58s | List palette | Multiple list styles (bullets, numbered, checkbox-like and decorative markers). |
| 59–62s | List palette and feedback | More templates; toast says “Please insert the bullet list into the main text.” This suggests selected template inserts into editable text. |
| 63–68s | Text/markup toolbar and typing | Toolbar expands. Inline formatting/markup-like controls and hashtag/tag affordance visible; user types a short string. Exact semantics of every icon are not legible. |
| 69s | Editor state | Keyboard closes; text remains in draft. |
| 70–73s | Recording/audio panel | “Recording” section with Recording and Add Audio File actions. Playback UI/metadata are not shown. |
| 74–78s | Drawing canvas | Full-screen drawing canvas with undo/redo, color swatches, pen/marker/eraser-like tools and Done. |
| 79–81s | Drawing inserted | Completed drawing appears inline in diary page; body remains editable. |
| 82–84s | Save / success feedback | “First Entry Saved!” celebration, 3-Day Habit Challenge and reminder time selector. Diary-saved toast visible. This is a first-entry state, not proven recurring save behavior. |

## 3. Reference capabilities checklist

### Diary home/list
- [x] Date-oriented entry list.
- [x] Mood emoji displayed on entries.
- [x] New-entry action.
- [x] Search affordance visible.
- [x] Overflow/navigation menu.
- [ ] Exact search behavior not demonstrated.
- [ ] Exact sort/filter behavior not demonstrated.
- [ ] Entry detail/edit/delete behavior not demonstrated.

### Editor core
- [x] Back, Save and overflow affordance.
- [x] Entry date and date picker.
- [x] Mood chooser.
- [x] Title and body.
- [x] Keyboard-aware writing.
- [x] Bottom editor toolbar.

### Rich content tools
- [x] Theme/background palette with categories and thumbnail grid.
- [x] Camera and Photos & Videos attachment entry point.
- [x] Sticker/emoji palette.
- [x] Placed sticker with visible transform/delete handles.
- [x] List template palette.
- [x] List insertion into main text suggested by visible toast.
- [x] Text/formatting/tag-like toolbar controls visible; exact complete semantics are not legible.
- [x] Recording entry point.
- [x] Add Audio File entry point.
- [x] Drawing canvas with undo/redo, colors and tools.
- [x] Drawing inserted back into diary editor.
- [ ] Video playback/preview details not shown.
- [ ] Audio playback controls/metadata not shown.
- [ ] Full font family/size/color behavior not shown.

### First-save feedback
- [x] First-entry celebration.
- [x] 3-Day Habit Challenge promotion.
- [x] Reminder Time selector.
- [x] Dismiss/back action.
- [ ] Challenge progression and notification scheduling are not demonstrated.

## 4. Exactness limits

1. This is a video, not source code or a design file. Pixel-perfect dimensions, font names, asset IDs, exact color values and animation curves cannot be recovered reliably.
2. Some icons/labels are too small to identify confidently. Do not invent behavior for them.
3. Only a first-entry post-save panel is shown. Recurring save behavior is not shown.
4. The reference includes Pro/paywall and security/recovery options. They are not mandatory LifeOS requirements; privacy, offline behavior and secure recovery take precedence.
5. The reference does not demonstrate cloud sync, backup file format or database schema.

## 5. Preliminary LifeOS mapping

LifeOS already has a Room-backed diary list/editor, date/time, mood, title/body, tags, photos, voice recording/playback, location, search/calendar/insights and save confirmation. Extend those flows instead of creating a second diary database.

The current typed attachment model does not confirm persisted support for themes, positioned stickers, structured list blocks, drawing artifacts, imported audio files or video. These require explicit versioned data contracts, repository lifecycle, backup/restore and tests before enabling UI.

See `DIARY_REDESIGN_SPEC.md` and `DIARY_CLAUDE_IMPLEMENTATION_PLAN.md`.
