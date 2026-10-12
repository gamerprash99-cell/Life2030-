package com.lifeos.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
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
import com.lifeos.app.ui.ai.AiAction
import com.lifeos.app.ui.ai.AiAssistantScreen
import com.lifeos.app.ui.capture.CaptureHubScreen
import com.lifeos.app.ui.diary.DiaryCalendarScreen
import com.lifeos.app.ui.diary.DiaryCalendarViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.ui.diary.DiaryDetailScreen
import com.lifeos.app.ui.diary.DiaryInsightsScreen
import com.lifeos.app.ui.diary.DiaryInsightsViewModel
import com.lifeos.app.ui.diary.DiaryScreen
import com.lifeos.app.ui.diary.DiarySearchScreen
import com.lifeos.app.ui.diary.DiarySearchViewModel
import com.lifeos.app.ui.diary.DiaryTheme
import com.lifeos.app.ui.expenses.ExpensesScreen
import com.lifeos.app.ui.habits.HabitDetailScreen
import com.lifeos.app.ui.habits.HabitsScreen
import com.lifeos.app.ui.home.HomeScreen
import com.lifeos.app.ui.life.LifeScreen
import com.lifeos.app.ui.notes.NoteEditorScreen
import com.lifeos.app.ui.notes.NotesScreen
import com.lifeos.app.ui.profile.ProfileScreen
import com.lifeos.app.ui.search.GlobalSearchScreen
import com.lifeos.app.ui.security.AppLockSetupScreen
import com.lifeos.app.ui.settings.SettingsScreen
import com.lifeos.app.ui.snap.SnapScreen
import com.lifeos.app.ui.snap.SnapViewerScreen
import com.lifeos.app.ui.tasks.TasksScreen
import com.lifeos.app.ui.timeline.TimelineScreen

/**
 * Whether the full-screen composer (New / Edit Memory) currently has the window.
 *
 * The composer is rendered as an in-place branch *inside* a destination
 * ([com.lifeos.app.ui.diary.DiaryScreen] and DiaryDetailScreen swap it in place),
 * so `LifeOSNavHost` can see the route but never "is the composer open". Without
 * this holder the bottom bar could only be hidden while the keyboard happened to
 * be up, which is why the editor competed with the navigation and lost ~100dp of
 * height every time the keyboard closed.
 *
 * One holder is created per `LifeOSNavHost` and provided to the whole tree, so
 * the read below invalidates only the Scaffold's slot — not the destinations.
 */
@Stable
class ComposerChromeState internal constructor() {
    var open by mutableStateOf(false)
}

val LocalComposerChrome = staticCompositionLocalOf<ComposerChromeState> { ComposerChromeState() }

/**
 * Declares that the calling composable is the full-screen composer and therefore
 * owns the whole window: [LifeOSNavHost] then keeps the bottom bar off and hands
 * the content no bottom padding, for as long as the composer stays composed —
 * with the keyboard up *and* with it down.
 *
 * Call this once from the composer itself, so both of its entry points (the Diary
 * list and the memory detail screen) are covered by construction. It changes no
 * navigation: Back still goes through the composer's existing `onDismiss`.
 */
@Composable
fun ComposerWindowOwner() {
    val chrome = LocalComposerChrome.current
    DisposableEffect(Unit) {
        chrome.open = true
        onDispose { chrome.open = false }
    }
}

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
@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun LifeOSNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()

    // The Diary composer is the app's only full-screen text surface and it
    // consumes the window's bottom inset itself (see DiaryEditor). While it is
    // open the bottom bar must not be composed and the content must be given no
    // bottom padding — otherwise the bar's height is subtracted from the
    // composer's box while the bar itself is also painted below the writing
    // surface, and the editor reads as a panel inside the navigation instead of
    // as a focused editing flow.
    //
    // With the composer closed — and on every other destination — the bar and the
    // Scaffold's padding are exactly what they were before.
    val chrome = LocalComposerChrome.current
    val route = backStackEntry?.destination?.route
    val composerOwnsWindow = chrome.open &&
        (route == Screen.Diary.route || route == Screen.DiaryDetail.route)

    // One rule, one place: the Diary composer owns the whole window while it is
    // open, and App Lock setup is a security flow that must not carry the
    // primary tab bar at all (see BottomBarVisibility.kt). Both are evaluated by
    // a pure function so the behaviour is unit-testable.
    val showBottomBar = shouldShowBottomBar(route, composerOwnsWindow)

    CompositionLocalProvider(LocalComposerChrome provides chrome) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            bottomBar = { if (showBottomBar) LifeOSBottomBar(navController) }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = ROOT_TABS_GRAPH,
                modifier = Modifier.padding(
                    top = padding.calculateTopPadding(),
                    // Two different "no bottom bar" cases need two different
                    // numbers. While the Diary composer is open it consumes the
                    // bottom inset itself, so the Scaffold's fallback (which is
                    // still a gap the composer must not inherit) is zeroed
                    // explicitly. When a route merely *hides* the bar — App Lock
                    // setup — the Scaffold already reports the system navigation
                    // inset instead, which is exactly what that flow needs.
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
                            onOpenProfile = { navController.navigate(Screen.Profile.route) { launchSingleTop = true } },
                            onOpenSnap = { navController.navigate(Screen.Snap.route) { launchSingleTop = true } },
                            onOpenSnapItem = { captureId ->
                                navController.navigate(Screen.SnapView.createRoute(captureId)) { launchSingleTop = true }
                            },
                            onOpenSearch = { navController.navigate(Screen.GlobalSearch.route) { launchSingleTop = true } },
                            onOpenAi = { navController.navigate(Screen.AiAssistant.route) { launchSingleTop = true } }
                        )
                    }

                    // ---- Primary tabs (Aurora Noir IA) -------------------------
                    composable(Screen.Notes.route) {
                        NotesScreen(
                            onOpenNote = { noteId ->
                                navController.navigate(Screen.NoteEditor.createRoute(noteId)) { launchSingleTop = true }
                            }
                        )
                    }
                    composable(Screen.Life.route) {
                        LifeScreen(
                            onOpenTimeline = { navController.navigate(Screen.Timeline.route) { launchSingleTop = true } },
                            onOpenSearch = { navController.navigate(Screen.GlobalSearch.route) { launchSingleTop = true } },
                            onOpenDiary = { navController.navigate(Screen.Diary.route) { launchSingleTop = true } },
                            onOpenTasks = { navController.navigate(Screen.Tasks.route) { launchSingleTop = true } },
                            onOpenHabits = { navController.navigate(Screen.Habits.route) { launchSingleTop = true } },
                            onOpenExpenses = { navController.navigate(Screen.Expenses.route) { launchSingleTop = true } }
                        )
                    }

                    // You — the personal hub (profile, app lock, backup and
                    // settings). It replaces the removed Insights tab as the
                    // bottom bar's fourth slot, so "your space" is one tap from
                    // anywhere instead of an overlay pushed from Home.
                    composable(Screen.Profile.route) {
                        ProfileScreen(
                            onOpenSettings = { navController.navigate(Screen.Settings.route) { launchSingleTop = true } },
                            onOpenAppLock = { navController.navigate(Screen.AppLockSetup.route) { launchSingleTop = true } }
                        )
                    }
                }

                // ---- Track modules (stacked above the tabs) ----------------
                composable(Screen.Tasks.route) { TasksScreen() }
                composable(Screen.Habits.route) {
                    HabitsScreen(onOpenHabit = { habitId -> navController.navigate(Screen.HabitDetail.createRoute(habitId)) { launchSingleTop = true } })
                }

                // ---- Redesign additions ------------------------------------
                composable(Screen.Capture.route) {
                    CaptureHubScreen(
                        onClose = { navController.popBackStack() },
                        onOpenCamera = { navController.navigate(Screen.Snap.route) { launchSingleTop = true } }
                    )
                }
                composable(
                    Screen.NoteEditor.route,
                    arguments = listOf(navArgument("noteId") { type = NavType.StringType })
                ) { entry ->
                    val noteId = entry.arguments?.getString("noteId").orEmpty()
                    NoteEditorScreen(noteId = noteId, onBack = { navController.popBackStack() })
                }
                composable(Screen.GlobalSearch.route) {
                    GlobalSearchScreen(
                        onBack = { navController.popBackStack() },
                        onOpenNote = { noteId -> navController.navigate(Screen.NoteEditor.createRoute(noteId)) { launchSingleTop = true } },
                        onOpenDiary = { entryId -> navController.navigate(Screen.DiaryDetail.createRoute(entryId)) { launchSingleTop = true } },
                        onOpenCapture = { captureId -> navController.navigate(Screen.SnapView.createRoute(captureId)) { launchSingleTop = true } }
                    )
                }
                composable(Screen.AiAssistant.route) {
                    AiAssistantScreen(
                        onBack = { navController.popBackStack() },
                        onAction = { action ->
                            val route = when (action) {
                                AiAction.LIFE -> Screen.Life.route
                                AiAction.DIARY -> Screen.Diary.route
                                AiAction.TASKS -> Screen.Tasks.route
                                AiAction.HABITS -> Screen.Habits.route
                                AiAction.EXPENSES -> Screen.Expenses.route
                                AiAction.SEARCH -> Screen.GlobalSearch.route
                            }
                            navController.navigate(route) { launchSingleTop = true }
                        }
                    )
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
                    DiaryTheme {
                        DiaryScreen(
                            onBack = { navController.popBackStack() },
                            onOpenEntry = { entryId -> navController.navigate(Screen.DiaryDetail.createRoute(entryId)) { launchSingleTop = true } },
                            onOpenSearch = { navController.navigate(Screen.DiarySearch.route) { launchSingleTop = true } },
                            onOpenCalendar = { navController.navigate(Screen.DiaryCalendar.route) { launchSingleTop = true } },
                            onOpenInsights = { navController.navigate(Screen.DiaryInsights.route) { launchSingleTop = true } }
                        )
                    }
                }

                // Each of these owns its own ViewModel, created through the same
                // manual ServiceLocator as every other screen. Reading the locator
                // inside the destination's own composable (rather than hoisting
                // the ViewModels up here) is what scopes them to the back-stack
                // entry: popping the screen discards its state, which is what you
                // want of a search — leaving would not expect the old query to be
                // sitting there when you came back.
                composable(Screen.DiarySearch.route) {
                    val locator = LocalServiceLocator.current
                    val searchViewModel: DiarySearchViewModel = viewModel(
                        factory = LambdaViewModelFactory {
                            DiarySearchViewModel(locator.searchDiaryEntriesUseCase)
                        }
                    )
                    val searchState by searchViewModel.state.collectAsStateWithLifecycle()
                    DiaryTheme {
                        DiarySearchScreen(
                            state = searchState,
                            onQueryChange = searchViewModel::onQueryChange,
                            onMoodFilterChange = searchViewModel::onMoodFilterChange,
                            onSubmit = searchViewModel::onSubmit,
                            onClearQuery = searchViewModel::onClearQuery,
                            onBack = { navController.popBackStack() },
                            onOpenEntry = { entryId ->
                                navController.navigate(Screen.DiaryDetail.createRoute(entryId)) { launchSingleTop = true }
                            }
                        )
                    }
                }

                composable(Screen.DiaryCalendar.route) {
                    val locator = LocalServiceLocator.current
                    val calendarViewModel: DiaryCalendarViewModel = viewModel(
                        factory = LambdaViewModelFactory {
                            DiaryCalendarViewModel(locator.getDiaryCalendarUseCase)
                        }
                    )
                    val calendarState by calendarViewModel.state.collectAsStateWithLifecycle()
                    DiaryTheme {
                        DiaryCalendarScreen(
                            state = calendarState,
                            onSelectDay = calendarViewModel::selectDay,
                            onPreviousMonth = calendarViewModel::previousMonth,
                            onNextMonth = calendarViewModel::nextMonth,
                            onBack = { navController.popBackStack() },
                            onOpenEntry = { entryId ->
                                navController.navigate(Screen.DiaryDetail.createRoute(entryId)) { launchSingleTop = true }
                            }
                        )
                    }
                }

                composable(Screen.DiaryInsights.route) {
                    val locator = LocalServiceLocator.current
                    val insightsViewModel: DiaryInsightsViewModel = viewModel(
                        factory = LambdaViewModelFactory {
                            DiaryInsightsViewModel(locator.getDiaryInsightsUseCase)
                        }
                    )
                    val insightsState by insightsViewModel.state.collectAsStateWithLifecycle()
                    DiaryTheme {
                        DiaryInsightsScreen(
                            state = insightsState,
                            onBack = { navController.popBackStack() },
                            onReload = insightsViewModel::reload
                        )
                    }
                }
                composable(
                    Screen.DiaryDetail.route,
                    arguments = listOf(navArgument("entryId") { type = NavType.StringType })
                ) { entry ->
                    val entryId = entry.arguments?.getString("entryId").orEmpty()
                    DiaryTheme {
                        DiaryDetailScreen(
                            entryId = entryId,
                            onBack = { navController.popBackStack() }
                        )
                    }
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
                                com.lifeos.app.domain.model.TimelineItemType.SNAP ->
                                    Screen.SnapView.createRoute(item.sourceId)
                            }
                            navController.navigate(route) { launchSingleTop = true }
                        }
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
                composable(Screen.Snap.route) {
                    SnapScreen(
                        onClose = { navController.popBackStack() },
                        onPosted = {
                            // Land the user on the Timeline so the snap they just
                            // took is the first thing they see land.
                            navController.popBackStack()
                            navController.navigate(Screen.Timeline.route) { launchSingleTop = true }
                        }
                    )
                }
                composable(
                    Screen.SnapView.route,
                    arguments = listOf(navArgument("captureId") { type = NavType.StringType })
                ) { entry ->
                    val captureId = entry.arguments?.getString("captureId").orEmpty()
                    SnapViewerScreen(captureId = captureId, onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
