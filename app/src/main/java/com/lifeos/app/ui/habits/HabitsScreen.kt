package com.lifeos.app.ui.habits

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.data.db.entities.HabitEntity
import com.lifeos.app.data.db.entities.HabitTrackingMode
import com.lifeos.app.data.db.entities.HabitType
import com.lifeos.app.ui.components.LifeOSPrimaryButton
import com.lifeos.app.ui.components.LifeOSProgressBar
import com.lifeos.app.ui.components.LifeOSScreenHeader
import com.lifeos.app.ui.components.LifeOSSurface
import com.lifeos.app.ui.components.LiquidGlassFab
import com.lifeos.app.ui.theme.LifeOSRadius
import com.lifeos.app.ui.theme.LifeOSSpacing
import com.lifeos.app.ui.theme.LocalLifeOSSemanticColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class HabitFilter(val label: String) {
    ALL("All"),
    DUE("Due"),
    COMPLETED("Completed")
}

@Composable
fun HabitsScreen(
    onOpenHabit: (String) -> Unit,
    onAddHabit: (Boolean) -> Unit,
    onEditHabit: (String) -> Unit
) {
    val locator = LocalServiceLocator.current
    val viewModel: HabitsViewModel = viewModel(
        factory = LambdaViewModelFactory { HabitsViewModel(locator.habitRepository) }
    )
    val state by viewModel.uiState.collectAsState()
    var filter by rememberSaveable { mutableStateOf(HabitFilter.ALL) }
    var archivedOpen by rememberSaveable { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<HabitEntity?>(null) }
    var celebration by remember { mutableStateOf<HabitCelebration?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.celebrations.collect { celebration = it }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            floatingActionButton = {
                LiquidGlassFab(
                    icon = Icons.Filled.Add,
                    contentDescription = if (state.hasHabits) "Add habit" else "Create your first habit",
                    onClick = { onAddHabit(!state.hasHabits) }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(
                    start = LifeOSSpacing.screenPadding,
                    end = LifeOSSpacing.screenPadding,
                    top = 8.dp,
                    bottom = LifeOSSpacing.fabContentClearance + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.lg)
            ) {
                item(key = "header") {
                    LifeOSScreenHeader(
                        title = "Habits",
                        eyebrow = relativeDayLabel(state.selectedEpochDay, state.todayEpochDay),
                        subtitle = if (state.selectedEpochDay == state.todayEpochDay) {
                            "Small repetitions, compounding privately on this device."
                        } else {
                            fullDayLabel(state.selectedEpochDay)
                        },
                        actions = {
                            IconButton(onClick = { archivedOpen = true }) {
                                Icon(
                                    Icons.Filled.Archive,
                                    contentDescription = "Archived habits",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    )
                }

                if (!state.hasHabits) {
                    item(key = "onboarding") {
                        FirstHabitInvitation(onCreate = { onAddHabit(true) })
                    }
                    item(key = "privacy") {
                        HabitPrivacyNote()
                    }
                } else {
                    item(key = "date-strip") {
                        HabitDateStrip(
                            selectedEpochDay = state.selectedEpochDay,
                            todayEpochDay = state.todayEpochDay,
                            onSelect = viewModel::selectDate,
                            onShiftWeek = viewModel::shiftWeek,
                            onToday = viewModel::goToToday
                        )
                    }
                    item(key = "score") {
                        HabitScoreHero(state)
                    }
                    item(key = "filters") {
                        HabitFilterRow(
                            selected = filter,
                            allCount = state.cards.size,
                            dueCount = state.cards.count { it.isDue && !it.isDone },
                            completedCount = state.cards.count { it.isDone },
                            onSelect = { filter = it }
                        )
                    }

                    val visible = when (filter) {
                        HabitFilter.ALL -> state.cards
                        HabitFilter.DUE -> state.cards.filter { it.isDue && !it.isDone }
                        HabitFilter.COMPLETED -> state.cards.filter { it.isDone }
                    }

                    if (visible.isEmpty()) {
                        item(key = "empty-filter") {
                            EmptyFilterCard(
                                state = state,
                                filter = filter,
                                onAdd = { onAddHabit(false) }
                            )
                        }
                    } else {
                        items(visible, key = { "habit-${it.habit.id}" }) { card ->
                            HabitCard(
                                card = card,
                                onOpen = { onOpenHabit(card.habit.id) },
                                onToggle = {
                                    if (card.habit.trackingMode == HabitTrackingMode.CHECKLIST) {
                                        onOpenHabit(card.habit.id)
                                    } else {
                                        viewModel.setTick(card, !card.isDone)
                                    }
                                },
                                onIncrement = { viewModel.increment(card) },
                                onDecrement = { viewModel.decrement(card) },
                                onSlip = { viewModel.logSlip(card) },
                                onEdit = { onEditHabit(card.habit.id) },
                                onArchive = { viewModel.archive(card.habit.id) },
                                onDelete = { deleteTarget = card.habit }
                            )
                        }
                    }

                    item(key = "footer") {
                        Text(
                            "Everything here is stored on this device. No account, no upload.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = LifeOSSpacing.sm)
                        )
                    }
                }
            }
        }

        celebration?.let { event ->
            HabitCelebrationOverlay(
                celebration = event,
                onDismiss = { celebration = null },
                onShare = { shareHabitCelebration(context, it) }
            )
        }
    }

    if (archivedOpen) {
        ArchivedHabitsSheet(
            archived = state.archived,
            onDismiss = { archivedOpen = false },
            onRestore = viewModel::unarchive,
            onDelete = { deleteTarget = it }
        )
    }

    deleteTarget?.let { habit ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete ${habit.name}?") },
            text = {
                Text("This permanently removes the habit, its history and its checklist. Archiving keeps history and hides it from the dashboard.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(habit.id)
                    deleteTarget = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun HabitDateStrip(
    selectedEpochDay: Long,
    todayEpochDay: Long,
    onSelect: (Long) -> Unit,
    onShiftWeek: (Long) -> Unit,
    onToday: () -> Unit
) {
    val selected = LocalDate.ofEpochDay(selectedEpochDay)
    val weekStart = selected.minusDays((selected.dayOfWeek.value - 1).toLong())
    val days = (0L..6L).map { weekStart.plusDays(it) }
    val canGoForward = weekStart.plusWeeks(1).toEpochDay() <= todayEpochDay
    val monthLabel = weekStart.format(DateTimeFormatter.ofPattern("MMMM yyyy"))

    LifeOSSurface(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onShiftWeek(-7) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous week")
                }
                Text(
                    monthLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                if (selectedEpochDay != todayEpochDay) {
                    TextButton(onClick = onToday, contentPadding = PaddingValues(horizontal = 6.dp)) {
                        Text("Today", style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    Spacer(Modifier.width(36.dp))
                }
                IconButton(
                    onClick = { onShiftWeek(7) },
                    enabled = canGoForward,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = "Next week",
                        tint = if (canGoForward) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                days.forEach { date ->
                    HabitDayCell(
                        date = date,
                        selected = date.toEpochDay() == selectedEpochDay,
                        isToday = date.toEpochDay() == todayEpochDay,
                        enabled = date.toEpochDay() <= todayEpochDay,
                        onClick = { onSelect(date.toEpochDay()) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitDayCell(
    date: LocalDate,
    selected: Boolean,
    isToday: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background by animateColorAsState(
        targetValue = when {
            selected -> HabitIris
            isToday -> HabitIrisSoft
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        },
        label = "dayBackground"
    )
    val content by animateColorAsState(
        targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        label = "dayContent"
    )
    val scale by animateFloatAsState(if (selected) 1f else 0.94f, label = "dayScale")

    Column(
        modifier
            .height(62.dp)
            .clip(RoundedCornerShape(LifeOSRadius.large))
            .background(background.copy(alpha = if (enabled) 1f else 0.4f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            date.format(DateTimeFormatter.ofPattern("EEEEE")),
            style = MaterialTheme.typography.labelSmall,
            color = content.copy(alpha = 0.72f)
        )
        Spacer(Modifier.height(3.dp))
        Text(
            "${date.dayOfMonth}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = content,
            modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
        )
        Spacer(Modifier.height(2.dp))
        if (isToday) {
            TodayDot(if (selected) Color.White.copy(alpha = 0.9f) else HabitIris)
        } else {
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun HabitScoreHero(state: HabitsUiState) {
    val gradient = Brush.linearGradient(listOf(HabitIris, HabitIrisDeep))
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(gradient)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (state.isRestDay) "Rest day" else "Habit score",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.80f)
                )
                if (state.isRestDay) {
                    Text(
                        "Nothing is scheduled. Unscheduled days never count as misses.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                } else {
                    Text(
                        "${state.completedCount} of ${state.scheduledCount} done",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    Text(
                        if (state.completedCount >= state.scheduledCount) {
                            "Every habit due today is complete."
                        } else {
                            "${state.scheduledCount - state.completedCount} still open on this day."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.82f)
                    )
                    Spacer(Modifier.height(4.dp))
                    HabitWhiteProgress(percent = state.scorePercent)
                }
            }
            Spacer(Modifier.width(16.dp))
            if (state.isRestDay) {
                Box(
                    Modifier.size(92.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Spa,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
            } else {
                HabitScoreRing(percent = state.scorePercent, label = "complete")
            }
        }
    }
}

@Composable
private fun HabitWhiteProgress(percent: Int) {
    val fraction by animateFloatAsState(percent / 100f, label = "scoreProgress")
    Box(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.20f))
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(8.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
private fun HabitFilterRow(
    selected: HabitFilter,
    allCount: Int,
    dueCount: Int,
    completedCount: Int,
    onSelect: (HabitFilter) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        HabitFilter.entries.forEach { filter ->
            val count = when (filter) {
                HabitFilter.ALL -> allCount
                HabitFilter.DUE -> dueCount
                HabitFilter.COMPLETED -> completedCount
            }
            val isSelected = filter == selected
            Surface(
                onClick = { onSelect(filter) },
                shape = RoundedCornerShape(LifeOSRadius.pill),
                color = if (isSelected) HabitIris else MaterialTheme.colorScheme.surface,
                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            ) {
                Text(
                    "${filter.label}  $count",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun HabitCard(
    card: HabitCardState,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onSlip: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    val habit = card.habit
    val accent = habitColorOf(habit)
    val done = card.isDone
    var menuOpen by remember { mutableStateOf(false) }

    LifeOSSurface(
        Modifier.fillMaxWidth(),
        tint = if (done) accent.copy(alpha = 0.075f) else MaterialTheme.colorScheme.surface,
        onClick = onOpen,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(habitEmoji(habit), fontSize = 24.sp)
                }
                Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (!habit.category.isNullOrBlank()) {
                            Text(
                                habit.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = accent
                            )
                        }
                        if (habit.habitType == HabitType.QUIT) {
                            Text(
                                "Quit",
                                style = MaterialTheme.typography.labelSmall,
                                color = LocalLifeOSSemanticColors.current.warning
                            )
                        }
                    }
                }
                Box {
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "Habit options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                            onClick = { menuOpen = false; onEdit() }
                        )
                        if (habit.habitType == HabitType.QUIT && !done) {
                            DropdownMenuItem(
                                text = { Text("Log a slip") },
                                onClick = { menuOpen = false; onSlip() }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Archive") },
                            leadingIcon = { Icon(Icons.Filled.Archive, contentDescription = null) },
                            onClick = { menuOpen = false; onArchive() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            },
                            onClick = { menuOpen = false; onDelete() }
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(shape = RoundedCornerShape(LifeOSRadius.pill), color = accent.copy(alpha = 0.12f)) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Filled.LocalFireDepartment,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            if (card.analytics.currentStreak > 0) {
                                "${card.analytics.currentStreak} ${card.analytics.streakUnit}${if (card.analytics.currentStreak == 1) "" else "s"}"
                            } else {
                                "Start today"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Text(
                    habitScheduleLabel(habit),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            when (habit.trackingMode) {
                HabitTrackingMode.TICK -> Text(
                    when {
                        done && habit.habitType == HabitType.QUIT -> "Stayed on track on this day."
                        done -> "Completed on this day."
                        habit.habitType == HabitType.QUIT -> "Not logged yet — record a slip only if it happened."
                        else -> "Not logged yet."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HabitTrackingMode.COUNT -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${card.progress} / ${card.target}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.weight(1f))
                        CountStepper(
                            enabled = !done,
                            onDecrement = onDecrement,
                            onIncrement = onIncrement,
                            accent = accent
                        )
                    }
                    HabitProgress(fraction = card.progress.toFloat() / card.target.coerceAtLeast(1), accent = accent)
                }
                HabitTrackingMode.CHECKLIST -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        if (card.progress > 0) {
                            "${card.progress} of ${card.target.coerceAtLeast(card.checklistItemCount)} items checked"
                        } else {
                            "${card.checklistItemCount} checklist items · open to check them off"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HabitProgress(
                        fraction = card.progress.toFloat() / card.target.coerceAtLeast(1),
                        accent = accent
                    )
                }
            }

            HabitMiniHeatmap(cells = card.miniHeatmap, color = accent)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Filled.Repeat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        if (card.analytics.longestStreak > card.analytics.currentStreak && card.analytics.longestStreak > 0) {
                            "Best ${card.analytics.longestStreak} ${card.analytics.streakUnit}s"
                        } else {
                            habitTrackingLabel(habit, card.checklistItemCount)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                HabitCheckButton(
                    done = done,
                    accent = accent,
                    label = when {
                        done -> "Done"
                        habit.habitType == HabitType.QUIT -> "On track"
                        habit.trackingMode == HabitTrackingMode.CHECKLIST -> "Open"
                        else -> "Check in"
                    },
                    onClick = onToggle
                )
            }
        }
    }
}

@Composable
private fun HabitCheckButton(
    done: Boolean,
    accent: Color,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(LifeOSRadius.pill),
        color = if (done) accent else accent.copy(alpha = 0.12f),
        contentColor = if (done) onHabitColor(accent) else accent
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CountStepper(
    enabled: Boolean,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    accent: Color
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            onClick = onDecrement,
            enabled = enabled,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text("−", modifier = Modifier.padding(horizontal = 11.dp, vertical = 3.dp), style = MaterialTheme.typography.titleMedium)
        }
        Surface(
            onClick = onIncrement,
            enabled = enabled,
            shape = CircleShape,
            color = accent.copy(alpha = if (enabled) 0.14f else 0.06f)
        ) {
            Text(
                "+",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                style = MaterialTheme.typography.titleMedium,
                color = accent.copy(alpha = if (enabled) 1f else 0.4f)
            )
        }
    }
}

@Composable
private fun HabitProgress(fraction: Float, accent: Color) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), label = "habitProgress")
    Box(
        Modifier
            .fillMaxWidth()
            .height(7.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.12f))
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(7.dp)
                .clip(CircleShape)
                .background(accent)
        )
    }
}

@Composable
private fun FirstHabitInvitation(onCreate: () -> Unit) {
    LifeOSSurface(
        Modifier.fillMaxWidth(),
        tint = HabitIrisWash,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.size(84.dp).clip(CircleShape).background(HabitIris.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🔥", fontSize = 38.sp)
            }
            Text(
                "Build your first habit",
                style = MaterialTheme.typography.headlineSmall,
                color = HabitIrisDeep,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                "A short guided setup covers the name, colour, schedule, reminder and how you want to track it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            LifeOSPrimaryButton(text = "Start guided setup", onClick = onCreate)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MiniFeature("🔒", "Private")
                MiniFeature("📶", "Offline")
                MiniFeature("🔥", "Streaks")
            }
        }
    }
}

@Composable
private fun MiniFeature(emoji: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(emoji, fontSize = 13.sp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = HabitIrisDeep)
    }
}

@Composable
private fun HabitPrivacyNote() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = LifeOSSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("🔒", fontSize = 15.sp)
        Text(
            "Habits never leave the phone. LifeOS has no internet permission, no account and no telemetry.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyFilterCard(state: HabitsUiState, filter: HabitFilter, onAdd: () -> Unit) {
    val (title, body) = when {
        state.isRestDay -> "A rest day" to "No habits are scheduled for this date. Rest days are neutral, never misses."
        filter == HabitFilter.DUE -> "Nothing due" to "Every habit due on this date is complete. Switch to All to review the full list."
        filter == HabitFilter.COMPLETED -> "Nothing completed yet" to "Mark a habit done and it appears here with its real streak and history."
        else -> "No habits on this date" to "Add a habit or choose another date."
    }
    LifeOSSurface(Modifier.fillMaxWidth(), contentPadding = PaddingValues(24.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (state.isRestDay) "🌙" else "✨", fontSize = 30.sp)
            Text(title, style = MaterialTheme.typography.titleMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (filter == HabitFilter.ALL && !state.isRestDay) {
                TextButton(onClick = onAdd) { Text("Add a habit") }
            }
        }
    }
}

@Composable
private fun ArchivedHabitsSheet(
    archived: List<HabitEntity>,
    onDismiss: () -> Unit,
    onRestore: (String) -> Unit,
    onDelete: (HabitEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = LifeOSSpacing.screenPadding)
                .padding(bottom = LifeOSSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)
        ) {
            Text("Archived habits", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Archiving keeps every completion and checklist row. Restore a habit to bring it back to the dashboard.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (archived.isEmpty()) {
                Text(
                    "Nothing archived.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = LifeOSSpacing.xl)
                )
            } else {
                archived.forEach { habit ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(habitColorOf(habit).copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(habitEmoji(habit), fontSize = 20.sp)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(habit.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                habitScheduleLabel(habit),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onRestore(habit.id) }) {
                            Icon(Icons.Filled.Restore, contentDescription = "Restore ${habit.name}")
                        }
                        IconButton(onClick = { onDelete(habit) }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete ${habit.name}",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
