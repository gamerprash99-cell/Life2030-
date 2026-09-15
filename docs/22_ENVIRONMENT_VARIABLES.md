# 22 — Environment Variables

## Summary

**This project uses zero build-time environment variables.** Confirmed by
searching the entire repository for System.getenv, custom BuildConfig
fields, local.properties-based secrets, and .env file patterns — none exist.

The only "configuration value" resembling a secret is the AI API key, and
it is deliberately not an environment variable — it's a runtime value the
end user types into the running app (Settings screen), stored via
core/util/SettingsStore.kt (Android DataStore Preferences) on their own device.

## Table (per the requested format)

| Variable | Purpose | Required | Where Used | Example |
|---|---|---|---|---|
| (none exist) | — | — | — | — |

## The one runtime "secret" (not an environment variable)

| Value | Purpose | Required for | Where entered | Where stored | Example format |
|---|---|---|---|---|---|
| Anthropic API key | Authenticates AI feature calls | AI features only (app works fully without it) | ui/settings/SettingsScreen.kt, a plain text field | core/util/SettingsStore.kt via DataStore Preferences key ai_api_key (unencrypted — see docs/08_SECURITY.md) | YOUR_ANTHROPIC_API_KEY |

## Standard Android files that could hold secrets (currently empty/absent)

| File | Status in this repo |
|---|---|
| local.properties | Not present in the repo (normal — machine-specific, typically holds the local Android SDK path, not app secrets) |
| .env | Not present, not referenced anywhere in Gradle config |
| keystore.properties (common convention for signing secrets) | Not present — ties to docs/16_KNOWN_ISSUES.md Issue #2 (no signing config exists yet) |

## Recommendation if secrets are ever needed at build time

NOT VERIFIED FROM CODEBASE — forward-looking guidance only. If a future
feature requires a build-time secret (e.g. a shared/default API key, a
signing keystore password), the standard, safe Android pattern is:
1. Add the value to local.properties or a dedicated keystore.properties
2. Read it in app/build.gradle.kts via Properties() loaded from that file
3. Expose it to code only via BuildConfig fields, never hardcoded in a .kt file
4. Ensure the properties file is listed in .gitignore
