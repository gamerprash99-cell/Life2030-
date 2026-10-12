package com.lifeos.app.ui.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The note editor must never throw away typing on an accidental Back. These
 * cases pin the rule that decides when leaving is safe: a new note is dirty only
 * once it holds something, while an existing note is dirty on any change from
 * what was loaded — including clearing a field.
 */
class NoteEditorDirtyTest {

    private val existing = NoteBaseline(
        isNew = false,
        title = "Grocery list",
        content = "milk, eggs",
        tags = listOf("home")
    )

    private val newNote = NoteBaseline(isNew = true, title = "", content = "", tags = emptyList())

    @Test
    fun `untouched existing note is not dirty`() {
        assertFalse(isNoteDirty("Grocery list", "milk, eggs", listOf("home"), existing))
    }

    @Test
    fun `edited content marks an existing note dirty`() {
        assertTrue(isNoteDirty("Grocery list", "milk, eggs, bread", listOf("home"), existing))
    }

    @Test
    fun `added tag marks an existing note dirty`() {
        assertTrue(isNoteDirty("Grocery list", "milk, eggs", listOf("home", "urgent"), existing))
    }

    @Test
    fun `clearing an existing note is a change and stays dirty`() {
        assertTrue(isNoteDirty("", "", emptyList(), existing))
    }

    @Test
    fun `trailing whitespace in the title is not a change`() {
        assertFalse(isNoteDirty("Grocery list  ", "milk, eggs", listOf("home"), existing))
    }

    @Test
    fun `an untouched new note is not dirty`() {
        assertFalse(isNoteDirty("", "", emptyList(), newNote))
    }

    @Test
    fun `typing into a new note makes it dirty`() {
        assertTrue(isNoteDirty("Idea", "", emptyList(), newNote))
    }

    @Test
    fun `a new note typed into and cleared again is not dirty`() {
        assertFalse(isNoteDirty("", "   ", emptyList(), newNote))
    }

    @Test
    fun `tag parsing trims blanks and empty segments`() {
        assertEquals(listOf("home", "urgent"), parseNoteTags(" home ,, urgent , "))
    }
}
