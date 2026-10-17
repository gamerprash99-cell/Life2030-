package com.lifeos.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * The one corner-radius scale for the whole app — the LifeOS 2030 redesign.
 *
 * The redesign's geometry is tighter and more deliberate than the previous
 * Lumen scale: chips and tags take a small 12dp curve, controls (buttons,
 * fields, switches) 16dp, cards and grouped panels 22dp, and the full-window
 * capture surface 30dp. Pills stay fully round. Every step is a few dp tighter
 * than Lumen so a card reads as an engineered panel rather than a soft blob.
 *
 * The token *names* are unchanged, so every existing call site keeps compiling
 * and silently adopts the new geometry. The diary keeps its own paper geometry
 * (see `DiaryPaper.kt`).
 */
object LifeOSRadius {
    /** Tags, small chips, badges. */
    val small = 12.dp
    /** Controls: buttons, fields, switches, icon tiles. */
    val medium = 16.dp
    /** Cards and grouped content panels — the redesign's dominant radius. */
    val large = 22.dp
    /** Larger grouped panels and inner surfaces that sit on a card. */
    val xlarge = 22.dp

    /** A full-width bottom sheet / capture surface — the largest radius in the app. */
    val sheet = 30.dp
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
