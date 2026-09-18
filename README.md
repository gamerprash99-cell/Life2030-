# LifeOS — Android

LifeOS is a native **Kotlin + Jetpack Compose + Material 3** personal life-management
app built around an **offline-first, privacy-first, local-first** model. It is
designed as a private "second brain": notes, tasks, habits, diary, expenses,
media captures and a cross-feature timeline all live in one encrypted,
on-device database.

- **Application ID / namespace**: `com.lifeos.app`
- **Version**: `0.2.0` (`versionCode` 2) — see `app/build.gradle.kts`
- **Module layout**: single Gradle module, `:app` (`settings.gradle.kts`)

---

## 1. Project Overview

**Purpose.** LifeOS brings the small, repeating parts of personal life
management — what you need to do, what you did, how you felt, where your money
went, and the moments you captured — into one connected experience.

**Product vision.** A "second brain" that you fully own. Nothing is sent to a
server: the app has no backend, no account system, no telemetry, and no cloud
AI. Every feature is designed to work with the device in airplane mode.

**Core principles**
- **Offline-first** — all features work without a connection.
- **Privacy-first** — no data leaves the device; no analytics/telemetry SDKs.
- **Local data ownership** — the database lives in app-private internal storage,
  encrypted at rest, and can be exported/restored by the user as JSON.
- **Android-native architecture** — Kotlin, Jetpack Compose, Room, WorkManager,
  CameraX; no web runtime or cross-platform layer.

---

## 2. Technology Stack

Only technologies actually present in this repository
(`build.gradle.kts`, `app/build.gradle.kts`):

| Area | Technology |
|---|---|
| Language | Kotlin `2.0.21` |
| Android plugin | Android Gradle Plugin `8.6.1`, KSP `2.0.21-1.0.28` |
| UI | Jetpack Compose (`compose-bom:2024.11.00`), Material 3 |
| Navigation | Navigation Compose `2.8.4` |
| Persistence | Room `2.6.1` (SQLite) |
| DB encryption | SQLCipher (`net.zetetic:sqlcipher-android:4.6.1`) |
| Preferences | DataStore Preferences `1.1.1` |
| Scheduling | WorkManager `2.10.0` (`work-runtime-ktx`) |
| Camera | CameraX `1.4.0` (`camera-core/camera2/lifecycle/view/video`) |
| Audio | `android.media.MediaRecorder` / `MediaPlayer` |
| Image loading | Coil `2.7.0` (`coil-compose`) |
| Serialization | kotlinx.serialization JSON `1.7.3` |
| Async | kotlinx.coroutines `1.9.0` |
| Security | `androidx.biometric` declared; App Lock is PIN-based (see §10) |
| Testing | JUnit `4.13.2`, AndroidX Test JUnit `1.2.1`, Espresso `3.6.1`, Compose UI Test |
| Build | Gradle `8.9`, JDK `17` |

**Android versions**: `compileSdk 35`, `minSdk 26`, `targetSdk 35`, JVM target 17.

> Dynamic color is intentionally **off** (`LifeOSTheme(dynamicColor = false)`), so
> the LifeOS brand palette is stable across devices.

---

## 3. Architecture

The app is one module with a layered, unidirectional flow:

```text
Compose UI
   ↓
ViewModel (StateFlow)
   ↓
Use cases / repositories
   ↓
Room DAOs / DataStore / local services
```

- **`ui/`** — Compose screens, `LifeOSNavHost`, reusable components.
- **`domain/`** — pure models and use cases (`NoteBlock`, `TimelineItem`,
  `ExpenseCategories`, `BuildTimelineUseCase`, `GetHomeSummaryUseCase`).
- **`data/`** — Room `AppDatabase`, entities, DAOs, repositories.
- **`core/`** — cross-cutting infrastructure:
  - `core/di/` — `ServiceLocator` + Compose `LocalServiceLocator` +
    `LambdaViewModelFactory`.
  - `core/security/` — `DatabasePassphraseProvider`, `PinHasher`.
  - `core/reminders/` — WorkManager scheduling/worker.
  - `core/intelligence/` — deterministic, on-device analyzers.
  - `core/ai/` — `AiRepository`, a compatibility facade over the local engine.
  - `core/util/` — dates, settings, permissions, media, notifications.

**Dependency injection** is manual (no Hilt/Dagger). `LifeOSApplication` builds a
single `ServiceLocator`; `MainActivity` provides it via
`CompositionLocalProvider`. Screens construct their ViewModel with
`LambdaViewModelFactory { XViewModel(locator.xRepository) }`.

**State management** is plain AndroidX (`ViewModel` + `StateFlow`/`Flow`); there
is no third-party MVI/Redux framework.

**UI code never touches Room directly** — it always goes through repositories
(or use cases).

---

## 4. Features

| Area | Current implementation |
|---|---|
| **Home** | Today dashboard: greeting/date, Daily Momentum progress, quick actions, Today's Priorities, habits streak, Recent Activity, LifeOS Intelligence, Capture entry. `ui/home/HomeScreen.kt`, `HomeViewModel.kt`, `domain/usecase/GetHomeSummaryUseCase.kt` |
| **Tasks** | Priorities, due date/time, category, description, repeat rules, reminders, overdue handling. `ui/tasks/TasksScreen.kt` |
| **Habits** | Daily/custom schedules, goal counts, streaks, 12-week heatmap. `ui/habits/HabitsScreen.kt`, `HabitDetailScreen.kt` |
| **Diary** | Private journal with mood tagging, user-written; explicit Save. `ui/diary/DiaryScreen.kt` |
| **Notes** | Block-based rich text, pin/favorite/archive/trash, folder chips, local AI actions. `ui/notes/` |
| **Expenses** | Monthly spend, daily average, fixed budget, remaining, recent transactions, add-expense sheet with categories. `ui/expenses/ExpensesScreen.kt` |
| **Timeline** | Day-by-day merge of notes/tasks/habits/expenses/diary/captures. `ui/timeline/TimelineScreen.kt`, `BuildTimelineUseCase.kt` |
| **Capture** | Photo (CameraX), video (CameraX Recorder), audio (MediaRecorder), quick thought; post-capture confirmation and a detail viewer. `ui/capture/` |
| **Search** | Cross-feature `LIKE` search (not full-text/semantic). `ui/search/SearchScreen.kt` |
| **App Lock** | PIN-only gate (`NONE`/`PIN`), salted PBKDF2 hash, recovery + lockout. `ui/security/`, `core/security/` |
| **Settings** | App Lock, reminders toggle, Intelligence toggle, backup/export/restore. `ui/settings/SettingsScreen.kt` |
| **Profile** | Local display name + profile photo (local content URI). `ui/profile/ProfileScreen.kt` |
| **Intelligence** | On-device analyzers, reports, assistant chat, insights. `core/intelligence/` |
| **Backup / Restore** | JSON export (app-private file) + restore via Android SAF. `data/repository/BackupRepository.kt` |
| **Reminders** | Per-task/habit WorkManager jobs + notification permission on demand. `core/reminders/` |
| **Onboarding** | 4-page first launch with optional JSON restore. `ui/onboarding/OnboardingScreen.kt` |
| **Media** | App-private capture storage, Coil previews, local audio/video playback. `core/util/MediaStorage.kt`, `ui/capture/` |

---

## 5. Expenses

The Expenses screen (`ui/expenses/ExpensesScreen.kt`) is repository-backed and
shows:

- **This Month** card — total spend for the current month
  (`ExpenseRepository.observeTotalInRange`).
- **Daily avg** — `total / number of days in the current month`.
- **Budget** — a fixed local constant (`15_000.0` in `ExpensesScreen.kt`).
- **Left** — `max(budget - total, 0)`.
- **Recent Transactions** — current-month expenses from
  `ExpenseRepository.observeInRange`, each with category emoji, title and amount.
- **Empty state** — shown when no expenses exist this month.
- **Purple FAB (+)** — opens the existing Material 3 `ModalBottomSheet`
  "Add expense".

**Adding an expense**
1. Enter `Amount (INR)` and an optional `Merchant or note`.
2. Pick one category from `ExpenseCategories.ALL`
   (`domain/model/Categories.kt`): Food, Cafe, Shopping, Travel, Entertainment,
   Education, Bills, Health, Subscriptions, Other.
3. `Save` calls `ExpensesViewModel.addExpense()`, which records the date, time,
   amount, category and merchant through `ExpenseRepository` → `ExpenseDao`.

**Category selection behavior (current).** A single `selectedCategory` state
exists inside `AddExpenseSheet`; the default is `Food`. Tapping a chip updates
that one state. The selected chip is rendered in the theme's
`colorScheme.primary` (purple/lavender) with `onPrimary` content color, so
exactly one category looks active at a time; all others keep the normal
glass-chip appearance. The existing "Selected: <name>" line is retained.

**Add-expense sheet presentation (current).** The sheet opens in the expanded
position by default (`rememberModalBottomSheetState(skipPartiallyExpanded = true)`)
so the whole form is visible without a manual drag. Its body scrolls when the
available height shrinks (keyboard, landscape, large font scales). Shape, drag
handle, background, fields, buttons, swipe-to-dismiss and Back-to-dismiss are
unchanged.

---

## 6. Database

See [`docs/05_DATABASE.md`](./docs/05_DATABASE.md) for the full schema.

- **Provider**: Room `2.6.1` on SQLite, defined in
  `data/db/AppDatabase.kt`. File name `lifeos.db` (`DatabasePassphraseProvider.DATABASE_NAME`).
- **Encryption**: SQLCipher (`SupportOpenHelperFactory`). The passphrase is a
  random per-install secret wrapped by an Android Keystore AES-GCM key
  (`core/security/DatabasePassphraseProvider.kt`); the raw passphrase is never
  stored in plaintext.
- **Schema version**: `1`, `exportSchema = true`; the exported schema is
  committed at `app/schemas/com.lifeos.app.data.db.AppDatabase/1.json`.
- **Entities (7 tables)**: `notes`, `tasks`, `habits`, `habit_completions`,
  `expenses`, `diary_entries`, `captures`.
- **Migrations**: none yet. There is **no destructive fallback**, so a future
  schema change must add an explicit Room `Migration`.
- **Data preservation**: key-resolution failures throw
  `DatabaseKeyUnavailableException` and surface `DataKeyErrorScreen`
  (retry/exit) rather than ever rotating the key or deleting data.
- **Reads/writes** go through DAOs (`data/db/dao/`) wrapped by repositories
  (`data/repository/`). Reactive reads are exposed as `Flow`.

---

## 7. Navigation

Navigation Compose with a single `NavController` in
`ui/navigation/LifeOSNavHost.kt`:

- Four primary destinations (**Home, Tasks, Habits, Insights**) live in one
  nested graph (`root_tabs`) with `saveState`/`restoreState`/`launchSingleTop`,
  so the bottom bar returns to a section's saved state.
- Secondary screens (Notes, Note Editor, Habit Detail, Expenses, Diary,
  Timeline, Capture Detail, Search, AI Assistant, Profile, Settings, App Lock
  Setup) are registered **outside** that graph and stack on top; system Back
  dismisses them one level at a time.
- Routes are declared in `ui/navigation/Screen.kt`.

---

## 8. UI / UX design system

Centralized in `ui/theme/` and `ui/components/`, specified in
[`docs/DESIGN.md`](./docs/DESIGN.md).

- **Colors** (`ui/theme/Color.kt`): LifeOS violet primary `#7C4DFF`, magenta
  secondary `#D946EF`, lavender `#EADDFF`, plus category accent colors.
- **Theme** (`Theme.kt`): light/dark `ColorScheme`s + a `LocalGlassColors`
  CompositionLocal for the glass surfaces. Dynamic color is off by default.
- **Typography / Shape / Spacing**: Material 3 type scale, large rounded shapes
  and `LifeOSSpacing` tokens.
- **Reusable components**: `GlassCard`, `GlassChip` (now supports a `selected`
  state driven by `colorScheme.primary`), `LifeOSCard`, `LifeOSGradientButton`,
  `LifeOSBadge`, `LifeOSSectionHeader`, `LifeOSBottomBar`, `LifeOSTopBar`,
  `ReminderTimePickerDialog`, `ProfileAvatar`.
- **Responsive / insets**: `Scaffold` + `WindowInsets`; the app uses
  `adjustResize` (see manifest) so forms stay above the keyboard.
- **Bottom navigation**: Home, Tasks, Habits, Insights with always-visible
  labels and a lavender selected pill.

---

## 9. Security

See [`docs/08_SECURITY.md`](./docs/08_SECURITY.md).

- **App Lock**: `NONE` or `PIN`. PINs and recovery answers are hashed with
  PBKDF2-HMAC-SHA256 (120k iterations, per-secret salt, `v2$…` format); legacy
  v1 hashes still verify. Five failures escalate a lockout (30 s → 16 min).
- **Database**: SQLCipher-encrypted at rest; Keystore-wrapped passphrase.
- **No biometric mode**: the former `BIOMETRIC` option was removed; a legacy
  stored `BIOMETRIC` value resolves to `NONE`.
- **No cloud secrets**: there is no API key or remote credential in the app.
- `android:allowBackup="false"` plus backup/data-extraction rules keep the
  database out of OS auto-backup.

---

## 10. Permissions

Declared in `app/src/main/AndroidManifest.xml`; each is requested at runtime
only when its feature is used (`core/util/PermissionManager.kt`):

| Permission | Requested when |
|---|---|
| `CAMERA` | Opening photo or video capture |
| `RECORD_AUDIO` | Opening audio capture |
| `POST_NOTIFICATIONS` | Enabling reminders (Android 13+) |

`android.permission.INTERNET` is **not declared**. `android.hardware.camera` is
declared `required="false"`.

---

## 11. Media / Capture

- Photo: CameraX `ImageCapture`; Video: CameraX `VideoCapture`/`Recorder`;
  Audio: `MediaRecorder`; Thought: text.
- Files are written to **app-private storage** (`core/util/MediaStorage.kt`,
  `filesDir/captures/`) — never to public storage or MediaStore.
- `CaptureSheet` shows a confirmation with a real preview before closing;
  `CaptureDetailScreen` reopens captures from the Timeline.
- `FileProvider` (`res/xml/file_paths.xml`) is used only to share an exported
  backup file.

---

## 12. Offline AI / Intelligence

`core/intelligence/` implements a **local, deterministic** intelligence engine
(`LifeOSIntelligenceEngine`, `MoodAnalyzer`, `NoteTextAnalyzer`,
`PatternDetector`, `ReportGenerator`, `LocalQuestionEngine`, …). `core/ai/AiRepository.kt`
is a compatibility facade over it. There is **no cloud model, API key, network
call, or telemetry**. `core/ai/runtime/LifeVoiceInputController.kt` optionally
uses Android's on-device `SpeechRecognizer` when available.

LIFE Phase 18 is integrated as a local, allow-listed controller with
repository-only data access. Production neural inference remains gated on a real
validated checkpoint — see [`docs/33_LIFE_INTEGRATION_PHASE18.md`](./docs/33_LIFE_INTEGRATION_PHASE18.md).

---

## 13. Build & Development

**Requirements**: JDK 17, Android SDK with `platforms;android-35` and
`build-tools;35.0.0`, Gradle 8.9.

This snapshot ships `gradle/wrapper/gradle-wrapper.properties` but **not**
`gradlew`/`gradlew.bat`/`gradle-wrapper.jar` (see `docs/16_KNOWN_ISSUES.md`
Issue #1). Generate the wrapper once with `gradle wrapper --gradle-version 8.9`,
or run a system Gradle 8.9 directly:

```text
gradle test
gradle assembleDebug
gradle assembleRelease
gradle lintDebug
```

- **Debug build**: `gradle assembleDebug`.
- **Release**: minified (`isMinifyEnabled = true`); signs with
  `keystore.properties` when present (copy `keystore.properties.example`),
  otherwise falls back to debug signing so `assembleRelease` still compiles.
- **CI**: `.github/workflows/android-build.yml` installs JDK 17 + Android SDK 35,
  sets up Gradle 8.9, and runs `gradle test assembleDebug assembleRelease`.

---

## 14. Testing

See [`docs/14_TESTING.md`](./docs/14_TESTING.md).

JVM unit tests live under `app/src/test/` and cover PIN hashing, DB-key
decision logic, habit stats, repeat rules, date utils, offline intelligence and
backup serialization. Instrumentation/Compose test dependencies are declared but
there is currently **no `app/src/androidTest/` suite**.

---

## 15. Repository Structure

```text
LifeOS/
├── app/
│   ├── build.gradle.kts
│   ├── schemas/com.lifeos.app.data.db.AppDatabase/1.json
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/lifeos/app/
│       │   │   ├── MainActivity.kt, LifeOSApplication.kt
│       │   │   ├── core/     (ai, di, intelligence, life, reminders, security, util)
│       │   │   ├── data/     (db/entities, db/dao, repository)
│       │   │   ├── domain/   (model, usecase)
│       │   │   └── ui/       (theme, components, navigation, <feature> screens)
│       │   └── res/          (values, xml, drawable, mipmap)
│       └── test/java/com/lifeos/app/…   (JVM unit tests)
├── docs/                     (detailed documentation set)
├── .github/workflows/android-build.yml
├── build.gradle.kts, settings.gradle.kts, gradle.properties
├── README.md, UPDATE.md, FINAL_RELEASE_CHECKLIST.md
└── keystore.properties.example
```

See [`docs/21_FILE_STRUCTURE.md`](./docs/21_FILE_STRUCTURE.md) for per-file notes.

---

## 16. Privacy

- All user data is stored in the encrypted Room database and DataStore in
  **app-private internal storage**.
- Captures are app-private files.
- Exported backups are written to app-private storage and only leave the app
  when the user explicitly shares them.
- No `INTERNET` permission, no analytics, no telemetry, no cloud AI, no
  third-party data upload. Data never leaves the device unless the user exports
  and shares it.

---

## 17. Current UI/UX behavior (highlights)

- Four-destination bottom bar (Home, Tasks, Habits, Insights) with saved-state
  restoration; Settings is reached from Profile.
- Home dashboard with animated Daily Momentum and live repository-backed data.
- Expenses: monthly card + recent transactions + FAB → expanded add-expense
  sheet with a single purple-highlighted selected category.
- Capture: full-screen studio with post-capture confirmation.
- App Lock: 4-digit PIN keypad with auto-advance and lockout.

---

## 18. Recent Changes

**2026-09-19 — Expenses micro UX fix**
- The existing "Add expense" `ModalBottomSheet` now opens expanded by default
  (`skipPartiallyExpanded = true`) and its body scrolls, so the whole form is
  reachable immediately across screen sizes, aspect ratios, insets and font
  scales. Design, fields, buttons, drag handle, swipe/Back dismissal unchanged.
- The selected expense category chip is now rendered in the theme's
  `colorScheme.primary` with `onPrimary` content color; exactly one category is
  highlighted and the highlight follows the existing `selectedCategory` state.
  `GlassChip` gained an optional `selected` parameter (reused, not duplicated).
- Documentation audited and updated (README, UPDATE, changelog, features,
  frontend, database, testing, known issues, file structure, release checklist).
- Verified in this environment: `gradle :app:compileDebugKotlin`,
  `:app:assembleDebug`, `:app:testDebugUnitTest` (81 tests, 0 failures) and
  `:app:lintDebug` (0 errors, 4 pre-existing warnings).

**2026-09-18 — Profile / Expenses / App Lock UI pass**
- Local profile-photo picker with persisted URI; Settings moved to Profile.
- Stitch visual treatment for Profile, Expenses, Add Expense and App Lock while
  keeping repositories/use cases and local data flows.
- Navigation Back handling and onboarding startup-gating hardened; audio
  recording/playback lifecycle hardened.

Earlier 2026-09-16/17 UI, navigation, security and LIFE Phase 18 work is
recorded in [`UPDATE.md`](./UPDATE.md) and [`docs/17_CHANGELOG.md`](./docs/17_CHANGELOG.md).

---

## 19. Known Limitations / Remaining Issues

Real, verified limitations (full list in
[`docs/16_KNOWN_ISSUES.md`](./docs/16_KNOWN_ISSUES.md)):

- **No Gradle wrapper scripts** committed (`gradlew`/JAR), so a fresh clone
  cannot use `./gradlew` until the wrapper is generated.
- **No release keystore** in the repo; a real keystore must be supplied via
  `keystore.properties` before Play submission.
- **Backup restore verification** still needs execution against representative
  valid/invalid backups on a device.
- **AI Assistant chat** does not inject real app data as context
  (`contextBlock = null`).
- **No `androidTest/` instrumentation suite** exists yet.
- Lint reports 4 pre-existing warnings (an obsolete `-v26` resource folder, an
  unused round launcher icon + unused `tagline` string, and a missing
  monochrome launcher-icon tag, plus 3 informational autoboxing hints) — none
  are errors and none are from the Expenses fix.
- Database/UI behavior on upgrades still depends on adding explicit Room
  migrations once the schema changes past v1.

---

## 20. Development Guidelines

To keep LifeOS production-safe:

- **Architecture**: keep UI → ViewModel → use case/repository → DAO. Never let
  a screen import a DAO or open Room directly.
- **Offline-first**: do not add `INTERNET`, HTTP clients, Firebase, analytics,
  telemetry, or cloud AI. New intelligence must stay on-device.
- **Database safety**: never add a destructive fallback or rotate/delete the DB
  key; add explicit `Migration`s and bump the schema version when entities
  change. Keep `app/schemas/…/1.json` committed.
- **Navigation**: keep the nested `root_tabs` graph + outside-stacked secondary
  routes; register new screens in both `Screen.kt` and `LifeOSNavHost.kt`.
- **UI consistency**: reuse `ui/theme/` tokens and `ui/components/` rather than
  hardcoding colors/sizes; follow `docs/DESIGN.md`.
- **Security/privacy**: request permissions only when a feature needs them; keep
  PIN hashing and lockout intact; never log or upload user data.
- **Docs**: preserve historical notes and update the affected current-state
  documents (and changelog) with every change.

---

## Build status

Verified in the audit environment with Gradle 8.9 + Android Gradle Plugin 8.6.1
(offline, engine `android.aapt2FromMavenOverride`):

- `gradle :app:compileDebugKotlin` — **PASS**
- `gradle :app:assembleDebug` — **PASS**
- `gradle :app:testDebugUnitTest` — **PASS** (81 tests, 0 failures)
- `gradle :app:lintDebug` — **PASS** (0 errors, 4 pre-existing warnings)

The repository still does not ship the Gradle wrapper JAR/scripts, so builds use
a locally-installed Gradle 8.9 distribution. Instrumentation tests were not run
(no emulator/device, no `androidTest` suite).

When a full Android environment is available, run:

```text
./gradlew test
./gradlew assembleDebug
```

and the configured instrumentation checks as applicable.

## Documentation

Detailed documentation remains in [`docs/`](./docs); historical notes are
preserved and current-state corrections are recorded in the affected files.

Start with:

1. [`docs/00_PROJECT_OVERVIEW.md`](./docs/00_PROJECT_OVERVIEW.md)
2. [`docs/02_ARCHITECTURE.md`](./docs/02_ARCHITECTURE.md)
3. [`docs/DESIGN.md`](./docs/DESIGN.md)
4. [`docs/19_DEVELOPER_HANDOVER.md`](./docs/19_DEVELOPER_HANDOVER.md)
5. [`docs/16_KNOWN_ISSUES.md`](./docs/16_KNOWN_ISSUES.md)

## LIFE Phase 18 Consolidation

LIFE is integrated into the LifeOS source as a local, allow-listed controller
with repository-only data access and optional Android on-device voice input. No
cloud AI or network fallback is part of the LIFE execution path. Production
neural inference remains gated on a real validated LIFE checkpoint. See
`FINAL_RELEASE_CHECKLIST.md` and `docs/33_LIFE_INTEGRATION_PHASE18.md`.
