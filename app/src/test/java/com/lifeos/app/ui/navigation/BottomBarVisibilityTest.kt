package com.lifeos.app.ui.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomBarVisibilityTest {
    @Test
    fun `hides when app lock setup route`() {
        val route = Screen.AppLockSetup.route
        assertFalse(shouldShowBottomBar(route, composerOwnsWindow = false))
        assertFalse(shouldShowBottomBar(route, composerOwnsWindow = true))
    }

    @Test
    fun `hides when diary composer owns window for diary route`() {
        val route = Screen.Diary.route
        assertFalse(shouldShowBottomBar(route, composerOwnsWindow = true))
        assertTrue(shouldShowBottomBar(route, composerOwnsWindow = false))
    }

    @Test
    fun `hides when diary composer owns window for diary detail route`() {
        val route = Screen.DiaryDetail.route
        assertFalse(shouldShowBottomBar(route, composerOwnsWindow = true))
        assertTrue(shouldShowBottomBar(route, composerOwnsWindow = false))
    }

    @Test
    fun `shows for tabs when composer not owned`() {
        assertTrue(shouldShowBottomBar(Screen.Home.route, false))
        assertTrue(shouldShowBottomBar(Screen.Tasks.route, false))
        assertTrue(shouldShowBottomBar(Screen.Habits.route, false))
        assertTrue(shouldShowBottomBar(Screen.Profile.route, false))
    }

    @Test
    fun `hides for full-window redesign flows`() {
        assertFalse(shouldShowBottomBar(Screen.Capture.route, false))
        assertFalse(shouldShowBottomBar(Screen.NoteEditor.route, false))
        assertFalse(shouldShowBottomBar(Screen.GlobalSearch.route, false))
        assertFalse(shouldShowBottomBar(Screen.AiAssistant.route, false))
    }

    @Test
    fun `shows for the new primary tabs`() {
        assertTrue(shouldShowBottomBar(Screen.Notes.route, false))
        assertTrue(shouldShowBottomBar(Screen.Insights.route, false))
        assertTrue(shouldShowBottomBar(Screen.Life.route, false))
    }

    @Test
    fun `handles null route safely`() {
        assertFalse(shouldShowBottomBar(null, composerOwnsWindow = false))
        assertFalse(shouldShowBottomBar(null, composerOwnsWindow = true))
    }
}
