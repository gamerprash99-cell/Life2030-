# 21 — File Structure

Only meaningful files/folders are documented. Generated/boilerplate files
are skipped where trivial.

---

### build.gradle.kts (root)
PURPOSE: Declares plugin versions shared across the project (AGP, Kotlin, Compose compiler, KSP)
DEPENDENCIES: None
WHAT BREAKS IF MODIFIED: Changing a plugin version here can break the entire build if incompatible with app/build.gradle.kts's dependencies

### settings.gradle.kts
PURPOSE: Declares this is a single-module project (include(":app"))
WHAT BREAKS IF MODIFIED: Removing the :app include breaks the build entirely

### app/build.gradle.kts
PURPOSE: All app-level configuration — SDK versions, every dependency, build types, Room/KSP config
WHAT BREAKS IF MODIFIED: Central file — most build issues trace back here. Removing a dependency will break every file that imports it.

### app/src/main/AndroidManifest.xml
PURPOSE: Declares permissions, the Application class, MainActivity, and the FileProvider
WHAT BREAKS IF MODIFIED: Removing a uses-permission breaks the corresponding feature (e.g. removing CAMERA breaks photo/video capture); removing allowBackup="false" re-enables OS auto-backup of the local database (see docs/08_SECURITY.md)

---

## app/src/main/java/com/lifeos/app/

### MainActivity.kt
PURPOSE: App entry point. Sets up the theme, provides ServiceLocator via CompositionLocalProvider, and wraps the app in OnboardingGate then AppLockGate then LifeOSNavHost
IMPORTANT FUNCTIONS: AppLockGate() — the biometric gating composable
DEPENDENCIES: LifeOSApplication, ServiceLocator, LifeOSNavHost
WHAT BREAKS IF MODIFIED: MainActivity owns the app entry point and existing Compose activity lifecycle.

### LifeOSApplication.kt
PURPOSE: Application subclass; builds the single ServiceLocator instance and ensures the notification channel exists on startup
WHAT BREAKS IF MODIFIED: If not registered in AndroidManifest.xml's android:name=".LifeOSApplication", ServiceLocator won't be available anywhere

---

## core/ — cross-cutting infrastructure

### core/di/ServiceLocator.kt
PURPOSE: The single composition root — constructs the database, repositories, local Intelligence engine and use cases, exactly once (singleton pattern)
DEPENDENCIES: Everything in data/ and the AI classes in core/ai/
WHAT BREAKS IF MODIFIED: Removing a repository here breaks every screen that depends on it via LocalServiceLocator

### core/di/LocalServiceLocator.kt
PURPOSE: Defines the Compose CompositionLocal (LocalServiceLocator) and a generic ViewModelProvider.Factory (LambdaViewModelFactory)
WHAT BREAKS IF MODIFIED: Removing this breaks every screen's LocalServiceLocator.current call

### core/ai/AiRepository.kt
PURPOSE: Compatibility facade over the local `LifeOSIntelligenceEngine` for existing UI call sites
DEPENDENCIES: `core/intelligence/`
WHAT BREAKS IF MODIFIED: Changes can affect note actions, diary drafting, reports and Assistant responses

### core/ai/AiModels.kt
PURPOSE: NoteAiAction enum (the 12 note actions), ExtractedTask, ChatMessage
WHAT BREAKS IF MODIFIED: Adding/removing a NoteAiAction enum value automatically changes the AI actions dropdown in NoteEditorScreen.kt


### core/reminders/ReminderScheduler.kt and ReminderWorker.kt
PURPOSE: Schedules/fires WorkManager jobs for Task/Habit reminders
WHAT BREAKS IF MODIFIED: Changing the unique work name pattern could cause duplicate reminders instead of replacing old ones

### core/util/SettingsStore.kt
PURPOSE: All persisted app settings (App Lock type, onboarding flag, dark theme, AI features toggle and other existing preferences) via DataStore Preferences
WHAT BREAKS IF MODIFIED: Renaming a Preferences.Key string silently loses previously saved values for existing users

### core/util/DateTimeUtils.kt, IdGenerator.kt, MediaStorage.kt, PermissionManager.kt, NotificationHelper.kt
PURPOSE: Small, focused utilities used throughout the app — date/time conversions, UUID generation, app-private file paths for captures, runtime permission helpers, and notification posting
WHAT BREAKS IF MODIFIED: IdGenerator changes would affect primary key generation for every new database row

---

## data/ — persistence layer

### data/db/AppDatabase.kt
PURPOSE: The Room database definition — lists all 7 entities, current schema version
WHAT BREAKS IF MODIFIED: See docs/05_DATABASE.md and docs/15_TROUBLESHOOTING.md — schema changes without a Migration crash the app for existing users

### data/db/entities/*.kt
PURPOSE: One file per database table's row shape (NoteEntity, TaskEntity, HabitEntity+HabitCompletionEntity, ExpenseEntity, DiaryEntity, CaptureEntity)
WHAT BREAKS IF MODIFIED: Any field change requires a matching Room migration

### data/db/dao/*.kt
PURPOSE: One interface per entity defining every SQL query used by the app
WHAT BREAKS IF MODIFIED: Query changes directly affect what data repositories/screens see

### data/repository/*.kt
PURPOSE: The "business logic" layer — one repository per feature, plus BackupRepository. Validation, ID generation, and orchestration (e.g. habit streak math) lives here.
WHAT BREAKS IF MODIFIED: Screens never talk to DAOs directly — changing a repository method's signature requires updating every ViewModel that calls it

---

## domain/ — pure business logic (no Android framework dependencies)

### domain/model/*.kt
PURPOSE: NoteBlock (rich-text block types), TimelineItem, Categories.kt (expense categories + habit analytics/heatmap models)
WHAT BREAKS IF MODIFIED: NoteBlock changes require care since it's serialized to JSON and stored in the database (NoteEntity.contentJson) — old notes must still deserialize correctly

### domain/usecase/BuildTimelineUseCase.kt
PURPOSE: Aggregates Notes/Tasks/Habits/Expenses/Diary/Captures into the unified Timeline for a given day
WHAT BREAKS IF MODIFIED: This is the concrete implementation of the app's "everything is connected" principle

### domain/usecase/GetHomeSummaryUseCase.kt
PURPOSE: Combines live task/habit/expense data into the Home dashboard's summary
WHAT BREAKS IF MODIFIED: Directly drives what HomeScreen.kt displays

---

## ui/ — screens and ViewModels

Each subfolder (home/, notes/, tasks/, habits/, expenses/, diary/,
timeline/, capture/, insights/, search/, ai/, settings/, onboarding/)
contains one or more *Screen.kt Composable files and their paired
ViewModel classes (sometimes in the same file, e.g. TasksScreen.kt
contains TasksViewModel).

### ui/navigation/Screen.kt and LifeOSNavHost.kt
PURPOSE: Route definitions and the single NavHost wiring every screen together
WHAT BREAKS IF MODIFIED: Adding a new screen requires a matching entry in both files, or it will be unreachable

### ui/theme/*.kt
PURPOSE: The design system — colors, typography, shapes, the LifeOSTheme() composable
WHAT BREAKS IF MODIFIED: Affects the visual appearance of the entire app

### ui/components/GlassCard.kt, LifeOSBottomBar.kt, LifeOSDesignComponents.kt, ReminderTimePickerDialog.kt
PURPOSE: Shared, reusable UI pieces used across multiple screens, including the 2026 LifeOS design primitives
WHAT BREAKS IF MODIFIED: Changes ripple across every screen that uses these components

---

## .github/workflows/

### .github/workflows/android-build.yml
PURPOSE: GitHub Actions CI — builds a debug APK on every push/PR to main. Works around the missing gradlew wrapper (see docs/16_KNOWN_ISSUES.md Issue #1) by installing Gradle 8.9 directly and running `gradle assembleDebug` instead of `./gradlew assembleDebug`.
WHAT BREAKS IF MODIFIED: If gradlew is ever committed, this file should be updated to use `./gradlew` instead of `gradle` (noted in a comment inside the file itself). Changing the pinned Gradle version here without also updating gradle/wrapper/gradle-wrapper.properties can cause CI and local builds to use different Gradle versions.

---

## docs/

PURPOSE: This documentation set. See docs/19_DEVELOPER_HANDOVER.md's
"Documentation maintenance rule" — keep these in sync with the code.

## README.md (project root)

PURPOSE: Quick-start build/run instructions and a feature status table, written earlier in this project's development
NOTE: /docs is now the authoritative, detailed documentation set; README.md should be treated as a short pointer to it going forward.

## Current-state addendum — 2026-09-16

This document remains part of the LifeOS documentation set. Current UI/UX, motion, responsive and accessibility rules are centralized in [`DESIGN.md`](./DESIGN.md). The current Intelligence implementation is local/offline and requires no external AI provider or API key. Build/test statements are only considered verified when the exact command has been executed in a real Android/Gradle environment.

## Current implementation snapshot — 2026-09-16

New shared presentation code includes ui/components/LifeOSTopBar.kt; the updated primary screen and capture implementations remain in their existing feature packages.


## Current UI additions — 2026-09-16

- `ui/home/HomeScreen.kt` — Stitch-inspired Today dashboard and top-right actions.
- `ui/profile/ProfileScreen.kt` — local Profile destination opened from Home.
- `ui/security/AppLockScreen.kt` and `AppLockSetupScreen.kt` — PIN-only lock/recovery flow.
- Removed `core/security/AppLockManager.kt` after biometric support was retired.

## 2026-09-18 affected areas
- `ui/profile/ProfileScreen.kt` — profile photo and profile actions.
- `ui/expenses/ExpensesScreen.kt` — expenses dashboard and add-expense sheet.
- `ui/security/AppLockScreen.kt` — keypad-oriented lock UI.
- `ui/navigation/LifeOSNavHost.kt` — navigation Back handling.
- `MainActivity.kt` — onboarding startup gating.
- `core/util/SettingsStore.kt` — local profile-photo URI preference.
- `ui/capture/AudioCaptureScreen.kt` and `CaptureMediaPreview.kt` — audio lifecycle fixes.
