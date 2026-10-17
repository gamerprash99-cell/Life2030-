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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lifeos.app.ui.theme.LifeOSRadius
import com.lifeos.app.ui.theme.LifeOSSpacing
import com.lifeos.app.ui.theme.LocalAmbientGlowColors
import com.lifeos.app.ui.theme.LocalGlassColors
import com.lifeos.app.ui.theme.LocalLifeOSSemanticColors
import com.lifeos.app.ui.theme.PrismIndigo
import com.lifeos.app.ui.theme.PrismSky
import com.lifeos.app.ui.theme.PrismSpectrum

/**
 * Lumen — the app's surface material.
 *
 * A soft **matte** object: opaque, warm and physical, sitting on the canvas
 * rather than floating over it. v4 painted a frosted lens; v5 paints a thing
 * you could pick up. Depth comes from a long, low shadow and a single warm
 * hairline — there is no backdrop blur, no specular sheen and no bright rim.
 *
 *  1. an opaque vertical fill, a hair lighter at the top where the lamp falls
 *     and flat at the foot,
 *  2. an optional [tint] overlay, a whisper of the accent colour beside it,
 *  3. a faint upper wash — matte surfaces catch light, they do not glint,
 *  4. a pooled base shadow, the object settling into the desk,
 *  5. one warm hairline around the whole shape.
 *
 * When [interactive] is true the surface reacts to touch through scale and a
 * lifted shadow. Because the sheet is matte it does *not* brighten on press —
 * the physical movement is the feedback. Callers animate the scale themselves
 * through the same press signal — [LiquidGlassSurface], [LiquidGlassButton] and
 * [LifeOSCard] already do.
 *
 * All surfaces share one token set and one ambient wash, which is what makes
 * separate cards read as one material instead of separate controls.
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(LifeOSRadius.medium),
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
            elevation = if (pressed) elevation * 1.35f else elevation,
            shape = shape,
            ambientColor = shadowBase.copy(alpha = 0.14f + 0.05f * lift),
            spotColor = shadowBase.copy(alpha = 0.22f + 0.08f * lift)
        )
        .clip(shape)
        .drawBehind {
            val w = size.width
            val h = size.height
            val rimPath = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawBehind)) }

            // 1. The sheet: an opaque matte fill, a hair lighter at the top where
            //    the lamp falls, settling to the flat token colour at the foot.
            //    Matte, so it never brightens on press — the scale and shadow
            //    carry the touch instead.
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lerp(glass.surface, Color.White, 0.03f + 0.04f * lift),
                        glass.surface
                    ),
                    startY = 0f,
                    endY = h
                ),
                topLeft = Offset.Zero,
                size = Size(w, h)
            )

            // 2. The tint: a whisper of the accent colour of the content beside
            //    the surface, so a tinted card reads as related without shouting.
            if (tint != null) {
                drawRect(
                    color = tint.copy(alpha = 0.06f + 0.06f * lift),
                    topLeft = Offset.Zero,
                    size = Size(w, h)
                )
            }

            // 3. A faint upper wash. A matte object catches light; it does not
            //    glint. Kept to a whisper so it can never read as a specular
            //    highlight the way v4's liquid sheen did.
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to glass.sheen.copy(alpha = glass.sheen.alpha * (0.50f + 0.30f * lift)),
                        0.18f to Color.Transparent
                    ),
                    startY = 0f,
                    endY = h
                ),
                topLeft = Offset.Zero,
                size = Size(w, h)
            )

            // 4. The pooled base shadow: the object settling into the desk.
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.62f to Color.Transparent,
                        1f to glass.innerShadow.copy(alpha = glass.innerShadow.alpha + 0.08f * lift)
                    ),
                    startY = 0f,
                    endY = h
                ),
                topLeft = Offset.Zero,
                size = Size(w, h)
            )

            // 5. One warm hairline all the way round. A physical object has a
            //    single edge — not v4's bright top rim and deeper dark foot.
            drawPath(rimPath, glass.border, style = Stroke(1.dp.toPx()))
        }
}

/**
 * A matte sheet with optional press reactivity: opaque, warm and, when
 * [onClick] is set, reacting to touch with a spring scale, a lifted shadow and
 * a strengthening accent tint. It deliberately does not brighten on press —
 * a matte object doesn't, so the movement carries the feedback instead.
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(LifeOSRadius.medium),
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

/** A fully-rounded matte button. [prominent] wears the identity gradient. */
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

/** Selected glass chips carry a stronger tint; unselected ones hold the bare frost. */
@Composable
private fun selectRich(selected: Boolean, tint: Color?): Color? {
    if (tint == null) return if (selected) MaterialTheme.colorScheme.primary else null
    return if (selected) tint else tint.copy(alpha = 0.4f)
}

/**
 * The one identity gradient — clay -> amber -> bronze — for the few identity
 * moments in the app: the hero, the Capture action and the AI orb. Every other
 * surface is a flat token colour, so seeing this gradient always means "this is
 * LifeOS itself" and never "this is decoration". The stops are chosen deep
 * enough to carry white text and icons at AA; see `Color.kt`.
 *
 * [horizontal] runs it left-to-right for bars and buttons (a two-stop, tighter
 * sweep that stays legible behind a label); [alpha] fades the whole sweep for
 * use as a wash behind content.
 */
fun prismBrush(horizontal: Boolean = false, alpha: Float = 1f): Brush {
    val stops = if (horizontal) listOf(PrismIndigo, PrismSky) else PrismSpectrum
    val faded = if (alpha >= 1f) stops else stops.map { it.copy(alpha = it.alpha * alpha) }
    return if (horizontal) Brush.horizontalGradient(faded) else Brush.linearGradient(faded)
}

/**
 * The warm wash behind the whole app — the light the matte surfaces sit in.
 * Paints the theme background, three soft clay / amber / sage hazes and a
 * gentle vignette that keeps the edges calm so content never floats against a
 * hot border.
 *
 * Washes are painted with DrawScope radial gradients, not image assets, so they
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
