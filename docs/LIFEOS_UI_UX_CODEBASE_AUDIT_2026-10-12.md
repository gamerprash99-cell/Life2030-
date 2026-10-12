# LifeOS UI/UX and Codebase Audit
**Review date:** 12 October 2026  
**Repository reviewed:** gamerprash99-cell/Life2030-  
**Branch:** audit/ui-ux-codebase-review-2026-10-12  
**Scope:** Static repository review of product/UI specifications, app entry and startup, navigation, theme/motion/components, representative feature screens, manifest/build configuration, tests, CI, and documentation.

> This is a source-based audit, not a claim that the app was installed and manually tested on physical devices. Findings are labeled as confirmed from repository evidence, risks requiring runtime verification, or recommendations. No application source, database schema, migrations, or user-facing features were changed as part of this audit branch.

## Executive summary

LifeOS already has substantial product and engineering work. It is a native Kotlin/Jetpack Compose app with a single application module, local Room persistence, SQLCipher integration, DataStore, local reminders, media capture, app-lock components, an offline intelligence package, navigation, tests, and several written product/design plans. This is not a blank or inherently unsalvageable codebase.

The current challenge is that the product has accumulated multiple layers of design decisions and many feature surfaces. The repository's redesign plan says the “Aurora Noir” design-system phase and major screen phase have been implemented, while the broader dead-code sweep remains pending. The UI/navigation specification still describes an earlier neon violet/magenta palette and some older navigation concepts. The UPDATE history also documents a previous navigation structure that differs from the newer five-section plan. These are clear signs of documentation/design drift and a need for one authoritative product contract.

The most important recommendation is **not to redesign every screen simultaneously**. Establish one canonical design system, measure the launch journey, improve the first-use and Home experience, then refactor shared components and migrate screens in a controlled sequence. Remove dead code only after static and runtime reachability checks. Preserve the working features and local data model.

### Highest-priority findings

| Priority | Finding | Evidence / status | Recommended action |
|---|---|---|---|
| P0 | Design specifications are not fully synchronized | Confirmed: REDESIGN_PLAN.md specifies Aurora Noir; UI_UX_FLOW_NAVIGATION.md still documents an older violet/magenta token set and older screen assumptions | Establish a single source of truth and mark older documents as historical or update them without erasing history |
| P0 | Startup is a complex, critical UX path | Confirmed: MainActivity and LifeOSApplication gate composition on encrypted Room/database initialization and provide a DataKeyErrorScreen | Measure cold/warm startup; ensure splash never hangs and key/database failures have humane, actionable recovery |
| P0 | A green CI build is not a UX sign-off | Confirmed: latest visible run #283 reports success and includes unit tests plus debug/release assembly according to the workflow | Add repeatable UI/navigation/accessibility checks and real-device smoke testing |
| P1 | Dead-code cleanup is explicitly incomplete | Confirmed: REDESIGN_PLAN.md calls the wider sweep pending; tasks/todo.md records specific cleanup work | Build a reachability inventory and remove only proven-unreferenced symbols in small, tested batches |
| P1 | Design consistency must be checked screen-by-screen | Confirmed: many separate screens and component files exist; the plan intentionally preserves Diary's paper aesthetic | Audit actual runtime screenshots, typography, spacing, surfaces, empty states, and navigation per screen |
| P1 | The app has a broad feature surface and complex routes | Confirmed: navigation registers capture, notes, diary, tasks, habits, expenses, search, AI, profile/settings and other screens | Validate every primary journey and system Back behavior; simplify discovery rather than deleting modules |
| P1 | Glass and ambient background need performance/accessibility validation | Confirmed: shared LiquidGlass and LifeOSAmbientBackground are central to visual styling; actual frame-time and contrast were not measured | Use opaque surfaces for dense content, keep glass selective, verify contrast and reduced-motion behavior |
| P2 | Static analysis and dead-code claims are not yet evidenced by this audit | No lint or IDE usage analysis was executed against a local checkout | Add lint, compiler warnings review, dependency analysis, and call-site verification to the cleanup process |

## 1. Audit scope and confidence

### What was inspected

- Repository root and recursive tree on the default branch.
- Recent commits and the latest visible Android Actions run.
- REDESIGN_PLAN.md, UI_UX_FLOW_NAVIGATION.md, PRD.md, TRD.md, UPDATE.md, and FINAL_RELEASE_CHECKLIST.md.
- app/build.gradle.kts and AndroidManifest.xml.
- MainActivity.kt and LifeOSApplication.kt.
- Theme files: Color.kt, Theme.kt, Motion.kt.
- Shared visual components: LiquidGlass.kt and LifeOSBottomBar.kt.
- Representative screens: HomeScreen.kt, ExpensesScreen.kt, HabitsScreen.kt, CaptureHubScreen.kt.
- LifeOSNavHost.kt.
- The source/test inventory and the Android CI workflow.
- Repository code-search results for the named shared bottom-bar component and related cleanup references.

### What was not verified

- No physical device or emulator was launched during this repository review.
- No screenshots, video recordings, frame-time traces, memory profiles, or accessibility scanner output were available.
- No local Gradle command was run by this review.
- No complete IDE/compiler usage graph was generated.
- No exhaustive line-by-line review of all 237 Kotlin source files was performed.
- No destructive code changes were made, and no claim is made that a specific composable or class is definitely unused unless its reachability is verified.

### Evidence labels used throughout

- **Confirmed:** directly visible in repository files or GitHub Actions metadata.
- **Risk:** plausible concern requiring runtime, call-site, or device validation.
- **Recommendation:** proposed product or engineering change, not a statement that the current code is broken.
- **Not verified:** evidence is insufficient to conclude either success or failure.

## 2. Repository and engineering baseline

The default branch contains 382 tree entries, including 237 Kotlin files, application source, tests, Gradle wrapper/configuration, design/product documentation, and a stored ZIP related to a Diary paper redesign. The project is organized as a single Android application module with packages for core services, data, domain, UI, navigation, theme, and widget code.

The application build configuration identifies Kotlin/JVM target 17, compile SDK 35, min SDK 26, target SDK 35, Compose, Navigation Compose, Room, KSP, SQLCipher, DataStore, WorkManager, Biometric, CameraX, Coil, and test dependencies. The source set includes unit tests and Android instrumentation tests.

The latest visible commit is dd36cca5807d8cf04addaeb560052c57728c0d6b, with the message “Fix latest Android CI compilation failures.” Its description says it restores missing AndroidX dependencies, aligns legacy UI and offline intelligence with current repositories, and repairs Room v6 schema tests. The associated run #283 is marked successful. Its workflow steps include unit tests, debug APK assembly, release APK assembly, and APK artifact upload.

**Interpretation:** The codebase is actively evolving and has real infrastructure. The latest CI signal is good, but the prior compilation failures and ongoing cleanup tasks justify a stabilization pass before adding more visual complexity.

### Immediate engineering baseline checklist

1. Record the exact default branch commit SHA and the current working-tree state before local work.
2. Run the existing CI workflow locally where possible; do not infer local success from GitHub status.
3. Capture baseline startup timings on at least one lower-mid-range Android device and one recent device.
4. Record a screenshot for each key route in light mode and dark mode.
5. Keep the current APK and a data backup for testing; never use a destructive database reset as a shortcut.
6. Keep a baseline list of navigation destinations, permissions, database entities, and current test results.

## 3. Product and design direction: resolve the specification conflict

### Confirmed discrepancy

REDESIGN_PLAN.md names “Aurora Noir” as the chosen premium visual language: porcelain/obsidian canvas, iris/indigo primary, orchid secondary, mint for success, champagne gold for selected achievement moments, and neutral soft depth. It describes the new shared token system, glass engine, component kit, five-slot Home/Notes/Capture/Insights/Life navigation, and a broader dead-code sweep that is still pending.

UI_UX_FLOW_NAVIGATION.md describes an older design language with primary #7C4DFF, secondary #D946EF, and a more generalized glassmorphism concept. Its information architecture also describes Home/Notes/Capture/Insights/Life, but its wording and token details do not fully match the newer Aurora Noir direction. UPDATE.md contains an earlier navigation history with Home/Tasks/Habits/Insights as primary tabs. These documents may be snapshots from different phases, but they are not currently a single unambiguous specification.

### Why this matters

When different documents define different palettes, primary tabs, or screen responsibilities, engineers can make locally reasonable choices that produce a globally inconsistent app. A screen may technically use MaterialTheme while still having an older layout, a different card radius, a different gradient, a different navigation expectation, or a separate motion pattern. More animation cannot solve a product contract that is unclear.

### Required resolution

Create a concise, authoritative “Current Product and UI Contract” section in the active design document. It should state:

- The five main destinations: Home, Notes, Capture, Insights, Life.
- Capture is a central action, not an additional stateful tab.
- Tasks, Habits, Expenses, Diary, Timeline, Calendar, Search, Profile, Settings, and App Lock remain available through defined paths.
- Aurora Noir is the current token direction, subject to the actual user-approved brand choice.
- Diary keeps its deliberate paper/handwriting visual identity but uses shared behavior, spacing, accessibility, and navigation rules.
- All screens must use shared type, spacing, shape, color, motion, and component tokens unless there is an explicitly documented reason not to.
- Offline-first, privacy-first behavior is part of UX, not merely an engineering detail.
- Every metric shown to the user must be based on local data or display a truthful empty/unavailable state.

Do not erase the earlier documentation history. Update the active spec, link older material as historical, and record the decision in UPDATE.md.

## 4. The redesign principle: one product, not a stack of unrelated screens

LifeOS's strongest product idea is that notes, captures, diary entries, tasks, habits, spending, and insights form one personal memory system. The design should communicate that connection through shared time, search, tags, and clear routes.

The app should feel like a calm, high-quality personal utility. “Premium” should come from clarity, consistency, fast response, thoughtful details, typography, and confidence—not from putting every screen inside gradients, blur, glowing borders, or oversized cards.

### Design principles to standardize

1. **Content leads.** User notes, tasks, habit completion, spending totals, diary entries, and memories should be more visually important than decoration.
2. **One dominant action per screen.** A user should quickly understand the primary next step.
3. **Progressive disclosure.** Keep secondary actions in menus, details, or sheets rather than displaying every possible control at once.
4. **One spacing rhythm.** Use shared tokens and a small scale rather than arbitrary padding on each screen.
5. **A consistent type hierarchy.** Use display text only for one hero or page-level moment; titles and body content should remain readable and compact.
6. **One surface hierarchy.** Use the page canvas, standard content surface, raised/modal surface, and selected/accent surface consistently.
7. **Color communicates meaning.** Iris for primary action; mint for positive/success; gold for earned or notable achievements; red for destructive/error. Do not make every category a saturated accent.
8. **Motion explains state changes.** Animate selection, expansion, confirmation, and progress. Do not animate every element on every recomposition.
9. **Touch targets are forgiving.** Keep interactive areas around the established 48dp minimum, including icon-only actions.
10. **Empty states teach the next action.** “No items” should explain how to start and offer one direct action.
11. **Errors explain recovery.** Especially for database key, backup, media, permission, and reminder failures.
12. **Privacy is visible and honest.** Local AI should be described as local; unavailable online-style features should not pretend to work.

## 5. Launch and startup: first impression and reliability

### Confirmed architecture

MainActivity installs the Android splash screen, enables edge-to-edge, and keeps the native splash visible while the database initialization state is Initializing. LifeOSApplication constructs the service locator and opens the encrypted database on a background dispatcher. MainActivity only composes the repository-backed main app after the initialization state becomes Ready. It routes initialization errors to DataKeyErrorScreen and exposes retry/exit actions.

This is a deliberate attempt to avoid rendering screens before the encrypted database is ready and to avoid blocking the UI thread during database opening.

### Risks to validate

- **Splash duration:** SQLCipher native loading, Keystore access, Room initialization, and key derivation may create a long first-open experience on slower hardware. The source comments identify several operations involved, but no measured timing was part of this audit.
- **Perceived deadlock:** if initialization is slow and the splash has no visible indication of progress, users may assume the app is frozen.
- **Error recovery:** a database/key error is high stakes because users associate LifeOS with personal memory. Retry must be safe and the error must never encourage deleting data as the first response.
- **Theme consistency:** the system splash, status-bar icon polarity, app canvas, and first Compose frame must agree in both light and dark modes.
- **First-use context:** the first successful launch should explain the app briefly without forcing a long tutorial or requesting every permission.
- **First frame:** avoid composing hidden, heavy destinations before the app is ready; the source already comments on gating the Home ViewModel and main UI, so preserve that optimization.

### Recommended startup design

**State A — Native launch:** use the platform splash with a crisp LifeOS icon and a background that matches the initial theme as closely as Android permits. Avoid adding a second animated logo screen that delays access.

**State B — Initialization:** if opening takes long enough to be noticeable, show a restrained, branded progress state only after the platform splash has transitioned. Use clear text such as “Preparing your private workspace” rather than fake progress percentages. Do not show a loading state for work that is already complete.

**State C — First successful launch:** a short, skippable introduction should communicate the core promise: capture, organize, understand; private data stored locally. Let users enter Home quickly. Ask for camera, microphone, and location permissions only when they choose the related feature.

**State D — Recovery:** database-key errors need a distinct recovery surface with plain-language explanation, retry, safe support/debug details, and exit. Do not silently wipe the database, regenerate a key over existing encrypted data, or hide the underlying cause.

### Startup test matrix

- Fresh install with empty database.
- Warm relaunch with an existing database.
- First database open after app update.
- Dark mode and light mode.
- Low-memory device and slow storage.
- App backgrounded during initialization, then resumed.
- Initialization exception followed by Retry.
- Keystore/key failure path.
- Device rotation during initialization and immediately after readiness.
- Large local database with many notes, diary entries, expenses, and captures.

Record median and 95th-percentile timings from repeat runs. Set a performance target after a real baseline is measured; do not invent a benchmark from source inspection alone.

## 6. Home: the daily command center

Home is the highest-impact redesign target because it determines whether the app feels useful within seconds. REDESIGN_PLAN.md describes a Home command center with daily progress, quick actions, AI nudge, today's tasks/habits, weekly rhythm, spending, and recent activity. UPDATE.md also records earlier work on the large empty gradient gap, progress, quick actions, priorities, habits, recent activity, local intelligence, and Capture.

### Recommended hierarchy

1. **Compact header:** greeting/date, global search, and one profile/settings entry point. Do not place three equally strong actions beside the greeting.
2. **Today's focus:** a compact summary of the one to three most important items. The first screen should answer “What needs my attention today?”
3. **Tasks:** show the nearest or most important tasks with inline completion. Provide a clear route to all tasks.
4. **Habits:** show today's habit completion, current streak where meaningful, and a direct check-in action.
5. **Capture:** make it easy to create a thought, photo, video, audio note, or mood entry. Avoid duplicating multiple large Capture cards.
6. **Spending:** show today's or current-period spending with clear date context and a route to Expenses.
7. **Recent memory:** show the latest useful note, diary entry, or capture, with time and type.
8. **Intelligence:** one evidence-based local suggestion, only when enough data exists. Do not reserve a large card for generic or fabricated advice.

The exact order should respect the master product preference: Today → Tasks → Habits → Spending → Recent Activity → Intelligence. Quick Capture should remain immediately accessible, without overwhelming the daily hierarchy.

### Layout and density

- Use one main progress/summary treatment rather than several competing hero cards.
- Avoid a huge greeting area that pushes actionable content below the fold.
- On small phones, prioritize the first useful action and let the rest scroll naturally.
- On larger phones, use more whitespace and slightly wider cards without stretching text lines.
- Use LazyColumn for vertically repeated content; preserve stable item keys where appropriate.
- Do not nest multiple independently scrolling vertical containers without a clear interaction reason.
- Ensure bottom content clears the navigation bar and bottom app navigation.
- Use a single, shared empty-state component for new users with no tasks, habits, or memories.

### Home interactions to verify

- Complete a task; progress updates from real data.
- Check in a habit; streak and completion state update.
- Add an expense; the spending summary updates.
- Capture a thought; the latest memory and timeline update.
- Open a recent memory; Back returns to the previous Home state.
- Search from Home; Back returns to Home.
- Rotate the device or background/restore the app; no duplicate navigation stack or lost input.
- Test long titles, large font scale, no data, and many records.

### Design acceptance criteria

- The top screen communicates today's state without requiring a scroll.
- At least one primary action is obvious and accessible.
- No hardcoded statistics or fake activity.
- Each card is tappable only if the whole card is intended to be interactive; nested buttons must have clear independent semantics.
- The page remains calm when every module has data.
- A user can identify the next action within a few seconds without needing to understand the app's internal module names.

## 7. Bottom navigation and navigation graph

### Current documented direction

The newer redesign plan defines Home, Notes, Capture, Insights, and Life, with Capture as a raised central action. UI_UX_FLOW_NAVIGATION.md defines four root destinations and treats Capture as an overlay. UPDATE.md documents an earlier root-tab setup. LifeOSNavHost.kt contains the navigation implementation and many feature destinations.

### Main risk

Navigation can compile and still feel inconsistent if the actual destination graph, the visible tab bar, and the documentation disagree. This is especially important for Back behavior, tab state restoration, full-window composers, and screens launched from several different locations.

### Recommendations

1. Treat Home, Notes, Insights, and Life as the root tabs; Capture should open as an action/overlay rather than creating a fifth saved tab stack.
2. Keep each root tab's state when switching tabs if that is the intended product behavior.
3. Keep detail screens stacked above their originating tab; system Back should return to the previous meaningful context.
4. Handle unsaved editor changes with a clear confirmation. Avoid losing a note or diary entry on an accidental gesture.
5. Declare literal routes before parameterized routes to avoid route collisions.
6. Make full-window screens own the appropriate insets while the bottom bar is hidden. Restore the bar reliably after dismissing a full-window screen.
7. Avoid manual screen switching that bypasses Compose Navigation.
8. Avoid duplicate NavControllers or a second hand-maintained stack.
9. Give selected tabs proper selected semantics and readable labels.
10. Keep the center Capture target large enough without obscuring the other destinations.
11. Confirm deep links, saved state, and process-death restoration only where they are supported by the product.
12. Use a consistent title/back pattern across detail screens.

### Mandatory navigation regression flows

- Home → Notes → Note Editor → save → Back.
- Home → Notes → Note Editor → edit → Back with unsaved changes.
- Home → Life → Timeline → item detail → Back.
- Home → Expenses → add expense → save → Back.
- Home → Tasks → task detail/editor → Back.
- Home → Habits → Habit Detail → Back.
- Home → Capture → take/pick media or create thought → result/detail → Back.
- Insights → LifeOS AI → Back.
- Profile → Settings → App Lock → Back.
- Switch root tabs repeatedly and verify each expected stack/state.
- Open Capture from different origins and verify dismissal returns to the correct origin.
- Test system Back, gesture Back, and three-button navigation.

These should be automated where possible and also smoke-tested on an emulator or device.

## 8. Design system: tokens, typography, spacing, shapes

### Confirmed status

The repository has Color.kt, Type.kt, Shape.kt, Spacing.kt, Motion.kt, and Theme.kt. Color.kt comments explicitly say public legacy token names are kept for source compatibility. Theme.kt defines light and dark Material 3 color schemes. Motion.kt documents a common family of timings, easing, and springs. This is a useful foundation and should be strengthened, not replaced.

### Risks

- Legacy token names can remain for compatibility while their semantics become confusing. A token called “magenta” might now represent an iris/orchid role or a component may use literal colors instead of semantic tokens.
- The old UI specification and current theme values differ.
- A screen can look inconsistent even while every color comes from the theme, if typography and spacing are manually defined.
- Large type scales may work on the developer's device but overflow at 1.3–2.0 font scale.
- The light and dark themes need independent contrast checks; the same translucent surface may not read equally well on both canvases.

### Recommended token contract

**Color roles:** canvas, surface, raised surface, primary, on-primary, primary container, secondary, success, warning, destructive, outline, muted text, focus, disabled. Components should request a semantic role rather than a one-off hex value.

**Typography:** define page title, section title, card title, body, secondary body, label, numeric emphasis, and caption. Use a small number of styles. Monetary totals and progress numbers should be legible, not stylized beyond readability.

**Spacing:** adopt a documented scale such as 4, 8, 12, 16, 20, 24, and 32dp. Existing token values should be inspected before selecting the final scale; do not mechanically replace every existing spacing value.

**Shapes:** use a few standard corner sizes for controls, cards, sheets, and pills. Very rounded shapes should be reserved for components where they improve the visual language. Avoid mixing several different corner radii on a single screen without purpose.

**Elevation:** define a few levels, not a unique shadow per component. Shadows should not be the only way to distinguish layers.

**Motion:** standardize fast feedback, standard transitions, slow/large transitions, and spring interaction. Make repeated state changes low-cost and avoid animating entire lists unnecessarily.

### One-by-one cleanup procedure

1. Inventory public token names and references.
2. Identify literal colors, arbitrary dp values, and local animation specs in UI files.
3. Classify each literal as a semantic exception, a visual asset, or a candidate for a token.
4. Introduce/adjust tokens additively where possible.
5. Migrate shared components first.
6. Migrate one screen at a time.
7. Compare screenshots before/after in light and dark modes.
8. Remove legacy tokens only when references and compatibility expectations have been checked.
9. Run tests and build after each coherent batch.

## 9. Glassmorphism, ambient background, and visual performance

### Confirmed

LiquidGlass.kt contains shared glass-related drawing and interaction utilities. MainActivity wraps the navigation content in LifeOSAmbientBackground and comments describe the soft lavender/magenta/sky ambient background. REDESIGN_PLAN.md says the newer direction softens the glass engine with neutral shadows, finer sheen, and thinner rims. The product spec says not to overuse glass and prioritizes readability, accessibility, battery, and performance.

### Recommendation

Keep the glass treatment for selected app chrome and a few meaningful hero surfaces: the bottom navigation, a central Capture action, selected top bars, and a carefully chosen hero panel. Use solid or nearly opaque surfaces for dense lists, notes, forms, charts, expense rows, and content that users read for long periods.

A blurred or translucent background behind every list row makes content less distinct and can increase rendering work. The visual system should not require a blur effect to make a card feel premium.

### Visual rules

- Never layer glass over glass repeatedly.
- Use one primary gradient at a time on a screen.
- Do not use glow as the default focus indicator.
- Keep body text contrast at WCAG AA targets: 4.5:1 for normal text and 3:1 for large text.
- Test contrast against the actual composited background, not just the source color.
- Keep disabled text and icon states understandable.
- Avoid placing long notes over moving or high-contrast ambient color clouds.
- Provide clear pressed, selected, disabled, and focused states.
- Ensure content is still understandable if the ambient background is static or disabled.
- Avoid expensive blur/alpha layers in long, scrolling lists.
- Keep animation optional where platform accessibility preferences require reduced motion.

### Performance verification

Measure cold and warm launch, scrolling, opening Capture, switching tabs, and expanding bottom sheets. Use Android Studio Layout Inspector and system tracing/Perfetto on representative hardware. Look for repeated recomposition, unstable list keys, expensive custom drawing, excessive offscreen layers, and loading media on the main thread. No performance defect is claimed without measurements; this is a high-value verification area because the visual engine is global.

## 10. Capture: reduce time-to-memory

Capture is a signature feature. It should make the app feel useful before the user has organized anything. The current CaptureHubScreen contains UI for photo, video, audio, thought, mood, and optional location, and it uses permission request launchers. The manifest comments say permissions should be requested only when their features are selected.

### Recommended experience

- Make the six capture types visually distinct but not six oversized marketing cards.
- Put the most frequent actions first; use the user's actual product priorities rather than assuming all types are equally common.
- Let the thought field accept a short entry immediately.
- Camera and audio actions should launch directly into the chosen mode after permission is granted.
- Explain why a permission is needed at the point of use.
- If permission is denied, keep thought capture and other unrelated functions usable.
- After save, confirm completion briefly and offer “View memory” or “Capture another.”
- Persist type, timestamp, URI/path, duration where relevant, and metadata consistently.
- Ensure each capture is visible in the timeline with readable type/date/time.
- Avoid making a user choose a folder, tags, or AI categorization before the moment can be saved.
- Keep optional enrichment (tags, location, categorization) after the core capture.

### Permission states to test

- Granted already.
- Denied once.
- Permanently denied / “don't ask again.”
- Camera hardware unavailable.
- Microphone unavailable.
- Photo Picker cancelled.
- Media URI access expires or source content is removed.
- Capture save fails due to storage or encoding error.
- User leaves the app while recording or capturing.
- Screen rotation and lifecycle changes during video/audio capture.

### Video and media quality requirements

Portrait and landscape video must preserve aspect ratio and rotation metadata. Fullscreen playback should not stretch video. Audio playback/recording should show clear elapsed time and recording state. Large media files should not be read fully into memory. All controls must clear gesture/navigation insets and remain usable with large fonts.

## 11. Notes and editing

Notes should be fast to scan and safe to edit. The plan documents All/Recent/Pinned/Favorites/Archived filters, folders, tags, card metadata, and actions such as pin, favorite, archive, move, and delete. It also describes a richer editor with formatting and attachments. These are useful capabilities, but the interface must avoid exposing all editing options at once.

### Notes list recommendations

- Put title and search at the top.
- Keep filter chips scrollable and legible.
- Make the selected filter obvious.
- Choose grid versus list based on device width and note content; text-heavy notes generally benefit from a list layout.
- Use a clear title, a short excerpt, and updated time; avoid too many tags on every card.
- Put less-frequent actions in an overflow menu or contextual selection mode.
- Distinguish archive from delete; ask for confirmation only where risk warrants it.
- Include a clear empty state for each filter and folder.
- Search should find titles and body content using the local index/query path; do not imply cloud search.

### Editor recommendations

- Focus the title/body field appropriately when creating a note.
- Keep formatting actions available without permanently consuming too much vertical space.
- Make autosave behavior explicit and robust. If saving is delayed, show a subtle saved/saving state.
- Confirm unsaved changes if leaving would discard them.
- Ensure multiline text works with keyboard, predictive text, selection, and paste.
- Avoid overlapping the editor toolbar with the keyboard or bottom system bar.
- Keep attachment actions contextual and permissions limited to the selected action.
- Treat AI transformations as proposals. The user must review content before it overwrites or creates data.
- Test large notes, emoji, multilingual content, long titles, empty body, and rapid navigation.

### Important preservation rule

Do not rebuild the notes database or change entities merely to achieve a different visual layout. The presentation can evolve while existing records, IDs, folders, timestamps, and relationships remain intact.

## 12. Tasks and habits

Tasks and Habits are core daily utility screens. They should emphasize completion, timing, and useful status rather than elaborate decoration.

### Tasks screen

- Prioritize overdue, due today, and upcoming tasks in a predictable order.
- Keep completion fast and reversible where appropriate.
- Show date/time context without making every row visually noisy.
- Make priority and reminder status understandable with text/icons, not color alone.
- Distinguish no tasks from no tasks due today.
- Keep add/edit flows compact and keyboard-safe.
- Explain reminder permission or exact-alarm access only when it affects a selected reminder.
- Ensure recurring tasks do not create duplicate reminders after edits or device reboot.
- Test due-date changes, completion, undo, repeat rules, time-zone boundaries, and reminder rescheduling.

### Habits screen

The LifeOS project direction explicitly calls for direct visibility of streak, best streak, today/week/month completion, percentage, dots/heatmap, and progress. Make those metrics easy to see without forcing a detail-screen visit.

Recommended order:
1. Today's completion summary.
2. Habit list with one-tap check-in.
3. Current streak and best streak.
4. Weekly completion dots.
5. Monthly heatmap or completion history.
6. Detailed trends on the habit detail screen.

Use a compact visual summary rather than several separate cards repeating the same completion percentage. State what the denominator means: for example, “4 of 6 planned check-ins” is clearer than an unexplained “67%.” Do not punish a user visually for missing a day. Streaks should be informative rather than shaming.

### Habit edge cases

- Habit scheduled on selected weekdays only.
- Habit created mid-week.
- Time-zone change and daylight-saving transitions where applicable.
- Completion undone.
- Habit paused or archived.
- No historical data.
- Long habit name.
- Large font scale.
- Month/week boundaries.
- Best streak and current streak calculated consistently with the actual schedule.

## 13. Expenses: trustworthy numbers and fast entry

Expenses are high-trust UI: users need confidence in amounts, dates, categories, and totals. ExpensesScreen.kt contains a form/sheet approach and expense-related view-model/repository dependencies. The recent project conversation specifically identifies a desire to keep the existing expense flow and improve the add-expense sheet so the form is visible without needing an awkward extra drag.

### Recommendations

- Preserve the current add-expense concept if it works; do not redesign it from scratch merely for novelty.
- On opening, the sheet should reveal its title, amount input, and primary save action in a sensible initial state.
- Make the sheet's initial and expanded states predictable. Do not require users to discover that they must drag it upward just to see basic fields.
- Keep the amount field prominent and numeric-keyboard friendly.
- Use a clear date default, category picker, and optional note.
- Keep save disabled until required values are valid; show a specific error rather than silently rejecting input.
- Prevent double submission.
- Display currency consistently and never assume a currency from a locale without verifying the app's existing setting/data contract.
- Separate budget progress from actual spending; label both.
- Explain whether totals refer to today, week, month, or a selected date range.
- Make empty states useful, and do not show zero as if it were a real trend when the dataset is empty.
- Confirm destructive deletes and provide undo where feasible.

### Expense regression tests

- Add a valid amount.
- Empty amount, invalid decimal, negative amount, and very large amount.
- Category change.
- Date change.
- Cancel and dismiss sheet.
- Save and immediate double tap.
- Rotate with a partially completed form.
- Add while keyboard is visible.
- Existing expense edit and delete.
- Totals after insert, edit, and delete.
- Currency and locale display.
- Empty month and historical month.
- App process recreation without accidental duplicate insert.

## 14. Diary and Timeline: preserve identity, unify behavior

The redesign plan explicitly says Diary and Timeline keep a deliberate paper/handwriting aesthetic and should receive refinement rather than being forced into Aurora Noir. That is a reasonable product choice. Consistency does not mean every screen must look identical.

The right boundary is: **distinct expression, shared rules**. Diary can use paper textures and reflective writing space while sharing accessible typography, touch targets, safe insets, back behavior, destructive-action patterns, and data semantics.

### Diary recommendations

- Preserve the paper aesthetic but reduce decorative layers behind long-form text.
- Keep mood selection clear, optional, and easy to change.
- Show date/time and attachments consistently.
- Voice note recording must make active recording and elapsed time obvious.
- Location should remain optional and permission-gated.
- Weather must not show fabricated data. The manifest explicitly notes the build has no permitted weather source and reports unavailable state.
- Empty states should invite writing without pressuring the user.
- Confirm deletion and ensure attachment cleanup does not orphan files or break historical entries.
- Test long journal entries, voice attachments, images, mood labels, and editor layout at large font scales.

### Timeline recommendations

Timeline is the chronological memory system. It should combine notes, diary entries, tasks/events, expenses, photos, videos, audio, and captures without losing type context.

Each item should show:
- A recognizable type marker.
- A readable title/content excerpt.
- A local date/time formatted consistently.
- Useful context such as amount, mood, duration, or attachment thumbnail where relevant.
- A clear route to the full record.

Use stable chronological ordering, consistent time-zone handling, and pagination/lazy loading for large histories. Empty timeline, same-time items, deleted media, unavailable attachment files, and daylight-saving boundaries require tests. Search and filters should refine the same underlying memory model rather than create a second contradictory timeline.

## 15. Insights and offline LifeOS AI

### Product promise

The product requirements state that core features and personal data must remain on-device. The repository includes a core intelligence package with analyzers for diary, mood, habits, tasks, productivity, trends, patterns, reports, and local questions. There are also domain intelligence classes. This is a promising foundation for a lightweight, deterministic local intelligence engine.

### UI recommendations

- Lead with a short explanation of what the app can infer from available local data.
- Show the period and data coverage behind each insight.
- Distinguish observations from recommendations.
- Never claim correlation or causation without a defensible method.
- Show “not enough data yet” instead of manufacturing a score.
- Give each insight a path to its underlying records.
- Make recommendations dismissible and avoid repetitive generic messages.
- Let users decide which data domains an AI question can use.
- Ask before creating a task, rewriting a note, or saving a diary entry.
- Keep generated content reviewable and never silently mutate important data.
- Explain that local rule-based analysis is not a general-purpose cloud chatbot.
- Make AI response sources inspectable: e.g. “Based on 12 diary entries from September,” if that count is actually available.

### Reliability rules

- Every statistic should be reproducible from local records.
- Every date range must be explicit.
- Empty and sparse data should have defined behavior.
- Mood inference must be described as a heuristic, not a diagnosis.
- Recommendations must be phrased as suggestions, not medical or financial certainty.
- If the engine cannot answer a question, it should say so and suggest a supported query.
- Test negation, mixed mood words, spelling variants, multilingual text if supported, duplicate records, and date-range boundaries.
- Avoid building a large language model into the app simply to market the feature as AI.

### Architecture protection

Do not bypass repositories or Room to make the AI screen simpler. UI state should flow through the existing data/repository/use-case path. Keep analysis deterministic and testable, and preserve the ability to improve it later without requiring an external service.

## 16. Profile, Settings, App Lock, permissions, and privacy

### Settings

Settings should be organized by user intent rather than implementation detail. Suggested groups:
- Appearance and accessibility.
- Privacy and App Lock.
- Notifications and reminders.
- Data export/backup and restore.
- Storage and media.
- About/version and diagnostic details.

Use clear descriptions for settings with consequences. Avoid duplicate switches that control the same underlying preference. Make the current setting visible and confirm only high-impact destructive actions.

### App Lock

The repository includes biometric/PIN-related code and a PIN hasher test. Preserve Android BiometricPrompt integration. Never store biometric material. PIN/recovery secrets must not be plaintext. A forgotten lock should require a secure recovery path; never add a hardcoded bypass or silent reset. Test biometric unavailable, cancelled, locked out, PIN retry limits, process recreation, and recovery failure.

### Permission handling

The manifest declares notification, audio, camera, coarse/fine location, alarm-related and foreground-service permissions, with comments describing contextual requests. Keep that principle. Request permissions when the user chooses the relevant capability. A denied camera permission should not block note writing or task management. Explain permanent denial and provide a route to Android Settings without repeatedly launching permission prompts.

### Privacy baseline

The manifest explicitly says there is no INTERNET permission and no external API, network code, or analytics in this build. Preserve this property. The build configuration and manifest should be re-audited whenever dependencies are added. Do not introduce telemetry, Firebase, remote databases, cloud AI, or automatic uploads of user data.

### Backup/export UX

A local-first product needs trustworthy export and restore. Clearly state what is included, where the file will be saved, and whether attachments are included. Restore should validate input before modifying data, preserve the current database until the restore succeeds, and handle duplicate IDs and schema versions intentionally. Never implement backup by deleting the live database first.

## 17. Responsive layouts, insets, accessibility, and polish

The project already uses edge-to-edge and has a dedicated LifeOSSystemBars component. Capture screens also use statusBarsPadding/navigationBarsPadding in source. These are good foundations, but edge-to-edge needs device-level verification.

### Device matrix

At minimum test:
- Small-width phone.
- Typical modern phone.
- Large phone.
- Landscape for relevant screens, especially media.
- Font scale 1.0, 1.3, and 2.0.
- Gesture navigation.
- Three-button navigation.
- Light and dark themes.
- Keyboard visible and hidden.
- Empty and dense data.
- Long translations/strings and long user content.

### Accessibility checklist

- Meaningful content descriptions for icon-only actions.
- Decorative imagery excluded from accessibility traversal.
- Correct roles and selected semantics for tabs and chips.
- Logical screen-reader order.
- Touch targets meeting the project's minimum target.
- Contrast measured on actual composed surfaces.
- Text can scale without clipping or overlapping numeric charts.
- Error text is associated with the relevant input.
- Progress indicators have meaningful labels.
- Color is not the only signal for mood, category, completion, or warning.
- Motion is subtle and does not convey essential information only through animation.
- Focus order works when navigating with keyboard/switch access where applicable.

### Polish checklist

- No floating button over the last list row.
- No title hidden behind status bar.
- No bottom sheet save button hidden behind the keyboard.
- No clipped rounded corners caused by nested backgrounds.
- No horizontal overflow on small screens.
- No awkward label wrapping in bottom navigation.
- No layout jumps when loading data or showing an empty state.
- No flicker between splash and app theme.
- No repeated shimmer/skeleton after data is already available.
- No inaccessible contrast in disabled controls.
- No accidental double-tap duplicate actions.
- No destructive action triggered by an ambiguous icon alone.

## 18. Dead code and unused component audit plan

### Confirmed current status

REDESIGN_PLAN.md states that Phase 6 is partial and the broader dead-code sweep remains pending. It specifically records removal of a speculative LifeOSRowGap and newly unused imports. The codebase contains multiple component files such as LifeOSKit.kt, LifeOSKitExtras.kt, LifeOSDesignComponents.kt, GlassCard.kt, LiquidGlass.kt, LifeOSBottomBar.kt, and several feature-specific component files. This is not proof that any particular component is dead, but it is a good reason to perform a systematic inventory.

### What qualifies as dead code

A symbol should be treated as a removal candidate only if:
1. It has no live call sites in main source.
2. It is not referenced by navigation, dependency injection, reflection, serialization, Android XML, manifest declarations, generated wiring, or resource-name lookup.
3. It is not required by tests or tooling.
4. It is not part of a deliberate public compatibility surface.
5. Removing it does not change runtime behavior.
6. The build and relevant tests pass after removal.

Unused imports are lower-risk, but even these should be cleaned with normal tooling. Unused dependencies are separate from unused Kotlin declarations: confirm actual use before removing a library. A class referenced by a test but not app runtime may still be useful test infrastructure.

### Suggested inventory

Create a spreadsheet or Markdown table with:
- File/symbol.
- Type (composable, ViewModel, model, helper, token, dependency, resource).
- Call-site count.
- Entry path / reason it is reachable.
- Tests.
- Confidence (high/medium/low).
- Proposed action.
- Verification command.
- Removal commit.

### Safe cleanup sequence

1. Remove unused imports and trivial local variables with formatter/IDE tooling.
2. Inspect explicitly documented legacy overloads and deprecated APIs.
3. Search all call sites for each candidate.
4. Check routes, Android manifest, XML resources, DI and reflection.
5. Remove one small group at a time.
6. Run compilation and unit tests after each group.
7. Review the diff for accidental feature removal.
8. Keep the original commit history; never use destructive resets.
9. Update REDESIGN_PLAN.md and UPDATE.md with completed symbols and verification.
10. Do not delete whole feature folders just because their entry point is not on the bottom bar.

### High-risk false positives

- Composables invoked through a route that appears rarely in normal use.
- Manifest components and alarm/boot receivers.
- Serialization models and Room entities/converters.
- Backup and migration code used only during restore/upgrade.
- Notification permission hosts.
- Preview-only composables and test helpers.
- Public token names deliberately retained for source compatibility.
- Widget providers called by Android rather than by app code.
- Legacy-looking code that is still used by Diary's intentionally separate visual system.

## 19. Testing and CI improvements

### Confirmed CI

The latest visible Android CI run #283 reports success. The workflow is triggered on push to main, pull requests to main, and manual dispatch. The workflow says it runs tests and assembles both debug and release variants and uploads APK artifacts. The latest job steps show the test/build step succeeded.

### Gaps not established by CI metadata

A successful build does not prove:
- Navigation flows are correct on a device.
- Bottom sheets fit above system bars.
- Media capture works on every supported device.
- TalkBack order is correct.
- Startup latency is acceptable.
- Every migration path preserves user data.
- Dark mode contrast is correct.
- No dead code remains.

### Recommended verification pipeline

**Per change**
- Kotlin compilation.
- Unit tests for the changed behavior.
- Relevant Room schema/migration tests if data contracts change.
- Static checks/lint when supported by the project.
- Diff review and documentation update.

**Before redesign milestones**
- Debug APK assembly.
- Release APK assembly and minification validation.
- Full unit test suite.
- Instrumentation tests on an emulator.
- Navigation smoke tests.
- Screenshot comparison for critical screens.
- Permission and system-bar checks.
- Performance smoke test for startup and scrolling.

**Before a release**
- Test fresh install and upgrade from the prior supported version.
- Test export and restore with realistic data.
- Test Room migrations using historical schemas.
- Test App Lock and recovery.
- Test reminder scheduling through reboot and permission denial.
- Test capture lifecycle and media playback.
- Inspect APK permissions and dependency tree.
- Verify no unexpected internet permission, network client, API key, telemetry, or upload path was introduced.
- Review release signing separately from CI's fallback debug signing configuration.

### Suggested UI test scenarios

Use semantics rather than brittle pixel coordinates. Tests should verify labels, enabled state, saved data, and destination behavior. Pixel/screenshot tests are useful for stable layout regressions, but they should complement behavior tests, not replace them.

## 20. File-by-file review targets

This is a prioritized review map, not a claim that every listed file contains a defect.

| Area | Files | Review focus |
|---|---|---|
| Startup | MainActivity.kt, LifeOSApplication.kt | Startup timing, theme transition, error recovery, readiness gating |
| Design tokens | Color.kt, Type.kt, Shape.kt, Spacing.kt, Motion.kt, Theme.kt | Canonical token roles, literals, accessibility, legacy aliases |
| Shared visuals | LiquidGlass.kt, GlassCard.kt, LifeOSKit.kt, LifeOSKitExtras.kt, LifeOSDesignComponents.kt | Duplicated components, overuse of glass, API consistency, reachability |
| Navigation | Screen.kt, LifeOSNavHost.kt, BottomBarVisibility.kt, LifeOSBottomBar.kt | Root tabs, state restoration, Back, overlay/inset behavior |
| Home | HomeScreen.kt, HomeViewModel.kt, GetHomeSummaryUseCase.kt | Hierarchy, live metrics, recomposition, empty state |
| Capture | CaptureHubScreen.kt, CaptureViewModel.kt, CameraCaptureScreen.kt, VideoCaptureScreen.kt, AudioCaptureScreen.kt, CaptureDetailScreen.kt | Permission paths, time-to-capture, lifecycle, media metadata |
| Notes | NotesScreen.kt, NotesListScreen.kt, NoteEditorScreen.kt, NotesViewModel.kt | Search/filter, edit safety, empty state, autosave, data preservation |
| Tasks | TasksScreen.kt, reminder scheduler and use cases | Due order, recurring rules, reminder states, inline completion |
| Habits | HabitsScreen.kt, HabitDetailScreen.kt, HabitStatsCalculator.kt | Streak math, today/week/month summary, empty/sparse history |
| Expenses | ExpensesScreen.kt, ExpenseRepository.kt, ExpenseDao.kt | Add sheet visibility, amount validation, totals, currency, data integrity |
| Diary | DiaryScreen.kt, DiaryEditor.kt, DiaryPaper.kt, related ViewModels | Paper identity, editor insets, mood/media/location, recovery |
| Timeline | TimelineScreen.kt, BuildTimelineUseCase.kt, TimelineItem.kt | Ordering, readable content, time zones, media availability |
| Intelligence | core/intelligence and domain/intelligence | Duplicate responsibilities, deterministic outputs, truthful confidence |
| Security | AppLockManager.kt, DatabasePassphraseProvider.kt, PinHasher.kt | Secure storage, lockout, recovery, failure handling |
| Persistence | AppDatabase.kt, DAOs, entities, migrations/schema files | Migration safety, query performance, historical data |
| Documentation | README.md, UPDATE.md, PRD.md, TRD.md, UI_UX_FLOW_NAVIGATION.md, REDESIGN_PLAN.md | Current-state accuracy, history preservation, one canonical design contract |

## 21. Step-by-step implementation roadmap

Do not execute every phase in one giant PR. Each phase should have an explicit acceptance checklist and a green build.

### Phase 0 — Freeze a trustworthy baseline

**Goal:** know what currently works before changing appearance.

1. Inspect Git status and recent commits locally.
2. Record current default-branch SHA.
3. Run the current test/build workflow.
4. Capture screenshots of launch, Home, Notes, Capture, Insights, Life/Timeline, Tasks, Habits, Expenses, Diary, Profile, Settings, App Lock, and onboarding.
5. Capture both themes and the main device/font-scale matrix.
6. List current routes and permissions.
7. Record baseline startup timings and scrolling behavior.
8. Preserve an app data backup and the current installable artifact.

**Acceptance:** the baseline is reproducible, current failures are separated from regressions, and screenshots exist for comparison.

### Phase 1 — Resolve design and navigation contract

1. Confirm the active design direction is Aurora Noir.
2. Reconcile navigation documentation with the actual root-tab graph.
3. Define screen responsibilities and entry points.
4. Define the shared token contract.
5. Document exceptions such as Diary's paper theme.
6. Record accessibility, privacy, offline, and data-integrity requirements.
7. Update docs without truncating historical UPDATE.md content.

**Acceptance:** a developer can implement any screen without guessing the primary palette, navigation, spacing, or interaction model.

### Phase 2 — Fix launch and first-use journey

1. Measure cold/warm startup.
2. Test initialization and key-error recovery.
3. Match splash and app themes.
4. Keep onboarding short, skippable, and permission-light.
5. Ensure first-run Home explains what the app can do and has useful empty states.
6. Ensure users can reach a useful action immediately.

**Acceptance:** no unexplained blank screen, no indefinite splash, no destructive recovery, and no unnecessary permission request at launch.

### Phase 3 — Stabilize the shared design system

1. Review token aliases and semantic roles.
2. Consolidate duplicate card/button/chip/section-header patterns.
3. Establish typography and spacing rules.
4. Standardize touch targets and accessibility semantics.
5. Validate light/dark contrast.
6. Keep glass limited to intentional surfaces.
7. Validate motion and performance.

**Acceptance:** a shared component behaves consistently in multiple screens, and screen-level code does not need to reimplement basic design behavior.

### Phase 4 — Redesign Home

1. Use the Today → Tasks → Habits → Spending → Recent Activity → Intelligence priority.
2. Keep quick capture accessible.
3. Remove competing hero cards and excessive decoration.
4. Make each summary data-backed.
5. Add concise empty states.
6. Verify scrolling, long content, and font scale.
7. Test cross-feature updates.

**Acceptance:** Home is useful with both empty and populated data, with no misleading metrics and no awkward initial blank space.

### Phase 5 — Improve Capture and media

1. Optimize the initial Capture hub.
2. Make Thought capture immediate.
3. Validate permission request and denial flows.
4. Fix sheet/inset/keyboard behavior.
5. Validate camera/video/audio lifecycle.
6. Verify saved metadata and timeline integration.
7. Add media error recovery.

**Acceptance:** a thought can be saved quickly; other capture modes behave predictably; no capture path loses metadata or silently fails.

### Phase 6 — Refine feature screens one by one

Recommended order:
1. Notes and editor.
2. Tasks.
3. Habits.
4. Expenses.
5. Timeline.
6. Diary.
7. Insights and LifeOS AI.
8. Search.
9. Profile and Settings.
10. App Lock and onboarding polish.

For each screen: write the primary user task, simplify hierarchy, migrate to shared components, test empty/populated/error states, check accessibility and insets, and verify navigation.

**Acceptance:** each screen passes a dedicated checklist before the next screen begins.

### Phase 7 — Dead-code and dependency cleanup

1. Build the reachability inventory.
2. Remove imports and proven unused symbols in small batches.
3. Inspect dependencies and resources.
4. Preserve compatibility aliases until all references are checked.
5. Keep database/migration/backup/security code unless its role is explicitly understood.
6. Run compilation, tests, lint, and release assembly.
7. Review diff and update docs.

**Acceptance:** every deletion has a reason and verification; no feature is removed as collateral cleanup.

### Phase 8 — Release hardening

1. Run full unit and instrumentation suites.
2. Test database migration from prior supported schema versions.
3. Verify export/restore.
4. Verify reminders and reboot behavior.
5. Verify App Lock and recovery.
6. Check permissions and network/dependency surface.
7. Test small/large phones and accessibility settings.
8. Produce a release checklist with verified evidence.

**Acceptance:** release readiness is based on tested behavior, not visual confidence or a single green build.

## 22. Suggested issue backlog, in execution order

### P0 — Must resolve before broad visual implementation

- [ ] Establish the canonical active design and navigation specification.
- [ ] Capture baseline screenshots for all key routes.
- [ ] Measure cold and warm startup and validate DataKeyErrorScreen recovery.
- [ ] Confirm the actual bottom-navigation destinations and Back behavior.
- [ ] Establish light/dark contrast and minimum touch-target checks.
- [ ] Verify no regression in local storage, offline operation, or database migrations.

### P1 — Highest product impact

- [ ] Rework Home hierarchy to prioritize today's actions.
- [ ] Make Capture fast and permission-safe.
- [ ] Improve Expenses add-sheet initial visibility and keyboard behavior.
- [ ] Ensure Habits shows streak, best streak, completion summaries, and heatmap/progress directly.
- [ ] Standardize reusable cards, section headers, empty states, buttons, and chips.
- [ ] Test Notes editor save/discard behavior.
- [ ] Verify Timeline type/date/time/content consistency.
- [ ] Validate all primary navigation flows.

### P2 — Cleanup and refinement

- [ ] Inventory duplicate or overlapping components.
- [ ] Remove proven unused imports, functions, tokens, and composables.
- [ ] Review dependencies for unused artifacts.
- [ ] Add screenshot and accessibility regression tests where feasible.
- [ ] Tune animation and blur after performance measurement.
- [ ] Refine onboarding copy and empty states.
- [ ] Review Settings information architecture and backup/export clarity.

### P3 — Optional future work, only after core stability

- [ ] More sophisticated local insight explanations.
- [ ] Widgets polish and accessibility.
- [ ] Additional adaptive layouts.
- [ ] Richer note formatting if current data representation supports it safely.
- [ ] More nuanced animation for meaningful transitions.
- [ ] Further visual theming options only if they do not fragment the design system.

## 23. Definition of done for a redesigned screen

A screen is not complete just because its screenshot looks good. It is complete when:

- The primary user task is obvious.
- Existing features and data paths are preserved.
- Empty, loading, success, error, and populated states are designed.
- Keyboard, system bars, gestures, and navigation are correct.
- Text scales without clipping.
- Color contrast and semantics are checked.
- Animation explains state and does not block interaction.
- Local data updates are reflected consistently.
- The screen works offline.
- No unnecessary permissions or network dependencies are introduced.
- Unit/UI tests relevant to the change pass.
- The screen has been visually compared in light and dark mode.
- The implementation uses shared components and tokens where appropriate.
- Documentation and UPDATE.md record the actual change and verification.
- The diff does not contain unrelated refactors or destructive data changes.

## 24. Product decisions that should not be made casually

1. **Do not change the root navigation again without user-journey evidence.** Navigation changes are expensive because every route and Back flow is affected.
2. **Do not replace the entire Diary aesthetic to force uniformity.** The current plan intentionally preserves its paper/handwriting identity.
3. **Do not add cloud AI.** The privacy and offline-first promise is core product identity.
4. **Do not add a large model just to claim AI.** The local intelligence engine can be useful when it is honest, testable, and evidence-backed.
5. **Do not recreate the Room database.** Data migrations must preserve user records.
6. **Do not remove old-looking components before proving they are unused.** Android manifest, routes, tests, and dependency injection can hide reachability.
7. **Do not add more glass, gradients, or animation to compensate for poor hierarchy.** Fix layout and action priority first.
8. **Do not add onboarding permission requests.** Ask at feature use.
9. **Do not show fake statistics or generic insights as personalized.** Empty data is a legitimate state.
10. **Do not call the app production-ready from a green build alone.** Device behavior, migration, accessibility, privacy, and release configuration matter.

## 25. Final assessment

### What is already promising

- A clear product premise: one private personal memory system.
- Native Kotlin/Compose architecture.
- Shared design token and component files.
- Encrypted local database integration.
- Explicit migration/schema test assets.
- Offline intelligence modules and related unit tests.
- A broad set of feature screens already exists.
- Startup initialization is intentionally asynchronous and error-aware.
- The manifest documents contextual permission use and absence of internet permission.
- CI currently has a successful visible run.
- Existing redesign and release documentation provides a basis for controlled execution.

### What should happen next

The next action should be a **baseline UX and navigation validation pass**, not a sweeping refactor. Resolve the specification drift, test the actual launch path, capture screen states, and verify the bottom-navigation/back-stack model. Then improve Home, Capture, and Expenses first because those are high-frequency entry points and visible usability issues. After shared components stabilize, refine each feature screen and perform the dead-code audit with evidence.

### Readiness score

**Preliminary repository-process readiness: 62/100.**

This is a provisional planning score, not a measured app-quality score. It reflects a successful visible CI run, substantial existing architecture and tests, and documented privacy/migration intent, balanced against specification drift, an explicitly incomplete dead-code sweep, and the lack of runtime/device UX evidence in this audit. It does not claim that the app is 62% complete or that a production release is approved.

### Remaining verification blockers

- Physical-device screenshots and interaction review.
- Cold/warm startup timing measurements.
- End-to-end navigation and system Back tests.
- Accessibility/contrast/font-scale results.
- Complete static analysis of unused symbols and dependencies.
- Full current test/lint/release output independently rerun.
- Migration, export/restore, media, reminder, and App Lock regression evidence.

**Bottom line:** LifeOS does not need an uncontrolled rewrite. It needs a single design contract, a measured startup journey, a carefully prioritized screen-by-screen redesign, and a safe code cleanup process. The existing work is valuable; the goal is to make it coherent, reliable, and genuinely production-ready without losing features or personal data.
