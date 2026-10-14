# LifeOS — Design System & UI/UX Specification

**Status:** Production design specification
**Version:** v6 — "AURA"
**Supersedes:** v5 "Lumen" (warm matte paper) → now the *dark, frosted-glass* language the spec §53 asks for
**Platform:** Android (native, Jetpack Compose, minSdk 26)
**Owner:** LifeOS

> **v6 note.** The visual language is **AURA**: a deep lagoon (teal→plum) canvas
> behind frosted translucent glass, lit by a warm **coral** signature with
> **mint** as its calm partner — the "premium glassmorphism, futuristic, calm,
> Gen-Z" direction of spec §53, and the opposite of generic blue-black glass.
> The token *names* are unchanged (`ui/theme/*`), only their values moved, so
> every existing call site adopted the new look without a rewrite.
>
> The IA is also restored to the spec (§4): the primary bar is now
> **Home · Notes · ＋Capture · Insights · Life**, and the Insights tab (§27/§28)
> is a real screen again, wired to the offline intelligence engine.
>
> Sections below that still describe v5 "Lumen" (its warm palette, matte
> material and "You" tab) are **superseded** and pending a full text refresh;
> the code in `ui/theme/*` and `ui/components/*` is the authority.

> This document is the single source of truth for how LifeOS looks, moves and
> behaves. Where a value appears in this document it is the value implemented in
> code (`ui/theme/*`, `ui/components/*`).

---

## 1. Product in one line

LifeOS is a private, offline-first Android app for capturing, organising and
understanding your own life — notes, tasks, habits, spending, diary, memories
and an on-device assistant — with **no account, no cloud and no telemetry**.

---

## 2. The design language — "Lumen"

**Concept: a linen desk under a lamp.**

A warm, paper-near canvas carries soft **matte** objects. Depth comes from a
long, low shadow and a single warm hairline, not from a painted lens — nothing in
the app is frosted and nothing glows cool. One muted **clay** is the loud colour,
with **sage** and **amber/bronze** as its quiet partners, so **content leads and
colour means something**.

The Diary was v4's one deliberate exception to a cool glass system. Under Lumen
the rest of the app has moved *toward* the Diary rather than the reverse, so the
whole product now shares one warm material.

### Principles

1. **Content leads.** Chrome never outranks the user's own data. The score of a
   screen is its information, not its decoration.
2. **One loud thing.** The identity gradient is scarce. A screen that paints
   everything loudly paints nothing meaningfully.
3. **Matte is physical.** A surface is an object you could pick up: opaque, with
   a top light, a pooled shadow and one warm edge. It does not glint, and it does
   not brighten on press — movement is the feedback.
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

### Brand — warm clay, sage and amber

| Token | Light | Dark | Role |
|---|---|---|---|
| `LifeOSPrimary` (clay) | `#A8512F` | `#E08B62` | Primary action & selection |
| `LifeOSPrimaryDeep` | `#8A3F23` | — | Pressed states, gradient depth |
| `LifeOSSecondary` (sage) | `#6E7F63` | `#93A588` | Gradient partner, informational |
| `LifeOSTertiary` (amber) | `#C08A3E` | `#E0B070` | Warm third accent, energy |
| `LifeOSAccentLavender` | `#F6E7DF` | — | Soft clay wash for containers |
| `PrismIndigo` (clay) | `#A8512F` | `#A8512F` | Identity gradient stop 1 |
| `PrismSky` (amber) | `#AD651F` | `#AD651F` | Identity gradient stop 2 |
| `PrismCoral` (bronze) | `#8F6E18` | `#8F6E18` | Identity gradient stop 3 |

**The identity gradient** (`prismBrush()`, `PrismSpectrum`) is
`PrismIndigo → PrismSky → PrismCoral`. Reserved for: the Home hero, the centre
Capture action, the AI orb and the AI message bubbles. Never for a card, a list
row, a section header — **and never for the selected bottom-nav tab**, which is
solid clay (see §11).

**The gradient is deliberately deep.** Every stop keeps white text and icons at
or above WCAG AA — clay 5.4:1, amber 4.5:1, bronze 4.8:1. Any brightening of
these three values silently breaks every filled identity surface in the app, so
they are a contract, not a preference. The bronze tone also sits far better under
warm matte surfaces than a hot gold ramp would.

### Canvas, surface & content

| Token | Light | Dark | Role |
|---|---|---|---|
| `LifeOSBackground` | `#F7F4EF` | `#16130F` | The page behind everything |
| `LifeOSSurface` | `#FFFDF9` | `#221D17` | Cards and sheets |
| `LifeOSSurfaceVariant` | `#EFEAE2` | `#2B251E` | Wells, insets, recessed panels |
| `LifeOSSurfaceRaised` | `#FFFFFF` | `#302921` | Menus, popovers, raised sheets |
| `LifeOSTextPrimary` | `#241D15` | `#F6EFE6` | Headings and body |
| `LifeOSTextSecondary` | `#6B5F52` | `#BCAE9D` | Supporting copy |
| `LifeOSTextMuted` | `#7A6E60` | `#9C9083` | Structural labels, disabled hints |
| `LifeOSBorder` | `#1F6B5F52` | `#26F6EFE6` | Warm hairline on opaque surfaces |
| `LifeOSOutline` | `#A99C8C` | `#5A5044` | Controls, unchecked shapes |

Borders are tinted *from the ink* (a warm sepia) rather than set to a neutral
black alpha, so a hairline never reads as a cold grey rule on a warm sheet.

### Surface material (legacy `Glass*` names)

The whole component kit reads these by name; under Lumen their values describe an
opaque warm sheet with a soft top light and a pooled base shadow — **no blur and
no specular sheen**.

| Token | Light | Dark | Role |
|---|---|---|---|
| `Glass` | `#F7FFFDF9` | `#F22A241D` | The matte sheet (near-opaque) |
| `GlassBorder` | `#1F6B5F52` | `#26F6EFE6` | The one warm hairline |
| `GlassTopEdge` | `#40FFFFFF` | `#33F6EFE6` | Soft top light (kept, barely used) |
| `GlassBottomEdge` | `#1A000000` | `#40000000` | Kept for legacy call sites |
| `GlassSheen` | `#14FFFFFF` | `#12FFFFFF` | A whisper of upper wash — not a highlight |
| `GlassInnerShadow` | `#143B2F1E` | `#33000000` | Pooled base shadow (warm in light) |

### Ambient wash (the light the matte sits in)

| Token | Light | Dark |
|---|---|---|
| `AmbientGlowLavender` (clay haze) | `#4DAD5636` | `#4D8A4A2E` |
| `AmbientGlowMagenta` (amber haze) | `#3DD98A45` | `#3D6E4A22` |
| `AmbientGlowSky` (sage haze) | `#3D6E7F63` | `#3D3E4A34` |

### State & premium

| Token | Light | Dark | Role |
|---|---|---|---|
| `LifeOSSuccess` | `#3F7D4E` | `#7CC48F` | Done, on-track |
| `LifeOSWarning` | `#9A6216` | `#E8B45C` | Attention |
| `LifeOSDanger` | `#B23A34` | `#FF9B8F` | Destructive |
| `LifeOSInfo` | `#3A6EA5` | `#8FB8E0` | Neutral information |
| `LifeOSGold` / `LifeOSGoldBright` | `#B8860B` / `#E8B44A` | `#E8C978` | **Premium only**: streaks, records, achievements |

Gold is the one colour with a licence requirement: it may only mark a personal
record, a streak or an achievement. Anywhere else it is a bug. It is a flat
accent token — it is *not* a gradient stop, precisely so it stays scarce.

### Diary — now the warm pole, not an exception

The Diary remains a **paper object** (`DiaryPaperBase`, `DiaryPaperCard`,
`DiaryLavender`, ruled lines, a margin rule, a cloth ribbon) with a handwriting
face. In v4 this was a deliberate exception to an obsidian system; under Lumen
the rest of the app has adopted the same warmth, so the Diary is now the
reference the whole design leans on. Mood colours (`DiaryMood*`) are data, not
chrome.

---

## 4. Typography

Bundled in `res/font` so the app is fully offline — no runtime download.

- **Display / headings:** **Fraunces** (regular, semibold, bold) — a warm,
  slightly old-style serif that carries real letterform character at heading
  sizes. It is what makes the app read as a well-made object rather than a
  dashboard. It carries `display*` and `headline*` **only**.
- **UI / body:** **Manrope** (regular, medium, semibold, bold) — a quiet,
  slightly geometric sans with open counters. Legible at 12sp, neutral enough to
  sit under a serif without competing. It carries every `title*`, `body*` and
  `label*`.
- **Diary hand:** **Kalam** (light, regular, bold) — a real handwriting face used
  only for things a person writes in the diary: the date, a memory's title and
  body, the empty-page invitation. Controls in the Diary stay in Manrope.

Both variable upstream faces are instanced to static TTFs at build-prep time
(`fontTools varLib.instancer`), so the shipped files need no runtime
variable-axis support:

- **Fraunces** at `opsz=48` (display optical size), `SOFT=0`, `WONK=0` (clean,
  non-wonky letterforms), `wght` 400 / 600 / 700.
- **Manrope** at `wght` 400 / 500 / 600 / 700.

### Scale

| Style | Size / line / tracking | Weight | Face | Use |
|---|---|---|---|---|
| `displayLarge` | 48 / 56 / −1.0 | Bold | Fraunces | Hero numerals |
| `displayMedium` | 40 / 48 / −0.8 | Bold | Fraunces | Hero `%` |
| `displaySmall` | 32 / 42 / −0.5 | Bold | Fraunces | Large figures |
| `headlineLarge` | 30 / 38 / −0.4 | Bold | Fraunces | Screen titles |
| `headlineMedium` | 25 / 33 / −0.3 | Bold | Fraunces | Greeting |
| `headlineSmall` | 21 / 29 / −0.2 | SemiBold | Fraunces | **Section titles** |
| `titleLarge` | 19 / 26 / −0.1 | SemiBold | Manrope | Top-bar titles |
| `titleMedium` | 16 / 23 | SemiBold | Manrope | Card titles |
| `titleSmall` | 14 / 20 | SemiBold | Manrope | Row titles |
| `bodyLarge` | 16 / 26 | Regular | Manrope | Primary reading |
| `bodyMedium` | 14 / 23 | Regular | Manrope | Secondary |
| `bodySmall` | 12.5 / 19 | Regular | Manrope | Captions |
| `labelLarge` | 14 / 18 / +0.2 | SemiBold | Manrope | Buttons |
| `labelMedium` | 12 / 16 / +0.4 | SemiBold | Manrope | Chips, tabs |
| `labelSmall` | 11 / 14 / +0.5 | Medium | Manrope | Eyebrows |

Serif headings want slightly more size and line-height than the geometric sans
they replaced, so display steps sit a step larger than v4 and carry negative
tracking to bind into units. Body relaxes to a ~1.6× line-height for comfortable
long-form reading. Labels keep positive tracking so small caps read as deliberate
eyebrows. `headlineSmall` is the **section voice** — every section title in the
app is serif, so the app "speaks" in Fraunces and the user's content sits in
Manrope beneath it.

---

## 5. Shape & radius

Defined in `ui/theme/Shape.kt` (`LifeOSRadius`, `LifeOSShapes`). Softer and more
pillowy than v4 — the warm palette asks for a gentler curve — while controls stay
tighter than cards so they still read as pressable.

| Token | Value | Use |
|---|---|---|
| `small` | 16 dp | Chips, image tiles |
| `medium` | 22 dp | Controls, small panels |
| `large` | 28 dp | Cards, list rows, top bar |
| `xlarge` | 34 dp | Hero surfaces, bottom-nav pill |
| `sheet` | 40 dp | Full-width sheets, capture surface |
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

`LifeOSElevation`: `none · card 4 · raised 10 · floating 16 · sheet 22 · hero 28`,
with `shadowAlphaRest 0.12` and `shadowAlphaLift 0.20`. Shadows are mixed from
`LifeOSShadow` (`#2A1F14` — warm, never a brand hue) so depth stays neutral and
never becomes a coloured smear. Under Lumen a matte surface expresses depth
**primarily through its shadow**: the ladder is deeper and softer than v4's,
because v4's rim and sheen no longer carry any of the work.

---

## 8. The matte material (`liquidGlass`)

`ui/components/LiquidGlass.kt`. Located behind an unchanged public API — the name
is legacy, the material is not. Android has no backdrop-blur behind a sibling
tree at minSdk 26, so v4 *painted* a frosted lens. Lumen paints a solid object
instead:

1. an **opaque vertical fill** — a hair lighter at the top where the lamp falls,
   flat at the foot;
2. an optional accent `tint` overlay, a *whisper* (6% base) of the content's
   colour;
3. a faint **upper wash** (the `GlassSheen` slot at ~7%) — matte surfaces catch
   light, they do not glint;
4. a **pooled base shadow** settling into the desk;
5. **one warm hairline** around the whole shape — a physical object has a single
   edge, not a bright top rim and a deeper dark foot.

One token set and one ambient wash make separate cards read as one material.
`interactive` surfaces react to press through **scale and a lifted shadow**; they
deliberately do **not** brighten, because a matte object doesn't.

**Where it is used:** the ambient background, the bottom-nav pill, top bars,
cards, quick-action tiles, capture tiles, and the Capture/AI identity surfaces.
**Where it is not:** long-form reading (Notes body, Diary page), where a calm
opaque surface protects legibility.

### The ambient background (`LifeOSAmbientBackground`)

Paints the theme background, three soft clay / amber / sage radial hazes and a
gentle vignette that settles the edges — so a wash never reaches the system bars
hot. Drawn with `DrawScope` gradients: no assets, any screen size, smooth in both
themes.

---

## 9. Motion

`ui/theme/Motion.kt`. Heavier and more physical than v4 — the palette is warm and
tactile, so the motion is too. Surfaces *lift and settle* rather than snap.

- **Ease** (`CubicBezier(0.05, 0.7, 0.1, 1.0)`) for entrances, exits and content swaps;
- **Exit** (`CubicBezier(0.3, 0, 0.8, 0.15)`) for leaving;
- **`springy()`** (damping 0.80, medium-low stiffness) for press-scale feedback — the finger's weight;
- **`snappy()`** (damping 0.86, medium stiffness) for toggles, chips, tab selection;
- **`gentle()`** (no overshoot) for layout settles — progress, counters, widths.

Durations: `instant 110 · fast 200 · standard 300 · slow 440 · lazy 620` ms —
each a touch longer than v4 so movement reads as weight. `staggerDelay(index)`
steps list entrance by 50 ms (capped at 8 items). Nothing animates that isn't
caused by the user or by data arriving.

---

## 10. Components

`ui/components/`. Every component reads tokens by intent and implements all eight
UI states (rest, hover, focus, pressed, selected, disabled, loading, error) where
the state applies.

| Component | Purpose |
|---|---|
| `LiquidGlassSurface` | The base matte sheet, optionally press-reactive |
| `LiquidGlassChip` | Round filter/choice chip; `selected` strengthens the tint |
| `LiquidGlassButton` | Fully-round button; `prominent` = identity gradient |
| `LiquidGlassFab` | Circular floating action; `prominent` = identity orb |
| `LifeOSSurface` / `LifeOSCard` | Token-configured matte card |
| `LifeOSTopBar` | Floating header: title, subtitle, optional back/search, trailing actions |
| `LifeOSBottomBar` | The floating navigation pill (see §11) |
| `LifeOSPrimaryButton` / `LifeOSSecondaryButton` | Hero and quiet actions |
| `LifeOSIconBadge` / `LifeOSPill` / `LifeOSMetric` | Small identity and status atoms |
| `LifeOSSectionTitle` | The serif section voice; optional trailing action |
| `LifeOSProgressBar` / `LifeOSProgressRing` | Identity-gradient-filled progress |
| `LifeOSWeeklyRhythm` | The shared seven-day consistency row |
| `LifeOSBanner` | Inline notice with an action |
| `LifeOSEmptyState` | Honest empty state with one next step |
| `prismBrush()` | The single identity gradient brush (`horizontal`, `alpha`) |

---

## 11. Information architecture & navigation

### The primary bar

A floating frosted pill with five slots. Capture is **not** a tab — it is the
raised centre action that opens the full-window Capture hub, so it must not join
the tab selection model.

```
Home   ·   Notes   ·   [ ＋ Capture ]   ·   Insights   ·   Life
```

| Slot | Route | What it is |
|---|---|---|
| **Home** | `home` | The command centre: today's real progress, quick actions, today's tasks, habits, weekly rhythm, spend, recent activity, AI entry |
| **Notes** | `notes` | Long-form thinking: list, filters, editor |
| **＋ Capture** | `capture` | Full-window hub: thought, photo, video, audio, mood, location, attach |
| **Insights** | `insights` | Spec §27/§28 dashboard: weekly/monthly report + all-time stats, every figure from the offline engine |
| **Life** | `life` | The connected-history hub: timeline, calendar, search, and every module |

Labels are always visible; icon-only navigation is a UX trap. The active tab is a
**solid coral pill** (content in `onPrimary`) — loud enough that "where am I" is
never a guess, and coral holds the label at ~8.9:1 where a bright gradient stop
would not. The bar hides itself on full-window flows (Capture, Note editor, AI,
Snap, Global Search, App Lock setup) and while the Diary composer owns the window.

### What changed from the previous IA

- **The Insights tab is restored** (spec §4): `insights` is a primary tab again,
  backed by `ui/insights/InsightsViewModel` over the offline
  `LifeOSIntelligenceEngine` — weekly and monthly reports plus all-time personal
  statistics. The route, screen, view model and bar slot all exist.
- **"You" moved out of the bar.** The personal hub (profile, lock, backup,
  settings) is reached from Home's avatar and the Life hub, freeing the fourth
  slot for Insights as the spec requires.

### Route map

```
root_tabs (bottom bar visible)
├── home
├── notes
├── insights
└── life

full-screen / pushed (bar hidden or stacked above tabs)
├── capture            ── full-window sheet
├── note/{noteId}      ── full-window editor
├── ai                 ── full-height transcript
├── snap               ── camera
├── search             ── full-window search
├── profile            ── personal hub (pushed from Home avatar / Life)
├── timeline, tasks, habits, habits/{habitId}, expenses
├── diary + diary/search, diary/calendar, diary/insights, diary/{entryId}
├── settings, settings/app_lock
└── snap/view/{captureId}
```

---

## 12. Screen specifications

### Home — the command centre
Header (greeting, date, search, avatar) → **hero** (today's % of goals done, in
the identity gradient, with inline task/habit/spend stats) → quick-action rail
(Tasks · Habits · Money · Diary) → overdue banner (only when true) → today's
tasks (tappable completion) → habits (streak + progress) → weekly rhythm → spend
+ quick capture → recent activity → AI entry. AI is the **last** entry, never a
competing hero: today's real day ranks first.

### Notes — thinking surface
Title bar with search and new-note; filter pills (All · Recent · Pinned ·
Favorites · Archived); note cards with folder and updated time; pinned notes
marked; identity-gradient FAB. The editor is a full-window, distraction-free
surface.

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
is the paper pole of the design (§3).

---

## 13. Accessibility

- **Target:** WCAG 2.2 AA for text and UI components.
- **Contrast, checked at the token layer.** Every text/background pair in §3 is
  arithmetic-checked: `onSurface` 14.9:1 light / 13.6:1 dark, `onSurfaceVariant`
  6.4:1 / 7.1:1, `muted` 4.9:1 / 4.9:1, and the identity gradient's three stops
  5.4 / 4.5 / 4.8 against white. This is *why* the gradient stops are deep (§3) —
  the values, not the call sites, hold the contract. Final automated
  contrast/state verification (axe-style gate) remains part of the QA pass.
- **Content colour always comes from the theme, never from a static token.**
  `MaterialTheme.colorScheme.*` and `LocalLifeOSSemanticColors.current.*` flip
  with the theme; the raw `LifeOSPrimary` / `LifeOSSuccess` values are the *light*
  stops and do not. Used as a text or icon colour they measure 3.3:1 on the dark
  canvas — under AA. Every call site that had been doing this (39 of them) now
  reads the theme, and **no UI code reads the static brand tokens directly any
  more**: `LifeOSPrimary` / `LifeOSSecondary` / `LifeOSSuccess` / … are now purely
  the light-stop primitives the theme schemes are built from, and reach a screen
  only through `MaterialTheme.colorScheme.*` or `LocalLifeOSSemanticColors`.
  **One deliberate exception: the Diary.** Its screens render on a fixed paper
  surface (`diaryPage`, `DiaryPaperCard`) and never read `colorScheme.surface`, so
  they do not flip with the theme. Its ink and status tints (`DiaryInk*`,
  `DiaryActionViolet`, and the warning tint on the location row) therefore stay
  static on purpose — routing them through the theme would put the *dark* amber
  (`#E8B45C`, ~1.5:1 on cream) onto cream paper. The Diary's contrast is checked
  against the paper tokens, not the app canvas.
- Every text style scales to 200% without clipping; key numbers use
  `fontScale`-safe layouts and wrapping is allowed on labels. Section titles
  ellipsise rather than pushing their action off-screen.
- Colour is never the only signal: completion uses a check glyph, not just green;
  selected tabs use a filled pill *and* a filled icon; destructive actions carry
  a word.
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
- **Fonts:** bundled (Fraunces, Manrope, Kalam) — no runtime download, no internet
  permission.
- **Theming:** `LifeOSTheme` provides the Material scheme plus three composition
  locals — `LocalGlassColors`, `LocalAmbientGlowColors`,
  `LocalLifeOSSemanticColors` — so a palette change is a one-file edit and no
  screen reaches for a raw hex. (Three files still hardcode a colour: two camera
  overlays that must sit on live video, and the Diary insights positive/negative
  accent pair.)
- **Dynamic colour** stays opt-in (`dynamicColor = false` by default) so the
  LifeOS brand palette is stable.
- **Backwards compatibility:** every legacy public token and component name is
  preserved, so existing call sites adopt the new look without a rewrite and the
  app keeps compiling throughout the migration. Legacy names are honest about
  their lineage — `Glass*` now means "matte surface", `Prism*` means "identity
  gradient stop", and `PoppinsFamily` is a deprecated alias for `ManropeFamily`.

---

## 15. Change log

| Version | Name | Change |
|---|---|---|
| v2 | Neon/Gen-Z gradient | Original violet→magenta gradient language |
| v3 | Aurora Noir | Cinematic jewel tones, calmer palette |
| v4 | Prism Glass | Rebuilt from zero: obsidian canvas, painted glass material, single iridescent spectrum, reworked IA (Insights removed, "You" added), new type/shape/motion scales |
| **v5** | **Lumen** | **Rebuilt from zero again: warm paper canvas, MATTE surfaces (blur, sheen and dual-tone rim removed), clay/sage/amber palette replacing the cool spectrum, Fraunces + Manrope replacing Poppins at a heavier type scale, softer radii, deeper shadows, heavier springs, one deep identity gradient unified behind `prismBrush()`, serif section voice, solid-clay active tab** |
| **v6** | **AURA** | **Dark, spec-§53 frosted-glass language: deep lagoon (teal→plum) canvas, translucent frosted surfaces, soft borders, large radii, floating layers; warm coral signature + calm mint partner replacing the warm-paper palette; teal→rust→coral identity gradient; WCAG-AA checked in both themes; IA restored to the spec bar (Insights back, "You" moved out). Token names unchanged — a values-only swap behind the same contract.** |
