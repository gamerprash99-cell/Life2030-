# LifeOS — Design System & UI/UX Specification

**Status:** Production design specification
**Version:** v4 — "Prism Glass"
**Supersedes:** v3 "Aurora Noir" (removed)
**Platform:** Android (native, Jetpack Compose, minSdk 26)
**Owner:** LifeOS

> This document is the single source of truth for how LifeOS looks, moves and
> behaves. The previous design language has been removed entirely; the app was
> rebuilt from zero on the system described here. Where a value appears in this
> document it is the value implemented in code (`ui/theme/*`, `ui/components/*`).

---

## 1. Product in one line

LifeOS is a private, offline-first Android app for capturing, organising and
understanding your own life — notes, tasks, habits, spending, diary, memories
and an on-device assistant — with **no account, no cloud and no telemetry**.

---

## 2. The design language — "Prism Glass"

**Concept: light refracted through glass.**

A dark-first, cool-neutral obsidian canvas carries frosted, translucent
surfaces. A single iridescent spectrum — indigo → sky → coral — is the *only*
loud colour in the app and is reserved for identity moments: the hero, the
Capture action and the AI orb. Everything else is calm neutral ink on glass, so
**content leads and colour means something**.

### Principles

1. **Content leads.** Chrome never outranks the user's own data. The score of a
   screen is its information, not its decoration.
2. **One loud thing.** The spectrum gradient is scarce. A screen that paints
   everything loudly paints nothing meaningfully.
3. **Glass is a lens, not a texture.** Surfaces describe depth (edge light, sheen,
   pooled shadow), so the UI reads as physical layers over a living background.
4. **Calm, honest data.** No fake graphs, no invented metrics, no decorative
   numbers. Every figure traces back to a stored row.
5. **Touch is physical.** Anything the finger touches springs; anything that
   merely appears eases. Nothing bounces or spins for its own sake.
6. **Offline and legible.** The type is bundled, the palette is stable, and every
   text style survives 200% font scaling.

---

## 3. Colour tokens

Defined in `ui/theme/Color.kt` and mapped into the Material 3 scheme in
`ui/theme/Theme.kt`. Light and dark are first-class and equally finished.

### Brand — the Prism spectrum

| Token | Light | Dark | Role |
|---|---|---|---|
| `LifeOSPrimary` / Iris | `#5B49E6` | `#B9AEFF` | Primary action & selection |
| `LifeOSPrimaryDeep` | `#3B2CC4` | — | Pressed states, gradient depth |
| `LifeOSSecondary` / Sky | `#2F9BE0` | `#7FD0F5` | Gradient partner, informational |
| `LifeOSTertiary` / Coral | `#E0576B` | `#FF9AA5` | Warm third accent, energy |
| `LifeOSAccentLavender` | `#E9E5FF` | — | Selected containers |
| `PrismIndigo` | `#7C6BFF` | `#7C6BFF` | Identity gradient stop 1 |
| `PrismSky` | `#4CC9F0` | `#4CC9F0` | Identity gradient stop 2 |
| `PrismCoral` | `#FF7A85` | `#FF7A85` | Identity gradient stop 3 |

**The identity gradient** (`prismBrush()`, `PrismSpectrum`) is
`PrismIndigo → PrismSky → PrismCoral`. Reserved for: the Home hero, the centre
Capture action, the AI orb and message gradient. Never for a card, a list row or
a section header.

### Canvas, surface & content

| Token | Light | Dark | Role |
|---|---|---|---|
| `LifeOSBackground` | `#F5F5FA` | `#08080C` | The page behind everything |
| `LifeOSSurface` | `#FFFFFF` | `#121218` | Cards and sheets |
| `LifeOSSurfaceVariant` | `#ECECF3` | `#1B1B24` | Wells, insets, recessed panels |
| `LifeOSSurfaceRaised` | `#FFFFFF` | `#1E1E28` | Menus, popovers, raised sheets |
| `LifeOSTextPrimary` | `#14141A` | `#F4F4F8` | Headings and body |
| `LifeOSTextSecondary` | `#5C5C6E` | `#A6A6B8` | Supporting copy |
| `LifeOSTextMuted` | `#8A8A9C` | `#6E6E80` | Structural labels, disabled hints |
| `LifeOSBorder` | `#14000000` | `#1FFFFFFF` | Hairline on opaque surfaces |
| `LifeOSOutline` | `#9A9AAE` | `#4A4A5A` | Controls, unchecked shapes |

### Glass

| Token | Light | Dark | Role |
|---|---|---|---|
| `Glass` | `#E8FFFFFF` | `#CC14141C` | The frosted sheet |
| `GlassBorder` | `#14000000` | `#1FFFFFFF` | Outer hairline |
| `GlassTopEdge` | `#B3FFFFFF` | `#66FFFFFF` | Bright rim (light hits the top) |
| `GlassBottomEdge` | `#1A000000` | `#40000000` | Deep rim (shadow at the foot) |
| `GlassSheen` | `#33FFFFFF` | `#1AFFFFFF` | Specular highlight |
| `GlassInnerShadow` | `#14000000` | `#33000000` | Pooled base shadow |

### Ambient glow (the colour the glass refracts)

| Token | Light | Dark |
|---|---|---|
| `AmbientGlowLavender` (indigo haze) | `#4D8B7CFF` | `#4D4A3AC0` |
| `AmbientGlowMagenta` (violet haze) | `#3DFF9AA5` | `#3D4A2E7A` |
| `AmbientGlowSky` (sky haze) | `#3D4CC9F0` | `#38204A8A` |

### State & premium

| Token | Light | Dark | Role |
|---|---|---|---|
| `LifeOSSuccess` | `#15803D` | `#4ADE80` | Done, on-track |
| `LifeOSWarning` | `#B45309` | `#FBBF24` | Attention |
| `LifeOSDanger` | `#C81E3A` | `#FF8AA0` | Destructive |
| `LifeOSInfo` | `#2563EB` | `#7FC4FF` | Neutral information |
| `LifeOSGold` | `#B8860B` | `#F0C868` | **Premium only**: streaks, records, achievements |

Gold is the one colour with a licence requirement: it may only mark a personal
record, a streak or an achievement. Anywhere else it is a bug.

### Diary — the deliberate exception

The Diary is a **paper object**, not glass. It keeps warm paper surfaces
(`DiaryPaperBase`, `DiaryPaperCard`, `DiaryLavender`, ruled lines, a margin rule,
a cloth ribbon) and a handwriting face. This is intentional: a notebook should
feel like a notebook even when the rest of the OS is obsidian. The Diary shares
the app's type scale, radii vocabulary, motion and spacing — only its *material*
and *hue* differ. Mood colours (`DiaryMood*`) are data, not chrome.

---

## 4. Typography

Bundled in `res/font` so the app is fully offline — no runtime download.

- **Display / UI:** **Poppins** (regular, medium, semibold, bold) — a geometric
  sans with open counters that reads modern and premium at display sizes and
  stays legible at body sizes.
- **Diary hand:** **Kalam** (light, regular, bold) — a real handwriting face used
  only for things a person writes in the diary: the date, a memory's title and
  body, the empty-page invitation. Controls in the Diary stay in Poppins.

### Scale (Poppins)

| Style | Size / line / tracking | Weight | Use |
|---|---|---|---|
| `displayLarge` | 46 / 54 / −1.0 | Bold | Hero numerals |
| `displayMedium` | 38 / 46 / −0.8 | Bold | Hero `%` |
| `displaySmall` | 31 / 39 / −0.6 | Bold | Large figures |
| `headlineLarge` | 29 / 36 / −0.5 | Bold | Screen titles |
| `headlineMedium` | 24 / 31 / −0.4 | Bold | Greeting |
| `headlineSmall` | 20 / 27 / −0.2 | SemiBold | Section leads |
| `titleLarge` | 18 / 25 / −0.1 | SemiBold | Top-bar titles |
| `titleMedium` | 16 / 22 | SemiBold | Card titles |
| `titleSmall` | 14 / 20 | SemiBold | Row titles |
| `bodyLarge` | 16 / 25 | Regular | Primary reading |
| `bodyMedium` | 14 / 22 | Regular | Secondary |
| `bodySmall` | 12 / 18 | Regular | Captions |
| `labelLarge` | 14 / 18 / +0.2 | SemiBold | Buttons |
| `labelMedium` | 12 / 16 / +0.4 | SemiBold | Chips, tabs |
| `labelSmall` | 11 / 14 / +0.5 | Medium | Eyebrows |

Display sizes carry negative tracking so headings bind into units; labels carry
positive tracking so small caps read as deliberate eyebrows; body keeps a ~1.6×
line-height so long entries stay comfortable.

---

## 5. Shape & radius

Defined in `ui/theme/Shape.kt` (`LifeOSRadius`, `LifeOSShapes`). Pebble-like
curvature — generously round, but controls tighter than cards so they still read
as pressable.

| Token | Value | Use |
|---|---|---|
| `small` | 14 dp | Chips, image tiles |
| `medium` | 18 dp | Controls, small panels |
| `large` | 24 dp | Cards, list rows, top bar |
| `xlarge` | 30 dp | Hero surfaces, bottom-nav pill |
| `sheet` | 36 dp | Full-width sheets, capture surface |
| `pill` | 50 dp | Fully-round buttons, pills |

The Diary keeps its own paper geometry and is deliberately outside this scale.

---

## 6. Spacing

Base-4 rhythm (`ui/theme/Spacing.kt`): `xs 4 · sm 8 · md 12 · lg 16 · xl 24 ·
xxl 32 · xxxl 48 · huge 64`. Named semantic steps: `screenPadding 24`,
`compactPadding 16`, `sectionSpacing 20`. Every screen snaps to these so vertical
rhythm is identical screen to screen.

---

## 7. Elevation & the soft shadow

`LifeOSElevation`: `none · card 3 · raised 8 · floating 14 · sheet 20 · hero 26`.
Shadows are neutral (mixed from `LifeOSShadow`), never grey slabs and never a
brand smear. A glass surface expresses depth through its **rim and sheen**, with
the drop shadow as support; colour depth is applied only on the identity orb.

---

## 8. The glass material (`liquidGlass`)

`ui/components/LiquidGlass.kt`. Android has no backdrop-blur behind a sibling
tree at minSdk 26, so the material is *painted* to read as a curved optical lens:

1. a translucent vertical gradient — cooler/brighter at the top, deeper at the foot;
2. an optional accent `tint` overlay that picks up the content's colour;
3. a specular **sheen** across the upper third (the "liquid" highlight);
4. a **pooled base shadow** (the lens curving away from the light);
5. a **dual-tone rim** — a bright line on the top edge, a deeper line on the
   bottom — stroked from the shape's outline and clipped to each half.

One token set and one ambient glow pool make separate sheets read as one
connected material. `interactive` surfaces react to press: brighter sheet,
stronger tint, lifted shadow, spring scale.

**Where glass is used:** the ambient background, the bottom-nav pill, top bars,
cards, quick-action tiles, capture tiles, and the Capture/AI identity surfaces.
**Where it is not:** long-form reading (Notes body, Diary page), where a calm
opaque surface protects legibility.

### The ambient background (`LifeOSAmbientBackground`)

Paints the theme background, three soft indigo/violet/sky radial glows and a
gentle vignette that settles the edges — so glows never reach the system bars
hot. Drawn with `DrawScope` gradients: no assets, any screen size, smooth in both
themes.

---

## 9. Motion

`ui/theme/Motion.kt`. One language, two feels:

- **Ease** (`CubicBezier(0.05, 0.7, 0.1, 1.0)`) for entrances, exits and content swaps;
- **Exit** (`CubicBezier(0.3, 0, 0.8, 0.15)`) for leaving;
- **`springy()`** (damping 0.78) for press-scale feedback — the finger's feel;
- **`snappy()`** (damping 0.88, high stiffness) for toggles, chips, tab selection;
- **`gentle()`** (no overshoot) for layout settles — progress, counters, widths.

Durations: `instant 90 · fast 170 · standard 260 · slow 380 · lazy 560` ms.
`staggerDelay(index)` steps list entrance by 45 ms (capped at 8 items). Nothing
animates that isn't caused by the user or by data arriving.

---

## 10. Components

`ui/components/`. Every component reads tokens by intent and implements all eight
UI states (rest, hover, focus, pressed, selected, disabled, loading, error) where
the state applies.

| Component | Purpose |
|---|---|
| `LiquidGlassSurface` | The base glass sheet, optionally press-reactive |
| `LiquidGlassChip` | Round filter/choice chip; `selected` strengthens the tint |
| `LiquidGlassButton` | Fully-round button; `prominent` = spectrum hero |
| `LiquidGlassFab` | Circular floating action; `prominent` = spectrum orb |
| `LifeOSSurface` / `LifeOSCard` | Token-configured glass card |
| `LifeOSTopBar` | Floating glass header: title, subtitle, optional back/search, trailing actions |
| `LifeOSBottomBar` | The floating navigation pill (see §11) |
| `LifeOSPrimaryButton` / `LifeOSSecondaryButton` | Hero and quiet actions |
| `LifeOSIconBadge` / `LifeOSPill` / `LifeOSMetric` | Small identity and status atoms |
| `LifeOSProgressBar` / `LifeOSProgressRing` | Spectrum-filled progress |
| `LifeOSWeeklyRhythm` | The shared seven-day consistency row |
| `LifeOSBanner` | Inline notice with an action |
| `LifeOSEmptyState` | Honest empty state with one next step |
| `PrismBrush` (`prismBrush()`) | The single identity gradient brush |

---

## 11. Information architecture & navigation

### The primary bar

A floating, frosted glass pill with five slots. Capture is **not** a tab — it is
the raised centre action that opens the full-window Capture hub, so it must not
join the tab selection model.

```
Home   ·   Notes   ·   [ ＋ Capture ]   ·   Life   ·   You
```

| Slot | Route | What it is |
|---|---|---|
| **Home** | `home` | The command centre: today's real progress, quick actions, today's tasks, habits, weekly rhythm, spend, recent activity, AI entry |
| **Notes** | `notes` | Long-form thinking: list, filters, editor |
| **＋ Capture** | `capture` | Full-window hub: thought, photo, video, audio, mood, location, attach |
| **Life** | `life` | The connected-history hub: timeline, calendar, search, and every module |
| **You** | `profile` | Profile, App Lock, Local Backup, Settings |

Labels are always visible; icon-only navigation is a UX trap. The active tab is a
lit spectrum pill so "where am I" is never a guess. The bar hides itself on
full-window flows (Capture, Note editor, AI, Snap, Global Search, App Lock setup)
and while the Diary composer owns the window.

### What changed from the previous IA

- **The Insights tab was removed.** Its analytics lived only on that screen and
  duplicated figures the Life hub and the AI assistant already surface. The route,
  screen, view model and bottom-bar slot were deleted. The underlying analytics
  (habit stats, weekly rhythm, spend) are unchanged and still appear where they
  belong.
- **"You" replaces it** as the fourth slot, so the personal hub (profile, lock,
  backup, settings) is one tap from anywhere instead of an overlay pushed from
  Home.
- The AI assistant's answer chips now point at **Life** instead of the removed
  Insights screen.

### Route map

```
root_tabs (bottom bar visible)
├── home
├── notes
├── life
└── profile            (You)

full-screen / pushed (bar hidden or stacked above tabs)
├── capture            ── full-window sheet
├── note/{noteId}      ── full-window editor
├── ai                 ── full-height transcript
├── snap               ── camera
├── search             ── full-window search
├── timeline, tasks, habits, habits/{habitId}, expenses
├── diary + diary/search, diary/calendar, diary/insights, diary/{entryId}
├── settings, settings/app_lock
└── snap/view/{captureId}
```

---

## 12. Screen specifications

### Home — the command centre
Header (greeting, date, search, avatar) → **hero** (today's % of goals done, as a
ring/bar in the identity gradient, with inline task/habit/spend stats) →
quick-action rail (Tasks · Habits · Money · Diary) → overdue banner (only when
true) → today's tasks (tappable completion) → habits (streak + progress) → weekly
rhythm → spend + quick capture → recent activity → AI entry. AI is the **last**
entry, never a competing hero: today's real day ranks first.

### Notes — thinking surface
Title bar with search and new-note; filter pills (All · Recent · Pinned ·
Favorites · Archived); note cards with folder and updated time; pinned notes
marked; spectrum FAB. The editor is a full-window, distraction-free surface.

### Capture — the hub
Thought composer first (fastest capture), then a grid of tiles: Photo, Video,
Audio, Mood, Location, Attach. Everything is saved privately on-device.

### Life — connected history
Timeline, calendar, global search and module entry points, so every captured
thing can be found in one place.

### You — personal hub
Profile card (name, photo, crop editor), then App Lock, Local Backup, Settings,
About. States plainly that data never leaves the device.

### Tasks · Habits · Expenses · Diary
Each keeps its own focused, single-purpose layout on the shared system. The Diary
is the paper exception (§3).

---

## 13. Accessibility

- **Target:** WCAG 2.2 AA for text and UI components. *Status: designed to target;
  final automated contrast/state verification (axe-style gate) is part of the QA
  pass and is not claimed here as already measured.*
- Every text style scales to 200% without clipping; key numbers use
  `fontScale`-safe layouts and wrapping is allowed on labels.
- Colour is never the only signal: completion uses a check glyph, not just green;
  selected tabs use a pill plus a filled icon; destructive actions carry a word.
- The bottom bar always shows labels, and each tab exposes a `Role.Tab` with a
  `selected` semantic so screen readers announce position.
- Icon-only controls carry `contentDescription`; decorative icons are `null`.
- Touch targets are ≥ 48 dp.
- Motion is decorative-free; entrances are short and non-oscillating so reduced-motion
  users are not disoriented.

---

## 14. Platform & implementation notes

- **Stack:** Kotlin, Jetpack Compose, Material 3, Room + SQLCipher, DataStore,
  offline intelligence package. Single `:app` module.
- **Fonts:** bundled (Poppins, Kalam) — no runtime download, no internet permission.
- **Theming:** `LifeOSTheme` provides the Material scheme plus three composition
  locals — `LocalGlassColors`, `LocalAmbientGlowColors`,
  `LocalLifeOSSemanticColors` — so a palette change is a one-file edit and no
  screen reaches for a raw hex.
- **Dynamic colour** stays opt-in (`dynamicColor = false` by default) so the
  LifeOS brand palette is stable.
- **Backwards compatibility:** every legacy public token and component name is
  preserved, so existing call sites adopt the new look without a rewrite and the
  app keeps compiling throughout the migration.

---

## 15. Change log

| Version | Name | Change |
|---|---|---|
| v2 | Neon/Gen-Z gradient | Original violet→magenta gradient language |
| v3 | Aurora Noir | Cinematic jewel tones, calmer palette |
| **v4** | **Prism Glass** | **Rebuilt from zero: obsidian canvas, painted glass material, single iridescent spectrum, reworked IA (Insights removed, "You" added), new type/shape/motion scales** |
