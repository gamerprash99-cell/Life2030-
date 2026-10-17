# LifeOS Redesign — Working Plan (v3 "Aurora Noir")

Direction chosen by the user: **a new premium visual language**, built on top of
the current working tree (nothing discarded), adding AI/insights, a Notes +
Capture hub, and global search/widgets, while removing dead code.

## The new design language — "Aurora Noir"

A cinematic, jewel-toned system that replaces the neon violet→magenta "Gen-Z"
gradient with a calmer, more premium palette:

| Role            | Light            | Dark             | Notes |
|-----------------|------------------|------------------|-------|
| Canvas          | porcelain        | obsidian         | calm, slightly tinted |
| Brand Iris      | `#5A4BE8`        | `#A99CFF`        | primary, the "press me" colour |
| Orchid          | `#8B3DDB`        | `#D8A8FF`        | gradient partner (calm, not hot pink) |
| Aurora Mint     | `#0F9E8E`        | `#5EEAD4`        | success / secondary accent |
| Champagne Gold  | `#B8860B`/`#E8B44A` | `#F0C868`     | premium highlight: streaks, achievements |
| Rose            | `#C81E3A`        | `#FF8098`        | danger only |

Depth is **neutral and soft** (not brand-coloured), glass is **quieter** (a
finer rim, a softer sheen) so content leads, and motion uses one spring family.

## Execution phases (each ends with a green build)

1. **Design tokens v3** — `Color.kt`, `Type.kt`, `Shape.kt`, `Spacing.kt`,
   `Motion.kt`, `Theme.kt`. All public token names kept (source-compatible).
2. **Glass engine v3** — `LiquidGlass.kt` (depth, rim, sheen, ambient background).
3. **Component kit v3** — `LifeOSKit.kt`, `LifeOSBottomBar.kt`, `LifeOSTopBar.kt`,
   `GlassCard.kt`, `LifeOSDesignComponents.kt`, plus new shared pieces.
4. **Navigation + features** — Notes, Capture hub, Insights, AI assistant,
   Global search, widgets.
5. **Screens, one by one** — Home, Capture, Notes, Insights, AI, Tasks, Habits,
   Expenses, Diary, Timeline, Search, Profile, Settings, App Lock, Snap, Onboarding.
6. **Dead code sweep** — remove unused tokens/components/functions.

## Progress log

- [x] Baseline build green (`:app:compileDebugKotlin`).
- [x] Phase 1 — tokens v3 (`Color.kt` Aurora Noir palette, `Theme.kt` schemes +
      gold/goldSoft/shadow semantics, `Type.kt`, `Shape.kt` sheet 34dp, `Motion.kt`
      `Exit`/`gentle`/`enter`/`exit`/`staggerDelay`/`LifeOSElevation.hero`).
- [x] Phase 2 — glass engine v3 (`LiquidGlass.kt` neutral soft shadow, finer sheen,
      1dp/1.2dp rim, softened FAB shadow).
- [x] Phase 3 — kit v3 (`LifeOSKitExtras.kt`: LifeOSSearchBar, LifeOSFilterChips,
      LifeOSStatTile, LifeOSSkeleton(+List), LifeOSIconButton, LifeOSHeroCard,
      LifeOSEyebrow, LifeOSBanner).
- [x] Phase 4 — navigation + features:
      - New 5-slot bottom bar **Home · Notes · ＋Capture · Insights · Life**
        (`LifeOSBottomBar.kt`); Capture is a raised centre action.
      - New routes in `Screen.kt` (Notes, NoteEditor, Insights, Life, Capture,
        GlobalSearch, AiAssistant) registered in `LifeOSNavHost.kt`.
      - `BottomBarVisibility.kt` full-window set extended to Capture, NoteEditor,
        GlobalSearch, AiAssistant; covered by `BottomBarVisibilityTest`.
      - New screens: Capture hub, Notes (+ editor), Insights, Life hub,
        Global search, and the on-device **LifeOS AI** assistant.
      - Home redesigned from scratch as the command centre (daily-update hero,
        quick-action rail, AI nudge, today's tasks/habits, weekly rhythm, spend,
        recent activity).
      - **Widgets**: a "Today" home-screen widget (`widget/TodayWidgetProvider`,
        classic `RemoteViews` — Glance isn't in the offline cache), reading the
        same local DB: tasks left today, habit check-ins, spend today.
- [x] Phase 5 — screens. Home redesigned from scratch; Habits and Expenses moved
      onto the shared empty-state/eyebrow vocabulary; **Tasks** gained an animated
      progress hero and a pill count header; **Profile** gained a section eyebrow;
      **HabitDetail** replaced its stray Material `TopAppBar` with `LifeOSTopBar`
      to match every other screen. **Settings, App Lock, Snap, Onboarding** were
      audited and already render on the new system (tinted hero cards, section
      headers, gradient CTAs, animated page transitions) — no churn needed.
      **Diary / Timeline keep their deliberate paper/handwriting aesthetic**
      (refine-only per user choice), not unified into Aurora Noir.
- [x] Phase 6 (partial) — removed speculative `LifeOSRowGap` and newly-unused
      imports. Broader sweep still pending after Phase 5 completes.

---

## Superseding pass — Figma fidelity rebuild (v4)

The direction changed. Rather than hand-authoring a house language ("Aurora
Noir"), every screen was rebuilt from the supplied Figma file
(`ehSpxDfVot6SgPIdd2Tvf4`), which is now the single source of truth for colour,
type, spacing and layout. Nothing below is discarded — the data layer,
ViewModels, routes and features are untouched; this is presentation only.

What that means for the phases above:

- **Tokens (Phase 1)** are now *derived* from the Figma frame rather than
  invented: canvas top ≈ `#1F3B46`, mid ≈ `#0D161F`, bottom / nav `#102029`,
  card `#1C303A`, mint `#A8E8D3`. Values were sampled from the exported frame
  renders in `design/figma/`, not eyeballed.
- **Type (Phase 1)** is single-voice Inter (one variable font). The Fraunces /
  Manrope / Kalam files and their families are gone.
- **The kit (Phase 3)** is replaced by `ui/components/LifeOSCore.kt`.
  `LifeOSDesignComponents.kt`, `LifeOSTopBar.kt` and the superseded kit pieces
  were deleted; screens now compose from the new kit only.
- **Screens (Phase 5)** were rebuilt, not restyled: each uses the Figma page
  header (86dp: 9sp mint eyebrow + 24sp title + 42dp action), the canvas, the
  24dp/gap-18 content rhythm and the r22 card. No destination applies its own
  system insets — `LifeOSNavHost`'s `Scaffold` does that once for all of them.
- **Diary / Timeline still stand apart on purpose.** The design's Diary frame is
  a page, not a card, so the paper palette (`theme/DiaryColors.kt`) and
  `GlassCard` are kept; the app chrome around them is the only thing rebuilt.
- **Dead-code sweep (Phase 6)** is now complete for the app chrome.

Reference renders live in `design/figma/` (git-ignored, 5 MB — design process,
not shipped assets).
