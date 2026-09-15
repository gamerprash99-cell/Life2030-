package com.lifeos.app.domain.model

/** "WHERE DID YOU SPEND?" quick-pick grid — Section 14. */
data class ExpenseCategoryDef(val name: String, val emoji: String)

object ExpenseCategories {
    val ALL = listOf(
        ExpenseCategoryDef("Food", "🍔"),
        ExpenseCategoryDef("Cafe", "☕"),
        ExpenseCategoryDef("Shopping", "🛍"),
        ExpenseCategoryDef("Travel", "🚕"),
        ExpenseCategoryDef("Entertainment", "🎮"),
        ExpenseCategoryDef("Education", "📚"),
        ExpenseCategoryDef("Bills", "🏠"),
        ExpenseCategoryDef("Health", "💊"),
        ExpenseCategoryDef("Subscriptions", "📱"),
        ExpenseCategoryDef("Other", "❤️"),
    )

    fun emojiFor(category: String): String = ALL.find { it.name == category }?.emoji ?: "❤️"
}

/** Habit Analytics — Section 12. */
data class HabitAnalytics(
    val habitId: String,
    val currentStreak: Int,
    val longestStreak: Int,
    val completionsThisMonth: Int,
    val totalDaysThisMonth: Int,
    val completionPercentThisMonth: Int,
    val missedDaysThisMonth: Int,
    val totalCompletionsAllTime: Int
)

/** One cell of the GitHub-style heatmap — Section 13. */
enum class HeatmapIntensity { MISSED, PARTIAL, COMPLETED, EXCEPTIONAL, NO_DATA }

data class HeatmapCell(
    val epochDay: Long,
    val intensity: HeatmapIntensity,
    val progressCount: Int,
    val goalCount: Int
)
