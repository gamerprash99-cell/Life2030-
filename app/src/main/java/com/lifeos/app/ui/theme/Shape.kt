package com.lifeos.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * The one corner-radius scale for the whole app — Lumen.
 *
 * Softer and more pillowy than v4: the warmth of the palette asks for a
 * gentler curve, so every step is a few dp rounder while controls stay
 * tighter than cards so they still read as pressable. [small] covers chips
 * and image tiles, [medium] controls and small panels, [large] cards and
 * tiles, [xlarge] hero/primary surfaces, and [sheet] the full-width bottom
 * sheet. Pills stay fully round. The diary keeps its own paper geometry
 * (see `DiaryPaper.kt`).
 */
object LifeOSRadius {
    val small = 16.dp
    val medium = 22.dp
    val large = 28.dp
    val xlarge = 34.dp

    /** A full-width bottom sheet / capture surface — the largest radius in the app. */
    val sheet = 40.dp
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
