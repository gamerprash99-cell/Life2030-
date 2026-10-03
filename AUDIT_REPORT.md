# LifeOS (LIFE-2026) — Full Codebase Audit Report

**Repository:** `/home/LIFE-2026` · remote `https://github.com/gamerprash99-cell/Life2030-.git`
**Checked out:** branch `feat/diary-stitch-redesign` @ `26936bd` (32 commits, 2 ahead of `origin/main`)
**Audit type:** READ-ONLY. No code, schema, tests, CI, or docs were modified. Findings below are verified
against source unless labelled "Needs verification". Secrets are redacted as required.

---

## 1. Executive Summary

LifeOS is a genuinely offline-first, on-device personal-life journaling app (notes, tasks, habits, expenses,
diary, captures, AI-style insights, app lock) built on a hand-wired dependency-injection container over a
single SQLCipher-encrypted Room database. The architecture — UI → ViewModel → UseCase → Repository → DAO —
is consistent and almost uniformly followed. The three strongest qualities:

- **Offline-first is real and verified:** no `INTERNET` permission in source or merged manifest; no OkHttp/
  Retrofit/Ktor/URL/WebView; the "AI" layer is a local rule/analysis engine.
- **Security posture is above typical indie-app level:** PBKDF2-hashed PIN (`PinHasher`), escalating lockout,
  SQLCipher encryption at rest keyed by an Android-Keystore–wrapped per-install passphrase, constant-time
  compares.
- **Discipline:** Room at version 1 with `exportSchema=true`, warm-up of the SQLCipher key derivation off the
  UI thread, layered code, 101 unit tests with 0 failures.

The main weaknesses are **drift between code and the ship-state of the UI/UX**, **duplicated date math** that
can cause real bugs, **unreachable screens** (AI Assistant, Insights) caused by dead Home callbacks, and a
**handful of performance red flags** (full-table reads per day, whole-dashboard recomputation per write, zero
DB indices). The repo is 2 commits ahead of `origin/main` on a feature branch; the local working tree is the
only place these states exist.

---

## 2. Scope & Method

- **Scope:** all 122 Kotlin files under `app/src/main` (~13.5K LOC), `app/src/test` (13 files, 947 LOC),
  Gradle/config/Manifest, and `docs/` (27 markdown docs). Git history reviewed (32 commits, 12 local/remote
  branches).
- **Method:** 8 parallel exploration subagents (usage trace, dead code, duplicates, security/offline,
  performance, testing/docs/git, database/navigation, architecture/Compose), cross-checked by direct
  single-file reads of every flagged symbol, plus a fresh graphify pipeline (AST + semantic extraction)
  producing `graphify-out/graph.json`, `graph.html`, `graph.svg`, `GRAPH_REPORT.md`.
- **Confidence key:** CONFIRMED (author read the code path) / HIGH (two agents + trace agree) / SUSPECTED
  (inference, unverified) / Needs verification.
- **False-positive control:** DI (`ServiceLocator`), navigation (`Screen`, `LifeOSNavHost`), Manifest,
  serialization, Room DAO queries, tests, and Compose previews were checked before any "dead" label.

---

## 3. Repository Snapshot & Git Hygiene

| Area | Result |
|---|---|
| Language | 100% Kotlin (Compose Multiplatform-style UI, single `:app` module) |
| Total Kotlin LOC | 13,551 `app/src/main` + 947 `app/src/test` |
| Commits on HEAD | 32 |
| HEAD | `26936bd` `feat(diary)`: Stitch-inspired journal redesign with local analytics and connection radar |
| Branch | `feat/diary-stitch-redesign` (2 commits ahead of `origin/main`) |
| Uncommitted | `graphify-out/` (this audit's graph artifacts, intentionally untracked) |
| CI | `.github/workflows/android-build.yml` |

**Git observations (CONFIRMED):** branch naming is consistent (`feat/…`, `fix/…`, `redesign/…`); history shows
PR-merge hygiene (merge commits #4/#5/#6). Note: `origin/fix/audit-hardening` exists remotely — a hardening
branch never merged to `main`, suggesting past audit work was left unfinished.

---

## 4. Technology Stack & Offline-First Verification

| Claim | Evidence | Verdict |
|---|---|---|
| No network permission | `INTERNET` absent from `AndroidManifest.xml` and merged manifest | CONFIRMED — genuinely offline |
| No HTTP client / cloud SDK | No OkHttp/Retrofit/Ktor/`java.net.URL`/WebView/Firebase/analytics in Gradle or source | CONFIRMED |
| On-device "AI" | `core/ai/AiRepository` + `core/intelligence/*` = local rule engines (`MoodAnalyzer`, `NoteTextAnalyzer`, `LocalQuestionEngine`, `ReportGenerator`) | CONFIRMED |
| Storage | Room + SQLCipher (`net.zetetic.database.sqlcipher`) | CONFIRMED |
| DI | Hand-rolled `ServiceLocator` + `LocalServiceLocator` CompositionLocal | CONFIRMED |

**Takeaway:** offline-first is a real property of the code, not a documentation claim.

---

## 5. Dead Code — Confirmed

| Priority | File | Symbol | Evidence | Confidence | Recommendation |
|---|---|---|---|---|---|
| HIGH | `app/src/main/java/com/lifeos/app/core/ai/AiModels.kt:34-37` | `AiResponseCard` (sealed) + nested `ExpenseSummary`, `HabitSummary`, `TaskExtraction`, `MessageItem` | Zero references outside the file (checked all 122 files); `AiResult` is the live type | CONFIRMED | Delete (verify `AiResult` carries all needed payloads first) |
| HIGH | `app/src/main/java/com/lifeos/app/core/ai/AiRepository.kt:35-37` | `fun draftDiaryEntry` | No caller anywhere (`grep draftDiaryEntry` → only self + engine def) | CONFIRMED | Delete; diary drafting isn't surfaced |
| HIGH | `app/src/main/java/com/lifeos/app/core/ai/AiRepository.kt:47-59` | `suspend fun chat(history)` + its `ChatMessage` interplay | `grep "\.chat("` → no callers; AI screen that would use it is unreachable (see §12) | CONFIRMED | Delete or wire the AI screen |
| HIGH | `app/src/main/java/com/lifeos/app/core/intelligence/LifeOSIntelligenceEngine.kt:97-107` | `fun draftDiaryEntry` | Sole caller is the dead `AiRepository.draftDiaryEntry` → transitively dead | CONFIRMED | Delete with §5 row 2 |
| MED | `app/src/main/java/com/lifeos/app/ui/navigation/Screen.kt:32` | Companion `bottomNavItems` | `LifeOSBottomBar.kt:68` defines its own `internal val bottomNavItems`; `Screen.kt`'s is unused | CONFIRMED | Delete one copy (see §6) |
| MED | `app/src/main/java/com/lifeos/app/ui/ai/AiAssistantScreen.kt` | Whole screen + `AiAssistantViewModel` | Route `ai_assistant` registered (`LifeOSNavHost.kt:148`) but reachable from nowhere (dead Home callback, §12) | CONFIRMED (reachability), SUSPECTED (intent restore) | Re-wire via Home/Profile or remove with route |

---

## 6. Duplicate Logic

| Priority | File | Symbol | Evidence | Confidence | Recommendation |
|---|---|---|---|---|---|
| HIGH | `app/src/main/java/com/lifeos/app/ui/insights/InsightsScreen.kt:81-82` | Raw `epochDay * 86_400_000L` millis math | `DateTimeUtils.startOfLocalDayMillis` (`:84-89`) exists precisely to replace this; raw math breaks non-UTC + DST days | CONFIRMED | Replace with `DateTimeUtils.startOfLocalDayMillis/endOfLocalDayMillis` |
| HIGH | `app/src/main/java/com/lifeos/app/domain/usecase/BuildTimelineUseCase.kt:114-117` | Private `epochMillisToMinutesOfDay` | Performance duplicate of `DateTimeUtils.minutesToLocalTime` + `LocalTime.hour*60+minute` | CONFIRMED | Use shared helper |
| MED | 7+ call sites (`InsightsScreen`, `TasksScreen`, `DiaryScreen`, `LifeOSIntelligenceEngine`, use-cases…) | `now.hour * 60 + now.minute` inline | `DateTimeUtils.nowMinutesOfDay()` (`:23-26`) is the canonical helper | CONFIRMED | Delegate to util; single source of truth |
| MED | `Screen.kt:32` vs `LifeOSBottomBar.kt:68` | `bottomNavItems` definitions | Two lists of the same 5 items drift independently | CONFIRMED | One source (e.g. in `Screen`), consumed by bar |

**Note:** `DateTimeUtils` itself is a well-formed consolidation point — the duplication is callers bypassing it.

---

## 7. Unused Components

| Priority | File | Symbol | Evidence | Confidence | Recommendation |
|---|---|---|---|---|---|
| HIGH | `app/src/main/java/com/lifeos/app/ui/home/HomeScreen.kt:79,80,83,86` | `onOpenAiAssistant`, `onOpenNotes`, `onOpenInsights`, `onOpenSettings` | Declared params, never invoked in body (only `onOpenSearch`, `onOpenTasks`, `onOpenHabits`, `onOpenDiary`, `onOpenExpenses`, `onOpenProfile` are called) | CONFIRMED | Remove params + `LifeOSNavHost.kt:70-77` wiring, or wire real entry points |
| HIGH | `LifeOSNavHost.kt:70,71,74,77` | Nav lambdas for the above | Dead because HomeScreen never calls them | CONFIRMED | See above |
| MED | `app/src/main/java/com/lifeos/app/ui/home/DiaryPreviewSection.kt` (if present) | Older diary preview | Superseded by Stitch diary card in current Home | Needs verification | Confirm then delete |
| MED | `app/src/main/java/com/lifeos/app/docs of AI` — `docs/11_AI_SYSTEM.md` etc. | Cloud-era docs | Docs describe network-backed AI / API key flow that does not exist in code | CONFIRMED (doc/code mismatch) | Update docs to offline reality (see §16) |

---

## 8. Unnecessary Complexity

| Priority | File | Symbol | Evidence | Confidence | Recommendation |
|---|---|---|---|---|---|
| MED | `LifeOSNavHost.kt:150-171` | Two `when` blocks (LDI dispatch) | Identical navigation `when` structures duplicated (~lines 150-160 and 161-171) | CONFIRMED | Merge into single dispatch table |
| MED | `core/ai/AiRepository.kt` | `AiResult`/`AiResponse`/`AiModels` trio | Three result-model families for one local engine | CONFIRMED | Consolidate to `AiResult` |
| LOW | `core/ai/runtime/LifeVoiceInputController.kt` | Runtime voice/object-recognition hooks | High-degree node but no UI consumer visible | SUSPECTED | Review; if unused, remove or finish wiring |
| LOW | `domain/model/Categories.kt` vs `ExpenseCategories` | Two category enums | Overlap in expense category modelling | Needs verification | Merge into one domain enum |

---

## 9. Architecture Consistency

**What is consistent (CONFIRMED):** the UI → ViewModel → UseCase → Repository → DAO flow is followed across
Notes, Tasks, Habits, Expenses, Diary, Captures, Home, Insights. `ServiceLocator` constructs one shared
`LifeController`/`LifeOSIntelligenceEngine`; ViewModels get typed repos via `LambdaViewModelFactory`.
Gradle + KSP Room + `exportSchema` are sane.

**Inconsistencies:**

| Priority | Finding | Evidence | Confidence |
|---|---|---|---|
| MED | `HomeScreen` is a monolith | 400+ line composable with sections inline instead of one-per-file (contrast with Diary's section-based split) | CONFIRMED |
| MED | Dead top-level navigation wiring | NavHost wires 4 callbacks that Home never triggers (§7) — architecture permits but the UI envelope is stale relative to the "ship state" | CONFIRMED |
| LOW | Two folder-based data views | Notes use folder grouping while Diary uses `epochDay` time grouping; both fine, but note duplication of day-boundary logic | CONFIRMED |
| LOW | Opaque number types | `Long` epochDay / `Int` minutes-of-day used bare; no value-class wrappers → the §6 math bugs are type-forgiving | CONFIRMED |

---

## 10. Database (Room) Analysis

Verified facts from `AppDatabase.kt`:
- Single `AppDatabase` @version **1**, `exportSchema = true`, 7 entities, DAOs for each.
- **No `@Index` anywhere** → every day-scoped query is a full table scan (ties to §15).
- **No `@Migration`** — schema frozen at v1; `fallbackToDestructiveMigration` deliberately NOT used (correct),
  so v2 must ship explicit migrations (Room will crash otherwise).
- Enums persisted by `.name` via `Converters` (brittle to renames; acceptable at v1).
- SQLCipher integration is done right: `System.loadLibrary("sqlcipher")`, `SupportOpenHelperFactory(passphrase)`,
  Keystore-wrapped random passphrase (`DatabasePassphraseProvider`), background `warmUpOpen`.

**Scores:** correctness of encryption 9/10 · schema/migration readiness 4/10 · index strategy 2/10.

---

## 11. Offline / Privacy / Encryption

- **At rest:** SQLCipher via Keystore-wrapped per-install passphrase — strong; the DB file is encrypted.
- **Backup:** `BackupRepository.exportToFile` serializes the **entire DB to plaintext JSON** and writes with
  `file.writeText(text)` (`BackupRepository.kt:60-64`). No encryption component on export.
  | Priority | Finding | Confidence | Fix |
  |---|---|---|---|
  | HIGH | Plaintext backup = full data leak if exported to cloud/messaged apps | CONFIRMED | Encrypt backup (reuse passphrase-Keystore or backup-specific key); note plaintext is symmetric with the documented "restore" contract |
- **Transit:** zero network surface — nothing leaves the device.
- **Memory/log hygiene:** no credential logging observed in source; PIN never stored plaintext.
- **Permission surface:** minimal (store/camera/mic gated by `PermissionManager` with rationale).

---

## 12. Navigation Audit

Routes (17 in `Screen.kt`): Home, Notes, NoteEditor, Search, Tasks, Habits, Expenses, Diary, DiaryDetail,
Capture, Insights, Profile, Settings, AiAssistant, TaskDetail, HabitDetail, Upcoming.

| Priority | Finding | Evidence | Confidence |
|---|---|---|---|
| HIGH | **AI Assistant screen unreachable** | Only nav trigger is `HomeScreen.onOpenAiAssistant` (never invoked) → `LifeOSNavHost.kt:70` never fires; route at `:148` orphaned. No other nav call targets `ai_assistant` | CONFIRMED |
| HIGH | **Insights screen unreachable** similarly | `onOpenInsights` (`HomeScreen.kt:83`) never invoked; only trigger is `LifeOSNavHost.kt:74` | CONFIRMED |
| HIGH | **Notes list practically orphaned** | `onOpenNotes` dead; Notes route is reachable only via `LifeDestination.Notes` LDI dispatch (`LifeOSNavHost.kt:153,164`) — the local-intent engine, not primary UI | CONFIRMED |
| MED | Settings reachable via Profile only | `onOpenSettings` (`:86`) dead on Home; live path is `ProfileScreen` → Settings (`:177`) | CONFIRMED |
| MED | Duplicate `when` dispatch blocks | `:150-160` and `:161-171` duplicate navigation for LDI results | CONFIRMED |
| LOW | Predictive-back handled | `navController`/back handling present (`:12b3586` adds predictive back) | CONFIRMED |

**Net effect:** three screens (AI Assistant, Insights, Notes list) are effectively unreachable from the shipped
UI envelope — the biggest functional-vs-code gap in the app.

---

## 13. UI / Compose Analysis

- **Consistency:** shared design language — `LifeOSTheme` (dark/light), `LifeOSDesignComponents`
  (`LifeOSCard`, section headers, tiles), `GlassCard`, consistent bottom bar (`LifeOSBottomBar`), `LifeOSNavHost`
  scaffold. Stitch-inspired redesign landed for Diary, Capture, and Home.
- **Accessibility:** `Material3` defaults; focus/labels largely via strings; some `Text`/icon-only tiles without
  content descriptions — **Needs verification** for a11y completeness.
- **State mgmt:** `StateFlow` + `collectAsState` in ViewModels; navigation state hoisted to NavHost. Solid.
- **Redesign consistency:** Diary/Home/Capture ship the new Stitch aesthetics; Tasks/Expenses/Insights still on
  older chrome → visual drift across screens (med priority).

---

## 14. Security Audit

| Priority | File:Line | Finding | Confidence | Recommendation |
|---|---|---|---|---|
| HIGH | `SettingsStore.kt:181-186` | **Recovery-answer verification is unthrottled** — infinite local brute force via the security-question path (answer lowercased+trimmed), while PIN path (`:145-179`) has escalating lockout | CONFIRMED | Apply same lockout/backoff to `verifyRecoveryAnswer`; consider constant-time compare |
| MED | `PinHasher` / `SettingsStore` | PIN at-rest is PBKDF2 hash+salt, constant-time compare — GOOD. Verify salt stored in encrypted prefs (not plaintext prefs) | Confirm storage | Keep; document |
| MED | `BackupRepository.kt:60-64` | Plaintext full-data export (§11) | CONFIRMED | Encrypt export |
| LOW | `MainActivity.kt` `AppLockGate` | Auto-lock grace 30 s; lock config change forces re-unlock — good. Recents-screen privacy (FLAG_SECURE) not observed | Needs verification | Add FLAG_SECURE if recents privacy desired |
| LOW | Recovery question strength | Security answer expands attack surface vs PIN-only | CONFIRMED | At minimum throttle (HIGH above), ideally optional |

---

## 15. Performance Audit

| Priority | File:Line | Finding | Confidence | Recommendation |
|---|---|---|---|---|
| HIGH | `BuildTimelineUseCase.kt:32-38` | `noteRepo.observeAll().first()`, ditto tasks & habits → loads **full tables** every day, then filters in Kotlin by `createdAt` | CONFIRMED | Add DAO day-window queries (`WHERE createdAt >= ? AND < ?`), ideally with index |
| HIGH | `GetHomeSummaryUseCase.kt:63-82,162` | Recomputed whole dashboard (habits, notes, tasks, expenses, diary… all streams combined) on **any** write | CONFIRMED | Scope queries to the summary window; combine `combine()` flows instead of full refresh |
| MED | Whole app | **Zero `@Index`** on all 7 tables | CONFIRMED | Index `createdAt`, `epochDay`, `habitId`, completion time columns |
| LOW | `DiaryConnections.build` etc. | Analytics passes read broad history; fine at small scale | CONFIRMED | Time-box when data grows |
| LOW | SQLCipher | Explicit warm-up off-main (`AppDatabase.warmUpOpen`) — GOOD | CONFIRMED | Keep |

---

## 16. Testing / Documentation / Git History

**Testing — STRONG unit layer, NO instrumentation:**
- 101 JVM tests, **0 failures, 0 skips**, 13 files. Inventory: HabitStatsCalculator 18, PinHasher 9, MoodAnalyzer 10,
  RepeatRuleCalculator 10, NoteTextAnalyzer 8, DiaryConnections 8, DateTimeUtils 7, DatabasePassphraseProvider 6,
  BackupSerialization 3, BottomNavItems 3 (verified).
- **Gaps:** no `app/src/androidTest` directory → no Room DAO tests, no migration tests, no navigation tests,
  no Compose UI tests. The two HIGH use cases in §15 have no coverage.

**Documentation — extensive but stale in the AI/network dimension:**
- `docs/00`–`25`, `DESIGN.md`, `DOCUMENTATION_AUDIT.md`, `CHANGELOG.md`, `UPDATE.md` — a genuine 27-file doc tree.
- `docs/11_AI_SYSTEM.md` and related describe a cloud-AI/API-key system that has been replaced by the local
  engine; `README`/`UPDATE` also skew network-era. **Doc–code drift (CONFIRMED).**

**Git history:** 32 commits, consistent conventional prefixes, PR merge commits, meaningful messages. Clean.

---

## 17. Graphyfy Visual Report

Fresh graph rebuilt for HEAD `26936bd` in `graphify-out/` (all artifacts regenerated by this audit):

| Artifact | Path | Content |
|---|---|---|
| Knowledge graph | `graphify-out/graph.json` | 1451 nodes, 5062 edges, 59 communities, 151 hub nodes (degree≥10) |
| Interactive map | `graphify-out/graph.html` | Open in a browser: pan/zoom, click to pin, ring = hub, search box |
| Community diagram | `graphify-out/graph.svg` | Per-community discs + intra-community edges for the 9 largest groups |
| Report | `graphify-out/GRAPH_REPORT.md` | Hub-node tables, community breakdown, per-file node counts, edge-type histogram |

**Graph findings:**
- **Top hubs:** `DiaryEntity`, `LifeOSCard` (design system), `LifeSessionMemory.remember`, `DateTimeUtils`,
  `NoteEntity`, DAOs/Repos → confirms the model + utils + shared-components layer is the true core.
- **Community c989 (525 nodes):** the AI layer dominates one community — indicates the local-intelligence graph
  is dense (fine, but it's the region with the most dead code, §5).
- **Community c1125 (176 nodes):** `LifeModels`/`PermissionManager` runtime scaffold — healthy.
- Edge histogram: imports 2143 · calls 912 · references 869 · method 579 · contains 362; only 6
  `semantically_similar_to` edges (no hidden logic families that would indicate dangerous parallel
  implementations).

---

## TOP TECHNICAL DEBT (ranked)

### Immediate Review (functional/security correctness)
1. Recovery-answer brute-force throttle — `SettingsStore.kt:181-186` (HIGH).
2. Unreachable screens (AI Assistant / Insights / Notes list) — either wire or delete the dead Home callbacks
   (`HomeScreen.kt:79-86`, `LifeOSNavHost.kt:70-77`).
3. Plaintext backup export — encrypt (`BackupRepository.kt:60-64`).

### Refactor Candidates (correctness + perf)
4. Replace raw `epochDay * 86_400_000L` math with `DateTimeUtils` day-boundary helpers (`InsightsScreen.kt:81-82`,
   `BuildTimelineUseCase.kt:114-117`).
5. Day-window DAO queries + `@Index` for timeline (`BuildTimelineUseCase`, `GetHomeSummaryUseCase`) — removes
   full-table scans and per-write recomputation.
6. Deduplicate `bottomNavItems` (`Screen.kt` vs `LifeOSBottomBar.kt`).

### Cleanup Candidates (dead code)
7. Delete `AiModels.AiResponseCard` family, `AiRepository.draftDiaryEntry`/`chat`,
   `LifeOSIntelligenceEngine.draftDiaryEntry`.
8. Merge duplicate `when` blocks in `LifeOSNavHost`.

### Monitor
9. Docs vs code drift on the AI/offline story.
10. DB v2 planning: explicit `@Migration`s required (no destructive fallback is in place — good).
11. Instrumentation test layer (androidTest) ahead of any schema/navigation changes.

---

## Next Steps (non-destructive, phased)

1. **Phase 1 — Quick wins (no behavior change):** add lockout/throttle to recovery-answer path; add DB day-window
   queries and indices; replace raw epoch math with `DateTimeUtils`. Unit tests already cover utils/analyzers.
2. **Phase 2 — Navigation repair decision:** product call on AI Assistant + Insights + Notes list — (a) wire them
   from Home/Profile, or (b) prune routes + dead callbacks + dead AI code together.
3. **Phase 3 — Backup encryption:** encrypt export/import reusing the Keystore-wrapped key; update the restore
   contract + `docs/05_DATABASE.md` + `08_SECURITY.md`.
4. **Phase 4 — Merge/cherry-pick audit work:** reconcile `origin/fix/audit-hardening` against `main` before
   landing `feat/diary-stitch-redesign`.
5. **Phase 5 — Regression harness:** add first `androidTest` (Room + DAO + navigation) before any v2 migration.
6. **Phase 6 — Final architecture pass:** consolidate AI result models, categories, and HomeScreen sections.

*Constraints honored: nothing in the repository was modified; `graphify-out/` is the only new directory (build
artifacts of this audit, untracked).*