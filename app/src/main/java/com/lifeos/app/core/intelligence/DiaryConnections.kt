package com.lifeos.app.core.intelligence

import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.domain.model.TimelineItem
import com.lifeos.app.domain.model.TimelineItemType

/**
 * A relationship graph derived from real, persisted Diary days.
 *
 * This is the aesthetic that replaces a generic "graph visualization" with an
 * honest, physically derivable structure: the diary entry sits at the centre
 * and its stored/detected mood, extracted keywords, and the same-day Notes,
 * Tasks, Habits, Expenses and Captures (via [com.lifeos.app.domain.usecase.BuildTimelineUseCase])
 * radiate around it. Nothing is invented — every node maps to a stored row and
 * every edge maps to a date or content relationship.
 */
data class DiaryNode(
    val id: String,
    val label: String,
    val type: DiaryNodeType,
    val subtitle: String? = null
) {
    val shortLabel: String
        get() = if (label.length > 22) label.take(19).trimEnd() + "…" else label
}

enum class DiaryNodeType { ENTRY, MOOD, KEYWORD, NOTE, TASK, HABIT, EXPENSE, CAPTURE }

data class DiaryEdge(val from: String, val to: String, val relation: String)

data class DiaryGraph(val nodes: List<DiaryNode>, val edges: List<DiaryEdge>) {
    val isEmpty: Boolean get() = nodes.isEmpty()
}

/**
 * Pure builder — no repositories, no suspension. Feed it one day's diary
 * entries and that day's timeline and get back the graph. The keyword/
 * mood analyzers it uses are the same deterministic, offline analyzers the
 * rest of LifeOS uses (MoodAnalyzer, KeywordExtractor).
 */
object DiaryConnections {

    private val MAX_KEYWORDS_PER_ENTRY = 2
    private val MAX_TIMELINE_PER_TYPE = 4
    private val MAX_TIMELINE_NODES = 12

    fun build(entries: List<DiaryEntity>, timeline: List<TimelineItem>): DiaryGraph {
        if (entries.isEmpty() && timeline.isEmpty()) return DiaryGraph(emptyList(), emptyList())

        val nodes = mutableListOf<DiaryNode>()
        val edges = mutableListOf<DiaryEdge>()
        val seenMood = mutableSetOf<String>()
        val seenKeyword = mutableSetOf<String>()

        val entryNodes = entries.map { entry ->
            val displayedTitle = entry.title?.takeIf { it.isNotBlank() }
                ?: entryPreview(entry)
            DiaryNode(
                id = "entry-${entry.id}",
                label = displayedTitle,
                type = DiaryNodeType.ENTRY,
                subtitle = entry.mood ?: "Diary entry"
            )
        }
        nodes += entryNodes

        entries.forEach { entry ->
            val entryId = "entry-${entry.id}"
            val moodKey = entry.mood ?: MoodAnalyzer.analyze(entry.content).mood.name
            if (seenMood.add(moodKey)) {
                nodes += DiaryNode("mood-$moodKey", moodKey, DiaryNodeType.MOOD, "mood")
            }
            edges += DiaryEdge(entryId, "mood-$moodKey", "mood")

            KeywordExtractor.extract(entry.content, maxKeywords = MAX_KEYWORDS_PER_ENTRY).forEach { result ->
                val keyword = result.keyword
                if (seenKeyword.add(keyword)) {
                    nodes += DiaryNode("kw-$keyword", keyword, DiaryNodeType.KEYWORD, "theme")
                }
                edges += DiaryEdge(entryId, "kw-$keyword", "mentions")
            }
        }

        val timelineNodes = timeline
            .take(MAX_TIMELINE_NODES)
            .groupBy { it.type }
            .flatMap { (type, items) -> items.take(MAX_TIMELINE_PER_TYPE) }
            .map { item ->
                DiaryNode(
                    id = "t-${item.id}",
                    label = item.title,
                    type = item.type.toDiaryNodeType(),
                    subtitle = item.subtitle
                )
            }
        nodes += timelineNodes

        timelineNodes.forEach { node ->
            entryNodes.forEach { entryNode ->
                edges += DiaryEdge(entryNode.id, node.id, "same day")
            }
        }

        return DiaryGraph(sortNodes(nodes), sortEdges(edges))
    }

    private fun entryPreview(entry: DiaryEntity): String {
        val text = entry.content.trim()
        if (text.isEmpty()) return "Diary entry"
        val compact = text.replace(Regex("\\s+"), " ")
        return if (compact.length <= 26) compact else compact.take(24).trimEnd() + "…"
    }

    private fun TimelineItemType.toDiaryNodeType(): DiaryNodeType = when (this) {
        TimelineItemType.NOTE -> DiaryNodeType.NOTE
        TimelineItemType.TASK_COMPLETED -> DiaryNodeType.TASK
        TimelineItemType.HABIT_COMPLETED -> DiaryNodeType.HABIT
        TimelineItemType.EXPENSE -> DiaryNodeType.EXPENSE
        TimelineItemType.CAPTURE -> DiaryNodeType.CAPTURE
        TimelineItemType.DIARY -> DiaryNodeType.ENTRY
    }

    private fun sortNodes(nodes: List<DiaryNode>): List<DiaryNode> = nodes.sortedBy { it.id }
    private fun sortEdges(edges: List<DiaryEdge>): List<DiaryEdge> =
        edges.sortedWith(compareBy({ it.from }, { it.to }, { it.relation }))
}