# 22 — Environment Variables / Secrets

## Current status

The current LifeOS source does not require runtime environment variables or external API keys.

| Variable | Required? | Purpose |
|---|---|---|
| None | No | The current app has no external service dependency |

## Security rule

Do not add cloud AI/API keys to source, DataStore, build config or documentation as a prerequisite for LifeOS Intelligence. The current Intelligence Engine is local/offline.

If a future optional developer-only integration is ever added, it must be explicitly separated from the released offline application and documented as such.

## Current-state addendum — 2026-09-16

This document remains part of the LifeOS documentation set. Current UI/UX, motion, responsive and accessibility rules are centralized in [`DESIGN.md`](./DESIGN.md). The current Intelligence implementation is local/offline and requires no external AI provider or API key. Build/test statements are only considered verified when the exact command has been executed in a real Android/Gradle environment.

