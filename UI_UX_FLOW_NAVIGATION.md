# LifeOS — UI/UX, Flow & Navigation

> Companion to `PRD.md` and `TRD.md`. Describes the redesigned information
> architecture, the navigation graph, every key user flow, the screen
> inventory, the design language, and the accessibility rules.

> **Corrections — 2026-10-12.** Some details below are historical and no longer
> match the code. Read them with these corrections, and treat
> [`docs/CURRENT_PRODUCT_UI_CONTRACT.md`](CURRENT_PRODUCT_UI_CONTRACT.md) as the
> authoritative contract:
> - **Palette (§1):** the current tokens are Aurora Noir —
>   `LifeOSPrimary #5A4BE8` and `LifeOSSecondary #8B3DDB` (light). The older
>   `#7C4DFF` / `#D946EF` values recorded here are superseded.
> - **Calendar (§3.2, §4.5):** there is no `life_calendar` route; the calendar
>   that exists is the Diary calendar (`diary/calendar`).
> - **Notes editor (§4.2):** the editor has a title, a body and tags with an
>   explicit Save; there is no rich-text toolbar, no attachments and no in-editor
>   AI sheet in the current build.
> - **AI (§4.10, §5.3):** LifeOS AI is an on-device, rule-based assistant over
>   permitted local data. There is no on-device speech-to-text diary flow and no
>   external/cloud processing.

---

## 1. Design Language — Glassmorphism, used with discipline

**Feel:** premium, minimal, modern, futuristic, calm, personal, Gen-Z — but
readability, accessibility, battery and performance come first.

**Rules**
1. Glass is for **chrome and hero surfaces** (bottom bar, top bar, hero cards,
   capture sheet, AI panel). Body/list surfaces may be opaque for performance.
2. Never stack glass on glass more than one level deep.
3. Text on glass must still meet **WCAG AA** (4.5:1 body, 3:1 large). When in
   doubt, darken the surface or drop the blur.
4. Motion is smooth and subtle (springs, 150–400ms); no gratuitous animation.
5. Large rounded corners; soft borders (1dp hairline); soft shadows; layered
   surfaces; a few floating elements.

**Tokens** (single source of truth, `ui/theme/`):

| Token group | File | Examples |
|---|---|---|
| Color | `Color.kt` | `LifeOSPrimary #5A4BE8`, `LifeOSSecondary #8B3DDB` (Aurora Noir — see the corrections note), surface/background, category accents, glass tints |
| Type | `Type.kt` | display / headline / title / body / label scale |
| Shape | `Shape.kt` | `LifeOSRadius.small/medium/large/xlarge/pill` |
| Spacing | `Spacing.kt` | `screenPadding 24`, `sectionSpacing 20`, `minTouchTarget 48` |
| Glass | `Theme.kt` | `LocalGlassColors { surface, border }` (light + dark) |

**Glass icon system** (spec §54) — soft depth, translucent, consistent
proportions:

| Concept | Icon |
|---|---|
| Notes | Glass Document |
| AI | Glass AI Orb |
| Tasks | Glass Checklist |
| Habits | Glass Flame |
| Expenses | Glass Wallet |
| Diary | Glass Journal |
| Capture | Glass Camera |
| Insights | Glass Graph |
| Calendar | Glass Calendar |
| Settings | Glass Gear |

---

## 2. Information Architecture

```
LifeOS
├── HOME            → command center ("What's happening in my life today?")
├── NOTES           → smart notes (+ editor, folders, AI actions)
├── + CAPTURE  ★    → photo · video · audio · thought · mood  (centre, prominent)
├── INSIGHTS        → monthly dashboard · productivity · habits · money · AI
└── LIFE            → timeline · calendar · search · memory hub
                      └ reaches Tasks · Habits · Expenses · Diary · Memories
```

---

## 3. Navigation Model

### 3.1 Bottom navigation (5 sections)
A floating glass pill, always-visible stacked labels. The centre **`+`** is a
raised, gradient, larger target — the most pressable element on screen.

```
┌──────────────────────────────────────────────────────────────┐
│   ⌂          ✎          ╭─────╮          ◫          ◔         │
│  Home      Notes       │  ＋  │       Insights     Life       │
│                        ╰─────╯                                │
└──────────────────────────────────────────────────────────────┘
```

- Tabs 1,2,4,5 are `root_tabs` destinations (saved/restored per tab).
- The `+` opens the **Capture hub** overlay from anywhere; it is not a stateful
  tab.
- Re-tapping the visible tab is a no-op; tapping another tab restores its stack.

### 3.2 Screen graph

```
root_tabs
 ├─ home
 ├─ notes ──────────────▶ note_editor/{noteId}   (noteId = "new" | id)
 ├─ insights ───────────▶ ai_assistant           (LifeOS AI)
 └─ life ───────────────▶ timeline
                         ├▶ life_calendar
                         ├▶ global_search
                         ├▶ diary ─┬▶ diary/search
                         │         ├▶ diary/calendar
                         │         ├▶ diary/insights
                         │         └▶ diary/{entryId}
                         ├▶ tasks ─▶ (task editor sheet)
                         ├▶ habits ─▶ habits/{habitId}
                         └▶ expenses

overlays (outside root_tabs)
 ├─ capture            (full-window; from centre + and from Home)
 │   └▶ capture_view/{captureId}
 ├─ timeline, life_calendar, global_search, diary*, tasks, habits*, expenses
 ├─ ai_assistant
 └─ profile ─▶ settings ─▶ settings/app_lock
```

Rules:
- Only `home/notes/insights/life` live in `root_tabs`; everything else stacks on
  top and Back dismisses one level.
- Literal routes are declared before argument routes (`diary/search` before
  `diary/{entryId}`).
- Full-window surfaces (Capture, Diary composer, AI) call
  `ComposerWindowOwner()` → the bar hides and the surface owns the bottom inset.

### 3.3 Back behaviour
| From | Back goes to |
|---|---|
| A tab | Previous tab (or exits at Home) |
| Overlay | The screen beneath it |
| Capture hub | Where it was opened from |
| Editor with unsaved changes | Confirm-discard dialog |

---

## 4. Screen Inventory

### 4.1 Home
- **Header**: greeting + full date + day status dot + profile avatar.
- **Today's Progress** card: animated ring/bar %, plus task/habit/spend chips.
- **Quick Actions**: Capture · Task · Expense · Diary.
- **Today's Tasks**: real, inline-toggleable.
- **Today's Habits**: real, with streak badges.
- **Today's Spending** and **Latest Memory** cards.
- **Prominent `+ CAPTURE`**.
- Reorders by time of day (morning vs evening) — same data, different priority.

### 4.2 Notes
- Top bar: title + search + sort/menu.
- Filter chips: **All · Recent · Pinned · Favorites · Archived**.
- Folder rail: College · Projects · Ideas · Personal · Finance · Other (+ new).
- Grid/list of note cards (pin/favorite markers, tag chips, updated time).
- Swipe/menu actions: pin, favorite, archive, move, delete.
- FAB → **Note Editor**.
- Editor: title, rich-text toolbar (B/I/U, H1, bullets, numbers, checklist,
  highlight, link), tags, attachments (photo/video/audio/file), and an
  **✨ ASK LIFEOS AI** sheet.

### 4.3 Capture hub (centre `+`)
Full-window glass sheet, six big tiles: **Photo · Video · Audio · Thought ·
Mood · (Location optional)**. Quick-thought field is inline and borderless for
speed. After capture: confirmation with a real preview, then it lands on the
Life Timeline automatically.

```
+ CAPTURE
┌───────────────┬───────────────┐
│ 📷  Photo     │ 🎥  Video     │
├───────────────┼───────────────┤
│ 🎙  Audio     │ ✍  Thought    │
├───────────────┼───────────────┤
│ 😊  Mood      │ 📍  Location  │
└───────────────┴───────────────┘
        ＋ quick thought…
```

### 4.4 Insights
- Month header + segmented period (Week · Month · Year).
- "YOUR MONTH" score cards: task completion %, habit consistency, money spent,
  diary entries, memories captured.
- Highlights: best habit, biggest spending category, most productive day.
- Productivity panel: this vs last period, improvement, most active times.
- Entry points to habit analytics and expense insights.
- **✨ Ask LifeOS AI** entry.

### 4.5 Life (hub)
- Tabs/segments: **Timeline · Calendar · Search**.
- **Timeline**: dated, spine-and-node journal merging tasks, habits, expenses,
  diary, notes, captures; each entry tappable to its source.
- **Calendar**: month grid with per-day activity dots/counts; tap a day for the
  full day summary (tasks, habits, expenses, diary, memories, notes, photos).
- **Search**: one field, grouped results (Notes / Diary / Memories / Photos /
  Expenses / Audio).
- Quick links to Tasks, Habits, Expenses, Diary, Memories.

### 4.6 Tasks
Scopes (Today / Upcoming / Overdue / All), priorities, categories, repeat,
reminders; overdue reschedule sheet; AI task extraction review sheet.

### 4.7 Habits
List with streaks; detail with current/longest streak, monthly completion,
missed days, and a GitHub-style heatmap (weekly/monthly/yearly).

### 4.8 Expenses
Monthly total card, daily average, user-set budget (with an honest "no budget"
state), recent transactions, category breakdown; FAB → add-expense sheet with a
single highlighted category; payment notifications opt-in.

### 4.9 Diary (Daily Memory)
Editorial, paper feeling distinct from Notes: day masthead, centre-keeping date
strip, hairline spine, mood-first composer, post-save confirmation, full-page
detail; search, calendar, and local intelligence reached from the header.

### 4.10 LifeOS AI
Not a generic chatbot — it feels native:
- Greetings line: *"How can I help with your life?"*
- Suggestion chips: *How was my week? · What did I spend? · Show unfinished
  tasks · Analyze my habits · Summarize my diary*.
- Answers are **cards** (metric + source + a `VIEW …` action).
- **Permission banner**: shows exactly which modules AI may read; a per-request
  privacy notice before any external processing (Cancel / Allow).
- Actions require review: *I found 4 tasks … [CREATE ALL] [REVIEW]*.

---

## 5. Key User Flows

### 5.1 Capture in seconds (FR-C, spec §21/22)
```
Launch ─▶ Home (or any tab) ─▶ tap ＋ ─▶ choose Photo
       ─▶ shoot ─▶ optional caption/mood/tag ─▶ Save
       ─▶ confirmation (names day + minute) ─▶ added to Life Timeline
```

### 5.2 Note → tasks (AI, spec §8/32/33)
```
Notes ─▶ open/create note ─▶ ✨ ASK LIFEOS AI ─▶ "Extract tasks"
      ─▶ AI shows "TASKS FOUND: …" with suggested date
      ─▶ user edits/reviews ─▶ [CREATE TASKS] ─▶ tasks persisted
```

### 5.3 Voice → diary (AI, spec §20/34)
```
Diary ─▶ New memory ─▶ 🎙 record
      ─▶ on-device speech-to-text ─▶ AI drafts title + text + mood + tags
      ─▶ [EDIT] [REGENERATE] [SAVE]  (never auto-saved)
      ─▶ saved to Timeline ─▶ feeds monthly summary
```

### 5.4 Expense in seconds (FR-E, spec §14)
```
Expenses ─▶ ＋ ─▶ pick category (single highlight) ─▶ amount ─▶ Save
         ─▶ recorded ─▶ monthly total + insights update
```

### 5.5 Ask about your life (AI, spec §29/35/36)
```
Insights (or AI) ─▶ ✨ Ask LifeOS AI ─▶ "How was my August?"
  ─▶ permission check (modules only) ─▶ engine reads permitted repos
  ─▶ answer card: tasks %, best habit, spend + biggest category,
     diaries, memories, most productive day
```

### 5.6 Daily review (spec §49)
```
Evening ─▶ Home shows review entry (or notification)
        ─▶ YOUR DAY: tasks x/y, habits x/y, spend, memories, diary status
        ─▶ HOW WAS YOUR DAY? (quick mood) ─▶ optional note ─▶ save
```

### 5.7 First launch (spec §42)
```
Welcome ─▶ "Your data is yours" ─▶ create LifeOS space ─▶ optional restore
        ─▶ (permissions requested later, only when a feature needs them)
```

---

## 6. States & Empty States

Every list/collection must define: **loading** (skeleton/shimmer), **content**,
**empty** (honest invitation + one primary action), **error** (retry), and
**success/confirmation** (names what was stored). No fabricated numbers, no
"0/0" placeholders — an empty state is an invitation, not a broken counter.

---

## 7. Accessibility (WCAG 2.2 AA)

- Contrast: body ≥ 4.5:1, large ≥ 3:1, verified against glass tints in both
  themes.
- Touch targets ≥ 48dp (`LifeOSSpacing.minTouchTarget`).
- TalkBack: every icon has a content description; toggles expose
  `stateDescription`; the bottom bar exposes `Role.Tab` + `selected`.
- Support font scaling to 200% and stacked bottom-bar labels (icon above text)
  with ellipsis fallback on narrow screens.
- Motion respects reduced-motion where feasible; no flashing.
- Predictive back (`enableOnBackInvokedCallback="true"`) is honoured; no Back
  press is swallowed.

---

## 8. Visual Hierarchy Rules

- One thing leads each screen (a single hero card/section).
- Glass chrome floats over a calm, slightly tinted page background.
- Colour communicates meaning consistently: primary = action/selection,
  success = completion, warning = streak/attention, danger = destructive.
- Emoji are used only as content (moods, categories), never as UI controls, and
  never in navigation labels.

---

## 9. Copy & Tone (`ux-writing`)

- Clear, concise, human, honest. Errors use **what → why → how**.
- Confirmations name the real stored value ("Saved to Saturday, 15 Aug · 10:42").
- Privacy copy is plain-language and specific about what leaves the device.
- Destructive copy is explicit; irreversible actions require typing `DELETE`.

*End of UI/UX, Flow & Navigation.*
