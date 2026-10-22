# LifeOS — Product Requirements Document (PRD)

> **App name:** LifeOS · **Platform:** Android (native)
> **Application ID:** `com.lifeos.app`
> **Doc status:** v1.0 — baseline for the full-app redesign
> **Source spec:** `life os .txt` (Master Product Specification, 62 sections)

---

## 1. Product Identity

| Field | Value |
|---|---|
| Name | LifeOS |
| Type | Personal Life Management Application (an Android app — **not** an operating system) |
| Tagline | *Capture your life. Organize your life. Understand your life.* |
| Promise | *"Your life. Your data."* |
| Platform | Android, Kotlin + Jetpack Compose, single `:app` module |
| Model | Offline-first, local-data-first, private-by-design |

LifeOS is one connected application for capturing, organizing, tracking,
understanding and preserving personal life. It must **not** feel like six or
seven mini-apps bolted together; it must feel like one product connected through
**date, time, tags, timeline, relationships and LifeOS AI**.

---

## 2. Problem & Vision

### 2.1 Problem
Personal life is scattered across a notes app, a to-do app, a habit tracker, an
expense tracker, a journal, a photo roll and a cloud AI chatbot. Each holds a
fragment. None can answer *"what was my life actually like last month?"* and all
of them want the user's data on a server.

### 2.2 Vision
A single private "second brain" that:
1. makes **capturing a moment take seconds**,
2. **connects every module** through one Life Timeline,
3. turns raw stored data into **honest, on-device insight**, and
4. keeps **the user in full control of their data** — locally stored, exportable,
   and never uploaded without explicit, per-category consent.

### 2.3 Differentiator
The value is the **connection between features**, not the feature count:

```
Photo → Life Capture → Timeline → Monthly memory count → Insights → LifeOS AI
Note  → AI detects tasks → user approves → Task → completed → Timeline → productivity insight
Voice → speech-to-text → AI organizes → Diary → mood+tags → Timeline → monthly summary
Expense → category → monthly total → spending insight → LifeOS AI
```

---

## 3. Core Principles (non-negotiable)

1. LifeOS is an **Android app**, not an OS.
2. **User data belongs to the user.**
3. **Local storage is the default.**
4. **Offline-first** — every core feature works in airplane mode.
5. No personal data leaves the device without awareness + permission.
6. AI respects **permission-based data access**.
7. AI never **silently modifies** important user data.
8. AI diary entries must be **reviewable** before saving.
9. AI-created tasks must be **reviewable** before creation.
10. Destructive actions require confirmation; the most serious require typing
    `DELETE`.
11. No hardcoded statistics or fabricated user data — everything is
    database-backed.
12. Modular features; fast, responsive UI.
13. Do not overload Home; **Capture must be extremely fast**.
14. Never trap the user inside LifeOS — export always exists.
15. Request Android permissions **only when the feature is used**
    (*minimum permission, maximum control*).
16. Do not overuse glassmorphism; **readability, accessibility, battery and
    performance come first**.

---

## 4. Target Users & Jobs-To-Be-Done

| Persona | Job-to-be-done |
|---|---|
| The journaler | "Let me capture today before it disappears, and read it back in a year." |
| The student | "Keep study notes, assignments, deadlines and study habits in one place." |
| The self-tracker | "Show me my streaks, spending and mood honestly over time." |
| The privacy-minded | "Keep everything on my phone; let me export and delete it myself." |

Primary user stories:
- *As a user, I can capture a photo/thought/mood in under ~5 seconds from launch.*
- *As a user, I can turn a note into tasks with one review step.*
- *As a user, I can see one day, one week, or one month of my whole life.*
- *As a user, I can ask a question about my own data and get an answer from that data.*
- *As a user, I can lock the app and back up/export everything without a server.*

---

## 5. Information Architecture (5-section navigation)

The primary navigation is **five sections**; Capture is the prominent centre
action.

```
        HOME        NOTES        + CAPTURE        INSIGHTS        LIFE
   (command      (smart        (photo/video/     (life           (timeline,
    center)       notes)        audio/thought/    insights,       calendar,
                                mood)             analytics)      memory hub)
```

| Section | Purpose | Primary contents |
|---|---|---|
| **Home** | "What's happening in my life today?" | greeting + date, Today's Progress, Habits, Tasks, Spending, Latest Memory, prominent `+ CAPTURE` |
| **Notes** | Smart notes, richer than a basic notes app | All / Recent / Pinned / Favorites / Archived / Folders; editor; attachments; AI actions |
| **+ Capture** | Moment capture in seconds | Photo, Video, Audio, Thought, Mood (→ memory → timeline) |
| **Insights** | Understand the data | Monthly dashboard, productivity, habit analytics, expense insights, AI analysis/reports |
| **Life** | The connected personal history | Life Timeline, Life Calendar, Search, and entry points to Tasks / Habits / Expenses / Diary / Memories |

**Track modules** (Tasks, Habits, Expenses) and the **Diary** reach the user
through Home quick access and the Life hub, keeping the bar at five.

---

## 6. Functional Requirements

Priority key: **P0** = first redesign release, **P1** = next, **P2** = later.

### 6.1 Home Dashboard — P0
- FR-H1: Dynamic greeting + full date (e.g. *Good morning* · *Saturday, 15 Aug 2026*).
- FR-H2: **Today's Progress** — a live percent from real completed tasks + habits.
- FR-H3: **Today's Habits** — real habits with completion state.
- FR-H4: **Today's Tasks** — real tasks, toggleable inline.
- FR-H5: **Today's Spending** — real sum of today's expenses.
- FR-H6: **Latest Memory** — most recent capture/memory with time.
- FR-H7: Prominent **`+ CAPTURE`** affordance.
- FR-H8: IA prioritises *morning* (tasks, habits, calendar, capture) vs
  *evening* (unfinished tasks, habit completion, spending, diary reminder,
  daily review) — same data, reordered by time of day.
- FR-H9: Home never shows fabricated numbers; empty states are honest.

### 6.2 Smart Notes — P0
- FR-N1: Create text/rich notes: title, paragraphs, bold, italic, underline,
  headings, bullet/numbered lists, checklists, highlight, links, tags.
- FR-N2: Attachments: photos (multiple), videos, audio, documents/files.
- FR-N3: Organization: All, Recent, Pinned, Favorites, Archived, Folders
  (default folders: College, Projects, Ideas, Personal, Finance, Other).
- FR-N4: Note actions: create/rename folder, move, pin, favorite, archive,
  delete (→ trash), restore, search, sort (recently edited/created,
  alphabetical, oldest, newest).
- FR-N5: **AI Smart Notes** (see §6.8): summarize, generate title, organize,
  rewrite, grammar, shorten/lengthen, extract points, **extract tasks**,
  create checklist, generate ideas, explain, structure messy thoughts, study
  questions, action items.

### 6.3 Tasks — P0
- FR-T1: Fast create: launch → `+` → type → save.
- FR-T2: Fields: title, description, date, time, priority (High/Med/Low),
  category, reminder, repeat, notes, attachments.
- FR-T3: Recurring (daily / weekly set / monthly), scheduled, reminder-based.
- FR-T4: Overdue handling: Reschedule / Keep for tomorrow / Delete.
- FR-T5: **AI task extraction** from notes, diary, voice, captured thoughts —
  always with a review step before creation.

### 6.4 Habits — P0
- FR-B1: Create habits (name, icon, category, frequency daily/weekly/custom,
  goal, reminder, start date).
- FR-B2: Analytics: current streak, longest streak, completion %, missed days,
  weekly/monthly progress, total completions, personal records.
- FR-B3: GitHub-style **heatmap** (weekly/monthly/yearly) with intensity levels
  Missed / Partial / Completed / Exceptional.

### 6.5 Expenses — P0
- FR-E1: *Record spending in seconds*: category grid → amount → save.
- FR-E2: Categories: Food, Cafe, Shopping, Travel, Entertainment, Education,
  Bills, Health, Subscriptions, Other.
- FR-E3: Optional fields: date, time, merchant, payment method (Cash/UPI/Card/
  Bank/Other), note, tags. Never force unnecessary input.
- FR-E4: Insights: today/week/month totals, category breakdown, biggest
  category, daily average, trends, previous-period comparison, highest expense,
  most frequent category.
- FR-E5: **AI expense analysis** — claims only from stored data; never invents
  financial information.
- FR-E6: Optional automatic capture of payment notifications → expenses
  (on-device, opt-in, off by default).

### 6.6 Personal Diary & Moods — P0
- FR-D1: Diary is **emotional/reflective**, different from Notes. Fields: date,
  time, title, text, mood, tags, photos, videos, audio, optional location.
- FR-D2: Mood palette: Great, Happy, Calm, Normal, Sad, Angry, Stressed, Loved,
  Excited.
- FR-D3: **AI Diary**: voice → text → AI-structured entry; user must
  **edit / regenerate / save** (never auto-saved). AI mood/tags are suggestions.
- FR-D4: Mood data feeds Life Insights. Analysis must **not** be presented as
  medical or psychological diagnosis.

### 6.7 Life Capture, Timeline & Calendar — P0
- FR-C1: Capture types: Photo, Video, Audio, Thought, Mood, (optional Location).
- FR-C2: A capture can carry photo/video/audio + caption + mood + tags +
  location + date + time, and becomes part of the Life Timeline automatically.
- FR-C3: **Life Timeline** merges tasks, habits, expenses, diary, notes and
  captures by date/time into one scrollable history.
- FR-C4: **Life Calendar** shows per-date counts (tasks completed, habits,
  expenses, diary, memories, notes, photos) and opens the full day.

### 6.8 LifeOS AI (central assistant) — P0/P1
- FR-A1: One central assistant (not one per module). Understands only permitted
  data. Answer questions: *How was my August? How much did I spend on food? What
  habits am I struggling with? What were my most productive days? Show my
  unfinished tasks. Summarize my last 7 diary entries. What did I do on 5 Aug?*
- FR-A2: **AI context permissions** with four levels: L1 current content,
  L2 selected content, L3 selected modules, L4 full LifeOS (explicit opt-in).
- FR-A3: **AI privacy**: prefer on-device processing. If external processing is
  needed, show exactly what will leave the device, with Cancel / Allow.
  Never secretly upload.
- FR-A4: **AI actions** (not just answers): create tasks from a note, etc. User
  approves; user can edit before creation.
- FR-A5: Interactive response cards (e.g. *YOUR FOOD SPENDING ₹6,450, 38% …*
  with a `VIEW EXPENSES` action).
- FR-A6: AI never invents data and distinguishes fact from interpretation.

### 6.9 Search & Tags — P0/P1
- FR-S1: One **global search** across notes, diary, memories, photos, expenses,
  audio; grouped results with counts; opens the item.
- FR-S2: Eventually supports text, titles, tags, categories, dates, metadata,
  audio transcripts, optional semantic search.
- FR-S3: **Smart tags**: default tag set + user tags + AI-suggested tags the
  user must add explicitly.

### 6.10 Insights & Reviews — P0/P1
- FR-I1: Monthly "YOUR MONTH" dashboard: task completion %, habit consistency,
  money spent, diary entries, memories captured, best habit, biggest spending
  category, most productive day.
- FR-I2: Productivity: tasks completed/overdue, completion rate, most productive
  days/times, habit consistency, weekly/monthly productivity, improvement vs
  last period. **All from real data.**
- FR-I3: **Daily Review** (end of day): tasks, habits, spending, memories,
  diary status + a quick "How was your day?" mood + optional note.
- FR-I4: **Weekly Review** with an AI summary.
- FR-I5: **Monthly Life Report** with an AI summary.
- FR-I6: **Achievements** — optional, premium and subtle (streaks, task/diary/
  memory/note milestones). Never childish.

### 6.11 Privacy, Security, Backup — P0
- FR-P1: App lock: PIN (and/or biometric), auto-lock timeout, hide sensitive
  notification previews, encrypted local storage.
- FR-P2: **Backup & Restore**: create/restore backup, export/import. Backup file
  e.g. `LifeOS_Backup_15_Aug_2026.lifeos`. Must honestly state that local storage
  cannot guarantee survival through device failure, factory reset, OS action or
  uninstall.
- FR-P3: **Data export** (complete backup, notes, diary, expense, task, habit,
  media). *Users should never feel trapped inside LifeOS.*
- FR-P4: **Trash / recovery** for recently deleted items, with restore and
  permanent delete.
- FR-P5: Deletion confirmation; permanent deletion requires typing `DELETE`.

### 6.12 Notifications & Reviews — P1
- FR-NT1: Task reminders, habit reminders, diary reminders — each independently
  controllable.

### 6.13 Onboarding — P0
- FR-O1: First launch: Welcome → "Your data is yours" → create LifeOS space →
  optional restore. Permissions requested **contextually, later**, never all at
  once.

---

## 7. Non-Functional Requirements

| Area | Requirement |
|---|---|
| Offline | 100% of core features usable with no network |
| Privacy | No INTERNET permission in this build; no telemetry/analytics; no cloud AI |
| Security | DB encrypted at rest (SQLCipher); Keystore-wrapped passphrase; PIN hashing + lockout |
| Performance | Cold start to Home should not block on DB; lists stay smooth as data grows; capture opens fast |
| Accessibility | Contrast-checked palette; ≥48dp touch targets; TalkBack semantics; large-font and label support |
| Robustness | No destructive DB fallback; explicit Room migrations; data preserved across upgrades |
| Localization | UTF-8; ₹ (INR) money handling by default; scalable strings |
| Battery | Glass effects used sparingly; no background pollers beyond reminders |

---

## 8. Success Metrics

- Time-to-first-capture ≤ ~5 seconds from cold start.
- % of days with at least one capture/memory.
- Notes-to-task AI approval rate.
- 7-day and 30-day diary / habit retention.
- Crash-free sessions ≥ 99.5%.
- Backup/export completion rate (no "trapped" feeling).

---

## 9. Release Phases (maps to spec §58)

| Phase | Scope |
|---|---|
| **1 Foundation** | Project, design system, 5-section navigation, local DB, storage, settings, security foundation |
| **2 Productivity** | Tasks, Habits, Notes, categories, tags, search |
| **3 Personal Life** | Diary, Life Capture, photos/video/audio, Timeline, Calendar |
| **4 Finance** | Expenses, categories, spending dashboard, financial insights |
| **5 AI** | AI infrastructure, AI Notes, AI Diary, task extraction, AI search, Life Insights, permission-based context |
| **6 Privacy & Backup** | App lock, biometrics, encryption, backup/restore/export/import, data management |
| **7 Advanced** | Semantic search, smart timeline, personal patterns, AI monthly/weekly reports, smart suggestions |

---

## 10. Out of Scope (for now)

- Any server, account system, or cloud sync.
- Cloud AI fallback without explicit per-request consent.
- Medical/psychological diagnosis or assessment.
- iOS / web clients.

---

## 11. Key Product Rules (the 25 from spec §59)

LifeOS is an app not an OS · user owns data · local-first · offline-first · no
upload without awareness · AI respects permissions · AI never silently edits
data · AI diary/tasks reviewable · destructive actions confirmed · serious ones
need `DELETE` · no hardcoded stats · real DB-backed data · modular · fast UI ·
don't overload Home · capture is extremely fast · modules interconnected ·
backup/export are first-class · never trap the user · privacy understandable ·
permissions only when required · don't overuse glass · keep readability/a11y ·
one unified product, not mini-apps.

---

## 12. Final Experience Statement

> *"This app helps me understand the story of my life while keeping me in
> control of my data."*

Capture your life. Organize your life. Understand your life.
Private by design. Offline by default. Built around you.

*End of PRD.*
