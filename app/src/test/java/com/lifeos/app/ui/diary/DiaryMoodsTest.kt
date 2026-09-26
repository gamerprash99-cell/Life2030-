package com.lifeos.app.ui.diary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiaryMoodsTest {

    @Test
    fun `the eight moods mirror the composer design`() {
        assertEquals(8, DiaryMoods.OPTIONS.size)
        assertTrue(
            DiaryMoods.OPTIONS.map { it.label }
                .containsAll(listOf("Happy", "Calm", "Tired", "Sad", "Anxious", "Stressed", "Angry", "Excited"))
        )
    }

    @Test
    fun `labels are unique`() {
        val labels = DiaryMoods.OPTIONS.map { it.label }
        assertEquals(labels.size, labels.distinct().size)
    }

    @Test
    fun `persisted keys are unique`() {
        val keys = DiaryMoods.OPTIONS.map { it.key }
        assertEquals(keys.size, keys.distinct().size)
    }

    /**
     * The five moods LifeOS shipped before the Daily Memory redesign must keep
     * their exact persisted keys, or every existing entry loses its mood.
     */
    @Test
    fun `legacy mood keys are still resolvable`() {
        listOf("😊 Happy", "😌 Calm", "😔 Sad", "😤 Stressed", "🤩 Excited").forEach { key ->
            assertEquals(key, DiaryMoods.fromStored(key)?.key)
        }
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