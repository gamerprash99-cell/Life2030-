package com.lifeos.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * One motion language for the whole app — Aurora Noir.
 *
 * Motion is smooth and subtle; nothing bounces, flashes or spins for its own
 * sake. Two feels cover everything: an [Ease] curve for entrances/exits and
 * content swaps, and the [springy] / [snappy] springs for anything the finger
 * touches, so direct manipulation always feels physical. [gentle] is the calm
 * default for layout settles (progress, counters, width changes).
 */
object LifeOSMotion {
    // Durations (ms). Fast enough to feel instant, slow enough to read as motion.
    const val instant = 90
    const val fast = 160
    const val standard = 240
    const val slow = 360
    const val lazy = 520

    /** Material's "emphasized decelerate" — content arriving. */
    val Ease = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

    /** The mirror "emphasized accelerate" — content leaving. */
    val Exit = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

    /** A gentle spring for press-scale feedback. */
    fun <T> springy(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)

    /** A tighter spring for small toggles, chips and tab selection. */
    fun <T> snappy(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessHigh)

    /** A calm, non-overshooting spring for layout settles (progress, counters). */
    fun <T> gentle(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 1f, stiffness = Spring.StiffnessLow)

    fun <T> emphasize(): FiniteAnimationSpec<T> = tween(durationMillis = standard, easing = Ease)

    fun <T> enter(): FiniteAnimationSpec<T> = tween(durationMillis = standard, easing = Ease)

    fun <T> exit(): FiniteAnimationSpec<T> = tween(durationMillis = fast, easing = Exit)

    /** A stagger step for list entrance: item [index] arrives [stepMs] after the last. */
    fun staggerDelay(index: Int, stepMs: Int = 45): Int = (index.coerceAtMost(8)) * stepMs
}

/** The shared elevation ladder — soft, neutral shadows, never grey slabs or brand smears. */
object LifeOSElevation {
    val none = 0.dp
    val card = 3.dp
    val raised = 7.dp
    val floating = 12.dp
    val sheet = 18.dp
    val hero = 24.dp

    /** The one soft shadow colour. Brand-tinted depth is applied at call sites when wanted. */
    val shadowAlphaRest = 0.10f
    val shadowAlphaLift = 0.16f

    val hairline: Dp = 1.dp
}
