package com.lifeos.app.ui.components

import com.lifeos.app.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomNavItemsTest {

    @Test
    fun `order is Home Tasks Habits`() {
        assertEquals(
            listOf(Screen.Home, Screen.Tasks, Screen.Habits),
            bottomNavItems.map { it.screen }
        )
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