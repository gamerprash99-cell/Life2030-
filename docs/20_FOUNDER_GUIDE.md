# 20 — Founder Guide

*Written for you, the founder, assuming very limited programming knowledge.*

## Basic concepts explained in plain English

**Frontend** — the part of an app you actually see and touch: buttons,
screens, text. In your app, the frontend *is* the whole app, since LifeOS
has no separate website.

**Backend** — a server, usually running somewhere in the cloud, that stores
data and handles logic that shouldn't live on the user's device (like
processing payments or managing shared data between users). **Your app has
no backend.** Everything happens directly on the user's phone.

**Database** — organized storage for data (like a very structured
spreadsheet). Your app's database lives *inside the user's phone*, not on
any server you control. It's called "Room" (a Google technology) and under
the hood it's SQLite, a very common, simple database format.

**API (Application Programming Interface)** — a way for one piece of
software to ask another piece of software to do something. Your app has one
API connection: to Anthropic (the company behind Claude AI), and only when
the user turns AI features on and enters their own key.

**GitHub** — a website where code is stored, versioned, and where
developers collaborate. Think of it as "Google Drive for code, with a
complete history of every change." Your project isn't on GitHub yet — that's
one of the first things to set up (see `docs/12_GITHUB_WORKFLOW.md`).

**Git** — the actual tool (GitHub is just a website that hosts projects that
use Git) that tracks every change ever made to the code, who made it, and
when — like "track changes" in a Word document, but far more powerful.

**A commit** — one saved snapshot of a change, with a short message
describing what changed. Like saving a version of a document with a note
explaining what you did.

**A branch** — a parallel copy of the code where a developer can safely try
something new without affecting the main, working version. Once it's ready
and tested, it gets merged back in.

**Deployment** — the process of taking finished code and making it actually
available to real users (publishing an app to the Play Store, for example).
Your app has not been deployed anywhere yet.

**Environment variables** — settings/secrets (like passwords or API keys)
that are kept *outside* the code itself, so they're not accidentally shared
publicly. Your app doesn't currently use any at build time — the one "key"
it needs (the AI key) is typed in by each user, inside the running app.

**Authentication** — the process of a user proving who they are (logging
in). **Your app has none of this.** There's no login screen and no
accounts. See `docs/07_AUTHENTICATION.md`.

**An SDK (Software Development Kit)** — a pre-built toolkit that gives
developers ready-made building blocks instead of writing everything from
scratch. Your app uses several: Jetpack Compose (Google's toolkit for
building screens), CameraX (Google's toolkit for camera features), Room
(Google's toolkit for local databases).

**Dependencies** — other people's pre-written code that your app relies on,
instead of your developer writing everything from zero. Listed in full in
`docs/23_DEPENDENCIES.md`.

---

## How LifeOS actually works, using these concepts

Your app is a single Android app (no website, no server). When a user opens
it:
1. The **frontend** (the Compose screens) shows them the Home dashboard.
2. Any data they create (a note, a task, a habit) is saved straight into the
   **database** that lives on their own phone.
3. If they turn on AI features and paste in their own Anthropic **API** key,
   certain buttons (like "Summarize this note") send a small piece of text
   to Anthropic's servers and show back the AI's response — but only when
   the user explicitly taps that button.
4. There is no **backend** and no **authentication** — nothing is uploaded
   anywhere by default, and there's no "your account" concept at all.

---

## "If a developer asks me..." — answers based on your actual repository

**Q: What stack did you use?**
A: Kotlin and Jetpack Compose (Android's current, official native UI
toolkit), a local Room/SQLite database, and Anthropic's API for optional AI
features. No backend, no Firebase, no cross-platform framework — it's a
native Android app.

**Q: Why did you choose this stack?**
⚠️ NOT VERIFIED FROM CODEBASE as a documented rationale — but based on what
was built, the choices favor: (a) being fully local/private by default
(no backend to build or pay for), and (b) using Google's current
recommended tools rather than older or third-party alternatives, which
keeps the codebase approachable for any Android developer to pick up.

**Q: How does your app work?**
A: See the section above — it's a self-contained Android app storing
everything locally, with an optional opt-in AI layer.

**Q: Where is the backend?**
A: There isn't one. All logic runs on the device.

**Q: Where is the database?**
A: On each user's own phone, inside the app's private storage — not a
server you host or pay for.

**Q: How does authentication work?**
A: It doesn't exist yet — there's no login. The only access control is an
optional fingerprint/PIN lock on the whole app, which uses the phone's own
built-in security, not a LifeOS account system.

**Q: How does the AI work?**
A: The user pastes their own Anthropic API key into Settings. From then on,
certain buttons across the app (on notes, in the diary, in a weekly review,
and a chat screen) send a small, specific piece of text to Anthropic and
show the response — nothing is sent automatically or in the background, and
every AI suggestion has to be manually approved before it's saved.

**Q: How do you deploy it?**
A: Not yet — this is a gap (see `docs/13_DEPLOYMENT.md`). There's no
release signing set up and it hasn't been published to the Play Store.
That's real, necessary future work, not something already done.

**Q: How do you manage versions?**
A: The code has a version number (`0.1.0-phase1-6`) but the project isn't
on GitHub yet, so there's no version history to point to. Setting this up
is a near-term step (`docs/12_GITHUB_WORKFLOW.md`).

**Q: How do you test it?**
A: Currently, only by manually clicking through the app — there are no
automated tests yet, though the tools to write them are already installed
and ready to use (`docs/14_TESTING.md`).

**Q: What remains to be built?**
A: See `docs/18_ROADMAP.md`. In priority order: get it actually building
from a clean checkout, set up release signing, encrypt the local database
and the stored AI key, wire up the "restore backup" button, and add
automated tests. Bigger future ideas (accounts, cloud sync, smarter search)
are further out and would be significant new projects, not small additions.

**Q: What are the biggest technical risks?**
A: 1) The database and the AI key aren't encrypted yet, which matters a lot
given this app stores diary entries and personal notes — this should be
fixed before real users trust it with real data. 2) There are no automated
tests, so changes are only verified by hand right now. 3) The app has never
been confirmed to actually compile and run end-to-end in a real build
environment — that first real build is an important checkpoint.

---

## What "complete" actually means for your app right now

Every core feature (Notes, Tasks, Habits, Expenses, Diary, Timeline,
Search, Capture, AI, App Lock, Backup/Export) has real, working code behind
it — this is not a mockup or a prototype with fake buttons. What's
**not** complete is everything *around* the app: it hasn't been built and
run in a real environment yet, it isn't signed for release, it isn't on the
Play Store, and it doesn't have encryption on sensitive data yet. Think of
it as: **the house is fully built inside, but the front door doesn't have a
lock installed yet, and it hasn't had its final safety inspection.**
