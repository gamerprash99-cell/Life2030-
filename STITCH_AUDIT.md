# Diary × Stitch alignment — audit and root-cause report

**Repository:** `/home/LIFE-2026` · branch `feat/stitch-timeline-diary`
**Stitch reference:** project `2728851852731597429`, "Serene Lavender Mindful Journal"
**Date:** 2026-09-27

> A separate file from `AUDIT_REPORT.md`. That report is a whole-codebase audit
> taken on `feat/diary-stitch-redesign` @ `26936bd` and still describes the
> database as v1 with no migrations; the database is now **v5** with four
> migrations. It is left untouched rather than silently rewritten.

---

## 1. What was already correct

The Diary is not a mock-up with a data layer bolted on. The full
`Compose → ViewModel → Repository → Room` path was in place and working, along
with real device media: `MediaStorage` (app-private files), `DevicePhotoImporter`,
`DiaryAudioRecorder`, `DiaryAudioPlayer`, and `DateTimeUtils` for day maths.
Photos, voice notes, places, moods and tags are all serialised into the existing
`attachmentsJson` column, so the media story needed no schema change. The
post-save confirmation is emitted only after a committed write, and the composer
stamps its minute on open rather than on save. None of that was rebuilt.

The defects below are gaps *inside* an otherwise sound architecture.

## 2. Root causes

### 2.1 Blocking file IO on the main thread — `DiaryRepository` (P0)

Room moves *its own* suspend DAO calls onto its query executor. The Kotlin
between them does not move with them. `delete`, `updateEntry` and
`removeAttachment` each decode a JSON attachment list and call
`MediaStorage.deleteIfExists`, which is a blocking `File.delete()`. Every caller
is a `viewModelScope` — that is, `Dispatchers.Main`. So one tap on "remove this
photo" unlinked a file on the UI thread, in the middle of a Compose frame.

**Why it happened:** each function relied on Room's dispatcher switch to cover
work that Room knows nothing about. The threading contract was never stated
anywhere, so each author reasonably assumed the `suspend` covered the body.

**Fix:** `DiaryRepository` now owns the switch — every write does its row reads,
JSON work and file unlinking inside `withContext(io)`. Done in the repository
rather than at the call sites so all four are fixed at once and the contract
lives with the layer that owns the storage. `io` is injectable so tests can drive
storage work on a test scheduler instead of racing a live thread pool.

### 2.2 Delete racing the screen that requested it — `DiaryDetailViewModel` (P0)

`MemoryDeleteDialog`'s confirm handler calls `viewModel.delete(entryId)` and then
`onBack()` immediately. Popping the detail destination clears its back-stack
entry, which clears the `ViewModelStore` entry, which cancels `viewModelScope` —
and the delete was running in that scope. The request to delete was competing
with its own cancellation, and it loses whenever the SQLCipher-encrypted commit
is slower than the navigation transition.

**Why it happened:** `viewModelScope` looks like a durable home for work, but it
is scoped to the ViewModel's lifetime, and this ViewModel's lifetime ended on the
very next line.

**Fix:** the repository call is wrapped in `withContext(NonCancellable)`, so once
the user has asked for the deletion it is honoured. This is the one place
`NonCancellable` is appropriate: a user-initiated, already-accepted destructive
request. It was deliberately *not* added to `DiaryEditorViewModel.save()` — the
composer's ViewModel outlives the overlay (dismissing it only flips `showEditor`),
so there is no cancellation race to close there, and it would not help against
process death anyway.

### 2.3 `SnackbarHostState` with no host — `DiaryDetailScreen` (P0)

The screen created a `SnackbarHostState` and called `showSnackbar("Memory
copied")` after Copy, but never composed a `SnackbarHost`. A `SnackbarHostState`
is only a channel; with no host, `showSnackbar` suspended until its (never
arriving) dismissal and nothing was ever drawn. Copy is the one detail action
with no effect of its own, so a user who tapped it got no evidence it had
happened.

**Fix:** a `SnackbarHost` is now composed at the bottom of the page, inset
clear of the navigation bar and the FAB clearance.

### 2.4 Add-photo unreachable on a memory with no photos — `DiaryDetailScreen` (P1)

The photo strip was gated on `if (photos.isNotEmpty())`. The strip *contains the
"add" tile*, so gating it on already having photos removed the only route to
adding one: a memory written without photos could never get any from its own
detail page. The affordance was absent rather than present-and-empty.

**Fix:** the strip is rendered unconditionally. A memory with no photos now shows
the add tile on its own. `onAdd` opens the same real composer the day view uses,
so the photo is persisted through the normal path — no separate write route.

### 2.5 Save button offering an action that was refused — `DiaryEditor` (P1)

The composable re-derived its own enablement as `content.isNotBlank() && !saving`,
a *weaker* rule than `DiaryEditorState.canSave`. Two of the ViewModel's guards
were missing from the button: the row still loading (`isLoading`, where a save
would insert a duplicate instead of updating) and a take still recording. In
both cases the pill rendered fully enabled and the tap hit an early `return` in
`save()` — a live control that silently does nothing, which is worse than an
honestly disabled one.

**Fix:** the `saving: Boolean` parameter is replaced by `canSave: Boolean`, taken
straight from the state, so there is one authority instead of a weaker
duplicate. Each guard in that property is now also a promise about what the user
sees, and is pinned by a test.

### 2.6 Content under the status bar — both diary roots (P1)

`MainActivity` calls `enableEdgeToEdge()`, and the only `Scaffold` in the
hierarchy is the app's bottom-bar one, so the `NavHost` receives a bottom inset
and no top inset. `ProfileScreen` and `DiaryEditorOverlay` both applied
`statusBarsPadding`/safe-drawing insets themselves; the diary day view and detail
page did not. Their back buttons and date lines rendered underneath the clock.

**Fix:** both diary roots now consume the top inset, matching what the composer
and Profile already did.

### 2.7 24dp touch target — `DiaryPhotoTile` (P1)

The remove chip on a photo was a 24dp clickable box, half the 48dp minimum, on
the control a user reaches for when tidying up a memory.

**Fix:** the touch target is now the full 48dp with the 24dp circle nested and
corner-aligned inside it, so the drawn size and position are unchanged.

### 2.8 One colour doing two jobs — `Color.kt` and 7 screens (P2)

`DiaryInkViolet` (`#21005D`) served as both the heading ink *and* the colour of
every actionable control: the FAB, the "+ Memory" action, the detail actions, the
add-photo/add-voice/add-location affordances, the selected mood and the selected
day. A near-black heading colour on a button reads as disabled, so the Diary's
single most pressable control looked inert.

**Fix:** Stitch's primary `#6C47EB` is introduced as `DiaryActionViolet` and used
for anything pressable; `DiaryInkViolet` is retuned to Stitch's heading indigo
`#211A44` and used only for text and the timeline spine. The rule is now legible
from hue alone: *pressable is violet, readable is ink*. Also in this pass — the
FAB becomes a white `Icons.Filled.Add` on the violet (it was a `Text("+")` glyph
on near-black); "+ Memory" and the saved-sheet primary gain Stitch's full-pill
treatment; the selected-mood ring goes to Stitch's 2dp `#6C47EB`; the Save pill
becomes white-on-violet with the pale disabled pill; tag chips use Stitch's
`#493D7B` on lavender.

## 3. Deliberately not changed

- **`LifeOSSpacing.screenPadding` stays 24dp.** Stitch's canvas margin is 20dp,
  but this token is shared by every screen; changing it would restyle Home,
  Tasks, Habits, Expenses, Notes and Capture, which is outside the Diary. A 4dp
  delta is imperceptible, and the alternative is an unrequested app-wide change.
- **The timeline stays an editorial, cardless stream.** Stitch's component notes
  describe white cards. The cardless "quiet paper" direction is the established
  Diary design and rebuilding the list is a redesign, not a repair; functional
  correctness outranks cosmetic similarity.
- **`DiaryEntity.title` stays nullable and unused by the editor.** The design has
  no title field, so no new column and no migration.
- **A wrong comment was corrected, not a behaviour changed.**
  `DiaryEditorOverlay` claimed it "intentionally covers the bottom bar". It does
  not — the `NavHost` is already inset by the outer `Scaffold`. The comment now
  describes the real arrangement and the real inset handling.

## 4. Stitch reference that could not be read

The Stitch HTML download URLs returned zero-byte files via `curl`, and `webfetch`
rejected them with `StatusCode: non 2xx status code (400)`. The design system's
`designMd` guidance, the theme tokens and the per-screen metadata *were* readable
and are what the colour and spacing decisions above are based on. The raw
`htmlCode`/CSS was **not** inspected, and no claim is made about it.

## 5. Verification

See `UPDATE.md` for the commands and their results in this change's entry. The
two riskiest fixes are each pinned by a test that was confirmed to **fail** when
the fix was reverted:

| Fix | Test | Without the fix |
|---|---|---|
| 2.1 off-main storage | `DiaryRepositoryTest` — `deleting an entry does no storage work on the calling thread` | fails |
| 2.2 delete survives close | `DiaryDetailViewModelTest` — `a delete is honoured even though the screen that asked for it was closed mid-flight` | fails |
