# 14 — Testing

## Current verification status — 2026-09-16

The supplied source snapshot has not been compile-verified in the current environment. The Gradle wrapper scripts/JAR are absent and no usable Android Gradle build environment is available here.

Therefore the following are **required commands, not claimed results**:

```text
./gradlew test
./gradlew assembleDebug
```

Relevant instrumentation/UI tests should also be run when an Android device/emulator environment is available.

## Manual acceptance checklist

- Fresh install
- Onboarding page navigation
- Skip
- Get started
- JSON restore from Android document picker
- Invalid JSON handling
- Valid backup restore
- Dynamic Home greeting/date
- Tasks
- Habits
- Diary
- Expenses
- Notes
- Timeline
- Capture
- Settings
- App Lock None
- App Lock PIN
- PIN recovery
- Biometric availability/setup
- Successful biometric authentication
- Failed biometric authentication
- Back navigation
- Bottom navigation
- Small screen
- Large screen
- Font scaling
- Dark mode

## UI-specific checks

- No content under system navigation bars
- Interactive targets at least 48dp
- No clipped text at supported font scales
- No hardcoded current dates/times/statistics
- Animations remain short and non-blocking
- Long lists use lazy containers where appropriate

Never document a test as passed unless the exact check was actually executed.

## Current-state addendum — 2026-09-16

This document remains part of the LifeOS documentation set. Current UI/UX, motion, responsive and accessibility rules are centralized in [`DESIGN.md`](./DESIGN.md). The current Intelligence implementation is local/offline and requires no external AI provider or API key. Build/test statements are only considered verified when the exact command has been executed in a real Android/Gradle environment.

