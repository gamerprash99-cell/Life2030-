package com.lifeos.app.core.intelligence

import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.domain.model.TimelineItem
import com.lifeos.app.domain.model.TimelineItemType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiaryConnectionsTest {

    private fun entry(
        id: String = "e1",
        content: String = "Focused on study and felt happy.",
        mood: String? = "😊 Happy",
        day: Long = 20000
    ) = DiaryEntity(
        id = id, title = null, content = content, mood = mood,
        tagsCsv = "", dateEpochDay = day, timeMinutes = 60,
        createdAt = 0L, updatedAt = 0L
    )

    private fun timelineItem(
        id: String, type: TimelineItemType, title: String, day: Long = 20000
    ) = TimelineItem(
        id = id, type = type, title = title, subtitle = "detail for $id",
        dateEpochDay = day, timeMinutes = 120, icon = "x", sourceId = "s-$id"
    )

    @Test
    fun `empty inputs produce an empty graph`() {
        val graph = DiaryConnections.build(emptyList(), emptyList())
        assertTrue(graph.isEmpty)
        assertTrue(graph.nodes.isEmpty())
        assertTrue(graph.edges.isEmpty())
    }

    @Test
    fun `entry gains mood node and edge`() {
        val graph = DiaryConnections.build(listOf(entry()), emptyList())
        val types = graph.nodes.map { it.type }
        assertTrue(types.contains(DiaryNodeType.ENTRY))
        assertTrue(types.contains(DiaryNodeType.MOOD))
        assertTrue(graph.edges.any { it.relation == "mood" })
    }

    @Test
    fun `the stored mood string is preserved on the mood node`() {
        val graph = DiaryConnections.build(listOf(entry(mood = "😌 Calm")), emptyList())
        val moodNode = graph.nodes.first { it.type == DiaryNodeType.MOOD }
        assertEquals("😌 Calm", moodNode.label)
    }

    @Test
    fun `detected mood is used when no mood is stored`() {
        val happy = DiaryConnections.build(listOf(entry(mood = null, content = "amazing wonderful day")), emptyList())
        val moodNode = happy.nodes.first { it.type == DiaryNodeType.MOOD }
        assertTrue(moodNode.label in setOf("VERY_POSITIVE", "POSITIVE"))
    }

    @Test
    fun `shared keyword creates one node connected to every entry`() {
        val a = entry("e-a", content = "study biology and study again", day = 20000)
        val b = entry("e-b", content = "study mathematics in the morning", day = 20000)
        val graph = DiaryConnections.build(listOf(a, b), emptyList())

        val studyNode = graph.nodes.first { it.type == DiaryNodeType.KEYWORD && it.label == "study" }
        // Both entries mention "study", so it must be linked to both.
        assertEquals(setOf("entry-e-a", "entry-e-b"), graph.edges.filter { it.to == studyNode.id }.map { it.from }.toSet())
    }

    @Test
    fun `same-day timeline items are attached to the entry`() {
        val items = listOf(
            timelineItem("t1", TimelineItemType.TASK_COMPLETED, "Ship the report"),
            timelineItem("t2", TimelineItemType.EXPENSE, "Groceries")
        )
        val graph = DiaryConnections.build(listOf(entry()), items)

        val taskNode = graph.nodes.first { it.type == DiaryNodeType.TASK }
        val expenseNode = graph.nodes.first { it.type == DiaryNodeType.EXPENSE }
        assertEquals("detail for t1", taskNode.subtitle)
        assertTrue(graph.edges.any { it.from == "entry-e1" && it.to == taskNode.id && it.relation == "same day" })
        assertTrue(graph.edges.any { it.from == "entry-e1" && it.to == expenseNode.id })
    }

    @Test
    fun `timeline is capped to keep the radar legible`() {
        val items = (0 until 20).map { timelineItem("cap-$it", TimelineItemType.NOTE, "Item $it") }
        val graph = DiaryConnections.build(listOf(entry()), items)
        val sideNodes = graph.nodes.count {
            it.type !in setOf(DiaryNodeType.ENTRY, DiaryNodeType.MOOD, DiaryNodeType.KEYWORD)
        }
        assertTrue("expected capped side nodes but found $sideNodes", sideNodes <= 12)
        assertFalse(graph.isEmpty)
    }

    @Test
    fun `graph of a day with no entry but timeline renders a fallback`() {
        val items = listOf(timelineItem("t1", TimelineItemType.CAPTURE, "A photo"))
        val graph = DiaryConnections.build(emptyList(), items)
        // Nodes exist but no entry is present; the UI shows the friendly copy instead.
        assertFalse(graph.isEmpty)
        assertTrue(graph.nodes.none { it.type == DiaryNodeType.ENTRY })
        assertTrue(graph.edges.isEmpty())
    }
}