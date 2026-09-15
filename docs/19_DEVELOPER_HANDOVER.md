# 19 — Developer Handover

*Written as if the original developer is gone and you're a senior Android
developer picking this project up cold.*

## What this project is, in one paragraph

LifeOS is a native Android app (Kotlin + Jetpack Compose) that combines
Notes, Tasks, Habits, Expenses, Diary, and a unified Timeline into one
local-first, offline-capable personal life-management tool, with an
optional, user-controlled AI layer (Anthropic API) for note actions, task
extraction, diary drafting, and a chat assistant. There is no backend, no
user accounts, and no cloud sync — everything lives in a local Room/SQLite
database on the user's device.

## First 60 minutes for a new developer

- [ ] 0-5 min: Read this document fully before touching code.
- [ ] 5-10 min: Read docs/00_PROJECT_OVERVIEW.md and docs/02_ARCHITECTURE.md
      for the big picture.
- [ ] 10-15 min: git init if not already done, get the repo pushed to
      GitHub (docs/12_GITHUB_WORKFLOW.md), or clone from wherever it now lives.
- [ ] 15-25 min: Open the project in Android Studio, let Gradle Sync
      run. If ./gradlew is missing, see docs/15_TROUBLESHOOTING.md /
      docs/16_KNOWN_ISSUES.md Issue #1 — this is a known, expected gap,
      not something you broke.
- [ ] 25-35 min: Skim docs/21_FILE_STRUCTURE.md to know where things live.
- [ ] 35-45 min: Run the app on an emulator or device (once the build
      succeeds). Click through Home, Notes, Tasks, Habits, Timeline,
      Settings to get a feel for it.
- [ ] 45-55 min: Read docs/05_DATABASE.md and open
      data/db/AppDatabase.kt plus one entity (e.g. TaskEntity.kt) side by side.
- [ ] 55-60 min: Read docs/16_KNOWN_ISSUES.md in full so you don't
      accidentally "rediscover" and re-report something already known.

## How to run it

See docs/13_DEPLOYMENT.md for full detail. Short version:
```bash
git clone <repo-url>
cd LifeOS
# Open in Android Studio, Gradle Sync, Run
```

## Repository structure

Full detail in docs/21_FILE_STRUCTURE.md. The one-sentence version:
`ui/` = screens+ViewModels, `domain/` = pure business logic, `data/` =
Room database + repositories, `core/` = cross-cutting infrastructure (DI,
AI client, security, reminders, utilities).

## Architecture

Full detail in docs/02_ARCHITECTURE.md. Key thing to internalize: this is
a single Android module, not a multi-module or multi-repo project.
Dependency injection is a hand-written ServiceLocator
(core/di/ServiceLocator.kt), not Hilt/Dagger — this was a deliberate
choice, documented in docs/24_ARCHITECTURAL_DECISIONS.md (ADR-002). Don't
"fix" this by introducing Hilt without discussing it first — it's not a bug.

## Environment setup

No .env file, no build-time secrets. See docs/22_ENVIRONMENT_VARIABLES.md —
there is genuinely nothing to configure to get a build running locally
besides Android Studio + SDK 35 + JDK 17.

## Database

Room/SQLite, 7 tables, schema version 1, no migrations exist yet. If you
change any @Entity class's fields, you must bump the @Database(version = ...)
number in AppDatabase.kt and write a Migration object, or the app will
crash for existing users on upgrade. See docs/05_DATABASE.md and
docs/15_TROUBLESHOOTING.md.

## APIs

Exactly one external API call exists in the whole app: Anthropic's Messages
API, made from core/ai/AiClient.kt, called only via core/ai/AiRepository.kt.
Full contract in docs/06_API_DOCUMENTATION.md.

## Authentication

There isn't any. Just an optional device-level biometric/PIN "App Lock." See
docs/07_AUTHENTICATION.md. Don't assume a User concept exists anywhere —
it doesn't.

## Deployment

Not yet set up for real release — no signing config, no CI/CD, no Play
Console integration. See docs/13_DEPLOYMENT.md.

## Git workflow

No git history exists yet in the delivered repo. See docs/12_GITHUB_WORKFLOW.md
for how to initialize it and a recommended branch/PR flow going forward.

## Current known issues and technical debt

Full, current list: docs/16_KNOWN_ISSUES.md. The highest-priority items
for a new developer to be aware of immediately:

1. Missing Gradle wrapper scripts (blocks CLI builds)
2. No release signing config (blocks real releases)
3. Database and AI API key are not encrypted at rest — treat any
   real user testing as happening on data you're comfortable being
   readable if the device/emulator storage were inspected directly
4. Backup restore exists in code but has no UI button — don't assume
   it's reachable by users today
5. Zero automated tests exist despite test frameworks being declared as dependencies

## Future work

See docs/18_ROADMAP.md — organized as NOW / NEXT / LATER / FUTURE.

## Security warnings

- Do not commit a real Anthropic API key anywhere in source control —
  the app is specifically designed so the key is a runtime, user-entered
  value, never a build-time secret. If you ever add a "default" or
  "developer" key for testing convenience, make absolutely sure it's kept
  out of version control — none of that plumbing exists yet, so you'd be
  adding it fresh.
- Do not enable fallbackToDestructiveMigration() on the Room database
  builder in a release build — it silently wipes all user data on any
  schema mismatch. See docs/15_TROUBLESHOOTING.md.
- Treat the findings in docs/08_SECURITY.md (especially the two HIGH
  ones) as pre-existing, known gaps — not something to silently work around
  or ignore in future features that touch sensitive data.

## Things that must NOT be changed casually

- MainActivity must remain a FragmentActivity (not ComponentActivity) —
  AppLockManager's use of BiometricPrompt depends on this. Changing the
  base class will silently break App Lock.
- ServiceLocator as the single composition root — every repository,
  the database instance, and the AI client are constructed exactly once
  here. Don't instantiate a second AppDatabase or a second AiClient
  elsewhere; always go through LocalServiceLocator.current.
- The Room @Database(version = 1) number and entity field sets — never
  change these without a corresponding Migration, per the Database
  section above.
- android:allowBackup="false" in AndroidManifest.xml — this is a
  deliberate privacy control (see docs/08_SECURITY.md), not an oversight.
  Don't flip it to true without understanding you're re-enabling OS auto
  cloud backup of the local database.
- The pattern of AI writes requiring explicit user approval (extracted
  tasks needing a "CREATE TASKS" tap; AI diary drafts needing "Approve") —
  this is a deliberate product/trust decision baked throughout the AI
  screens, not incidental UX. Don't "streamline" it into a silent auto-save
  without recognizing you're reversing a stated design principle.

## Documentation maintenance rule

Whenever a feature, API, database structure, dependency, architecture,
deployment process, or security mechanism changes, the relevant file(s) in
/docs must be updated in the same PR. Documentation that silently drifts
from the code is worse than no documentation, because it's actively misleading.
