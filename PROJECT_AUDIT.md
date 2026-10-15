# PROJECT_AUDIT.md — LifeOS

**Audit date:** 2026-10-13
**Auditor:** principal Android engineering pass
**Working clone:** `/home/Life2030-`
**Branch:** `redesign/from-zero-2026` (HEAD `90a142b`, clean working tree)
**Remote:** `https://github.com/gamerprash99-cell/Life2030-.git`

This audit describes the repository exactly as inspected with shell commands on the
date above. Nothing here is inferred from documentation alone; where a claim is
unverified it is labelled as such.

---

## 1. What this repository is

A **single-module native Android application** (`:app`, namespace `com.lifeos.app`)
implementing **LifeOS** — the private, offline-first personal life manager described
in `life os .txt` (the master product specification, present at repo root and at
`/storage/emulated/0/life os/life os .txt`).

It is **not** an empty starter. It is a large, mature, actively-developed Compose
app on a fresh "from zero" redesign branch.

| Metric | Value |
|---|---|
| Kotlin sources (main) | 194 |
| Kotlin sources (unit test) | 39 |
| Kotlin sources (androidTest) | 2 |
| Room schema version | 6 |
| `minSdk` / `targetSdk` / `compileSdk` | 26 / 35 / 35 |
| `versionCode` / `versionName` | 2 / 0.2.0 |
| Last commit | `90a142b` fix(ui): read colour from the theme, never from the static brand tokens |
| Debug APK present | `app/build/outputs/apk/debug/app-debug.apk` (46 MB, 2026-10-13 19:04) |

---

## 2. Toolchain available in this environment

The environment is an **Android/Termux-style root shell**, not a desktop CI box.
Java is *not* on the default `PATH`; a pinned JDK and SDK are provided under
`/home/tools`.

| Component | Location | Version |
|---|---|---|
| JDK | `/home/tools/amazon-corretto-17.0.20.12.1-linux-aarch64` | Corretto 17 |
| Android SDK | `/home/tools/android-sdk` | platform 35, build-tools 34+35 |
| Gradle | `/home/tools/gradle-8.9/bin/gradle` | 8.9 (matches wrapper) |
| Kotlin | via AGP KGP | 2.0.21 |
| AGP | via version catalogue in root `build.gradle.kts` | 8.6.1 |
| `aapt2` override | `/home/tools/aapt2` | required on aarch64 |

Build helper (already present, reusable):
- `/home/tools/lifeos-build-home.sh` → builds `/home/Life2030-` with
  `GRADLE_USER_HOME=/home/tools/gradle-home`.
- `/home/tools/lifeos-build.sh` → same, but targets the *other* clone at
  `/storage/emulated/0/AndroidIDEProjects/Life2030-`.

**Two clones exist.** `/home/Life2030-` is the one with the newest work
(`redesign/from-zero-2026`). `/storage/emulated/0/AndroidIDEProjects/Life2030-`
sits on a different, older state — do not confuse them.

---

## 3. Architecture as implemented

Single Gradle module, cleanly layered by package under
`app/src/main/java/com/lifeos/app/`:

```
core/        ai, di, intelligence, life, location, media, reminders,
             security, transactions, util
data/        db (Room: entities, dao, Converters, AppDatabase v6),
             repository
domain/      intelligence, model, usecase
ui/          theme, components, navigation, onboarding, home, notes, tasks,
             habits, expenses, diary, capture, snap, timeline, life, search,
             ai, profile, settings, security
widget/      app widget
```

- **UI:** Jetpack Compose, Material 3, one Activity, Navigation Compose.
- **Persistence:** Room **2.6.1** with **6** exported schema versions committed
  under `app/schemas/` (consumed by `AppDatabaseSchemaTest` and
  `MigrationTestHelper`).
- **Encryption at rest:** SQLCipher (`net.zetetic:sqlcipher-android:4.6.1`); a
  random per-install passphrase wrapped by an Android Keystore AES-GCM key
  (`core/security/DatabasePassphraseProvider`).
- **Settings:** DataStore Preferences.
- **Background:** WorkManager for reminders (`core/reminders`).
- **Security:** PIN app-lock (`PinHasher`, `LockoutPolicy`); biometric dependency
  is currently declared but biometric unlock was removed (known issue #13).
- **Media:** CameraX in-app camera (`ui/snap`), Coil for image loading.
- **AI:** an **on-device** intelligence engine (`core/intelligence`,
  `core/ai/runtime`) — no cloud provider, no embedded API key. This is a
  deliberate local-first choice.

### Design language
The current branch rebuilt the UI on **"Lumen v5"** (`design.md`): warm paper
canvas, matte surfaces (no blur/sheen), clay/sage/amber palette, bundled
**Fraunces** (headings) + **Manrope** (UI) + **Kalam** (diary hand). Tokens live
in `ui/theme/*` and are consumed through `MaterialTheme.colorScheme.*`,
`LocalGlassColors`, `LocalAmbientGlowColors`, `LocalLifeOSSemanticColors`.

---

## 4. Feature coverage vs. the specification

Legend: **Implemented** · **Partial** · **Missing**

| Spec area | State | Notes |
|---|---|---|
| Home dashboard | Implemented | Real DB-backed progress, tasks, habits, spend, weekly rhythm, AI entry |
| Notes (create/edit/organise/rich text) | Implemented | `ui/notes`, folders, pin/favourite/archive/trash, editor",
| Task management (+recurrence, reminders) | Implemented | `RepeatRuleCalculator` tested; WorkManager scheduling |
| Habit tracker + analytics + heatmap | Implemented | `HabitStatsCalculator` tested |
| Expense tracker (exact money) | Implemented | Integer minor units; category breakdown |
| Diary + mood + media | Implemented | Largest feature area (24 files) |
| Life Capture (photo/video/audio/thought/mood/location) | Implemented | `ui/capture`, `ui/snap` |
| Life Timeline | Implemented | `domain/usecase/BuildTimelineUseCase`, `TimelineItem` projection |
| Life Calendar | Implemented | `diary/calendar` + day view |
| Global search | Implemented | `ui/search` |
| Smart tags | Implemented | relations + tag UI |
| Insights / reviews / reports | **Partial** | The **Insights tab was removed**; some analytics live on Life hub + AI |
| LifeOS AI (assistant, context permissions, consent) | **Partial** | On-device engine exists; context wiring and consent levels need verification against spec §30–§32 |
| Security (lock, PIN, encryption) | Implemented | PIN lock + SQLCipher; biometric removed |
| Backup / Restore / Export / Import | Implemented | JSON archive, transactional restore; round-trip not device-verified |
| Notifications / daily & weekly review | **Partial** | Reminders exist; review flows to be confirmed |
| Trash / recovery | Implemented | per-entity trash + restore |

---

## 5. Verified build/test evidence

*(This section is filled by the verification run started during this audit. Values
are written only after the command completes.)*

- `:app:testDebugUnitTest` — see §5.1 below.
- `:app:assembleDebug` — see §5.1 below.

### 5.1 Baseline verification command
```
/home/tools/lifeos-build-home.sh :app:testDebugUnitTest :app:assembleDebug
```
Result recorded in `TESTING.md` §Baseline.

Existing committed schema files: `app/schemas/com.lifeos.app.data.db.AppDatabase/6.json`.

---

## 6. Known constraints & risks

1. **No `gradlew` wrapper scripts in git** for local use (only
   `gradle/wrapper/gradle-wrapper.properties`). CI and this environment use a
   system Gradle 8.9 instead. Should be fixed for reproducibility.
2. **Stale planning artifacts.** `tasks/plan.md` and `tasks/todo.md` describe a
   *different* branch that removed Notes/Capture/AI/Insights — they do **not**
   describe this branch. They must be rewritten or deleted to avoid confusion.
3. **Design-vs-spec divergence.** The spec (§4) requires a five-section bar of
   Home · Notes · ＋Capture · **Insights** · Life. The current build ships
   Home · Notes · ＋Capture · Life · **You**. This is a material product decision
   needing owner sign-off (see IMPLEMENTATION_PLAN.md §Decisions).
4. **Biometric dependency** (`androidx.biometric`) is declared but unused — dead
   weight to remove or re-wire.
5. **Instrumentation test surface is thin** (2 files) and cannot run here without
   an emulator/device.
6. **No Figma file is linked** in the repository; the Figma MCP configured in this
   environment is **read-only** (Framelink). Design-token *push* would require a
   file key + a token with `file_variables:write`.
7. **Release signing** has scaffolding (`keystore.properties.example`); no real
   keystore is committed (correct).

---

## 7. Audit conclusion

LifeOS is a **real, substantially-built application** with genuine persistence,
security, tests and a deliberate design system — far past a prototype. The work
remaining is not "build the app" but:

1. Reconcile **product-spec deviations** (Insights, AI consent/context levels,
   reviews) with the owner.
2. Close **verification gaps** (device/instrumentation, backup round-trip,
   migration execution, accessibility gate).
3. **Harden and polish** to a release candidate (wrapper, dead deps, docs,
   release signing guidance).
4. Apply a **Figma-led visual pass** on top of the Lumen v5 system.

Priority order and acceptance criteria for those items live in
`IMPLEMENTATION_PLAN.md`.
