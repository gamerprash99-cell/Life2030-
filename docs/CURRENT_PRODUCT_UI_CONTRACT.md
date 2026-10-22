# LifeOS — Current Product & UI Contract

**Status:** authoritative, current as of 2026-10-12
**Supersedes (for the areas it covers):** the design-language token table and the
information-architecture details in `UI_UX_FLOW_NAVIGATION.md`, and the informal
phase notes in `REDESIGN_PLAN.md`.

This document is the single source of truth for *what LifeOS is right now*. Where
an older document disagrees with this one, this one wins for implementation
decisions, and the older document is retained as history (it records a phase, not
the current contract). Compare against the code before trusting any spec,
including this one.

---

## 1. Product premise

LifeOS is one private, on-device personal memory system. Notes, captures, diary
entries, tasks, habits, spending and insights are facets of a single body of
personal data, connected by time, search, tags and clear routes between them.

Two product promises are non-negotiable:

1. **Local-first.** Core features and personal data stay on the device. The app
   ships with no `INTERNET` permission and no network client, telemetry or cloud
   AI. Any change that adds one is a product decision, not an implementation
   detail.
2. **Data honesty.** Every number shown to the user is derived from persisted
   local data, or the UI shows a truthful empty/unavailable state. No fabricated
   statistics, no fake trends, no implied coverage that does not exist.

---

## 2. Information architecture (the five destinations)

Primary navigation is exactly five slots: **Home · Notes · (Capture) · Insights ·
Life**.

| Slot | Kind | Route |
|---|---|---|
| Home | Stateful root tab | `home` |
| Notes | Stateful root tab | `notes` |
| Capture | **Action, not a tab** — a raised centre button opening a full-window flow | `capture` |
| Insights | Stateful root tab | `insights` |
| Life | Stateful root tab | `life` |

Only `home`, `notes`, `insights` and `life` live inside the `root_tabs` graph and
are saved/restored per tab. Capture is intentionally *not* a stateful tab: it
opens the Capture hub overlay and is never a selected destination.

Every other screen is a **track module**: it stacks on top of the active tab and
system Back dismisses it one level at a time. Modules are **not** removed from the
product just because they are not in the bottom bar.

### 2.1 Module entry points

| Module | Route(s) | Reached from |
|---|---|---|
| Tasks | `tasks` | Home quick action · Home overdue banner · Life hub · AI action · Timeline |
| Habits | `habits`, `habits/{habitId}` | Home quick action · Home habits "See all" · Life hub · AI action · Timeline |
| Expenses | `expenses` | Home "Spent today" tile · Life hub · AI action · Timeline |
| Diary | `diary`, `diary/{entryId}`, `diary/search`, `diary/calendar`, `diary/insights` | Home "Diary" quick action · Life hub · AI action · global search |
| Timeline | `timeline` | Home "Recent activity" See all · Life hub · after posting a Snap |
| Global search | `search` | Home header search · Life hub · AI action |
| LifeOS AI | `ai` | Home (last section) · Insights |
| Profile / Settings / App Lock setup | `profile`, `settings`, `settings/app_lock` | Home avatar · Profile |
| Snap / capture viewer | `snap`, `snap/view/{captureId}` | Capture hub (Photo/Video) · Home "Quick capture" · recent-activity item · search · Timeline |

> Correction to `UI_UX_FLOW_NAVIGATION.md` §3.2: there is **no `life_calendar`
> route**. The calendar that exists is the Diary calendar (`diary/calendar`). The
> Life hub links to Timeline, Search, Diary, Tasks, Habits and Expenses.

### 2.2 Full-window routes (bottom bar hidden)

The primary bottom bar is hidden on: the Diary composer, `AppLockSetup`, `Snap`,
`Capture`, `NoteEditor`, `GlobalSearch`, and `AiAssistant`. Each of these owns the
window and its own insets. See `ui/navigation/BottomBarVisibility.kt` — the rule
is one testable fact (`shouldShowBottomBar`), not a scattered boolean.

### 2.3 Navigation rules

- Literal routes are declared **before** parameterised routes
  (`diary/search` before `diary/{entryId}`) so a literal is never swallowed.
- Root-tab taps use `popUpTo(startDestination) { saveState = true }` +
  `launchSingleTop` + `restoreState`, so a tab's stack is preserved.
- Detail screens stack above their originating context; Back returns to it.
- A screen that is reachable from several places must return to where it was
  opened, not to a fixed tab.
- `enableOnBackInvokedCallback` is honoured; no Back press is silently swallowed.

---

## 3. Design language — "Aurora Noir"

A cinematic, jewel-toned system: porcelain/obsidian canvas, iris primary, orchid
secondary, aurora-mint for success, champagne gold reserved for *earned* moments,
and neutral soft depth. This is the current token direction and matches
`ui/theme/Color.kt`.

> Correction to `UI_UX_FLOW_NAVIGATION.md` §1: the palette table there still lists
> the older neon `LifeOSPrimary #7C4DFF` / `LifeOSSecondary #D946EF`. Those hex
> values are **historical**. The live values are below.

| Role | Light | Dark | Token |
|---|---|---|---|
| Canvas | `#F5F3FA` | `#08070D` | `LifeOSBackgroundLight/Dark` |
| Surface | `#FFFFFF` | `#141220` | `LifeOSSurfaceLight/Dark` |
| Surface variant (recessed) | `#ECE8F5` | `#201D2E` | `LifeOSSurfaceVariantLight/Dark` |
| Primary text | `#141118` | `#F2F0FA` | `LifeOSTextPrimary*` |
| Secondary text | `#5A5470` | `#A9A3C2` | `LifeOSTextSecondary*` |
| Iris (primary action) | `#5A4BE8` | `#A99CFF` | `LifeOSPrimary` / scheme `primary` |
| Orchid (gradient partner) | `#8B3DDB` | `#D8A8FF` | `LifeOSSecondary` |
| Aurora mint (success) | `#0F9E8E` | `#5EEAD4` | `LifeOSTertiary` |
| Champagne gold (earned only) | `#B8860B` / `#E8B44A` | `#F0C868` | semantic `gold` |
| Warning | `#B45309` | `#FBBF24` | semantic `warning` |
| Danger | `#C81E3A` | `#FF8AA0` | scheme `error` |
| Outline | `#9A94AE` | `#6C6682` | `LifeOSOutline*` |

Non-color tokens: `LifeOSSpacing` (4/8/12/16/24/32/48/64 plus named
`screenPadding 24`, `sectionSpacing 20`, `minTouchTarget 48`,
`fabContentClearance 96`), `LifeOSRadius`, `LifeOSTypography`, `LifeOSShapes`,
and `Motion.kt` (one spring family + documented timings).

### 3.1 Design principles (the rules behind the tokens)

1. **Content leads.** User content outranks decoration on every screen.
2. **One dominant action per screen.** The next step is obvious.
3. **Progressive disclosure.** Secondary actions live in menus/sheets.
4. **One spacing rhythm and one type hierarchy.** Use tokens, not per-screen dp.
5. **One surface hierarchy** (canvas → content → raised/modal → selected/accent).
6. **Color carries meaning:** iris = action/selection, mint = success,
   gold = earned, red = destructive/error. Not every category is a loud accent.
7. **Motion explains state changes**, never decorates every recomposition.
8. **Touch targets ≥ 48dp**, including icon-only actions.
9. **Empty states teach the next action** and offer exactly one primary action.
10. **Errors explain recovery** (database key, backup, media, permission,
    reminder).
11. **Privacy is visible and honest** — local AI is described as local;
    unavailable features say so rather than pretending.

### 3.2 Glass and the ambient background

Glass is reserved for *chrome and a few hero surfaces*: the bottom bar, the raised
Capture action, selected top bars, and the occasional hero panel. Dense content —
lists, notes, forms, charts, expense rows, long-form reading — uses solid or
near-opaque surfaces. Never layer glass on glass more than one level. Do not use
glow as the default focus indicator. Test contrast against the *composited*
background, not the source colour. See `ui/components/LiquidGlass.kt` and
`LifeOSAmbientBackground`.

### 3.3 The Diary exception

Diary and the diary-derived Timeline deliberately keep a **paper/handwriting**
aesthetic (warm paper even in dark mode, pen-ink controls, mood palette) and are
**not** unified into Aurora Noir. This is an intentional product choice, not a
missed migration. The boundary is *distinct expression, shared rules*: Diary uses
its own palette but the app's shared behaviour — accessible typography, 48dp
targets, insets, Back semantics, destructive-action patterns, and data
semantics. Diary tokens live at the bottom of `Color.kt` and are used only by the
Diary module (and the shared timeline that renders diary entries).

---

## 4. Screen responsibilities (summary)

- **Home** — today's command centre, ordered: today's progress → tasks → habits →
  weekly rhythm → spending/capture → recent activity → LifeOS AI (last).
- **Notes** — long-form thinking; search, the five smart views, folders, overflow
  actions. Editor: title + body + tags, explicit Save, unsaved-changes guard.
- **Capture** — six capture types with an inline quick-thought field; permission
  requested only for the selected capability.
- **Insights** — on-device analytics over permitted local data; honest "not enough
  data yet" states; no causation claims.
- **Life** — connected-history hub: Timeline, search, and quick links to the track
  modules.
- **Track modules** — Tasks, Habits, Expenses, Diary, Timeline, Search, Profile,
  Settings, App Lock, Snap. Preserve their existing data paths.

Diary keeps its paper identity (see §3.3). Snap keeps its full-window camera.

---

## 5. Data, permissions and privacy contract

- **No internet permission, no network code, no analytics, no cloud AI** in this
  build. Re-audit the manifest and dependency tree whenever a dependency is added.
- **Permissions are requested at the moment of use**, never at launch: camera and
  microphone for the specific capture, location for a location capture, exact-alarm
  and notification access only when a timed reminder is actually armed. A denied
  permission must leave unrelated features fully usable.
- **Encrypted at rest.** Room runs over SQLCipher; the passphrase is wrapped by an
  Android Keystore key. A key failure routes to `DataKeyErrorScreen`, which is
  non-destructive: it preserves the database and offers Retry / Exit. It never
  deletes or regenerates a key over existing encrypted data.
- **Persistence is preserved.** Presentation may change; existing records, ids,
  folders, timestamps, attachments and relationships must stay intact. Do not
  rebuild the Room database or change entities for a visual change.
- **App Lock** uses the platform BiometricPrompt and a hashed PIN; no biometric
  material is stored, no hardcoded bypass exists.

---

## 6. Accessibility & responsiveness baseline

- WCAG 2.2 AA: body ≥ 4.5:1, large ≥ 3:1, measured on the composited surface in
  **both** themes.
- Touch targets ≥ 48dp (`LifeOSSpacing.minTouchTarget`).
- Every icon-only action has a meaningful content description; tabs/chips expose
  `Role.Tab`/`selected` semantics; toggles expose state.
- Colour is never the only signal (mood, category, completion, warning).
- Text scales to 200% without clipping or overlapping numeric charts.
- Edge-to-edge content clears system bars, gesture insets and the keyboard.

---

## 7. Definition of done for a screen change

A screen is done when: its primary task is obvious; existing features and data
paths are preserved; empty/loading/populated/error states are designed;
keyboard/insets/Back are correct; text scales; contrast and semantics are checked;
local data updates flow through; it works offline; no new permission or network
path was added; relevant tests pass; it has been compared in light and dark mode;
it reuses shared tokens/components; and the change is recorded in `UPDATE.md`.

---

## 8. Relationship to the other documents

| Document | Role now |
|---|---|
| **This file** | Authoritative current product/UI contract |
| `REDESIGN_PLAN.md` | Phase log for the Aurora Noir build; §6 dead-code sweep still pending |
| `UI_UX_FLOW_NAVIGATION.md` | Detailed flow/IA reference — **read with the corrections in §2.1 and §3**; its palette table and some module details are historical |
| `PRD.md` / `TRD.md` | Product & technical requirements |
| `docs/*` | Per-area documentation; `docs/DESIGN.md` referenced by `DOCUMENTATION_AUDIT.md` does not exist yet |
| `UPDATE.md` | Running change log; keep history, append only |

History is preserved. When an older document is contradicted, correct the *active*
document and link the older material as historical rather than deleting it.
