package com.lifeos.app.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.lifeos.app.ui.components.LifeOSBottomBar
import com.lifeos.app.ui.diary.DiaryDetailScreen
import com.lifeos.app.ui.diary.DiaryScreen
import com.lifeos.app.ui.expenses.ExpensesScreen
import com.lifeos.app.ui.habits.HabitDetailScreen
import com.lifeos.app.ui.habits.HabitsScreen
import com.lifeos.app.ui.home.HomeScreen
import com.lifeos.app.ui.profile.ProfileScreen
import com.lifeos.app.ui.security.AppLockSetupScreen
import com.lifeos.app.ui.settings.SettingsScreen
import com.lifeos.app.ui.tasks.TasksScreen
import com.lifeos.app.ui.timeline.TimelineScreen

/**
 * Route of the nested graph that owns the primary (bottom-navigation)
 * destinations. Keeping them inside one graph lets the bottom bar pop the
 * whole tab stack in a single hop (`popUpTo(rootGraph) { saveState = true }`)
 * without ever leaving duplicate tab destinations on the back stack.
 *
 * Child/secondary screens (Habit Detail, Expense, Diary, …) live OUTSIDE this
 * graph so they stack naturally on top of the active tab and Android's system
 * Back dismisses them one level at a time. See [ROOT_TABS_GRAPH].
 */
// Local opt-in, not the propagating marker: `isImeVisible` is read in this file
// only, and callers (MainActivity) must not be forced to opt in as well.
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun LifeOSNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()

    // The Diary composer is the app's only full-screen text surface and it
    // consumes the IME inset itself. While the keyboard is up the bottom bar is
    // hidden behind it yet still reserves its own height in the Scaffold's
    // padding — and the composer's `imePadding()` then lifts the content by that
    // same height a second time. That double count is the blank band the height
    // of the navigation bar between the editor and the keyboard.
    //
    // So on exactly the two composer routes the bar is not composed while the
    // IME is visible, and the content is given no bottom padding to double
    // count. With the keyboard closed — and on every other destination — the
    // bar and the padding are exactly what they were before.
    val route = backStackEntry?.destination?.route
    val composerOwnsWindow = (route == Screen.Diary.route || route == Screen.DiaryDetail.route) &&
        WindowInsets.isImeVisible

    Scaffold(
        bottomBar = { if (!composerOwnsWindow) LifeOSBottomBar(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = ROOT_TABS_GRAPH,
            modifier = Modifier.padding(
                top = padding.calculateTopPadding(),
                // Set explicitly rather than trusting the Scaffold's fallback:
                // when the bar is absent the Scaffold would otherwise hand back
                // its content-window-inset bottom, which is still a gap the
                // composer must not inherit.
                bottom = if (composerOwnsWindow) 0.dp else padding.calculateBottomPadding()
            )
        ) {
            navigation(startDestination = Screen.Home.route, route = ROOT_TABS_GRAPH) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        onOpenTasks = { navController.navigate(Screen.Tasks.route) { launchSingleTop = true } },
                        onOpenHabits = { navController.navigate(Screen.Habits.route) { launchSingleTop = true } },
                        onOpenExpenses = { navController.navigate(Screen.Expenses.route) { launchSingleTop = true } },
                        onOpenDiary = { navController.navigate(Screen.Diary.route) { launchSingleTop = true } },
                        onOpenTimeline = { navController.navigate(Screen.Timeline.route) { launchSingleTop = true } },
                        onOpenProfile = { navController.navigate(Screen.Profile.route) { launchSingleTop = true } }
                    )
                }
                composable(Screen.Tasks.route) { TasksScreen() }
                composable(Screen.Habits.route) {
                    HabitsScreen(onOpenHabit = { habitId -> navController.navigate(Screen.HabitDetail.createRoute(habitId)) { launchSingleTop = true } })
                }
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
                    onOpenEntry = { entryId -> navController.navigate(Screen.DiaryDetail.createRoute(entryId)) { launchSingleTop = true } }
                )
            }
            composable(
                Screen.DiaryDetail.route,
                arguments = listOf(navArgument("entryId") { type = NavType.StringType })
            ) { entry ->
                val entryId = entry.arguments?.getString("entryId").orEmpty()
                DiaryDetailScreen(
                    entryId = entryId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Timeline.route) {
                TimelineScreen(
                    onBack = { navController.popBackStack() },
                    onOpenItem = { item ->
                        val route = when (item.type) {
                            com.lifeos.app.domain.model.TimelineItemType.DIARY ->
                                Screen.DiaryDetail.createRoute(item.sourceId)
                            com.lifeos.app.domain.model.TimelineItemType.TASK_COMPLETED -> Screen.Tasks.route
                            com.lifeos.app.domain.model.TimelineItemType.HABIT_COMPLETED -> Screen.Habits.route
                            com.lifeos.app.domain.model.TimelineItemType.EXPENSE -> Screen.Expenses.route
                        }
                        navController.navigate(route) { launchSingleTop = true }
                    }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onBack = { navController.popBackStack() },
                    onOpenSettings = { navController.navigate(Screen.Settings.route) { launchSingleTop = true } },
                    onOpenAppLock = { navController.navigate(Screen.AppLockSetup.route) { launchSingleTop = true } }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onOpenAppLockSetup = { navController.navigate(Screen.AppLockSetup.route) { launchSingleTop = true } },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.AppLockSetup.route) {
                AppLockSetupScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}