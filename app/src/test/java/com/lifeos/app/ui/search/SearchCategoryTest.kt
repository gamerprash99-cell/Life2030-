package com.lifeos.app.ui.search

import com.lifeos.app.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchCategoryTest {

    @Test
    fun `notes hits route to the note editor with the hit id`() {
        assertEquals("notes/editor?noteId=note-42", SearchCategory.NOTES.routeFor("note-42"))
    }

    @Test
    fun `tasks hits route to the tasks list`() {
        assertEquals(Screen.Tasks.route, SearchCategory.TASKS.routeFor("task-1"))
    }

    @Test
    fun `expenses hits route to the expenses list`() {
        assertEquals(Screen.Expenses.route, SearchCategory.EXPENSES.routeFor("expense-1"))
    }

    @Test
    fun `diary hits route to the matching diary entry, not just the list`() {
        assertEquals(Screen.DiaryDetail.createRoute("diary-7"), SearchCategory.DIARY.routeFor("diary-7"))
    }

    @Test
    fun `every category resolves to a non-empty route`() {
        SearchCategory.entries.forEach { category ->
            assertTrue(category.routeFor("some-id").isNotBlank())
        }
    }
}