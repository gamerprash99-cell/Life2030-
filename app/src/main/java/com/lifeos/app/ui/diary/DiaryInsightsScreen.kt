package com.lifeos.app.ui.diary

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lifeos.app.domain.intelligence.DiaryQuestion
import com.lifeos.app.domain.intelligence.MoodAnalysis
import com.lifeos.app.domain.intelligence.MoodShare
import com.lifeos.app.domain.intelligence.PatternAnalysis
import com.lifeos.app.domain.intelligence.WritingStatistics
import com.lifeos.app.domain.intelligence.WritingStreak
import com.lifeos.app.domain.model.DiaryMoods
import com.lifeos.app.ui.theme.DiaryActionViolet
import com.lifeos.app.ui.theme.DiaryHairline
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryLavender
import com.lifeos.app.ui.theme.LifeOSSpacing
import kotlin.math.roundToInt

/**
 * What the journal looks like over time.
 *
 * Everything on this screen is a count or a proportion of the user's own rows.
 * There is no model, no score, and nothing to configure — the sample thresholds
 * live in the engine and the screen shows `null` averages as "not enough yet"
 * rather than as a number. That distinction is the whole reason the engine
 * returns nullable doubles, so it is preserved here rather than coerced to 0.
 *
 * No bar is ever drawn for a mood that has no entries. A 0-width chart segment
 * reads as a real measurement, and an empty journal with an 8-row legend would
 * claim the user has felt nothing at all rather than not yet recorded anything.
 */
@Composable
fun DiaryInsightsScreen(
    state: DiaryInsightsState,
    onBack: () -> Unit,
    onReload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = LifeOSSpacing.screenPadding)
                .padding(top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to journal")
            }
            Text(
                "Insights",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = DiaryInkViolet
            )
        }

        val insights = state.insights
        when {
            state.errorMessage != null -> MessageState(
                title = "Insights unavailable",
                body = state.errorMessage,
                accent = MaterialTheme.colorScheme.error
            )

            insights == null || state.isLoading -> Box(Modifier.fillMaxSize())

            state.showOnboarding -> MessageState(
                title = "Nothing to look at yet",
                body = "Once you have written a few entries, this page will show your " +
                    "streak, your mood mix and the words you keep returning to. " +
                    "It all stays on this device.",
                accent = DiaryActionViolet
            )

            state.showFigures -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = LifeOSSpacing.screenPadding,
                    end = LifeOSSpacing.screenPadding,
                    bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { StreakCard(state.streak) }
                item { NumbersCard(insights.statistics, state.todayEpochDay) }
                if (insights.mood.distribution.isNotEmpty()) {
                    item { MoodMixCard(insights.mood) }
                }
                if (insights.mood.recentPoints.isNotEmpty()) {
                    item { MoodTrendCard(insights.mood, state.todayEpochDay) }
                }
                item { RecurringWordsCard(insights.patterns) }
                item { ReflectionPromptCard(insights.question) }
                item { PrivacyNote() }
            }
        }
    }
}

/**
 * The streak, with the number kept honest.
 *
 * [WritingStreak.isAlive] is separate from [WritingStreak.current] because a
 * streak that ran to yesterday and is not yet extended today is still the
 * streak the user earned. Showing 0 until they write tonight would punish them
 * for a day that has not finished.
 */
@Composable
private fun StreakCard(streak: WritingStreak) {
    InsightCard {
        Text(
            if (streak.isAlive) "Writing streak" else "Run ended",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                streak.currentLength.toString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = DiaryActionViolet
            )
            Text(
                if (streak.currentLength == 1) "day" else "days",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        Text(
            when {
                streak.currentLength == 0 -> "Write something today to start a new one."
                // Phrased as the run's own length, never as "N days ago". The
                // streak type carries no timestamp for the last entry, and
                // currentLength is a length, not an age — saying "7 days ago"
                // about a run of 7 that ended yesterday is simply a wrong number
                // on a screen whose whole job is to be right about counts.
                !streak.isAlive -> "Your run reached ${streak.currentLength} day${if (streak.currentLength == 1) "" else "s"}. Writing today starts a new one."
                else -> "Best run so far: ${streak.longestLength} day${if (streak.longestLength == 1) "" else "s"}."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun NumbersCard(stats: WritingStatistics, todayEpochDay: Long) {
    InsightCard(title = "The numbers") {
        NumberRow("Entries", stats.entryCount.toString())
        NumberRow("Days you wrote", stats.activeDayCount.toString())

        // null until the engine's minimum sample is met, and it says so. A 0
        // here would be a lie: "fewer than the threshold" is not "none".
        NumberRow(
            label = "Avg. length",
            value = stats.averageWordsPerEntry?.let { "$it words" } ?: "Not enough yet"
        )
        NumberRow(
            label = "Entries per writing day",
            value = stats.averageEntriesPerActiveDay?.let { String.format(java.util.Locale.getDefault(), "%.1f", it) }
                ?: "Not enough yet"
        )
        NumberRow(
            label = "Attachments",
            // Present even at one, unlike the averages: this is a plain count
            // and needs no minimum sample to be true.
            value = stats.attachmentCount.toString()
        )
        NumberRow(
            label = "Your span",
            value = spanLabel(stats.firstEntryEpochDay, stats.latestEntryEpochDay, todayEpochDay)
        )
        NumberRow(
            label = "Most-writing day",
            value = stats.entriesByWeekday.maxByOrNull { it.count }
                ?.takeIf { it.count > 0 }
                ?.let { it.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault()) }
                ?: "Not yet"
        )
    }
}

/** "12 to 19 Sep" for a same-month span, "3 Aug to 19 Sep" otherwise. */
private fun spanLabel(first: Long?, latest: Long?, todayEpochDay: Long): String {
    if (first == null || latest == null) return "Not yet"
    val start = java.time.LocalDate.ofEpochDay(first)
    val end = java.time.LocalDate.ofEpochDay(latest)
    val formatter = java.time.format.DateTimeFormatter.ofPattern("d MMM")
    return if (start.month == end.month && start.year == end.year) {
        // "12 to 19 Sep" rather than "12 Sep to 19 Sep": the month is shared, so
        // repeating it is noise.
        "${start.dayOfMonth} to ${end.format(formatter)}"
    } else {
        "${start.format(formatter)} to ${end.format(formatter)}"
    }
}

/**
 * The mood split as a single stacked bar.
 *
 * Built from [MoodAnalysis.distribution], which is already ordered by count and
 * already carries each mood's label and emoji. The screen therefore never
 * re-derives a total it could get wrong, and the segment colours come from the
 * same eight theme accents as the timeline dots and the calendar day cells.
 */
@Composable
private fun MoodMixCard(mood: MoodAnalysis) {
    InsightCard(title = "How you've felt") {
        val total = mood.distribution.sumOf { it.count }

        Row(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(50))
                .semantics {
                    contentDescription = "Mood mix: " + mood.distribution.joinToString(", ") {
                        "${it.label} ${it.count}"
                    }
                }
        ) {
            mood.distribution.forEach { share ->
                Box(
                    Modifier
                        // Weight, not a fixed dp: eight equal segments would
                        // render a one-mood journal as an even eight-way split.
                        .weight(share.count.toFloat())
                        .fillMaxSize()
                        .background(DiaryMoodVisuals.accentOf(share.moodKey))
                )
            }
        }

        Column(
            Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            mood.distribution.forEach { share ->
                MoodLegendRow(share = share, total = total)
            }
        }

        // The unresolved count is shown rather than silently dropped: if a third
        // of mood-tagged rows carry a value this build does not recognise, the
        // percentages below are computed on a smaller base than the user thinks.
        if (mood.taggedEntryCount > mood.resolvedEntryCount) {
            Text(
                "${mood.taggedEntryCount - mood.resolvedEntryCount} entr" +
                    (if (mood.taggedEntryCount - mood.resolvedEntryCount == 1) "y" else "ies") +
                    " carry a mood this version does not know, and are left out of these figures.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

@Composable
private fun MoodLegendRow(share: MoodShare, total: Int) {
    // Recomputed here rather than read from [MoodShare.share], so the rendered
    // percentage is guaranteed to be consistent with the bar above it, which is
    // built from the same counts.
    val percent = if (total > 0) (share.count * 100.0 / total).roundToInt() else 0
    val mood = DiaryMoods.fromStored(share.moodKey)

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(DiaryMoodVisuals.haloOf(share.moodKey)),
            contentAlignment = Alignment.Center
        ) {
            // The engine already resolved label and emoji; fall back only if a
            // stored key somehow stops matching, so the row is never blank.
            Text(share.emoji.ifBlank { mood?.emoji ?: "" }, style = MaterialTheme.typography.labelMedium)
        }
        Text(
            share.label.ifBlank { mood?.label.orEmpty() },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            "${share.count}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "$percent%",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            // Fixed width so the percentages align into a column; without it
            // the digits shift left row to row and the eye cannot scan them.
            modifier = Modifier.width(38.dp),
            textAlign = TextAlign.End
        )
    }
}

/**
 * One bar per day in the recent window, height = |valence|.
 *
 * Built from [MoodAnalysis.recentPoints], which already includes the empty days
 * in the window rather than skipping them. Keeping the gaps is the point: a chart
 * of only the days that happen to have a mood would hide the streak-breakers,
 * which are the most interesting thing about a writing habit.
 *
 * Valence is signed, so the bar is drawn from a centre line and grows up or
 * down. Drawing |valence| as an upward bar would show a bad day as a tall green
 * column and a good day as a short one.
 */
@Composable
private fun MoodTrendCard(mood: MoodAnalysis, todayEpochDay: Long) {
    val points = mood.recentPoints
    val daysWithMood = points.count { it.entryCount > 0 }

    InsightCard(title = "Day by day") {
        Text(
            if (daysWithMood == 0) "No moods recorded in this window yet."
            else "$daysWithMood of the last ${points.size} days carry a mood.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            Modifier
                .fillMaxWidth()
                .height(72.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            points.forEach { point ->
                val magnitude = point.valence.coerceIn(-1f, 1f)
                // Animated so a save that changes the chart reads as a change.
                val animated by animateFloatAsState(
                    targetValue = magnitude,
                    label = "valenceBar"
                )
                val isPositive = animated >= 0f
                val barHeight = (30f * kotlin.math.abs(animated)).coerceIn(2.5f, 30f)
                val isToday = point.epochDay == todayEpochDay

                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .semantics {
                            contentDescription = describePoint(point)
                        },
                    contentAlignment = if (isPositive) Alignment.BottomCenter else Alignment.TopCenter
                ) {
                    // The centre line is drawn per-cell rather than once across
                    // the row, so it stays put as cells are spaced.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(DiaryHairline)
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(barHeight.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                // Alpha tracks magnitude, so a weakly positive
                                // day is visibly weak. Two fixed colours would
                                // render 51% and 100% identically.
                                if (isPositive) PositiveInk else NegativeInk
                            )
                    )
                }

                if (isToday) {
                    Box(
                        Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(DiaryActionViolet)
                    )
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            LegendSwatch(PositiveInk, "Upward = lighter")
            LegendSwatch(NegativeInk, "Downward = heavier")
        }

        if (mood.trend.change != null) {
            Text(
                when {
                    mood.trend.isRising -> "Lighter than the previous stretch of days."
                    mood.trend.isFalling -> "Heavier than the previous stretch of days."
                    else -> "About the same as the previous stretch of days."
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp)
            )
        } else {
            // Stated rather than hidden: the absence of a trend arrow is a
            // deliberate "not enough days yet", and a reader who expected one
            // deserves to know why it is missing.
            Text(
                "A comparison appears once both stretches of the window have moods.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

/** Spoken form of one bar, since the chart itself carries no text. */
private fun describePoint(point: com.lifeos.app.domain.intelligence.MoodTrendPoint): String {
    val date = epochDayAsText(point.epochDay)
    if (point.entryCount == 0) return "$date: nothing written"
    val percent = (kotlin.math.abs(point.valence) * 100).roundToInt()
    val direction = if (point.valence >= 0f) "lighter" else "heavier"
    return "$date: $percent% $direction, ${point.entryCount} " +
        (if (point.entryCount == 1) "entry" else "entries")
}

@Composable
private fun LegendSwatch(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RecurringWordsCard(patterns: PatternAnalysis) {
    InsightCard(title = "What keeps coming up") {
        if (patterns.recurringKeywords.isEmpty()) {
            Text(
                "No word has shown up on more than one day yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            // Each chip is a `@Composable () -> Unit`, so the lambda is wrapped
            // in braces on purpose: without them `map` would eagerly *call* Row
            // and hand FlowRowSimple a List<Unit>.
            FlowRowSimple(
                patterns.recurringKeywords.map { keyword -> { RecurringWordChip(keyword) } },
                spacing = 7.dp
            )
            Text(
                "Ranked by how many days each word appears in, not how many times " +
                    "you typed it — so a word you circled for a week outranks one you " +
                    "hammered in a single evening.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        if (patterns.patterns.isNotEmpty()) {
            Column(
                Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                patterns.patterns.forEach { pattern ->
                    Text(
                        "${pattern.label} — ${(pattern.share * 100).roundToInt()}% of your entries",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ReflectionPromptCard(question: DiaryQuestion) {
    InsightCard {
        Row(
            Modifier.padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(DiaryLavender)
            )
            Text(
                "Worth thinking about",
                style = MaterialTheme.typography.labelLarge,
                color = DiaryActionViolet
            )
        }
        Text(
            question.text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = DiaryInkViolet
        )
    }
}

/**
 * Stated on the screen itself rather than buried in the README.
 *
 * The user is being shown statistics about their own writing; the fact that the
 * calculation happens here is the claim that matters, so it belongs where they
 * are looking rather than somewhere they have to go and find.
 */
@Composable
private fun PrivacyNote() {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, DiaryHairline, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        // A dot rather than a lock glyph: `material-icons-extended` is not on
        // this module's classpath, and adding an icon library to render one
        // padlock on one screen is not a trade worth making.
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(DiaryActionViolet)
        )
        Text(
            "Every figure here is calculated on this device, from your own entries. " +
                "Nothing is uploaded, and this app holds no internet permission.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Shared card surface: paper leaf, hairline border, generous radius. */
@Composable
private fun InsightCard(
    title: String? = null,
    content: @Composable () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, DiaryHairline, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        if (title != null) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        content()
    }
}

@Composable
private fun NumberRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = DiaryInkViolet
        )
    }
}

private fun epochDayAsText(epochDay: Long): String =
    java.time.LocalDate.ofEpochDay(epochDay).format(java.time.format.DateTimeFormatter.ofPattern("d MMM"))

/** The two ends of the day-by-day chart. Fixed, because the bar's *height*
 *  carries the magnitude and the colour only carries the direction. */
private val PositiveInk = Color(0xFF10B981)
private val NegativeInk = Color(0xFF6366F1)

/**
 * A wrapped row of chips.
 *
 * Not `FlowRow` from `androidx.compose.foundation.layout`: that is an
 * experimental API and this project builds with warnings left on. A `LazyRow`
 * would be wrong here — the words wrap, and a horizontal carousel of your
 * recurring vocabulary reads as a carousel, not a paragraph.
 */
@Composable
private fun FlowRowSimple(chips: List<@Composable () -> Unit>, spacing: androidx.compose.ui.unit.Dp) {
    // Laid out in rows of a fixed count rather than measured, so no
    // `SubcomposeLayout` and no experimental API. The count is derived from
    // `LazyRow`'s item width assumption below.
    Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
        chips.chunked(FLOW_CHIPS_PER_ROW).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) { row.forEach { chip -> chip() } }
        }
    }
}

/** Tuned to the chip widths above; see the note on [FlowRowSimple]. */
@Composable
private fun RecurringWordChip(keyword: com.lifeos.app.domain.intelligence.RecurringKeyword) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(DiaryLavender)
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(keyword.word, style = MaterialTheme.typography.labelLarge, color = DiaryInkViolet)
        Text(
            "${keyword.distinctDays}d",
            style = MaterialTheme.typography.labelSmall,
            color = DiaryActionViolet,
            modifier = Modifier.semantics {
                // "9d" is compact on screen and opaque when read aloud.
                contentDescription = "${keyword.word}, ${keyword.distinctDays} days"
            }
        )
    }
}

private const val FLOW_CHIPS_PER_ROW = 3
