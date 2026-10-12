package com.lifeos.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * The one corner-radius scale for the whole app — Prism Glass.
 *
 * Pebble-like curvature: cards and sheets are generously round, controls are
 * tighter so they still read as pressable, and pills are fully round. Four
 * steps cover every surface the app actually has: [small] for chips and image
 * tiles, [medium] for controls and small panels, [large] for cards and tiles,
 * and [xlarge] for hero/primary surfaces. [sheet] is the full-width bottom
 * sheet. The diary keeps its own paper geometry (see `DiaryPaper.kt`).
 */
object LifeOSRadius {
    val small = 14.dp
    val medium = 18.dp
    val large = 24.dp
    val xlarge = 30.dp

    /** A full-width bottom sheet / capture surface — the largest radius in the app. */
    val sheet = 36.dp
    val pill = 50.dp
}

val LifeOSShapes = Shapes(
    extraSmall = RoundedCornerShape(LifeOSRadius.small),
    small = RoundedCornerShape(LifeOSRadius.medium),
    medium = RoundedCornerShape(LifeOSRadius.large),
    large = RoundedCornerShape(LifeOSRadius.xlarge),
    extraLarge = RoundedCornerShape(LifeOSRadius.sheet),
)

/** The rounded shape for a control or tile at [radius] from [LifeOSRadius]. */
fun lifeOSRounded(radius: androidx.compose.ui.unit.Dp): RoundedCornerShape = RoundedCornerShape(radius)
