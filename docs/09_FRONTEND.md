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
    ├── habits/                  ← Habits list + detail/heatmap
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
| `habits` | `HabitsScreen` | |
| `habits/{habitId}` | `HabitDetailScreen` | |
| `expenses` | `ExpensesScreen` | |
| `diary` | `DiaryScreen` | |
| `timeline` | `TimelineScreen` | |
| `insights` | `InsightsScreen` | |
| `search` | `SearchScreen` | |
| `ai_assistant` | `AiAssistantScreen` | |
| `settings` | `SettingsScreen` | |

Bottom navigation bar (`ui/components/LifeOSBottomBar.kt`) shows only the four
primary destinations in `Screen.bottomNavItems`: **Home, Tasks, Habits,
Insights** (each with an always-visible label and a lavender selected pill).
Settings is reached from **Profile**; Notes, Expenses, Diary, Timeline, Search,
Capture Detail, AI Assistant, Profile and App Lock Setup are secondary routes
stacked outside the `root_tabs` graph. A new developer who expects Settings or
Expenses in the bottom bar should look there instead.

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

Most small forms use Compose `AlertDialog`s with `OutlinedTextField`s (e.g.
"Add task" / "Add habit"). The **Add expense** form is a Material 3
`ModalBottomSheet` (`androidx.compose.material3.ModalBottomSheet` inside
`ExpensesScreen.kt`); it opens expanded by default
(`rememberModalBottomSheetState(skipPartiallyExpanded = true)`) and its body is
scrollable. There is no shared form-validation library or framework — each
screen does its own minimal validation inline (e.g. `if (title.isBlank()) return`).

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
- `LifeOSBottomBar.kt` — the four-item bottom navigation bar (Home, Tasks, Habits, Insights)
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

Only `ui/ai/AiAssistantScreen.kt`, `ui/notes/*`, `ui/diary/DiaryScreen.kt`,
and `ui/insights/InsightsScreen.kt` talk to any external service — and they
do so through `core/ai/AiRepository`, which delegates to the local Intelligence Engine. All other screens use the existing repository/domain boundaries
via repositories.

## Reusable components

- `GlassCard` / `GlassChip` (`ui/components/GlassCard.kt`) — `GlassChip` now
  accepts an optional `selected` flag; when set it uses
  `MaterialTheme.colorScheme.primary`/`onPrimary`, otherwise the original glass
  surface. Used by the Add Expense category row.
- `LifeOSBottomBar` (`ui/components/LifeOSBottomBar.kt`) — Home, Tasks, Habits,
  Insights with animated selected pills and always-visible labels
- `LifeOSTopBar` (`ui/components/LifeOSTopBar.kt`) — consistent screen headers
- `ReminderTimePickerDialog` (`ui/components/ReminderTimePickerDialog.kt`)
- `LifeOSCard` / `LifeOSGradientButton` / `LifeOSBadge` /
  `LifeOSSectionHeader` (`ui/components/LifeOSDesignComponents.kt`)
- `ProfileAvatar`, `PinComponents`

There is currently no dedicated shared component for buttons, text fields,
or list rows — each screen builds its own `AlertDialog`/`OutlinedTextField`
combinations inline. This is a documented opportunity for future
consolidation (see `docs/18_ROADMAP.md`).

## Shared presentation components

- `LifeOSTopBar` (`ui/components/LifeOSTopBar.kt`) — consistent primary/secondary screen header
- `LifeOSBottomBar` — persistent five-section navigation
- `LifeOSCard` / `LifeOSGradientButton` / `LifeOSBadge` — shared tactile surfaces and actions

## Current-state addendum — 2026-09-16

This document remains part of the LifeOS documentation set. Current UI/UX, motion, responsive and accessibility rules are centralized in [`DESIGN.md`](./DESIGN.md). The current Intelligence implementation is local/offline and requires no external AI provider or API key. Build/test statements are only considered verified when the exact command has been executed in a real Android/Gradle environment.

## Current implementation snapshot — 2026-09-16

The 2026-09-16 frontend pass maps the Stitch HTML reference into native Compose components: shared header, floating selected navigation, rounded cards, quick actions, animated progress, and full-screen capture studio.

## 2026-09-18 UI integration notes
The supplied Stitch screens are implemented as native Jetpack Compose rather than embedded HTML. Profile, Expenses, Add Expense, and App Lock preserve real application state and callbacks. Profile photo selection uses Android's document picker and stores only the selected local content URI in DataStore.

## 2026-09-19 frontend update — Expenses micro UX
- `AddExpenseSheet` (`ui/expenses/ExpensesScreen.kt`) now uses `rememberModalBottomSheetState(skipPartiallyExpanded = true)` and a scrollable body, so the existing sheet opens expanded and all fields remain reachable under IME/landscape/font scaling.
- `GlassChip` gained an optional `selected` parameter; the Expenses category row passes `selected = selectedCategory == cat.name`, rendering the active category in the theme's primary color with `onPrimary` text. The existing single selection state is reused.
