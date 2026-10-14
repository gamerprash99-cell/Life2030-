# LifeOS — Technical Requirements Document (TRD)

> Pairs with `PRD.md`. Describes the target technical design of the LifeOS
> Android app after the full redesign, and how it maps onto the existing
> codebase (which already implements Tasks, Habits, Expenses, Diary, Reminders).

---

## 1. Technology Stack

| Area | Technology | Notes |
|---|---|---|
| Language | Kotlin `2.0.21` | JVM target 17 |
| Build | Gradle `8.9`, AGP `8.6.1`, KSP `2.0.21-1.0.28` | single module `:app` |
| UI | Jetpack Compose (`compose-bom 2024.11.00`), Material 3 | no XML views |
| Navigation | `navigation-compose 2.8.4` | single `NavHost` |
| Persistence | Room `2.6.1` (SQLite) | `exportSchema = true` |
| DB encryption | SQLCipher `net.zetetic:sqlcipher-android:4.6.1` | Keystore-wrapped passphrase |
| Preferences | DataStore Preferences `1.1.1` | app settings only |
| Scheduling | WorkManager `2.10.0` + AlarmManager | reminders |
| Camera | CameraX `1.4.0` | photo/video capture |
| Audio | `MediaRecorder` / `MediaPlayer` | audio memories |
| Image loading | Coil `2.7.0` | local files only |
| Serialization | `kotlinx-serialization-json 1.7.3` | backup + attachment JSON |
| Async | `kotlinx-coroutines 1.9.0` | `Flow`/`StateFlow` |
| Testing | JUnit `4.13.2`, AndroidX Test, Espresso, Compose UI Test | JVM + instrumentation |

**Android:** `compileSdk 35`, `minSdk 26`, `targetSdk 35`.

**Hard constraints**
- No `INTERNET` permission, no HTTP client, no Firebase/analytics/telemetry.
- Dynamic color is **off** (`LifeOSTheme(dynamicColor = false)`) so the brand
  palette is device-stable.
- No destructive Room fallback. Schema changes require explicit `Migration`s.

---

## 2. Architecture

Layered, unidirectional data flow. UI never touches Room directly.

```
Compose UI  ─▶  ViewModel (StateFlow)  ─▶  Use case / Repository  ─▶  Room DAO
                                             │                        │
                                             ├─ DataStore (settings)  │
                                             ├─ File storage (media) ─┘
                                             └─ Local intelligence (no network)
```

### 2.1 Layers
- **`ui/`** — Compose screens, `LifeOSNavHost`, theme + reusable components.
- **`domain/`** — pure models, use cases, on-device intelligence analyzers.
- **`data/`** — Room `AppDatabase`, entities, DAOs, repositories; backup.
- **`core/`** — DI, security, reminders, media, location, util; AI facade.

### 2.2 Dependency injection
Manual DI (no Hilt/Dagger). `LifeOSApplication` builds one `ServiceLocator`;
`MainActivity` publishes it via `CompositionLocalProvider(LocalServiceLocator)`.
Screens build ViewModels with `LambdaViewModelFactory { XViewModel(locator.x) }`.
**Every `ServiceLocator` member stays `by lazy`** — the first dereference must
never run on the main thread (it would trigger the one-time SQLCipher open).

### 2.3 Startup contract
`DatabaseInit` = `Initializing → Ready | Error`. The native splash stays until
`Ready`. `AppDatabase.warmUpOpen()` runs on `Dispatchers.IO`. The UI that reads a
repository is composed **only** when `Ready`. No UI renders behind the splash
(building Home's ViewModel would force the DB open on the main thread).

---

## 3. Redesigned Navigation

### 3.1 Primary (nested `root_tabs` graph)
`Home`, `Notes`, `Insights`, `Life` are the four tab destinations. The
**centre `+ CAPTURE`** button is a fifth, visually prominent action that opens
the Capture hub; it is registered **outside** the tabs graph as a full-screen
overlay so it can be opened from anywhere.

### 3.2 Secondary (stacked above tabs)
`NoteEditor`, `Capture`, `CaptureViewer`, `Diary` (+ search/calendar/insights/
detail), `Timeline`, `LifeCalendar`, `GlobalSearch`, `Tasks`, `Habits` (+
detail), `Expenses`, `AiAssistant`, `Profile`, `Settings`, `AppLockSetup`.

Tabs use `popUpTo(startDestination){saveState=true}`, `launchSingleTop`,
`restoreState`. Overlays push on top; system Back pops one level.

### 3.3 Route table
Routes live in `ui/navigation/Screen.kt`; registration in `LifeOSNavHost.kt`.
`BottomBarVisibility.kt` is the single pure rule for "is the bar shown".
Literal routes must be declared **before** any argument route they could match
(e.g. `diary/search` before `diary/{entryId}`).

### 3.4 Full-screen owner contract
A full-window surface (Diary composer, Capture, AI) declares ownership via
`ComposerWindowOwner()` so the Scaffold hides the bar and hands over the bottom
inset. See `UI_UX_FLOW_NAVIGATION.md`.

---

## 4. Data Model

### 4.1 Existing entities (schema v5)
`tasks`, `habits`, `habit_completions`, `expenses`, `diary_entries`,
`reminders`. All keyed by TEXT `id`; timestamps as `Long`; day keys as
`dateEpochDay` (`LocalDate.toEpochDay()`), local times as `timeMinutes`.

> **History note:** `notes` and `captures` tables existed in schema v1, were
> **dropped in migration v2→v3**, and are **re-introduced in v5→v6** as
> first-class redesigned tables (§4.3). The rebuild must preserve the existing
> five tables untouched.

### 4.2 New/updated entities

#### `NoteEntity` (`notes`)
| Column | Type | Notes |
|---|---|---|
| `id` | TEXT PK | `IdGenerator.newId()` |
| `title` | TEXT? | |
| `content` | TEXT | rich/plain text payload |
| `contentJson` | TEXT | structured blocks (rich text) default `[]` |
| `folderId` | TEXT? | nullable = uncategorized |
| `tagsCsv` | TEXT | |
| `isPinned` | BOOLEAN | |
| `isFavorite` | BOOLEAN | |
| `isArchived` | BOOLEAN | |
| `isDeleted` | BOOLEAN | trash flag (soft delete) |
| `attachmentsJson` | TEXT | serialized attachment list default `[]` |
| `aiGenerated` | BOOLEAN | drafts start unreviewed |
| `createdAt` | Long | |
| `updatedAt` | Long | |
Indices: `updatedAt`, `createdAt`, `isDeleted`.

#### `NoteFolderEntity` (`note_folders`)
`id` TEXT PK, `name` TEXT, `sortOrder` INT, `createdAt` Long.
Seeded defaults: College, Projects, Ideas, Personal, Finance, Other.

#### `CaptureEntity` (`captures`)
| Column | Type | Notes |
|---|---|---|
| `id` | TEXT PK | |
| `type` | TEXT | PHOTO / VIDEO / AUDIO / THOUGHT / MOOD |
| `caption` | TEXT? | |
| `mood` | TEXT? | |
| `tagsCsv` | TEXT | |
| `mediaPath` | TEXT? | app-private file path |
| `mediaMime` | TEXT? | |
| `latitude` / `longitude` | REAL? | optional |
| `place` | TEXT? | optional label |
| `dateEpochDay` | Long | |
| `timeMinutes` | Int | |
| `createdAt` | Long | |
| `updatedAt` | Long | |
Indices: `dateEpochDay`, `type`.

Media (photo/video/audio) stays as **files** in app-private storage
(`filesDir/captures/`); the row stores a path + MIME, never a blob.

### 4.3 Migration `MIGRATION_5_6`
Pure additive: create `notes`, `note_folders`, `captures` (+ their indices and
the seed folders). No existing table is touched, so all current rows survive.
The SQL mirrors Room's generated `CREATE TABLE` exactly (enum → TEXT, Boolean →
INTEGER, non-null Kotlin field → NOT NULL) with Room's default index names
`index_<table>_<column>`. Bump `@Database(version = 6)`. KSP regenerates
`app/schemas/com.lifeos.app.data.db.AppDatabase/6.json` on build; commit it.

**Rule:** never rotate or delete the DB key; on key-resolution failure throw
`DatabaseKeyUnavailableException` and surface `DataKeyErrorScreen`.

---

## 5. Repositories & Use Cases

### 5.1 Repositories (`data/repository/`)
- `TaskRepository`, `HabitRepository`, `ExpenseRepository`, `DiaryRepository`,
  `ReminderRepository` (existing).
- **`NoteRepository` (new)** — CRUD + trash/restore, pin/favorite/archive,
  folders, sort, `LIKE` search, backup/restore, attachment file lifecycle
  (delete orphaned media on edit/delete, on `Dispatchers.IO`).
- **`CaptureRepository` (new)** — create from capture hub, observe by day/range,
  delete (and unlink its file), backup/restore, `LIKE` search over captions/tags.
- `BackupRepository` — extended to serialize the new `NoteEntity`,
  `NoteFolderEntity`, `CaptureEntity` along with the existing entities.

### 5.2 Use cases (`domain/usecase/`)
- Existing: `BuildTimelineUseCase`, `GetHomeSummaryUseCase`,
  `CaptureTransactionUseCase`, diary search/calendar/insights.
- **New:** `BuildLifeTimelineUseCase` (extend `TimelineItemType` with
  `NOTE`, `CAPTURE`), `GetInsightsUseCase` (monthly dashboard + productivity),
  `GetLifeCalendarUseCase` (per-day activity counts), `GlobalSearchUseCase`
  (cross-module), `AiAssistantUseCase` (question → intent → data → answer).

Timeline is a **real-time aggregation**, never its own table (§ spec 37/60).

---

## 6. Offline Intelligence ("LifeOS AI")

`domain/intelligence/` is a **local, deterministic** engine — no cloud model,
no API key, no network.

| Component | Responsibility |
|---|---|
| `StatisticsEngine` | counts, averages, comp-against-previous-period |
| `MoodAnalyzer` / `DiaryAnalyzer` | mood mix, diary stats |
| `PatternDetector` | recurring words, patterns |
| `KeywordExtractor` | tag/keyword suggestions |
| `ExpenseCategorizer` / `TransactionParser` | expense categorization (incl. notification capture) |
| `LocalQuestionEngine` | deterministic reflection prompts |
| **`LocalAssistantEngine` (new)** | maps an NL question to a structured query over repositories, then to an answer + cards |

### 6.1 AI context permissions (spec §30)
`AiPermissionLevel`: `CURRENT` (L1), `SELECTED` (L2), `MODULES` (L3),
`FULL` (L4). Persisted in DataStore as a set of allowed module keys
(`notes, tasks, habits, expenses, diary, photos, audio, location`). The engine
receives **only** permitted data; enforcement is server-side-of-the-app (in the
use case), not in the UI.

### 6.2 AI actions (spec §32)
Task extraction returns a **proposal** (`List<ProposedTask>`), rendered for
review; only an explicit user approval calls the repository. AI diary drafts are
`aiGenerated = true, isReviewed = false` until confirmed. No silent writes.

### 6.3 Voice
Optional Android on-device `SpeechRecognizer` (`LifeVoiceInputController`). No
audio leaves the device for transcription; if unavailable, degrade gracefully.

---

## 7. Security & Privacy

- **DB**: SQLCipher, random per-install passphrase wrapped by a Keystore
  AES-GCM key; never stored in plaintext.
- **App lock**: PIN (4 digits, PBKDF2-HMAC-SHA256, 120k iters, per-secret salt);
  escalating lockout; recovery question. Optional biometric.
- **Auto-lock**: locks after a grace period when the app returns to foreground.
- **Notifications**: `POST_NOTIFICATIONS` requested only when reminders are
  enabled; sensitive-preview hiding option.
- **No network egress.** Media is app-private. Backups are app-private until the
  user explicitly shares them.
- `android:allowBackup="false"` + backup/data-extraction rules keep the DB out
  of OS auto-backup.

---

## 8. Media Pipeline

- Photo: CameraX `ImageCapture`; Video: CameraX `VideoCapture`/`Recorder`;
  Audio: `MediaRecorder`.
- Written to `filesDir/captures/` via `MediaStorage`; **never** public storage or
  MediaStore.
- `FileProvider` (`res/xml/file_paths.xml`) is used **only** to share an exported
  backup.
- Deleting an owning row deletes its files (repositories own file lifecycle).

---

## 9. Dependency & Module Changes (implementation checklist)

1. `data/db/entities/`: add `NoteEntity`, `NoteFolderEntity`, `CaptureEntity`.
2. `data/db/dao/`: add `NoteDao`, `NoteFolderDao`, `CaptureDao`.
3. `data/db/AppDatabase.kt`: register entities/DAOs, `version = 6`, add
   `MIGRATION_5_6`.
4. `data/repository/`: add `NoteRepository`, `CaptureRepository`; extend
   `BackupRepository`.
5. `core/di/ServiceLocator.kt`: add lazy `noteRepository`, `captureRepository`,
   and the new use cases.
6. `domain/model/TimelineItem.kt`: extend `TimelineItemType`.
7. `ui/navigation/`: new `Screen` routes; 5-section `LifeOSBottomBar`; NavHost
   wiring; extend `BottomBarVisibility`.
8. `ui/`: new `notes/`, `capture/`, `insights/`, `life/`, `ai/` packages;
   redesign `home/`.
9. `ui/theme/` + `ui/components/`: glassmorphism tokens/components (§10).
10. Tests: pure-logic unit tests for repositories/use cases/engines; keep
    existing tests green.

---

## 10. UI / Design System (see `UI_UX_FLOW_NAVIGATION.md`)

- **Style**: premium, glassmorphism, Gen-Z, minimal, modern, calm, personal.
- **Tokens** in `ui/theme/`: colors, type, shape (`LifeOSRadius`), spacing
  (`LifeOSSpacing`), glass (`LocalGlassColors`).
- **Reusable**: `GlassCard`, `GlassChip`, `LifeOSCard`, `LifeOSGradientButton`,
  `LifeOSBadge`, `LifeOSSectionHeader`, `LifeOSTopBar`, `LifeOSBottomBar`,
  `ProfileAvatar`.
- Glass is used for chrome and hero surfaces only; body surfaces can be opaque
  for readability and battery. All colour pairs must meet WCAG AA; 48dp minimum
  targets; TalkBack labels on all controls.

---

## 11. Build & Verification

```
# with a system Gradle 8.9 and JDK 17 (no wrapper JAR committed)
gradle :app:compileDebugKotlin
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
gradle :app:lintDebug
```

CI: `.github/workflows/android-build.yml` (JDK 17 + Android SDK 35 + Gradle 8.9,
runs `test assembleDebug assembleRelease`).

**Definition of done for a change:** compiles (`compileDebugKotlin`), unit tests
pass, lint has no new errors, migrations are explicit and additive, and no
`INTERNET`/telemetry was introduced.

---

## 12. Testing Strategy

- **Unit (JVM)**: repositories (with in-memory fakes), use cases, intelligence
  engines, backup serialization, navigation rules (`BottomBarVisibility`).
- **Instrumentation**: Room migration tests (`MigrationTestHelper`) validating
  v5→v6 against the exported schema; Compose UI tests for key flows.
- **Manual/QA**: cold start, capture speed, accessibility (TalkBack, large font),
  dark mode, backup/restore round-trip.

---

## 13. Risks & Mitigations

| Risk | Mitigation |
|---|---|
| Migration SQL mismatch with Room schema | Mirror generated SQL; add migration test; commit exported schema JSON |
| Main-thread DB open regression | All ServiceLocator members lazy; UI gated on `Ready` |
| Glass effects hurting perf/battery/a11y | Use glass only for chrome/hero; opaque body; contrast-checked |
| Duplicate tab stacking | Keep tabs in one graph, overlays outside; literal routes first |
| Scope creep across modules | Deliver in phases (§PRD 9); each phase compiles + tests green |

*End of TRD.*
