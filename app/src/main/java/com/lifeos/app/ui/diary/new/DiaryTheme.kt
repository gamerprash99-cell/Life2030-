package com.lifeos.app.ui.diary.new

import androidx.compose.runtime.Composable
import com.lifeos.app.ui.theme.LifeOSTheme

/**
 * Diary theme wrapper — pins Diary screens to the light scheme so diary
 * text renders as dark pen ink on paper regardless of the system theme.
 *
 * Extracted from `DiaryPaper.kt` (ui/diary/) so the old UI layer can be
 * removed without breaking the nav host and new screens that use it.
 */
@Composable
fun DiaryTheme(content: @Composable () -> Unit) {
    LifeOSTheme(darkTheme = false, content = content)
}
