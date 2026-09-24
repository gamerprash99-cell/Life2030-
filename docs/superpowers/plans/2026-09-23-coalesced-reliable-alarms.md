# LifeOS Coalesced, Reliable Background Alarms Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Task/Habit reminders fire reliably *outside the app* (background / locked / Doze), with no artificial limit on the number of reminders, and without multiple simultaneous reminders producing overlapping/stacked alarms. Deliver one intelligent, coalesced alarm per scheduled time, a polished LifeOS-style full-screen alarm, per-group snooze (5/10/15/20/60), and robust re-arming across reboot / app-update / clock / timezone changes.

**Architecture:** Keep Compose Navigation (single NavController, nested `root_tabs` graph), Room v4 (no schema/migration change), manual DI. The state machine still lives in `ReminderEntity → ReminderRepository → ReminderScheduler → AlarmReceiver → AlarmPlaybackService → AlarmFullScreenActivity`. The scheduler becomes **event-based**: one `AlarmManager` alarm per *distinct trigger time* (each carrying a snapshot of every reminder due at that time), instead of one alarm per reminder.

**Tech Stack:** Kotlin, Jetpack Compose, navigation-compose 2.8.4, Room 2.6.1 + SQLCipher, `AlarmManager.setAlarmClock`, foreground service (`foregroundServiceType="mediaPlayback"`), `core-ktx` (`ContextCompat.startForegroundService`, `ServiceCompat.startForeground`).

**Spec:** user request "production audit, root-cause fix, and reliable background alarm implementation (no artificial limits; one intelligent alarm when multiple reminders coincide; STOP + snooze 5/10/15/20/60 with per-type snooze; robust reboot/time-change rescheduling; no data loss; then PR + push)."

## Global Constraints

- Do NOT rewrite architecture, replace Compose Navigation, or touch the Room schema/version/entities/migrations. DB stays v4.
- Do NOT remove or change working features; do NOT add cloud/network/telemetry/AI dependencies; offline-first stays. No INTERNET permission.
- No artificial limits (`take(5)`, hardcoded caps on reminder count/event size).
- Never write the GitHub PAT to disk; ask the user for it before pushing.
- Preserve the existing back-navigation behavior (overlay-aware bottom-bar is a recent verified fix — do not regress).
- `setAlarmClock` when exact access is granted (or API < 31); permission-free `setAndAllowWhileIdle` fallback when denied — never throw.

## Root Causes (now confirmed from code audit + Android official docs)

1. **Doze per-app fire limit kills reminders after ~5–6.** Current `ReminderScheduler` arms each reminder with `setExactAndAllowWhileIdle` (or `setAndAllowWhileIdle`). Android's official Doze docs: the system "will not let an app fire **setExactAndAllowWhileIdle**/`setAndAllowWhileIdle` alarms more than once per nine minutes, per app" while dozing. With one alarm per reminder, a burst of reminders (or several at once) trips this limit and later alarms are dropped/deferred — matching the user-reported "after 5–6 reminders, alarms stop working."
   - **Fix:** coalesce reminders by trigger time into **events** (one alarm per unique time, snapshot in the PendingIntent), and use **`setAlarmClock`** — the one AlarmManager API that is exempt from the 9-minute limit and causes the system to exit Doze before delivery. Number of alarms becomes bounded by distinct trigger times, not reminder count.
2. **Delivery depends on the app being open.** `AlarmReceiver` posts a notification and tries `startActivity`; on Android 10+ a background activity start is restricted, and `USE_FULL_SCREEN_INTENT` is not always permitted — so the ringing full-screen UI only reliably appears while the app is foregrounded. Also the broadcast's `goAsync()` window can expire during a cold SQLCipher DB open.
   - **Fix:** start a foreground **`AlarmPlaybackService`** from the exact-alarm receiver (officially exempt — `setAlarmClock` "will be allowed to start a foreground service even if the app is in the background. Requires SCHEDULE_EXACT_ALARM"). The service holds a partial wakelock, plays the looping ring + vibration, posts a single HIGH-importance alarm notification (InboxStyle, full-screen intent), and launches the full-screen activity. It keeps ringing even if the activity can't be shown. If FGS start is rejected (exact access denied), the receiver falls back to the existing notification-only path. Instant presentation reads the snapshot from intent extras — **no DB read before audio begins**.
3. **Simultaneous reminders produce overlapping, double-beeper alarms.** Each reminder had its own alarm, its own notification, its own full-screen activity.
   - **Fix:** one event per trigger → one receiver invocation, one notification, one full-screen activity showing grouped **TASKS** / **HABITS** sections, with STOP and per-type snooze.

## Review Focus

- 25 reminders → 1 alarm per distinct trigger time; no 9-minute-limit stall when exact access granted.
- Alarm fires in background/Doze while the app is closed: audible ring + vibration via FGS + wakelock, heads-up/full-screen on lock screen, dismissible STOP, audible even if the full-screen UI is blocked.
- Two reminders at the same minute → ONE alarm UI listing both, grouped by type; snoozing "Tasks" vs "Habits" only snoozes that group.
- Boot / app-update / TIME_SET / TIMEZONE_CHANGED re-arm from the Room source of truth (timezone change re-derives from the `reminderEpochMillis` wall-clock mirrors, so recurring reminders stay pinned to local time).
- No duplicate scheduling after repeated reprojections (idempotent: same trigger-key → replaced; removed keys → cancelled).
- `ReminderScheduleCalculatorTest` (13) still green; new `AlarmEventProjectorTest` green; compile/lint clean.

## Status — COMPLETE (2026-09-24)

All implementation steps from this plan are implemented, compiled, and verified:

- [x] `AlarmEvent` model + `AlarmEventProjector` (one event per distinct trigger, no caps) + `AlarmEventProjectorTest` (9 tests)
- [x] `AlarmEventCodec` snapshot-in-extras encoding + `AlarmIntents` (stable event keys, `FLAG_UPDATE_CURRENT` idempotent re-arm)
- [x] `ReminderScheduler` rewritten: `setAlarmClock` ladder (Doze-exempt) → `setAndAllowWhileIdle` fallback, `reproject()` diff vs `scheduled_event_keys` prefs
- [x] `AlarmPlaybackService`: `mediaPlayback` FGS, looping tone + vibration + wakelock, ONE HIGH InboxStyle notification (FSI + STOP/Snooze), notification-only fallback on `startForeground` refusal
- [x] `AlarmReceiver` rewritten: decode event → start FGS → `goAsync` `handleEventFired`
- [x] `AlarmFullScreenActivity` rewritten in Compose: dark LifeOS theme, TASKS/HABITS groups, STOP, per-type snooze selector + 5/10/15/20/60 chips; `setShowWhenLocked`/turnScreenOn preserved
- [x] `ReminderRepository` single `reprojectAll()` funnel + `normalizePastDue` + `handleEventFired(Set)` + `snoozeMany`
- [x] `BootReceiver`: TIME_SET/TIMEZONE_CHANGED → `rebuildFromMirrors(taskRepository.getAllForBackup, habitRepository.getAllForBackup)`
- [x] Habit add-dialog + detail Reminder card gated on `ExactAlarmPermissionHost` (SCHEDULE_EXACT_ALARM) — tasks already gated
- [x] Manifest: `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_MEDIA_PLAYBACK` / `WAKE_LOCK`, `mediaPlayback` service declaration
- [x] strings.xml: section / snooze-target / per-type strings; existing alarm keys re-valued
- [x] No Room schema/migration change (DB v4 untouched); no artificial limits; no INTERNET/network/AI added
- [x] Verified: `:app:testDebugUnitTest` BUILD SUCCESSFUL (108 tests, 0 failures; `ReminderScheduleCalculatorTest` + `AlarmEventProjectorTest` included), `:app:assembleDebug` SUCCESSFUL, `:app:lintDebug` 0 errors
- [x] `UPDATE.md` + `CHANGELOG.md` updated
- [ ] Commit + push `feat/reliable-coalesced-alarms` + open PR — pending user's GitHub PAT (never written to disk)

On-device spot-checks still recommended where no emulator is available: a real alarm under Doze, lock-screen full-screen/heads-up, per-type snooze, and reboot/time-zone re-arm.