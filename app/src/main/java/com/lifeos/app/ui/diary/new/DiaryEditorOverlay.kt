package com.lifeos.app.ui.diary.new

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable

/**
 * Animated overlay for the Diary editor. Reused by the new Diary home and detail
 * screens when they swap the editor in place.
 *
 * Extracted from the old `DiaryEditor.kt` (ui/diary/) so the old UI layer can be
 * removed without breaking the new screens that still use it.
 */
@Composable
fun DiaryEditorOverlay(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(androidx.compose.animation.core.tween(220)) + slideInVertically(tween(300)) { it / 14 },
        exit = fadeOut(tween(160)) + slideOutVertically(tween(200)) { it / 20 }
    ) { content() }
}
