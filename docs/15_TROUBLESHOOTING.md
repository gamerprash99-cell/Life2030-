# 15 — Troubleshooting

## Build cannot start with `./gradlew`

The supplied project snapshot may not contain the Gradle wrapper scripts/JAR. Verify the repository contents before assuming the wrapper exists.

Preferred recovery on a development machine is to open the project in Android Studio with a compatible JDK/Android SDK or regenerate the wrapper using an installed compatible Gradle version. Do not hide build failures by disabling tasks or suppressing compiler errors.

## Backup restore fails

Check:

1. the selected document is JSON;
2. the file is a LifeOS backup;
3. required backup fields are present;
4. the existing `BackupRepository` reports a compatible format;
5. the current restore semantics are understood before retrying against real data.

The UI uses Android Storage Access Framework and `application/json`.

## Biometric setup unavailable

LifeOS uses Android's biometric enrollment. If the device has no suitable enrolled biometric, direct the user to Android security/biometric settings and re-check availability after returning to LifeOS.

LifeOS cannot identify which enrolled fingerprint/face belongs to a particular person.

## Intelligence

LifeOS Intelligence is local and does not require an API key or internet connection. If a local analysis fails, inspect the corresponding analyzer and repository data flow rather than looking for a cloud credential.

## UI problems

For spacing, colors, typography, animation and responsive behavior, use [`DESIGN.md`](./DESIGN.md) as the current design reference.

## Current-state addendum — 2026-09-16

This document remains part of the LifeOS documentation set. Current UI/UX, motion, responsive and accessibility rules are centralized in [`DESIGN.md`](./DESIGN.md). The current Intelligence implementation is local/offline and requires no external AI provider or API key. Build/test statements are only considered verified when the exact command has been executed in a real Android/Gradle environment.

