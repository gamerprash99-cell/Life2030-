package com.lifeos.app.ui.diary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiaryMoodsTest {

    @Test
    fun `exactly five moods mirror the composer design`() {
        assertEquals(5, DiaryMoods.OPTIONS.size)
        assertTrue(DiaryMoods.OPTIONS.map { it.label }.containsAll(listOf("Happy", "Calm", "Sad", "Stressed", "Excited")))
    }

    @Test
    fun `stored equivalence is case-insensitive`() {
        assertEquals(DiaryMoods.OPTIONS.first(), DiaryMoods.fromStored("😊 Happy"))
        assertEquals(DiaryMoods.OPTIONS.first(), DiaryMoods.fromStored("😊 happy"))
    }

    @Test
    fun `unknown stored values resolve to null`() {
        assertNull(DiaryMoods.fromStored("Weird mood"))
        assertNull(DiaryMoods.fromStored(null))
    }

    @Test
    fun `display label falls back to the stored string for custom moods`() {
        assertEquals("So-so", DiaryMoods.displayLabel("So-so"))
        assertEquals("Calm", DiaryMoods.displayLabel("😌 Calm"))
        assertEquals("", DiaryMoods.displayLabel(null))
    }
}