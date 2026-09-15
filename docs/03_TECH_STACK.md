# 03 — Tech Stack

All versions below are copied verbatim from `build.gradle.kts`,
`app/build.gradle.kts`, and `gradle/wrapper/gradle-wrapper.properties`.
None are guessed.

| Technology | Version | Purpose | Where Used | Why |
|---|---|---|---|---|
| Kotlin | 2.0.21 | Primary programming language | Entire `app/src/main/java` tree | Google's recommended, modern language for Android |
| Android Gradle Plugin (AGP) | 8.6.1 | Builds the Android app | `build.gradle.kts` (root) | Current stable AGP at time of writing |
| Kotlin Compose Compiler plugin | 2.0.21 | Compiles Jetpack Compose UI code | `app/build.gradle.kts` | Kotlin 2.0+ moved the Compose compiler to this dedicated plugin |
| KSP (Kotlin Symbol Processing) | 2.0.21-1.0.28 | Annotation processing for Room | `app/build.gradle.kts` | Faster, Kotlin-first alternative to kapt, generates Room's database code |
| Gradle | 8.9 (wrapper) | Build tool | `gradle/wrapper/gradle-wrapper.properties` | ⚠️ NOT VERIFIED: the gradlew/gradlew.bat wrapper scripts are not present in this repository, only the properties file. See docs/13_DEPLOYMENT.md and docs/16_KNOWN_ISSUES.md |
| Jetpack Compose BOM | 2024.11.00 | UI toolkit version alignment | `app/build.gradle.kts` | Ensures all Compose libraries use compatible versions |
| Compose Material 3 | via BOM | UI components | Every file under ui/ | Google's current design system |
| Compose Navigation | 2.8.4 | Screen-to-screen navigation | `ui/navigation/LifeOSNavHost.kt` | Standard Compose navigation library |
| Room | 2.6.1 | Local SQLite database/ORM | `data/db/` | Google's recommended persistence library for Android |
| DataStore Preferences | 1.1.1 | Key-value settings storage | `core/util/SettingsStore.kt` | Modern replacement for SharedPreferences |
| AndroidX Biometric | 1.1.0 | Fingerprint/PIN app lock | `core/security/AppLockManager.kt` | Standard biometric authentication API |
| AndroidX WorkManager | 2.10.0 | Scheduled background jobs | `core/reminders/ReminderScheduler.kt`, `ReminderWorker.kt` | Precise, battery-friendly one-time reminder jobs |
| OkHttp | 4.12.0 | HTTP client | `core/ai/AiClient.kt` | Makes the HTTPS call to Anthropic's API; used directly instead of a heavier framework since only one endpoint is called |
| kotlinx.serialization (JSON) | 1.7.3 | JSON encode/decode | `core/ai/AiClient.kt`, `data/repository/BackupRepository.kt`, `domain/model/NoteBlock.kt` | Serializes note blocks, backup exports, and AI request/response bodies |
| kotlinx.coroutines | 1.9.0 | Asynchronous programming | Throughout ViewModels and repositories | Backs every suspend fun and Flow |
| Coil Compose | 2.7.0 | Image loading | Declared in app/build.gradle.kts; ⚠️ NOT VERIFIED as actually invoked — no AsyncImage usage found in ui/ source | Intended for displaying captured photo thumbnails |
| CameraX (core, camera2, lifecycle, view, video) | 1.4.0 | Camera capture | `ui/capture/CameraCaptureScreen.kt`, `ui/capture/VideoCaptureScreen.kt` | Photo and video capture |
| AndroidX Fragment KTX | 1.8.5 | FragmentActivity base class | `MainActivity.kt` | Required because BiometricPrompt needs a FragmentActivity, not a plain ComponentActivity |
| Material Icons Extended | via material-icons-extended | Icon set | Throughout ui/ | Icons like Camera, Mic, Send, etc. |
| JUnit | 4.13.2 | Unit test framework | Declared as testImplementation in app/build.gradle.kts | ⚠️ Declared but zero test files exist — see docs/14_TESTING.md |
| AndroidX Test / Espresso | 1.2.1 / 3.6.1 | Instrumented UI test framework | Declared as androidTestImplementation | ⚠️ Declared but zero test files exist |

## Not present (confirmed by repository inspection)

| Category | Status |
|---|---|
| Backend framework (Node/Express, Python/Django, etc.) | ❌ Not present |
| Firebase (Auth, Firestore, Realtime DB, Analytics, Crashlytics) | ❌ Not present |
| Hosting / cloud provider config | ❌ Not present |
| Retrofit / Ktor / other networking framework | ❌ Not present (OkHttp used directly) |
| Dependency injection framework (Hilt/Dagger/Koin) | ❌ Not present — manual ServiceLocator used instead |
| CI/CD tooling (GitHub Actions, Fastlane, etc.) | ✅ Present — `.github/workflows/android-build.yml` builds a debug APK on push/PR (see `docs/12_GITHUB_WORKFLOW.md`); no test/release/publish automation yet |
| Capacitor / React Native / Flutter (cross-platform layer) | ❌ Not present — this is a native Android/Kotlin app |
| iOS project | ❌ Not present — Android only |

## Minimum/target Android versions

From `app/build.gradle.kts`:

| Setting | Value |
|---|---|
| compileSdk | 35 |
| minSdk | 26 (Android 8.0 Oreo) |
| targetSdk | 35 |
| Java/Kotlin JVM target | 17 |
