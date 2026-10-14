# Redesign pass: working voice capture, a real identity, and motion

Branch `redesign/from-zero-2026` — three commits on top of `90a142b`. Everything
stays native: Kotlin, Compose, Material 3, ViewModels, Room, local media, offline.
The AURA palette is unchanged; this pass fixes a dead feature, restores the spec's
navigation, and replaces the off-brand identity with the app's own.

## 1. Voice capture was dead — now it records

The live Capture hub wired its **Audio** tile to a stub that only printed
*"Voice notes are coming next"*, while the one working recorder lived behind an
unreachable `CaptureSheet`. So the Audio tile never recorded anything.

- The tile now opens `AudioCaptureScreen`, recorded through the **same tested
  `AudioRecorder`** the diary composer already uses (live timer + real amplitude
  from `MediaRecorder`, not a fake animation).
- On stop, `CaptureViewModel.saveAudio` writes a real `CaptureType.AUDIO` row with
  its own measured duration and `mimeType "audio/mp4"`.
- Failures are honest and typed — `START_FAILED` / `TOO_SHORT` / `STOP_FAILED`
  explain what happened and never silently delete the user's take.
- `DiaryAudioRecorder` gained an injectable file factory so both callers share one
  implementation; `CaptureViewModel` now depends on the `LocationProvider`
  interface so it is testable on the JVM; the dead `CaptureSheet.kt` is gone.

## 2. Insights restored to the bottom bar

`Screen.Insights` and its NavHost route are wired again, so the IA reads
**Home · Notes · ＋Capture · Insights · Life** as the spec requires (the old "You"
slot becomes Insights). `InsightsViewModel` reads the on-device
`LifeOSIntelligenceEngine` (exposed via `ServiceLocator`), so every figure comes
from stored rows — no placeholders. `BottomNavItemsTest` asserts the new order.

## 3. Identity: launcher mark, start screen, motion

**Colour only on the mark** — the shape is unchanged.

- **Launcher.** The adaptive icon dropped the generic indigo `#6C63FF` tile for
  the app's own lagoon (deep teal→plum) with a mint→coral ring and a coral
  "captured moment" bead. Added an Android 13+ **monochrome** layer so themed
  icons tint correctly.
- **Start screen.** `LifeOSStartScreen` plays once after Android's native splash:
  the ring traces in (a 270° arc), the bead springs into its opening, then the
  wordmark and promise rise over a progress shimmer, before a 320 ms crossfade
  into the app. It is theme-aware (bright mint→coral on the dark lagoon, deeper
  brand steps in the light polarity) and honours the system **reduced-motion**
  setting. The native splash background now uses the same lagoon
  (`colors.xml` / `values-night/colors.xml`) instead of flat white.
- **Motion** (calm, decorative-free): the centre Capture action carries a slow
  breathing halo; the bottom-bar selection pill fades in with a small icon lift
  (it stays solid clay — the 5.4:1 contrast rationale is kept in a comment); the
  capture confirmation draws a real animated tick; the Home AI orb breathes
  gently.

## Verification

```text
gradle :app:assembleDebug :app:testDebugUnitTest
  -> BUILD SUCCESSFUL in 10m 27s
     469 tests across 39 suites, 0 failures, 0 skipped, 0 errors
       (incl. CaptureViewModelTest: 2 tests, 0 failures)
     app/build/outputs/apk/debug/app-debug.apk  (46.4 MB)
```

Design previews used for review are tracked under `design/` and served locally
with `python3 -m http.server 8787` from the repo root:
`design/ui-preview.html` (screens) and `design/additions.html` (logo, start
screen, motion).

## Not verified here

No Android device or emulator is available in this environment, so on-device
appearance of the launcher icon, the start screen and the motion — real density,
mask shapes, timed splash handoff, and the recorder with a real microphone — has
**not** been performed and is not claimed. The build and the JVM test suite are
the evidence.

---

# Diary on real content, and the Stitch visual pass

Two layers, one branch.

The branch's earlier work made the Diary render **real, stored data** — attachments,
favourites, composer — on top of `main`'s Diary redesign. This pass audits that
result against the supplied Stitch design, fixes the defects the audit found, and
aligns the remaining visual details. Everything stays native: Kotlin, Compose,
Material 3, ViewModels, Room, local media, offline.

## Root causes fixed

Three of these are user-visible data bugs, not cosmetics. Each has a regression
test that fails when the fix is reverted.

**1. Deleting a memory could silently fail.** Confirming Delete pops the
destination immediately, which clears the back-stack entry and therefore the
ViewModel — and with it `viewModelScope`. The delete was racing its own
cancellation, so on a slow commit the row survived and reappeared on the timeline
after the user was told it was gone. Fixed with `withContext(NonCancellable)`.
Proven by a test that holds the DAO read on a cancellable `CompletableDeferred`,
 closes the store mid-flight, and asserts the row is still gone.

**2. Every Diary write did file I/O on the UI thread.** `DiaryRepository`'s writes
read a row, decode its attachment JSON, touch the filesystem, and write the row
back. Room moves *its own* suspend calls to its query executor, but the Kotlin in
between — especially `MediaStorage.deleteIfExists`, a blocking `File.delete()` —
ran on whatever thread called in, and every caller is `viewModelScope` (Main). One
"remove this photo" tap unlinked a file on the UI thread. Fixed by moving the
switch to the layer that owns the storage: an injectable `io: CoroutineDispatcher`
(defaulting to `Dispatchers.IO`) with `withContext(io)` around `createEntry`,
`updateEntry`, `delete`, `removeAttachment` and `restoreFromBackup`. Fixing it at
the repository rather than at each call site fixes every present and future caller
at once, and keeps the threading contract where the storage is. Two tests assert
no storage work happens on the calling thread.

**3. Copy gave no feedback, and photo-less memories had no way to add one.**
`DiaryDetailScreen` had a `SnackbarHostState` but no `SnackbarHost`, so
`showSnackbar` resolved into the void and the one action with no visible effect
of its own looked broken. The `DiaryPhotoStrip` was gated on `photos.isNotEmpty()`,
which hid the strip's add tile — the only affordance for adding a photo from the
detail page — exactly when a memory had no photos. The strip is now rendered
unconditionally, and the host is rendered.

**4. Save could be tapped into nothing.** The editor computed its own enabled
state locally while the authoritative `DiaryEditorState.canSave` already existed,
so the two could disagree. The button now renders from `canSave`, which also
covers the loading and active-recording guards the local copy missed.

**5. Top insets were missing.** `MainActivity` calls `enableEdgeToEdge()` and the
only `Scaffold` in the hierarchy is the bottom-bar one, so day view and detail page
inherited no top inset and rendered under the status bar. Both now apply
`statusBarsPadding()`, matching what `DiaryEditorOverlay` already did.

**6. The photo-remove target was 24dp.** Half the accessible minimum, on the
control used to clean up a photo. The touch target is now the full 48dp, with the
24dp chip nested inside it so the drawn size and position are unchanged.

## Visual pass

- **One action colour.** `DiaryInkViolet` was both heading ink *and* the fill for
  every pressable control, so tappability was not legible. `DiaryInkViolet` is now
  heading ink only (`#211A44`), and a new `DiaryActionViolet` (`#6C47EB`, Stitch
  primary) carries the FAB, "+ Memory", detail actions, attachment actions and
  tag chips. Pressable vs. readable is now told by hue alone.
- The FAB glyph became a real `Add` icon with a content description instead of a
  bare `+` character.
- "+ Memory" became a filled pill rather than a bare violet word, so it reads as
  something to press.
- The selected-mood ring went from a 1.5dp mood-coloured hairline to a 2dp
  `#6C47EB` ring, which had been the weakest selection signal in the picker.
- Status-bar insets, as above.

## Deliberately not changed

Recorded with reasons in `STITCH_AUDIT.md` §3: `LifeOSSpacing.screenPadding` stays
at 24dp (shared by every screen, Stitch asks for 20dp), the timeline keeps its
established cardless editorial layout, and `DiaryEntity.title` stays nullable
because the design has no title field.

No schema, migration, manifest, dependency, permission, or network change. Room
remains v5. No new lint findings.

## Testing

- `DiaryRepositoryTest.kt` (new, 9 tests) — real files in a temp directory rather
  than a mocked `MediaStorage`, because the behaviour under test is precisely
  "does the user's photo actually get unlinked". Plus two calling-thread
  assertions.
- `DiaryDetailViewModelTest.kt` (new, 4 tests) — the delete/pop race above, plus
  the three adjacent actions on the same ViewModel.
- `DiaryViewModelTest.kt` (extended, +3 tests) — pins each guard in
  `DiaryEditorState.canSave` that the Save button now renders from.

```text
gradle --offline :app:compileDebugKotlin        -> BUILD SUCCESSFUL (0 errors)
gradle --offline :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --rerun-tasks
                                                 -> BUILD SUCCESSFUL in 4m 35s
                                                    173 tests, 0 failures, 0 skipped, 18 suites
                                                    lint: 8 issues, 0 errors, 0 in the diary
                                                    app/build/outputs/apk/debug/app-debug.apk (42.3 MB)
```

The 8 lint issues are pre-existing and in files this branch does not touch.

## Not verified here

No Android device or emulator is available in this environment, so on-device
visual and interaction review — keyboard and inset behaviour, photo picker, voice
recording and playback, share sheet, snackbar, and real-density appearance — has
**not** been performed and is not claimed. `app/build/reports/` holds the JVM and
lint reports; `STITCH_AUDIT.md` holds the design audit.
