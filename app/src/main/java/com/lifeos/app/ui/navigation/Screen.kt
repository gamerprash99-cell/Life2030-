package com.lifeos.app.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Notes : Screen("notes")
    object NoteEditor : Screen("notes/editor?noteId={noteId}") {
        fun createRoute(noteId: String? = null) = "notes/editor?noteId=${noteId ?: ""}"
    }
    object Tasks : Screen("tasks")
    object Habits : Screen("habits")
    object HabitDetail : Screen("habits/{habitId}") {
        fun createRoute(habitId: String) = "habits/$habitId"
    }
    object Expenses : Screen("expenses")
    object AddExpense : Screen("expenses/add")
    object Diary : Screen("diary")
    object DiaryEditor : Screen("diary/editor?entryId={entryId}") {
        fun createRoute(entryId: String? = null) = "diary/editor?entryId=${entryId ?: ""}"
    }
    object Timeline : Screen("timeline")
    object CaptureDetail : Screen("capture/{captureId}") {
        fun createRoute(captureId: String) = "capture/$captureId"
    }
    object Insights : Screen("insights")
    object Search : Screen("search")
    object AiAssistant : Screen("ai_assistant")
    object Settings : Screen("settings")
    object AppLockSetup : Screen("settings/app_lock")

    companion object {
        /** Bottom nav destinations, in display order — Section 5/57. */
        val bottomNavItems = listOf(Home, Timeline, Tasks, Habits, Settings)
    }
}
