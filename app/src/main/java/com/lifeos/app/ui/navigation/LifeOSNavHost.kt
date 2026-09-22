package com.lifeos.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.lifeos.app.ui.ai.AiAssistantScreen
import com.lifeos.app.ui.capture.CaptureDetailScreen
import com.lifeos.app.ui.capture.CaptureSheet
import com.lifeos.app.ui.components.LifeOSBottomBar
import com.lifeos.app.ui.diary.DiaryDetailScreen
import com.lifeos.app.ui.diary.DiaryScreen
import com.lifeos.app.ui.expenses.ExpensesScreen
import com.lifeos.app.ui.habits.HabitDetailScreen
import com.lifeos.app.ui.habits.HabitsScreen
import com.lifeos.app.ui.home.HomeScreen
import com.lifeos.app.ui.insights.InsightsScreen
import com.lifeos.app.ui.notes.NoteEditorScreen
import com.lifeos.app.ui.notes.NotesListScreen
import com.lifeos.app.ui.profile.ProfileScreen
import com.lifeos.app.ui.search.SearchScreen
import com.lifeos.app.ui.security.AppLockSetupScreen
import com.lifeos.app.ui.settings.SettingsScreen
import com.lifeos.app.ui.tasks.TasksScreen
import com.lifeos.app.ui.timeline.TimelineScreen

/**
 * Route of the nested graph that owns the four primary (bottom-navigation)
 * destinations. Keeping them inside one graph lets the bottom bar pop the
 * whole tab stack in a single hop (`popUpTo(rootGraph) { saveState = true }`)
 * without ever leaving duplicate tab destinations on the back stack.
 *
 * Child/secondary screens (Notes Editor, Habit Detail, Expense, Diary, …)
 * live OUTSIDE this graph so they stack naturally on top of the active tab
 * and Android's system Back dismisses them one level at a time.
 */
private const val ROOT_TABS_GRAPH = "root_tabs"

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun LifeOSNavHost() {
    val navController = rememberNavController()
    var showCapture by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = { LifeOSBottomBar(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = ROOT_TABS_GRAPH,
            modifier = Modifier.padding(padding)
        ) {
            navigation(startDestination = Screen.Home.route, route = ROOT_TABS_GRAPH) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        onOpenTasks = { navController.navigate(Screen.Tasks.route) },
                        onOpenHabits = { navController.navigate(Screen.Habits.route) },
                        onOpenCapture = { showCapture = true },
                        onOpenAiAssistant = { navController.navigate(Screen.AiAssistant.route) },
                        onOpenNotes = { navController.navigate(Screen.Notes.route) },
                        onOpenExpenses = { navController.navigate(Screen.Expenses.route) },
                        onOpenDiary = { navController.navigate(Screen.Diary.route) },
                        onOpenInsights = { navController.navigate(Screen.Insights.route) },
                        onOpenSearch = { navController.navigate(Screen.Search.route) },
                        onOpenTimeline = { navController.navigate(Screen.Timeline.route) },
                        onOpenProfile = { navController.navigate(Screen.Profile.route) }
                    )
                }
                composable(Screen.Tasks.route) { TasksScreen() }
                composable(Screen.Habits.route) {
                    HabitsScreen(onOpenHabit = { habitId -> navController.navigate(Screen.HabitDetail.createRoute(habitId)) })
                }
                composable(Screen.Insights.route) { InsightsScreen() }
            }

            composable(Screen.Notes.route) {
                NotesListScreen(
                    onOpenNote = { noteId -> navController.navigate(Screen.NoteEditor.createRoute(noteId)) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                Screen.NoteEditor.route,
                arguments = listOf(navArgument("noteId") { type = NavType.StringType; nullable = true; defaultValue = "" })
            ) { entry ->
                val noteId = entry.arguments?.getString("noteId")?.ifBlank { null }
                NoteEditorScreen(noteId = noteId, onBack = { navController.popBackStack() })
            }
            composable(
                Screen.HabitDetail.route,
                arguments = listOf(navArgument("habitId") { type = NavType.StringType })
            ) { entry ->
                val habitId = entry.arguments?.getString("habitId").orEmpty()
                HabitDetailScreen(habitId = habitId, onBack = { navController.popBackStack() })
            }
            composable(Screen.Expenses.route) { ExpensesScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.Diary.route) {
                DiaryScreen(
                    onBack = { navController.popBackStack() },
                    onOpenEntry = { entryId -> navController.navigate(Screen.DiaryDetail.createRoute(entryId)) }
                )
            }
            composable(
                Screen.DiaryDetail.route,
                arguments = listOf(navArgument("entryId") { type = NavType.StringType })
            ) { entry ->
                val entryId = entry.arguments?.getString("entryId").orEmpty()
                DiaryDetailScreen(entryId = entryId, onBack = { navController.popBackStack() })
            }
            composable(Screen.Timeline.route) {
                TimelineScreen(
                    onBack = { navController.popBackStack() },
                    onOpenCapture = { captureId -> navController.navigate(Screen.CaptureDetail.createRoute(captureId)) }
                )
            }
            composable(
                Screen.CaptureDetail.route,
                arguments = listOf(navArgument("captureId") { type = NavType.StringType })
            ) { entry ->
                val captureId = entry.arguments?.getString("captureId").orEmpty()
                CaptureDetailScreen(captureId = captureId, onBack = { navController.popBackStack() })
            }
            composable(Screen.Search.route) {
                SearchScreen(
                    onBack = { navController.popBackStack() },
                    onOpenHit = { hit -> navController.navigate(hit.category.routeFor(hit.id)) }
                )
            }
            composable(Screen.AiAssistant.route) {
                AiAssistantScreen(
                    onBack = { navController.popBackStack() },
                    onOpenDestination = { destination -> navController.navigate(destination.route()) },
                    onOpenFeedback = { feedback -> navController.navigate(feedback.destination.route()) }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onBack = { navController.popBackStack() },
                    onOpenSettings = { navController.navigate(Screen.Settings.route) },
                    onOpenAppLock = { navController.navigate(Screen.AppLockSetup.route) }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onOpenAppLockSetup = { navController.navigate(Screen.AppLockSetup.route) })
            }
            composable(Screen.AppLockSetup.route) {
                AppLockSetupScreen(onBack = { navController.popBackStack() })
            }
        }
    }

    if (showCapture) {
        CaptureSheet(onDismiss = { showCapture = false })
    }
}