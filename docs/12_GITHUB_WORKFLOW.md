# 12 — GitHub Workflow

## Current state — important

⚠️ **NOT VERIFIED FROM CODEBASE / NOT YET SET UP**: As delivered, this
repository has **no `.git` directory** (no version control has been
initialized yet), **no `.github/` directory** (no GitHub Actions, issue
templates, or PR templates), and no branch history to document. This
section therefore describes:
1. The exact commands needed to get this repository onto GitHub, and
2. A **recommended** workflow for going forward (not a description of an
   existing one, since none exists yet).

## Step 1 — Getting this repository onto GitHub for the first time

```bash
cd LifeOS
git init
git add .
git commit -m "Initial commit: LifeOS Android scaffold"
git branch -M main
git remote add origin <your-repo-url>
git push -u origin main
```

(This is copied from the project's own `README.md`, which already documents
this exact sequence.)

## Recommended branch strategy

⚠️ Recommendation only — not yet enforced by any tooling in this repo
(no branch protection config exists to inspect, since there's no GitHub
repo connected yet).

| Branch | Purpose |
|---|---|
| `main` | Always buildable; represents the current best-known-good state |
| `feature/<short-description>` | One branch per feature or fix, branched from `main` |

## Recommended beginner-friendly workflow

```
Idea
 ↓
Create a GitHub Issue describing it
 ↓
Create a branch: git checkout -b feature/add-note-linking
 ↓
Develop the change
 ↓
Test manually (see docs/14_TESTING.md — no automated tests exist yet)
 ↓
git add . && git commit -m "Add note-to-note linking"
 ↓
git push origin feature/add-note-linking
 ↓
Open a Pull Request on GitHub, targeting main
 ↓
Review (self-review if solo; a teammate's review if not)
 ↓
Merge the Pull Request
 ↓
Tag a release if this completes a meaningful version (see docs/17_CHANGELOG.md)
```

## Commits

⚠️ No commit history exists yet to establish a convention from. **Recommendation**:
adopt [Conventional Commits](https://www.conventionalcommits.org/) style
(`feat:`, `fix:`, `docs:`, `chore:`) from the very first commit, since it
makes `docs/17_CHANGELOG.md` maintainable going forward.

## Pull Requests

⚠️ No PR template exists (`.github/PULL_REQUEST_TEMPLATE.md` is absent).
**Recommendation**: at minimum, a PR description should state what changed
and which `docs/*.md` files need a corresponding update (see the
Documentation Maintenance Rule at the end of this doc set).

## Releases / Tags

⚠️ No tags exist yet (no git history at all). The app's own
`app/build.gradle.kts` currently declares:
```kotlin
versionCode = 1
versionName = "0.1.0-phase1-6"
```
**Recommendation**: tag releases as `v0.1.0`, `v0.2.0`, etc., matching
`versionName`, and bump both together.

## Issues

⚠️ No issue templates exist. `docs/16_KNOWN_ISSUES.md` in this doc set
should be migrated into real GitHub Issues once the repo is created, so
they're trackable and closeable.

## GitHub Actions / CI/CD

**Now present**: `.github/workflows/android-build.yml` — builds a debug APK
on every push/PR to `main`, and can also be triggered manually
(`workflow_dispatch`).

Because the repository doesn't yet commit the Gradle wrapper scripts
(`gradlew`/`gradlew.bat`/`gradle-wrapper.jar` — see `docs/16_KNOWN_ISSUES.md`
Issue #1), this workflow installs Gradle 8.9 directly via
`gradle/actions/setup-gradle@v4` and runs `gradle assembleDebug` instead of
`./gradlew assembleDebug`. The resulting APK is uploaded as a build
artifact (`lifeos-debug-apk`), downloadable from the Actions run summary.

**What it does NOT do yet**:
- Run tests (`docs/14_TESTING.md` — there are none to run)
- Run a release/signed build (`docs/16_KNOWN_ISSUES.md` Issue #2 — no signing config exists)
- Publish anywhere (Play Store, GitHub Releases)

**Recommended next step once `gradlew` is committed**: simplify the
workflow's build step from `gradle assembleDebug` to `./gradlew assembleDebug`,
and remove the explicit `setup-gradle` version pin in favor of letting the
wrapper resolve its own version automatically.

## Current-state addendum — 2026-09-16

This document remains part of the LifeOS documentation set. Current UI/UX, motion, responsive and accessibility rules are centralized in [`DESIGN.md`](./DESIGN.md). The current Intelligence implementation is local/offline and requires no external AI provider or API key. Build/test statements are only considered verified when the exact command has been executed in a real Android/Gradle environment.

## Current implementation snapshot — 2026-09-16

No CI workflow was changed by the UI pass. The supplied source snapshot still lacks the Gradle wrapper, so build verification must be performed in a complete Android environment.


---

## Current source snapshot — 2026-09-17

This documentation set is aligned to the supplied LifeOS Android source snapshot. The source of truth is the Kotlin/Jetpack Compose implementation under `app/src/main/java/com/lifeos/app/`, together with `app/build.gradle.kts` and `app/src/main/AndroidManifest.xml`.

### Verified architecture facts
- Native Kotlin Android application using Jetpack Compose + Material 3.
- Navigation uses Navigation Compose with a `root_tabs` nested graph for Home, Tasks, Habits and Insights; secondary screens remain stackable routes.
- Local persistence uses Room/SQLite with SQLCipher for database-at-rest encryption, plus DataStore for preferences/settings.
- Repositories and use cases remain the application data boundary; UI does not directly own Room access.
- Offline intelligence is implemented under `core/intelligence/` using deterministic local analyzers, rules, lexicons, statistics and templates.
- Capture uses CameraX for photo/video and Android `MediaRecorder` for audio, with runtime permissions requested only when capture is selected.
- Backup/restore is local and uses Android Storage Access Framework/document picker flows.
- App Lock is PIN-only (`NONE` / `PIN`) in the current source; no cloud identity or biometric App Lock implementation is present.
- The manifest does not declare `android.permission.INTERNET`.

### Verification boundary
The repository snapshot supplied for this documentation pass does not contain the Gradle wrapper scripts/JAR. Therefore this environment does not claim a fresh Gradle build, instrumentation run, or physical-device verification unless an executed command is recorded elsewhere in the project history.

### Maintenance rule
Historical sections are intentionally retained for traceability. When historical documentation conflicts with the current source, the current source and the latest dated current-state section take precedence; historical changelog entries should not be rewritten merely to make history appear current.
