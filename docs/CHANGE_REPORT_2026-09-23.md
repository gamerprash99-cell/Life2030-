# LifeOS Change Report — 2026-09-23

Branch: `feat/remove-notes-capture-life-intelligence` (batch 2)
Scope: production-fix batch on top of the Notes/Capture/LIFE AI removal —
**reliable reminders**, **startup unlock gate**, **Back-hardening**.

Readiness is reported from verification that actually ran in this environment
(builds, JVM tests, exported Room schema, lint). Anything on-device is listed
under **Not verified here** with the exact sandbox blocker.

---

## What was fixed

### 1. Task & habit reminders no longer depend on WorkManager
- **Root cause:** reminders were WorkManager jobs that Doze / app-standby
  bucketing can defer by minutes or drop entirely while the app is backgrounded.
- **Fix:** a Room v4 `reminders` table is the durable source of truth; each
  enabled row is projected onto an **AlarmManager exact alarm**
  (`setAlarmClock`, Doze-exempt), re-armed after every fire (DAILY / WEEKDAYS)
  or self-disabled (ONCE). Snooze echoes the notification without disturbing the
  next real occurrence. Re-arms happen at boot / package-replaced / time-zone
  changes and at app startup. Alarm UI: full-screen activity + HIGH-importance
  alarm notification (per-notification sound/vibration).
- **Safety:** migration v3→v4 is pure additive; exact-alarm permission lost on
  API 31+ degrades to a Doze-aware inexact alarm instead of dropping the
  reminder; receivers use `goAsync()` and never crash on broadcast errors.
- **Scope removed:** `ReminderWorker` and the WorkManager dependency (nothing
  else used it).

### 2. Startup no longer blocks on the encrypted DB open
- The SQLCipher cold open (native load + Keystore passphrase + PBKDF2) was
  landing on Home's first query after the UI rendered. It now runs on a
  background coroutine gated by `databaseState`; the app shows a lightweight
  static brand splash until the DB is `Ready`, and any key/init failure lands
  on the existing non-destructive recovery screen with a Retry path.

### 3. Settings reachable and destackable
- Settings gained an explicit back arrow + pop-back, and every secondary
  navigation in the nav host is `launchSingleTop`, closing the Profile → Settings
  system-Back gap.

## Verification (actually executed here)
```text
/home/gradle-8.9/bin/gradle :app:clean :app:assembleDebug :app:testDebugUnitTest \
     :app:lintDebug :app:assembleDebugAndroidTest
-> BUILD SUCCESSFUL (~4m23s, 84 tasks)
-> Unit tests: 93 passed, 0 failed / 0 ignored
   (incl. 7 new ReminderScheduleCalculator tests + schema tests against 4.json)
-> lintDebug: 0 errors, 22 warnings (pre-existing category — dependency-version,
   Info autoboxing/Icon/unused-resource, pre-existing LavenderTextField Modifier
   ordering; no new warnings introduced by this batch)
-> APK: app-debug.apk (41.9 MB); androidTest APK builds
-> Schema export: app/schemas/.../AppDatabase/4.json checked in
```
Greenhouse gates were re-run from a clean tree after the final commit-relevant
edits (string resources + schema/calculator test fixes).

## Not run (environment blocker, stated verbatim)
On-device/emulator execution is blocked in this sandbox:
`pm install/uninstall`, `am start`, `input`, `settings put`, `dumpsys <target>` all
throw `SecurityException: Permission Denial: ... requires <permission> that the
User 0 ... and the shell ... do not have.` Only read-only queries
(`pm list packages`, `cmd package list packages`) succeed; there is no emulator
and no physical device.

Consequently these could not be executed here and remain as device checklist
items:
- cold-start timing measurement (Problem 2 regression test on a real device);
- v3→v4 migration against a real SQLCipher file (`connectedDebugAndroidTest`
  `AppDatabaseMigrationTest` both cases);
- an actual alarm firing end-to-end, including Doze, reboot, time-zone change,
  and exact-alarm-permission-revoked fallback;
- full-screen alarm UI on a locked screen and snooze re-echo;
- the Settings "Allow exact alarms" system flow.

These paths were verified at compile / JVM-test / lint / schema level and by
code reasoning only.

## Files touched (summary)
- New: reminders pipeline (`ReminderScheduleCalculator`, `ReminderScheduler`,
  `AlarmReceiver`, `BootReceiver`, `AlarmFullScreenActivity`,
  `ReminderRepository`, `ReminderDao`, `ReminderEntity`), `ReminderRepeatSelector`,
  `app/schemas/.../4.json`.
- Modified: `AppDatabase` (v4 + `MIGRATION_3_4`), `ServiceLocator`, `TaskRepository`,
  `HabitRepository`, `BackupRepository`, `LifeOSApplication`, `MainActivity`,
  `NotificationHelper`, `SettingsStore`, `LifeOSNavHost`, `SettingsScreen`,
  `TasksScreen`, `HabitsScreen`, `HabitDetailScreen`, `AndroidManifest`,
  `strings.xml`, `build.gradle.kts`, schema + migration tests.
- Deleted: `ReminderWorker.kt`.