# 13 — Deployment

## Important calibration before anything else

⚠️ **This app has never been built or run in the environment where this
documentation was authored** — there was no Android SDK, no emulator, and
no internet access to download Gradle/Maven dependencies available in that
environment. Every command below is the *correct, standard* command for a
project structured this way, but none have been executed end-to-end against
this exact codebase. Treat your first `Gradle Sync` in Android Studio as the
real verification step, and consult `docs/16_KNOWN_ISSUES.md` for one known
gap (the missing `gradlew` wrapper script) before you start.

## Required software (development setup)

| Tool | Why |
|---|---|
| Android Studio (Ladybug/2024.2 or newer recommended) | Includes Android SDK management, emulator, and Gradle integration |
| Android SDK Platform 35 | Matches `compileSdk = 35` / `targetSdk = 35` in `app/build.gradle.kts` |
| JDK 17 | Matches `sourceCompatibility`/`targetCompatibility`/`kotlinOptions.jvmTarget` in `app/build.gradle.kts` |

## Installation

```bash
git clone <your-repo-url>
cd LifeOS
```
Then open the `LifeOS` folder in Android Studio and let it run Gradle Sync.

### ⚠️ Known gap: missing Gradle wrapper scripts

The repository includes `gradle/wrapper/gradle-wrapper.properties`
(specifying Gradle 8.9) but **not** the `gradlew` / `gradlew.bat` wrapper
scripts or `gradle-wrapper.jar`. Android Studio can usually regenerate these
automatically on first open/sync. If building from the command line instead
of Android Studio, first run (from a machine with Gradle installed globally):
```bash
gradle wrapper --gradle-version 8.9
```
This regenerates the missing `gradlew` files. See `docs/16_KNOWN_ISSUES.md`
(Issue #1) for full detail.

## Environment variables

**None required to build.** See `docs/22_ENVIRONMENT_VARIABLES.md` — the
only "secret" the app uses (the AI API key) is entered by the end user
inside the running app, not supplied at build time.

## Build commands

Once `gradlew` is present (see above):

```bash
# Debug build (unsigned, for testing on a device/emulator)
./gradlew assembleDebug

# Release build (minified/proguarded, per app/build.gradle.kts's release buildType)
./gradlew assembleRelease

# Install debug build directly to a connected device/emulator
./gradlew installDebug
```

Output APKs land in `app/build/outputs/apk/debug/` or `app/build/outputs/apk/release/`.

## Signing for release

⚠️ **NOT PRESENT / NOT VERIFIED**: no `signingConfigs` block exists in
`app/build.gradle.kts`, and no keystore file exists in the repository. The
`release` build type currently has no signing configuration attached —
running `assembleRelease` today will produce an **unsigned** APK, which
cannot be installed on most devices or uploaded to the Play Store as-is.
**This must be set up before any real release.**

## Testing before release

⚠️ No automated tests exist to run (see `docs/14_TESTING.md`). Manual
testing on a device/emulator is currently the only verification path.

## Android build (this app IS the Android build — no separate step)

There is no separate "web" or "backend" deployment — the entire product is
the Android APK/AAB described above.

## Web deployment

**Not applicable.** No web frontend exists in this repository.

## Backend deployment

**Not applicable.** No backend exists in this repository (see `docs/10_BACKEND.md`).

## Publishing to the Google Play Store

⚠️ **NOT VERIFIED / NOT SET UP**: No Play Console configuration, no
`play-publisher` Gradle plugin, no service account JSON, and no store
listing metadata (screenshots, descriptions) exist in this repository. This
is a manual future step: build a signed release AAB
(`./gradlew bundleRelease`), create a Play Console listing, and upload it.

## Release process (recommended, since none exists yet)

1. Confirm `versionCode`/`versionName` in `app/build.gradle.kts` are bumped
2. `./gradlew assembleRelease` (once signing is configured)
3. Manually smoke-test the signed APK on a real device
4. Tag the commit in git (see `docs/12_GITHUB_WORKFLOW.md`)
5. Attach the APK/AAB to a GitHub Release, or upload to Play Console

## Rollback procedure

⚠️ **NOT VERIFIED / NOT SET UP** — since there is no CI/CD and no
production deployment history, there is no established rollback procedure.
For a local-first app with no backend, "rollback" primarily means: keep the
previous release's signed APK/AAB available, and be able to re-publish it
to the Play Store if a new version has a critical bug (Play Store supports
staged rollouts and halting a rollout, which is the standard mitigation —
but no rollout has occurred yet to reference).

## Current-state addendum — 2026-09-16

This document remains part of the LifeOS documentation set. Current UI/UX, motion, responsive and accessibility rules are centralized in [`DESIGN.md`](./DESIGN.md). The current Intelligence implementation is local/offline and requires no external AI provider or API key. Build/test statements are only considered verified when the exact command has been executed in a real Android/Gradle environment.

## Current implementation snapshot — 2026-09-16

The UI update introduces no deployment-time service, API key, backend, analytics SDK or network requirement.


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
