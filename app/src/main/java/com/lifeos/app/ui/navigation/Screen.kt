package com.lifeos.app.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Tasks : Screen("tasks")
    object Habits : Screen("habits")
    object HabitDetail : Screen("habits/{habitId}") {
        fun createRoute(habitId: String) = "habits/$habitId"
    }
    object Expenses : Screen("expenses")
    object Diary : Screen("diary")
    object DiaryDetail : Screen("diary/{entryId}") {
        fun createRoute(entryId: String) = "diary/$entryId"
    }
    object Timeline : Screen("timeline")
    object Settings : Screen("settings")
    object Profile : Screen("profile")
    object AppLockSetup : Screen("settings/app_lock")
}
