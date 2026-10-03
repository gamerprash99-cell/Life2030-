# New Memory editor: structural UX pass (Phase 1) — 2026-09-30

Third pass on the Diary composer. The keyboard/cursor behaviour and the single
writing surface from `2026-09-30-diary-editor-single-surface.md` are kept; this
pass only changes the *chrome* around the writing surface and how the composer
owns the window.

## Root causes

1. **The composer competes with the bottom navigation.**
   `DiaryScreen` renders `DiaryEditor` as an in-place `if (showEditor)` branch
   inside the `diary` NavHost destination. `LifeOSNavHost` can therefore only see
   the *route*, never "the composer is open". Its only signal was
   `WindowInsets.isImeVisible`, so the bar was composed again the moment the
   keyboard closed — and it competed with the editor and stole ~100dp of height.

2. **Excessive unused vertical space with the keyboard hidden.**
   The writing field is a bare `BasicTextField` with `heightIn(min = 132.dp)` on
   a near-white card (`DiaryPaperCard` on `LifeOSBackgroundLight`, separated only
   by a 1dp hairline), so a short entry floats in a large empty card. There is no
   boundary and no intent to the writing area, and the fixed 132dp minimum cannot
   respond to the height actually available.

3. **The layout jumps between keyboard states.**
   Opening/closing the keyboard flipped the bottom bar on and off, so the editor
   box changed by ~100dp (bar) plus the IME height in a single step.

4. **Header is vertically heavy.** The title is `headlineMedium` (28sp) with no
   `maxLines`/`softWrap` guard, so at a large font scale "Edit Memory" wraps and
   the header grows a second line, squeezing the card. The Save pill is a filled
   `DiaryActionViolet` block sitting in the same band as the top of the writing
   surface.

5. **The date/time strip is heavy and can clip.** Both halves use `bodyLarge`
   SemiBold; `d MMMM yyyy` + `h:mm a` do not fit a 320dp-wide phone at 16sp, so
   the date ellipsises. The tappable halves are only ~26dp tall.

6. **Wrong bottom inset once the bar is hidden.** `WindowInsets.ime` is
   `AndroidWindowInsets(Type.ime())` and is 0 when the IME is hidden (verified in
   `WindowInsetsHolder` bytecode). `imePadding()` therefore contributes nothing
   with the keyboard closed, so the composer must inset itself with
   `systemBars.bottom ∪ ime` once `LifeOSNavHost` stops supplying the bar's
   padding.

## Fix

- `LifeOSNavHost`: a tiny `LocalComposerChrome` holder, provided above the
  `Scaffold`. `DiaryEditor` declares it owns the window for as long as it is
  composed (one `DisposableEffect`), so the bar is not composed and the content
  gets no bottom padding for the *whole* editing flow — keyboard up or down.
  Every other destination is untouched, and the navigation graph, back stack and
  `popBackStack` behaviour are unchanged.
- `DiaryEditor` insets itself with `WindowInsets.systemBars[Bottom] ∪
  WindowInsets.ime`: the larger of the two on the bottom edge, correct for
  gesture and 3-button navigation, and never counted twice because the Scaffold
  now supplies 0dp below.
- Header: one 48dp row. Title is `titleLarge` serif bold, `maxLines = 1`,
  `softWrap = false`, ellipsising so a large font scale can never wrap it or push
  Save off. Save becomes a tonal lavender pill with a 48dp minimum touch target.
- Date/time strip: `labelLarge`, tighter padding, 40dp minimum touch height,
  date SemiBold in ink violet (primary) and time regular in `onSurfaceVariant`
  (secondary). Both halves still open exactly the same pickers.
- Writing surface: a nested rounded surface (18dp radius, `DiaryLavender` tint,
  `DiaryHairline` border) so the writing area has a real boundary against the
  card and against the metadata below it. Its minimum height is
  `max(132.dp, viewportHeight * 0.55f)` measured once from the card's
  `BoxWithConstraints` viewport — it fills the space available, is never a
  hardcoded small fixed height, and long memories still grow and scroll.

## Files

- `app/src/main/java/com/lifeos/app/ui/navigation/LifeOSNavHost.kt`
- `app/src/main/java/com/lifeos/app/ui/diary/DiaryEditor.kt`
- `app/src/main/java/com/lifeos/app/ui/theme/Spacing.kt`
- `UPDATE.md`

## Constraints held

- No ViewModel, repository, use-case, Room, migration, navigation-graph,
  permission-launcher or media-storage change. No new dependency, no network, no
  AI, no analytics, no new screen, no manual screen switching.
- `DiaryScreen.kt` and `DiaryDetailScreen.kt` are not modified: the composer
  declares the window itself, so both entry points are covered by construction.
- No absolute positioning, no magic coordinates, no negative offsets.