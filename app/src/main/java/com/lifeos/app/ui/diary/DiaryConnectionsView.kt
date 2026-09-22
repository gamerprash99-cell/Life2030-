package com.lifeos.app.ui.diary

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lifeos.app.core.intelligence.DiaryGraph
import com.lifeos.app.core.intelligence.DiaryNode
import com.lifeos.app.core.intelligence.DiaryNodeType
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryLavender
import com.lifeos.app.ui.theme.DiaryMoodCalm
import com.lifeos.app.ui.theme.DiaryMoodExcited
import com.lifeos.app.ui.theme.DiaryMoodHappy
import com.lifeos.app.ui.theme.DiaryMoodSad
import com.lifeos.app.ui.theme.DiaryMoodStressed
import com.lifeos.app.ui.theme.LifeOSPrimary

private const val MAX_CENTRAL = 3
private const val MAX_SIDE = 6
private val RADAR_HEIGHT = 260.dp

private data class RadarPos(val x: Dp, val y: Dp)

/**
 * "This day, connected" — the graph visual from the Diary redesign, rendered
 * with pure Compose Canvas. Nodes are chips floating above a hairline web of
 * straight edges. Every node is a real record: the day's diary entry at the
 * centre, its stored/detected mood and extracted keywords to the left, and the
 * same-day Notes/Tasks/Habits/Expenses/Captures (from `BuildTimelineUseCase`)
 * to the right. Nothing here is simulated.
 */
@Composable
fun DiaryConnectionsView(
    graph: DiaryGraph,
    dayLabel: String,
    modifier: Modifier = Modifier,
    emptyMessage: String = "No entry written on this day yet — connections appear once there's something to connect."
) {
    val hasEntry = graph.nodes.any { it.type == DiaryNodeType.ENTRY }
    if (graph.isEmpty || !hasEntry) {
        ColumnClearance(modifier) {
            Text(emptyMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val entries = graph.nodes.filter { it.type == DiaryNodeType.ENTRY }.take(MAX_CENTRAL)
    val left = graph.nodes.filter { it.type == DiaryNodeType.MOOD || it.type == DiaryNodeType.KEYWORD }.take(MAX_SIDE)
    val right = graph.nodes.filter { it.type !in nodesOnLeft && it.type != DiaryNodeType.ENTRY }.take(MAX_SIDE)

    BoxWithConstraints(modifier.fillMaxWidth().height(RADAR_HEIGHT)) {
        val density = LocalDensity.current
        val positions = remember(entries, left, right, maxWidth, maxHeight) {
            computeRadarLayout(entries, left, right, maxWidth, maxHeight)
        }

        Canvas(Modifier.fillMaxSize()) {
            val edgeColor = LifeOSPrimary.copy(alpha = 0.14f)
            graph.edges.forEach { edge ->
                val a = positions[edge.from] ?: return@forEach
                val b = positions[edge.to] ?: return@forEach
                drawLine(
                    color = edgeColor,
                    start = with(density) { Offset(a.x.toPx(), a.y.toPx()) },
                    end = with(density) { Offset(b.x.toPx(), b.y.toPx()) },
                    strokeWidth = with(density) { 1.2.dp.toPx() },
                    cap = StrokeCap.Round
                )
            }
        }

        entries.forEach { node -> positions[node.id]?.let { pos -> RadarNodeChip(node, pos, isEntry = true) } }
        left.forEach { node -> positions[node.id]?.let { pos -> RadarNodeChip(node, pos, isEntry = false) } }
        right.forEach { node -> positions[node.id]?.let { pos -> RadarNodeChip(node, pos, isEntry = false) } }
    }

    Text(
        "Center: the entry · sides: mood, themes and what else happened that day · $dayLabel",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.padding(top = 6.dp)
    )
}

private val nodesOnLeft = setOf(DiaryNodeType.MOOD, DiaryNodeType.KEYWORD)

private fun computeRadarLayout(
    entries: List<DiaryNode>,
    left: List<DiaryNode>,
    right: List<DiaryNode>,
    width: Dp,
    height: Dp
): Map<String, RadarPos> {
    val out = mutableMapOf<String, RadarPos>()
    fun column(nodes: List<DiaryNode>, x: Dp) {
        nodes.forEachIndexed { index, node ->
            out[node.id] = RadarPos(
                x = x,
                y = height * (index + 1) / (nodes.size + 1)
            )
        }
    }
    column(entries, width / 2f)
    column(left, width * 0.16f)
    column(right, width * 0.84f)
    return out
}

@Composable
private fun RadarNodeChip(node: DiaryNode, pos: RadarPos, isEntry: Boolean) {
    val dotColor = nodeDotColor(node)
    val chipWidth = 96.dp
    Box(
        modifier = Modifier
            .width(chipWidth)
            .offset(x = pos.x - chipWidth / 2, y = pos.y - 12.dp),
        contentAlignment = Alignment.Center
    ) {
        val shape = RoundedCornerShape(50)
        val container = if (isEntry) DiaryInkViolet else MaterialTheme.colorScheme.surface
        val border = if (isEntry) DiaryInkViolet else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, border, shape),
            color = container,
            shape = shape
        ) {
            CompositionLocalProvider(
                LocalContentColor provides if (isEntry) Color.White else MaterialTheme.colorScheme.onSurface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isEntry) 9.dp else 7.dp)
                            .background(dotColor, shape = RoundedCornerShape(50))
                    )
                    Text(
                        text = if (isEntry) node.shortLabel else node.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isEntry) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnClearance(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxWidth().height(RADAR_HEIGHT).background(Color.Transparent), contentAlignment = Alignment.Center) {
        content()
    }
}

private fun nodeDotColor(node: DiaryNode): Color = when (node.type) {
    DiaryNodeType.ENTRY -> Color(0xFFFFDDAE)
    DiaryNodeType.MOOD -> moodColorForLabel(node.label)
    DiaryNodeType.KEYWORD -> DiaryLavender
    DiaryNodeType.NOTE -> LifeOSPrimary
    DiaryNodeType.TASK -> DiaryMoodCalm
    DiaryNodeType.HABIT -> DiaryMoodHappy
    DiaryNodeType.EXPENSE -> DiaryMoodStressed
    DiaryNodeType.CAPTURE -> DiaryMoodExcited
}

/** Stored pills use their hue; analyzer moods (e.g. VER Positive) map onto the nearest pill. */
private fun moodColorForLabel(label: String): Color {
    DiaryMoods.colorOf(label).let { if (it != DiaryLavender) return it }
    return runCatching { DiaryMoods.fromAnalysis(com.lifeos.app.core.intelligence.Mood.valueOf(label)) }
        .getOrNull()
        ?.let { DiaryMoods.colorOf(it.key) }
        ?: DiaryLavender
}