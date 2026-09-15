# 20 — Founder Guide

## What LifeOS is

LifeOS is a native Android personal life-management app that brings Notes, Tasks, Habits, Diary, Expenses, Timeline and Life Capture together on one device.

## Privacy model

The current product direction is offline-first and privacy-first. Core data is stored locally. LifeOS Intelligence runs locally using deterministic Kotlin analysis; no cloud AI provider or API key is required.

## UI direction

The 2026 visual direction is premium, calm and modern: lavender/violet identity, soft surfaces, rounded cards, grouped settings, clear typography and restrained motion. See `docs/DESIGN.md`.

## Backup

Users can export a local JSON backup and restore it through Android's native document picker. Restore is routed through `BackupRepository`.

## App Lock

Users can choose None, Biometric or PIN. Biometric authentication is provided by Android and is based on device-enrolled biometrics. A PIN is the independent app-specific option.

## Current limitations

- Build verification depends on a proper Android/Gradle environment.
- Automated test coverage must be expanded and executed.
- Database-at-rest encryption remains a security hardening item.
- Some existing feature-model fields remain outside their creation UI.

## Current-state addendum — 2026-09-16

This document remains part of the LifeOS documentation set. Current UI/UX, motion, responsive and accessibility rules are centralized in [`DESIGN.md`](./DESIGN.md). The current Intelligence implementation is local/offline and requires no external AI provider or API key. Build/test statements are only considered verified when the exact command has been executed in a real Android/Gradle environment.

