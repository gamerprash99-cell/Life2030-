package com.lifeos.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * One motion language for the whole app — Lumen.
 *
 * Heavier and more physical than v4: the palette is warm and tactile, so the
 * motion is too. Surfaces *lift and settle* rather than snap into place, and
 * every duration is a touch longer so the weight reads. Two feels still cover
 * everything: an [Ease] curve for entrances/exits and content swaps, and the
 * [springy] / [snappy] springs for anything the finger touches. [gentle] is the
 * calm default for layout settles (progress, counters, width changes). Nothing
 * bounces or spins for its own sake, and every animation degrades gracefully
 * under reduced motion.
 */
object LifeOSMotion {
    // Durations (ms). A touch slower than v4 so movement reads as weight.
    const val instant = 110
    const val fast = 200
    const val standard = 300
    const val slow = 440
    const val lazy = 620

    /** Material's "emphasized decelerate" — content arriving. */
    val Ease = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

    /** The mirror "emphasized accelerate" — content leaving. */
    val Exit = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

    /** A gentle spring for press-scale feedback — the finger's weight. */
    fun <T> springy(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMediumLow)

    /** A tighter spring for small toggles, chips and tab selection. */
    fun <T> snappy(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMedium)

    /** A calm, non-overshooting spring for layout settles (progress, counters). */
    fun <T> gentle(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 1f, stiffness = Spring.StiffnessLow)

    fun <T> emphasize(): FiniteAnimationSpec<T> = tween(durationMillis = standard, easing = Ease)

    fun <T> enter(): FiniteAnimationSpec<T> = tween(durationMillis = standard, easing = Ease)

    fun <T> exit(): FiniteAnimationSpec<T> = tween(durationMillis = fast, easing = Exit)

    /** A stagger step for list entrance: item [index] arrives [stepMs] after the last. */
    fun staggerDelay(index: Int, stepMs: Int = 50): Int = (index.coerceAtMost(8)) * stepMs
}

/** The shared elevation ladder — long, soft, neutral shadows, never grey slabs or brand smears. */
object LifeOSElevation {
    val none = 0.dp
    val card = 4.dp
    val raised = 10.dp
    val floating = 16.dp
    val sheet = 22.dp
    val hero = 28.dp

    /** The one soft shadow colour. Brand-tinted depth is applied at call sites when wanted. */
    val shadowAlphaRest = 0.12f
    val shadowAlphaLift = 0.20f

    val hairline: Dp = 1.dp
}
