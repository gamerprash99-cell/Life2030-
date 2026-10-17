package com.lifeos.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.TaskEntity
import com.lifeos.app.domain.usecase.HomeSummary
import com.lifeos.app.domain.model.TimelineItem
import com.lifeos.app.ui.components.LifeOSCard
import com.lifeos.app.ui.components.LifeOSLoadingDots
import com.lifeos.app.ui.components.LifeOSPageHeader
import com.lifeos.app.ui.components.LifeOSProgressTrack
import com.lifeos.app.ui.theme.LifeOSMint
import com.lifeos.app.ui.theme.LifeOSSurfaceDark
import com.lifeos.app.ui.theme.LifeOSSurfaceLight
import com.lifeos.app.ui.theme.LocalLifeOSSemanticColors
import com.lifeos.app.ui.theme.lifeOSCanvasBrush
import java.util.Locale

/**
 * Home — the command center, rebuilt to the Figma "Home" frame.
 *
 * Layout, top to bottom: an 86dp page header (uppercase date eyebrow + the
 * greeting, with one 42dp action on the right), the universal search well, the
 * day's progress card (real percent + task/habit counts), "Still on your list"
 * with the tasks that are still open, two tracking shortcuts (Habits, Spent
 * today), and the newest memory worth keeping.
 *
 * Every number comes from [HomeSummary], assembled live from persisted rows —
 * nothing here is a placeholder, and there is no "Illustrative data" caption in
 * the shipped UI (that line exists only in the Figma comp).
 */
@Composable
fun HomeScreen(
    onOpenTasks: () -> Unit,
    onOpenHabits: () -> Unit,
    onOpenExpenses: () -> Unit,
    onOpenDiary: () -> Unit,
    onOpenTimeline: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSnap: () -> Unit,
    onOpenSnapItem: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAi: () -> Unit
) {
    val locator = LocalServiceLocator.current
    val viewModel: HomeViewModel = viewModel(
        factory = LambdaViewModelFactory {
            HomeViewModel(locator.getHomeSummaryUseCase, locator.taskRepository, locator.habitRepository)
        }
    )
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val dark = androidx.compose.foundation.isSystemInDarkTheme()

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(lifeOSCanvasBrush(dark)),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        item {
            LifeOSPageHeader(
                eyebrow = summary?.dateLabel.orEmpty(),
                title = summary?.greeting ?: "LifeOS",
                actionIcon = Icons.Outlined.Tune,
                actionDescription = "Profile and settings",
                onAction = onOpenProfile
            )
        }

        item {
            Column(
                Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                HomeSearchWell(onOpenSearch = onOpenSearch, onOpenAi = onOpenAi)

                val s = summary
                if (s == null) {
                    LifeOSLoadingDots(Modifier.padding(vertical = 40.dp))
                } else {
                    DailyProgressCard(s)

                    LifeOSSectionRow(
                        title = "Still on your list",
                        action = "Tasks →",
                        onAction = onOpenTasks
                    )

                    if (s.overdueTaskCount > 0) {
                        OverdueRow(count = s.overdueTaskCount, onOpen = onOpenTasks)
                    }

                    val remaining = s.tasksToday.filter { !it.isCompleted }
                    if (remaining.isEmpty()) {
                        HomeNoticeRow(
                            icon = Icons.Outlined.CheckCircle,
                            text = if (s.tasksToday.isEmpty()) {
                                "Nothing planned today. Add a task to give the day a spine."
                            } else {
                                "Everything on today's list is done. Enjoy the quiet."
                            },
                            actionLabel = "Add task",
                            onAction = onOpenTasks
                        )
                    } else {
                        remaining.take(3).forEach { task ->
                            TaskLine(
                                task = task,
                                onToggle = { viewModel.toggleTask(task.id, true) },
                                onOpen = onOpenTasks
                            )
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val doneHabits = s.habitsToday.count { it.isDone }
                        val topHabit = s.habitsToday.firstOrNull()
                        ShortcutCard(
                            icon = Icons.Outlined.LocalFireDepartment,
                            label = "Habits",
                            value = "${doneHabits} / ${s.habitsToday.size} done",
                            sub = topHabit?.let { "${it.habit.name} ${it.progressCount}/${it.goalCount}" }
                                ?: "Add a habit →",
                            onClick = onOpenHabits,
                            modifier = Modifier.weight(1f)
                        )
                        ShortcutCard(
                            icon = Icons.Outlined.AccountBalanceWallet,
                            label = "Spent today",
                            value = formatMoney(s.todaySpend),
                            sub = "View all →",
                            onClick = onOpenExpenses,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    LifeOSSectionRow(
                        title = "A moment worth keeping",
                        action = "Life →",
                        onAction = onOpenTimeline
                    )

                    val memory = s.recentActivity.firstOrNull { it.imagePath != null }
                        ?: s.recentActivity.firstOrNull()
                    if (memory != null) {
                        MemoryCard(
                            item = memory,
                            onOpen = { onOpenDiary() },
                            onOpenSource = { onOpenSnapItem(memory.sourceId) }
                        )
                    } else {
                        HomeNoticeRow(
                            icon = null,
                            text = "No memories yet. Capture today before it fades.",
                            actionLabel = "Write",
                            onAction = onOpenDiary
                        )
                    }
                }
            }
        }
    }
}

/** The search well: opens global search, with the AI shortcut on its trailing edge. */
@Composable
private fun HomeSearchWell(onOpenSearch: () -> Unit, onOpenAi: () -> Unit) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (dark) LifeOSSurfaceDark else LifeOSSurfaceLight)
            .clickable(onClick = onOpenSearch)
            .padding(horizontal = 15.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            Icons.Outlined.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(
            "Search tasks, notes or memories",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "Ask AI",
            style = MaterialTheme.typography.labelSmall,
            color = com.lifeos.app.ui.theme.LifeOSMint,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onOpenAi)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/** Today's real progress: percent, a 5dp mint track, and the counts behind it. */
@Composable
private fun DailyProgressCard(summary: HomeSummary) {
    val percent = summary.dailyUpdatePercent.coerceIn(0, 100)
    val done = summary.tasksCompletedToday + summary.habitsToday.count { it.isDone }
    val total = summary.tasksTotalToday + summary.habitsToday.size

    LifeOSCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    summary.dayStatusLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (total == 0) "A clean page for today." else "Small steps still count.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                "$percent%",
                fontSize = 32.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
                color = LifeOSMint
            )
        }
        LifeOSProgressTrack(progress = percent / 100f)
        Text(
            if (total == 0) {
                "Nothing scheduled yet — add a task or a habit to start."
            } else {
                "$done of $total goals done today"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** A section heading with its mint trailing action. */
@Composable
private fun LifeOSSectionRow(title: String, action: String, onAction: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            action,
            style = MaterialTheme.typography.labelMedium,
            color = com.lifeos.app.ui.theme.LifeOSMint,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onAction)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/** One remaining task: tappable completion circle, details, chevron to open Tasks. */
@Composable
private fun TaskLine(task: TaskEntity, onToggle: () -> Unit, onOpen: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(21.dp)
                    .clip(RoundedCornerShape(50))
                    .border(2.dp, MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(50))
            )
        }
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                task.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                taskSubtitle(task),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = "Open tasks",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onOpen)
                .padding(14.dp)
        )
    }
}

private fun taskSubtitle(task: TaskEntity): String {
    val time = task.reminderEpochMillis?.let {
        com.lifeos.app.core.util.DateTimeUtils.formatMinutes(
            com.lifeos.app.core.util.DateTimeUtils.minutesOfDay(it)
        )
    }
    val priority = when (task.priority.name) {
        "HIGH" -> "High priority"
        "LOW" -> "Low priority"
        else -> null
    }
    val parts = listOfNotNull(time, priority).ifEmpty { listOf("Due today") }
    return parts.joinToString(" · ")
}

/** The overdue nudge, in the warning tone. */
@Composable
private fun OverdueRow(count: Int, onOpen: () -> Unit) {
    val warning = LocalLifeOSSemanticColors.current.warning
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(warning.copy(alpha = 0.10f))
            .clickable(onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "$count task${if (count == 1) "" else "s"} slipped past their day.",
            style = MaterialTheme.typography.bodySmall,
            color = warning,
            modifier = Modifier.weight(1f)
        )
        Text(
            "Reschedule",
            style = MaterialTheme.typography.labelMedium,
            color = warning
        )
    }
}

/** A quiet inline notice with an optional action. */
@Composable
private fun HomeNoticeRow(
    icon: ImageVector?,
    text: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.60f))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            actionLabel,
            style = MaterialTheme.typography.labelMedium,
            color = com.lifeos.app.ui.theme.LifeOSMint,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onAction)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/** One tracking shortcut card: icon + label, a big value, and a sub-line. */
@Composable
private fun ShortcutCard(
    icon: ImageVector,
    label: String,
    value: String,
    sub: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LifeOSCard(modifier = modifier, onClick = onClick, contentPadding = 18.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(17.dp)
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            sub,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** The latest memory: photo (when there is one), title, its own words, and time. */
@Composable
private fun MemoryCard(
    item: TimelineItem,
    onOpen: () -> Unit,
    onOpenSource: () -> Unit
) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (dark) LifeOSSurfaceDark else LifeOSSurfaceLight)
            .clickable(onClick = onOpenSource)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier
                .size(width = 88.dp, height = 79.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            val path = item.imagePath
            if (path != null) {
                AsyncImage(
                    model = path,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(item.icon, style = MaterialTheme.typography.headlineSmall)
            }
        }
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                item.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            item.subtitle?.takeIf { it.isNotBlank() }?.let { quote ->
                Text(
                    "“$quote”",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val meta = buildList {
                add(com.lifeos.app.core.util.DateTimeUtils.formatMinutes(item.timeMinutes))
                item.moodOrCategory?.takeIf { it.isNotBlank() }?.let { add("#$it") }
            }.joinToString(" · ")
            Text(
                meta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatMoney(amount: Double): String {
    val digits = if (amount % 1.0 == 0.0) 0 else 2
    return "₹" + String.format(Locale.getDefault(), "%,.${digits}f", amount)
}
