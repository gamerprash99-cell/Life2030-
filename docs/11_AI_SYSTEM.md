# 11 — AI System (Rewritten: LifeOS Intelligence Engine, Fully Offline)

## This document supersedes the previous version

The AI architecture described in earlier drafts of this doc (an Anthropic
API client requiring a user-supplied key) has been **completely replaced**.
LifeOS's "Ask LifeOS AI" and all AI note/diary/report features now run
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
    |-- TaskAnalyzer            TaskAnalyzer.kt
    |-- HabitAnalyzer           HabitAnalyzer.kt
    |-- ProductivityAnalyzer    ProductivityAnalyzer.kt
    |-- TrendAnalyzer           TrendAnalyzer.kt
    |-- PatternDetector         PatternDetector.kt
    |-- CorrelationAnalyzer     PatternDetector.kt
    |-- RecommendationEngine    RecommendationAndStatistics.kt
    |-- StatisticsEngine        RecommendationAndStatistics.kt
    |-- ReportGenerator         ReportGenerator.kt (weekly + monthly)
    |-- NoteTextAnalyzer        NoteTextAnalyzer.kt (extractive note actions)
    `-- LocalQuestionEngine     LocalQuestionEngine.kt (chat intent matching)
```

`core/ai/AiRepository.kt` is now a **thin compatibility layer** over this
engine — kept so the four UI call sites (Notes, Diary, Insights, AI
Assistant) didn't need a full rewrite, only their old "no API key" branches
removed.

## What algorithms/techniques are used

All deterministic, explainable, no ML model file:

| Capability | Technique |
|---|---|
| Mood detection | Lexicon lookup (`MoodLexicon.kt`, ~150 hand-picked words with fixed weights) + simple negation handling ("not happy" flips the sign) |
| Keyword extraction | Frequency counting after stopword removal — the same well-established technique behind many "tag cloud" features |
| Note summarization | Extractive: scores each sentence by keyword density, keeps the top N in original order |
| Task extraction / checklists | Rule-based: matches sentences against action-cue phrases ("need to", "remember to") and common action-verb sentence starts |
| Habit streaks | Already-existing `HabitRepository.computeAnalytics()` logic (unchanged), just summarized by the engine |
| Productivity score | Plain average of task-completion% and habit-completion% — no hidden weighting |
| Trend detection | Current-period-vs-previous-period percentage change (arithmetic, not forecasting) |
| Pattern/correlation | Mean comparison (e.g. average mood on habit-completed days vs. not) — explicitly labeled as a simple comparison, not a statistical correlation coefficient |
| Weekly/monthly reports | Template-based natural language: if/else branches over already-computed numbers build the sentences |
| "Ask LifeOS AI" chat | Keyword/phrase intent matching against ~7 known question categories, routed to the matching analyzer; anything unmatched gets an honest "I don't understand that yet" plus a list of what it can answer |

## Honesty about what's NOT offered

Several of the original "AI note actions" (Rewrite, Improve grammar, Make
longer, Generate ideas, Explain content, Study questions) are genuinely
**generative writing tasks** that a rule-based/extractive engine cannot do
well. Rather than fake these with garbled output, `LifeOSIntelligenceEngine.runNoteAction()`
returns a clear, honest message for these specific actions explaining that
they need a generative model LifeOS doesn't include, and points to the
actions that *do* work offline (Summarize, Organize text, Extract points,
Create checklist, Make shorter). This is a deliberate product/honesty
choice, not an oversight.

Similarly, "draft a diary entry from rough thoughts" is now a **modest,
honest reformatting** (capitalization cleanup + a one-line mood
reflection), not a full generative rewrite — see `LifeOSIntelligenceEngine.draftDiaryEntry()`'s
doc comment.

## Approximate added footprint

No downloaded model file, no bundled weights — everything is plain Kotlin
source (~15 small files, a few hundred lines each, mostly arithmetic and a
hand-written word list). This compiles to ordinary bytecode inside the
existing APK; the realistic added size is on the order of tens of KB, not
megabytes. ⚠️ **NOT INDEPENDENTLY MEASURED** — no build environment was
available to produce an actual APK and diff its size; this is an estimate
based on the source size, not a measured APK delta.

## Network access required?

**None.** Confirmed by direct code search: no `HttpURLConnection`, no
OkHttp, no Retrofit, no Ktor, and no network permission remain anywhere in
the app (see `docs/08_SECURITY.md` and `AndroidManifest.xml` — the
`INTERNET` permission has been removed entirely, since nothing in the app
uses it anymore).

## Data privacy

Every analyzer reads through the existing repository layer (`data/repository/`)
— the same Room-backed repositories every other feature uses. No
diary/note/task/habit/expense content is ever serialized for transmission
anywhere; there is no transmission code to do so in the first place.

## Modularity for a future on-device model

`LifeOSIntelligenceEngine` is a single facade with plain method signatures
(`runNoteAction`, `weeklyReport`, `answerQuestion`, etc.). If a genuinely
useful, small on-device generative model is added later, it can be slotted
in as an *additional* path inside `NoteTextAnalyzer`/`LifeOSIntelligenceEngine`
for the specific actions that are honestly unsupported today, without
redesigning this facade or touching any UI call site.

## Current-state addendum — 2026-09-16

This document remains part of the LifeOS documentation set. Current UI/UX, motion, responsive and accessibility rules are centralized in [`DESIGN.md`](./DESIGN.md). The current Intelligence implementation is local/offline and requires no external AI provider or API key. Build/test statements are only considered verified when the exact command has been executed in a real Android/Gradle environment.

## Current implementation snapshot — 2026-09-16

The updated Insights presentation calls the existing local report path explicitly a local review; no cloud model or API key was introduced.
