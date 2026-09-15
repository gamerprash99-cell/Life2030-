# 24 — Architectural Decisions

## ADR-001 — Native Android stack

LifeOS remains Kotlin + Jetpack Compose + Material 3. No cross-platform UI rewrite is permitted for UI polish work.

## ADR-002 — Local-first persistence

Room remains the source of truth for core structured data. DataStore remains the preference store.

## ADR-003 — Local Intelligence

The Intelligence Engine uses deterministic analyzers, lexicons, rules, statistics and templates. This avoids making a cloud AI service a requirement for the product.

## ADR-004 — Navigation Compose

Existing Navigation Compose and the existing NavController/back stack remain authoritative. UI polish must not replace navigation with manual screen switching.

## ADR-005 — Figma as visual reference

Figma is used to establish visual hierarchy, component language and motion intent. Android implementation maps those decisions into the existing Compose design system rather than copying web-specific implementation code.

## ADR-006 — Shared design tokens

Colors, spacing and shapes are centralized in `ui/theme/`. Reusable components live in `ui/components/`. Screen-specific code should consume those tokens instead of introducing arbitrary dp/color values.

## ADR-007 — Android biometric behavior

Biometric authentication remains delegated to `BiometricPrompt`. LifeOS does not maintain a biometric database and cannot distinguish enrolled people.

## ADR-008 — Backup restore via SAF

JSON restore uses `ACTION_OPEN_DOCUMENT` and `application/json`, avoiding broad storage permission and preserving the existing `BackupRepository` boundary.

## Current-state addendum — 2026-09-16

This document remains part of the LifeOS documentation set. Current UI/UX, motion, responsive and accessibility rules are centralized in [`DESIGN.md`](./DESIGN.md). The current Intelligence implementation is local/offline and requires no external AI provider or API key. Build/test statements are only considered verified when the exact command has been executed in a real Android/Gradle environment.

