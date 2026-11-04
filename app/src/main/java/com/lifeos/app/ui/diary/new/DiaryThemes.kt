package com.lifeos.app.ui.diary.new

import androidx.compose.ui.graphics.Color

/**
 * Diary theme/background definitions — bundled presentation themes for entries.
 *
 * Data model extracted from the old `DiaryThemePickerSheet.kt` (ui/diary/)
 * so the old UI layer can be removed without losing the theme definitions
 * reused by the new theme picker sheet.
 */
data class DiaryThemeDef(
    val key: String,
    val name: String,
    val category: String,
    val paperBase: Color,
    val paperCard: Color,
    val ruledLine: Color,
    val marginLine: Color,
    val isDark: Boolean = false
)

object DiaryThemes {
    val ORIGINAL = DiaryThemeDef(
        key = "original", name = "Original", category = "Customize",
        paperBase = Color(0xFFEFE6D2), paperCard = Color(0xFFFCF7EA),
        ruledLine = Color(0xFFD3C4A3), marginLine = Color(0xFFC4796A)
    )
    val PASTEL_PINK = DiaryThemeDef(
        key = "pastel_pink", name = "Pastel Pink", category = "Popular",
        paperBase = Color(0xFFFDF0F5), paperCard = Color(0xFFFFF5F8),
        ruledLine = Color(0xFFEACFD9), marginLine = Color(0xFFE39AA3)
    )
    val WARM_CREAM = DiaryThemeDef(
        key = "warm_cream", name = "Warm Cream", category = "Popular",
        paperBase = Color(0xFFFDF6EC), paperCard = Color(0xFFFFFBF0),
        ruledLine = Color(0xFFEED9B4), marginLine = Color(0xFFE3B56A)
    )
    val DEEP_PAPER = DiaryThemeDef(
        key = "deep_paper", name = "Deep Paper", category = "Dark",
        paperBase = Color(0xFF2A2330), paperCard = Color(0xFF3A3040),
        ruledLine = Color(0xFF4A3F50), marginLine = Color(0xFF8A7BA0), isDark = true
    )
    val ARTSY_WASH = DiaryThemeDef(
        key = "artsy_wash", name = "Artsy Wash", category = "Artsy",
        paperBase = Color(0xFFF3E5F5), paperCard = Color(0xFFF9EFFD),
        ruledLine = Color(0xFFE1BEE7), marginLine = Color(0xFFCE93D8)
    )
    val CUTE_MINT = DiaryThemeDef(
        key = "cute_mint", name = "Cute Mint", category = "Cute",
        paperBase = Color(0xFFE8F5F0), paperCard = Color(0xFFFEFFF9),
        ruledLine = Color(0xFFB2DFDB), marginLine = Color(0xFF80CBC4)
    )
    val LIFESTYLE_SAGE = DiaryThemeDef(
        key = "lifestyle_sage", name = "Lifestyle Sage", category = "Lifestyle",
        paperBase = Color(0xFFF1F4EC), paperCard = Color(0xFFF9FDF2),
        ruledLine = Color(0xFFDCEDC8), marginLine = Color(0xFF9CCC65)
    )

    val CATEGORIES = listOf("Customize", "Popular", "Dark", "Artsy", "Cute", "Lifestyle")
    val ALL = listOf(ORIGINAL, PASTEL_PINK, WARM_CREAM, DEEP_PAPER, ARTSY_WASH, CUTE_MINT, LIFESTYLE_SAGE)

    fun byKey(key: String): DiaryThemeDef = ALL.firstOrNull { it.key == key } ?: ORIGINAL
    fun byCategory(category: String): List<DiaryThemeDef> = ALL.filter { it.category == category }
}
