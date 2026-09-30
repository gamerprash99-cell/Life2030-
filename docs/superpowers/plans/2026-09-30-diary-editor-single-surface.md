# Diary editor: one writing surface, no keyboard gap — 2026-09-30

Second pass on the Diary composer after the 2026-09-28 keyboard/cursor fix. The
keyboard/cursor behaviour from that pass is kept; this pass only removes the
blank band above the keyboard, collapses the date/time card into a thin strip,
and pulls the media rows inside the white memory card.

## Root cause of the blank band (the important finding)

It is **not** an extra `imePadding()`, a `Spacer()`, a capped editor height or
`imeNestedScroll()`. It is a double count of the bottom edge:

1. `LifeOSNavHost` wraps every destination in a `Scaffold` whose `bottomBar` is
   `LifeOSBottomBar`, and applies the resulting `PaddingValues` to the `NavHost`
   with `Modifier.padding(padding)`. The bar is ~68dp of content plus its own
   `navigationBarsPadding()`, so the Diary destination's box is already that
   much shorter than the window.
2. `DiaryEditor` then applies `Modifier.imePadding()` to its root column, which
   adds the **full** IME height on top of the already-reduced box.

Arithmetic: content bottom `= windowBottom - bottomBarHeight - imeHeight`, while
the keyboard's top edge is `= windowBottom - imeHeight`. The difference is
`bottomBarHeight` — a blank band the exact height of the navigation bar, sitting
between the editor and the keyboard, visible because the bar itself is hidden
behind the keyboard.

`enableEdgeToEdge()` means the window is not resized for the IME, so the inset
has to be consumed in Compose — it just must not be counted twice.

## Fix

- `LifeOSNavHost`: while the IME is visible **and** the current destination is
  `diary` or `diary/{entryId}`, the bottom bar is not composed and the content
  gets no bottom padding. With the keyboard closed, and on every other
  destination, behaviour is unchanged.
- `DiaryEditor`: one vertical structure — header, thin date/time strip, then the
  white card taking the whole remaining viewport (`weight(1f)`), with a single
  scroll viewport inside the card holding text + media, and the three action
  icons pinned at the card's bottom. No `BoxWithConstraints`, no
  `heightIn(max = …)`, no outer scroll layer.
- Media: `DiaryEditorAttachments` renders inside the card's scroll viewport, so
  photos/voice/place/tags/weather are part of the memory content. Its own
  screen-horizontal padding is dropped (it would double-indent inside the card).
- Duplicate add-controls: the shared `DiaryPhotoStrip` / `DiaryVoiceNoteRow` /
  `DiaryLocationRow` get a `showAddAction` parameter, defaulting to `true` so
  the detail screen is byte-for-byte unchanged. The composer passes `false`,
  because the three pinned icons are the add entry points there. Idle rows with
  nothing to show collapse to nothing instead of printing a second control.

## Files

- `app/src/main/java/com/lifeos/app/ui/navigation/LifeOSNavHost.kt`
- `app/src/main/java/com/lifeos/app/ui/diary/DiaryEditor.kt`
- `app/src/main/java/com/lifeos/app/ui/diary/DiaryEditorAttachments.kt`
- `app/src/main/java/com/lifeos/app/ui/diary/DiaryComponents.kt`
- `UPDATE.md`

## Constraints held

- No ViewModel, repository, use-case, Room, migration, navigation-graph,
  permission-launcher or media-storage change. `attachmentsJson` still holds an
  ordered photo list plus at most one voice note and one place.
- The three launchers and their permission timing are untouched: photo uses the
  existing `PickVisualMedia` contract, microphone and location are still
  tap-triggered, and nothing is requested at Diary launch.
- No new dependencies, no network, no AI, no analytics, no hardcoded device
  heights, no negative offsets, no magic constants.
- `LifeOSNavHost` is touched only for the two Diary routes; every other screen
  keeps its current inset behaviour.
