# 23 — Dependencies

Source of truth: app/build.gradle.kts and build.gradle.kts.

## Direct dependencies (declared in app/build.gradle.kts)

| Name | Version | Purpose | Direct/Transitive | Where Used | Importance | Upgrade Risk |
|---|---|---|---|---|---|---|
| androidx.core:core-ktx | 1.15.0 | Kotlin extensions for Android core APIs | Direct | Throughout | High | Low |
| androidx.lifecycle:lifecycle-runtime-ktx | 2.8.7 | Lifecycle-aware coroutines | Direct | ViewModels | High | Low |
| androidx.lifecycle:lifecycle-viewmodel-ktx | 2.8.7 | ViewModel + coroutines | Direct | Every ViewModel class | High | Low |
| androidx.lifecycle:lifecycle-viewmodel-compose | 2.8.7 | viewModel() composable function | Direct | Every screen | High | Low |
| androidx.activity:activity-compose | 1.9.3 | Compose integration for Activities | Direct | MainActivity.kt | High | Low |
| androidx.fragment:fragment-ktx | 1.8.5 | FragmentActivity base class | Direct | MainActivity.kt, required for BiometricPrompt | High, removing breaks App Lock | Low |
| org.jetbrains.kotlinx:kotlinx-coroutines-android | 1.9.0 | Async programming | Direct | Everywhere | Critical | Low |
| org.jetbrains.kotlinx:kotlinx-serialization-json | 1.7.3 | JSON serialization | Direct | AiClient.kt, BackupRepository.kt, NoteBlock.kt | High | Low |
| androidx.compose:compose-bom | 2024.11.00 | Version alignment for all Compose libs | Direct | Everywhere in ui/ | Critical | Medium |
| androidx.compose.ui:ui | via BOM | Core Compose UI | Direct | Everywhere | Critical | Low |
| androidx.compose.ui:ui-graphics | via BOM | Compose graphics primitives | Direct | ui/theme/Color.kt etc | Medium | Low |
| androidx.compose.ui:ui-tooling-preview | via BOM | @Preview support | Direct | N/A, no @Preview functions currently exist | Low currently | Low |
| androidx.compose.material3:material3 | via BOM | Material 3 components | Direct | Everywhere in ui/ | Critical | Medium |
| androidx.compose.material:material-icons-extended | via BOM | Full icon set | Direct | Every screen using Icons.Filled.* | Medium | Low |
| androidx.navigation:navigation-compose | 2.8.4 | Screen navigation | Direct | ui/navigation/ | Critical | Low |
| androidx.compose.ui:ui-tooling | via BOM, debug only | Compose debug tooling | Direct (debugImplementation) | Debug builds only | Low | Low |
| androidx.room:room-runtime | 2.6.1 | Room database engine | Direct | data/db/ | Critical | Medium |
| androidx.room:room-ktx | 2.6.1 | Kotlin coroutines/Flow support for Room | Direct | Every DAO's Flow return types | Critical | Low |
| androidx.room:room-compiler | 2.6.1 | Annotation processor (KSP) generating Room code | Direct (ksp) | Build-time only | Critical | Medium |
| androidx.datastore:datastore-preferences | 1.1.1 | Settings storage | Direct | core/util/SettingsStore.kt | High | Low |
| androidx.biometric:biometric | 1.1.0 | Biometric authentication | Direct | core/security/AppLockManager.kt | High | Low |
| androidx.work:work-runtime-ktx | 2.10.0 | Background job scheduling | Direct | core/reminders/ | High | Medium |
| com.squareup.okhttp3:okhttp | 4.12.0 | HTTP client | Direct | core/ai/AiClient.kt, the only network call in the app | High | Low |
| io.coil-kt:coil-compose | 2.7.0 | Image loading | Direct | Declared but not currently invoked anywhere in ui/ (no AsyncImage usage found) | Low currently | Low |
| androidx.camera camera-core / camera-camera2 / camera-lifecycle / camera-view / camera-video | 1.4.0 | Camera capture | Direct | ui/capture/CameraCaptureScreen.kt, VideoCaptureScreen.kt | High | Medium |
| junit:junit | 4.13.2 | Unit testing | Direct (testImplementation) | Declared but no test files exist | Low currently | Low |
| androidx.test.ext:junit | 1.2.1 | Android instrumented testing | Direct (androidTestImplementation) | Declared but unused | Low currently | Low |
| androidx.test.espresso:espresso-core | 3.6.1 | UI testing | Direct (androidTestImplementation) | Declared but unused | Low currently | Low |
| androidx.compose.ui:ui-test-junit4 | via BOM | Compose UI testing | Direct (androidTestImplementation) | Declared but unused | Low currently | Low |

## Build plugins (root build.gradle.kts)

| Plugin | Version | Purpose |
|---|---|---|
| com.android.application | 8.6.1 | Android application build plugin |
| org.jetbrains.kotlin.android | 2.0.21 | Kotlin/Android compiler plugin |
| org.jetbrains.kotlin.plugin.compose | 2.0.21 | Compose compiler plugin |
| com.google.devtools.ksp | 2.0.21-1.0.28 | Kotlin Symbol Processing, used by Room |

## Upgrade risk notes for a new developer

- Compose BOM bump: pins many Compose library versions together — test the
  whole UI after any BOM bump, not just the screen you were working on.
- Room bump: pair any behavior-changing bump with a check of
  docs/05_DATABASE.md's migration guidance.
- AGP/Kotlin/KSP version triplet must stay mutually compatible — the KSP
  version string is literally suffixed with the Kotlin version it matches
  (2.0.21-1.0.28). Bumping Kotlin without bumping the matching KSP version
  breaks Room's annotation processing step.
- CameraX: all five CameraX artifacts are pinned to the same cameraxVersion
  variable in app/build.gradle.kts — always bump them together.

## Dependencies NOT present (confirmed)

Hilt/Dagger, Retrofit/Ktor, Firebase (any module), a logging library
(Timber etc.), Glide, Lottie, Accompanist, any analytics SDK, any
crash-reporting SDK, any ads SDK, any payments SDK.
