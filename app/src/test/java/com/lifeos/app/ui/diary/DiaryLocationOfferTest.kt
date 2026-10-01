package com.lifeos.app.ui.diary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Location row's state machine, tested directly.
 *
 * [locationRowOffer] is the single place that decides what the row offers, so
 * these tests exist to pin the one property the Phase 4 bug report was about:
 * **a state may never offer an action that cannot succeed.** Before this, every
 * status other than the two permission ones fell through to "Add current
 * location", so a device with location switched off still showed a live-looking
 * button that could do nothing.
 */
class DiaryLocationOfferTest {

    @Test
    fun `idle offers the normal add action and no explanation`() {
        val offer = locationRowOffer(LocationStatus.IDLE)

        assertEquals("Add current location", offer.title)
        assertEquals(LocationRowAction.REQUEST, offer.action)
        assertNull(offer.detail)
    }

    @Test
    fun `permission denied asks again, because the OS prompt is still available`() {
        val offer = locationRowOffer(LocationStatus.PERMISSION_DENIED)

        assertEquals(LocationRowAction.REQUEST, offer.action)
        // The detail has to name the actual requirement, not just the outcome.
        assertNotNull(offer.detail)
        assertTrue(offer.detail!!.contains("Allow location", ignoreCase = true))
    }

    @Test
    fun `permanent denial routes to app settings, never to another prompt`() {
        val offer = locationRowOffer(LocationStatus.PERMISSION_PERMANENTLY_DENIED)

        // Requesting again here is the exact failure mode PermissionManager was
        // written to prevent: Android silently no-ops a twice-denied prompt.
        assertEquals(LocationRowAction.OPEN_APP_PERMISSIONS, offer.action)
        assertEquals(LocationRowTone.WARNING, offer.tone)
        assertTrue(offer.detail!!.contains("Settings", ignoreCase = true))
    }

    @Test
    fun `location switched off routes to location settings, not app permissions`() {
        val offer = locationRowOffer(LocationStatus.SERVICE_DISABLED)

        assertEquals(LocationRowAction.OPEN_LOCATION_SETTINGS, offer.action)
        // The regression itself: this state must never offer the plain add
        // action, which cannot work while the device switch is off.
        assertTrue(offer.action != LocationRowAction.REQUEST)
        assertTrue(offer.title.contains("turned off", ignoreCase = true))
    }

    @Test
    fun `no location service offers no button at all`() {
        val offer = locationRowOffer(LocationStatus.NO_PROVIDER)

        assertEquals(LocationRowAction.NONE, offer.action)
        assertNotNull(offer.detail)
    }

    @Test
    fun `a missing fix is offered as a retry, not as an error`() {
        val offer = locationRowOffer(LocationStatus.NO_FIX)

        assertEquals(LocationRowAction.RETRY, offer.action)
        // A device that has just been woken up indoors has no fix yet. That is
        // a normal answer, not a failure, so it must not be dressed as one.
        assertEquals(LocationRowTone.NEUTRAL, offer.tone)
    }

    @Test
    fun `an unexpected failure is the only state that is presented as an error`() {
        val offer = locationRowOffer(LocationStatus.FAILED)

        assertEquals(LocationRowAction.RETRY, offer.action)
        assertEquals(LocationRowTone.ERROR, offer.tone)
    }

    @Test
    fun `a request in flight offers nothing, so no action appears mid-request`() {
        val offer = locationRowOffer(LocationStatus.REQUESTING)

        assertEquals(LocationRowAction.NONE, offer.action)
    }

    /**
     * The cross-state invariant, asserted over every status at once so a future
     * enum value cannot reintroduce the bug by omission.
     */
    @Test
    fun `no blocked state ever offers the plain add action`() {
        val blocked = listOf(
            LocationStatus.SERVICE_DISABLED,
            LocationStatus.NO_PROVIDER,
            LocationStatus.PERMISSION_PERMANENTLY_DENIED,
            LocationStatus.NO_FIX,
            LocationStatus.FAILED,
            LocationStatus.REQUESTING
        )

        blocked.forEach { status ->
            assertTrue(
                "$status must not offer REQUEST",
                locationRowOffer(status).action != LocationRowAction.REQUEST
            )
        }
    }

    @Test
    fun `every state that requires the user to act explains what to do`() {
        val actionable = listOf(
            LocationStatus.PERMISSION_DENIED,
            LocationStatus.PERMISSION_PERMANENTLY_DENIED,
            LocationStatus.SERVICE_DISABLED,
            LocationStatus.NO_PROVIDER,
            LocationStatus.NO_FIX,
            LocationStatus.FAILED
        )

        actionable.forEach { status ->
            val detail = locationRowOffer(status).detail
            assertNotNull("$status must explain itself", detail)
            assertTrue("$status detail must not be blank", detail!!.isNotBlank())
        }
    }

    @Test
    fun `red is reserved for the genuinely unexpected failure`() {
        val all = LocationStatus.entries
        val errors = all.filter { locationRowOffer(it).tone == LocationRowTone.ERROR }

        assertEquals(listOf(LocationStatus.FAILED), errors)
    }

    @Test
    fun `the composer's location icon label tells the truth in every state`() {
        // Same tap in every state, but never announced as a promise it cannot
        // keep — the a11y twin of the offer invariant above.
        assertEquals("Add a location to this memory", locationActionDescription(LocationStatus.IDLE))
        assertTrue(locationActionDescription(LocationStatus.SERVICE_DISABLED).contains("turned off", ignoreCase = true))
        assertTrue(locationActionDescription(LocationStatus.PERMISSION_PERMANENTLY_DENIED).contains("settings", ignoreCase = true))
        assertTrue(locationActionDescription(LocationStatus.NO_PROVIDER).contains("no location service", ignoreCase = true))
    }
}
