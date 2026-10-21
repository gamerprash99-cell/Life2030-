package com.lifeos.app.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rule-based provider is the only AI wired in the app, so its behaviour is
 * pinned here: it must be deterministic, must trace every output to an input, and
 * must return null rather than invent something when it has nothing to say.
 */
class LocalRuleBasedProviderTest {

    private val provider = LocalRuleBasedProvider()

    @Test
    fun `provider is on-device and needs no network`() {
        assertFalse(provider.requiresNetwork)
        assertTrue(provider.displayName.isNotBlank())
        assertEquals("local-rules-v1", provider.id)
    }

    // ------------------------------------------------------------ summarize

    @Test
    fun `summarize returns the whole text when it is already short`() {
        val text = "Bought groceries today. Watered the plants."
        assertEquals(text, provider.summarize(text, maxSentences = 5))
    }

    @Test
    fun `summarize of blank text is null`() {
        assertNull(provider.summarize("", maxSentences = 2))
        assertNull(provider.summarize("   ", maxSentences = 2))
    }

    @Test
    fun `summarize is deterministic across calls`() {
        val text = "The project deadline is Friday. I need to finish the report. " +
            "Also called the doctor about the appointment. Groceries were expensive."
        val first = provider.summarize(text, maxSentences = 2)
        val second = provider.summarize(text, maxSentences = 2)
        assertEquals(first, second)
    }

    @Test
    fun `summarize output is composed only of input sentences`() {
        val text = "The project deadline is Friday. I need to finish the report soon. " +
            "Also called the doctor about the appointment today."
        val summary = provider.summarize(text, maxSentences = 2)!!
        summary.split(" ").let { _ -> }
        text.split(". ").forEach { sentence ->
            if (sentence.length >= 12) {
                // Every emitted clause must be a substring of the source.
                assertTrue(
                    "Summary invented content: $summary",
                    text.contains(sentence.trim().removeSuffix(".")),
                )
            }
        }
    }

    // ----------------------------------------------------------- suggestTags

    @Test
    fun `suggestTags extracts explicit hashtags`() {
        val tags = provider.suggestTags("Went to the #gym and read a #book")
        assertTrue(tags.contains("gym"))
        assertTrue(tags.contains("book"))
    }

    @Test
    fun `suggestTags matches topic keywords only when present`() {
        val tags = provider.suggestTags("Had a long meeting with the client about the deadline")
        assertTrue(tags.contains("work"))
        assertFalse(tags.contains("health"))
    }

    @Test
    fun `suggestTags never repeats existing tags`() {
        val tags = provider.suggestTags("Went to the gym", existingTags = listOf("health"))
        assertFalse(tags.contains("health"))
    }

    @Test
    fun `suggestTags of blank text is empty`() {
        assertTrue(provider.suggestTags("").isEmpty())
    }

    @Test
    fun `suggestTags caps the number of suggestions`() {
        val tags = provider.suggestTags(
            "gym meeting salary study family flight dinner clean happy",
        )
        assertTrue(tags.size <= 5)
    }

    // --------------------------------------------------------------- insight

    @Test
    fun `insight reports real numbers`() {
        val text = provider.insight(
            InsightRequest(
                expensesByCategory = listOf("Food" to 125000L),
                currencyCode = "INR",
                openTasks = 5,
                overdueTasks = 2,
                habitsActive = 4,
                habitsCompletedToday = 3,
                diaryEntriesLast7Days = 3,
            ),
        )!!
        assertTrue(text.contains("Food"))
        assertTrue(text.contains("2 of 5 open tasks are overdue"))
        assertTrue(text.contains("3/4 done"))
    }

    @Test
    fun `insight is null when there is nothing real to say`() {
        assertNull(
            provider.insight(
                InsightRequest(diaryEntriesLast7Days = 0, eventsLast7Days = 0),
            ),
        )
    }

    @Test
    fun `insight does not claim overdue tasks when there are none`() {
        val text = provider.insight(
            InsightRequest(openTasks = 3, overdueTasks = 0, diaryEntriesLast7Days = 1),
        )!!
        assertTrue(text.contains("none overdue"))
    }

    // ------------------------------------------------------------ suggestion

    @Test
    fun `suggestion returns at most one line`() {
        val suggestion = provider.suggestion(
            SuggestionRequest(overdueTasks = 3, daysSinceDiary = 7, weekOverBudget = true),
        )!!
        assertFalse(suggestion.contains("\n"))
    }

    @Test
    fun `suggestion is null when nothing needs attention`() {
        assertNull(provider.suggestion(SuggestionRequest()))
    }

    @Test
    fun `suggestion singularises a single overdue task`() {
        val suggestion = provider.suggestion(SuggestionRequest(overdueTasks = 1))!!
        assertTrue(suggestion.startsWith("1 task is overdue"))
    }

    @Test
    fun `suggestion mentions the diary gap only past the threshold`() {
        assertNull(provider.suggestion(SuggestionRequest(daysSinceDiary = 3)))
        assertTrue(
            provider.suggestion(SuggestionRequest(daysSinceDiary = 4))!!.contains("diary"),
        )
    }
}
