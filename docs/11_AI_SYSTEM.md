# 11 — AI System (Rewritten: LifeOS Intelligence Engine, Fully Offline)

## This document supersedes the previous version

The AI architecture described in earlier drafts of this doc (an Anthropic
API client requiring a user-supplied key) has been **completely replaced**.
LifeOS's "Ask LifeOS AI" and all AI note/diary/report features run
**100% on-device**, with zero network calls, zero API keys, and zero
external endpoints anywhere in the app.

## Provider

None. There is no external AI provider. `core/ai/AiClient.kt` (the old
OkHttp-based Anthropic client) has been **deleted** from the codebase.

## Where AI logic lives

`app/src/main/java/com/lifeos/app/core/intelligence/` — the LifeOS
Intelligence Engine:

```
LifeOSIntelligenceEngine (LifeOSIntelligenceEngine.kt)
    |
    |-- DiaryAnalyzer           DiaryAnalyzer.kt
    |-- MoodAnalyzer            MoodAnalyzer.kt  (lexicon in MoodLexicon.kt)
    |-- KeywordExtractor        MoodAnalyzer.kt
    |-- TaskAnalyzer             TaskAnalyzer.kt
    |-- HabitAnalyzer            HabitAnalyzer.kt
    |-- ProductivityAnalyzer     ProductivityAnalyzer.kt
    |-- TrendAnalyzer            TrendAnalyzer.kt
    |-- PatternDetector          PatternDetector.kt
    |-- CorrelationAnalyzer      PatternDetector.kt
    |-- RecommendationEngine     RecommendationAndStatistics.kt
    |-- StatisticsEngine         RecommendationAndStatistics.kt
    |-- ReportGenerator          ReportGenerator.kt (weekly + monthly)
    |-- NoteTextAnalyzer         NoteTextAnalyzer.kt (extractive note actions)
    |-- LocalQuestionEngine      LocalQuestionEngine.kt (chat intent matching)
    `-- generative/              Phase 19 local-model seam
         |-- LocalGenerativeModel.kt
         |-- LocalModelState.kt
         `-- LocalGenerativeGateway.kt
```

`core/ai/AiRepository.kt` remains a **thin compatibility layer** over the
engine so existing UI call sites do not need an architecture rewrite.

## What algorithms/techniques are used

The existing Level 1 intelligence remains deterministic and explainable:

| Capability | Technique |
|---|---|
| Mood detection | Lexicon lookup + simple negation handling |
| Keyword extraction | Frequency counting after stopword removal |
| Note summarization | Extractive sentence scoring |
| Task extraction / checklists | Rule-based action cues and verbs |
| Habit streaks | Existing `HabitRepository.computeAnalytics()` |
| Productivity score | Task/habit completion arithmetic |
| Trend detection | Current-period vs previous-period change |
| Pattern/correlation | Mean comparison, not a statistical correlation coefficient |
| Weekly/monthly reports | Template-based natural language |
| Ask LifeOS AI | Local intent matching routed to analyzers |

## Phase 19 — Offline generative foundation

Phase 19 introduces a provider-independent contract for a **future genuine
on-device generative model** without bundling a model yet.

`LocalGenerativeModel` defines the only model contract. It exposes explicit
model metadata and a bounded `GenerationRequest` containing system
instruction, prompt, maximum output tokens, and temperature.

`LocalGenerativeGateway` is the single orchestration boundary. When no model
is installed it returns an explicit `Unavailable` result. It does **not** use
network services, API keys, hidden fallbacks, or fake generative output.

`LifeOSIntelligenceEngine` now exposes `localGenerativeModelState` and
`generateLocally(...)` while preserving all existing deterministic methods
and repository wiring. `ServiceLocator` therefore remains compatible without
requiring a new dependency or changing ViewModel signatures.

Phase 19 intentionally adds **zero model weights** and **zero inference
runtime dependencies**. APK size should not materially increase from this
foundation alone.

## Phase 20 target — actual local model

The next phase can provide a verified implementation of `LocalGenerativeModel`
using a suitable Android on-device runtime and a bundled/packaged quantized
model. Model selection must be based on device RAM, latency, licensing,
quality, ABI/package footprint, and offline operation rather than an arbitrary
APK-size target.

The final app must keep the data path local:

```
Room repositories
      |
      v
Relevant context selector
      |
      v
Prompt builder
      |
      v
LocalGenerativeModel
      |
      v
Generated response
```

No user content may be sent to a remote AI provider.

## Honesty about what's NOT offered

Several original AI note actions (Rewrite, Improve grammar, Make longer,
Generate ideas, Explain content, Study questions) require genuine
text generation. Until Phase 20 supplies a verified local generative model,
these actions must not pretend that deterministic rules are generative AI.

## Network access required?

**None.** The LifeOS AI architecture remains offline-first and does not add
an external AI endpoint or API key.

## Data privacy

Every existing analyzer continues to use the repository layer. A future
local model must receive only locally selected context and must never
serialize LifeOS content for transmission.

## Current-state addendum — 2026-09-17

Phase 19 is implemented on branch `phase-19-offline-ai-foundation`. It adds
only the local generative abstraction, lifecycle/result types, gateway, and
facade integration. No model weights or third-party inference runtime were
added in this phase. Build/test status is only considered verified when the
exact Android/Gradle command has been executed in a real build environment.
