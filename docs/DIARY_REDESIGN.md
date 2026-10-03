# LifeOS Diary — Redesign & Local Intelligence Specification

Status: implemented. Branch `fix/diary-date-strip-center-today`.

## 1. What this change is

A redesign of the Diary into a searchable, calendared, insight-bearing journal,
plus a **new, offline-only** intelligence layer that reads entries already on
the device.

The redesign direction was explored in Google Stitch (project
`15017831027874600189`, design system **Nocturne & Sol**,
`assets/e8ebbccdfcc145bba03f5edf81ad9e2f`). Four screens were generated as
distinct directions:

| # | Stitch screen | Screen ID | Idea it contributed |
|---|---|---|---|
| A | LifeOS Today Diary | `3cea3c6241644ba0a60dbd6ef1817b30` | 8-disc mood selector; daily header hierarchy |
| B | LifeOS Memory Timeline | `242f30c3bd7f445abe50dea25400699e` | mood-tinted timeline nodes; italic day dividers |
| C | LifeOS Insights | `1ed12aaf59ad4789a9a764bf48016686` | stats band, 7-day mood chart, themes, "Looking back", privacy note |
| D | LifeOS Month Calendar | `4f85e288255c416793a98c9604f48a66` | month grid with mood-tinted day dots, today ring, dimmed future |

Direction E (premium journal) was not generated separately: design system A,
"Nocturne & Sol", already *is* the premium editorial journal direction, so a
fifth screen would have been a duplicate.

### What was taken from Stitch, and what was not

Taken: layout hierarchy, spacing rhythm (24dp margins, `space-xl` between
blocks), hairline-bordered paper cards, pill buttons, mood-disc geometry,
section labels, and the explicit "computed on this device" privacy note.

Deliberately **not** taken:

- **Serif display faces.** The concepts pair Newsreader with Plus Jakarta
  Sans. Bundling two font families is a new binary dependency and a whole-app
  typography change far beyond the Diary. Existing screens already get their
  editorial feel from `FontFamily.Serif` on headings at zero cost.
- **A four-tab bottom nav** (Journal / Memories / Insights / Search). LifeOS
  has a fixed seven-destination bar. Search, Calendar and Insights are reached
  from within the Diary, not by adding tabs.

## 2. Starting state (why most of this is new)

The brief assumed an intelligence layer, search and statistics already existed.
They did not. Verified before implementation:

- **No** `Analyzer`, `Insight`, `LocalQuestion`, `PatternDetect` or
  `StatisticsEngine` type existed anywhere in `app/src`.
- **No search anywhere** — zero `LIKE` or `@RawQuery` in any DAO.
- **No insights, statistics, streaks or mood charts** — no `Insight` composable
  existed in `ui/` at all.
- `DiaryEntity.title` and `.mood` were both persisted and written, but
  `DiaryEditorViewModel.onTitleChange` and `.onMoodChange` had **zero callers**.
  Mood was display-only; it could not be set from the UI, which made every
  mood-based feature impossible until this change.

So the intelligence work is greenfield, and the editor's mood and title fields
were un-wiring existing-but-unreachable state rather than adding new columns.
Neither required a schema migration.

## 3. Domain foundation

`DiaryMood` moved from `ui/diary` to `domain/model`, and was split.

The persisted value is a single string that already carries an emoji — the
keys are `"😊 Happy"`, `"😌 Calm"`, … — so the domain type keeps `key`, `label`
and `emoji` exactly as they are, and gains one new field:

```kotlin
enum class MoodValence { POSITIVE, NEUTRAL, NEGATIVE }
```

Valence is what lets a pure-Kotlin analyzer reason about mood without
importing Compose colours. Colour mapping stays in the UI layer
(`DiaryMoodVisuals.accentOf` / `.haloOf`), which is the whole reason the split
was needed.

The persisted strings are untouched, so existing entries keep their mood and
the three newer moods remain additive. **No migration.**

## 4. Intelligence layer

New package `domain/intelligence`. Pure Kotlin — no Android, no Room, no
Compose, no coroutines, no I/O — so it is unit-testable on a plain JVM and
deterministic: the same entries always produce the same insights.

| Type | Responsibility |
|---|---|
| `DiarySnapshot` | Input DTO. Plain data, decoupled from `DiaryEntity`. |
| `KeywordExtractor` | Tokenize, drop stopwords and 1–2 char tokens, rank by frequency. |
| `MoodAnalyzer` | Distribution, valence balance, per-day dominant mood, streaks, trend series. |
| `StatisticsEngine` | Entries, words, active days, longest streak, weekday and hour distribution. |
| `PatternDetector` | Recurring keywords across days, day-of-week and time-of-day concentration, tag frequency. |
| `LocalQuestionEngine` | Deterministic reflective prompt per day. |
| `DiaryAnalyzer` | Facade. Takes a `DiarySnapshot`, returns `DiaryInsights`. |

Design rules:

- **No network, ever.** There is no client to configure. `LocalQuestionEngine`
  reads a fixed table.
- **No medical or psychological claims.** Nothing infers a diagnosis, a mood
  disorder, or a cause. Insights are described as counts and co-occurrences:
  "you wrote about *work* on 6 days", never "you seem anxious lately".
- **Honest about small samples.** Every statistic that depends on volume
  returns `null`, not a confident-looking zero, below its minimum sample size,
  and the UI omits the row rather than showing a fake one.
- **Never blocks the UI.** Analysis runs in `viewModelScope` on
  `Dispatchers.Default`.

## 5. Data access

Three new DAO queries, all parameterised, none structural — **schema stays at
version 5**:

| Query | Purpose |
|---|---|
| `search(query, mood, …)` | `LIKE` across title, content and tags. |
| `getAllInRange(fromEpochDay, toEpochDay)` | Calendar month grid. |
| `getAll()` | Analytics snapshot (whole journal, on a background dispatcher). |

`LIKE` metacharacters in user input (`%`, `_`, `\`) are escaped to `ESCAPE '\'`
so a query for `50%` or `a_b` matches literally instead of turning into a
wildcard.

FTS was rejected deliberately: it needs a migration, a trigger-managed shadow
table, and `MATCH` query syntax. A `LIKE` scan over a personal journal's worth
of rows is imperceptible and needs no schema change.

## 6. Screens

| Screen | Route | Contents |
|---|---|---|
| Search | `diary/search` | Field, mood filter, relevance-ordered results with match highlighting. |
| Calendar | `diary/calendar` | Month grid, mood-tinted day dots, selected-day previews, "Write on this day". |
| Insights | `diary/insights` | Stats band, 7-day mood chart, recurring themes, streaks, reflection prompt. |
| Today | `diary` | Existing, plus mood + title in the composer. |
| Timeline | `timeline` | Existing. Unchanged. |

Entry points from the Diary day header: search and calendar as icon actions,
insights as a band above the list.

## 7. Editor

- **Mood selector** — 8 discs, horizontally scrollable, current selection
  ringed in violet with the pastel halo behind it. Tapping the selected mood
  again clears it, so mood is genuinely optional.
- **Title field** — optional, single line, above the writing surface.
- Both sit inside the existing single scroll viewport, so `imeNestedScroll`,
  the caret-into-view behaviour, and the `BasicTextField` selection theming are
  untouched and still apply to the new fields.
- The 1000-character limit still applies to the body only. Title is capped at
  120 characters, tags at 32.

## 8. Privacy

Unchanged and load-bearing:

- No `INTERNET` permission is declared.
- Analysis reads rows already on the device and returns numbers. Nothing is
  written anywhere else.
- No diary text, title, tag or mood value is logged. The analyzers return
  aggregate models only, so there is nothing to log even by accident.
- Insights screen states that the figures are computed on-device.

## 9. Verification

`gradle :app:testDebugUnitTest :app:compileDebugKotlin` — 231 tests at the
cleanup baseline, plus new unit tests for every analyzer and for the `LIKE`
escaping. Room migration tests unchanged and still passing: the schema is
untouched.

## 10. Known limitations

- Search is a `LIKE` scan, not an index. Correct and fast at journal scale; a
  real index is not worth a migration here.
- `KeywordExtractor` ranks by raw frequency, so a word repeated often in one
  entry can outrank a word spread across many. `PatternDetector` exists
  precisely to cover the spread-across-days case.
- Streaks count days with at least one entry, not days with a mood set, since
  mood remains optional.