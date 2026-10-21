package com.lifeos.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lifeos.app.core.designsystem.theme.LifeOs
import com.lifeos.app.feature.home.HomeScreen
import com.lifeos.app.feature.insights.InsightsScreen
import com.lifeos.app.feature.life.LifeScreen
import com.lifeos.app.feature.notes.NoteEditorScreen
import com.lifeos.app.feature.notes.NotesScreen
import com.lifeos.app.feature.search.SearchScreen
import com.lifeos.app.feature.settings.SettingsScreen
import com.lifeos.app.feature.tracking.DiaryScreen
import com.lifeos.app.feature.tracking.ExpensesScreen
import com.lifeos.app.feature.tracking.HabitsScreen
import com.lifeos.app.feature.tracking.TasksScreen

/**
 * Routes. Five top-level sections per the spec (Home, Notes, +Capture, Insights,
 * Life) plus the detail routes reached from them.
 */
object Routes {
    const val HOME = "home"
    const val NOTES = "notes"
    const val CAPTURE = "capture"
    const val INSIGHTS = "insights"
    const val LIFE = "life"

    const val NOTE_EDITOR = "note_editor"
    const val TASKS = "tasks"
    const val HABITS = "habits"
    const val EXPENSES = "expenses"
    const val DIARY = "diary"
    const val TIMELINE = "timeline"
    const val CALENDAR = "calendar"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val TRASH = "trash"

    fun noteEditor(noteId: String?) = "$NOTE_EDITOR/${noteId ?: "new"}"
}

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val isAction: Boolean = false,
)

private val BOTTOM_DESTINATIONS = listOf(
    BottomDestination(Routes.HOME, "Home", Icons.Default.Home),
    BottomDestination(Routes.NOTES, "Notes", Icons.Default.StickyNote2),
    BottomDestination(Routes.CAPTURE, "Capture", Icons.Default.AddCircle, isAction = true),
    BottomDestination(Routes.INSIGHTS, "Insights", Icons.Default.Insights),
    BottomDestination(Routes.LIFE, "Life", Icons.Default.Person),
)

/**
 * Root scaffold: bottom navigation for the five sections and a [NavHost] for the
 * section content plus detail screens.
 *
 * Detail routes intentionally hide the bottom bar so the focused task (writing a
 * note, adding an expense) owns the screen.
 */
@Composable
fun LifeOsApp(
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in BOTTOM_DESTINATIONS.map { it.route }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    BOTTOM_DESTINATIONS.forEach { destination ->
                        val selected = backStackEntry?.destination?.hierarchy
                            ?.any { it.route == destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    destination.icon,
                                    contentDescription = destination.label,
                                    tint = when {
                                        destination.isAction -> MaterialTheme.colorScheme.primary
                                        selected -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            },
                            label = { Text(destination.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = LifeOs.tokens.softPinkSurface,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding()),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenTasks = { navController.navigate(Routes.TASKS) },
                    onOpenHabits = { navController.navigate(Routes.HABITS) },
                    onOpenTimeline = { navController.navigate(Routes.TIMELINE) },
                )
            }
            composable(Routes.NOTES) {
                NotesScreen(
                    onOpenNote = { id -> navController.navigate(Routes.noteEditor(id)) },
                    onCreateNote = { navController.navigate(Routes.noteEditor(null)) },
                )
            }
            composable(Routes.CAPTURE) {
                com.lifeos.app.feature.capture.CaptureHost()
            }
            composable(Routes.INSIGHTS) { InsightsScreen() }
            composable(Routes.LIFE) {
                LifeScreen(
                    onOpenTimeline = { navController.navigate(Routes.TIMELINE) },
                    onOpenCalendar = { navController.navigate(Routes.CALENDAR) },
                    onOpenTrash = { navController.navigate(Routes.TRASH) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenTasks = { navController.navigate(Routes.TASKS) },
                    onOpenHabits = { navController.navigate(Routes.HABITS) },
                    onOpenExpenses = { navController.navigate(Routes.EXPENSES) },
                    onOpenDiary = { navController.navigate(Routes.DIARY) },
                    onOpenSearch = { navController.navigate(Routes.SEARCH) },
                )
            }

            composable("${Routes.NOTE_EDITOR}/{noteId}") { entry ->
                val noteId = entry.arguments?.getString("noteId")
                NoteEditorScreen(
                    noteId = noteId,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.TASKS) {
                TasksScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.HABITS) {
                HabitsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.EXPENSES) {
                ExpensesScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.DIARY) {
                DiaryScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.TIMELINE) {
                com.lifeos.app.feature.life.TimelineScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.CALENDAR) {
                com.lifeos.app.feature.life.CalendarScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.TRASH) {
                com.lifeos.app.feature.life.TrashScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SEARCH) {
                SearchScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
