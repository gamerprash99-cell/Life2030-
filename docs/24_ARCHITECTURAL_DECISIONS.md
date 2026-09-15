# 24 — Architectural Decision Records

Each ADR below is grounded in either an explicit code comment (quoted
directly) or is clearly marked as inferred reasoning where no comment exists.

---

## ADR-001: No backend server — fully local-first architecture

**Context**: The app needs to store notes, tasks, habits, expenses, and
diary entries somewhere.

**Decision**: All data lives in a local Room/SQLite database on the user's
device. No backend server, no cloud database.

**Alternatives considered**: Not documented in code comments — inferred
only. A cloud-backed architecture (Firebase, custom backend) would be the
typical alternative for this kind of app.

**Reason** (inferred from repeated patterns across the code): Privacy and
simplicity. Multiple files explicitly emphasize local-only data — e.g.
AndroidManifest.xml's comment: "INTERNET is only used for the optional,
user-controlled LifeOS AI feature... All other features... are fully
offline." and BackupRepository.kt's comment: "'Your life. Your data.'... a
complete, human-readable JSON export... written to app-private storage so
it can then be shared/saved by the user explicitly (never uploaded automatically)."

**Consequences**: No multi-device sync, no user accounts, no server costs
or server maintenance burden, strong default privacy posture, but a
significant future architectural project if cloud sync is ever wanted (see
docs/18_ROADMAP.md, FUTURE section).

**Status**: Implemented and consistently followed throughout the codebase.

---

## ADR-002: Manual dependency injection (ServiceLocator) instead of Hilt/Dagger

**Context**: The app needs a way to construct and share repositories, the
database, and the AI client across many screens/ViewModels.

**Decision**: A single hand-written singleton class, core/di/ServiceLocator.kt,
constructs everything once and exposes it via a Compose CompositionLocal.

**Alternatives considered**: Hilt (Google's recommended DI framework for
Android) — explicitly named and rejected in the code's own comment.

**Reason** (direct quote from ServiceLocator.kt):
"Deliberately not Hilt/Dagger — for a scaffold of this size, manual DI is
far less likely to break the build (no KSP/annotation-processor version
coupling) while still keeping every dependency created in exactly one
place. Swap for Hilt later if the team prefers, without touching any
ViewModel signatures."

**Consequences**: Simpler build (already using KSP for Room, so this
avoids stacking a second annotation processor), but no compile-time
dependency graph validation that Hilt would provide, and manual wiring
doesn't scale as gracefully to a very large app. The comment explicitly
frames this as reversible.

**Status**: Implemented. Reversal path is explicitly documented in the code itself.

---

## ADR-003: Timeline is computed live, not stored as its own table

**Context**: The app needs a unified, chronological feed of everything the
user did in a day (Notes, Tasks, Habits, Expenses, Diary, Captures).

**Decision**: domain/usecase/BuildTimelineUseCase.kt queries all six
repositories fresh, for a given day, and merges the results in memory. No
timeline database table exists.

**Alternatives considered**: A denormalized timeline table, updated via
triggers or write-time hooks whenever any of the six source tables changes
— explicitly rejected.

**Reason** (direct quote from BuildTimelineUseCase.kt):
"The Life Timeline... is deliberately NOT its own database table. It is a
real-time aggregation over Notes, Tasks, Habits, Expenses, Diary and
Captures — this is what the spec means by 'connect through Date, Time,
Tags, Timeline, Relationships' rather than duplicating data into a
redundant table."

**Consequences**: The Timeline can never drift out of sync with the source
data (a correctness win), at the cost of doing more work per Timeline view
(six queries merged in-memory rather than one indexed table read) — an
acceptable tradeoff at personal-app data volumes, but worth revisiting if
this app were ever to support very large datasets.

**Status**: Implemented.

---

## ADR-004: AI features are opt-in and every AI-derived write requires explicit user approval

**Context**: The app has an optional AI layer that can suggest tasks,
diary entries, and content edits.

**Decision**: (a) AI calls only happen when the user explicitly taps an
action — never automatically or in the background. (b) No AI output is
written to the database without a separate, explicit confirmation step.

**Evidence this is a deliberate, consistent pattern rather than incidental**:
- AiClient.kt's own comment: "AI calls are made ONLY when the user
  explicitly triggers an AI action... never in the background... Nothing
  this returns is written to the database automatically; every caller must
  route suggestions through an explicit user-approval step."
- NoteEditorScreen.kt: extracted tasks require tapping "CREATE TASKS" in a
  dialog before TaskRepository.createFromAiExtraction() is called.
- DiaryEntity.isReviewed field + DiaryScreen.kt's "Approve" button:
  AI-drafted diary entries are saved with isReviewed = false and require a
  separate confirmation before being treated as reviewed.

**Alternatives considered**: Not documented — inferred only. A more
"seamless" design could auto-save AI suggestions and let users undo them
instead of requiring upfront approval; explicitly not chosen here.

**Consequences**: Slightly more friction for the user per AI interaction,
in exchange for a strong trust/transparency guarantee — the user always
knows exactly what data was AI-generated vs. self-authored.

**Status**: Implemented consistently across every AI touchpoint in the app.

---

## ADR-005: OkHttp used directly instead of Retrofit for the single AI API call

**Context**: The app needs to call exactly one HTTP endpoint (Anthropic's
Messages API).

**Decision**: core/ai/AiClient.kt builds the request and parses the
response manually using OkHttp plus kotlinx.serialization's JsonElement
tree-walking, rather than adding Retrofit.

**Alternatives considered**: Not documented in a code comment for this
specific choice — inferred from the fact that Retrofit does not appear in
app/build.gradle.kts at all, while OkHttp (which Retrofit would sit on top
of anyway) is used directly.

**Reason** (inferred): With only one endpoint ever called, Retrofit's main
value — declarative, typed multi-endpoint interfaces — isn't needed; using
OkHttp directly avoids one extra dependency and one extra layer of
abstraction for a single POST call.

**Consequences**: If the app ever needs to call multiple distinct external
APIs, revisiting this decision in favor of Retrofit would be a reasonable
future discussion.

**Status**: Implemented.
