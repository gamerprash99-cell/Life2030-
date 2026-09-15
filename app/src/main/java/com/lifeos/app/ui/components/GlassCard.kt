package com.lifeos.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.blur
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.lifeos.app.ui.theme.LocalGlassColors

/**
 * The single reusable "frosted glass" surface used across LifeOS (Section 53/54).
 *
 * Deliberately used sparingly — for Home dashboard summary cards, AI response
 * cards, and key highlight surfaces — not globally, per the spec's explicit
 * warning against overusing glass effects (readability & performance first).
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 24.dp,
    content: @Composable () -> Unit
) {
    val glass = LocalGlassColors.current
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .shadow(elevation = 12.dp, shape = shape, ambientColor = glass.border, spotColor = glass.border)
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(glass.surface, glass.surface.copy(alpha = glass.surface.alpha * 0.85f))
                )
            )
            .border(width = 1.dp, color = glass.border, shape = shape)
            .padding(20.dp)
    ) {
        content()
    }
}

/**
 * A lighter-weight glass chip for tags/categories, avoiding the heavier shadow
 * of GlassCard so lists of these stay performant.
 */
@Composable
fun GlassChip(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val glass = LocalGlassColors.current
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .clip(shape)
            .background(glass.surface)
            .border(width = 1.dp, color = glass.border, shape = shape)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        content()
    }
}
