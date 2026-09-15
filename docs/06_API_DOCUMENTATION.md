# 06 — API Documentation

## Overview

This app **exposes no API of its own** (there is no backend server). It
makes exactly **one** outbound API integration: to Anthropic's Messages API,
and only when the user has enabled AI features and supplied their own key.

---

## Anthropic Messages API

- **Service**: Anthropic (`api.anthropic.com`)
- **Purpose**: Powers all AI features — note actions, task extraction from
  notes, diary drafting, weekly review summaries, and AI Assistant chat.
- **Endpoint**: `https://api.anthropic.com/v1/messages`
- **HTTP method**: `POST`
- **Client code**: `app/src/main/java/com/lifeos/app/core/ai/AiClient.kt`
- **Prompt-assembly code**: `app/src/main/java/com/lifeos/app/core/ai/AiRepository.kt`

### Authentication

- **Header**: `x-api-key: <user's own Anthropic API key>`
- **Also sent**: `anthropic-version: 2023-06-01`, `content-type: application/json`
- The key is supplied by the end user in Settings (`ui/settings/SettingsScreen.kt`),
  stored locally via `core/util/SettingsStore.kt` (Android DataStore
  Preferences — **not encrypted at rest**, see `docs/08_SECURITY.md`).
- `AiClient` is constructed with `apiKeyProvider = { settingsStore.aiApiKeyBlocking() }`
  (see `core/di/ServiceLocator.kt`) — the key is read fresh on every call, so
  updating it in Settings takes effect immediately.

### Model used

⚠️ **Flag for review**: `AiClient.kt` hardcodes:
```kotlin
private val model: String = "claude-sonnet-4-6"
```
This string should be verified against Anthropic's currently valid model
identifiers before shipping — it is exactly what's in the code, not
independently verified against a live API call (no network access was
available to test this integration during development).

### Request shape (from `AiClient.kt`)

```json
{
  "model": "claude-sonnet-4-6",
  "max_tokens": 1024,
  "system": "<system prompt, varies by feature>",
  "messages": [
    { "role": "user", "content": "<assembled prompt>" }
  ]
}
```

`max_tokens` varies by feature (700 for note actions, 400 for task
extraction/diary drafting, 500 for weekly review, 700 for chat) — set at
each call site in `AiRepository.kt`.

### Response handling

`AiClient.complete()` parses the response manually (no generated client),
extracting `content[].text` fields and joining them. Returns a sealed
`AiResult`:
- `AiResult.Success(text)`
- `AiResult.Error(message)` — covers non-2xx HTTP responses and exceptions
- `AiResult.NoApiKey` — returned immediately, without making a network call, if no key is configured

### Error cases handled in code

| Case | Behavior |
|---|---|
| No API key set | `AiResult.NoApiKey` returned before any network call is made |
| Non-2xx HTTP response | `AiResult.Error("AI request failed (<code>): <body>")` |
| Empty response text | `AiResult.Error("AI returned an empty response.")` |
| Network/other exception | `AiResult.Error(e.message)` |
| Timeouts | OkHttp client configured with 15s connect / 60s read timeout |

### Which features call this, and what they send

| Feature | Function | What's sent to Anthropic |
|---|---|---|
| Note AI actions (summarize, rewrite, etc.) | `AiRepository.runNoteAction()` | The note's plain-text content only |
| Extract tasks from a note | `AiRepository.extractTasks()` | The note's plain-text content only |
| Draft a diary entry | `AiRepository.draftDiaryEntry()` | The raw thoughts text the user typed |
| Weekly review | `AiRepository.generateReviewSummary()` | A pre-aggregated stats string (task count, spend total, diary count) — **not** raw task/expense/diary content |
| AI Assistant chat | `AiRepository.chat()` | The conversation history only — ⚠️ `contextBlock` is always `null` from `AiAssistantScreen.kt`, so no other app data is sent |

This confirms the code comment's claim in `AiClient.kt`: *"only send the
specific text/context needed for that action — never your whole database."*
This was verified directly, not assumed.

### Files that call this API

- `core/ai/AiClient.kt` — the only file that performs the network call
- `core/ai/AiRepository.kt` — the only file that calls `AiClient`
- Screens calling `AiRepository`: `ui/notes/NotesViewModel.kt`,
  `ui/diary/DiaryScreen.kt`, `ui/insights/InsightsScreen.kt`,
  `ui/ai/AiAssistantScreen.kt`

### Environment variables required

**None.** The API key is entered by the end user at runtime through the app
UI and stored on-device — it is not a build-time environment variable or
secret baked into the app. See `docs/22_ENVIRONMENT_VARIABLES.md`.

---

## Other APIs / services

⚠️ **NOT PRESENT** — no other external API, SDK, or third-party service
(Firebase, Google Sign-In, Maps, Analytics, Crashlytics, push notifications,
payment processors) exists anywhere in the source tree or `app/build.gradle.kts`.
