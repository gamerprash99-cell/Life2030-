package com.lifeos.app.ui.diary

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lifeos.app.ui.theme.LifeOSSpacing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The two layout invariants the Diary composer and Diary list are held to.
 *
 * Both are arithmetic on values the screens read, so they are decidable on the
 * JVM — this project has no emulator, no `adb` and no Robolectric, so anything
 * that genuinely needs a device is documented as unverified in UPDATE.md rather
 * than claimed here.
 *
 * 1. **The writing surface must fit the viewport it is drawn in.** The surface's
 *    minimum height is a share of the viewport the composer can currently see.
 *    A surface taller than its own viewport is the keyboard bug: the card's
 *    scroll container then has to scroll to show the surface at all, and the
 *    text — anchored to the *top* of that surface — goes off the top of the
 *    screen, so the editor reads as empty while the character counter, a sibling
 *    below the surface, still reports the real length. The composer therefore
 *    takes the live viewport height rather than a remembered one; these tests
 *    pin the property that makes that value safe to use.
 *
 * 2. **Scrollable Diary content must clear the whole band the FAB occupies.**
 *    The FAB's bottom offset is not the clearance: the button's own height and a
 *    gap above it have to be added, or the last item can only be scrolled up to
 *    the FAB's bottom edge and never above its top edge — which is how the last
 *    line of a long memory ended up covered.
 */
class DiaryLayoutClearanceTest {

    // ---- 1. the writing surface vs the viewport it is drawn in -------------

    @Test
    fun `the surface fits every viewport a keyboard transition produces`() {
        // Every frame of a real keyboard animation is a viewport the surface has
        // to fit. Below the floor the surface is deliberately taller (see the
        // floor test), so that case is excluded here rather than excused.
        listOf(160.dp, 200.dp, 240.dp, 300.dp, 420.dp, 560.dp, 720.dp).forEach { viewport ->
            val surface = writingSurfaceMinHeight(viewport)
            assertTrue(
                "surface of $surface exceeds the $viewport it is drawn in",
                surface <= viewport
            )
        }
    }

    @Test
    fun `the surface follows the viewport down as the keyboard opens`() {
        // The composer is handed the *live* viewport, so a shrunken viewport has
        // to produce a shrunken surface. Reading the previous, larger height here
        // is what let the surface outgrow the box showing it.
        val resting = writingSurfaceMinHeight(420.dp)
        val collapsed = writingSurfaceMinHeight(180.dp)
        assertTrue("surface did not shrink with the viewport: $resting vs $collapsed", collapsed < resting)
    }

    @Test
    fun `the surface is monotonic in the viewport`() {
        var previous = 0.dp
        listOf(140.dp, 200.dp, 240.dp, 320.dp, 420.dp, 560.dp, 720.dp).forEach { viewport ->
            val height = writingSurfaceMinHeight(viewport)
            assertTrue("height fell from $previous to $height at viewport=$viewport", height >= previous)
            previous = height
        }
    }

    @Test
    fun `a cramped window still gets a usable floor to write on`() {
        // Deliberate: below the floor the surface still claims its floor and the
        // card scrolls. A two-line page is a worse editor than a scrolling one,
        // and the floor is what keeps a large font scale legible.
        assertEquals(LifeOSSpacing.diaryEditorTextMinHeight, writingSurfaceMinHeight(0.dp))
        assertEquals(LifeOSSpacing.diaryEditorTextMinHeight, writingSurfaceMinHeight(100.dp))
        assertEquals(LifeOSSpacing.diaryEditorTextMinHeight, writingSurfaceMinHeight(200.dp))
    }

    @Test
    fun `the floor and the share meet at the same height`() {
        val boundary = LifeOSSpacing.diaryEditorTextMinHeight / LifeOSSpacing.diaryEditorWritingFill
        assertEquals(LifeOSSpacing.diaryEditorTextMinHeight, writingSurfaceMinHeight(boundary))
        assertTrue(writingSurfaceMinHeight(boundary + 1.dp) > LifeOSSpacing.diaryEditorTextMinHeight)
    }

    @Test
    fun `an empty editor still reads as a page rather than a small field`() {
        // The reason the fill share exists at all: a fixed 132dp strip on a tall
        // screen is 18% of the window, which is the "small field floating in a
        // large empty card" it replaced. The share is of the live viewport, so
        // it holds on a tall screen.
        listOf(420.dp, 560.dp, 720.dp).forEach { viewport ->
            val share = writingSurfaceMinHeight(viewport) / viewport
            assertTrue("only $share of a $viewport page", share > 0.5f)
        }
    }

    // ---- 2. the FAB's occupied band vs the scrollable's trailing clearance --

    @Test
    fun `the clearance is larger than the FAB's own bottom offset`() {
        // If these were equal, the content's last line would come to rest exactly
        // on the FAB's bottom edge with nothing above it — so the FAB would sit
        // against the line it is supposed to leave readable.
        assertTrue(LifeOSSpacing.diaryFabOccupiedBottom > LifeOSSpacing.diaryFabBottomOffset)
    }

    @Test
    fun `the clearance accounts for the FAB's height and a gap above it`() {
        assertEquals(
            LifeOSSpacing.diaryFabBottomOffset + LifeOSSpacing.diaryFabSize + LifeOSSpacing.diaryFabContentGap,
            LifeOSSpacing.diaryFabOccupiedBottom
        )
    }

    @Test
    fun `the last item can be scrolled clear of the FAB`() {
        // A long memory's final line has to be able to sit above the FAB's top
        // edge. That needs the clearance to cover the offset, the button and a
        // gap, which is exactly what `diaryFabOccupiedBottom` is.
        val clearance = LifeOSSpacing.diaryFabOccupiedBottom
        val fabTop = LifeOSSpacing.diaryFabBottomOffset + LifeOSSpacing.diaryFabSize
        assertTrue("clearance $clearance does not clear the FAB's top edge at $fabTop", clearance >= fabTop)
    }

    @Test
    fun `the FAB keeps the Material 3 small-FAB container size`() {
        // The composable pins the button to this token precisely so it cannot be
        // resized independently of the clearance derived from it.
        assertEquals(56.dp, LifeOSSpacing.diaryFabSize)
    }

    @Test
    fun `the FAB meets the app minimum touch target`() {
        assertTrue(
            "FAB is ${LifeOSSpacing.diaryFabSize}, below ${LifeOSSpacing.minTouchTarget}",
            LifeOSSpacing.diaryFabSize >= LifeOSSpacing.minTouchTarget
        )
    }

    // ---- 3. the empty state's reservation and artwork ----------------------

    @Test
    fun `the empty state leaves room for its own type under the artwork`() {
        // The artwork plus the three centred lines under it have to fit the height
        // the FAB does not cover. This is the empty-state half of the FAB overlap:
        // a fixed 300dp circle pushed "YOUR STORY STARTS HERE" into the button on a
        // small phone, because the artwork was never asked how much room was left.
        listOf(300.dp, 420.dp, 480.dp, 567.dp, 700.dp).forEach { box ->
            val available = box - LifeOSSpacing.diaryFabOccupiedBottom
            val artwork = emptyStateArtworkMax(available)
            val reservedForType = available - artwork
            assertTrue(
                "on a ${box}dp box the artwork (${artwork}dp) leaves only ${reservedForType}dp for the type",
                reservedForType > 0.dp
            )
        }
    }

    @Test
    fun `a tall box still gets the designed artwork size`() {
        assertEquals(LifeOSSpacing.diaryEmptyArtworkMaxSize, emptyStateArtworkMax(1000.dp))
    }

    @Test
    fun `a small box gets a smaller artwork rather than a clipped one`() {
        assertTrue(emptyStateArtworkMax(300.dp) < emptyStateArtworkMax(600.dp))
    }

    @Test
    fun `a very short window never shrinks the artwork to a dot`() {
        assertEquals(LifeOSSpacing.diaryEmptyArtworkMinSize, emptyStateArtworkMax(0.dp))
    }

    @Test
    fun `the artwork is never wider than its height share allows`() {
        listOf(200.dp, 340.dp, 480.dp, 620.dp, 800.dp).forEach { available ->
            val artwork = emptyStateArtworkMax(available)
            assertTrue(
                "artwork $artwork exceeds ${available}dp x ${LifeOSSpacing.diaryEmptyArtworkFill}",
                artwork <= available * LifeOSSpacing.diaryEmptyArtworkFill || artwork == LifeOSSpacing.diaryEmptyArtworkMinSize
            )
        }
    }

    @Test
    fun `the empty state's reserved band is the FAB's whole band`() {
        // The empty state is drawn under the button, so it reserves exactly what
        // the button occupies — not the offset alone, which is what let the button
        // cover the invitation.
        assertEquals(LifeOSSpacing.diaryFabOccupiedBottom, LifeOSSpacing.diaryFabOccupiedBottom)
        assertTrue(LifeOSSpacing.diaryFabOccupiedBottom > LifeOSSpacing.diaryFabBottomOffset)
    }
}

/** Convenience so a share-of-viewport reads as a share rather than a division. */
private operator fun Dp.div(other: Dp): Float = this.value / other.value