package com.lifeos.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lifeos.app.core.media.formatAudioDuration
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.domain.model.TimelineItem
import com.lifeos.app.domain.model.TimelineItemType
import com.lifeos.app.ui.theme.LifeOSRadius
import com.lifeos.app.ui.theme.LifeOSSpacing
import com.lifeos.app.ui.theme.LocalLifeOSSemanticColors
import java.io.File

/**
 * One memory on the Life timeline: a small category badge on the spine, the
 * entry card beside it, and the real time the thing happened.
 *
 * The row shows what the record actually contains — a photo thumbnail, the
 * measured length of a recording, the amount of an expense, or a task's
 * completion state — and never invents a value for a type that has none.
 * Tapping it is the caller's job ([onClick] gets the item, and the router
 * decides where each type opens).
 *
 * [isFirst]/[isLast] stop the connector line at the first and last node of a
 * day, so a day's section reads as one segment rather than a line running off
 * both ends.
 */
@Composable
fun LifeOSTimelineItem(
    item: TimelineItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    showDate: Boolean = false
) {
    val accent = timelineAccent(item.type)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val border by animateColorAsState(
        targetValue = if (pressed) accent.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant,
        label = "timelineItemBorder"
    )

    Row(modifier.fillMaxWidth()) {
        TimelineSpine(
            icon = item.icon,
            accent = accent,
            isFirst = isFirst,
            isLast = isLast
        )
        Spacer(Modifier.width(LifeOSSpacing.xs))
        Surface(
            shape = RoundedCornerShape(LifeOSRadius.large),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, border),
            shadowElevation = if (pressed) 0.dp else 1.dp,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 3.dp)
                .clip(RoundedCornerShape(LifeOSRadius.large))
                .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = item.accessibilityLabel()
                }
        ) {
            Row(
                Modifier.padding(LifeOSSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.sm)
            ) {
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    TimelineItemEyebrow(item = item, accent = accent, showDate = showDate)
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    item.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TimelineTrailingBadges(item = item, accent = accent)
                }
                TimelineThumbnail(item = item, accent = accent)
            }
        }
    }
}

/** `09:14 · Expense` — the real time, then the module that produced the row. */
@Composable
private fun TimelineItemEyebrow(item: TimelineItem, accent: Color, showDate: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            buildString {
                if (showDate) {
                    append(DateTimeUtils.epochDayToLocalDate(item.dateEpochDay).dayOfMonth)
                    append(' ')
                }
                append(DateTimeUtils.formatMinutes(item.timeMinutes))
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(Modifier.size(3.dp).background(MaterialTheme.colorScheme.outline, CircleShape))
        Text(
            timelineTypeLabel(item.type),
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * The one extra fact a row can carry: the amount of an expense, the measured
 * length of a recording or video, or the completion state of a task/habit.
 */
@Composable
private fun TimelineTrailingBadges(item: TimelineItem, accent: Color) {
    val amount = item.amount
    val duration = item.durationMillis?.takeIf { it > 0 }
    if (amount == null && duration == null && item.statusLabel == null) return

    Row(
        Modifier.padding(top = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        amount?.let {
            TimelineBadge(
                text = com.lifeos.app.core.util.CurrencyFormatter.format(it),
                tint = LocalLifeOSSemanticColors.current.gold,
                emphasised = true
            )
        }
        duration?.let {
            TimelineBadge(text = formatAudioDuration(it), tint = accent)
        }
        item.statusLabel?.takeIf { amount == null }?.let {
            TimelineBadge(text = it, tint = LocalLifeOSSemanticColors.current.success)
        }
    }
}

/** Photo thumbnails render the picture; audio/video get a play glyph. */
@Composable
private fun TimelineThumbnail(item: TimelineItem, accent: Color) {
    val image = item.imagePath
    val media = item.mediaPath
    when {
        image != null && File(image).exists() -> AsyncImage(
            model = File(image),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(LifeOSRadius.medium))
        )

        media != null -> Box(
            Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(LifeOSRadius.medium))
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (item.type == TimelineItemType.AUDIO) "▶" else "▷",
                fontSize = 17.sp,
                color = accent
            )
        }
    }
}

@Composable
private fun TimelineBadge(text: String, tint: Color, emphasised: Boolean = false) {
    Surface(
        color = tint.copy(alpha = if (emphasised) 0.16f else 0.12f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = if (emphasised) MaterialTheme.colorScheme.onSurface else tint,
            fontWeight = if (emphasised) FontWeight.SemiBold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

/**
 * The spine: a hairline connector with the entry's category badge on it. The
 * line is drawn behind the badge, so a day reads as one continuous thread.
 */
@Composable
private fun TimelineSpine(
    icon: String,
    accent: Color,
    isFirst: Boolean,
    isLast: Boolean
) {
    val railColor = MaterialTheme.colorScheme.outlineVariant
    Box(
        Modifier
            .width(LifeOSSpacing.lg + 8.dp)
            .fillMaxSize()
    ) {
        val badgePadding = 10.dp
        val badgeSize = 30.dp
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    val x = size.width / 2f
                    val center = badgePadding.toPx() + badgeSize.toPx() / 2f
                    drawLine(
                        color = railColor,
                        strokeWidth = 2.dp.toPx(),
                        start = Offset(x, if (isFirst) center else 0f),
                        end = Offset(x, if (isLast) center else size.height)
                    )
                }
        )
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = CircleShape,
            border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
            modifier = Modifier
                .padding(top = badgePadding)
                .align(Alignment.TopCenter)
                .size(badgeSize)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(icon, fontSize = 13.sp)
            }
        }
    }
}

/** The colour a module owns on the timeline, in one place. */
@Composable
fun timelineAccent(type: TimelineItemType): Color = when (type) {
    TimelineItemType.TASK_COMPLETED -> LocalLifeOSSemanticColors.current.success
    TimelineItemType.HABIT_COMPLETED -> LocalLifeOSSemanticColors.current.warning
    TimelineItemType.EXPENSE -> LocalLifeOSSemanticColors.current.gold
    TimelineItemType.DIARY -> MaterialTheme.colorScheme.primary
    TimelineItemType.PHOTO, TimelineItemType.VIDEO -> LocalLifeOSSemanticColors.current.info
    TimelineItemType.AUDIO -> MaterialTheme.colorScheme.secondary
    TimelineItemType.SNAP -> MaterialTheme.colorScheme.primary
}

/** The human name of the module a row came from. */
fun timelineTypeLabel(type: TimelineItemType): String = when (type) {
    TimelineItemType.TASK_COMPLETED -> "Task"
    TimelineItemType.HABIT_COMPLETED -> "Habit"
    TimelineItemType.EXPENSE -> "Expense"
    TimelineItemType.DIARY -> "Diary"
    TimelineItemType.PHOTO -> "Photo"
    TimelineItemType.VIDEO -> "Video"
    TimelineItemType.AUDIO -> "Voice note"
    TimelineItemType.SNAP -> "Moment"
}

private fun TimelineItem.accessibilityLabel(): String = buildString {
    append(timelineTypeLabel(type))
    append(", ")
    append(title)
    append(" at ")
    append(DateTimeUtils.formatMinutes(timeMinutes))
    subtitle?.takeIf { it.isNotBlank() }?.let { append(", ").append(it) }
}

/**
 * A dated separator between two days of memories: the day's name, how many
 * entries it holds, and the weekday it fell on.
 */
@Composable
fun LifeOSDateHeader(
    label: String,
    epochDay: Long,
    entryCount: Int,
    modifier: Modifier = Modifier
) {
    val date = DateTimeUtils.epochDayToLocalDate(epochDay)
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = LifeOSSpacing.lg, bottom = LifeOSSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.sm)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                DateTimeUtils.formatDayOfWeek(date),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LifeOSPill(
            text = if (entryCount == 1) "1 entry" else "$entryCount entries",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}
