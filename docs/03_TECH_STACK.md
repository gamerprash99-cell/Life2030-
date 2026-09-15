# 03 — Technology Stack

| Area | Technology | Current role |
|---|---|---|
| Language | Kotlin | Application source |
| UI | Jetpack Compose | Native UI |
| Design | Material 3 | Components/theme foundation |
| Persistence | Room / SQLite | Local data |
| Preferences | DataStore Preferences | Settings |
| Navigation | Navigation Compose | App navigation |
| Scheduling | WorkManager | Task/habit reminders |
| Biometrics | AndroidX BiometricPrompt | Device biometric authentication |
| Camera | CameraX | Photo/video capture |
| Audio | MediaRecorder | Audio capture |
| Image loading | Coil | Capture/media presentation |
| Serialization | kotlinx.serialization JSON | Backup and local models |
| Local Intelligence | Plain Kotlin analyzers/rules/templates | Offline analysis |
| Testing framework | JUnit + AndroidX/Compose test dependencies | Declared; runtime coverage must be verified separately |

## Android versions

- compileSdk: 35
- minSdk: 26
- targetSdk: 35
- JVM target: 17

## Network/cloud

No `INTERNET` permission is declared. There is no OkHttp/Retrofit cloud-AI integration in the current source. Do not reintroduce one as a requirement for the released app.

## UI design system

See [`DESIGN.md`](./DESIGN.md) for current LifeOS visual tokens, responsive rules and motion specifications.

## Current-state addendum — 2026-09-16

This document remains part of the LifeOS documentation set. Current UI/UX, motion, responsive and accessibility rules are centralized in [`DESIGN.md`](./DESIGN.md). The current Intelligence implementation is local/offline and requires no external AI provider or API key. Build/test statements are only considered verified when the exact command has been executed in a real Android/Gradle environment.

