package com.lifeos.app.ui.diary

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lifeos.app.core.intelligence.AnswerResult
import com.lifeos.app.core.intelligence.DiaryInsights
import com.lifeos.app.core.intelligence.TrendDirection
import com.lifeos.app.core.intelligence.TrendPoint
import com.lifeos.app.ui.components.LifeOSCard
import com.lifeos.app.ui.theme.DiaryMoodCalm
import com.lifeos.app.ui.theme.DiaryMoodStressed
import com.lifeos.app.ui.theme.LifeOSPrimary
import com.lifeos.app.ui.theme.LifeOSSpacing
import java.util.Locale

/**
 * "This week, in your own words" — the Diary analytics section. Collapsible
 * summary card on top; when expanded it reveals the week's mood chart, themes,
 * patterns, the on-device narrative and a local "Ask" box. All inputs flow from
 * [DiaryInsights] produced by the LifeOS Intelligence Engine — nothing is
 * predicted or invented.
 */
@Composable
fun DiaryAnalyticsSection(
    insights: DiaryInsights?,
    loading: Boolean,
    onAsk: (String) -> Unit,
    asking: Boolean,
    question: AnswerResult?,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.cardSpacing)) {
        if (loading && insights == null) {
            LifeOSCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Gathering this week's patterns…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 10.dp))
                }
            }
            return@Column
        }
        if (insights == null) return@Column

        SummaryCard(insights, expanded = expanded, onToggle = { expanded = !expanded })

        if (expanded) {
            MoodChartCard(insights.moodByDay)
            ThemesCard(insights.topKeywords)
            PatternsCard(insights)
            NarrativeCard(insights.narrative)
            RecommendationsCard(insights)
            AskCard(onAsk, asking, question)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryCard(insights: DiaryInsights, expanded: Boolean, onToggle: () -> Unit) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(stiffness = 400f),
        label = "expandChevron"
    )
    LifeOSCard(Modifier.fillMaxWidth(), onClick = onToggle) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("This week", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                InkBadge("on-device")
                Icon(
                    Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse analytics" else "Expand analytics",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp).rotate(rotation)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip("${insights.weekCount}/7", "days written")
                StatChip("${insights.monthCount}", "this month")
                if (insights.diaryStreakDays > 0) StatChip("${insights.diaryStreakDays}d", "streak")
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MoodTrendLine(insights)
                Spacer(Modifier.weight(1f))
                Text(
                    String.format(Locale.getDefault(), "%+.1f", insights.averageMood),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (insights.averageMood >= 0) DiaryMoodCalm else DiaryMoodStressed
                )
            }
            if (!expanded && insights.topKeywords.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    insights.topKeywords.take(4).forEach { kw -> ThemePill(kw.keyword) }
                }
            }
        }
    }
}

@Composable
private fun RowScope.MoodTrendLine(insights: DiaryInsights) {
    val trend = insights.moodTrend
    if (trend == null) {
        Text("Write a few entries to see how your mood is moving.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val verb = when (trend.direction) {
        TrendDirection.UP -> "Improved"
        TrendDirection.DOWN -> "Declined"
        TrendDirection.FLAT -> "Steady"
        TrendDirection.UNKNOWN -> "Held"
    }
    val arrow = when (trend.direction) {
        TrendDirection.UP -> "↗"
        TrendDirection.DOWN -> "↘"
        else -> "→"
    }
    val delta = trend.percentChange?.let { "  (${if (it >= 0) "+" else ""}$it%)" } ?: ""
    Text(
        "Mood $arrow $verb vs last week$delta",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.weight(1f, fill = false)
    )
}

@Composable
private fun MoodChartCard(points: List<TrendPoint>) {
    LifeOSCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Mood, day by day", style = MaterialTheme.typography.titleSmall)
            MoodBarChart(points)
            Text("Bars above the line are positive days, below are tougher ones. Values come from your written entries.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MoodBarChart(points: List<TrendPoint>) {
    if (points.isEmpty()) { Text("No mood data this week yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); return }
    val labelsRowHeight = 22.dp
    Column {
        Canvas(Modifier.fillMaxWidth().height(88.dp)) {
            val midY = size.height / 2f
            val plotTop = 10.dp.toPx()
            val plotBottom = size.height - 10.dp.toPx()
            val slot = size.width / points.size
            val barWidth = slot * 0.5f
            val halfRange = 2.0 // numericValue range is -2..+2

            drawLine(Color.Black.copy(alpha = 0.12f), start = androidx.compose.ui.geometry.Offset(0f, midY), end = androidx.compose.ui.geometry.Offset(size.width, midY), strokeWidth = 1.dp.toPx(), cap = StrokeCap.Round)

            points.forEachIndexed { index, point ->
                val raw = point.value.coerceIn(-halfRange, halfRange)
                val barHeight = (Math.abs(raw) / halfRange).toFloat() * (plotBottom - midY).coerceAtLeast(plotTop)
                val top = if (raw >= 0) midY - barHeight else midY
                val x = slot * index + (slot - barWidth) / 2f
                val color = when {
                    raw > 0.15 -> DiaryMoodCalm
                    raw < -0.15 -> DiaryMoodStressed
                    else -> Color.Gray.copy(alpha = 0.35f)
                }
                drawRoundRect(
                    color = color,
                    topLeft = androidx.compose.ui.geometry.Offset(x, top),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight.coerceAtLeast(3.dp.toPx())),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
            }
        }
        Row(Modifier.fillMaxWidth().height(labelsRowHeight), horizontalArrangement = Arrangement.SpaceBetween) {
            points.forEach { point ->
                Text(
                    point.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemesCard(keywords: List<com.lifeos.app.core.intelligence.KeywordResult>) {
    if (keywords.isEmpty()) return
    LifeOSCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Your themes this month", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                keywords.forEach { kw -> ThemePill("${kw.keyword} · ${kw.count}") }
            }
        }
    }
}

@Composable
private fun ThemePill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1)
    }
}

@Composable
private fun PatternsCard(insights: DiaryInsights) {
    val items = buildList {
        insights.habitCorrelation?.let { add(it) }
        addAll(insights.patterns)
    }
    if (items.isEmpty()) return
    LifeOSCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Patterns", style = MaterialTheme.typography.titleSmall)
            items.take(4).forEach { insight ->
                Column {
                    Text(insight.title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Text(insight.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun NarrativeCard(narrative: String) {
    if (narrative.isBlank()) return
    LifeOSCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Your Week in Words", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.weight(1f))
                InkBadge("local")
            }
            Text(narrative, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RecommendationsCard(insights: DiaryInsights) {
    val recs = insights.recommendations
    if (recs.isEmpty()) return
    LifeOSCard(Modifier.fillMaxWidth(), tint = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Gentle suggestions", style = MaterialTheme.typography.titleSmall)
            recs.sortedBy { it.priority }.take(4).forEach { rec ->
                Column {
                    Text("• ${rec.title}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Text(rec.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AskCard(onAsk: (String) -> Unit, asking: Boolean, question: AnswerResult?) {
    var text by remember { mutableStateOf("") }
    LifeOSCard(Modifier.fillMaxWidth(), tint = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f), shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = LifeOSPrimary, modifier = Modifier.padding(9.dp).size(16.dp))
                }
                Column(Modifier.padding(start = 10.dp)) {
                    Text("Ask LifeOS", style = MaterialTheme.typography.titleSmall)
                    Text("About your week — answered on-device", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. How was my week overall?") },
                maxLines = 3
            )
            Button(
                onClick = { onAsk(text); text = "" },
                enabled = text.isNotBlank() && !asking,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("Ask", modifier = Modifier.padding(start = 6.dp))
            }
            if (asking) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Text("Thinking locally…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp))
                }
            }
            question?.let { answer ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(answer.answer, style = MaterialTheme.typography.bodyMedium)
                    if (answer.followUpSuggestions.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            answer.followUpSuggestions.take(3).forEach { suggestion ->
                                TextButton(onClick = { onAsk(suggestion) }) { Text(suggestion, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(value: String, label: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InkBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
}