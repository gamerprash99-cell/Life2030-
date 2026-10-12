package com.lifeos.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * The one corner-radius scale for the whole app.
 *
 * Before this, corners were written out per call site — 8, 12, 14, 18, 22, 24,
 * 28, 30 — so two cards sitting side by side could disagree about how round a
 * card is. Four steps cover every surface the app actually has: [small] for
 * chips and image tiles, [medium] for controls and small panels, [large] for
 * cards and tiles, and [xlarge] for hero/primary surfaces. [pill] is the fully
 * rounded shape for buttons and chips.
 *
 * The diary keeps its own paper geometry (see `DiaryPaper.kt`): paper has
 * corners, app cards do not, so the diary is deliberately outside this scale.
 */
object LifeOSRadius {
    val small = 12.dp
    val medium = 16.dp
    val large = 22.dp
    val xlarge = 28.dp

    /** A full-width bottom sheet / capture surface — the largest radius in the app. */
    val sheet = 34.dp
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
