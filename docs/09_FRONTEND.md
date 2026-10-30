# 09 — Frontend

There is no separate "frontend" project — the Android app itself is the
entire user-facing layer. This document covers the Compose UI structure.

## Application structure

```
app/src/main/java/com/lifeos/app/
├── MainActivity.kt              ← app entry point, sets up theme + gates
├── LifeOSApplication.kt         ← Application subclass, builds ServiceLocator
└── ui/
    ├── theme/                   ← design system (colors, type, shapes)
    ├── components/              ← shared reusable composables
    ├── navigation/               ← Screen routes + NavHost
    ├── home/                    ← Home dashboard
    ├── notes/                   ← Notes list + editor
    ├── tasks/                   ← Tasks list
    ├── habits/                  ← Habits dashboard + detail + editor/wizard + shared visuals
    ├── expenses/                ← Expenses list + add
    ├── diary/                   ← Diary list + AI draft flow
    ├── timeline/                ← Unified daily timeline
    ├── capture/                 ← Photo/Video/Audio/Thought capture
    ├── insights/                ← Weekly AI review
    ├── search/                  ← Global search
    ├── ai/                      ← AI Assistant chat
    ├── settings/                ← Settings screen
    └── onboarding/              ← First-launch intro
```

## Pages / Routes

Defined in `ui/navigation/Screen.kt` as a sealed class, wired into a single
`NavHost` in `ui/navigation/LifeOSNavHost.kt`:

| Route | Screen | Notes |
|---|---|---|
| `home` | `HomeScreen` | Start destination |
| `notes` | `NotesListScreen` | |
| `notes/editor?noteId={noteId}` | `NoteEditorScreen` | `noteId` optional — absent means "new note" |
| `tasks` | `TasksScreen` | |
| `habits` | `HabitsScreen` | Permanent root tab: date strip, filters, score, cards, archive/restore |
| `habits/edit?habitId={habitId}&wizard={wizard}` | `HabitEditorScreen` | Guided 10-step first-habit wizard, or the full normal form for add/edit |
| `habits/{habitId}` | `HabitDetailScreen` | Stats, clickable 12-week heatmap, day actions, reminder, share, archive/delete |
| `expenses` | `ExpensesScreen` | |
| `diary` | `DiaryScreen` | |
| `timeline` | `TimelineScreen` | |
| `insights` | `InsightsScreen` | |
| `search` | `SearchScreen` | |
| `ai_assistant` | `AiAssistantScreen` | |
| `settings` | `SettingsScreen` | |

Bottom navigation bar (`ui/components/LifeOSBottomBar.kt`) renders the
permanent information architecture **Home · Notes · ＋ Capture · Habits ·
Life**. The four tabs live in the `root_tabs` graph with
`savedState`/`restoreState`; Capture is a raised centre *action* that opens
the full-window Capture hub and is never a selected tab. Profile left the bar
but stays reachable from the Home avatar and the Life hub, with Settings, App
Lock and backup underneath it. There is no user-facing Insights destination.
Tasks, Expenses, Diary, Timeline, Search and Snap stack above the tabs as
track modules.

## State management

- **Pattern**: One `ViewModel` per screen, exposing `StateFlow`s.
- **No external state library** (no Redux/MVI framework) — this is plain
  Android Architecture Components (`androidx.lifecycle.ViewModel` +
  Kotlin `StateFlow`/`Flow`).
- **ViewModel construction**: via `core/di/LambdaViewModelFactory` (in
  `core/di/LocalServiceLocator.kt`) — a tiny generic factory that lets each
  ViewModel take constructor parameters (repositories) without Hilt.
  Example from `ui/tasks/TasksScreen.kt`:
  ```kotlin
  val viewModel: TasksViewModel = viewModel(
      factory = LambdaViewModelFactory { TasksViewModel(locator.taskRepository) }
  )
  ```

## Dependency access pattern

`core/di/LocalServiceLocator.kt` defines a `CompositionLocal`:
```kotlin
val LocalServiceLocator = staticCompositionLocalOf<ServiceLocator> { error(...) }
```
Provided once in `MainActivity.kt`:
```kotlin
CompositionLocalProvider(LocalServiceLocator provides serviceLocator) { ... }
```
Every screen then does `val locator = LocalServiceLocator.current` to reach
repositories, the AI layer, and settings.

## Forms

Forms are per-screen Compose layouts — mostly `OutlinedTextField`s inside
sections, sheets or dialogs (Tasks, Expenses, Notes, Diary). The permanent
Habits flow is the largest: `HabitEditorScreen.kt` hosts both a 10-step
guided wizard (name → icon → colour → Build/Quit → frequency → tracking →
reminder → category/Timeline → review → create) and the full normal form used
for later edits, with per-step validation, an unsaved-changes guard and an
explicit error state at save time. There is still no shared form framework.

## UI system / Design system

`ui/theme/`:
- `Color.kt` — the full LifeOS palette (glassmorphism surfaces, brand
  indigo/violet primary, category accent colors for expenses)
- `Type.kt` — Material 3 `Typography` scale
- `Shape.kt` — large rounded corners (8dp–32dp scale)
- `Theme.kt` — `LifeOSTheme()` composable wiring light/dark `ColorScheme`s,
  plus a custom `LocalGlassColors` CompositionLocal for the glassmorphism effect

`ui/components/`:
- `GlassCard.kt` — the signature translucent card component, used across
  Home, Timeline, Habits, Diary, etc.
- `LifeOSBottomBar.kt` — the 5-item bottom navigation bar
- `ReminderTimePickerDialog.kt` — shared Material 3 `TimePicker` dialog used
  by both the Add Task and Add Habit flows

## Error handling / Loading states

There is no centralized error-handling framework. Each ViewModel handles its
own errors locally and exposes them as a `StateFlow<String?>` that the
screen displays (commonly in an `AlertDialog` or inline `Text`). Examples:
`NoteEditorViewModel.aiResult`, `DiaryViewModel.aiError`,
`SettingsViewModel.exportStatus`.

Loading states are similarly per-screen `StateFlow<Boolean>` (e.g. `aiBusy`
in `NoteEditorViewModel`, `NotesViewModel.kt`), rendered as a
`CircularProgressIndicator` while true.

## API communication

No screen talks to an external service. `core/ai/AiRepository` is a
compatibility facade over the local, deterministic Intelligence Engine;
`ui/ai/AiAssistantScreen.kt`, Notes and Diary use it in-process. Every other
screen reads and writes through the repository/domain boundaries. The app has
no internet permission.

## Reusable components

- `GlassCard` / `GlassChip` (`ui/components/GlassCard.kt`)
- `LifeOSBottomBar` (`ui/components/LifeOSBottomBar.kt`) — Home, Notes,
  ＋ Capture, Habits, Life with animated selected pills and a raised centre action
- `ui/habits/HabitVisuals.kt` — the Habits-only iris accent, score ring,
  mini heatmap, celebration/confetti overlay, share helpers and reduced-motion
  detection
- `ReminderTimePickerDialog` (`ui/components/ReminderTimePickerDialog.kt`)

There is currently no dedicated shared component for buttons, text fields,
or list rows — each screen builds its own `AlertDialog`/`OutlinedTextField`
combinations inline. This is a documented opportunity for future
consolidation (see `docs/18_ROADMAP.md`).

## Shared presentation components

- `LifeOSTopBar` (`ui/components/LifeOSTopBar.kt`) — consistent primary/secondary screen header
- `LifeOSBottomBar` — persistent Home/Notes/Capture/Habits/Life navigation
- `LifeOSCard` / `LifeOSGradientButton` / `LifeOSBadge` — shared tactile surfaces and actions

## Current-state addendum — 2026-09-16

This document remains part of the LifeOS documentation set. Current UI/UX, motion, responsive and accessibility rules are centralized in [`DESIGN.md`](./DESIGN.md). The current Intelligence implementation is local/offline and requires no external AI provider or API key. Build/test statements are only considered verified when the exact command has been executed in a real Android/Gradle environment.

## Current implementation snapshot — 2026-09-16

The 2026-09-16 frontend pass maps the Stitch HTML reference into native Compose components: shared header, floating selected navigation, rounded cards, quick actions, animated progress, and full-screen capture studio.

## 2026-09-18 UI integration notes
The supplied Stitch screens are implemented as native Jetpack Compose rather than embedded HTML. Profile, Expenses, Add Expense, and App Lock preserve real application state and callbacks. Profile photo selection uses Android's document picker and stores only the selected local content URI in DataStore.
