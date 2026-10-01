
## 2026-09-30 — New Memory, final phase: every control the user can see is now big enough and nameable (branch `fix/diary-date-strip-center-today`)

Phase 5, the closing phase: **final polish, regression testing, and only those fixes that could be proven.** Phases 1–4 are the baseline and were **not** redesigned, restructured or re-laid-out. `DiaryEditor.kt`'s layout is unchanged — the composer window ownership, the latched writing surface, the two-stage Back, the counter under the text, the selection handle, the 28sp line height, the Phase 3 hierarchy, the pinned action row, its footer hairline and the Phase 4 location state machine are all intact, re-verified by marker before anything was touched. No architecture rewrite, no new screen, no new dependency, no network/AI/telemetry, no Room entity/DAO/migration, no `ViewModel`/repository/use-case/DI change, no navigation-graph change, no manifest or permission change, no change to the 1000-character rule, and no removal of any existing functionality.

**Everything except four things was left alone on purpose.** The brief for this phase was to fix *verified* issues and to document anything that cannot be verified rather than change it. The audit ran across the header, writing area, tags, location, attachments, navigation, consistency, accessibility, responsive behaviour, functional regression and performance; the great majority of what it turned up was either already correct (see *Verified as already correct*) or unverifiable without a device (see *Not verified here*). Four findings were provable from the source, and only those four were changed.

### Changed

- **The tag chip's remove control was 36dp — below the app's own 48dp minimum.** The `+ Add tag` control was raised to `minTouchTarget` in Phase 3, and its own comment said a smaller target was "below the app's own 48dp `minTouchTarget`" — but the `×` that removes a tag, sitting in the chip that control produces, was left at a hardcoded `size(36.dp)`. It now uses the same `LifeOSSpacing.minTouchTarget` token every other control in the card uses. This is the one accessibility follow-up that Phases 3 and 4 explicitly deferred, now closed. The chip's own padding drops from 7dp to 1dp on three sides so that **its height is unchanged**: `1 + 48 + 1` is the same 50dp that `7 + 36 + 7` produced, and the `×` lands in exactly the same place. The chip is 12dp wider, which is the unavoidable cost of a 12dp-larger target and the only visible difference.
- **The date and time halves were 38dp.** Phase 1 chose 38dp solely to keep the strip as short as the 26dp row it had replaced, and its comment said so. But these two halves are the **only** way to change a memory's date or time, so by this project's own stated standard they were the last controls in the composer that a user could see, want, and fail to hit. `diaryDateStripMinTouch` is now 48dp, equal to `minTouchTarget`. Cost: the strip is 10dp taller, so the writing surface gives up 10dp of resting height. That is affordable and was checked rather than assumed — the surface is a `weight(1f)` share of the card floored at 132dp, so it absorbs the loss without moving the card, the footer hairline or the pinned actions; and the date half already wraps to two lines at large font scales, where the floor was being exceeded anyway.
- **Six clickables were announced to TalkBack as unnamed buttons, and two were announced as two separate things.** A `contentDescription` on an `Icon` *inside* a `clickable` is exposed as its own non-clickable semantics node: the user is offered a label they cannot activate, and a button they cannot name. The description now sits on the same node that carries `onClick`, which is what makes each control one correctly-labelled button — the treatment `IconButton48` has used in this card since Phase 3. This affects the three composer action icons (photo, mic, location) and the photo tile's remove control. The icons' own descriptions are now `null`, because a description on both nodes would simply reintroduce the duplicate.
- **Eight clickables had no `Role.Button`.** Material semantics announce a `clickable` without a role as a generic double-tap target rather than as a button, so the date half, the time half, Save, the photo add tile, the voice-note add row, the photo remove control and the tag remove control were all missing the one word that tells a screen-reader user what kind of thing they have landed on. `role = Role.Button` is now set on each. This changes no layout, no size, no colour and no behaviour — it is semantics only.

### Verified as already correct, and therefore deliberately not touched

- **The 1000-character rule** is enforced in exactly two places (`take(MAX_MEMORY_CHARACTERS)` in the field's `onValueChange` and the identical guard in the ViewModel), both untouched.
- **Typing does no I/O.** `onContentChange` copies the state object and clears the error field; no repository call, no database write, no allocation proportional to the draft. Per-keystroke work is a single `String.take` and one state copy.
- **No repeated permission checks during composition.** The permission probes live in the `rememberDiaryEditorActionTriggers` click lambdas, so they run on a tap and not on every recomposition; the launchers are `rememberLauncherForActivityResult`-scoped, which is the correct lifetime.
- **No repeated disk or object work.** `File(photo.filePath).exists()` and its voice-note equivalent are each `remember`-keyed on the path, so a recomposition does not re-stat the filesystem. The date formatter is a single top-level `FULL_DATE_FORMATTER` reused by every call.
- **The nav architecture is untouched and New Memory is not a nav destination.** It is an overlay gated by `showEditor`, so Home → New Memory → Back cannot create a duplicate stack by construction; there is no route, no `navigate` and no `popBackStack` involved in opening or closing it. Every real route in the graph still uses `launchSingleTop`.
- **Insets are handled by the union of the system bars and the IME on the bottom edge only**, which is correct for gesture navigation and for 3-button navigation, and is not double-counted because the top edge belongs to the `Scaffold`.
- **No new colours, type styles, shapes or components were introduced.** The warning tone reuses the existing `LifeOSWarning` token, the disabled Save treatment reuses `DiarySaveDisabled`, the recovery pill reuses `DiaryLavender` and `DiaryActionViolet`, and every size introduced is the existing `LifeOSSpacing.minTouchTarget`.
- **The four-lint-item and five-`AutoboxingStateCreation` findings in `DiaryEditor.kt` are pre-existing and were left alone** — the two `AutoboxingStateCreation` hits are the time picker's `hour`/`minute` `mutableStateOf`, and changing them would be the premature optimisation this phase was told not to perform.

### Verification (offline, local Gradle 8.9 — this environment has no `gradlew` script)

- **Phase 1–4 integrity re-checked before any edit**, by content rather than by assertion: `ComposerWindowOwner` present in `DiaryNavHost`/`LifeOSNavHost`; `latchRestingViewport`, `writingSurfaceMinHeight` and the `weight(1f)` pinned viewport present and `BoxWithConstraints` count 0; two `isImeVisible` Back handlers across 3 `BackHandler`s; `characterCounterLabel`, `LocalTextSelectionColors` and `DIARY_EDITOR_LINE_HEIGHT = 28.sp` present; `maxLines = 2` on the date; two `HorizontalDivider(color = DiaryHairline)`; the `1000` rule intact; `LocationStatus`'s `SERVICE_DISABLED`/`NO_PROVIDER`/`NO_FIX` and `locationRowOffer` and `openLocationSettings` all present. The 20 Phase 2 `DiaryEditorLayoutTest` tests passed unchanged, as did all 12 Phase 4 `DiaryLocationOfferTest` tests.
- `:app:compileDebugKotlin` — BUILD SUCCESSFUL.
- `:app:testDebugUnitTest` — BUILD SUCCESSFUL, **229 tests, 0 failures, 0 errors, 0 skipped** (was 221; **+8** from the new `DiaryEditorAccessibilityTest`). Report: `app/build/test-results/testDebugUnitTest/`.
- `:app:assembleDebug` — BUILD SUCCESSFUL.
- `:app:assembleRelease` — BUILD SUCCESSFUL (R8 minify + `shrinkResources`, `lintVitalRelease` clean).
- `:app:lintDebug` — BUILD SUCCESSFUL, **0 errors**, and the reported set is the **same nine pre-existing items** as after Phases 1–4 (6 warnings, 3 information). None of the nine is in a Phase 5 change: the two in `DiaryEditor.kt` are the pre-existing time-picker `AutoboxingStateCreation` hints on lines 607–608, and the rest are in `app/build.gradle.kts`, `AndroidManifest.xml` and resources. Report: `app/build/reports/lint-results-debug.html`.
- **`DiaryEditorAccessibilityTest` (new, 8 tests)** pins the two rules this phase changed, so they cannot regress: that `minTouchTarget` is 48dp, that the date/time halves equal it, that the tag chip's 48dp target still yields the original 50dp chip height, that all sixteen audited interactive targets in the composer meet the minimum, and that the action-bar labels never announce a promise a state cannot keep.
- **`git status` reviewed in full.** The Phase 5 delta is five files: `ui/diary/DiaryEditor.kt`, `ui/diary/DiaryComponents.kt`, `ui/diary/DiaryEditorAttachments.kt`, `ui/theme/Spacing.kt` and the new test. The many other modified files in the working tree are the pre-existing uncommitted work that was already there before this phase and were not touched.

### Not verified here, and not claimed

This is the part of the brief that could **not** be executed, and it is most of the visual and responsive matrix. This environment has no `adb`, no emulator and no AVD, and the project has **no `ui-test-junit4` and no Robolectric** — only JUnit 4 and `kotlinx-coroutines-test`. So none of the following was observed, and none of it should be read as confirmed:

- **Rendering.** Any statement about how a row actually looks — the header, the empty state, the counter, the tag chips, the location panel, the pinned row, clipping, overlap, hidden buttons, bad wrapping, excessive blank space.
- **Responsive behaviour.** Small phone, large phone, portrait, landscape, and the two navigation-bar modes. The two changes with a *layout* consequence — the 10dp-taller date strip and the 12dp-wider tag chip — are argued from the layout rules above, not seen. Both are worth ten seconds of eyes on a device.
- **Font scaling.** Whether the two-line date still fits at 1.3× and 2.0× with the strip now taller, and whether the `maxLines = 1` title, counter, time and Save labels stay intact at large scales.
- **TalkBack.** That the merged label/role changes are announced as intended. The code is now correct by construction and the invariants are unit-tested, but no screen reader was run. A device pass should confirm the composer action row is announced as three nameable buttons.
- **Interaction.** Typing, deleting, Save, Back, tags, camera, microphone, location and the permission dialogs. The two-stage Back is unchanged and its logic is unchanged, but no Back press was simulated.
- **State restoration.** Process death and configuration-change restoration of the draft, the tag draft and the date/time pickers.

### Remaining issues

- **The counter scrolls out of view on a 1000-character entry** — unchanged since Phase 2, and still the most substantive known weakness of the layout.
- **Rotating with the keyboard open re-latches the viewport to a short resting height** until the keyboard is next closed — unchanged since Phase 2, a direct consequence of the latch that makes the transition smooth.
- **Media still flows in insertion order**, because `attachmentsJson` has no way to anchor an attachment to a text offset — unchanged since Phase 1.
- **Returning from system Location Settings shows a stale location state** until the next attempt, because nothing observes `LocationManager` changes. Unchanged since Phase 4; a resume-time re-check is the fix.
- **The footer location icon performs the same tap in every state**, so with location switched off the tap re-attempts and the row then explains, rather than going straight to settings. Unchanged since Phase 4 by choice: routing on state would couple the trigger to the state.
- **Three pre-existing deprecation warnings** remain for `Icons.Filled.ArrowBack` and `Icons.Filled.MenuBook`. They are cosmetic, sit in locked layout code, and swapping them now would be a change with no user-visible benefit.
- **`LocationStatus.FAILED` still has no producer** — every `LocationFailure` maps to a specific value. Retained deliberately as the only tone that reaches red and as the safe fallback for a reason added later.

## 2026-09-30 — New Memory: Location stops offering an action that cannot work (branch `fix/diary-date-strip-center-today`)

Phase 4 of the New Memory UX work: **Location state and its presentation.** Phases 1–3 are the baseline and are **not** rebuilt, redesigned or re-laid-out here — `ui/diary/DiaryEditor.kt` was **not modified at all in this pass**, so the composer window ownership, the latched writing surface, the two-stage Back, the counter, the selection handle, the 28sp line height, the Phase 3 tags/attachment hierarchy, the pinned action row and its footer hairline are all byte-identical to the state verified at the end of Phase 3. No architecture rewrite, no new screen, no new dependency, no network/AI/telemetry, no Room entity/DAO/migration, no `ViewModel`/repository/use-case/DI change, no navigation-graph change, **no manifest or permission change**, and no change to the 1000-character business rule.

- **Root cause: a missing state, not a styling problem.** `LocationStatus` had no value for "the OS location service is off", so `attachLocation` collapsed `PROVIDERS_DISABLED`, `NO_PROVIDER` and `NO_FIX` into a single `FAILED`. The row's label `when` handled only `PERMISSION_DENIED` and `PERMISSION_PERMANENTLY_DENIED`, so `FAILED` fell through to `else -> "Add current location"` — a live-looking tap target for a state where nothing can succeed. The honest reason was not missing; it was **written to the wrong place**: it went into `errorMessage`, a field shared with photo, voice, save and load errors, rendered once by `InlineErrorText` at the foot of the *whole* attachments column below Weather, in red, at `labelMedium` — hundreds of dp from the control that caused it, indistinguishable from an unrelated save error, and **erased by the next keystroke**, because `onContentChange` clears that field.
- **`LocationStatus` gained three values; nothing was removed or renamed.** `SERVICE_DISABLED` (permission fine, device switch off), `NO_PROVIDER` (no location service at all) and `NO_FIX` (permission fine, service on, no fix yet). Every existing `when` on the enum has an `else`, so the addition compiled without touching a single one of them. Deliberately **not** added: an `ATTACHED` value — a place is attached when `place != null`, and a second way to say that would give the state two sources of truth and put Phase 3's attached rendering at risk.
- **One state per cause, and the reason no longer leaves the location state.** The `when` over `LocationFailure` now maps all four reasons explicitly. The copy is no longer duplicated into the shared `errorMessage`; the reason lives in `locationStatus` and is explained by the row that owns it, where it cannot be confused with a save error and cannot be wiped by typing. The now-orphaned `locationErrorFor` was removed with it.
- **The decision is one pure, unit-tested function.** `locationRowOffer(status): LocationRowOffer` returns a title, an optional detail, exactly one `LocationRowAction`, and a `LocationRowTone`. It is called only when no place is attached and nothing is in flight, and it is the reason the reported bug is now a test rather than a code-reading exercise. The invariant it exists to enforce is **a state may never offer an action that cannot succeed**: the two states where there is genuinely nothing to press resolve to `LocationRowAction.NONE` and render no button at all.
- **Every state answers state → why → what to do.** Idle: "Add current location". Permission denied: "Allow & attach" (the OS prompt is still live here, so re-asking is correct). Permanently denied: "Open settings" — asking again is the exact failure mode `PermissionManager` was written to prevent, since Android silently no-ops a twice-denied prompt. Location off: "Turn location on" → **Android's location settings**, not app permissions, because the permission is already granted and the app-permissions page would be a second dead end. No service at all: an explanation and **no button**. No fix: "Try again", in a neutral tone, because a device woken up indoors has no fix yet and that is a normal answer rather than a failure.
- **Red is reserved for the genuinely unexpected.** `LocationRowTone` maps the three states the user must act on to the existing `LifeOSWarning` amber token; idle, "no fix" and "no service" stay neutral; **only** `FAILED` reaches `colorScheme.error`. A test asserts that `FAILED` is the single red state, so the palette cannot drift.
- **Both settings routes go through the existing centralized `PermissionManager`.** `openLocationSettings` is modelled directly on the `openExactAlarmSettings` precedent already in that object: SDK-guarded, wrapped in a catch, and always falling back to `openAppSettings` rather than crashing on an OEM build that lacks the action. No intent is constructed outside the centralized object, and the row receives lambdas rather than a `Context`, matching how `rememberPermissionState` already exposes `openSettings`. The composer still has exactly one permission flow per capability and still requests nothing before a tap.
- **The pinned location icon is now honest to a screen reader, and nothing else.** The footer icon runs the same tap in every state, so the *row* is where the state changes — but its label was a fixed "Add a location to this memory", which is the same misleading promise in spoken form. `DiaryEditorActionBar` gained a **defaulted** `locationStatus: LocationStatus = LocationStatus.IDLE`, mirroring how `isRecording` already makes the microphone's label honest. Defaulted, so both call sites keep compiling; it changes **no layout, no spacing, no icon and no behaviour** — only `contentDescription`.
- **Privacy is unchanged.** `DeviceLocationProvider` was not modified: no new fetch, no new log, no network, no analytics, no cloud. Grepping the four changed files for `Log.`/`println`/HTTP/analytics/Retrofit/Firebase returns zero. Coordinates remain local, encrypted in Room, used only to render a place name, exactly as before. The new unavailable-state copy deliberately contains no coordinates, because there are none to show.
- **Layout stability required no change, and the reason is structural.** The row lives inside Phase 3's `Attachments` group inside Phase 2's scroll viewport, and the card is `weight(1f)`, so a taller Location section scrolls and cannot resize the card, move the Phase 3 footer hairline, or push the pinned actions under the system UI. The idle affordance is still dropped on the composer's surface (the pinned icon owns it) while a *blocked* state keeps its recovery control, because that is guidance rather than a second way to add.
- **Scope:** `ui/diary/DiaryEditorViewModel.kt`, `ui/diary/DiaryComponents.kt` (the Location row and the new offer types), `ui/diary/DiaryEditorAttachments.kt` (wiring, plus the defaulted label parameter), `core/util/PermissionManager.kt`, and one line each in `DiaryScreen.kt` / `DiaryDetailScreen.kt` to pass the new state. Tests: `DiaryLocationOfferTest.kt` (new, 12) and `DiaryViewModelTest.kt` (extended, +12). **`ui/diary/DiaryEditor.kt`, `DeviceLocationProvider.kt`, `LifeOSNavHost.kt`, `Spacing.kt`, `DateTimeUtils.kt`, the manifest and the nav graph were not modified.**

### Verification (offline, local Gradle 8.9 — this environment has no `gradlew` script)

- `:app:compileDebugKotlin` — BUILD SUCCESSFUL. Two pre-existing deprecation warnings only (`Icons.Filled.ArrowBack` in the header, `Icons.Filled.MenuBook` in `DiaryScreen`), both in untouched code.
- `:app:assembleDebug` — BUILD SUCCESSFUL.
- `:app:assembleRelease` — BUILD SUCCESSFUL (R8 minify + `shrinkResources`, `lintVitalRelease` clean).
- `:app:testDebugUnitTest` — BUILD SUCCESSFUL, **221 tests, 0 failures, 0 errors, 0 skipped** (was 197; **+24** = 12 offer tests and 12 view-model state tests). The 20 Phase 2 `DiaryEditorLayoutTest` tests still pass unchanged, which is the strongest available evidence that the pinned-surface work did not regress.
- `:app:lintDebug` — BUILD SUCCESSFUL, **0 errors**, and the reported set is the same nine pre-existing items as after Phases 1–3 (6 warnings, 3 information). No new issue was introduced. `TouchTargetSizeCheck` did not fire, which is expected — it is a screenshot-based check that cannot run without instrumentation, so the 48dp claim rests on the code using `LifeOSSpacing.minTouchTarget`. Report: `app/build/reports/lint-results-debug.html`.
- **Explicit regression check of the locked Phase 1–3 behaviours**, by content rather than by assertion: `ComposerWindowOwner` present in `LifeOSNavHost`; `latchRestingViewport` + `writingSurfaceMinHeight` present and **`BoxWithConstraints` count 0**; two `isImeVisible` Back handlers across 3 `BackHandler`s; `characterCounterLabel`, `LocalTextSelectionColors` and `DIARY_EDITOR_LINE_HEIGHT = 28.sp` present; `maxLines = 2` on the date; two `HorizontalDivider(color = DiaryHairline)` and the `weight(1f)` pinned viewport; the `"Details"` umbrella label absent from live code (one mention remains in a Phase 3 comment); `DiaryEditor`'s signature still exactly 14 parameters with `attachments`/`editorActions` as single `@Composable () -> Unit` slots; the group order still Tags → Attachments → Photos → Voice → **Location** → Weather; `1000`-character enforcement intact; `errorMessage` still serving photo/voice/save/load (14 writes, `InlineErrorText` still rendered); and the action bar's `spacedBy(4.dp)` / `Icons.Outlined` layout untouched.

### Not verified here, and not claimed

- **The rendering and interaction claims.** This environment has no `adb`, no emulator, no AVD, and the project has **no `ui-test-junit4` and no Robolectric** — only JUnit 4 and `kotlinx-coroutines-test` in the unit-test source set. So of the requested matrix, the *state machine* rows (enabled, disabled, permission granted, permission denied, permanent denial, successful attachment) are genuinely executed and asserted, plus a long-draft non-interference test and a save test. The *render* rows — the location section's appearance after a long memory, keyboard open, keyboard closed, back navigation, and the visual result of each tone/label — **cannot be executed here at all** and are reasoned from the layout rules and component structure only. Two judgement calls are worth a human eye on a device: whether the amber "Location is turned off" row reads as a blocked state rather than a bug, and whether the two-line "turn location on / what to do" panel is the right weight beside the neutral rows.
- **`openLocationSettings` has never been fired on a device.** Its SDK guard and `try`/`catch` fallback follow the existing `openExactAlarmSettings` precedent, but only a real device can confirm which settings action a given OEM build resolves.

### Remaining issues

- **The footer icon's tap is still the same tap in every state** — the user taps it while location is off and the row then explains. It would be one tap shorter if the trigger routed straight to location settings when it already knows the service is off, but that would couple the trigger to the state and was judged the larger change. One extra tap, not a dead end.
- **A location that is switched off *while the editor is open* is not detected until the next attempt.** Nothing observes `LocationManager` settings changes, so a user who fixes it in Settings and returns sees the stale state until they tap again. A resume-time re-check would fix it and is a genuine follow-up.
- **The tag chip's remove control is still 36dp** — below the 48dp minimum touch target. Carried forward from Phase 3, still deliberately unaddressed.
- **Carried forward unchanged from Phases 2–3:** the counter scrolls out of view on a 1000-character entry; rotating with the keyboard open re-latches to a short resting height until the keyboard is next closed; media still flows in insertion order because `attachmentsJson` cannot anchor an attachment to a text offset; and `attachments()` remains a single slot.
- **`LocationStatus.FAILED` is now a catch-all with no known producer** — every `LocationFailure` maps to a specific value. It is retained deliberately as the only tone that reaches red and as the safe fallback for a reason added later, but nothing in the current code path constructs it.

## 2026-09-30 — New Memory: tags read as metadata, the action row reads as a footer, and the date stops truncating (branch `fix/diary-date-strip-center-today`)

Phase 3 of the New Memory UX work: **hierarchy, affordance and predictability in the metadata area.** Phases 1 and 2 are kept intact and unretouched — the full-window composer and its bottom-bar suppression, the self-inset editor, the latched resting writing surface, the two-stage Back, the counter under the text, the themed selection handle and the 28sp line height all still stand, and their markers are present in the source after this pass. No architecture rewrite, no new screen, no new dependency, no network/AI/telemetry, no Room entity/DAO/migration, no `ViewModel`/repository/use-case/DI change, no navigation-graph change, no permission-flow change, and no change to the 1000-character business rule.

- **The metadata is now two labelled groups instead of one flat column, and tags moved to the top.** `DiaryEditorAttachments` was ordering photos *above* tags, so a newly added photo read as though it were part of the entry's subject. It is now writing → **Tags** → **Attachments** (photos, voice, place, weather, errors), with a hairline and a `DiarySectionLabel("Attachments")` between the groups and a 4dp extra gap so the break reads as a section rather than another row. Each child still names itself exactly as before ("Photos", "Voice note", "Location") — the tags component already labelled itself "Tags", so only the second group needed a heading.
- **The umbrella "Details" label is gone, and that is a deliberate replacement, not a removal.** Phase 2 put `DiarySectionLabel("Details")` above a slot whose every child already self-labelled, which produced a nested sandwich: `Details > Photos / Tags / Voice note / Location`. Phase 2's own closing note recorded this as the reason splitting the groups was left for a later phase. The Phase 2 `HorizontalDivider` and its 16dp gap are **unchanged** — the separation between the writing and its metadata is still drawn in the same place with the same components; it is simply now stated by two precise group labels instead of one vague one.
- **The pinned actions got a visible edge; their position did not need fixing.** Worth being precise, because the reported symptom was that the controls "move around as content changes": they do not, and could not. The scrolling viewport above them carries `weight(1f)` inside the card's `Column`, so nothing in it — short draft or full 1000 characters — can displace them, and that is why the symptom is not a layout bug. What was real is that an 8dp gap gave the row no boundary, so it read as one more gap in the scrolling column and appeared to float at an arbitrary distance below a short entry. A `HorizontalDivider` plus a 16dp spacer now give that fixed position a visible edge, and it reads as the card's footer. The icons themselves are untouched: still 38dp visual inside a 48dp touch target, still `onSurfaceVariant`, so they stay secondary to the text and the Save pill.
- **`+ Add tag` was a 32dp clickable box of 12sp text** — below the app's own 48dp `LifeOSSpacing.minTouchTarget`, and quiet enough to read as decoration. It now meets the touch target, is set at the same 14sp `labelLarge` as the tag chips it produces, and announces as a button via `clickable(role = Role.Button)`. The violet outline was **kept** on purpose: it is the established accent in this card, and filling it would make a metadata affordance compete with the pinned actions below. The accessibility defect is fixed without adding a new visual.
- **The empty tags state is now legible without becoming a banner.** "No tags yet" was 12sp `bodySmall` — the smallest type in the card and the least legible line on the screen. It is now 14sp `bodyMedium` in the same muted `onSurfaceVariant`, matching the tag chips and the surrounding metadata. Its remedy ("+ Add tag") is always present in the same row, so nothing needs to be drawn to point at it.
- **The date-strip ellipsis is fixed, and the fix is the narrow one.** Phase 2 left `DateTimeUtils` alone and called the problem a metadata-typography decision. It was narrower than that: `DateTimeStrip` formatted the full `d MMMM yyyy` and then asked for `maxLines = 1` with `overflow = Ellipsis`, so at `fontScale ≈ 2.0` the date was cut mid-word ("26 Septem…"). `maxLines` is now `2` and `Ellipsis` is kept as the clamp for scales too large even two lines to hold. The date is two words plus a year, so it now wraps cleanly at the spaces and stays complete and tappable; the strip sits above the weighted card, so the extra line only ever costs the writing surface the height it needs, and nothing changes at normal font scales. **`DateTimeUtils` itself is untouched and no new formatter was added**, so every other consumer of `formatFullDate` is unaffected. This closes the item Phase 1 flagged and Phase 2 deferred.
- **Scope:** `ui/diary/DiaryEditor.kt`, `ui/diary/DiaryEditorAttachments.kt`, `ui/diary/DiaryComponents.kt` (the `DiaryTagEditor` block only). `DiaryEditorLayoutTest.kt` and `DiaryViewModelTest.kt` were **not** modified and all 197 tests still pass unchanged — this pass is layout and styling only, it introduced no new arithmetic rule, and the existing pinned-surface assertions cover the one structural property it could have broken. `LifeOSNavHost.kt`, `Spacing.kt`, `DateTimeUtils.kt`, `DiaryScreen.kt`, `DiaryDetailScreen.kt`, `DiaryEditorViewModel.kt`, the repositories, Room and the nav graph were not modified. The pre-existing uncommitted work in this working tree is unrelated and was left alone.

### Verification (offline, local Gradle 8.9 — this environment has no `gradlew` script)

- `:app:compileDebugKotlin` — BUILD SUCCESSFUL (one pre-existing `Icons.Filled.ArrowBack` deprecation warning, in the header back button this pass did not touch).
- `:app:assembleDebug` — BUILD SUCCESSFUL.
- `:app:assembleRelease` — BUILD SUCCESSFUL (R8 minify + `shrinkResources`, `lintVitalRelease` clean).
- `:app:testDebugUnitTest` — BUILD SUCCESSFUL, **197 tests, 0 failures, 0 errors, 0 skipped** (unchanged count; no test was added or edited by this pass).
- `:app:lintDebug` — BUILD SUCCESSFUL, **0 errors**. The reported set is identical to the nine items after Phases 1 and 2: 6 warnings and 3 information entries, all pre-existing and all in code this pass did not touch. Notably `TouchTargetSizeCheck` did **not** fire, which is expected — it is a screenshot-based check that cannot run without instrumentation, so the 48dp claim rests on the code using `LifeOSSpacing.minTouchTarget`, not on lint. Report: `app/build/reports/lint-results-debug.html`.
- **Not verified here, and not claimed:** every visual and interactive judgement in this pass. The 48dp add-tag target, the two-line date at `fontScale ≈ 2.0`, the empty-state legibility, the footer edge and the new group order are all *reasoned from the layout rules and the measured component structure above* — none of it was observed. This environment has no `adb`, no emulator and no AVD, and the project has no `ui-test-junit4` dependency, so there is no way to observe any of it here. The judgement most worth checking on a device is the one with no test behind it: whether the two-line date at a large font scale still reads as one control, and whether the new "Attachments" heading is too heavy next to the smaller "Photos" / "Voice note" / "Location" labels beneath it.

### Remaining issues

- **The tag chip's remove control is 36dp — below the 48dp minimum touch target** (`DiaryComponents.kt`, the `×` inside each chip). It was found and deliberately left alone in this pass: it is a different affordance from the "+ Add tag" control that was in scope, and raising it to 48dp makes every chip in the card visibly taller and chunkier. It is a real accessibility gap and a genuine follow-up, not a deliberate acceptance.
- **Rotating while the keyboard is open re-latches to a short resting height** until the keyboard is next closed, because the first measurement after the rotation is the collapsed one. The surface recovers on its own the moment the keyboard closes; it is cosmetic, not a stuck layout. (Carried forward from Phase 2, unchanged.)
- **The counter scrolls out of view for a long entry.** It is in document order under the text, as asked, and a 1000-character entry is around seventeen lines, so the counter is below the fold until the user scrolls. Making it sticky would mean per-frame scroll maths, which trades a real simplification for a small convenience; not worth it here. (Carried forward from Phase 2, unchanged.)
- **Media still flows after the text in insertion order.** Anchoring a photo or a take to a specific text offset is not representable in `attachmentsJson` and still needs a schema plus editor change. (Carried forward from Phase 2, unchanged.)
- **The metadata/attachment split is still one slot.** Phase 3 got the two groups onto the right side of the hierarchy *inside* `DiaryEditorAttachments`, but `attachments()` is still a single slot and `DiaryEditor` still has no separate parameters for tags and media. A caller that wanted to place them independently would still need a signature change; the two host screens remain untouched, which is what made this pass cheap.

## 2026-09-30 — New Memory: a stable writing surface, a two-stage Back, and metadata that stops merging into the text (branch `fix/diary-date-strip-center-today`)

Phase 2 of the New Memory UX work: **editing behaviour and long-text composition.** Phase 1's structure (full-window composer, self-inset, bounded writing surface, compact header and date strip) is kept and built on — `LifeOSNavHost.kt` and `Spacing.kt` are **untouched by this pass**, and the keyboard/navigation-bar fix is unchanged. No architecture rewrite, no new screen, no new dependency, no network/AI/telemetry, no Room entity/DAO/migration, no `ViewModel`/repository/use-case/DI change, no navigation-graph change, and no change to the 1000-character business rule.

- **Root cause of the keyboard jump — and of the Phase 1 recomposition limit: the same defect.** Phase 1 sized the writing surface as `max(132.dp, viewportHeight × 0.55f)`, and `viewportHeight` was read *after* `windowInsetsPadding` had subtracted the IME. So every frame of the keyboard animation changed the number, which changed the surface's height, which re-laid out the text. For a short entry the surface height **is** exactly that minimum, so it visibly resized through the whole animation. The jump and the `BoxWithConstraints` recomposition were not two problems; they were the jump *and* its cost.
- **The surface is now sized from a latched resting height (`DiaryEditor.kt`).** A single `onSizeChanged` on the scroll viewport records its height through `latchRestingViewport`, which is `max` — so the value stops changing the moment the viewport has been seen with the keyboard closed. The surface is sized from that, so the keyboard's per-frame animation can move the scroll viewport (which it must — that is the space the keyboard took) but cannot move the surface, the text, or the caret. `BoxWithConstraints` is gone. The `remember` keys drop the latch on rotation and on a font-scale change, since both change what "resting" means.
- **The surface is now allowed to be taller than the visible viewport**, and scrolling happens inside it. That is the point of a writing surface: the page is a page, the keyboard is a window onto it, and neither one is compressed to fit the other.
- **Back is two-stage, and only because the draft is at risk.** `BackHandler` used to dismiss the composer unconditionally, so one press while typing threw away the entry. There are now two mutually exclusive handlers: while `WindowInsets.isImeVisible`, Back calls `keyboardController.hide()` and the editor, its draft, and its scroll position stay exactly as they are; once the keyboard is already hidden, Back dismisses — the same `onDismiss` the two call sites already passed, so the nav stack is untouched in both states. The decision is made in the app rather than left to the IME's own Back handling, because this app sets `enableOnBackInvokedCallback` and which layer consumes the press is a platform detail. If the keyboard controller is ever null the first stage falls back to dismissal, because a Back press that consumes itself and does nothing is indistinguishable from a hung app.
- **The character counter moved to where it is read, and got readable.** It was `labelMedium` in `onSurfaceVariant` — the weakest element in the card — parked in the top-right beside the "Your memory" label, a couple of hundred dp above the text whose length it reports. It now sits directly under the writing surface, right-aligned against the text, at `labelLarge` in the Diary ink: legible, and still smaller than the `bodyLarge` it describes. The wording is `412 of 1000` rather than `412/1000`. The limit is unchanged and is not enforced here — the `take(1000)` in the field's `onValueChange` and the identical guard in `DiaryEditorViewModel` remain the only two places it is applied, and this composable only reports.
- **The counter, the writing surface and the metadata are now in a stated order.** A hairline and a `DiarySectionLabel("Details")` sit between the counter and the attachments, giving writing → counter → details → attachments. Before, the attachments were a flat 12dp-spaced column continuing straight off the bottom of the writing surface, so on a long entry the first photo read as part of the text. Both `DiarySectionLabel` and `HorizontalDivider` are existing components with existing usages in this codebase, and `attachments()` keeps its single slot and signature, so `DiaryScreen.kt` and `DiaryDetailScreen.kt` are unchanged.
- **The selection handle is themed instead of inherited.** `BasicTextField` was falling back to `LocalTextSelectionColors`, whose handle is `colorScheme.primary` — a pale lavender that nearly vanished against the writing surface's own lavender wash. The field is now wrapped in the standard `CompositionLocalProvider` with a `DiaryActionViolet` handle and a lavender highlight. Nothing else changes: selection, the long-press toolbar and every accessibility affordance of `BasicTextField` are untouched, and no custom cursor behaviour was introduced.
- **Text density: leading only.** The line height moved from 27sp to 28sp — 1.75× the unchanged 16sp body size. **The type size was not increased.** Worth being precise about the limit here: Compose 1.7 has no paragraph-gap feature (`ParagraphStyle` carries a line height, not space between paragraphs), so leading is the only mechanism available, and it is the one that reaches density without enlarging the text.
- **The three sizing and counter rules are now pure functions and are unit-tested** — `writingSurfaceMinHeight`, `latchRestingViewport` and `characterCounterLabel`. The tests walk a keyboard transition frame by frame and assert the resting height does not move at any step, which is the property the whole fix rests on; they cover the floor/share boundary at 240dp, cramped and tall pages, and the counter across an empty, partial and full draft. The 1000-character rule gained the regression tests it never had (a draft may be typed to the limit, typing past it is cut rather than rejected, a long mixed draft is cut at exactly 1000 characters, and a full-length draft is stored whole).
- **Scope:** `ui/diary/DiaryEditor.kt` + two test files (`DiaryEditorLayoutTest.kt` new, `DiaryViewModelTest.kt` extended). `LifeOSNavHost.kt`, `Spacing.kt`, `DiaryScreen.kt`, `DiaryDetailScreen.kt`, `DiaryEditorAttachments.kt`, `DiaryEditorViewModel.kt`, the repositories, Room and the nav graph were not modified. The pre-existing uncommitted work in this working tree is unrelated and was left alone.

### Verification (offline, local Gradle 8.9 — this environment has no `gradlew` script)

- `:app:compileDebugKotlin` — BUILD SUCCESSFUL (one pre-existing `Icons.Filled.ArrowBack` deprecation warning).
- `:app:assembleDebug` — BUILD SUCCESSFUL.
- `:app:assembleRelease` — BUILD SUCCESSFUL (R8 minify + `shrinkResources`, `lintVitalRelease` clean).
- `:app:testDebugUnitTest` — BUILD SUCCESSFUL, **197 tests, 0 failures, 0 errors, 0 skipped** (was 173; +24 = 20 layout-rule tests and 4 character-limit tests).
- `:app:lintDebug` — BUILD SUCCESSFUL, **0 errors**. The reported set is byte-for-byte the same nine items as after Phase 1: 6 warnings and 3 information entries, all pre-existing and in code this pass did not touch — `TasksScreen.kt` `ModifierParameter`, `strings.xml` `PluralsCandidate`, two `ObsoleteSdkInt`, `ic_launcher*` unused/monochrome, `AlarmPlaybackService.kt`, `AlarmFullScreenActivity.kt`, and the pre-existing `mutableStateOf` pair in `SimpleDiaryTimePicker` (which only shifted lines). *(This corrects Phase 1's entry above, which described the set as "6 warnings + 2 information items" — it was always 3; the set never changed.)* Report: `app/build/reports/lint-results-debug.html`.
- **Not verified here, and not claimed:** every interactive behaviour in this phase. IME open/close smoothness, caret and selection-handle appearance, the two-stage Back, text density at length, and the 320dp/landscape/font-scale-2.0 cases are all *reasoned from the layout rules above and unit-tested where the rule is pure arithmetic* — none of it was observed. This environment has no `adb`, no emulator and no AVD, and the project has no `ui-test-junit4` dependency, so there is no way to observe any of it here. The highest-risk untested claim is the "no jump" one: the latch makes the surface's height provably constant, but whether the *frame timing* reads as smooth still has to be seen on a device.

### Remaining issues

- **Date strip ellipsis at `fontScale ≈ 2.0` — re-evaluated and deliberately left.** Phase 1 flagged it; Phase 2 re-examined it and it is a *metadata typography* problem in `DateTimeStrip`, not an editing-surface one, so it is out of scope for a phase about typing and long-memory composition. Fixing it properly means a shorter date format or per-component wrapping, which is a metadata decision. It stays open for a later phase. *→ Resolved in Phase 3: the per-component wrapping route was the right one, and `DateTimeUtils` was left untouched.*
- **Rotating while the keyboard is open re-latches to a short resting height** until the keyboard is next closed, because the first measurement after the rotation is the collapsed one. The surface recovers on its own the moment the keyboard closes; it is cosmetic, not a stuck layout.
- **The counter scrolls out of view for a long entry.** It is in document order under the text, as asked, and a 1000-character entry is around seventeen lines, so the counter is below the fold until the user scrolls. Making it sticky would mean per-frame scroll maths, which trades a real simplification for a small convenience; not worth it here.
- **Media still flows after the text in insertion order.** Anchoring a photo or a take to a specific text offset is not representable in `attachmentsJson` and still needs a schema plus editor change.
- **The platform collaboration remains out of reach here.** `attachments()` is still one slot, so photos, tags, voice, place and weather share the "Details" section rather than being split into distinct metadata and attachment groups. Splitting them needs a new parameter on `DiaryEditor` and edits to both host screens; a hairline and a label were judged sufficient for now. *→ Phase 3 took the middle path that was flagged here: the two groups are now labelled and ordered inside `DiaryEditorAttachments`, so the caller-facing signature did **not** have to change and both host screens stayed untouched.*

## 2026-09-30 — New Memory is a focused editor again: full-window composer, adaptive writing surface, compact header and date strip (branch `fix/diary-date-strip-center-today`)

Phase 1 of the New Memory editor UX work: **structural fixes only.** The two earlier Diary passes (2026-09-29 keyboard behaviour, 2026-09-30 single writing surface and the keyboard-gap fix) are kept and built on. No architecture rewrite, no new screen, no new dependency, no network/AI/telemetry, no Room entity/DAO/migration, no `ViewModel`/repository/use-case/DI change, and no navigation-graph change — the nav graph, the routes, and the composer's `BackHandler(→ onDismiss)` are exactly as they were.

- **Root cause of the layout jump, and why the previous fix could not solve it (`LifeOSNavHost.kt`).** The composer is an *in-place branch inside* a destination (`DiaryScreen` and `DiaryDetailScreen` each swap it in via `DiaryEditorOverlay`), so the host could only see the route, never "the composer is open". Hiding the bottom bar therefore had to be keyed on `WindowInsets.isImeVisible`: with the keyboard up the bar was removed, and the moment the keyboard closed the bar came back and stole ≈68dp + its `navigationBarsPadding()`. The editor consequently jumped by the height of the whole navigation bar every time the keyboard opened or closed, and read as a *panel inside the navigation* rather than as a focused writing flow.
- **Window ownership is now declared by the composer itself.** A new `@Stable ComposerChromeState` + `LocalComposerChrome` is provided by `LifeOSNavHost` around the `Scaffold`, and the composer opts in through a single `ComposerWindowOwner()` call in `DiaryEditor`. While the composer is composed, the bottom bar is not composed at all and the content is given **zero** Scaffold bottom padding — with the keyboard up *and* with it down. Because the call lives in `DiaryEditor` (not in a screen), both entry points — New/Edit from the diary list **and** Edit from the memory detail screen — are covered by construction. The host still keeps the route check, so the state can only ever hide the bar on `diary` / `diary/{entryId}`. The read is confined to the Scaffold's slot, so it does not invalidate the destinations. **Net effect: the ≈100dp bar-height flip on every keyboard toggle is gone; the only remaining height change is the keyboard's own.**
- **The composer now insets itself (`DiaryEditor.kt`).** Because the Scaffold no longer contributes anything at the bottom, the root column pads itself with `WindowInsets.systemBars.only(Bottom).union(WindowInsets.ime)`. The union is deliberate: `WindowInsets.ime` is `AndroidWindowInsets(Type.ime())` and is **zero while the keyboard is closed** (verified in `WindowInsetsHolder`'s bytecode), so a plain `imePadding()` — the previous implementation — would have pushed the writing surface under the navigation bar as soon as the keyboard closed. Taking the union on the bottom edge keeps the larger of the two, which is right for gesture navigation (the keyboard covers the strip) and for 3-button navigation (the keyboard sits above the bar), and it is never double counted because the top edge is left entirely to the Scaffold. `imeNestedScroll()` on the inner viewport is unchanged, so the cursor-reveal and scroll-position behaviour from 2026-09-29 is preserved.
- **The writing surface is now bounded, tinted and adaptive.** The text previously sat directly on the near-white card with no edge at all, at a flat `132.dp` minimum. A new `WritingSurface` gives it the same rounded-surface language the voice/location rows already use: 18dp corners, a `DiaryLavender @ 22%` wash, a 1dp `DiaryHairline` border, and the shared `LifeOSSpacing.compactPadding` inside — so its edge is flush with those rows' panels and everything in the card lines up on one left edge. Its minimum height is now `max(132.dp, viewportHeight × 0.55f)`, measured with a single `BoxWithConstraints` around the existing scroll viewport, so an empty editor fills the page instead of floating as a small field, it shrinks with the keyboard, it grows on a tall screen, and a long memory still grows past it and scrolls. The `heightIn(max = …)` viewport cap, the outer scroll layer and the field's `weight(1f, fill = false)` from the 2026-09-30 pass stay gone — nothing hardcoded, no screen coordinates.
- **Header hierarchy inverted so the text is the subject.** The title steps down from `headlineMedium` to `titleLarge` and is pinned to one line (`maxLines = 1`, `softWrap = false`, ellipsis), so it can never grow a second row and squeeze the card at a large font scale nor push Save off the edge. The card's own label steps down to `titleSmall`. **Save changes** becomes a tonal lavender pill instead of a saturated filled block: it was the same weight as the top of the text it competes with. `defaultMinSize(minHeight = LifeOSSpacing.minTouchTarget)` keeps it on the app's 48dp minimum, so the header row is exactly one target tall next to the 48dp back button. The label, both states' colours (the disabled treatment is unchanged) and the click behaviour are the same.
- **Date + Time strip made readable on small phones.** `d MMMM yyyy` + `h:mm a` at `bodyLarge` no longer fits a 320dp phone — the date was being ellipsised mid-word, which is the opposite of readable. Both halves step down to `labelLarge`, and the hierarchy is now explicit: date semibold in the Diary ink (primary), time regular in the muted colour (secondary). The strip has no vertical padding of its own, so its height *is* the `diaryDateStripMinTouch = 38dp` minimum of a half — still as short as the previous version while each half goes from a ≈26dp target to a real one. Both halves remain independently tappable and open exactly the same two pickers, and the date still ellipsises rather than wrapping so it cannot push the time off the strip.
- **New spacing tokens in `LifeOSSpacing`**, next to the existing Diary rhythm tokens: `diaryEditorSection` 10 → 20dp (the gap between the writing surface and the media below it), `diaryEditorWritingFill = 0.55f`, `diaryDateStripMinTouch = 38.dp`. Existing tokens (`screenPadding`, `compactPadding`, `minTouchTarget`, `diaryEditorTextMinHeight`) are reused as-is rather than duplicated.
- **Deliberately preserved:** the three pinned in-card action icons and the attachments' `showAddAction = false` behaviour, the single scroll viewport with the text and the media in one content flow, the character counter and the 1000-char limit, focus/`FocusRequester`/cursor and selection across keyboard changes, all four attachment types, both pickers, Save, the detail screen's editing path, permissions (photo still uses `PickVisualMedia`, the manifest still declares no `CAMERA`), and the ordered-media model with its known text-offset limitation documented in the 2026-09-30 entry.
- **Scope:** 3 source files + this entry (+215/−65, whitespace-ignoring, so the `NavHost` re-indent is not counted): `ui/navigation/LifeOSNavHost.kt`, `ui/diary/DiaryEditor.kt`, `ui/theme/Spacing.kt`. `DiaryScreen.kt`, `DiaryDetailScreen.kt`, both Diary `ViewModel`s, the repositories and the nav graph were read but not touched. `MainActivity.kt` (`enableEdgeToEdge()`) and the `WindowInsets` behaviour above are the preconditions the fix relies on, both already true. The pre-existing uncommitted work in this working tree is unrelated to this pass and was left untouched.

### Verification (offline, local Gradle 8.9 — this environment has no `gradlew` script)

- `:app:compileDebugKotlin` — BUILD SUCCESSFUL.
- `:app:assembleDebug` — BUILD SUCCESSFUL.
- `:app:assembleRelease` — BUILD SUCCESSFUL (R8 minify + `shrinkResources`, `lintVitalRelease` clean).
- `:app:testDebugUnitTest` — BUILD SUCCESSFUL, **173 tests, 0 failures, 0 errors, 0 skipped**.
- `:app:lintDebug` — BUILD SUCCESSFUL, **0 errors**. The 6 warnings + 3 information entries it reports are all pre-existing and in files this pass did not touch (`TasksScreen.kt` `ModifierParameter`, `strings.xml` `PluralsCandidate`, two `ObsoleteSdkInt`, `ic_launcher*` unused/monochrome, `AlarmPlaybackService.kt`, `AlarmFullScreenActivity.kt`, and the pre-existing `mutableStateOf` in `SimpleDiaryTimePicker` which only shifted lines). Report: `app/build/reports/lint-results-debug.html`.
- **Not verified here:** the interactive keyboard and font-scale scenarios (IME open/close, typing to the bottom of a long entry, tapping mid-text, 320dp-wide phone, font scale 1.3–2.0) and the one-frame bar state before `DisposableEffect` runs on first entry. This environment has no `adb`, no emulator and no AVD, and the project has no Compose UI-test dependency, so no on-device result is claimed. The one behaviour that cannot be reasoned about statically is the IME's own animated height change, which is physically necessary; the bar's contribution to it is removed.

### Known limits carried forward

- At `fontScale ≈ 2.0` the date still ellipsises inside the strip. The typography was stepped down as far as readability allows (`labelLarge`); fully solving it needs a wrap-per-component or a shorter date format, which is a Phase 2 content decision, not a spacing fix.
- The `BoxWithConstraints` around the scroll viewport re-subcomposes while the keyboard animates (its constraints change every frame). The screen is a single text field plus at most a few media rows, so this is layout-and-compose work only — no re-decode, and `rememberScrollState`/`FocusRequester`/text-field state are all hoisted or keyed, so the scroll position, cursor and selection survive it. Avoiding it entirely would require either a fixed (forbidden) height or splitting the media out of the single scroll viewport that the 2026-09-30 pass deliberately established.
- Media flows after the text in insertion order; anchoring an image to a specific text offset is not representable in `attachmentsJson` and still needs a schema plus editor change.

## 2026-09-30 — Diary editor is one writing surface, and the blank band above the keyboard is gone (branch `fix/diary-date-strip-center-today`)

Second pass on the 2026-09-29 Diary fix. The keyboard/cursor behaviour from that pass is kept; only the layout around it changed. No ViewModel, repository, Room, migration, navigation-graph, permission or media-storage change; no new dependency, network, AI or telemetry.

- **Root cause of the blank band above the keyboard (this is the real fix).** It was not an extra `imePadding()`, a `Spacer()`, a capped card height or `imeNestedScroll()` — it was a double count of the bottom edge. `LifeOSNavHost` wraps every destination in a `Scaffold` whose `bottomBar` is `LifeOSBottomBar` and applies the resulting padding to the `NavHost`; the bar's own height (≈68dp of content + its `navigationBarsPadding()`) had already shortened the Diary box. `DiaryEditor` then applied `Modifier.imePadding()` to its root column, adding the **full** IME height on top of that already-reduced box. So the content bottom sat at `windowBottom − bottomBarHeight − imeHeight` while the keyboard's top edge is `windowBottom − imeHeight`: a blank band exactly the height of the navigation bar, visible because the bar itself was hidden behind the keyboard.
- **The double count is removed at its source (`LifeOSNavHost.kt`).** While the IME is visible *and* the current destination is `diary` or `diary/{entryId}`, the bottom bar is not composed and the content is explicitly given no bottom padding (set in code rather than relying on the Scaffold's content-window-inset fallback). With the keyboard closed, and on every other destination, the bar and the padding are exactly what they were — the change is scoped to the two routes that host the composer.
- **One writing surface (`DiaryEditor.kt`).** Header, then a thin date/time strip, then the white "Your memory" card taking every remaining pixel via `weight(1f)`. `BoxWithConstraints`, the `heightIn(max = …)` viewport cap, the outer scroll layer and the field's `weight(1f, fill = false)` are all gone; the card's size is purely what the header, the strip and the keyboard leave, so it shrinks with the IME and grows back when the keyboard closes. No hardcoded heights, offsets or spacers were added.
- **A single scroll viewport inside the card.** The `BasicTextField` sizes to its own text and shares one `verticalScroll` + `imeNestedScroll` viewport with the media, so the caret is revealed by bring-into-view in that viewport, a long entry still scrolls, and the position survives the keyboard opening and closing (`rememberScrollState` is unchanged).
- **Date + Time is now one thin strip** (`DateTimeStrip`, replacing the stacked two-column card): date on the left, time on the right, ~44dp instead of ~85dp. Same values, same violet accents, same typography, same independent tap targets opening the same two pickers. The date ellipsises rather than wrapping, so a long day string cannot push the time off the strip at a large font scale.
- **The media rows now live inside the white card**, in the same content flow as the text, so a photo, a voice note or a place is part of the memory instead of a panel outside it. `DiaryEditorAttachments` dropped its own screen-horizontal padding (it would double-indent inside the card).
- **No duplicate add-controls.** `DiaryPhotoStrip` / `DiaryVoiceNoteRow` / `DiaryLocationRow` gained a `showAddAction` parameter defaulting to `true`, so the detail screen is unchanged; the composer passes `false` because the three pinned icons are the add entry points there, and a row with nothing to show collapses to nothing. A real failure or a denied permission is still explained — only the redundant idle "add" buttons are gone.
- **The three icons stay inside the card, pinned below the scroll viewport**, so they cannot cover the text, the caret or the character counter, and cannot scroll away on a long entry. Permissions are untouched: photo still uses the existing `PickVisualMedia` flow (the manifest deliberately declares no `CAMERA`), microphone and location stay tap-triggered, and nothing is requested at Diary launch.
- **Known limit, deliberately not changed:** `attachmentsJson` stores an ordered photo list plus at most one voice note and one place. Media therefore flows after the text in insertion order; anchoring an image or a take to a specific *text offset* is not representable in that model and would need a schema plus editor change, which is out of scope here. Photos are still individually removable, and a single entry still carries at most one voice note and one place.

### Verification (offline, local Gradle 8.9)

- `:app:compileDebugKotlin` — BUILD SUCCESSFUL (one pre-existing `Icons.Filled.ArrowBack` deprecation warning).
- `:app:testDebugUnitTest` — BUILD SUCCESSFUL.
- `:app:assembleDebug` — BUILD SUCCESSFUL.
- `:app:assembleRelease` — BUILD SUCCESSFUL (R8 minify + `shrinkResources`, `lintVitalRelease` clean).
- **Not verified here:** the interactive keyboard scenarios (IME open/close, typing to the bottom of a long entry, tapping mid-text, font scale). This environment has no `adb`, no emulator and no AVD, and the project has no Compose UI-test dependency, so no on-device result is claimed. The inset arithmetic that produced the band is stated above, and the only behavioural risk introduced is the `showAddAction` default, which keeps the detail screen on its previous path.

## 2026-09-29 — Diary editor keeps the active text above the keyboard + three in-card action icons (branch `fix/diary-date-strip-center-today`)

Targeted Diary editor UX fix. The Diary screen design, theme, typography, card shapes, spacing, navigation, repositories, ViewModels and Room schema are unchanged — only editor *behaviour* and a three-icon utility row were touched.

- **Root cause of the keyboard problem.** `imePadding()` was applied *inside* the editor's `verticalScroll` container, so the IME inset became trailing padding on the scrolling content instead of shrinking the viewport. The editor viewport therefore stayed full-height behind the keyboard, and because the `BasicTextField` was unbounded it also grew downward with no cursor-reveal viewport of its own. Result: the active line/cursor slid under the keyboard while typing.
- **Insets fixed (`DiaryEditor.kt`).** `imePadding()` moved to the screen-level `Column`, so header and editor both end above the keyboard; the inner scroll now also uses `imeNestedScroll()` so IME open/close chains with the existing scroll position instead of resetting it.
- **The editor owns its own scrolling.** The text field is bounded by the card (`weight(1f, fill = false)` + `heightIn(min = diaryEditorTextMinHeight)`) and the card is capped with `heightIn(max = …)` against the *measured* viewport (`BoxWithConstraints`, so it adapts to the keyboard, aspect ratio and font scale — no hardcoded coordinates or magic offsets). `BasicTextField` keeps the cursor inside that viewport, so it auto-reveals the active line on typing, on paste and on tapping mid-text, and it does not fight the user: manual scrolls up are respected and nothing is locked to the bottom. A short entry still renders exactly as before (same `132.dp` minimum text height).
- **Three small left-side action icons inside the white "Your memory" card** (new `editorActions` slot at the bottom of the existing card, below a `10.dp` gap matching the card's own rhythm): photo, voice note, location. 19dp outlined Material icons on a 38dp `DiaryLavender` circle, 48dp touch target (existing `LifeOSSpacing.minTouchTarget`). The row is a non-weighted child of the card, so it stays pinned to the card while the text scrolls behind it and can never cover text, cursor or the counter. The mic icon carries a subtle `DiaryLavender`/`DiaryActionViolet` active tint while recording.
- **No new permission architecture.** The three launchers and their rationale/permanent-denial handling already existed in `DiaryEditorAttachments`; they were extracted into `rememberDiaryEditorActionTriggers(viewModel): DiaryEditorActionTriggers` so the in-card icons and the attachment rows below share one launcher per capability. Permissions are still requested only on the matching tap, never on launch. `AndroidManifest.xml`, `build.gradle.kts` and dependencies are untouched.
- **Camera = photo picker.** The manifest deliberately does **not** declare `CAMERA` (see the comment there: Diary attaches existing photos, so camera access would be an unnecessary permission). The icon therefore opens the existing `ActivityResultContracts.PickVisualMedia` flow (`viewModel.attachPhoto`) with zero permissions. Live camera capture is *not* implemented and was not invented here; adding it would mean declaring `CAMERA` plus a capture flow, which is a product decision outside this task.
- **Preserved:** existing text/1000-char limit/character counter, cursor and selection, copy/paste, keyboard behaviour, Save changes, date/time, Diary persistence, `DiaryEditorViewModel` state, Room entities/DAOs/migrations, navigation, offline/privacy behaviour.
- **Scope:** 5 files (+94/−33): `ui/diary/DiaryEditor.kt`, `ui/diary/DiaryEditorAttachments.kt`, `ui/diary/DiaryScreen.kt`, `ui/diary/DiaryDetailScreen.kt`, `UPDATE.md`.

### Verification (offline, local Gradle 8.9)

- `:app:compileDebugKotlin` — BUILD SUCCESSFUL.
- `:app:assembleDebug` — BUILD SUCCESSFUL.
- `:app:testDebugUnitTest` — BUILD SUCCESSFUL, 173 tests, 0 failures, 0 errors (incl. `DiaryViewModelTest` 30, `DiaryDetailViewModelTest` 4, `DiaryRepositoryTest` 9).
- `:app:assembleRelease` — see the table in the change report; R8 + `shrinkResources`.
- **Not verified here:** the interactive keyboard scenarios (IME open/close, long-entry typing, tap-to-position, font-scale). This environment has no `adb`/emulator/AVD, and the project has no Compose UI-test dependency, so no on-device result is claimed. The layout reasoning is stated above and the four affected call sites are unchanged in behaviour.

## 2026-09-29 — Verified-safe cleanup & hardening pass (branch `fix/diary-date-strip-center-today`)

Dead-code, dependency, security and performance cleanup. Every change was grep/count-verified for zero references before deletion; nothing in the Room schema/migrations, navigation, permissions, offline/privacy behavior or AI-dependency surface was touched.

- **Dead code removed** (all unused): composables (`CreateMemoryAction`, `SaveMemoryAction`, `PinKeypadPanel`, `LifeOSBadge`, `LifeOSIntelligenceCard`, `MoodSelector`, `EditorEyebrow`, `diaryFieldColors`, `PHOTO_TILE_SIZE`, `PHOTO_ASPECT_RATIO`; `MoodSelector.kt` renamed to `MoodDot.kt`); VM dead members (`DiaryViewModel.toggleFavorite`/`removeAttachment`, `DiaryEditorViewModel.wordCount`/`characterCount`/`hasAttachments`); DAO/repo members (`HabitDao.observeById`, `HabitCompletionDao.observeAllForHabit`/`getAllInRange`, `TaskDao.observeAll`/`observeCountForDay`/`observeCompletedCountForDay` + repo wrappers, `TaskRepository.getById`); theme tokens (12 `Color.kt`, 2 `Spacing.kt`); `DateTimeUtils` dead date helpers; `strings.xml` `tagline`; `proguard-rules.pro` AI keep rule.
- **Build/deps** (`app/build.gradle.kts`): removed `androidx.biometric:biometric:1.1.0`, `ui-tooling-preview` (debug-only), and unused androidTest deps (`espresso-core`, `ui-test-junit4`); added explicit `androidx.lifecycle:lifecycle-runtime-compose:2.8.7` (previously transitive) and an androidTest `androidx.collection:collection:1.4.4` pin (offline-cache resolution gap for compose-ui's androidTest variant; a patch-level no-op). Release now `isShrinkResources = true` — verified the app never calls `getIdentifier()` (0 usages).
- **Security:** `MainActivity.AppLockGate` made fail-closed — `collectAsStateWithLifecycle(initialValue = null)`; a null lock state renders an opaque placeholder and never content, so a missing/not-yet-loaded state cannot leak the app behind the PIN lock.
- **Threading:** PBKDF2 PIN hashing/verification (`SettingsStore.enablePinLock`/`attemptPinUnlock`/`attemptRecoveryAnswer`) moved off the main thread to `Dispatchers.Default`; `BackupRepository.exportToFile`/`importFromFile` JSON encode/decode + file I/O moved to `Dispatchers.IO`.
- **Storage:** backups are now pruned to the newest `MAX_KEPT_BACKUPS = 3` after export, and export filenames use `BuildConfig.VERSION_NAME` instead of a hardcoded version string.
- **Performance:** `DiaryDetailViewModel.entry` now observes a single row via new `DiaryDao.observeById`/`DiaryRepository.observeById` instead of `observeAll().map(firstOrNull)`; fixed a `MainScope()` coroutine leak on the copy-snackbar; `HomeScreen` LazyColumn now uses `key` + `contentType` (TimelineItem ids are unique: `task-`/`habit-`/`expense-`/`diary-`); `HabitsScreen` remembers today's completion flow keyed on epoch day; `PinKeypad` key rows hoisted to a static top-level val; `ProfileScreen` bitmap decoding is a two-pass `inSampleSize` decode.
- **Theme:** restored `LifeOSSuccess` and used the `LifeOSDanger`/`LifeOSWarning`/`LifeOSSuccess` tokens (not raw hex) for task priority coloring in `TasksScreen`.
- **Tests:** `AppDatabaseMigrationTest` seed INSERTs corrected to match `2.json` exactly (notes/captures columns). Three unit-test fakes gained the new `when`-covered `observeById`.
- **Docs:** `07_AUTHENTICATION.md` (biometric dep is gone), `11_AI_SYSTEM.md` (OkHttp is Coil's unreachable transitive baggage — no INTERNET permission, no egress path), `20_FOUNDER_GUIDE.md` (App Lock is NONE/PIN only).
- **Scope:** 38 files, +160/−368 lines.

### Verification (offline, local Gradle 8.9)

- `:app:compileDebugKotlin` — BUILD SUCCESSFUL.
- `:app:testDebugUnitTest :app:assembleDebug` — BUILD SUCCESSFUL (all unit tests green).
- `:app:assembleRelease` — BUILD SUCCESSFUL (R8 minify + shrinkResources).
- `:app:lint` — 0 errors, 6 warnings (all pre-existing in untouched files).
- `:app:compileDebugAndroidTestKotlin` (via `kspDebugAndroidTestKotlin`) — BUILD SUCCESSFUL; migration tests compile though no device/emulator is available to execute them here.

### Preserved

- `DiaryViewModel`/repository boundaries, Room schema and migrations, navigation, media handling, permissions, offline/privacy behavior, `SettingsStore.disableAppLock`, FLAG_SECURE, reminder lock-screen content, fine-location behavior, Coil (deferred), reminder scheduler (`reproject` left as-is), UPDATE.md history.

- **Five-day date strip:** kept the existing centre-snapping/date-bounds logic and refined the visible cell geometry so the Diary presents a balanced five-position strip with a clearer selected-day pill.
- **FAB positioning:** removed the extra empty-state-only bottom offset so both empty and populated Diary states use the same shared FAB clearance above the existing bottom navigation/safe area.
- **Timeline connector:** changed the saved-memory timeline rail so the mood node remains outside the card on the left and a short horizontal hairline connects the node directly to the card's left edge, matching the supplied reference while keeping the vertical rail content-aware.
- **Preserved:** Diary ViewModel/state, repository/use cases, Room/database schema and migrations, navigation, media, permissions, offline/privacy behavior and dependencies.
- **Files changed:** `app/src/main/java/com/lifeos/app/ui/diary/DiaryDateStrip.kt`, `app/src/main/java/com/lifeos/app/ui/diary/DiaryScreen.kt`, `app/src/main/java/com/lifeos/app/ui/diary/MemoryTimeline.kt`.

### Verification

Source was re-read after each targeted edit. The connected GitHub environment does not expose the local Android Gradle runtime, emulator/device or AVD, so compile/test/lint/device success is **not** claimed for this pass.

## 2026-09-28 — Five-day strip centres the selected day

- **Date strip now centres today.** `dayStripRange` ended at `today`, so the default selected day (today) was the strip's last cell and could never sit centred — only three of five positions were realised. The range now adds a bounded `FORWARD_DAYS = 2` window past today so the selected day occupies the middle cell (e.g. SAT 26 | SUN 27 | MON 28 | TUE 29 | WED 30). Future cells are visual-only; `DiaryViewModel.selectDay` still clamps all selections to `today-365 .. today`, so tomorrow cannot be journaled.
- **Memory body text** raised to the reference presence (18sp / 28sp line-height) in `MemoryTimeline.kt`.
- **Tests.** `DayStripRangeTest` pins the new bounds: forward window present, bounded, inclusive of both new ends; history-window, contiguity and five-dates guarantees preserved. Verified offline: full `testDebugUnitTest` green (the later flaky run failed only on pre-existing coroutine timing in `DiaryViewModelTest`/`DiaryDetailViewModelTest`, unrelated to this change), `compileDebugKotlin` clean, `lintDebug` (0 errors, 8 pre-existing issues).
- **Files changed:** `app/src/main/java/com/lifeos/app/ui/diary/DiaryDateStrip.kt`, `app/src/main/java/com/lifeos/app/ui/diary/MemoryTimeline.kt`, `app/src/test/java/com/lifeos/app/ui/diary/DayStripRangeTest.kt`.
- **Preserved:** date architecture and selection logic, ViewModel/repository boundaries, Room schema and migrations, navigation, no new dependencies.

# LifeOS — UPDATE

Change log for the `fix/audit-hardening` branch (UI/UX + navigation audit and redesign, 2026-09-17).
---

## 2026-09-28 — Diary connected timeline wire

- **Timeline rendering fix** (`ui/diary/MemoryTimeline.kt`). Replaced the fixed-height decorative `MemorySpine` with a content-aware timeline lane that fills the full height of each memory row and draws one continuous 1dp hairline through the mood node. The line now naturally spans long text, photo previews and tags instead of stopping after a small fixed segment.
- **First/middle/last handling:** the first item starts the rail at the node, middle items connect from the previous row through the node to the next row, and the last item terminates at its node. The existing `DiaryEntity`, `DiaryViewModel`, repository, Room schema, navigation and media flows are unchanged.
- **Spacing:** preserved the existing 58dp time column, 14dp timeline lane and 6dp card gap; only the spine geometry changed so the supplied connected-wire reference is achieved without hardcoded card-height assumptions.
- **Verification:** source-level review completed. GitHub Actions will run `gradle test assembleDebug assembleRelease --stacktrace` for the pull request. No device/emulator verification is claimed from this environment.

## 2026-09-26 — Diary audit follow-up (branch `feat/stitch-timeline-diary`)

A read-through of the merged Diary against the Stitch reference screens turned up
three defects and one missing design, all now fixed. No schema change, no new
dependency, no network, no permission change.

### 1. The voice-note timer never moved
`AudioRecorder.tick()` carries the contract "advance the elapsed-time reading.
Driven by the UI while recording", and `DiaryEditorViewModel.tickRecording()`
existed to call it — but nothing did. `DiaryVoiceNoteRow` reads
`elapsedMillis` and `amplitude` from `state.recording`, and that field was only
written by `start()` (0 ms, amplitude 0) and by `stop()`. A take therefore
displayed `0:00 Recording…` and a motionless level dot for its entire length: the
UI was presenting a live measurement it never received.

`DiaryEditorAttachments` now polls while a take runs. The effect is keyed on
`state.recording is RecordingState.Recording`, so the loop begins with the take
and is cancelled the moment it ends; `while (isActive)` tears it down with the
composition even if a stop is missed. Poll interval is 100 ms — fast enough that
`m:ss` does not visibly jump and the level dot smooths rather than strobes, and
`MediaRecorder.maxAmplitude` is cheap enough to read at that rate.

### 2. The save confirmation could not do what the design asks of it
The Stitch `saved_confirmation.html` screen is built around two actions: *View
memory* and *Add another memory*. The implementation was a 1.4 s auto-dismissing
toast, which structurally cannot offer either.

It is now a scrim plus a paper card carrying the two full-width pill actions. The
card's copy is derived from the stored row, not from the save event, so:

- a back-dated memory confirms the day it was actually filed under;
- an edit confirms the minute it kept, not the time the save happened.

"Add another memory" calls `startNewEntry()`, which re-seeds the draft from the
selected day — writing a second memory from a past day keeps it in that day.

### 3. Confirmation state moved into the ViewModel
It was a local `var showSaved` in `DiaryScreen`, which is exactly the kind of
state that cannot be tested. `DiaryViewModel` now holds `savedEntryId` and
derives `savedEntry` by resolving that id against the existing Room flow. Only
the id is stored, so the sheet cannot present a stale copy of a row that has since
changed. `selectDay`, `startNewEntry` and `startEdit` retire it; Back and a
scrim tap dismiss it. Ten new JVM cases cover the sheet's lifecycle and the
recording poll.

### 4. Dead code, one item of which was also wrong
Removed, after confirming repo-wide that nothing referenced them:
`EntryMoodPill`, `DiaryMoodChip` and `ThemeKeywordChip` (superseded by
`MoodSelector`'s hairline glyph row and by quiet inline tags), and
`DiaryEditorViewModel.recordingState` / `playbackState`.

`DiaryEditorViewModel.startAnother()` is deleted too, and it was the interesting
one: unused, and hard-coded `today()` + `nowMinutesOfDay()`, so had the
confirmation called it, "add another memory" would have filed the new memory
under today while the user sat reading a past day. `startNewEntry()` already
does the right thing.

### 5. Housekeeping
`DiaryDetailScreen.kt` imported `LocationOn` and `Star` twice each.

### Verification
Run offline against the Gradle 8.9 install at `/opt/gradle-8.9/bin/gradle`
(the repository has no `gradlew` checked in):

- `:app:compileDebugKotlin` — pass
- `:app:testDebugUnitTest` — **157 tests, 0 failures** (was 147)
- `:app:lintDebug` — 0 errors; 8 pre-existing issues, none in `ui/diary`
- `:app:assembleDebug` — pass (42.5 MB debug APK)
- `:app:assembleDebugAndroidTest` — pass

**Still not verified:** this sandbox has no emulator, device or AVD. The
confirmation sheet and the recording poll have not been viewed on a screen, and
the v4→v5 migration test still has not run against a real SQLCipher file. The
recording fix is pinned at the ViewModel contract by unit tests, not by watching
a take run.

---

## 2026-09-26 — Diary rebuilt on real data (branch `feat/stitch-timeline-diary`)

Turns the Diary from a visual mock-up into a working feature. Every attachment
is a real on-device artifact and every derived number is computed from stored
state — no sample entries, no generated weather, no network.

### Data / schema
- **Room v4 → v5** (`AppDatabase.MIGRATION_4_5`) adds a single column,
  `diary_entries.isFavorite INTEGER NOT NULL DEFAULT 0`. Additive and
  reversible-by-rollback only; no existing column is dropped or retyped, so no
  user data is rewritten. Exported as `schemas/…/5.json`.
- Photos, voice notes and the captured place were **already** storable: the
  `attachmentsJson` column existed since v1 and was simply never written to.
  `DiaryAttachment` (`domain/model/`) is a sealed, `kotlinx.serialization`
  polymorphic type — `Photo`, `VoiceNote`, `Place` — persisted in that column
  under a stable lowercase `kind` discriminator. `DiaryAttachments` encodes,
  decodes and groups them. Decoding is deliberately **total**: null, blank or
  corrupt payloads degrade to "no attachments" rather than throwing, so one bad
  row can never take down the list. Unknown keys are ignored, so a row written by
  a future build still reads.
- `DiaryRepository` gained `toggleFavorite`/`setFavorite`,
  `removeAttachment(id, filePath)` and media lifecycle handling. `updateEntry`
  deletes files that the new attachment set no longer references, and re-reads
  the row first so a favourite toggled elsewhere is never clobbered. `delete`
  removes the entry's photos and audio too, so no orphans are left behind.

### Composer (`ui/diary/DiaryEditor.kt`, `DiaryEditorViewModel.kt`)
- The bottom sheet was **deleted** and replaced by a full-screen composer. It is
  **not a navigation route**: there is no `Screen.DiaryEditor`. Both the day
  list and the detail screen own the composer as an overlay (`showEditor`), so
  Back closes a draft in place and never leaves a half-written memory on the
  navigation stack.
- `DiaryEditorViewModel` owns the draft, the captured minute, and the write, and
  reports commits through a monotonic `saveCount` rather than a boolean — saving
  the *same* memory twice is two events, and keying a one-shot UI effect on the
  id would swallow the second. The list screen shows the "Saved" confirmation and
  closes the composer; the detail screen closes it and leaves the updated entry
  in place.
- Title, body with a live character counter, 5-mood picker, tag editor, and a
  date/time picker. Saving requires a non-blank body.
- **Save cannot race the load.** Opening an existing memory reads the row
  asynchronously, and until it lands the draft has no id — so `canSave` is false
  while `isLoading` is true. Without that gate, a fast typist could save before
  the read completed and *insert a duplicate* instead of updating. A monotonic
  `loadToken` likewise drops a stale read that a newer open has overtaken.
- **Photos** — Android Photo Picker (`PickVisualMedia`), so no storage or
  media permission is needed. Each pick is copied into app-private storage so
  the attachment survives the source being deleted.
- **Voice notes** — real `MediaRecorder` (`DiaryAudioRecorder`) to AAC/M4A in
  app-private storage with the platform-measured duration, played back with
  `MediaPlayer` (`DiaryAudioPlayer`). Leaving the screen cancels an in-flight
  recording and releases the decoder; a file that has vanished from disk
  reports a "no longer on this device" state rather than failing silently.
- **Location** — `DeviceLocationProvider` uses only platform `LocationManager`
  and `Geocoder` (no Play Services, no third-party SDK). Coarse is enough for a
  place name; GPS is only read when fine access is granted. A geocoder backend
  that is absent or slow yields empty coordinates-only, never an invented city.
- **Weather** — `WeatherRepository` models `DiaryWeather` as
  `Recorded` / `Unavailable(reason)`. This build has **no permitted weather
  source** (no `INTERNET` permission, so no HTTP client is possible), so the row
  honestly reports unavailable. The branch is drawn so a future on-device source
  can be added without touching the UI.

### List & detail
- `DiaryScreen` renders the selected day from `observeForDay`, with a week
  strip, a timeline spine, and correct loading/empty states. Timestamps are
  formatted from the stored `timeMinutes`.
- `DiaryDetailScreen` rewritten: photos, playable voice note, tags, place,
  weather, and the real derived metadata (full date, time, word count computed
  from the stored body so it cannot drift). Actions: **Share** (system chooser;
  text plus a one-shot `FileProvider` read grant for the first photo),
  **Copy** (clipboard), **Edit**, **Delete** (confirmation dialog, then real
  deletion), and **Favourite**. Photos and the voice note can be removed in
  place, without opening the composer.

### Permissions & privacy
- Manifest adds `RECORD_AUDIO`, `ACCESS_COARSE_LOCATION` and
  `ACCESS_FINE_LOCATION`, plus non-required `microphone`/`location` features.
  `CAMERA` is deliberately **not** declared. `INTERNET` remains absent.
- Every prompt is user-initiated: the OS dialog only ever appears in response to
  tapping "Add location" / recording. Reuse of the existing
  `PermissionManager` semantics means a permission the user has already denied
  twice routes to system Settings instead of silently no-op'ing; both the
  location and mic actions re-read the live OS grant at the point of use, so a
  grant made in Settings is picked up on return.

### Testing
- `AppDatabaseSchemaTest` — three new v5 assertions: `isFavorite` exists, every
  pre-v5 column survives, and `attachmentsJson` is still there.
- `AppDatabaseMigrationTest` — `migrate4To5_addsIsFavoriteAndKeepsExistingEntries`
  seeds a real v4 row, migrates, and asserts the row survives, the content is
  byte-identical, `isFavorite` is backfilled to `0` (not NULL), and the new
  column is writable.
- New `DiaryAttachmentsTest` — 8 tests over the codec: round-trip of all three
  kinds, the stable `kind` tag, empty/null handling, corrupt-payload
  degradation, forward-compatibility with unknown fields, the accessors, and a
  place with no resolved name.
- `DeviceLocationProvider` satisfies lint's `MissingPermission` with an explicit
  per-provider grant check plus explicit `SecurityException` /
  `IllegalArgumentException` handling.

### Verified
`testDebugUnitTest` (119 tests, 0 failures), `assembleDebug`,
`assembleDebugAndroidTest` and `lintDebug` (0 errors) all pass, offline.

### Remaining
- `migrate4To5_addsIsFavoriteAndKeepsExistingEntries` is written but has **not**
  been executed: it is an instrumentation test and no emulator or device is
  attached to this environment. Run
  `:app:connectedDebugAndroidTest` before release.
- Weather stays unavailable until an on-device source exists.
- Geocoding is best-effort; on devices without a geocoder backend the place
  shows as coordinates.

---

## 2026-09-26 — Diary date strip, honest timestamps, startup fix (branch `feature/diary-date-strip-and-startup`)

Three problems, one pass. The Diary gains a real date picker, diary timestamps stop
lying, and the multi-second cold start gets an actual root cause. Room stays v4 with
its three migrations untouched, navigation is unchanged, and the app stays offline.

### 1. The date strip replaces the ticks and chevrons

The 2026-09-25 redesign gave day navigation seven ambiguous dots — a filled tick
meant either "selected" or "has memories" depending only on its size — plus two
chevrons, and gave the `displayLarge` day numeral more visual weight than the
memories themselves. Both are gone.

- `ui/diary/DiaryDateStrip.kt` *(new)* — a `LazyRow` of weekday-over-numeral cells
  that keeps the selected day centred. Everything is **derived, not hard-coded**: a
  cell is exactly one `VISIBLE_DATES`(=5)th of the measured width, so five dates fit
  a small phone, a large phone and a landscape window with no magic dp value, and it
  re-derives on configuration change instead of caching a stale pixel width.
- **Bounded by construction.** The range is `today-365 .. today` — 366 fixed cells,
  only ~5 ever realised, and no unbounded or ever-growing list. There is no tomorrow
  to journal, so the upper bound is `today` itself. `dayStripRange()` is a pure
  function precisely so the bound is unit-testable.
- **Centred flings** via `rememberSnapFlingBehavior(state, SnapPosition.Center)` from
  the `snapping` API already present in the resolved compose-foundation 1.7.5. No new
  dependency and no reaching into `LazyRow` internals. (There is deliberately no
  manual fling hook: the built-in provider is the version-correct path.)
- **One haptic per real day change**, keyed on the *selected* value rather than the
  tap or the drag. A tap that also re-centres the strip therefore cannot double-fire,
  and holding a finger down produces nothing. The day-swap is likewise driven by the
  index the strip has *settled* on, not by intermediate scroll positions.
- The "which days have memories" signal the ticks carried is preserved as a small
  dot under the numeral — now unambiguous, because selection is a tinted pill.
- `ui/diary/DiaryDayHeader.kt` — back + `+ Memory` row, then a compact hierarchy:
  `TODAY` eyebrow over `26 September · Saturday`. The strip is full-bleed so a
  centred cell is genuinely centred; the label above keeps its screen padding.
- `DiaryDayHeader`'s `canGoForward` / `onPrevious` / `onNext` parameters and
  `DiaryViewModel.shiftDay()` are **removed** — the strip is now the only day-navigation
  control, so they were dead weight. `selectDay()` clamps to the same range, in the
  ViewModel rather than the composable, so no caller can park the screen on a day the
  strip cannot render.

### 2. Timestamps stop lying

`DiaryViewModel.saveEntry` called `DateTimeUtils.nowMinutesOfDay()` **at save time**.
Open the composer at 8:04, write for twenty minutes, save, and the memory was filed
at 8:24 — the time silently moved, and there was no way to see it before committing.

- The minute is now captured in `startNewEntry()` / `startEdit()` and reused on save.
  `startEdit` captures the entry's *existing* `timeMinutes`, so re-saving an edited
  memory can never move it in the day's timeline.
- `timeMinutes` is captured once when the composer **opens** (from an injected
  clock, so the rule is testable on a JVM) and is rendered in `DiaryEditor` under
  the date
  with a small clock glyph (`Written at 8:04 AM` when editing), so the value that will
  be stored is visible before the save, and it does not drift while the user types.
- No schema change: `DiaryEntity.timeMinutes` and `createdAt` already existed.
  `DiaryDetailScreen` passes the stored `timeMinutes` through; it only ever edits.

### 3. Startup: the actual root cause

- **`ServiceLocator` was forcing SQLCipher open on the main thread.** Every member was
  an eager `val`, and each one dereferences `database` — directly, or through a
  repository that already holds a DAO — so the first assignment in
  `LifeOSApplication.onCreate()` called `AppDatabase.getInstance()` before `setContent`:
  native library load, Keystore load + AES/GCM unwrap, SharedPreferences read and the
  Room build. All members are now `by lazy`, which also means a feature nobody opens
  never pays for its repository.
- **The warm-up query was reading a whole table.** `AppDatabase.warmUpOpen()` used
  `habitDao().getAllForBackup()`, deserialising every habit row on the startup path —
  waste that grew with the user's data, paid for while the splash was still up. It is
  now `openHelper.writableDatabase.query("SELECT 1")`, which reaches the same code path
  at constant cost, inside `withContext(Dispatchers.IO)` so the blocking open can never
  land on the main thread regardless of caller.
- **`MainActivity` composed the app behind the splash.** It drew a background-coloured
  `Surface` over `LifeOSNavHost()` while the database was still opening — but
  *composing* the nav host builds Home's ViewModel, whose repository chain is exactly
  what forces `AppDatabase.getInstance()`. The overlay would have quietly put the
  SQLCipher open straight back on the main thread. The content is now genuinely gated
  on `DatabaseInit.Ready`; the native splash covers the wait, so nothing is lost.
- **`core/util/StartupTrace.kt` *(new)*** — `android.os.Trace` only, so the framework
  compiles it to a no-op unless a Perfetto/systrace session is attached: production
  cost is one boolean check per call site and nothing reaches logcat. Sections:
  `lifeos:Application.onCreate`, `lifeos:di.build`, `lifeos:db.open` (async),
  `lifeos:db.passphrase`, `lifeos:MainActivity.setContent`, `lifeos:home.firstFrame`.
  Supported from API 29, where `Trace.isEnabled()` was added; below that every entry
  point collapses to a bare call of the wrapped block.
- No logging, no `INTERNET` permission, no new runtime dependency.

### Animation
Restrained and system-setting-aware throughout (Compose animation coroutines already
honour "Remove animations", so no duration-scale plumbing was needed). The strip's
selected pill and numeral use non-bouncy springs; the mood label cross-fades over
160/110ms instead of snapping, since it changes on every tap and an instant swap
reads as a flicker exactly when the user is looking for confirmation;
`Modifier.fadeInAsContent()` (new, in `DiaryMotion.kt`) fades the empty state's open
spine in with no translation, because the state is already vertically centred and a
slide would read as the layout moving.

### Tests
New `DayStripRangeTest` (8 cases) pins the strip's bounds: ends on today, starts
exactly one window back, ascending and contiguous, no repeats, never a future day,
honours a narrower window, and asserts the year-long bound on purpose.
New `DiaryViewModelTest` (13 cases) covers the open-time-vs-save-time distinction,
per-entry stamps, edit preservation of both `timeMinutes` and `createdAt`, editor
minute lifecycle, past-day filing, the day clamps, day-scoped content and
`daysWithMemories`. The ViewModel gained two defaulted clock seams
(`nowMinutes`, `todayEpochDay`) purely so these are deterministic; production call
sites are unchanged. `kotlinx-coroutines-test:1.9.0` was added **test-only**,
version-matched to the existing `kotlinx-coroutines-android` so no second coroutines
is pulled in.

### Verification (offline, no network)
`/opt/gradle-8.9/bin/gradle --offline :app:testDebugUnitTest :app:assembleDebug
:app:assembleDebugAndroidTest :app:lintDebug` → **BUILD SUCCESSFUL**;
**132 unit tests, 0 failures/errors** (up from 111); lint **0 errors**, and every
remaining warning is pre-existing in files this change does not touch. Also
confirmed by diff review: no `Log.`/`println`/TODO, no secrets, no `INTERNET`
permission, and no change under `data/db/` migrations, `schemas/`, `ui/navigation/`,
`androidTest/` or the manifest.

### Not verified here
No emulator, device or AVD is available in this sandbox, so there are **no measured
before/after cold-start timings** — the trace sections above are the instrumentation
that makes that measurement possible on real hardware, not a substitute for it. The
UI has likewise not been viewed on a screen. Both remain open.

## 2026-09-25 — Diary redesigned as *Daily Memory* (branch `feat/diary-daily-memory-redesign`)

The Diary stops being a list of paper cards with a date strip and becomes an
editorial memory timeline you read one day at a time. UI layer only — the Room
schema (v4), entities, DAOs, migrations, repositories, navigation routes, DI
and Gradle dependencies are all untouched, and the app stays fully offline.

### Files
- `ui/diary/DiaryScreen.kt` — rewritten around `DiaryDayHeader` +
  `MemoryTimeline`; inline `+ Memory`; day-scoped `YOUR STORY STARTS HERE`
  empty state; `BackHandler` layered so back closes the editor, then the screen.
- `ui/diary/DiaryDayHeader.kt` *(new)* — back, prev/next chevrons, day
  eyebrow, `1 January · Tuesday` headline and a memory-position tick row.
- `ui/diary/MemoryTimeline.kt` *(new)* — `MemoryMoment`: 44dp time gutter,
  1dp spine with tapered first/last ends, mood dot, uppercase mood, journal
  text at 28sp leading.
- `ui/diary/DiaryEditor.kt` *(new)* — full-screen composer replacing
  `DiaryEditorSheet.kt` (deleted): mood-first, 8 moods, saving-aware action.
- `ui/diary/DiaryEmptyState.kt`, `MoodSelector.kt`, `MemoryDeleteDialog.kt`,
  `DiaryMotion.kt` *(new)*.
- `ui/diary/DiaryDetailScreen.kt` — route + ViewModel kept; adopts the same
  spine/mood/leading so a memory opened alone still reads as part of its day.
- `ui/diary/DiaryMoods.kt` — data-driven, 8 moods; `ui/theme/Color.kt` —
  mood/neutral tokens for the new palette.

### Deliberately not a Timeline clone
Timeline uses a 2px spine with 30dp paper badges and 24dp-radius cards. Diary
uses a 1dp spine, **no cards**, mood dots and type as the loudest element — the
two surfaces stay visibly different.

### Mood storage — additive, no migration
Mood is a `TEXT` column, so new moods need no schema work. Angry / Anxious /
Tired are added; the five original keys are preserved byte-for-byte and
`fromStored` still resolves them, so existing entries keep their mood.
`DiaryMoodsTest` now asserts all 8, plus unique labels/keys and legacy-key
compatibility.

### Bug fixed
`DiaryViewModel.saveEntry` stamped every new entry with `today()`, so writing
while reading a past day saved to today. It now writes to `selectedDay`. Added
an in-flight `saving` guard (reset in a `finally`, so a failed write cannot
wedge the composer) and a `shiftDay` guard refusing to move past today.

### Verification (offline, no network)
`/home/gradle-8.9/bin/gradle --no-daemon testDebugUnitTest assembleDebug lintDebug`
→ **BUILD SUCCESSFUL**; 111 unit tests, 0 failures/errors; lint 0 errors and 0
diary-related issues. Also confirmed: no secrets/logging/network in `ui/diary/`,
`INTERNET` permission still absent, and no diff under `data/`, `core/`,
`ui/navigation/`, `ui/components/`, `androidTest/` or `schemas/`.

### Docs corrected (pre-existing drift, unrelated to this change)
- Diary feature row + UI/UX section described removed Notes/Capture/Search/AI
  directories and a non-existent `core/intelligence/DiaryConnections.kt`.
- Bottom bar documented as four destinations including Insights; it is three
  (Home/Tasks/Habits), locked by `BottomNavItemsTest`.
- Two "Room v2" references should read v4.
- Not fixed (out of scope, still stale): README's Capture/Timeline/Expenses
  sections and the `docs/` historical entries.

## 2026-09-24 — Timeline & Diary visual pass from Stitch (branch `feat/stitch-timeline-diary`)

Follows the "LifeOS Timeline Overview" + "LifeOS Diary / Journal" Stitch
reference screens. Visual layer only — no navigation architecture, Room schema
(v4), repository or use-case changes; the app remains fully offline-first.

### Timeline (`ui/timeline/TimelineScreen.kt`)
- Header replaced with a centered dated headline and back/previous/next
  chevrons; the same Single-day navigation semantics (`selectedDate`,
  `LaunchEffect` reload, `canGoForward` guard) are preserved.
- Entries now render as a dated journal: a 2px `#E8E1EA` hairline spine on a
  fixed 32dp left track, 30dp paper node badges (`DiaryPaperCard`,
  1dp hairline border) carrying each entry's emoji, and 24dp-radius cards with
  title, subtitle and a time pill. First/last entries get tapered spine ends.
- Empty state upgraded to mirror the Diary's (lavender circle + timeline icon
  + title + caption).
- New `onOpenItem: (TimelineItem) -> Unit` callback (default no-op). The Nav
  host routes DIARY → Diary detail, TASK/HABIT/EXPENSE → their existing
  sections; system Back returns to the Timeline.
- `TimelineViewModel` and `BuildTimelineUseCase` untouched.

### Diary (`ui/diary/`)
- New pastel mood tokens in `ui/theme/Color.kt` (`DiaryMood*Pastel`,
  `DiaryHairline` `#E8E1EA`, `DiarySaveDisabled` `#F4ECFF`).
- `DiaryMoods.backgroundOf()` maps the five stored moods to their pastel fill;
  `EntryMoodPill` (list + detail) and the editor's selected mood chips now use
  pastel backgrounds with the saturated accent text. Mood keys persisted are
  unchanged, so existing entries keep their mood.
- Save button restyled as a flat lavender pill (`DiaryLavender` fill,
  `DiaryInkViolet` label, disabled `DiarySaveDisabled`, zero elevation).
- Day-strip chips: 16dp capsules, unselected bordered by the `#E8E1EA` hairline;
  selected still fills `DiaryLavender`.

### Database
- None. Room schema stays v2; `diary_entries` and all readers untouched.

### Verification (offline, deps cached)
- `gradle :app:testDebugUnitTest` — 108 tests, 0 failures.
- `gradle :app:assembleDebug` — BUILD SUCCESSFUL.
- `gradle :app:lintDebug` — 0 errors; 23 pre-existing warnings, none in changed
  files.

---

## 2026-09-24 — Coalesced, reliable alarms: one intelligent alarm per trigger time (branch `feat/reliable-coalesced-alarms`)

### Root causes fixed
1. **Doze per-app alarm limit.** The old scheduler armed one `AlarmManager`
   alarm per reminder (`lifeos://reminder/<id>`). Doze allows each app only
   ~once per 9 minutes of exact delivery across *all* its alarms, so beyond a
   handful of reminders the extras stopped firing — matching the "stops after
   5–6 reminders" symptoms.
2. **Delivery depended on the app being open.** A reminder delivered only a
   notification + a best-effort `startActivity`; in the background/locked/Doze
   states the screen could never show and the cold SQLCipher open inside
   `goAsync()` could exceed the broadcast window.
3. **No coalescing.** Two reminders at the same time produced two alarms →
   overlapping alarm UIs + double audio.

### What changed
- **`AlarmEvent` + `AlarmEventProjector` (new, pure).** The reminders table is
  now projected into one *event per distinct trigger time* (snooze returns
  override the real trigger; future-only; events sorted by time, items by id;
  **no caps** — the number of real alarms equals distinct trigger times, never
  the number of reminders). `id` = `event:<triggerAt>`.
- **`AlarmEventCodec` + `AlarmIntents` (new).** The full event snapshot travels
  inside each PendingIntent/Activity intent (trigger time, item ids/titles), so
  the alarm presents **instantly from extras with zero DB reads**. Stable
  identities make re-projection cancel/refresh idempotent (`FLAG_UPDATE_CURRENT`).
- **`ReminderScheduler` (rewritten).** One alarm per event via the
  exact-when-permitted ladder: `setAlarmClock` (Doze-exempt, no 9-minute limit,
  FGS-start allowed) when exact access is granted or API < 31, else
  `setAndAllowWhileIdle` (permission-free, never throws). `reproject()` diffs
  against a `scheduled_event_keys` preference set, cancels what left the set and
  re-arms the rest.
- **`AlarmPlaybackService` (new).** A `mediaPlayback` foreground service that
  *owns the audible alarm*: looping `TYPE_ALARM` tone + vibration + partial
  wakelock, plus ONE high-priority notification with a full-screen intent and
  STOP/Snooze actions. Started from the alarm broadcast (platform-permitted for
  `setAlarmClock`, and `mediaPlayback` is a background-startable FGS type), it
  rings even in deep Doze / locked / app-in-background. If `startForeground` is
  refused, it degrades to a single HIGH-importance notification carrying the
  event's own sound/flags. The full-screen activity plays **no sound** — no
  double-ring by construction.
- **`AlarmReceiver` (rewritten).** Decodes the event from extras, starts the
  FGS (fallback notification if refused), then `goAsync()`+IO runs
  `handleEventFired(ids)` and re-projects. Presentation never waits on the DB.
- **`AlarmFullScreenActivity` (rewritten, Compose/Material 3).** Dark LifeOS
  alarm screen: "TASK TIME"/"HABIT TIME"/"IT'S TIME" (existing string keys
  re-valued), scheduled time, grouped TASKS/HABITS sections, prominent STOP,
  snooze 5/10/15/20/60 with a Tasks/Habits/Both target selector when an event
  mixes types (per-type snooze). `setShowWhenLocked`/`turnScreenOn`/keep-screen-
  on preserved; `noHistory` + `singleTop` + CLEAR_TOP prevents stacked alarm UIs.
- **`ReminderRepository` (single `reprojectAll()` funnel).** Every mutation —
  create/edit/delete/fire/snooze/rebuild/toggle — ends in one projection pass;
  `normalizePastDue()` fast-forwards recurring reminders past missed windows and
  disables dead one-shots, `handleEventFired` advances the per-fire state, and
  `snoozeMany` supports per-type snooze.
- **`BootReceiver`:** TIME_SET / TIMEZONE_CHANGED now rebuilds the reminders
  table from the task/habit `reminderEpochMillis` wall-clock mirrors
  (`rebuildFromMirrors`) so recurring reminders stay pinned to local time.
- **Habits parity:** the habit add-dialog and habit-detail Reminder card now
  gate timed alarms behind `ExactAlarmPermissionHost` (SCHEDULE_EXACT_ALARM)
  chained after the notification-permission host — same care as tasks.
- **Manifest:** added `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`,
  `WAKE_LOCK` and the `mediaPlayback` FGS service declaration.
- **strings.xml:** kept the existing alarm keys (re-valued titles), added
  snooze-target, section, snooze-action and multi-item strings.

### Database
No Room schema/migration change. DB stays at version 4; `reminders` table and
all retained tables are untouched.

### Verification (offline, deps cached)
```text
gradle :app:testDebugUnitTest  -> BUILD SUCCESSFUL (108 tests, 0 failures
                                     incl. 9 new AlarmEventProjectorTest)
gradle :app:assembleDebug      -> BUILD SUCCESSFUL
gradle :app:lintDebug          -> BUILD SUCCESSFUL (0 errors; only pre-existing
                                     dependency/Info warnings)
```
On-device checks still recommended (no emulator here): an actual alarm firing
in Doze, lock-screen FSI/heads-up, per-type snooze, reboot/time-zone re-arm.

---
---

## 2026-09-23 — Exact-alarm permission flow (SCHEDULE_EXACT_ALARM), shared & permission-safe scheduler

Task and habit reminders have always used the permission-free, Doze-aware
inexact `AlarmManager` API, which is reliable but delivers reminders only when
the system batches them. This pass adds Android 12+ special access so timed
task reminders can fire at the exact scheduled minute.

### What changed
- **Manifest** (`AndroidManifest.xml`): declared `SCHEDULE_EXACT_ALARM`. This is
  a *special access* the user must enable on the system "Alarms & reminders"
  page — never requested at launch, and its absence can never crash or throw.
- **`ReminderScheduler`** (shared by tasks and habits): when exact scheduling
  is granted it uses `setExactAndAllowWhileIdle`; otherwise it falls back to the
  existing permission-free `setAndAllowWhileIdle`. Both are Doze-aware; a
  `SecurityException` can never propagate, so nothing throws and no reminder is
  left as a silent zombie. Habits keep their existing ungated flow unchanged.
- **`PermissionManager`**: added `openExactAlarmSettings(context)` → system
  `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` page (fallback to app details on old
  Android).
- **`ExactAlarmPermissionHost`** (new, `ui/components`): mirrors
  `ReminderPermissionHost`. Shows an explanatory dialog with "Open Settings" /
  "Not now" and re-checks the grant on every `ON_RESUME` (returning from the
  system page), releasing the held action only once the grant is present — it
  never claims "reminder scheduled" while access is still denied.
- **`SettingsScreen`**: added an "Alarms & reminders" row that reads the *live*
  system grant (allowed / not allowed / not required on API < 31) and deep-links
  to the exact-alarm system page. No fake in-app switch.
- **`TasksScreen`**: the task-reminder save is gated behind exact-alarm access
  (via `ExactAlarmPermissionHost`); habits call it untouched. `POST_NOTIFICATIONS`
  permission remains required for notifications.
- **`strings.xml`**: added the settings-row and `exact_alarm_dialog_*` strings.

### Database
No Room schema/migration change. DB stays at version 4; `reminders` table
unchanged; no columns dropped or added.

### Verification (offline, deps cached)
```text
gradle :app:compileDebugKotlin -> BUILD SUCCESSFUL
:app:testDebugUnitTest        -> BUILD SUCCESSFUL (12 suites, 99 tests, 0 failures)
:app:assembleDebug            -> BUILD SUCCESSFUL (APK + dex packaged)
:app:lintDebug                -> 0 errors, 0 warnings
```
Exact-alarm scheduling is Android-version-conditional (only lives on API 31+, is
granted by the user on the system page, and only ever *armed* on-task-save), so
it is not exercised by the current on-device scenario tests.

## 2026-09-18 — Audit hardening pass (P0 data safety, correctness, polish)

### P0: encrypted-database key handling is now non-destructive (`DatabasePassphraseProvider.kt`)
- **Before:** if the Android Keystore key/wrapped passphrase was missing or unreadable while a database file already existed, startup could generate a *new* passphrase. That made the existing encrypted database permanently unreadable — silent data loss.
- **After:** a pure decision table (`resolveAction`) encodes the contract: healthy install → **REUSE**; fresh install (no wrapped key, no DB file) → **CREATE_FRESH**; anything ambiguous (unreadable wrapped key, or existing DB with no wrapped key) → **FAIL_KEY_UNAVAILABLE**. The passphrase is never rotated and the database is never deleted. Failures throw `DatabaseKeyUnavailableException`, surfaced by a dedicated, non-destructive recovery screen.
- `AppDatabase` now opens `DatabasePassphraseProvider.DATABASE_NAME` so the provider and Room agree on one file name.

### Startup resilience
- `LifeOSApplication` now exposes a nullable `serviceLocator` plus `initializationError` and `retryInitialization()`; `ServiceLocator.get` is wrapped in `runCatching`.
- `MainActivity` gates on this: a locked/failed DB key shows `DataKeyErrorScreen` (Retry / Exit) instead of crashing or wiping data.

### App Lock (4-digit PIN + auto-lock)
- New `PinComponents` (`PinDots`, `PinKeypad`, `PinKeypadPanel`) with haptics and accessibility semantics.
- `AppLockSetupScreen` / `AppLockScreen` rewritten for an exactly-4-digit keypad with auto-advance and auto-submit; lockout countdown and Forgot-PIN recovery preserved.
- Auto-lock: `AUTO_LOCK_GRACE_MILLIS = 30s`; locking on `ON_STOP` and re-prompting after the grace period, with an "Auto-lock" toggle in Settings.

### Profile
- New `ProfileAvatar` (Coil `AsyncImage` with Person fallback). `ProfileScreen` supports a persisted profile photo (`OpenDocument` + `takePersistableUriPermission`), editable display name, App Lock/Backup/Settings rows, and an About dialog. Home header uses the avatar.

### Search
- `SearchScreen` rewritten with `SearchCategory`, `SearchHit`, debounced (300ms) querying, tappable results, and category-aware navigation. Fixes a compile error by using `NoteEntity.plainTextForSearch`.

### Dates & Timeline
- `DateTimeUtils.startOfLocalDayMillis` / `endOfLocalDayMillis` / `nowMinutesOfDay` added. `BuildTimelineUseCase` now uses **local** day boundaries (previously `epochDay * 86_400_000L`, which shifted users off UTC into the wrong calendar day). Timeline caps navigation at today and handles system Back.

### Habits
- Streaks are now schedule-aware (`HabitSchedule`, `isScheduled`, `parseCustomDays`): a Mon/Wed/Fri habit is no longer penalised for missing Tuesday, and an in-progress today does not break a run. `HabitRepository.computeAnalytics`/`computeHeatmap` use the schedule (off-days show as no-data). Legacy consecutive-day overloads retained.

### Tasks
- **Bug:** enum priorities are persisted as strings, so `ORDER BY priority` sorted alphabetically (HIGH < LOW < MEDIUM). Added an explicit `PRIORITY_ORDER` CASE expression for `observeForDay`/`observeAll`, and `observeOverdue(today, nowMinutes)` now includes due-today-past-time items.

### Notes
- `NotesListViewModel` with filters (All/Favorites/Archived/Trash) and folder chips; per-row actions (pin, favorite, archive, trash/restore, delete-forever).
- `NoteEditorViewModel.applyAiResult()` makes AI suggestions **explicit**: Generate title / convert to checklist / append prose, applied only when the user taps Apply (Copy and Close also available). This removes the earlier auto-apply behavior.

### Media capture
- `AudioCaptureScreen` (15-min cap), `VideoCaptureScreen` (5-min cap), and `CameraCaptureScreen` now handle system Back, bound/unbind the camera provider safely, cap duration, clean up partial files, and expose a discard action.

### Diary
- Removed the dedicated AI drafting path entirely (Section 23): entries are user-written and saved only on explicit Save; added delete confirmation and Back handling.

### Onboarding
- Replaced hardcoded colors with `MaterialTheme.colorScheme` tokens so onboarding follows light/dark theme.

### Reminders
- Global "Task & habit reminders" toggle in Settings is now persisted and enforced: turning it off cancels every scheduled WorkManager job (`ReminderScheduler.cancelAllReminders`, tagged work), turning it on re-registers all future task/habit reminders. The scheduler also self-defers when the flag is off, so a stray call can't re-arm notifications.

### Build hygiene & tests
- `app/schemas/` is no longer gitignored so the exported Room schema (`1.json`) is version-controlled.
- New/expanded unit tests: `DatabasePassphraseProviderTest` (data-safety decision table), schedule-aware `HabitStatsCalculatorTest`, and local-day-boundary `DateTimeUtilsTest`. **81 tests pass.**

### Verification
```text
gradle :app:compileDebugKotlin
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
```

## What changed

### Navigation (root cause fix)
- **Before:** a global `BackHandler` in `LifeOSNavHost` intercepted every back press and popped to whatever the single NavController happened to be on. Combined with a flat route list, going Home → Notes and back could leave stale entries on the stack. Home → Notes → Home did not work via the UI.
- **After:** the global `BackHandler` is gone. The four primary destinations (Home, Tasks, Habits, Insights) live in one nested graph (`root_tabs`) with `saveState`/`restoreState`/`launchSingleTop` on bottom-bar taps. Child screens (Notes, Note Editor, Habit Detail, Expenses, Diary, Timeline, Capture Detail, Search, AI Assistant, Profile, Settings, App Lock Setup) are registered outside the graph, so they stack on top of the active tab and system Back dismisses them one level at a time.

### Bottom navigation (`LifeOSBottomBar.kt`)
- Order is now **Home, Tasks, Habits, Insights** (was Home, Habits, Tasks, Insights).
- Labels are **always visible** (previously the label only appeared for the selected item).
- Selected state: animated lavender pill (`secondaryContainer`) with primary icon+text; unselected: neutral `onSurfaceVariant` icon+label.
- 48dp minimum touch target, press-scale animation, tab semantics (`Role.Tab` + `selected`).
- Removed the dead legacy `LifeOSBottomBar(selected, onSelect)` overload from `LifeOSDesignComponents.kt`.

### Home screen (`HomeScreen.kt`)
- **Root-cause fix for the large empty gap:** the decorative gradient was a `Box(Modifier.size(210.dp))` that forced the greeting block to 210dp+; it now uses `matchParentSize()` behind real content.
- Rebuilt header (LifeOS • Today + date, search/settings/profile actions), greeting, Daily Momentum with animated progress, quick-action row, Today's Priorities, habits streak, Recent Activity, LifeOS Intelligence, and a Capture entry point.

### Insights screen (`InsightsScreen.kt`)
- Cognitive Vitality circular indicator is properly sized (was 104dp before, text overlapped the dial), with typography hierarchy and an honest empty state.
- Insight stat cards use a Column (icon/value/label) so values never overlap.
- Pattern Intelligence is clearly presented as the **local** engine; Weekly Narrative now renders its extracted score in a dedicated row ("N / 100").
- Added `internal fun narrativeScore(narrative): Int` that parses both `score N/100` and `N/100` formats (falls back to 0).

### Build hygiene
- `assembleDebug` passes; `testDebugUnitTest` passes (64 tests); `lintDebug` reports **0 errors**.
- Fixed pre-existing lint errors outside the redesign scope (no behavior change):
  - `NotificationHelper`: wrap `notify()` in `SecurityException` handling (Android 13+ permission race) and drop an obsolete `SDK_INT >= O` branch (minSdk is 26).
  - `AndroidManifest.xml`: declared `android.hardware.camera` as `required="false"` (camera works on camera-less/ChromeOS devices).
  - `VideoCaptureScreen`, `MediaPreviewUtils`, `HabitsScreen`, `TimelineScreen`: suppressed the `ProduceStateDoesNotAssignValue` false positives (the lambdas do assign `value`).
- New tests: `BottomNavItemsTest` (order/labels/route uniqueness) and `NarrativeScoreTest` (both score formats + fallbacks).

## Not changed
- All repositories, use cases, ViewModels, DAOs, entities and the Room schema.
- No internet permission, no cloud/AI/telemetry calls; everything remains offline-first.
- README.md and docs/ are preserved (README build-status and snapshot sections were updated).

## Verification
```text
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

## 2026-09-18 — Stitch UI integration + interaction fixes
### Changed
- Integrated the supplied Stitch visual direction into the existing Kotlin/Jetpack Compose implementation for Profile, Expenses, Add Expense, and App Lock.
- Added local profile-photo selection and persistence through `SettingsStore`.
- Removed the Home-screen Settings action; Settings is now intentionally exposed from Profile.
- Kept system Back handling per-screen so secondary destinations consistently return to the previous destination (no global interceptor).
- Changed onboarding startup gating to wait for the persisted completion value before rendering, preventing the onboarding pages from flashing after the first launch.
- Hardened audio capture cleanup, recorder error handling, and local MediaPlayer preparation/playback.

### Preserved
- Existing Room database, repositories, use cases, navigation architecture, offline-first behavior, and app-lock hashing/recovery.
- No Room schema change was required for profile photo storage because the URI is a local DataStore preference.
- No cloud upload, telemetry, remote AI, or new network dependency was introduced.

### Verification
- Source-level audit completed against the supplied latest repository and Stitch HTML/screens.
- Full Gradle verification could not be executed because the supplied repository archive does not contain `gradlew`/`gradle-wrapper.jar`, and no system Gradle executable is available in the execution environment.

### Remaining
- Run the project's normal Android CI/build locally or in GitHub Actions once the wrapper is present to perform the final compiler, unit-test, instrumentation, and lint verification.

---

## 2026-09-19 — Expenses micro UX fix + documentation audit

### Change summary
Two small, additive UX improvements to the existing Expenses screen. No
redesign, no architecture, navigation, schema or business-logic changes.

### Affected files
- `app/src/main/java/com/lifeos/app/ui/expenses/ExpensesScreen.kt`
- `app/src/main/java/com/lifeos/app/ui/components/GlassCard.kt`
- Documentation: `README.md`, `UPDATE.md`, `docs/04_FEATURES.md`,
  `docs/05_DATABASE.md`, `docs/09_FRONTEND.md`, `docs/14_TESTING.md`,
  `docs/16_KNOWN_ISSUES.md`, `docs/17_CHANGELOG.md`, `docs/21_FILE_STRUCTURE.md`,
  `docs/DOCUMENTATION_AUDIT.md`, `docs/00_PROJECT_OVERVIEW.md`,
  `FINAL_RELEASE_CHECKLIST.md`

### Root cause / problem
1. **Sheet opened too low.** `AddExpenseSheet` used a default
   `ModalBottomSheet`, which opens at the *partially expanded* state; users had
   to drag the sheet upward to see the whole Add Expense form.
2. **Unclear category selection.** Category `GlassChip`s had no selected
   visual; selection was only indicated by the "Selected: <name>" text line.

### Implementation
1. **CHANGE #1 — expanded-by-default sheet.** In `AddExpenseSheet`, the
   existing `ModalBottomSheet` now receives
   `sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)`
   (existing Material 3 API) so it opens fully expanded. The form `Column` gained
   `verticalScroll(rememberScrollState())` so every field/button stays reachable
   when available height shrinks (IME, landscape, font scaling). Shape, drag
   handle, background, fields, category UI, buttons, swipe-to-dismiss and Back
   behavior are unchanged. No dialog, new screen, `Box` or hardcoded offsets
   were introduced.
2. **CHANGE #2 — selected category visual state.** The reusable `GlassChip`
   (`ui/components/GlassCard.kt`) gained an optional `selected: Boolean = false`
   parameter. When selected it uses `MaterialTheme.colorScheme.primary` as the
   container/border with `MaterialTheme.colorScheme.onPrimary` content color;
   otherwise it keeps its original glass surface/border. The Expenses call site
   passes `selected = selectedCategory == cat.name`, reusing the **existing**
   `selectedCategory` state (no second state introduced). Only one chip can be
   selected; selecting another moves the highlight immediately. The existing
   "Selected: <name>" line is retained.

### Preserved
- The entire Expenses screen layout, monthly card, daily average, budget/left
  math, recent transactions, empty state and FAB are untouched.
- `ExpenseRepository`, `ExpenseDao`, `ExpenseEntity`, `ExpenseCategories`,
  navigation, and all other screens are untouched.
- No Room schema/version/migration change.
- No network/cloud/AI/telemetry dependency; data stays on device.

### Verification
Executed in the audit environment with Gradle 8.9 + AGP 8.6.1 (offline; aapt2
override), not just source review:

```text
gradle :app:compileDebugKotlin   -> BUILD SUCCESSFUL
gradle :app:assembleDebug        -> BUILD SUCCESSFUL
gradle :app:testDebugUnitTest    -> BUILD SUCCESSFUL (81 tests, 0 failures)
gradle :app:lintDebug            -> BUILD SUCCESSFUL (0 errors, 4 pre-existing warnings)
```

The repository still does not ship `gradlew`/wrapper JAR, so a locally-installed
Gradle 8.9 distribution was used. Instrumentation tests were not run (no
emulator/device and no `androidTest` source set).

### Remaining
- Device/emulator UI verification of the two Expenses interactions (sheet
  initial position and category highlight) is still recommended — it was
  verified at compile/test/lint level only.
- `docs/16_KNOWN_ISSUES.md` Issue #1 (missing Gradle wrapper scripts) remains
  open.

---

## 2026-09-20 — Home screen redesign (`feat/expenses-micro-ux`)

Home rebuilt in Compose to match the Google Stitch dashboard design while
keeping the LifeOS purple theme and real Room data throughout.

### Home layout (top to bottom)
- **Header**: LifeOS wordmark + logo dot, Search, ProfileAvatar (unchanged flow).
- **Greeting band**: date line (`SATURDAY, 19 SEPTEMBER 2026`), live greeting
  (`DateTimeUtils.greeting()`), and a day-status label derived from completed
  goals ("Day on track" / "Building momentum" / "Fresh start" / "Day starting").
- **Circadian Velocity card**: `$dayProgress%` (minutes-elapsed / 1440, refreshed
  every 60 s), circular bolt progress ring, 4-segment bar ((pct/25), 0..4), and a
  TASKS / HABITS / SPEND stats pill — all real values from today's DAO flows.
- **Focus Now**: highest-priority incomplete task of today (first from
  `observeForDay`, which already sorts by `PRIORITY_ORDER`); when every task is
  done it shows the most recently completed one with a DONE state and a
  one-tap undo. Empty state when no tasks planned.
- **Quick Actions**: Diary / Expense / Timeline tiles routed to existing screens.
- **Habits**: "n/N Active" head + WEEKLY CONSISTENCY row (scheduled-day aware:
  a day counts only when every active habit scheduled on it passed its goal),
  per-habit rows with icon, `{n}d streak` badge, and one-tap check-in/undo that
  persists via `logProgress`/`clearProgress`.
- **Today's Activity**: real timeline (`BuildTimelineUseCase`, today only) with
  typed dot + time pill; empty state otherwise.
- FAB (camera) → Capture coverage, unchanged.

### Data-layer additions
- `DateTimeUtils.dayProgressPercent()`.
- `HabitCompletionDao.observeAllInRange(start, end)` +
  `HabitRepository.observeAllInRange` (weekly consistency window).
- `GetHomeSummaryUseCase` now also consumes `BuildTimelineUseCase` (4th
  dependency, wired in `ServiceLocator`) and exposes `focusTask`,
  `focusTaskIsDone`, `weeklyConsistency: List<DayCheck>`, `weeklyDoneDays`,
  `recentActivity`, plus per-habit `currentStreak/longestStreak/completionPercent`.
- `HomeViewModel`: `toggleTask` kept, `toggleFocusTask` added, `incrementHabit`
  replaced by `toggleHabit(habitId, isDone, goalCount)` (check → log to goal,
  uncheck → clear today's record).

### Verification
```text
gradle :app:compileDebugKotlin   -> BUILD SUCCESSFUL
gradle :app:assembleDebug        -> BUILD SUCCESSFUL
gradle :app:testDebugUnitTest    -> BUILD SUCCESSFUL (81 tests, 0 failures)
gradle :app:lintDebug            -> BUILD SUCCESSFUL (0 errors; only pre-existing
                                     dependency-version warnings)
```

No Room schema change (DB version stays 1), no new navigation stacks, no novel
dependencies, no cloud/AI/analytics; the Screen callback contract of
`HomeScreen` (and therefore `LifeOSNavHost`) is unchanged.

### Remaining
- Device/emulator visual check of the Stitch-to-LifeOS mapping (e.g. Fast Yet
  Fresh / "14 day streak" sample labels in the reference were intentionally not
  reproduced).

---

## 2026-09-20 — Home dashboard, profile photo, capture & navigation hardening

### DAILY UPDATE card (real data, no gestures)
- The speculative 4-segment "CIRCADIAN VELOCITY" card is gone. The reference
  spec shows a single **"TODAY'S PROGRESS" title + one percentage + one bar**,
  so the Home header card is now **DAILY UPDATE**: current percentage
  (`headlineLarge`), one `LinearProgressIndicator`, and the TASKS / HABITS /
  SPEND stat surface (real per-module counts from Room).
- The percentage is real and deterministic: completed tasks + completed habits
  over every task/habit scheduled today (`computeDailyUpdatePercent`), with
  `0` when nothing is planned so a fresh day never claims clock-driven
  progress. It no longer uses the device wall-clock / gesture math
  (`rememberDayProgress` + `DateTimeUtils.dayProgressPercent` removed from
  Home).
- `HomeSummary` swaps `focusTask`/`focusTaskIsDone` for `dailyUpdatePercent`;
  `toggleFocusTask()` removed (the task's real checkbox toggle in TODAY'S TASKS
  replaces it).
- **Fix 11** (focus-empty-state overlap): the Focus-Now card rendered even when
  empty, producing the overlapping text ghost. TODAY'S TASKS omits its section
  entirely when no tasks are scheduled, so there is nothing to overlap; the
  FAB/labels were already clear of the extended FAB. LifeOSSpacing reviewed.

### TODAY'S TASKS (replaces FOCUS NOW)
- Real, live list of today's tasks (checkbox toggles the Room state directly,
  struck-through when done, priority chip + time subtitles). Empty list → no
  badge, no ghost card. Follows the spec's TODAY'S TASKS block.

### Prominent Habits heading
- NEW-section heading now `titleMedium` SemiBold with an "n/N Active" count and
  an "Open all" affordance, matching the spec's TODAY'S HABITS block.

### Profile photo — persistent, cropped, resilient
- **Root cause of the disappearing photo:** the previous flow stored the picked
  image as a *content:// URI* backed by a temporary read grant. Grants expire
  at day change / process kill, and `takePersistableUriPermission` is rejected
  by many providers — so the avatar silently failed.
- **Fix:** picking now runs the photo through an **EXIF-correct downscale**
  (`androidx.exifinterface`), a new **square crop dialog** (Compose-only:
  Canvas preview, scrimmed bands, drag-to-position, corner-handle resize,
  `detectDragGestures`), then saves a JPEG to app-private storage
  `filesDir/profile-photos/profile_<timestamp>.jpg`. `SettingsStore` stores
  that absolute path — it survives day changes, restarts and process death. Old
  `content://` values still render when Coil can load them; on any load failure
  `ProfileAvatar` falls back to the letter avatar (first letter of display
  name) or a Person icon, never a broken image.
- `androidx.exifinterface:1.3.7` added (already on the runtime classpath via
  Coil — no APK growth); this also clears the corresponding lint warning.

### Fix 12 — Voice Capture crash
- `MediaStorage.newAudioFile()` and the `MediaRecorder` constructor ran on the
  UI thread **outside** the try/catch, so a failure (e.g. no space, corrupt
  state) crashed the app instead of showing an error. Both now live inside the
  try; the mic button re-requests permission if it is missing, `start()`/
  `stop()` are re-entrancy-guarded, and every failure path cleans up file +
  recorder and shows the existing error surface. Too-short recording discard
  preserved.

### Fix 13 — Bottom navigation reliability
- Re-tapping the already-visible tab previously *navigated again*, so repeated
  taps could leave duplicate stacks that made the tab appear "dead" (the Home
  case). Re-tap of the visible tab now pops back to the start destination
  instead of duplicating; other tabs keep the canonical
  `popUpTo<saveState> + launchSingleTop + restoreState` pattern. Navigation
  stays on `navigation-compose:2.8.4`.

### Verification
```text
gradle :app:compileDebugKotlin   -> BUILD SUCCESSFUL
gradle :app:assembleDebug        -> BUILD SUCCESSFUL (app-debug.apk built)
gradle test                      -> BUILD SUCCESSFUL (88 unit tests, 0 failures:
                                     81 prior + 7 new DailyUpdatePercentTest)
gradle :app:lintDebug            -> BUILD SUCCESSFUL (0 errors; ExifInterface
                                     warning cleared; only pre-existing Info
                                     autoboxing/Icon/unused-resource items)
```

No Room schema change (DB version stays 1), no nav-dependency change, no
cloud/AI/analytics, no gesture-based progress. `HomeScreen`'s callback contract
(and `LifeOSNavHost`) is unchanged: only the removed `toggleFocusTask` and the
`HomeSummary` field swap touched anything outside Home.

### Remaining
- On-device check of the crop dialog, the avatar persistence after a forced
  stop / device restart, the audio-capture error path, and the re-tap Home
  behavior (no emulator in this environment — verified via compile + JVM tests
  + code reasoning).

---

## 2026-09-20 — Home startup, Home empty states, bottom-nav labels & predictive back

### Fix 14 — Home startup freeze (4–5 s of blank/overlapping Home on cold launch)

Root cause was two-fold, both now fixed without touching architecture, schema
or Room:

1. **SQLCipher cold-open cost landed on the first render.** The encrypted
   database was opened lazily on Room's query executor at the *first* Home
   query — after the UI was already showing. SQLCipher's one-time key
   derivation (PBKDF2) and file open ran right when Home began collecting the
   summary flows, stalling data for several seconds (and making the system
   feel unresponsive to taps/Back during that window).
   - **Fix:** `AppDatabase.warmUpOpen(context, scope)` forces the database open
     on a background dispatcher from `LifeOSApplication.onCreate`, so the
     one-time key derivation overlaps UI setup instead of Home's first query.
2. **`GetHomeSummaryUseCase.assemble()` did full-history analytics per habit.**
   `habitRepo.computeAnalytics(habit, today)` issued one full-history Room query
   per active habit, and `BuildTimelineUseCase` pulled its 7 source streams one
   at a time — all serial, on the collector (main) thread.
   - **Fix:** new `HabitRepository.computeAnalyticsBatch(habits, today)` loads
     the completions table **once** and computes every habit's analytics
     in-memory (same `analyticsFor` math reused by `computeAnalytics`, so detail
     and Home stay consistent). `BuildTimelineUseCase` now fires its source
     queries concurrently (`coroutineScope { async { … } }`). The summary flow
     is `flowOn(Dispatchers.Default)`, so assembly never runs on the main thread.

### Fix 15 — Home empty-state cards overlapped their text (`LifeOSCard` Box root cause)
- `LifeOSCard` renders card children inside a `Box` (press-scale surface); two
  sibling `Text`s passed straight into it stack at the same top-start corner.
  The "No routines yet" (Habits) and "No activity recorded today" (Today's
  Activity) empty states did exactly that, so the title and body overlapped.
  (This was the most visible state right after launch, compounding Fix 14.)
- **Fix:** both Home empty states now wrap their texts in a
  `Column(verticalArrangement = Arrangement.spacedBy(10.dp))` — the same
  pattern `HabitsScreen` already used. The `LifeOSCard` container itself is
  unchanged (other callers rely on `Row`/single-child content).

### Fix 16 — Bottom-nav labels truncated on narrow screens
- The bottom bar rendered icon and label side by side inside a `weight(1f)`
  cell; on ~360dp displays "Insights" clipped to "Insi", "Home" to "Ho", etc.
- **Fix:** each `BottomNavEntry` is now a stacked column (icon above label,
  Material 3 `NavigationBar` direction), centered pill, 22dp icon, ellipsizing
  `labelMedium` text. Labels remain always visible and fully readable on
  standard 360–420dp phones.

### Fix 17 — System Back: verified + predictive-back enabled
- Code audit confirmed every screen's Back path is correct and none swallows
  presses (no global interceptor; nested `root_tabs` graph; per-screen
  `BackHandler`s call their dismiss/save). The perceived unresponsiveness
  traced to the Fix-14 main-thread stall during startup.
- `AndroidManifest.xml` now declares
  `android:enableOnBackInvokedCallback="true"` so Compose Navigation's
  `OnBackInvokedDispatcher` path is active (it was required for the predictive
  back contract at targetSdk 35).

### Audio / capture
- Full pipeline re-audited (`AudioCaptureScreen`, `CaptureSheet` confirmation,
  `CameraCaptureScreen`, `VideoCaptureScreen`, `CaptureMediaPreview`,
  `MediaStorage`, `PermissionManager`, Timeline audio rows, AI voice input).
  No concrete defect found in the current code; correct-by-construction paths
  were left untouched rather than fabricated (Fix 12 on-device check remains
  outstanding).

### Verification
```text
gradle :app:compileDebugKotlin   -> BUILD SUCCESSFUL
gradle :app:testDebugUnitTest    -> BUILD SUCCESSFUL (88 tests, 0 failures)
gradle :app:lintDebug            -> BUILD SUCCESSFUL (0 errors; only pre-existing warnings)
```

No Room schema change (DB version stays 1), no navigation architecture change,
no new dependencies, no cloud/AI/analytics. `HomeScreen`'s callback contract
and `LifeOSNavHost` are unchanged.

### Remaining
- On-device confirmation of the startup freeze elimination (timing), predictive
  back gesture animation, and the bottom-bar label rendering at 320–360dp (no
  emulator/device in this environment — verified via compile + 88 JVM tests +
  lint + code reasoning).

---

## 2026-09-21 — Capture sheet redesign (LifeOS Moment Capture, from the Stitch design project)

Implemented from the single Stitch design screen into `CaptureSheet.kt`'s
capture menu. **Visual-layer only**: navigation, data, persistence, permissions
and the photo/video/audio sub-screens are unchanged.

- **Header:** removed the top-right Close `IconButton` (dismiss now flows
  through system Back, tap-outside, and the "Close → Return to LifeOS" tile).
  Title `headlineMedium` → `displayLarge` (40/48, per measured 82px cap ≈
  27.3dp at 3x); subtitle `bodySmall` → `bodyMedium`, still `onSurfaceVariant`
  (measured ~`#494455`).
- **Quick-thought card:** surface tint `#F7F2F9` (`LifeOSCaptureCardLight`),
  20dp radius, no elevation; "Quick thought" label `titleMedium` →
  `headlineSmall`; input switched from `OutlinedTextField` (visible border) to a
  borderless M3 `TextField` — transparent container, single bottom indicator in
  `outline`, placeholder in `outline`, `bodyMedium`, `minLines = 3` — matching
  the design's borderless entry; "Save thought" `Button` unchanged.
- **Life Capture grid:** section label `titleMedium` → `titleLarge`; tiles use
  the design's lavender `#F1ECFF` (`LifeOSCaptureTileLight`) instead of white
  `surface`, no elevation; labels `titleMedium` → `titleLarge`; subtitles
  `labelSmall` → `bodyMedium`; icon container stays `primaryContainer` 44dp /
  15dp radius with 24dp branded icon.
- **Spacing:** outer side padding 22 → 30dp; card→header gap 28dp (top padding
  12 + 16 spacedBy); tile gap 12dp; title→subtitle 2dp — all from measured
  pixel geometry.
- Content now scrolls (`verticalScroll`) with `imePadding` so the sheet fits
  ~360×640 screens and the keyboard never covers the field.
- **Dark mode:** paired tone tokens added to `Color.kt`
  (`LifeOSCaptureCardDark` `#37323D`, `LifeOSCaptureTileDark` `#2C2844`).
- The punch-hole-era top-right decorative cluster (paper-plane doodle, floating
  "audio" label, ✕ shapes) was judged a Stitch ambient decoration and was
  deliberately **not** reproduced.

### Verification
```text
gradle :app:compileDebugKotlin   -> BUILD SUCCESSFUL
gradle :app:testDebugUnitTest    -> BUILD SUCCESSFUL (176 tests, 0 failures, 0 errors)
gradle :app:assembleDebug        -> BUILD SUCCESSFUL
gradle :app:lintDebug            -> BUILD SUCCESSFUL (0 errors; only pre-existing warnings)
```

No Room schema change (DB version stays 1), no navigation architecture change,
no new dependencies, no cloud/AI/analytics. `LifeOSNavHost` and the sheet's
callback contract are unchanged.

### Remaining
- On-device visual confirmation (no emulator/device in this environment —
  verified via compile + 176 JVM tests + lint + pixel-measurement spec match +
  code reasoning).

---

## 2026-09-21 — Diary (LifeOS Journal) redesign, from the Stitch design project

Implemented natively (Kotlin + Compose) from the Stitch "LifeOS Diary /
Journal" screens into `ui/diary/`. The plan (`tasks/plan.md`) and task list
(`tasks/todo.md`) live under `tasks/`. **Visual and informational layer only**:
Room schema, repositories, analyzers and navigation architecture are
unchanged.

- **Palette & tokens:** paper `#FDF8FF`, card `#FFFCFF`, ink violet `#21005D`,
  lavender `#EADDFF` (with dark-mode pairs) and a per-mood editorial palette —
  Goldenrod (happy), Sage (calm), Indigo (sad), Terracotta (stressed),
  dried-rose (excited) — added to `Color.kt` (`DiaryPaper`, `DiaryPaperCard`,
  `DiaryInkViolet`, `DiaryLavender`, `DiaryMood*`).
- **List screen (`DiaryScreen.kt` rewrite):** editorial header (title
  `headlineSmall`, subtitle `bodySmall`), 60dp round lavender FAB with ink
  plus, day strip (last 14 days, newest-first, All option), mood entry cards
  (24dp radius, date + mood pill, inline Edit/Delete, keyword chips), friendly
  empty state, all analytics below the entries.
- **Composer (`DiaryEditorSheet.kt`):** Dialog + Surface bottom sheet — grab
  handle, mood chip row (5 moods, tapped chip tinted), borderless
  `TextField` ("What happened today?"), Cancel / Save (enabled once non-blank),
  and Delete + date header when editing an existing entry.
- **Details (`DiaryDetailScreen.kt`):** entry-detail route `diary/{entryId}`
  (new `Screen.DiaryDetail` + `LifeOSNavHost` wiring) with editorial
  typography, mood pill, theme keyword chips, the day's connection radar and
  full editing/deleting from the sheet.
- **Local intelligence:** new `LifeOSIntelligenceEngine.diaryInsights(today)`
  (week/month counts, streak, average mood + trend via `TrendAnalyzer`,
  themes via `KeywordExtractor`, patterns via `PatternDetector`, narrative via
  `ReportGenerator.weekly`, recommendations via `CorrelationAnalyzer`) returned
  as a new `DiaryInsights` model with a ready-to-use all-in-one summary string.
  The engine is now exposed by `ServiceLocator` (`val intelligenceEngine`).
- **Connection radar (`DiaryConnections.kt` + `DiaryConnectionsView.kt`):** a
  pure builder turns the selected day's diaries + timeline (via
  `BuildTimelineUseCase`) into a node/edge graph — entry (center), mood and
  keywords (left rail), timeline items (right rail), capped for legibility —
  rendered on `Canvas` with `RadarNodeChip` overlays (DP-based layout). No
  entry on that day shows a friendly note instead of a graph.
- **Analytics section (`DiaryAnalyticsSection.kt`):** expandable cards —
  Summary (week/month/streak), mood bar chart (−2..+2 scale, last 14 days),
  top themes, repeating patterns, weekly narrative, recommendations, and an
  **Ask LifeOS** card that runs the existing offline `LocalQuestionEngine`
  against last week's diary (with follow-up suggestions chips).
- **Tests:** `DiaryConnectionsTest` (8) covering graph shape, mood
  preservation/detection, shared-keyword edges, same-day timeline wiring and
  capping; `DiaryMoodsTest` (5) covering stored-mood equivalence,
  fallback labels and analyzer mapping.

### Verification
```text
gradle :app:compileDebugKotlin   -> BUILD SUCCESSFUL
gradle :app:testDebugUnitTest    -> BUILD SUCCESSFUL (101 tests, 0 failures, 0 errors)
gradle :app:assembleDebug        -> BUILD SUCCESSFUL
gradle :app:lintDebug            -> BUILD SUCCESSFUL (0 errors; only pre-existing warnings)
```

No Room schema change (DB version stays 1), no new dependencies, no
cloud/AI/analytics. Mood values are the existing persisted strings; stored
mood wins, analyzer detection is the fallback (matching previous behaviour).

### Remaining
- On-device visual confirmation against the Stitch screens (no
  emulator/device in this environment — verified via compile + 101 JVM tests +
  lint + code reasoning); visual QA can't render screenshots here.

---

## 2026-09-23 — Remove Notes, Capture, LIFE AI, Intelligence, Insights & Search (branch `feat/remove-notes-capture-life-intelligence`)

### Scope removed
- **Notes & Capture:** Notes list/editor, Photo/Video/Audio capture (CameraX), Capture detail, and their sheets (FAB from Home) are gone. `core/ai`, `core/intelligence`, `core/life` (incl. `LifeModels`, `LifeVoiceInputController`) and the `ui/ai`, `ui/capture`, `ui/notes`, `ui/search`, `ui/insights` trees were deleted.
- **Insights / Statistics / Connections / Search:** the Insights bottom-nav tab, Diary analytics & connection-radar sections, the Search screen, and the "Weekly Rhythm" home/insights card were removed.
- **LIFE AI Assistant & settings:** the AI assistant screen, offline intelligence engine wiring, AI features toggle and "Privacy & Local Intelligence" settings section were removed.
- **Permissions/resources:** CAMERA / RECORD_AUDIO permissions, camera `uses-feature`, the captures FileProvider path, and the CameraX Gradle dependency were removed. Tagline now reads "Organize your life. Understand your day."

### Room migration v2 → v3 (non-destructive)
- `AppDatabase` is now version **3** with `MIGRATION_2_3`, registered alongside `MIGRATION_1_2`. No `fallbackToDestructiveMigration`.
- The migration drops **only** the removed features' tables — `notes` and `captures` — and touches nothing else. All retained tables (`tasks`, `habits`, `habit_completions`, `expenses`, `diary_entries`) keep every column and row.
- Exported schema `app/schemas/.../3.json` now contains exactly the five retained tables (KSP `exportSchema=true`).
- **Tests:** a JVM `AppDatabaseSchemaTest` asserts the exported 3.json keeps the five retained tables and drops `notes`/`captures` (runs on every unit-test run); an instrumentation `AppDatabaseMigrationTest` (in `app/src/androidTest`) seeds a v2 database with rows in both dropped tables, runs `MIGRATION_2_3` via `MigrationTestHelper` with `validateDroppedTables=true`, and asserts the tables are gone while retained tables survive. The instrumentation test uses the `FrameworkSQLiteOpenHelperFactory` (plain SQLite) so the helper can validate the migration; it compiles via `:app:assembleDebugAndroidTest` but must run on a device/emulator.

### Data preservation
- **Timeline data:** `TaskDao.getCompletedBetween` was re-introduced as a retained-feature query so the Timeline screen still lists completed tasks; `BuildTimelineUseCase` reads from it unchanged.
- **Backups:** `LifeOSBackup` no longer carries `notes`/`captures` arrays, but `CURRENT_FORMAT_VERSION` stays **1** and `ignoreUnknownKeys=true` remains, so older v1 backup files that include those keys still import (legacy keys are simply dropped). `BackupSerializationTest` gained a legacy-decode case.
- Repeated/off-device verification: `assembleDebug`, `testDebugUnitTest`, `lintDebug`, `assembleDebugAndroidTest` all build green (0 lint errors).

### Known issues / notes
- Deleted-feature data (`notes`, `captures`) is dropped on upgrade by design; no export path exists since the features are removed.
- The instrumentation migration test cannot be executed in this environment (no device/emulator); it is compiled and documented, and must be run via `:app:connectedDebugAndroidTest`.

---

## 2026-09-23 — Reliable reminders, startup unlock gate & back-navigation hardening (branch `feat/remove-notes-capture-life-intelligence`)

Three production-fix problems shipped together after the Notes/Capture/LIFE AI removal batch. No architecture rewrite: Compose Navigation + Room stays, everything stays offline-first.

### 1. Reliable task/habit reminders — WorkManager → AlarmManager exact alarms (Room v4)
The old reminder path ran WorkManager jobs, which Doze / app-standby bucketing can defer for minutes (or drop) while the app is backgrounded, so a "now + 2h" reminder could arrive late or never.

- **`reminders` table (Room v4, pure additive).** `ReminderEntity` (`id` = `"task:<id>"`/`"habit:<id>"`, entity type/id, title, `nextTriggerAtEpochMillis`, `repeatType` ONCE/DAILY/WEEKDAYS, `repeatDaysCsv`, `enabled`, sound/vibration flags, `snoozeMinutes`, `snoozeReturnAtEpochMillis`, `updatedAt`) + `ReminderDao`. `MIGRATION_3_4` creates the table and four `index_reminders_*` indexes; no retained table is touched and `fallbackToDestructiveMigration` remains off. Schema `app/schemas/.../4.json` is exported (KSP).
- **`ReminderScheduler` (AlarmManager).** One exact alarm per enabled reminder via `setAlarmClock` (`AlarmManager.AlarmClockInfo`) — Doze-exempt and the strongest priority the platform offers — with a Doze-aware `setAndAllowWhileIdle(RTC_WAKEUP)` fallback when exact-alarm permission is missing on API 31+. Each PendingIntent is an explicit broadcast to `AlarmReceiver` (no intent filter; cannot be triggered by other apps) with a `lifeos://reminder/<id>` data URI + `FLAG_UPDATE_CURRENT|FLAG_IMMUTABLE`, so ids never collide.
- **`ReminderScheduleCalculator`** — pure JVM recurrence: ONCE → no next; DAILY → +24h; WEEKDAYS → next Mon–Fri at the same wall-clock time (custom day CSV honored, defaults `1,2,3,4,5`). Covered by 7 unit tests.
- **State machine (`ReminderRepository`)** owns every transition in one place: a real fire advances DAILY/WEEKDAYS and re-arms or disables ONCE; a snooze echoes the current notification after N minutes *without* moving the next real occurrence; completing/archiving/deleting an entity cancels+removes its row; a global toggle cancels everything or re-arms all still-future reminders; `rebuildFromMirrors` rebuilds the table from the retained `reminderEpochMillis` mirrors on `tasks`/`habits` after backup restore.
- **Delivery.** `AlarmReceiver` runs the fire handler on `goAsync()`+IO, posts a HIGH-importance, CATEGORY_ALARM notification (per-notification sound/vibration flags; channel is silent by default so the sustained in-app ring doesn't double-beep) with a full-screen intent to `AlarmFullScreenActivity` when permitted, and launches that activity when the app is foregrounded. `AlarmFullScreenActivity` (showWhenLocked/turnScreenOn on target-API 27+) plays a looping `TYPE_ALARM` sound, vibes a repeating waveform, and offers STOP + Snooze 5/10/15/30 (slept back to the repository). `BootReceiver` (exported=true) re-arms on BOOT_COMPLETED / MY_PACKAGE_REPLACED / TIME_SET / TIMEZONE_CHANGED; `LifeOSApplication` re-arms at startup as an extra safety net.
- **Wiring.** `TaskRepository`/`HabitRepository` delegate reminder reconciliation to `ReminderRepository` (their `appContext` params and old `rescheduleAllReminders`/`ReminderScheduler.*Reminder` APIs are gone). `BackupRepository.restore` rebuilds reminders after restore. Task add-dialog → "Repeat reminder" selector (Once/Daily/Mon–Fri); Habit add-dialog and Habit-detail Reminder card get the same selector + time picker. Settings reminders toggle now arms/cancels exact alarms and surfaces an "Allow exact alarms" button (API 31+, `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM`).
- **Removed:** `ReminderWorker.kt` and the `androidx.work:work-runtime-ktx` dependency (WorkManager was used nowhere else).

### 2. Startup: encrypted-DB open never blocks the first frame
Previously the SQLCipher cold open ran on Home's first query *after* the UI was shown. Now `LifeOSApplication.onCreate` builds the DI container cheaply (lazy DB), then opens the database on a background coroutine; `databaseState` gates the UI on a lightweight static `BrandSplash` ("Unlocking your data…") until `Ready`, with a non-destructive `DataKeyErrorScreen` + retry path on `Error`. `SettingsStore.warmUp()` pre-caches the DataStore file and notification channel creation is moved off the main thread.

### 3. Profile → Settings system-Back gap closed
`SettingsScreen` was pushed without a Back affordance and the stack could lose its parent. It now renders a back arrow + `onBack` (popping back to Profile), and every secondary navigation in `LifeOSNavHost` uses `launchSingleTop` so repeated taps / back-chain navigation never duplicate destinations.

### Tests
- `ReminderScheduleCalculatorTest` (7: ONCE/DAILY/WEEKDAYS/custom-days/invalid-CSV-defaults/weekend-only).
- `AppDatabaseSchemaTest`: now asserts the exported 4.json — exactly the six retained tables (tasks, habits, habit_completions, expenses, diary_entries, **reminders**), notes/captures still gone, and the full reminders column set via the createSql.
- `AppDatabaseMigrationTest`: added a v3→v4 case (seeds v3, runs `MIGRATION_3_4` with validation, asserts `reminders` + its four indexes are SELECTable).
- `grep` verification (WorkManager, old scheduler APIs, COLUMN_NAME_* in NotificationHelper): no stale callers or hard-coded SQL remain.

### Verification
```text
gradle :app:clean :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
  -> BUILD SUCCESSFUL in ~4m23s
  -> 93 JVM unit tests, 0 failures         (app-debug.apk 41.9 MB)
  -> lintDebug: 0 errors, 22 warnings (pre-existing dependency/Info items only)
  -> assembleDebugAndroidTest: BUILD SUCCESSFUL (compiles; must run on a device)
```

### Remaining
- Rescue path verification is on-device only in this sandbox (`pm install/uninstall`, `am start`/`input`/`settings`/`dumpsys --` all throw `SecurityException: Permission Denial`; only `pm list`/`cmd package list` work), so the following could not be executed here: cold-start timing, the v3→v4 migration running against a real SQLCipher file, an actual alarm firing (Doze, reboot, permission-revoked fallback), full-screen-intent launch, and the Settings exact-alarm system screen. Verified here via compile + 93 JVM tests (incl. schema JSON) + lint + code reasoning; `:app:connectedDebugAndroidTest` covers the injectable parts on a device.

---

## 2026-09-23 — Startup splash, nav re-tap, permission-free reminders (branch `fix/startup-navigation-reminders`)

Follow-up hardening on the same feature branch base. No architecture, Room-schema, repository, navigation-structure or dependency-catalog changes beyond one additive AndroidX module.

### 1. "Unlocking your data…" replaced by the Android native splash
- **Root cause:** `MainActivity.BrandSplash()` was a custom full-screen Compose screen gated on `databaseState != Ready`; on first launch it could stay visible during the SQLCipher open and looked like a hand-rolled loading screen.
- **Fix:** `MainActivity` now calls `installSplashScreen()` before `super.onCreate` and keeps the system splash on screen (`setKeepOnScreenCondition`) only while `DatabaseInit.Initializing`; it dismisses on **Ready or Error**, straight into the real destination (Home / App Lock / Onboarding / DataKeyErrorScreen). `Theme.LifeOS.Splash` (parent `Theme.SplashScreen`, white background, launcher icon, `postSplashScreenTheme` → `Theme.LifeOS`) is applied to MainActivity; the application and `AlarmFullScreenActivity` keep `Theme.LifeOS`. The `BrandSplash` composable and its `Text("Unlocking your data…")` are deleted; a bare background `Surface` safety net (no label) backs the splash dismiss. New dependency: `androidx.core:core-splashscreen:1.2.0`.

### 2. First content no longer waits on reminder re-arming
- **Root cause:** `LifeOSApplication.launchDatabaseOpen` ran `reminderRepository.rebuildAllActive()` *before* setting `databaseState = Ready`, so the UI waited on re-scheduling every alarm.
- **Fix:** `Ready` now flips immediately after `AppDatabase.warmUpOpen`; `rebuildAllActive()` runs after in a non-blocking coroutine (`runCatching`). `BootReceiver` still covers reboot / package-update / time-change re-arming.

### 3. Bottom-bar re-tap no longer redirects to Home
- **Root cause:** the re-tap branch called `popBackStack(findStartDestination().id, false)`, which pops **to Home** no matter which tab was tapped (Habits→Habits landed on Home; same for Tasks), and cascaded into the intermittent Home-icon / Profile→Back→Tasks weirdness.
- **Fix:** every tap now uses one canonical pattern — `navigate(route) { popUpTo(startDest) { saveState = true }; launchSingleTop = true; restoreState = true }`. Re-tapping the visible tab stays on it; Profile → Back returns to the prior tab with the correct item selected. `LifeOSNavHost` structure (single NavHost, nested `root_tabs`) untouched.

### 4. Reminders drop the exact-alarm requirement
- **Root cause:** `ReminderScheduler.schedule()` preferred `setAlarmClock` and branched on `canScheduleExactAlarms()`, demanding `SCHEDULE_EXACT_ALARM` (API 31+ Settings nudge; denied by default on API 34+); Settings surfaced an "Alarms may be delayed · Allow exact alarms" button.
- **Fix:** LifeOS task/habit reminders don't need exact-to-the-minute delivery, so `schedule()` now always uses `alarmManager.setAndAllowWhileIdle(RTC_WAKEUP, triggerAt, pendingIntent)` — Doze-aware, permission-free, never throws for a missing exact-alarm grant, and the existing PendingIntent identity (`lifeos://reminder/<id>`, `FLAG_UPDATE_CURRENT|FLAG_IMMUTABLE`) still guarantees cancel/replace and no duplicates. The Settings exact-alarm launcher + button are removed (POST_NOTIFICATIONS request path and the global reminders toggle are unchanged), and `SCHEDULE_EXACT_ALARM` is gone from the manifest (POST_NOTIFICATIONS, VIBRATE, USE_FULL_SCREEN_INTENT remain). Past-skip, BootReceiver, Room source of truth and `cancelAll` untouched, verified by grep.

### 5. Unrelated UI
- No screen visuals, layouts, navigation structure, permissions flow or repository logic were touched beyond the four root causes above. The floating call/PiP overlay seen in screenshots is external (system) UI and was ignored.

### Files changed
- `app/build.gradle.kts` (add `core-splashscreen` 1.2.0)
- `app/src/main/AndroidManifest.xml` (MainActivity splash theme; drop `SCHEDULE_EXACT_ALARM`)
- `app/src/main/res/values/themes.xml` (add `Theme.LifeOS.Splash`)
- `app/src/main/java/com/lifeos/app/MainActivity.kt` (installSplashScreen + keep-on-screen; remove `BrandSplash`)
- `app/src/main/java/com/lifeos/app/LifeOSApplication.kt` (Ready-first, async re-arm)
- `app/src/main/java/com/lifeos/app/ui/components/LifeOSBottomBar.kt` (canonical navigate)
- `app/src/main/java/com/lifeos/app/core/reminders/ReminderScheduler.kt` (permission-free `setAndAllowWhileIdle`)
- `app/src/main/java/com/lifeos/app/ui/settings/SettingsScreen.kt` (remove exact-alarm launcher/button)
- `UPDATE.md`, plan `docs/superpowers/plans/2026-09-23-startup-navigation-reminders.md`

### Verification
```text
gradle :app:compileDebugKotlin   -> BUILD SUCCESSFUL
gradle :app:testDebugUnitTest    -> BUILD SUCCESSFUL (93 tests, 0 failures)
gradle :app:assembleDebug        -> BUILD SUCCESSFUL
gradle :app:lintDebug            -> BUILD SUCCESSFUL (0 errors; only pre-existing
                                     dependency-version / Info warnings)
```

### Remaining
- On-device: native-splash→Home transition timing, re-tap stays-on-tab behaviour, Profile→Back tab sync, and an actual reminder firing without exact-alarm access (no emulator/device here — verified via compile + 93 JVM tests + lint + code reasoning).

## 2026-09-23 — Back-nav fix on overlays, POST_NOTIFICATIONS gate, task-reminder parity & zombie-reminder normalization (branch `fix/startup-navigation-reminders`)

Completes this branch's hardening. No Room-schema (v4), entity, migration, repository-API or dependency-catalog changes.

### 1. Back navigation from a tab tapped on an overlay (Home → Profile → Habits → Back → Profile → Back → Home)
- **Root cause:** the previous fix made every bottom-bar tap the one canonical pattern — `navigate(route) { popUpTo(findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true }`, which is *always wrong when the current destination lives outside the tabs graph*. Tapping a tab while Profile/Habit Detail/Expenses is stacked on top **pops the overlay off the stack** (because `popUpTo` reaches past it to the tabs' start destination), so system Back from the tab returned to Home instead of Profile, and the overlay's saved state could ghost a duplicate.
- **Fix:** `LifeOSBottomBar` is now overlay-aware. `isOnTab` checks the visible destination's `hierarchy` for the nested tabs graph (`ROOT_TABS_GRAPH`, promoted from a private const to `internal const val` in `Screen.kt`, same package as the NavHost):
  - inside the tabs → the canonical switch is preserved unchanged;
  - on an overlay → the tab is **pushed on top** (no `popUpTo`), deduping first via `popBackStack(tabRoute, false)` so an existing tab below the overlay is returned to instead of duplicated; if none exists, `navigate { launchSingleTop = true }`. System Back now unwinds exactly Profile → tab → Profile → Home.

### 2. POST_NOTIFICATIONS is requested when a timed reminder is created — never at startup
- **Root cause:** the permission was only ever requested from the Settings toggle; creating a habit/task reminder on a fresh install silently scheduled a full-screen notification the OS would never deliver — a "broken reminder" the user had to debug via Settings.
- **Fix:** new `ReminderPermissionHost` composable (`ui/components/ReminderPermissionHost.kt`) owns its own request launcher (mirroring `PermissionManager`/`rememberPermissionState` semantics) and only runs the held action on actual OS grant. It is composed inline in the reminder creation/edit paths — `NewTaskDialog` Add, Habits add-habit confirm, `HabitDetailScreen.ReminderCard` (time + repeat-change arms), and the new task reminder editor — never at startup. API<33 or already granted → runs immediately; denied → an explanation AlertDialog ("Allow notifications?") holds the action until grant (or drops it); permanently denied → "Open Settings" via `PermissionManager.openAppSettings`. Clearing a reminder stays ungated. The Settings RemindersCard now routes a permanently-denied toggle to system Settings instead of a silent `request()`.

### 3. Task reminders reach full parity with habit reminders
- **Root cause:** habits get a dedicated reminder card in their detail screen; tasks could only set/remove a reminder at creation time — no edit, no repeat change, no way to see the alarm cadence from the task itself.
- **Fix:** `TaskRepository.setReminder(taskId, epochMillis, repeatType)` + `getReminderFor(taskId)` (mirrors `HabitRepository.setReminder`, sharing `syncReminderForTask` — same stable row id, so edit/clear replaces rather than duplicates), `TasksViewModel.updateReminder`/`reminderRepeatFor`, a tappable "Remind at …" chip on any task with a reminder, and `TaskReminderEditorDialog` (time picker + repeat selector + Save/Clear, insets-safe like `NewTaskDialog`), gated by the permission host when arming.

### 4. Past-due ("zombie") reminders normalize instead of silently never firing
- **Root cause:** `syncReminderForEntity` / `rebuildAllActive` / `rebuildFromMirrors` silently skipped rows whose trigger was already in the past, leaving them *enabled forever* with an alarm that could never fire — e.g. a DAILY reminder set after today's time would never trigger today's notification and stayed armed-forever-dormant rows in the DB.
- **Fix:** one arming path, `ReminderRepository.armNextOccurrence`, with a pure helper `ReminderScheduleCalculator.nextFutureOccurrenceMillis` (already-future → as-is; past DAILY/WEEKDAYS → fast-forward to the next future occurrence, persisted via `advanceTrigger`; past ONCE → disabled + alarm cancelled). All lifecycle arming now flows through it: create/edit sync, `rebuildAllActive` (boot/toggle/time-change), `rebuildFromMirrors` (backup restore / v4 upgrade), and `handleFired`'s recurring branch (which previously did a bare `rearmIfFuture`).

### Files changed
- `app/src/main/java/com/lifeos/app/ui/navigation/Screen.kt` (`ROOT_TABS_GRAPH` internal const)
- `app/src/main/java/com/lifeos/app/ui/navigation/LifeOSNavHost.kt` (reference shared const)
- `app/src/main/java/com/lifeos/app/ui/components/LifeOSBottomBar.kt` (overlay-aware tab tap)
- `app/src/main/java/com/lifeos/app/ui/components/ReminderPermissionHost.kt` (new)
- `app/src/main/java/com/lifeos/app/ui/tasks/TasksScreen.kt` (NewTaskDialog gate, TaskCard reminder chip, `TaskReminderEditorDialog`, VM `updateReminder`/`reminderRepeatFor`)
- `app/src/main/java/com/lifeos/app/data/repository/TaskRepository.kt` (`setReminder`/`getReminderFor`)
- `app/src/main/java/com/lifeos/app/ui/habits/HabitsScreen.kt` (add-habit gate)
- `app/src/main/java/com/lifeos/app/ui/habits/HabitDetailScreen.kt` (ReminderCard arms gated)
- `app/src/main/java/com/lifeos/app/ui/settings/SettingsScreen.kt` (permanently-denied → open Settings)
- `app/src/main/java/com/lifeos/app/core/reminders/ReminderScheduleCalculator.kt` (`nextFutureOccurrenceMillis`)
- `app/src/main/java/com/lifeos/app/data/repository/ReminderRepository.kt` (`armNextOccurrence`; rewire all arming)
- `app/src/test/java/com/lifeos/app/core/reminders/ReminderScheduleCalculatorTest.kt` (+6 fast-forward cases)
- `UPDATE.md`, plan `docs/superpowers/plans/2026-09-23-task-reminder-navigation-permissions.md`

### Verification
```text
gradle :app:compileDebugKotlin   -> BUILD SUCCESSFUL
gradle :app:assembleDebug        -> BUILD SUCCESSFUL
gradle :app:testDebugUnitTest    -> BUILD SUCCESSFUL (99 tests, 0 failures)
gradle :app:lintDebug            -> BUILD SUCCESSFUL (0 errors; only pre-existing warnings)
```

### Remaining
- On-device: overlay back-stack behaviour, the permission dialog lifecycle, and a real notification firing at the set time (no emulator/device in this sandbox — verified via compile + 99 JVM tests + lint + code reasoning).
 
### 2026-09-26 — Diary UI v2 complete implementation

- **Empty day (`ui/diary/DiaryEmptyState.kt`)** now uses an editorial empty state with a local Compose Canvas journal illustration, clearer story-start hierarchy, and a direct capture action. No remote image or network asset.
- **Composer (`ui/diary/DiaryEditor.kt`)** remains a full-screen writing surface but is now keyboard-first: the text field requests focus when the editor opens, the software keyboard is shown, and the existing IME/navigation inset union keeps the save row above the keyboard without double padding. Character count now handles singular/plural text cleanly.
- **Save feedback (`DiaryViewModel.kt` + `DiaryScreen.kt`)** now emits a monotonic confirmation event only after a successful local repository write and renders a short `Memory saved` fade confirmation after the editor closes.
- **Saved detail / timeline / date strip / mood selector** continue using the same editorial language, selected-day Room flow, mood markers, delete confirmation and bounded date navigation; no alternate data path was introduced.
- **Testing**: added a JVM test for the save-confirmation event and blank-save rejection. Existing DiaryViewModel and DayStripRange coverage remains in place.
- **Database/security**: no Room schema or migration change, no new permission, no network/API, no external AI, no telemetry and no dependency change.
- **Verification status**: GitHub branch source was updated successfully. No Android emulator/device or GitHub Actions workflow run is available for this branch in the connected environment, and local clone/build is unavailable because outbound GitHub DNS/network access is unavailable here. No build pass is claimed.


### 2026-09-26 — Diary UI reference alignment pass

- Applied the approved screen direction to the responsive Diary surface on `feature/diary-ui-v3-screen-references` without introducing phone-specific coordinates.
- The main `+` action is anchored to the measured content box with shared LifeOS safe-content spacing; it is not positioned by absolute x/y coordinates.
- Diary remains day-first and starts from `DateTimeUtils.today()` through `DiaryViewModel`, with the selected day and existing one-year history bounds preserved.
- Diary data remains Room-backed through `DiaryRepository`; unified Timeline remains connected through `BuildTimelineUseCase`, which includes Diary entries in its day aggregation.
- Existing editor focus/keyboard/inset behavior, timestamp capture, save guard, mood selection, detail/edit/delete routes and offline-first constraints remain in place.
- Scope guard: reference-only affordances are not backed by fabricated data. Photos, weather, location, voice, tags, favorites and share/copy are only surfaced when an existing local data/feature path supports them; no fake values or network service were introduced.
- Verification: branch source inspected after changes. No Android device/emulator or CI run is available in the connected environment, so build/device success is not claimed.

## 2026-09-27 — Diary × Stitch alignment: functional repairs and colour system (branch `feat/stitch-timeline-diary`)

Full gap analysis and root causes: `STITCH_AUDIT.md`. That file is separate from
`AUDIT_REPORT.md`, which is a whole-codebase audit taken on an older branch and
still describes the database as v1 (it is now v5); it is left untouched.

### Functional repairs

- **Storage work moved off the UI thread** (`data/repository/DiaryRepository.kt`).
  `delete`, `updateEntry`, `removeAttachment`, `createEntry` and `restoreFromBackup`
  each decoded the attachment JSON and called `MediaStorage.deleteIfExists` — a
  blocking `File.delete()` — on whatever thread called in. Every caller is a
  `viewModelScope`, so removing one photo unlinked a file on the main thread. The
  repository now owns a `withContext(io)` around that work; `io` is injectable
  (`Dispatchers.IO` in production) so tests drive it on a test scheduler.
- **Delete no longer races the screen that requested it** (`ui/diary/DiaryDetailScreen.kt`).
  Confirming a delete calls `viewModel.delete(id)` and then `onBack()` on the very
  next line; popping clears the back-stack entry, which clears the ViewModelStore
  entry, which cancels `viewModelScope` — the delete was competing with its own
  cancellation and lost whenever the encrypted commit was slower than the
  transition. Wrapped in `withContext(NonCancellable)`. Deliberately *not* applied
  to `DiaryEditorViewModel.save()`: the composer's ViewModel outlives the overlay,
  so there is no race there.
- **Copy now confirms itself** (`ui/diary/DiaryDetailScreen.kt`). A
  `SnackbarHostState` was created and `showSnackbar("Memory copied")` called, but
  no `SnackbarHost` was ever composed, so the confirmation was drawn nowhere. The
  host is now rendered at the bottom of the page.
- **Add-photo is reachable on a memory with no photos** (`ui/diary/DiaryDetailScreen.kt`).
  The photo strip carries the "add" tile but was gated on `photos.isNotEmpty()`,
  so a memory written without photos could never get one from its own detail page.
  The strip is now rendered unconditionally and shows the add tile on its own.
- **The Save button is no longer enabled when saving would be refused**
  (`ui/diary/DiaryEditor.kt`). The composable re-derived `canSave` locally as
  `content.isNotBlank() && !saving`, omitting the ViewModel's `isLoading` and
  "a take is still recording" guards — the pill looked live and the tap did
  nothing. The parameter is now `canSave`, taken from `DiaryEditorState.canSave`.
- **Status-bar insets applied to both diary roots** (`DiaryScreen.kt`,
  `DiaryDetailScreen.kt`). `MainActivity` uses `enableEdgeToEdge()` and the only
  `Scaffold` is the bottom-bar one, so the day view and detail page received no
  top inset and their back buttons and date lines rendered under the clock. Now
  matching what `DiaryEditorOverlay` and `ProfileScreen` already did.
- **Photo remove target enlarged to 48dp** (`ui/diary/DiaryComponents.kt`). The
  visible chip is unchanged at 24dp and corner-aligned; the clickable area is now
  the full accessible minimum.
- **Corrected a false comment**: `DiaryEditorOverlay` claimed it "intentionally
  covers the bottom bar". It does not — the `NavHost` is already inset by the
  outer `Scaffold`. The comment now describes the real arrangement.

### Visual / Stitch alignment

- **Split the colour that was doing two jobs** (`ui/theme/Color.kt`). `DiaryInkViolet`
  was both the heading ink and the colour of every pressable control, so the FAB
  and the primary actions were near-black and read as disabled. Stitch's primary
  `#6C47EB` is now `DiaryActionViolet` and is used for anything pressable;
  `DiaryInkViolet` is retuned to Stitch's heading indigo `#211A44` and used only
  for text and the timeline spine. The rule is legible from hue alone: pressable
  is violet, readable is ink. Added `DiaryActionVioletPressed`, `DiaryActionVioletSoft`
  and `DiaryTagInk` (`#493D7B`).
- FAB is a white `Icons.Filled.Add` on the action violet (was a `Text("+")` glyph
  on near-black); "+ Memory" and the saved-sheet primary action gain Stitch's
  full-pill treatment; the Save pill is white-on-violet with the pale disabled
  pill; the selected-mood ring is Stitch's 2dp `#6C47EB`; tag chips use
  `#493D7B` on lavender; the detail Edit/Share/Copy actions and the add-photo,
  add-voice, add-location and add-tag affordances are action violet.

### Scope held

- No Room schema change and no migration: the database is still v5 and
  `DiaryEntity` is untouched. Media continues to serialise into the existing
  `attachmentsJson` column.
- No new permission, no network/API, no external AI, no telemetry, no dependency
  change, no WebView, and no Stitch HTML/CSS copied into the app.
- `AndroidManifest.xml`, `build.gradle.kts` and the Room entities are byte-identical
  to `HEAD`.
- Deliberately not changed, and why, is recorded in `STITCH_AUDIT.md` §3:
  `LifeOSSpacing.screenPadding` stays at 24dp (shared by every screen; Stitch asks
  for 20dp), the timeline keeps its established cardless editorial layout, and
  `DiaryEntity.title` stays nullable because the design has no title field.

### Testing

- `app/src/test/java/com/lifeos/app/data/repository/DiaryRepositoryTest.kt` (new, 9 tests).
  Real files in a temp directory, not a mocked `MediaStorage`, because the
  behaviour is precisely "does the user's photo actually get unlinked". Covers:
  delete removes the row and its media; delete never touches a sibling entry's
  media; replacing attachments reclaims what was removed and keeps the rest;
  in-place removal; a stale tap on an unheld path changes nothing; a media file
  already gone does not fail the delete; and two tests asserting no storage work
  happens on the calling thread.
- `app/src/test/java/com/lifeos/app/ui/diary/DiaryDetailViewModelTest.kt` (new, 4 tests).
  Drives the delete/pop race directly: the DAO's read is held on a
  `CompletableDeferred` (a *cancellable* suspension) so the test can prove the
  write was still in flight when `ViewModelStore.clear()` ran. The other three
  pin the adjacent actions on the same ViewModel — deleting an already-gone
  entry, toggling favourite, and removing an attachment the entry does not hold.
- `app/src/test/java/com/lifeos/app/ui/diary/DiaryViewModelTest.kt` (extended, +3 tests).
  Pins each guard in `DiaryEditorState.canSave` that the Save button now renders
  from, since that property is now a promise about what the user sees. The existing
  construction sites pass the test scheduler to `DiaryRepository`.
- Both riskiest fixes were confirmed to **fail** with the fix reverted, so they are
  real regression tests and not tautologies:
  - reverting `NonCancellable` → `a delete is honoured even though the screen that
    asked for it was closed mid-flight` fails.
  - neutralising the IO switch → `deleting an entry does no storage work on the
    calling thread` and its create counterpart fail.

### Verification

```text
gradle --offline :app:compileDebugKotlin        -> BUILD SUCCESSFUL in 1m 13s   (0 errors; only pre-existing deprecation warnings)
gradle --offline :app:testDebugUnitTest --rerun-tasks
                                                 -> BUILD SUCCESSFUL in 2m 19s
                                                    173 tests, 0 failures, 0 skipped, 18 suites
gradle --offline :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --rerun-tasks
                                                 -> BUILD SUCCESSFUL in 4m 35s
                                                    app/build/outputs/apk/debug/app-debug.apk (42.3 MB)
                                                    lint: 8 issues, 0 errors, 0 in the diary — all pre-existing in
                                                    TasksScreen/strings.xml/AlarmPlaybackService/mipmap/AlarmFullScreenActivity
```

All three were re-run together with `--rerun-tasks` after a full diff review, so
these are clean runs and not cached `UP-TO-DATE` results. The diff review caught
one defect in the new work, since fixed: the `SnackbarHost` initially added
`navigationBarsPadding()`, which would have double-counted the navigation-bar
inset — the `NavHost` is already inset above a bottom bar that consumes that
inset itself. It now uses a fixed 16dp instead. Two unused colour tokens added
along the way were also removed rather than left as dead code.

### Remaining

- No Android emulator or device is available in this environment, so the on-device
  walkthrough — editor keyboard/insets, photo picker round-trip, recording, playback,
  share sheet, the Snackbar, and a visual comparison against the Stitch references
  at real densities — is **not** claimed. Everything above is verified by compile,
  173 JVM unit tests, lint and APK assembly. The new tests cover the two data-loss
  races directly; the remaining visual changes need a device pass before they can be
  called verified.
- `STITCH_AUDIT.md` §4 records that the Stitch HTML/CSS itself was not readable
  (zero-byte downloads, `webfetch` 400). The design-system guidance, theme tokens
  and screen metadata were readable and are what the visual changes are based on.

## 2026-09-27 — Diary reference integration pass

Applied the supplied Diary references to the existing production Diary without changing the app architecture, Room schema, navigation model, or shared/non-Diary features.

### Changed

- \`ui/diary/DiaryDayHeader.kt\`
  - New reference-style masthead: TODAY/relative day eyebrow, large full date, chevron, calendar action and overflow menu.
  - Existing bounded \`DiaryDateStrip\` remains the single day-selection mechanism.
  - Header actions route to existing Diary create/today/back behavior; no parallel navigation was introduced.

- \`ui/diary/MemoryTimeline.kt\`
  - Memory moments now use rounded paper cards with mood pills, readable body text, optional tags, real attachment photo previews and overflow Edit/Delete actions.
  - Existing chronological spine and Room-backed callbacks are preserved.
  - Photos are decoded from persisted \`attachmentsJson\`; missing files degrade to an explicit unavailable state instead of fake imagery.

- \`ui/diary/DiaryEditor.kt\`
  - Reference-style full-screen editor: Edit/New Memory title, top Save changes action, Date + Time selector surface, mood block, bounded 1000-character writing card and real photo/tag sections.
  - Date/time controls update the existing ViewModel fields rather than adding a new persistence path.
  - Existing IME/navigation-bar inset union remains the safe-area mechanism.

- \`ui/diary/DiaryEditorAttachments.kt\`
  - Photo and Tags now appear before optional Voice note / Location / Weather, matching the supplied editor hierarchy while preserving every existing attachment capability.

- \`ui/diary/DiaryComponents.kt\`
  - Photo tiles remain real app-private media with accessible 48dp removal targets.
  - Add-photo tile and tag-add dialog now use the supplied rounded/lavender reference language.
  - Shared Diary panels remain local to the Diary feature.

- \`ui/diary/DiaryDetailScreen.kt\`
  - Reference-style header, rounded content/media card, location/weather card when real local data exists, tag card, derived metadata card and four bottom action tiles.
  - Share, Copy, Delete and Favorite remain the existing real actions.
  - Existing delete NonCancellable protection and FileProvider sharing remain intact.

- \`ui/diary/DiaryEditorViewModel.kt\`
  - Draft body input is capped at 1000 characters for parity with the editor counter.
  - Draft photo/voice file cleanup is now launched on \`Dispatchers.IO\` instead of doing blocking file deletion on the UI thread.

- \`docs/superpowers/plans/2026-09-27-diary-reference-production-pass.md\`
  - Added the scoped implementation/verification plan.

### Scope / preservation

- No Room entity, DAO, migration, schema JSON, navigation route or DI architecture changes.
- No cloud, remote DB, AI API, telemetry, WebView, or new dependency.
- No non-Diary feature changed.
- Existing offline-first media, location, weather-unavailable semantics, save confirmation state, favorite behavior and Back navigation remain in the existing architecture.

### Verification status

Source was inspected and committed through the connected GitHub repository.

The connected environment does not provide the local Android project checkout/Gradle runtime, emulator, device or AVD, so no new compile/test/lint/device result is claimed for this branch. The repository's previously recorded 173-test verification in the prior Diary pass remains historical and is not presented as verification of these new commits.

### Remaining

A real device/emulator pass is still required to visually compare the four supplied references at phone density and to verify the keyboard/inset behavior, Photo Picker round-trip, tag dialog, audio, detail actions and overflow affordances after these UI changes.


### 2026-09-27 — Diary compact spacing + Mood UI removal

- Scoped UI-only correction for the supplied Diary/video references. The Day header, timeline cards, editor, attachment stack and detail editor were tightened without replacing the existing navigation, ViewModels, repository, Room model or media/location flows.
- New/Edit Memory: removed the interactive Mood heading and emoji selector from the composer and reflowed the remaining content upward. The existing DiaryEntity.mood field and stored historical mood data remain untouched; saved mood pills/markers outside the composer are preserved.
- Spacing: reduced oversized editor section gaps, tightened the Date/Time surface, changed the memory editor from a fixed 210dp height to a content-driven heightIn(min = 150dp), tightened Photos/Tags/Voice/Location rhythm, and reduced timeline card/spine spacing. Existing inset + vertical-scroll keyboard handling remains in place.
- Timeline: reduced the gap before/inside memory cards while preserving the existing FAB anchored to the lower-right content area via the existing non-coordinate-based layout.
- Database/security/offline: no Room schema/migration change, no new permission, no network/API, no external AI, no telemetry, and no dependency change.
- Files changed: ui/diary/DiaryEditor.kt, ui/diary/DiaryScreen.kt, ui/diary/DiaryDetailScreen.kt, ui/diary/DiaryEditorAttachments.kt, ui/diary/MemoryTimeline.kt.

### Verification

- Source diff was re-read after each targeted update.
- The connected GitHub environment does not provide the local Android checkout/Gradle runtime, emulator/device, or AVD. Therefore no new compile/test/lint/device-pass result is claimed for this UI change.
- Previously recorded Diary verification in UPDATE.md is historical and is not presented as verification of these latest commits.


## 2026-09-27 — Diary reference spacing + create action correction

- Removed the nested Diary `Scaffold`/duplicate system inset path that was adding a second top inset inside the already inset `LifeOSNavHost`; this removes the large hardcoded-looking blank band above the Diary header.
- Empty Diary state no longer treats the large illustration/halo as a create-memory tap target. A persistent lower-right violet `+` FAB now creates a memory on both empty and populated days, matching the supplied reference interaction.
- Reduced the timeline list's initial top padding and centralized Diary editor spacing values in `LifeOSSpacing`.
- Editor now uses the parent destination's existing safe area and only adds IME padding when the keyboard is present. The writing field uses `BringIntoViewRequester` so focusing the memory field scrolls it into view instead of leaving excessive whitespace or hiding the input behind the keyboard.
- Reduced the empty-editor writing surface minimum height from 150dp to the new shared 132dp Diary token while preserving the 1000-character limit and all existing Room/ViewModel/attachment behavior.
- No Room schema/migration, navigation route, permission, network, AI, telemetry or dependency changes.

### Verification

- Branch source was re-read after the changes and compared against `main`.
- GitHub comparison: 4 source files changed; no database/navigation files changed.
- No Android Gradle runtime, emulator/device or CI execution is available through the connected GitHub environment, so compile/test/lint/device success is not claimed for this pass.


## 2026-09-28 — Diary composer and date-strip UX pass (branch `fix/diary-keyboard-composer-ux-20260928`)

User-facing cleanup based on the supplied Diary screenshots. No Room schema, navigation, permission, network or dependency changes.

- Empty-day FAB moved lower so it no longer sits on top of the story copy while retaining the existing bottom-navigation clearance.
- Diary masthead/date strip spacing tightened so the five-day strip reads as one clean date control and the selected day has less visual bulk; history bounds and today limit remain unchanged.
- The composer plan is staged in `docs/superpowers/plans/2026-09-28-diary-composer-keyboard-ux.md` for the remaining keyboard/editor pass: IME-safe resizing/scroll, inline attachment actions and mood-row removal while preserving the persisted mood/attachments contracts.

Verification for this branch is source-level only; no local Android runtime is exposed by the GitHub connector.

## 2026-09-28 — Diary video UI correction pass (branch `fix/diary-video-ui-20260928`)

- **Editor keyboard behavior:** moved IME padding onto the actual vertically scrolling editor content instead of the fixed editor root. The header remains stable while the writing area and attachments consume the space available above the keyboard, reducing the large blank/covered area seen during typing.
- **Unwanted location indicator:** the video's small circular indicator was traced to the existing `LocationStatus.REQUESTING` UI. It now uses a subtle static status mark rather than an always-active `CircularProgressIndicator`, while the request state/message remains explicit.
- **Empty-day FAB clearance:** normalized the bottom clearance so the FAB uses the shared Diary content clearance without an extra arbitrary 12dp offset.
- **Preserved:** Compose navigation, ViewModels, DiaryRepository/Room contracts, attachments, permissions, offline-first behavior, schema and dependencies are unchanged.

### Verification

- GitHub branch comparison from `main`: 3 source files changed, 0 database/navigation/build dependency files changed.
- No local Android Gradle runtime, emulator/device or AVD is exposed through the connected GitHub environment. Therefore no compile/test/lint/device pass is claimed for this pass.


## 2026-09-28 — Diary saved timeline reference UI pass

- Refined `ui/diary/MemoryTimeline.kt` only for the saved-entry presentation: widened the time lane slightly, increased the timeline lane width, tightened the card rhythm, and reduced body typography so long memories read closer to the supplied reference.
- Kept the content-aware continuous spine introduced in the previous timeline fix; node geometry now follows the same 18dp center used by the rail.
- Preserved existing mood chips, tags, persisted photo previews, overflow Edit/Delete actions, click-to-open behavior, Room/ViewModel/repository boundaries and dynamic card height.
- No Room schema/migration, navigation, permission, network/API, external AI, telemetry or dependency changes.
- No absolute coordinates, fixed card heights or timeline-height assumptions were introduced.

### Verification

- GitHub source was re-read after the targeted edit.
- This connected environment does not expose the local Android Gradle runtime, emulator/device or AVD, so no new compile/test/lint/device result is claimed for this PR.


## 2026-09-28 — Diary reference-focused saved timeline v2

- Focused the saved-entry visual redesign in `ui/diary/MemoryTimeline.kt` around the supplied third reference: time gutter, timeline lane/node proportions, tighter card spacing, rounded card geometry and lighter long-form body typography.
- Kept the existing content-aware continuous spine, dynamic card heights, mood display, real local photo previews, tags, overflow actions and open/edit/delete callbacks.
- No Diary header, ViewModel, repository, Room/schema, navigation, permission, dependency, network or offline behavior changed in this pass.
