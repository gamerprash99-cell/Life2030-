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
    fun `hides for the habit editor and its wizard`() {
        // The guided wizard and the full add/edit form share this route.
        assertFalse(shouldShowBottomBar(Screen.HabitEditor.route, composerOwnsWindow = false))
        assertTrue(Screen.HabitEditor.route.startsWith("habits/edit"))
    }

    @Test
    fun `habit editor owns the bottom inset so it is not applied twice`() {
        assertTrue(ownsBottomInset(Screen.HabitEditor.route, composerOwnsWindow = false))
        assertFalse(ownsBottomInset(Screen.Habits.route, composerOwnsWindow = false))
        assertFalse(ownsBottomInset(Screen.HabitDetail.route, composerOwnsWindow = false))
    }

    @Test
    fun `diary composer keeps owning the bottom inset`() {
        assertTrue(ownsBottomInset(Screen.Diary.route, composerOwnsWindow = true))
        assertTrue(ownsBottomInset(Screen.DiaryDetail.route, composerOwnsWindow = true))
        assertFalse(ownsBottomInset(Screen.Diary.route, composerOwnsWindow = false))
    }

    @Test
    fun `inset ownership handles null route safely`() {
        assertFalse(ownsBottomInset(null, composerOwnsWindow = false))
        assertTrue(ownsBottomInset(null, composerOwnsWindow = true))
        assertFalse(ownsTopInset(null))
    }

    @Test
    fun `full-window editors own their bottom inset`() {
        assertTrue(ownsBottomInset(Screen.NoteEditor.route, composerOwnsWindow = false))
        assertTrue(ownsBottomInset(Screen.AiAssistant.route, composerOwnsWindow = false))
        assertTrue(ownsBottomInset(Screen.Capture.route, composerOwnsWindow = false))
        // Snap keeps the root insets so the compose step (no status padding of
        // its own) is not pushed under the status bar.
        assertFalse(ownsBottomInset(Screen.Snap.route, composerOwnsWindow = false))
    }

    @Test
    fun `full-window editors own their top inset`() {
        assertTrue(ownsTopInset(Screen.NoteEditor.route))
        assertTrue(ownsTopInset(Screen.AiAssistant.route))
        assertTrue(ownsTopInset(Screen.Capture.route))
        assertFalse(ownsTopInset(Screen.Snap.route))
        assertFalse(ownsTopInset(Screen.HabitEditor.route))
        assertFalse(ownsTopInset(Screen.Diary.route))
        assertFalse(ownsTopInset(Screen.Habits.route))
    }

    @Test
    fun `shows for the new primary tabs`() {
        assertTrue(shouldShowBottomBar(Screen.Notes.route, false))
        assertTrue(shouldShowBottomBar(Screen.Life.route, false))
        assertTrue(shouldShowBottomBar(Screen.Profile.route, false))
    }

    @Test
    fun `handles null route safely`() {
        assertFalse(shouldShowBottomBar(null, composerOwnsWindow = false))
        assertFalse(shouldShowBottomBar(null, composerOwnsWindow = true))
    }
}
