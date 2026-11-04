package com.lifeos.app.ui.diary.new

/**
 * Diary list templates — bullet, numbered, checkbox, dash.
 *
 * Data model extracted from the old `DiaryListPickerSheet.kt` (ui/diary/)
 * so the old UI layer can be removed without losing the template definitions
 * reused by the new sticker/list picker sheets.
 */
object DiaryListTemplates {
    val ALL = listOf(
        DiaryListTemplate("bullet", "Bullet list", "•", "Lists"),
        DiaryListTemplate("numbered", "Numbered list", "1.", "Lists"),
        DiaryListTemplate("checkbox", "Checkbox list", "- [ ]", "Tasks"),
        DiaryListTemplate("dash", "Dash list", "–", "Lists")
    )

    val CATEGORIES = ALL.map { it.category }.distinct()

    fun byCategory(category: String): List<DiaryListTemplate> =
        ALL.filter { it.category == category }
}

/** One selectable list style offered by the list picker sheet. */
data class DiaryListTemplate(
    val key: String,
    val name: String,
    val marker: String,
    val category: String
)
