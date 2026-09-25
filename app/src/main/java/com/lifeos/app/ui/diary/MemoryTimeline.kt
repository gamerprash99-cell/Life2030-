package com.lifeos.app.ui.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.ui.theme.DiaryHairline
import com.lifeos.app.ui.theme.LifeOSSpacing

/**
 * One memory moment. Deliberately not a card: no surface, no border, no
 * shadow. Hierarchy comes from the time gutter, the spine, and the generous
 * line height on the journal text itself — the loudest thing on the screen by
 * design.
 */
@Composable
fun MemoryMoment(
    entry: DiaryEntity,
    isFirst: Boolean,
    isLast: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasMood = !entry.mood.isNullOrBlank()
    val accent = DiaryMoods.colorOf(entry.mood)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        // Fixed-width time gutter so the spine and text stay aligned down the
        // page no matter how wide an individual time string renders.
        Box(Modifier.width(58.dp), contentAlignment = Alignment.TopEnd) {
            Text(
                DateTimeUtils.formatMinutes(entry.timeMinutes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp, end = 12.dp)
            )
        }

        MemorySpine(moodKey = entry.mood.takeIf { hasMood }, isFirst = isFirst, isLast = isLast)

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp, bottom = LifeOSSpacing.sectionSpacing)
        ) {
            if (hasMood) {
                Text(
                    DiaryMoods.displayLabel(entry.mood).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp
                )
                Spacer(Modifier.height(6.dp))
            }

            Text(
                entry.content,
                style = MaterialTheme.typography.bodyLarge,
                // Taller leading than the rest of the app so long entries stay
                // pleasant to read.
                lineHeight = 26.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            val tags = remember(entry.id, entry.tagsCsv) { entryTags(entry) }
            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                // Tags as quiet inline text, not chips: they annotate the
                // memory, they should not compete with it.
                Text(
                    tags.joinToString("  ·  "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                MemoryAction(label = "Edit", onClick = onEdit)
                MemoryAction(label = "Delete", onClick = onDelete, isDestructive = true)
            }
        }
    }
}

/** A quiet text action, 48dp tall so it is reachable without the row growing. */
@Composable
private fun MemoryAction(label: String, onClick: () -> Unit, isDestructive: Boolean = false) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.heightIn(min = 48.dp),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isDestructive) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * The vertical relationship between moments: a 1dp rule with the mood resting
 * on it as a small ring. Entries without a mood get a hollow dot so "no mood"
 * reads as absent rather than as a ninth feeling. First and last rows taper so
 * the rule reads as a continuation instead of a border.
 */
@Composable
private fun MemorySpine(moodKey: String?, isFirst: Boolean, isLast: Boolean) {
    Column(
        modifier = Modifier.width(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .width(1.dp)
                .height(if (isFirst) 0.dp else 10.dp)
                .background(DiaryHairline)
        )
        if (moodKey != null) {
            MoodDot(moodKey = moodKey, diameter = 12)
        } else {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(DiaryHairline)
            )
        }
        Box(
            Modifier
                .width(1.dp)
                .height(if (isLast) 8.dp else 18.dp)
                .background(DiaryHairline)
        )
    }
}

private fun entryTags(entry: DiaryEntity): List<String> = entry.tagsCsv
    .split(',')
    .map { it.trim() }
    .filter { it.isNotBlank() }
    .take(2)
