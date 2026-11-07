# LifeOS Diary Redesign — Phase C: Figma Design Spec

## Figma / Stitch Integration Status

**Google Stitch screen generation — UNAVAILABLE.**
- `tools.stitch.create_project({})` works — creates empty projects (e.g. `projects/15015649614941990127`).
- `tools.stitch.generate_screen_from_text()` consistently times out (MCP error -32001: Request timed out) across all projects and prompt variations.
- `tools.stitch.download_file()` works for reading existing files from existing projects.
- No new screens could be generated via Stitch in this environment.

**Figma MCP — NOT AVAILABLE.**
- The `use_figma` tool is not present in the available tool catalog.
- No Figma file could be created, populated, or prototyped.
- Prior Figma file `EcERw5RozoOh8fhf84Kafj` (session `652e70aa-3953-4abb-99c9-cefdf42af252`) exists from a previous session but its canvas was never populated due to MCP rate limits.

## Design Implementation

Since Stitch screen generation and Figma MCP were unavailable, the design below was implemented directly in Jetpack Compose based on:

- `docs/DIARY_REFERENCE_VIDEO_FRAME_AUDIT.md` (frame-by-frame audit of reference video `1000276437.mp4`)
- `docs/DIARY_REDESIGN_SPEC.md` (product, UI and backend specification)
- `docs/diary-redesign/01-figma-design-spec.md` (previous design specification)
- Existing Diary design tokens in `app/src/main/java/com/lifeos/app/ui/theme/`

The implementation creates new Compose UI files (v2) alongside the existing ones, using the existing ViewModels, repositories, DAOs, and Room entities. No data-layer changes were made.

## 1. Design System — Diary v2 (pastel pink / lavender)

**File:** `app/src/main/java/com/lifeos/app/ui/theme/DiaryDesign2.kt`

**Colors:**
- `Diary2Canvas` — #FDF0F5 (very light pink paper base)
- `Diary2PaperCard` — #FFF5F8 (near-white pink card)
- `Diary2SurfaceLavender` — #F3EDF7 (soft lavender tint)
- `Diary2Hairline` — #EACFD9 (pale pink border)
- `Diary2InkViolet` — #6A3FA0 (violet-purple heading ink)
- `Diary2InkSoft` — #8A7BA0 (muted secondary ink)
- `Diary2Lavender` — #F3EDF7 (soft lavender fill)
- `Diary2ActionViolet` — #C084FC (prominent pastel pink/purple action)
- `Diary2SaveDisabled` — #E9D5F3 (disabled Save fill)
- `Diary2RuledLine` — #FDE8EE (very pale ruled line)
- `Diary2MarginLine` — #F9CED7 (soft pink margin line)
- Mood pastel fills: Happy #FDECEA, Calm #E4F2F4, Sad #EAE9FF, Stressed #FFEAEA, Excited #FDF0FF

**Typography:** Uses existing `DiaryHandwriting` (Kalam) for dates/headings, bodyLarge (16sp, 1.75x leading) for writing surface.

**Spacing:** 48dp min touch target, 20dp screen padding, 16dp section spacing, 56dp FAB size.

**Shapes:** 8dp/16dp/20dp rounded corners.

**Typography:** Uses existing `DiaryHandwriting` (Kalam) for dates/headings, bodyLarge (16sp, 1.75x leading) for writing surface.

---

## 2. Design inputs (visual source of truth)

- **Reference video:** `1000276437.mp4` (~85s), audited in `docs/DIARY_REFERENCE_VIDEO_FRAME_AUDIT.md`.
- **Redesign spec:** `docs/DIARY_REDESIGN_SPEC.md`.
- **Baseline code:** `app/src/main/java/com/lifeos/app/ui/diary/` (package `diary`).

**Visual direction (from the video):**
- Very light pink / pastel diary canvas; editor fills the usable screen.
- Compact top row: Back, overflow/more, prominent pink Save action.
- Date row in handwritten/serif-like display treatment; mood emoji on the right.
- Quiet minimal title field; body placeholder "Write more here…".
- Slim floating/anchored editor toolbar near the lower edge.
- Tool selection opens contextual palettes/sheets, not full navigation away.
- Pastel pink / soft surfaces, pale borders, restrained shadow, rounded surfaces.
- Compact prominent pink Save action; editorial/handwritten date display; readable body.

**Out of scope (deliberately excluded):** paywall/"Let's Go Pro" screens, subscription gating, Diary Lock password/security-question flow, third-party promo/donate/share/follow-us screens, cloud sync/backup UI beyond existing local export, ads, telemetry, analytics, Firebase, external AI.

---

## 3. Screen inventory and navigation flow

Screens to design (each with default, empty, loading, error, permission-denied states):

| # | Screen / state | Route / trigger | Key elements |
|---|---|---|---|
| 1 | Diary landing/list (day-scoped) | `diary` | Pink illustrated header, date masthead, entry cards (title/body preview, date/time, mood emoji, attachment indicators, favorite), prominent New Entry FAB, empty state "YOUR STORY STARTS HERE", header overflow → search/calendar/insights |
| 2 | Entry editor (new/edit) | in-place branch of `diary` / `diary/{entryId}` | Top bar Back/overflow/Save (pink), date picker control, mood emoji affordance, optional title, ruled body writing surface, inline rich objects, persistent bottom toolbar above IME/gesture insets, contextual palettes above toolbar |
| 3 | Mood chooser | "How's your day?" panel | Emoji mood grid, selection closes panel, reflects in editor header |
| 4 | Date picker | month calendar grid | prev/next month, selected day distinct, Cancel/Done |
| 5 | Time picker | hour/minute rollers | Cancel/Done |
| 6 | Theme/background picker sheet | category tabs + thumbnail grid | Customize/Popular/Dark/Artsy/Cute/Lifestyle, Original/default, Apply, preview cannot overwrite body |
| 7 | Sticker/emoji picker sheet | category strip + glyph grid | Insert as movable/resizable object with delete/transform handles |
| 8 | List/template picker sheet | bullet/numbered/checkbox/dash | Insert at caret without deleting adjacent text |
| 9 | Add-media sheet | Camera + Photos & Videos | Photo Picker/SAF, dismiss |
| 10 | Audio panel | Recording + Add Audio File | Record/play/pause/stop, import via document picker, missing-file state |
| 11 | Drawing/signature canvas | pen/marker/eraser, color swatches, undo/redo, Done | Done inserts drawing inline |
| 12 | Entry detail view | `diary/{entryId}` | date/mood/title/body, photos, place/weather, tags, meta, share/copy/delete/favorite, edit |
| 13 | Save confirmation / first-entry celebration | after first durable save | "First Entry Saved!", optional 3-Day Diary Challenge, reminder time selector, dismiss; subsequent saves normal confirmation |
| 14 | Delete confirmation | deliberate + confirmed | entry day/time, confirm/cancel |
| 15 | Search | `diary/search` | query + mood filter, newest-first results |
| 16 | Calendar | `diary/calendar` | month grid, day dots, open entry |
| 17 | Insights | `diary/insights` | streak, stats, mood mix, 7-day mood chart, recurring words, reflection prompt |
| 18 | Permission-denied / missing-file / error states | inline | honest messages, retry, open settings |

**Navigation flow (prototype):**
Open Diary → create entry → select mood → edit text → apply theme/sticker → attach media/audio/drawing → save → reopen/edit entry → delete (confirmed) → back → search/calendar/insights → system Back.

---

## 4. Design tokens

All tokens scoped to Diary (pastel pink), do not re-theme the whole app.

**Colors (0–1 for Figma variables):**
- `Diary/Canvas` — very light pink base `#FDF0F5` (paper base)
- `Diary/PaperCard` — near-white pink `#FFF5F8` (cards/surfaces)
- `Diary/Hairline` — pale border `#EACFD9`
- `Diary/InkViolet` — primary ink `#6A3FA0`-family (handwriting/headings)
- `Diary/InkSoft` — muted secondary `#8A7BA0`
- `Diary/Lavender` — soft fill `#F3EDF7`
- `Diary/ActionViolet` — prominent Save pink/purple
- `Diary/SaveDisabled` — disabled save fill
- `Diary/RuledLine`, `Diary/MarginLine` — ruled-paper guide lines
- `Diary/Theme.*` — per-theme paper/card/ruled/margin colors (Original, Pastel Pink, Warm Cream, Deep Paper, Artsy Wash, Cute Mint, Lifestyle Sage)

**Typography:**
- Handwriting/display font for dates and headings (`DiaryHandwriting`)
- Body text for writing surface (`bodyLarge`, 16sp, 1.75x line height)
- Label/caption scales for metadata, counters, chips

**Spacing:** `LifeOSSpacing` tokens — `screenPadding`, `diaryFabSize`, `diaryFabBottomOffset`, `diaryEditorTextMinHeight`, `diaryEditorWritingFill`, `diaryDateStripMinTouch`, `minTouchTarget` (48dp), `compactPadding`, `diaryEditorSection`

**Shapes:** rounded corners 8–30dp, full-circle pills/avatars, hairline 1dp borders

**Elevation:** restrained shadow (2dp) on panels, scrim 32% on sheets

**Icon sizing:** 18–28dp tool icons, 48dp minimum touch targets

**Component states:** default/selected/pressed/disabled for buttons, chips, theme cards, sticker cards; active/inactive for recording/playback.

---

## 5. Reusable components and variants

| Component | Variants / states |
|---|---|
| `Diary/Button` | default, primary (pink Save), disabled, text |
| `Diary/Chip` | default, selected (pastel fill), compact |
| `Diary/EntryCard` | with/without mood, with/without attachment indicators, favorite on/off |
| `Diary/DateStripItem` | selected, past, today, has-memory dot |
| `Diary/MoodPill` | 8 mood keys + empty, pastel bg + emoji |
| `Diary/SaveAction` | enabled pink, disabled grey |
| `Diary/PaletteTab` | selected underline/indicator |
| `Diary/ThemeCard` | selected border + checkmark |
| `Diary/StickerCard` | glyph + label + category |
| `Diary/ListTemplateCard` | marker + name + category |
| `Diary/MediaTile` | Camera, Photos & Videos |
| `Diary/Sheet` | bottom sheet with scrim, Cancel/Apply, drag handle |
| `Diary/Dialog` | delete confirmation, date picker, time picker |
| `Diary/StickerLayerItem` | movable, scale, rotation, delete handle |
| `Diary/ErrorInline` | red label text |

---

## 6. Interaction specifications

| Interaction | Behavior |
|---|---|
| Tap New Entry | open editor seeded with current day, fresh draft |
| Tap entry | open detail view; Edit opens editor with existing draft |
| Tap mood | open mood panel; selection closes + reflects in header |
| Tap date | open month calendar; Cancel discards, Done applies |
| Tap theme | open theme sheet; category tabs + thumbnails; Apply commits to entry only |
| Tap sticker | insert at page center; sticker is movable/resizable with delete handle |
| Tap list template | insert marker at caret, adjacent text preserved |
| Tap media | open add-media sheet: Camera or Photos & Videos |
| Tap record | request mic, record/stop, playback, remove |
| Tap drawing | full-screen canvas; Done inserts inline drawing |
| Tap Save | block double-submit; progress; success only after DB write; confirmation sheet |
| System Back (editor) | two-stage: hide keyboard first, then dismiss; never discard saved data |
| Palette open | Back/outside closes palette, draft preserved |
| Permission denied | honest state, retry or open settings; never re-request granted |

---

## 7. Responsive / accessibility notes

- 320/360/411dp widths, large display, landscape; font scale 1.0–1.3+
- No content behind IME, gesture bar, or 3-button navigation; `WindowInsets` + IME insets
- Minimum 48dp touch targets; content descriptions on all actions
- Color never sole selected-state signal (border + checkmark + fill)
- TalkBack + keyboard navigation; semantic labels truthful per state (e.g. location icon)
- Preserve draft/scroll state while palettes open/close
- Dark-mode contrast; ruled lines faint enough not to fight ink at large font scales

---

## 8. Differences between reference video and LifeOS adaptation

| Reference | LifeOS adaptation | Reason |
|---|---|---|
| "Let's Go Pro" paywall | Excluded | No paywall for core diary (privacy/offline principle) |
| Diary Lock password/security question | Excluded | Reuse existing LifeOS App Lock (PIN) only if intentionally designed; not copied |
| Upgrade/Donate/Share/Follow/More Apps drawer | Excluded | Third-party promo, not LifeOS |
| Cloud sync/backup format | Existing local JSON backup only | No network, no cloud |
| Video playback details | Not implemented (video out of v1 scope) | Reference shows no playback; photo/audio/drawing prioritized |
| Audio playback controls/metadata | Existing voice-note player; imported audio panel | Per spec §3.10 |
| Exact pixel/fonts/assets | LifeOS-native approximation | Video cannot recover exact pixels/assets; documented uncertainty |

---

## 9. Next steps to complete the Figma gate

1. Retry Figma MCP calls (rate-limit window reset).
2. Re-open session `fileKey: "EcERw5RozoOh8fhf84Kafj"`.
3. Create pages: `Diary Screens`, `Components`, `Tokens`, `Prototype`.
4. Build tokens (colors, typography, spacing, radius) as Figma variables/styles.
5. Build components and variants (§5).
6. Compose all screens (§3) with Auto Layout, responsive constraints.
7. Wire prototype flow (§3).
8. Take composition screenshot; validate against reference video.
9. Link file URL + frame/node IDs in `01-figma-design-spec.md` (update this doc).
