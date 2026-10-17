package com.lifeos.app.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.data.db.entities.TaskEntity
import com.lifeos.app.domain.usecase.HabitSummaryRow
import com.lifeos.app.domain.usecase.HomeSummary
import com.lifeos.app.ui.components.HeaderActionCircle
import com.lifeos.app.ui.components.LifeOSBanner
import com.lifeos.app.ui.components.LifeOSHeroCard
import com.lifeos.app.ui.components.LifeOSIconButton
import com.lifeos.app.ui.components.LifeOSPill
import com.lifeos.app.ui.components.LifeOSSearchBar
import com.lifeos.app.ui.components.LifeOSSurface
import com.lifeos.app.ui.components.LifeOSWeeklyRhythm
import com.lifeos.app.ui.components.ProfileAvatar
import com.lifeos.app.ui.components.liquidGlass
import com.lifeos.app.ui.components.prismBrush
import com.lifeos.app.ui.theme.FigmaAccent
import com.lifeos.app.ui.theme.FigmaMuted
import com.lifeos.app.ui.theme.FigmaText
import com.lifeos.app.ui.theme.FigmaTrack
import com.lifeos.app.ui.theme.LifeOSRadius
import com.lifeos.app.ui.theme.LifeOSSpacing
import com.lifeos.app.ui.theme.LocalLifeOSSemanticColors
import java.util.Locale

/**
 * Home — the command centre. One hero (today's real progress), a quick-action
 * rail, an entry to LifeOS AI, today's tasks and habits, the shared weekly
 * rhythm, today's spend, and a preview of recent activity.
 *
 * Every number comes from [HomeSummary], which is assembled live from persisted
 * rows — nothing here is a placeholder.
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

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = LifeOSSpacing.screenPadding,
            end = LifeOSSpacing.screenPadding,
            top = LifeOSSpacing.md,
            bottom = 140.dp
        ),
        verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sectionSpacing)
    ) {
        item { HomeHeader(summary, onOpenSearch, onOpenProfile) }

        item {
            LifeOSSearchBar(
                value = "",
                onValueChange = {},
                onClick = onOpenSearch,
                placeholder = "Search your life…"
            )
        }

        item { DailyUpdateHero(summary) }

        item {
            QuickActionRow(
                onOpenTasks = onOpenTasks,
                onOpenHabits = onOpenHabits,
                onOpenExpenses = onOpenExpenses,
                onOpenDiary = onOpenDiary
            )
        }

        summary?.let { s ->
            if (s.overdueTaskCount > 0) {
                item {
                    LifeOSBanner(
                        icon = Icons.Filled.WarningAmber,
                        text = "${s.overdueTaskCount} task${if (s.overdueTaskCount == 1) "" else "s"} slipped past their day. Reschedule or clear them on Tasks.",
                        tint = LocalLifeOSSemanticColors.current.warning,
                        action = "Open",
                        onAction = onOpenTasks
                    )
                }
            }

            item {
                SectionHeader(
                    title = "Today's tasks",
                    trailing = if (s.tasksTotalToday > 0) "${s.tasksCompletedToday}/${s.tasksTotalToday}" else null,
                    onSeeAll = onOpenTasks
                )
            }
            if (s.tasksToday.isEmpty()) {
                item { EmptyRow(Icons.Outlined.CheckCircle, "Nothing planned today", "Add a task to give the day a spine.", "Add task", onOpenTasks) }
            } else {
                items(s.tasksToday.take(5), key = { it.id }) { task ->
                    TaskRow(task = task, onToggle = { viewModel.toggleTask(task.id, !task.isCompleted) })
                }
            }

            item {
                SectionHeader(
                    title = "Habits",
                    trailing = if (s.habitsToday.isNotEmpty()) "${s.habitsToday.count { it.isDone }}/${s.habitsToday.size}" else null,
                    onSeeAll = onOpenHabits
                )
            }
            if (s.habitsToday.isEmpty()) {
                item { EmptyRow(Icons.Filled.LocalFireDepartment, "No habits yet", "Small daily repetitions compound. Start one.", "Add habit", onOpenHabits) }
            } else {
                items(s.habitsToday.take(5), key = { it.habit.id }) { row ->
                    HabitRow(row = row, onToggle = { viewModel.toggleHabit(row.habit.id, row.isDone, row.goalCount) })
                }
            }

            item {
                LifeOSSurface(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("This week", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            LifeOSPill(text = "${s.weeklyDoneDays}/7 days", tint = LocalLifeOSSemanticColors.current.success)
                        }
                        LifeOSWeeklyRhythm(days = s.weeklyConsistency, dotSize = 32.dp)
                        Text(
                            when {
                                s.weeklyDoneDays >= 6 -> "A strong week. Keep the rhythm."
                                s.weeklyDoneDays >= 3 -> "Halfway there — momentum is building."
                                s.weeklyDoneDays > 0 -> "A start. Stack one more day."
                                else -> "A clean week ahead."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)) {
                    MiniStat(
                        label = "Spent today",
                        value = formatMoney(s.todaySpend),
                        icon = Icons.Filled.Payments,
                        tint = MaterialTheme.colorScheme.primary,
                        onClick = onOpenExpenses,
                        modifier = Modifier.weight(1f)
                    )
                    MiniStat(
                        label = "Quick capture",
                        value = "＋",
                        icon = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        onClick = onOpenSnap,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (s.recentActivity.isNotEmpty()) {
                item { SectionHeader(title = "Recent activity", trailing = null, onSeeAll = onOpenTimeline) }
                items(s.recentActivity.take(4), key = { it.id }) { activity ->
                    LifeOSSurface(Modifier.fillMaxWidth(), onClick = { onOpenSnapItem(activity.sourceId) }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)) {
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(LifeOSRadius.medium))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(activity.icon, style = MaterialTheme.typography.titleMedium)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(activity.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                activity.subtitle?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // LifeOS AI is the screen's last entry, not a banner above the user's
            // real day. Content leads: today's tasks, habits, spend and memories
            // rank first, and the on-device assistant is offered underneath them
            // as one more thing the user can reach — never as a competing hero.
            item { AiNudge(onOpenAi) }
        }
    }
}

@Composable
private fun HomeHeader(summary: HomeSummary?, onOpenSearch: () -> Unit, onOpenProfile: () -> Unit) {
    // The redesign's page header: a mint date kicker over the greeting, with the
    // search and profile actions as the shared 42×42 circles.
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = LifeOSSpacing.pageHeaderHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                (summary?.dateLabel ?: "").uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                ),
                color = FigmaAccent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                summary?.greeting ?: "Hello",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp, lineHeight = 28.sp),
                color = FigmaText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        HeaderActionCircle(Icons.Filled.Search, "Search", onOpenSearch)
        ProfileAvatar(size = 42.dp, onClick = onOpenProfile)
    }
}

@Composable
private fun DailyUpdateHero(summary: HomeSummary?) {
    val percent = summary?.dailyUpdatePercent ?: 0
    val progress by animateFloatAsState(percent / 100f, tween(600), label = "dailyUpdate")

    val tasksDone = summary?.tasksCompletedToday ?: 0
    val tasksTotal = summary?.tasksTotalToday ?: 0
    val habitsDone = summary?.habitsToday?.count { it.isDone } ?: 0
    val habitsTotal = summary?.habitsToday?.size ?: 0
    val summaryLine = if (tasksTotal == 0 && habitsTotal == 0) {
        "Nothing scheduled yet — add a task or a habit to start."
    } else {
        "$tasksDone of $tasksTotal tasks · $habitsDone of $habitsTotal habits complete"
    }

    // The redesign's "Daily progress" card: a kicker, a promise, the big number,
    // a slim mint track and one honest summary line. A standard card, not a
    // gradient hero — mint is the only lit colour in the frame.
    LifeOSSurface(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "TODAY, AT YOUR PACE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        ),
                        color = FigmaAccent
                    )
                    Text(
                        "Small steps still count.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FigmaMuted
                    )
                }
                Text(
                    "$percent%",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 26.sp, fontWeight = FontWeight.SemiBold),
                    color = FigmaText
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(FigmaTrack)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(5.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(FigmaAccent)
                )
            }
            Text(
                summaryLine,
                style = MaterialTheme.typography.labelSmall,
                color = FigmaMuted
            )
        }
    }
}

@Composable
private fun BoltDisc() {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Box(
        Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(onSurface.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.Bolt, contentDescription = null, tint = onSurface, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun QuickActionRow(
    onOpenTasks: () -> Unit,
    onOpenHabits: () -> Unit,
    onOpenExpenses: () -> Unit,
    onOpenDiary: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)) {
        QuickAction(Icons.Outlined.CheckCircle, "Tasks", LocalLifeOSSemanticColors.current.success, onOpenTasks, Modifier.weight(1f))
        QuickAction(Icons.Filled.LocalFireDepartment, "Habits", LocalLifeOSSemanticColors.current.warning, onOpenHabits, Modifier.weight(1f))
        QuickAction(Icons.Filled.Payments, "Money", MaterialTheme.colorScheme.primary, onOpenExpenses, Modifier.weight(1f))
        QuickAction(Icons.Filled.Description, "Diary", MaterialTheme.colorScheme.secondary, onOpenDiary, Modifier.weight(1f))
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(LifeOSRadius.large))
            .liquidGlass(shape = RoundedCornerShape(LifeOSRadius.large), tint = tint, interactive = true)
            .clickable(onClick = onClick)
            .padding(vertical = LifeOSSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sm)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
    }
}

@Composable
private fun AiNudge(onOpenAi: () -> Unit) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    // The orb breathes very gently — it reads as "alive / always here" without
    // pretending to be working (there is nothing to wait for until it is opened).
    val orbTransition = rememberInfiniteTransition(label = "aiOrb")
    val orbBreath by orbTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aiOrbBreath"
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LifeOSRadius.xlarge))
            .background(prismBrush(alpha = 0.14f))
            .clickable(onClick = onOpenAi)
            .padding(horizontal = LifeOSSpacing.lg, vertical = LifeOSSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)
    ) {
        Box(
            Modifier
                .size(40.dp)
                .graphicsLayer {
                    val s = 1f + 0.08f * orbBreath
                    scaleX = s
                    scaleY = s
                }
                .clip(CircleShape)
                .background(prismBrush()),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Bolt, contentDescription = null, tint = onSurface, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text("Ask LifeOS AI", style = MaterialTheme.typography.titleSmall, color = onSurface)
            Text("“What did I spend this month?”", style = MaterialTheme.typography.bodySmall, color = onSurfaceVariant)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SectionHeader(title: String, trailing: String?, onSeeAll: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
        trailing?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = LifeOSSpacing.sm))
        }
        Text(
            "See all",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(LifeOSRadius.pill))
                .clickable(onClick = onSeeAll)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun TaskRow(task: TaskEntity, onToggle: () -> Unit) {
    LifeOSSurface(Modifier.fillMaxWidth(), onClick = onToggle) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)) {
            Box(
                Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .then(
                        if (task.isCompleted) Modifier.background(LocalLifeOSSemanticColors.current.success)
                        else Modifier.border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) {
                    Icon(Icons.Filled.Check, contentDescription = "Completed", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            Text(
                task.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (task.priority.name == "HIGH") {
                LifeOSPill(text = "High", tint = LocalLifeOSSemanticColors.current.warning)
            }
        }
    }
}

@Composable
private fun HabitRow(row: HabitSummaryRow, onToggle: () -> Unit) {
    LifeOSSurface(Modifier.fillMaxWidth(), onClick = onToggle) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(LifeOSRadius.medium))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(row.habit.icon, style = MaterialTheme.typography.titleMedium)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(row.habit.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (row.currentStreak > 0) "${row.currentStreak}-day streak · ${row.progressCount}/${row.goalCount} today" else "${row.progressCount}/${row.goalCount} today",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .then(
                        if (row.isDone) Modifier.background(LocalLifeOSSemanticColors.current.success)
                        else Modifier.border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (row.isDone) {
                    Icon(Icons.Filled.Check, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, icon: ImageVector?, tint: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(LifeOSRadius.xlarge))
            .liquidGlass(shape = RoundedCornerShape(LifeOSRadius.xlarge), tint = tint, interactive = true)
            .clickable(onClick = onClick)
            .padding(LifeOSSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            icon?.let { Icon(it, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp)) }
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun EmptyRow(icon: ImageVector, title: String, body: String, action: String, onAction: () -> Unit) {
    LifeOSSurface(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                action,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(LifeOSRadius.pill))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

private fun formatMoney(amount: Double): String {
    val digits = if (amount % 1.0 == 0.0) 0 else 2
    return "₹" + String.format(Locale.getDefault(), "%,.${digits}f", amount)
}
