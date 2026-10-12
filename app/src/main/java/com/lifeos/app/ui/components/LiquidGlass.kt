package com.lifeos.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lifeos.app.ui.theme.LifeOSRadius
import com.lifeos.app.ui.theme.LifeOSSpacing
import com.lifeos.app.ui.theme.LocalAmbientGlowColors
import com.lifeos.app.ui.theme.LocalGlassColors
import com.lifeos.app.ui.theme.LocalLifeOSSemanticColors
import com.lifeos.app.ui.theme.PrismCoral
import com.lifeos.app.ui.theme.PrismIndigo
import com.lifeos.app.ui.theme.PrismSky

/**
 * Prism Glass — the app's material, rebuilt from zero.
 *
 * A sheet that blurs and refracts the colour behind it. On Android (minSdk 26)
 * there is no real backdrop-blur primitive behind a sibling tree, so the
 * material is *painted* to read as a curved optical lens:
 *
 *  1. a translucent vertical gradient (cooler and brighter where the light
 *     hits from above, deeper at the foot),
 *  2. an optional accent [tint] overlay that picks up the colour beside it,
 *  3. a specular sheen swept across the upper third (the "liquid" highlight),
 *  4. a soft shadow pooled at the base (the curve of the lens),
 *  5. a dual-tone rim — a bright light line along the top edge and a deeper
 *     shadow line along the bottom — drawn by stroking the shape's outline,
 *     clipped to the top/bottom halves.
 *
 * When [interactive] is true the surface *reacts*: a press brightens the sheet,
 * strengthens the tint and lifts the drop shadow. Callers animate the scale
 * themselves through the same press signal — [LiquidGlassSurface],
 * [LiquidGlassButton] and [LifeOSCard] already do.
 *
 * All glass shares one token set and one ambient glow pool, which is what makes
 * separate sheets read as one connected material instead of separate controls.
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(LifeOSRadius.xlarge),
    tint: Color? = null,
    interactive: Boolean = false,
    pressed: Boolean = false,
    elevation: Dp = 6.dp
): Modifier {
    val glass = LocalGlassColors.current
    // Depth is neutral; the material, not the shadow, carries the brand.
    val shadowBase = LocalLifeOSSemanticColors.current.shadow
    val lift = if (pressed) 1f else 0f
    return this
        .shadow(
            elevation = if (pressed) elevation * 1.4f else elevation,
            shape = shape,
            ambientColor = shadowBase.copy(alpha = 0.12f + 0.05f * lift),
            spotColor = shadowBase.copy(alpha = 0.18f + 0.07f * lift)
        )
        .clip(shape)
        .drawBehind {
            val w = size.width
            val h = size.height
            val rimPath = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawBehind)) }

            // 1. The sheet: brighter at the top (light from above), more
            //    translucent at the foot, and the whole sheet warms on press.
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lerp(glass.surface, Color.White, 0.07f + 0.07f * lift),
                        lerp(glass.surface.copy(alpha = glass.surface.alpha * 0.80f), Color.White, 0.02f * lift)
                    ),
                    startY = 0f,
                    endY = h
                ),
                topLeft = Offset.Zero,
                size = Size(w, h)
            )

            // 2. The tint: the accent colour of the content beside the glass.
            if (tint != null) {
                drawRect(
                    color = tint.copy(alpha = 0.12f + 0.10f * lift),
                    topLeft = Offset.Zero,
                    size = Size(w, h)
                )
            }

            // 3. The specular sheen across the upper third — the "liquid" highlight.
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to glass.sheen.copy(alpha = glass.sheen.alpha * (0.60f + 0.30f * lift)),
                        0.30f to Color.Transparent
                    ),
                    startY = 0f,
                    endY = h
                ),
                topLeft = Offset.Zero,
                size = Size(w, h)
            )

            // 4. The pooled base shadow (the lens curves down away from the light).
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.58f to Color.Transparent,
                        1f to glass.innerShadow.copy(alpha = glass.innerShadow.alpha + 0.10f * lift)
                    ),
                    startY = 0f,
                    endY = h
                ),
                topLeft = Offset.Zero,
                size = Size(w, h)
            )

            // 5. The dual-tone rim: a bright light line along the top edge and a
            //    deeper shadow line along the bottom edge.
            clipRect(0f, 0f, w, h * 0.5f) {
                drawPath(rimPath, glass.topEdge, style = Stroke(1.dp.toPx()))
            }
            clipRect(0f, h * 0.5f, w, h) {
                drawPath(rimPath, glass.bottomEdge, style = Stroke(1.2.dp.toPx()))
            }
        }
}

/**
 * A Prism Glass sheet with optional press reactivity: translucent, tint-frosted
 * and, when [onClick] is set, reacting to touch with a spring scale, a warm
 * brighten and a strengthening accent tint.
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(LifeOSRadius.xlarge),
    tint: Color? = null,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.97f else 1f,
        animationSpec = spring(stiffness = 800f, dampingRatio = 0.8f),
        label = "liquidPressScale"
    )
    Box(
        modifier = modifier
            .then(if (onClick != null) Modifier.scale(scale) else Modifier)
            .liquidGlass(shape = shape, tint = tint, interactive = onClick != null, pressed = pressed)
            .then(if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick) else Modifier)
            .padding(contentPadding)
    ) { content() }
}

@Composable
fun LiquidGlassChip(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    tint: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(stiffness = 900f),
        label = "chipPress"
    )
    val shape = RoundedCornerShape(LifeOSRadius.pill)
    Box(
        modifier = modifier
            .scale(scale)
            .liquidGlass(
                shape = shape,
                tint = selectRich(selected, tint),
                interactive = onClick != null,
                pressed = pressed
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
                } else Modifier
            )
            .defaultMinSize(minHeight = 40.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) { content() }
}

/** A fully-rounded Prism Glass button. [prominent] is the spectrum hero action. */
@Composable
fun LiquidGlassButton(
    text: String,
    modifier: Modifier = Modifier,
    prominent: Boolean = true,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(stiffness = 800f, dampingRatio = 0.8f),
        label = "buttonPress"
    )
    val shape = RoundedCornerShape(LifeOSRadius.pill)
    val baseModifier = modifier
        .scale(scale)
        .then(
            if (prominent) {
                Modifier
                    .shadow(
                        elevation = if (pressed) 18.dp else 12.dp,
                        shape = shape,
                        ambientColor = PrismIndigo.copy(alpha = 0.30f),
                        spotColor = PrismSky.copy(alpha = 0.26f)
                    )
                    .clip(shape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                lerp(PrismIndigo, Color.White, 0.12f * if (pressed) 1f else 0f),
                                lerp(PrismSky, Color.White, 0.10f * if (pressed) 1f else 0f)
                            )
                        )
                    )
            } else {
                Modifier.liquidGlass(shape = shape, tint = MaterialTheme.colorScheme.primary, interactive = true, pressed = pressed)
            }
        )
        .clickable(interactionSource = interaction, indication = null, onClick = onClick)
        .defaultMinSize(minHeight = LifeOSSpacing.minTouchTarget)
        .padding(horizontal = 22.dp, vertical = 15.dp)
    Box(modifier = baseModifier, contentAlignment = Alignment.Center) {
        Text(
            text,
            color = if (prominent) Color.White else MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/**
 * The floating primary action, in Prism Glass. The [prominent] variant is the
 * vivid spectrum orb — the identity gradient under a swept specular highlight
 * with the same dual-tone rim as every other sheet. The regular variant is the
 * bare frost, tinted with the primary accent. Both press with a spring scale.
 */
@Composable
fun LiquidGlassFab(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    prominent: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(stiffness = 700f, dampingRatio = 0.72f),
        label = "fabPress"
    )
    val shape = CircleShape
    val glass = LocalGlassColors.current
    val shadowBase = LocalLifeOSSemanticColors.current.shadow
    val lift = if (pressed) 1f else 0f

    val visual = if (prominent) {
        Modifier
            .shadow(
                elevation = if (pressed) 18.dp else 11.dp,
                shape = shape,
                ambientColor = shadowBase.copy(alpha = 0.24f),
                spotColor = PrismIndigo.copy(alpha = 0.30f)
            )
            .clip(shape)
            .background(
                Brush.linearGradient(listOf(PrismIndigo, PrismSky, PrismCoral))
            )
            .drawBehind {
                val w = size.width
                val h = size.height
                val rimPath = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawBehind)) }
                // Specular highlight swept across the top half.
                drawRect(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to glass.sheen.copy(alpha = glass.sheen.alpha * (0.9f + 0.3f * lift)),
                            0.52f to Color.Transparent
                        ),
                        startY = 0f,
                        endY = h
                    ),
                    topLeft = Offset.Zero,
                    size = Size(w, h)
                )
                clipRect(0f, 0f, w, h * 0.5f) { drawPath(rimPath, glass.topEdge, style = Stroke(1.4.dp.toPx())) }
                clipRect(0f, h * 0.5f, w, h) { drawPath(rimPath, glass.bottomEdge, style = Stroke(1.4.dp.toPx())) }
            }
    } else {
        Modifier.liquidGlass(shape = shape, tint = MaterialTheme.colorScheme.primary, interactive = true, pressed = pressed)
    }

    Box(
        modifier = modifier
            .scale(scale)
            .size(60.dp)
            .then(visual)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription,
            tint = if (prominent) Color.White else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(26.dp)
        )
    }
}

/** Selected glass chips carry a stronger tint; unselected ones hold the bare frost. */
@Composable
private fun selectRich(selected: Boolean, tint: Color?): Color? {
    if (tint == null) return if (selected) MaterialTheme.colorScheme.primary else null
    return if (selected) tint else tint.copy(alpha = 0.4f)
}

/** The one identity gradient brush, for hero surfaces that want the spectrum. */
fun prismBrush(horizontal: Boolean = false): Brush =
    if (horizontal) Brush.horizontalGradient(listOf(PrismIndigo, PrismSky))
    else Brush.linearGradient(listOf(PrismIndigo, PrismSky, PrismCoral))

/**
 * The calm, slowly-illuminated obsidian void behind the whole app — the colour
 * pool the glass refracts. Paints the theme background, three soft indigo /
 * violet / sky glows and a gentle vignette that keeps the edges calm so content
 * never floats against a hot border.
 *
 * Glows are painted with DrawScope radial gradients, not image assets, so they
 * cost no downloads, scale to any screen and sub-pixel smoothly in both themes.
 */
@Composable
fun LifeOSAmbientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val glow = LocalAmbientGlowColors.current
    val baseTop = scheme.background
    val baseBottom = lerp(scheme.background, scheme.primaryContainer, 0.22f)

    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(baseTop, baseBottom)))
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    // Indigo glow, top-left.
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0f to glow.primary,
                                1f to glow.primary.copy(alpha = 0f)
                            ),
                            center = Offset(w * 0.06f, h * 0.02f),
                            radius = w * 0.85f
                        ),
                        radius = w * 0.85f,
                        center = Offset(w * 0.06f, h * 0.02f)
                    )
                    // Violet glow, top-right.
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0f to glow.secondary,
                                1f to glow.secondary.copy(alpha = 0f)
                            ),
                            center = Offset(w * 1.00f, h * 0.12f),
                            radius = w * 0.72f
                        ),
                        radius = w * 0.72f,
                        center = Offset(w * 1.00f, h * 0.12f)
                    )
                    // Sky glow, bottom centre.
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0f to glow.tertiary,
                                1f to glow.tertiary.copy(alpha = 0f)
                            ),
                            center = Offset(w * 0.55f, h * 1.06f),
                            radius = h * 0.72f
                        ),
                        radius = h * 0.72f,
                        center = Offset(w * 0.55f, h * 1.06f)
                    )
                }
        )
        // Vignette: settle the edges back toward the background colour so the
        // glows never reach the system bars hot.
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    drawRect(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                0.72f to Color.Transparent,
                                1f to baseTop.copy(alpha = 0.35f)
                            ),
                            center = Offset(w * 0.5f, h * 0.5f),
                            radius = (w.coerceAtLeast(h) * 0.72f)
                        )
                    )
                }
        )
        content()
    }
}
