package com.lifeos.app.ui.components

import com.lifeos.app.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomNavItemsTest {

    @Test
    fun `order is Home Expenses Habits Life`() {
        assertEquals(
            listOf(Screen.Home, Screen.Expenses, Screen.Habits, Screen.Life),
            bottomNavItems.map { it.screen }
        )
    }

    @Test
    fun `expenses replaced notes as the second primary tab`() {
        // The Notes route was deleted from Screen entirely, so no entry can carry it.
        assertTrue(bottomNavItems.any { it.screen == Screen.Expenses })
        assertFalse(bottomNavItems.any { it.screen.route == "notes" })
        assertEquals("Expenses", bottomNavItems[1].label)
    }

    @Test
    fun `profile is not a primary tab`() {
        assertTrue(bottomNavItems.none { it.screen == Screen.Profile })
    }

    @Test
    fun `habits is a permanent primary destination`() {
        assertTrue(bottomNavItems.any { it.screen == Screen.Habits })
    }

    @Test
    fun `every entry has a non blank label`() {
        assertTrue(bottomNavItems.all { it.label.isNotBlank() })
    }

    @Test
    fun `labels are unique`() {
        val labels = bottomNavItems.map { it.label }
        assertEquals(labels.size, labels.distinct().size)
    }

    @Test
    fun `no duplicate routes`() {
        val routes = bottomNavItems.map { it.screen.route }
        assertEquals(routes.size, routes.distinct().size)
    }
}
