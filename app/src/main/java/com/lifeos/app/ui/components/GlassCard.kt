package com.lifeos.app.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.lifeos.app.ui.theme.LifeOSRadius

/**
 * A matte surface riding the Lumen material. These are thin wrappers over
 * [`liquidGlass`] so every existing caller (the bottom navigation, the Home
 * header, Timeline/Expenses/Habits cards) automatically inherits the full matte
 * treatment — the opaque warm sheet, the whisper of tint, the faint upper wash,
 * the pooled base shadow and the single warm hairline — instead of a flat
 * rectangle. The wrapper names are legacy; the material is not.
 */

/** Apply the matte surface chrome directly to a modifier (persistent chrome, headers). */
@Composable
fun Modifier.glassSurface(
    shape: Shape = RoundedCornerShape(LifeOSRadius.xlarge),
    tint: androidx.compose.ui.graphics.Color? = null
): Modifier = this.liquidGlass(shape = shape, tint = tint)

/** A matte card. Body/list surfaces are opaque, so content stays legible against the ambient wash. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = LifeOSRadius.xlarge,
    content: @Composable () -> Unit
) {
    LiquidGlassSurface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        contentPadding = PaddingValues(20.dp)
    ) { content() }
}

/** A matte chip with a stronger accent tint when selected. */
@Composable
fun GlassChip(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    content: @Composable () -> Unit
) {
    val tint = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        null
    }
    LiquidGlassChip(modifier = modifier, selected = selected, tint = tint) {
        content()
    }
}