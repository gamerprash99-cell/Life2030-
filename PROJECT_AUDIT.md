# LifeOS — Project Audit

Status of this document: **honest snapshot of what exists, what is verified, and
what is not.** It is written to be falsifiable. If a claim here is not backed by a
command that can be re-run in this environment, it is marked as *written, not
verified*.

Date of audit: 2026-10-20.

## 1. What this project is

LifeOS is a private, local-first Android app for managing personal life data:
notes, tasks, habits, expenses, diary, captures (photo/video/audio/thought/mood/
location), a timeline of real events, insights, search, trash/restore, settings,
and an on-device, consent-gated "LifeOS AI".

It is a **fresh, from-scratch implementation** built for this repository. No code
from any previous LifeOS build was copied; only the product specification text was
read as the source of truth.

- Source specification: `life os .txt` (2321 lines).
- Repository: `https://github.com/gamerprash99-cell/Life2030-`.

## 2. Environment actually available

| Item | Value | Verified by |
| --- | --- | --- |
| JDK | 17 | `java -version` |
| Android SDK | `/home/android-sdk` (platform 35, build-tools 34/35) | directory listing |
| Gradle | 8.9 (wrapper + direct binary) | wrapper generation |
| Emulator | **none** | `adb` has no devices |
| Network | available | dependency resolution |

**Consequence:** the only executable quality gates in this environment are
**JVM unit tests**, **Kotlin compilation**, **KSP/Room processing**, and
**Android lint**. Instrumented (`androidTest`) tests and Room migration tests are
**declared in the source tree but have never been executed here** because there is
no device or emulator. No instrumentation result is claimed anywhere.

## 3. Verified vs. written

### 3.1 Verified in this environment

- `:app:compileDebugKotlin --offline` — see `TESTING.md` for the exact command and
  the recorded outcome.
- `:app:testDebugUnitTest` — JVM unit tests for the domain and AI layers.
- `:app:lintDebug` — Android lint.

Anything not in the list above, or not reproduced by a command in `TESTING.md`,
must be treated as *written but unverified*.

### 3.2 Written but not verified (no emulator)

| Area | Files | Why unverified |
| --- | --- | --- |
| UI rendering / navigation | all `feature/**/*Screen.kt`, `ui/LifeOsApp.kt` | needs a device to run Compose |
| Persistence end-to-end | `core/database/**`, `core/data/**` | Room runtime needs a device |
| Camera / microphone / location capture | `feature/capture/**` | needs real hardware + runtime permissions |
| Share / export sheet | `feature/settings/SettingsScreen.kt` | needs a device to show a chooser |
| Notifications | settings toggle only | worker not yet implemented (see §6) |

## 4. Component inventory

Counts below are of files present in `app/src/main` at the time of audit.

- **Design system** — `core/designsystem/theme/`: `Color.kt`, `Theme.kt`,
  `Tokens.kt`, `Type.kt`. Uses the exact colour palette supplied in the spec
  (dark + light), with documented contrast ratios.
- **Database** — `core/database/`: `Entities.kt` (12 entities), DAOs
  (`NoteDao`, `TrackingDaos`, `CaptureDaos`, `SearchDao`), `LifeOsDatabase` (v1),
  converters.
- **Domain** — `core/domain/`: `Dates`, `Ids`, `Money`, `Recurrence`,
  `HabitProgress`, `Tags`, `Model`.
- **Data** — `core/data/`: repositories + `AppContainer` (manual DI).
- **AI** — `core/ai/`: `AiProvider` interface, `AiConsent` levels,
  `LocalRuleBasedProvider` (offline, deterministic).
- **Backup** — `core/backup/`: `DataExporter` (dependency-free, versioned JSON +
  text).
- **Features** — `feature/`: home, notes, capture, tracking (tasks/habits/expenses/
  diary), insights, life (timeline/calendar/trash), search, settings.

## 5. Data model

12 Room entities: `folders`, `tags`, `entity_tags`, `notes`, `tasks`, `habits`,
`habit_completions`, `expenses`, `diary_entries`, `captures`, `attachments`,
`life_events`.

Deliberate invariants:

- **Money is never a floating-point number.** `expenses.amountMinor` is a `Long`
  in exact minor units plus an ISO `currencyCode`.
- **Trash is soft delete.** Every user-visible record has a `deletedAt` column;
  `0` means live, non-zero is the deletion timestamp. Permanent delete is a
  separate, explicit action.
- **No seeded data.** There is no fake or demo content anywhere; the timeline is
  populated only by real user actions.
- **No destructive migration.** `fallbackToDestructiveMigration` is intentionally
  not enabled, so an unhandled schema change crashes loudly rather than silently
  erasing the user's data.

## 6. Known gaps (explicit, not hidden)

1. **`core/media` is not implemented.** Capture records and attachment rows exist
   and persist, but the code that writes media files to app-private storage is not
   present yet.
2. **`core/notifications` is not implemented.** The Settings toggle stores a
   preference; there is no `WorkManager` worker scheduling reminders yet.
3. **No onboarding flow.** The app opens directly into Home.
4. **AI is local and rule-based only.** There is no remote provider, by design
   (no embedded API keys — see `SECURITY.md`).
5. **Search uses `LIKE`, not FTS.** Correct and private, but not indexed; FTS4 is
   a documented follow-up.

## 7. Figma

The specification asked to "use figma to design new screen". The Figma integration
available in this environment is **read-only** (`get_figma_data`,
`download_figma_images`) and **no Figma file key was provided**, so no Figma file
could be created or edited. Designs were therefore authored directly as Compose
design tokens (`core/designsystem/theme/`) implementing the exact palette from the
spec. This limitation is stated plainly rather than implied to be met.

## 8. Honesty statement

Where this project claims success, the claim is tied to a command and its output
in `TESTING.md`. Where a capability is written but cannot run here, this document
says so. No test result, screenshot, or benchmark is invented.
