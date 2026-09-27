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
