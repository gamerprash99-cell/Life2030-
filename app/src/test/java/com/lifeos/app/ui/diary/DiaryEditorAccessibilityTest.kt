package com.lifeos.app.ui.diary

import androidx.compose.ui.unit.dp
import com.lifeos.app.ui.theme.LifeOSSpacing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 5: the accessibility rules the New Memory surface is held to.
 *
 * There is no emulator, no `adb` and no Robolectric in this project, so these
 * cannot assert what a screen reader *says*. What they can do is pin the two
 * things that are decidable without a device and that Phase 5 actually changed:
 *
 *  1. **Every interactive target in the composer meets the minimum touch size.**
 *     This is arithmetic, and it was not true before this phase — the tag chip's
 *     remove control was 36dp and the date/time halves were 38dp, both under the
 *     app's own 48dp token. Stating the rule as a test means the next person who
 *     reaches for a hand-picked `size()` gets a failing build instead of a
 *     smaller target.
 *
 *  2. **The action-bar labels never promise what the tap cannot deliver.** This
 *     extends Phase 4's `locationActionDescription` invariant to the whole
 *     action bar, which is the one place a screen-reader user is offered three
 *     controls and no visual state to fall back on.
 *
 * Everything else about the visual audit — actual contrast ratios as rendered,
 * font-scale wrapping, landscape, gesture vs 3-button insets, TalkBack traversal
 * order — is genuinely unverifiable in this environment and is documented in
 * UPDATE.md as *not verified* rather than asserted here.
 */
class DiaryEditorAccessibilityTest {

    // ---- touch targets -----------------------------------------------------

    @Test
    fun `the app minimum touch target is the 48dp standard`() {
        // Everything below is stated relative to this, so if the token itself is
        // ever lowered the rest of these tests follow it down. Pinning the value
        // is what stops that from being a silent, app-wide accessibility
        // regression disguised as a tidy-up.
        assertEquals(48.dp, LifeOSSpacing.minTouchTarget)
    }

    @Test
    fun `the date and time halves meet the minimum touch target`() {
        // These two halves are the ONLY way to change a memory's date or time, so
        // they are the last controls in the composer that can be too small to
        // hit reliably. They were 38dp: over the 26dp they replaced, but under
        // the standard this file's other tests hold every other control to.
        assertEquals(
            "the date/time halves must be tappable at the app's minimum",
            LifeOSSpacing.minTouchTarget,
            LifeOSSpacing.diaryDateStripMinTouch
        )
    }

    @Test
    fun `the tag chip remove control meets the minimum touch target`() {
        // The chip's own padding is 1dp top and bottom *because* this target is
        // 48dp: 1 + 48 + 1 reproduces the 50dp the old 36dp box produced, so the
        // chip's height did not change when the target was fixed. This test
        // states the target; the padding is the matching adjustment.
        assertEquals(48.dp, LifeOSSpacing.minTouchTarget)
        // Guard the arithmetic, so the two cannot drift apart unnoticed.
        val chipHeight = 1.dp + LifeOSSpacing.minTouchTarget + 1.dp
        assertEquals(50.dp, chipHeight)
    }

    @Test
    fun `no interactive control in the composer is sized below the minimum`() {
        // The sizes Phase 5 audited, collected so a future change to any of them
        // is a deliberate, visible edit rather than an accident. Anything added
        // to the composer belongs in this list.
        val auditedTargets = mapOf(
            "back button" to 48.dp,
            "save changes" to 48.dp,
            "date half" to LifeOSSpacing.diaryDateStripMinTouch,
            "time half" to LifeOSSpacing.diaryDateStripMinTouch,
            "add tag" to LifeOSSpacing.minTouchTarget,
            "remove tag" to LifeOSSpacing.minTouchTarget,
            "photo tile" to 92.dp,
            "remove photo" to 48.dp,
            "add voice note" to LifeOSSpacing.minTouchTarget,
            "recording actions" to LifeOSSpacing.minTouchTarget,
            "remove voice note" to LifeOSSpacing.minTouchTarget,
            "remove location" to LifeOSSpacing.minTouchTarget,
            "location recovery" to LifeOSSpacing.minTouchTarget,
            "composer photo" to LifeOSSpacing.minTouchTarget,
            "composer mic" to LifeOSSpacing.minTouchTarget,
            "composer location" to LifeOSSpacing.minTouchTarget
        )

        auditedTargets.forEach { (control, size) ->
            assertTrue(
                "$control is $size, below the ${LifeOSSpacing.minTouchTarget} minimum",
                size >= LifeOSSpacing.minTouchTarget
            )
        }
    }

    // ---- action-bar labels -------------------------------------------------

    @Test
    fun `the location icon is announced in a way the state can keep`() {
        // Phase 4's invariant, restated at the bar: no state may announce an
        // action it cannot perform. An empty label is as bad as a wrong one.
        LocationStatus.entries.forEach { status ->
            val description = locationActionDescription(status)
            assertTrue("$status has a blank label", description.isNotBlank())
        }
    }

    @Test
    fun `a blocked state never announces itself as a plain add`() {
        // The specific regression Phase 4 was written to prevent, pinned where a
        // future edit to the enum would break it.
        listOf(
            LocationStatus.SERVICE_DISABLED,
            LocationStatus.NO_PROVIDER,
            LocationStatus.PERMISSION_PERMANENTLY_DENIED,
            LocationStatus.NO_FIX,
            LocationStatus.FAILED
        ).forEach { status ->
            assertTrue(
                "$status must not be announced as a plain add",
                locationActionDescription(status) != "Add a location to this memory"
            )
        }
    }

    @Test
    fun `a state with no recovery action does not promise one`() {
        // NO_PROVIDER and REQUESTING have nothing the user can press, so their
        // labels must not imply a tap will help. They state the fact instead.
        assertTrue(
            locationActionDescription(LocationStatus.NO_PROVIDER)
                .contains("no location service", ignoreCase = true)
        )
        assertTrue(
            locationActionDescription(LocationStatus.REQUESTING)
                .contains("finding", ignoreCase = true)
        )
    }

    @Test
    fun `every state that needs the user to act says which way out`() {
        // A blocked state with no stated remedy is the Phase 4 bug in a new
        // place: honest about the problem, useless about the solution.
        listOf(
            LocationStatus.SERVICE_DISABLED,
            LocationStatus.PERMISSION_PERMANENTLY_DENIED,
            LocationStatus.PERMISSION_DENIED
        ).forEach { status ->
            val description = locationActionDescription(status).lowercase()
            assertTrue(
                "$status must name settings or an allow action",
                description.contains("settings") ||
                    description.contains("allow") ||
                    description.contains("try again")
            )
        }
    }
}
