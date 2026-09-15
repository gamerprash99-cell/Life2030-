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

Bottom navigation bar (`ui/components/LifeOSBottomBar.kt`) only shows 5 of
these routes: Home, Timeline, Tasks, Habits, Settings (`Screen.bottomNavItems`).
The rest (Notes, Expenses, Diary, Insights, Search) are reached via **quick-link
chips on the Home screen** (`HomeScreen.kt`), not the bottom bar directly —
worth knowing if a new developer expects them in the bottom nav and doesn't
find them there.

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

All forms in this app are simple Compose `AlertDialog`s with
`OutlinedTextField`s (e.g. "Add task" in `TasksScreen.kt`, "Add habit" in
`HabitsScreen.kt`, "Add expense" in `ExpensesScreen.kt`). There is no shared
form-validation library or framework — each screen does its own minimal
validation inline (e.g. `if (title.isBlank()) return`).

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

Only `ui/ai/AiAssistantScreen.kt`, `ui/notes/*`, `ui/diary/DiaryScreen.kt`,
and `ui/insights/InsightsScreen.kt` talk to any external service — and they
do so exclusively through `core/ai/AiRepository`, never calling `AiClient`
or OkHttp directly. All other screens talk only to the local Room database
via repositories.

## Reusable components

- `GlassCard` / `GlassChip` (`ui/components/GlassCard.kt`)
- `LifeOSBottomBar` (`ui/components/LifeOSBottomBar.kt`)
- `ReminderTimePickerDialog` (`ui/components/ReminderTimePickerDialog.kt`)

There is currently no dedicated shared component for buttons, text fields,
or list rows — each screen builds its own `AlertDialog`/`OutlinedTextField`
combinations inline. This is a documented opportunity for future
consolidation (see `docs/18_ROADMAP.md`).
