package com.lifeos.app.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Spacing scale: 4dp grid. Screens must use these, not ad-hoc values. */
object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val huge = 40.dp
}

/** Corner radii: large, calm, consistent. */
object Radii {
    val sm = 10.dp
    val md = 14.dp
    val lg = 20.dp
    val xl = 24.dp
    val pill = 500.dp
}

/** Fully rounded shape for chips/tags/pills. */
val PillShape = RoundedCornerShape(Radii.pill)

/** Minimum interactive size per accessibility guidance. */
val MinTouchTarget = 48.dp

/** Standard control heights. */
object ControlHeight {
    val button = 50.dp
    val field = 52.dp
    val chip = 34.dp
}

val LifeOsShapes = Shapes(
    extraSmall = RoundedCornerShape(Radii.sm),
    small = RoundedCornerShape(Radii.md),
    medium = RoundedCornerShape(Radii.lg),
    large = RoundedCornerShape(Radii.xl),
    extraLarge = RoundedCornerShape(Radii.xl),
)

/** Motion durations (kept short; battery and calmness first). */
object Motion {
    const val shortMs = 120
    const val standardMs = 200
    const val sheetMs = 260
}
