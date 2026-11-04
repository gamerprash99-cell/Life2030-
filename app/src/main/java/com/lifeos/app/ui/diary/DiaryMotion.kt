package com.lifeos.app.ui.diary

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Restrained enter animation for a whole page region — used when the day being
 * read is empty, so stepping onto a day with no memories resolves the open
 * spine instead of it snapping into place.
 *
 * Deliberately a plain fade with no translation: the empty state is already
 * vertically centred, so a slide would read as the layout moving rather than as
 * the content arriving.
 */
@Composable
fun Modifier.fadeInAsContent(durationMillis: Int = 240): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis = durationMillis, easing = FastOutSlowInEasing))
    }
    return this.graphicsLayer { alpha = progress.value }
}

/**
 * Restrained enter animation for a memory moment: fade in while drifting up a
 * few pixels, staggered by position so a day of entries resolves top-to-bottom
 * instead of appearing all at once.
 *
 * Implemented with an [Animatable] rather than a bouncy enter/exit transition
 * so it is cheap inside a scrolling `LazyColumn` and — because the animation
 * runs in the composition's coroutine context — automatically honours Android's
 * "Remove animations" accessibility setting.
 *
 * The reveal plays exactly once per [key]. A `LazyColumn` disposes and rebuilds
 * an item every time it scrolls off and back on screen; without a remembered
 * flag the entry would re-run its fade on every return, which reads as the list
 * flickering while you scroll. [rememberSaveable] (backed by the lazy list's own
 * saveable state) is what makes "already revealed" survive that recycling, so
 * scrolling past a memory and back never animates it a second time.
 */
@Composable
fun Modifier.revealAsMemory(index: Int = 0, key: Any? = index): Modifier {
    var hasRevealed by rememberSaveable(key) { mutableStateOf(false) }
    val progress = remember { Animatable(if (hasRevealed) 1f else 0f) }
    LaunchedEffect(key) {
        if (hasRevealed) {
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        // Cap the stagger so a long day never makes the last entry feel late.
        if (index > 0) delay((index.coerceAtMost(6) * 45).toLong())
        progress.animateTo(1f, tween(durationMillis = 280, easing = FastOutSlowInEasing))
        hasRevealed = true
    }
    return this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 14.dp.toPx()
    }
}
