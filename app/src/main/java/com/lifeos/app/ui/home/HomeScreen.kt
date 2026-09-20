package com.lifeos.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.TaskEntity
import com.lifeos.app.domain.model.TimelineItem
import com.lifeos.app.domain.model.TimelineItemType
import com.lifeos.app.domain.usecase.DayCheck
import com.lifeos.app.domain.usecase.HabitSummaryRow
import com.lifeos.app.domain.usecase.HomeSummary
import com.lifeos.app.ui.components.LifeOSCard
import com.lifeos.app.ui.components.ProfileAvatar
import com.lifeos.app.ui.theme.LifeOSPrimary
import com.lifeos.app.ui.theme.LifeOSSpacing
import com.lifeos.app.ui.theme.LifeOSWarning
import java.time.LocalDate
import java.util.Locale

@Composable
fun HomeScreen(
    onOpenTasks: () -> Unit,
    onOpenHabits: () -> Unit,
    onOpenCapture: () -> Unit,
    onOpenAiAssistant: () -> Unit = {},
    onOpenNotes: () -> Unit = {},
    onOpenExpenses: () -> Unit = {},
    onOpenDiary: () -> Unit = {},
    onOpenInsights: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenTimeline: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenProfile: () -> Unit = {}
) {
    val locator = LocalServiceLocator.current
    val viewModel: HomeViewModel = viewModel(
        factory = LambdaViewModelFactory {
            HomeViewModel(locator.getHomeSummaryUseCase, locator.taskRepository, locator.habitRepository)
        }
    )
    val summary by viewModel.summary.collectAsState()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = LifeOSSpacing.screenPadding,
                end = LifeOSSpacing.screenPadding,
                top = 8.dp,
                bottom = LifeOSSpacing.extendedFabContentClearance
            ),
            verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sectionSpacing)
        ) {
            item { HomeHeader(onOpenSearch, onOpenProfile) }
            summary?.let { s ->
                item {
                    GreetingHeader(
                        dateLabel = s.dateLabel,
                        greeting = s.greeting,
                        dayStatusLabel = s.dayStatusLabel
                    )
                }
                item {
                    DayProgressCard(
                        dailyUpdatePercent = s.dailyUpdatePercent,
                        tasksDone = s.tasksCompletedToday,
                        tasksTotal = s.tasksTotalToday,
                        habitsDone = s.habitsToday.count { it.isDone },
                        habitsTotal = s.habitsToday.size,
                        spend = s.todaySpend
                    )
                }
                item {
                    TodayTasksSection(
                        summary = summary,
                        onOpenTasks = onOpenTasks,
                        onToggleTask = viewModel::toggleTask
                    )
                }
            }
            item { QuickActionsSection(onOpenDiary, onOpenExpenses, onOpenTimeline) }
            item { HabitsSection(summary = summary, onOpenHabits = onOpenHabits, onToggleHabit = viewModel::toggleHabit) }
            item { ActivityHeader(summary) }
            val activityItems = summary?.recentActivity.orEmpty()
            if (activityItems.isEmpty()) {
                item {
                    LifeOSCard(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("No activity recorded today", style = MaterialTheme.typography.titleMedium)
                            Text("Tasks, habits, expenses and captures you log today will appear here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                itemsIndexed(activityItems) { index, item ->
                    TimelineRow(item, isLast = index == activityItems.lastIndex)
                }
            }
        }

        FloatingActionButton(
            onClick = onOpenCapture,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = LifeOSSpacing.screenPadding, bottom = 16.dp)
                .padding(4.dp),
            containerColor = LifeOSPrimary,
            contentColor = Color.White
        ) {
            Icon(Icons.Filled.Camera, contentDescription = "Capture")
        }
    }
}

@Composable
private fun HomeHeader(onSearch: () -> Unit, onProfile: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("LifeOS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(LifeOSPrimary)
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onSearch) { Icon(Icons.Filled.Search, contentDescription = "Search") }
        ProfileAvatar(size = 40.dp, onClick = onProfile)
    }
}

@Composable
private fun GreetingHeader(dateLabel: String, greeting: String, dayStatusLabel: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                dateLabel.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box(
                Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(LifeOSPrimary)
            )
            Text(
                dayStatusLabel,
                style = MaterialTheme.typography.labelMedium,
                color = LifeOSPrimary
            )
        }
        Text(greeting, style = MaterialTheme.typography.headlineLarge)
    }
}

/**
 * The "DAILY UPDATE" card. Percent comes from [HomeSummary.dailyUpdatePercent],
 * which is computed live from real persisted data (completed tasks + habits),
 * and is drawn as a single horizontal bar — no four-segment artwork, no
 * device-clock fill.
 */
@Composable
private fun DayProgressCard(
    dailyUpdatePercent: Int,
    tasksDone: Int,
    tasksTotal: Int,
    habitsDone: Int,
    habitsTotal: Int,
    spend: Double
) {
    LifeOSCard(Modifier.fillMaxWidth(), tint = MaterialTheme.colorScheme.surface) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("DAILY UPDATE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${dailyUpdatePercent.coerceIn(0, 100)}%", style = MaterialTheme.typography.headlineLarge)
                        Text("of today's goals", style = MaterialTheme.typography.labelMedium, color = LifeOSPrimary, modifier = Modifier.padding(bottom = 4.dp))
                    }
                }
            }
            LinearProgressIndicator(
                progress = { dailyUpdatePercent.coerceIn(0, 100) / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50)),
                color = LifeOSPrimary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                strokeCap = StrokeCap.Round
            )
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DayStat(Icons.Filled.CheckCircle, "$tasksDone/$tasksTotal", "TASKS", Modifier.weight(1f))
                    DayStat(Icons.Filled.LocalFireDepartment, "$habitsDone/$habitsTotal", "HABITS", Modifier.weight(1f))
                    DayStat(Icons.Filled.Payments, formatAmount(spend), "SPEND", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DayStat(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Icon(icon, contentDescription = null, tint = LifeOSPrimary, modifier = Modifier.size(15.dp))
        Column(Modifier.padding(start = 6.dp)) {
            Text(value, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * "TODAY'S TASKS" — the real list of today's tasks from Room. When nothing is
 * planned the section renders nothing at all (no placeholder card, no stale
 * text), so the dashboard stays clean and the cards can never overlap.
 */
@Composable
private fun TodayTasksSection(summary: HomeSummary?, onOpenTasks: () -> Unit, onToggleTask: (String, Boolean) -> Unit) {
    val tasks = summary?.tasksToday.orEmpty()
    if (tasks.isEmpty()) return
    val pendingCount = tasks.count { !it.isCompleted }
    HomeSectionHeader("Today's Tasks") {
        if (pendingCount > 0) {
            Box(
                Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text("$pendingCount", style = MaterialTheme.typography.labelSmall, color = LifeOSPrimary)
            }
        }
        Spacer(Modifier.width(6.dp))
        SectionAction("View tasks", onOpenTasks)
    }
    LifeOSCard(Modifier.fillMaxWidth(), tint = MaterialTheme.colorScheme.surface) {
        Column {
            tasks.forEachIndexed { index, task ->
                TodayTaskRow(task = task, onToggle = { onToggleTask(task.id, !task.isCompleted) })
                if (index < tasks.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                }
            }
        }
    }
}

@Composable
private fun TodayTaskRow(task: TaskEntity, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    if (task.isCompleted) LifeOSPrimary
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                )
        )
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                task.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
            )
            Text(
                todayTaskSubtitle(task),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onToggle) {
            Icon(
                if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = if (task.isCompleted) "Task done, undo completion" else "Mark task done",
                tint = if (task.isCompleted) LifeOSPrimary else MaterialTheme.colorScheme.outline
            )
        }
    }
}

private fun todayTaskSubtitle(task: TaskEntity): String {
    val parts = listOfNotNull(
        task.category?.let { "#$it" },
        task.description?.takeUnless { it.isBlank() }
    )
    return if (parts.isEmpty()) {
        task.priority.name.lowercase().replaceFirstChar { it.uppercase() } + " priority"
    } else {
        parts.joinToString(" · ")
    }
}

@Composable
private fun QuickActionsSection(onOpenDiary: () -> Unit, onOpenExpenses: () -> Unit, onOpenTimeline: () -> Unit) {
    HomeSectionHeader("Quick Actions")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        QuickActionTile("Diary", Icons.Filled.Book, onOpenDiary, Modifier.weight(1f))
        QuickActionTile("Expense", Icons.AutoMirrored.Filled.ReceiptLong, onOpenExpenses, Modifier.weight(1f))
        QuickActionTile("Timeline", Icons.Filled.Timeline, onOpenTimeline, Modifier.weight(1f))
    }
}

@Composable
private fun QuickActionTile(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = LifeOSPrimary, modifier = Modifier.size(21.dp))
                }
            }
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

/**
 * Habits block with a prominent title (larger, semibold onSurface) so the
 * section reads as a clear heading rather than a small caption — alongside the
 * trailing "n/N Active" and "Open all" actions.
 */
@Composable
private fun HabitsSection(summary: HomeSummary?, onOpenHabits: () -> Unit, onToggleHabit: (String, Boolean, Int) -> Unit) {
    val habits = summary?.habitsToday.orEmpty()
    val doneToday = habits.count { it.isDone }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Habits",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.weight(1f))
        Text("$doneToday/${habits.size} Active", style = MaterialTheme.typography.labelMedium, color = LifeOSPrimary)
        Spacer(Modifier.width(8.dp))
        SectionAction("Open all", onOpenHabits)
    }
    if (habits.isEmpty()) {
        LifeOSCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("No routines yet", style = MaterialTheme.typography.titleMedium)
                Text("Create a small habit to start a local streak.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        LifeOSCard(Modifier.fillMaxWidth(), tint = MaterialTheme.colorScheme.surface) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("WEEKLY CONSISTENCY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "${summary?.weeklyDoneDays ?: 0} of 7 days",
                            style = MaterialTheme.typography.labelMedium,
                            color = LifeOSPrimary
                        )
                    }
                    Surface(color = LifeOSPrimary, shape = CircleShape, modifier = Modifier.size(32.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                WeeklyConsistencyRow(summary?.weeklyConsistency.orEmpty())
                Spacer(Modifier.height(2.dp))
                habits.forEach { row ->
                    HabitRow(row, onToggleHabit = { onToggleHabit(row.habit.id, row.isDone, row.goalCount) })
                }
            }
        }
    }
}

@Composable
private fun WeeklyConsistencyRow(days: List<DayCheck>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        days.forEach { day ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    weekdayLetter(day.epochDay),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (day.isToday) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(7.dp))
                Box(
                    Modifier.size(30.dp).clip(CircleShape).background(
                        when {
                            day.isDone -> LifeOSPrimary
                            day.isToday -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        day.isDone -> Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                        day.isToday -> Box(Modifier.size(6.dp).clip(CircleShape).background(LifeOSPrimary))
                        else -> Box(Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant))
                    }
                }
            }
        }
    }
}

private fun weekdayLetter(epochDay: Long): String {
    val value = LocalDate.ofEpochDay(epochDay).dayOfWeek.value
    return "MTWTFSS"[value - 1].toString()
}

@Composable
private fun HabitRow(row: HabitSummaryRow, onToggleHabit: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(row.habit.icon, style = MaterialTheme.typography.titleMedium)
            }
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(row.habit.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                if (row.currentStreak > 0) {
                    Surface(color = LifeOSWarning.copy(alpha = 0.16f), shape = RoundedCornerShape(50)) {
                        Text(
                            "${row.currentStreak}d streak",
                            style = MaterialTheme.typography.labelSmall,
                            color = LifeOSWarning,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            maxLines = 1
                        )
                    }
                }
            }
            Text(
                habitSubtitle(row),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        IconButton(onClick = onToggleHabit) {
            Icon(
                if (row.isDone) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = if (row.isDone) "Undo ${row.habit.name} completion" else "Mark ${row.habit.name} complete",
                tint = if (row.isDone) LifeOSPrimary else MaterialTheme.colorScheme.outline
            )
        }
    }
}

private fun habitSubtitle(row: HabitSummaryRow): String {
    return if (row.habit.goalCount > 1) {
        "${row.progressCount}/${row.goalCount} today"
    } else if (row.isDone) {
        "Completed today"
    } else {
        "Tap to check in"
    }
}

@Composable
private fun ActivityHeader(summary: HomeSummary?) {
    HomeSectionHeader("Today's Activity") {
        Text("Real-time stream", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TimelineRow(item: TimelineItem, isLast: Boolean) {
    Row(Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 2.dp)) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                shape = CircleShape,
                modifier = Modifier.size(20.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        iconFor(item.type),
                        contentDescription = null,
                        tint = LifeOSPrimary,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
            if (!isLast) {
                Box(
                    Modifier
                        .width(2.dp)
                        .height(34.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                )
            }
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        item.subtitle?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            DateTimeUtils.formatMinutes(item.timeMinutes),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(if (isLast) 0.dp else 12.dp))
        }
    }
}

private fun iconFor(type: TimelineItemType): ImageVector = when (type) {
    TimelineItemType.NOTE -> Icons.Filled.EditNote
    TimelineItemType.TASK_COMPLETED -> Icons.Filled.Check
    TimelineItemType.HABIT_COMPLETED -> Icons.Filled.LocalFireDepartment
    TimelineItemType.EXPENSE -> Icons.Filled.Payments
    TimelineItemType.DIARY -> Icons.Filled.Book
    TimelineItemType.CAPTURE -> Icons.Filled.Camera
}

@Composable
private fun HomeSectionHeader(title: String, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun SectionAction(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = LifeOSPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    )
}

private fun formatAmount(amount: Double): String {
    val digits = if (amount % 1.0 == 0.0) 0 else 2
    return "₹" + String.format(Locale.getDefault(), "%,.${digits}f", amount)
}