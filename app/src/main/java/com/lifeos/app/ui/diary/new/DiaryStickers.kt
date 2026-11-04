package com.lifeos.app.ui.diary.new

/**
 * Diary sticker definitions — emoji stickers grouped by category.
 *
 * Data model extracted from the old `DiaryStickerPickerSheet.kt` (ui/diary/)
 * so the old UI layer can be removed without losing the sticker definitions
 * reused by the new sticker picker sheet.
 */
data class DiaryStickerDef(
    val assetKey: String,
    val label: String,
    val glyph: String,
    val category: String
)

object DiaryStickers {
    val ALL = listOf(
        DiaryStickerDef("nature_leaf", "Leaf", "🍃", "Nature"),
        DiaryStickerDef("nature_flower", "Flower", "🌸", "Nature"),
        DiaryStickerDef("nature_sun", "Sun", "☀️", "Nature"),
        DiaryStickerDef("nature_moon", "Moon", "🌙", "Nature"),
        DiaryStickerDef("nature_star", "Star", "⭐", "Nature"),
        DiaryStickerDef("feel_heart", "Heart", "❤️", "Feelings"),
        DiaryStickerDef("feel_sparkle", "Sparkle", "✨", "Feelings"),
        DiaryStickerDef("feel_cloud", "Cloud", "☁️", "Feelings"),
        DiaryStickerDef("feel_rainbow", "Rainbow", "🌈", "Feelings"),
        DiaryStickerDef("act_book", "Book", "📖", "Activities"),
        DiaryStickerDef("act_coffee", "Coffee", "☕", "Activities"),
        DiaryStickerDef("act_camera", "Camera", "📷", "Activities"),
        DiaryStickerDef("act_music", "Music", "🎵", "Activities"),
        DiaryStickerDef("sym_check", "Check", "✅", "Symbols"),
        DiaryStickerDef("sym_pin", "Pin", "📌", "Symbols"),
        DiaryStickerDef("sym_flag", "Flag", "🚩", "Symbols")
    )

    val CATEGORIES = ALL.map { it.category }.distinct()

    fun byCategory(category: String): List<DiaryStickerDef> =
        ALL.filter { it.category == category }

    fun byKey(key: String): DiaryStickerDef =
        ALL.firstOrNull { it.assetKey == key } ?: DiaryStickerDef("placeholder", "Sticker", "●", "Symbols")
}
