package com.lifeos.app.ui.navigation

import com.lifeos.app.core.life.LifeDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LifeDestinationRoutesTest {

    @Test
    fun `note destination routes to the note editor with its id`() {
        assertEquals(Screen.NoteEditor.createRoute("note-1"), LifeDestination.Note("note-1").route())
    }

    @Test
    fun `section destinations route to their screens`() {
        assertEquals(Screen.Notes.route, LifeDestination.Notes.route())
        assertEquals(Screen.Diary.route, LifeDestination.Diary.route())
        assertEquals(Screen.Tasks.route, LifeDestination.Tasks.route())
        assertEquals(Screen.Habits.route, LifeDestination.Habits.route())
        assertEquals(Screen.Expenses.route, LifeDestination.Expenses.route())
        assertEquals(Screen.Home.route, LifeDestination.Home.route())
    }

    @Test
    fun `every destination resolves to a non-empty route`() {
        listOf(
            LifeDestination.Notes,
            LifeDestination.Diary,
            LifeDestination.Tasks,
            LifeDestination.Habits,
            LifeDestination.Expenses,
            LifeDestination.Home,
            LifeDestination.Note("x")
        ).forEach { destination ->
            assertTrue(destination.route().isNotBlank())
        }
    }
}