package com.lifeos.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lifeos.app.ui.theme.FigmaAccent
import com.lifeos.app.ui.theme.FigmaElevated
import com.lifeos.app.ui.theme.FigmaOnAccent
import com.lifeos.app.ui.theme.FigmaText
import com.lifeos.app.ui.theme.LifeOSElevation
import com.lifeos.app.ui.theme.LifeOSRadius
import com.lifeos.app.ui.theme.LifeOSSpacing
import com.lifeos.app.ui.theme.LocalLifeOSSemanticColors
import com.lifeos.app.ui.theme.PrismCoral
import com.lifeos.app.ui.theme.PrismIndigo
import com.lifeos.app.ui.theme.PrismSky

// ===========================================================================
// LifeOS component kit (v2)
//
// Thin, token-driven building blocks every screen composes from. They all ride
// the Lumen matte material, so a screen built from them
// is automatically consistent with the rest of the app. Nothing here reads a
// raw hex or a magic dp — colour comes from the theme and spacing from
// [LifeOSSpacing].
// ===========================================================================

/**
 * The standard content surface — a matte sheet with the app's card radius and
 * comfortable padding. This is the single building block screens use for
 * grouped content, so "a card" looks the same everywhere.
 */
@Composable
fun LifeOSSurface(
    modifier: Modifier = Modifier,
    tint: Color? = null,
    contentPadding: PaddingValues = PaddingValues(LifeOSSpacing.lg),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    LiquidGlassSurface(
        modifier = modifier,
        shape = RoundedCornerShape(LifeOSRadius.xlarge),
        tint = tint,
        onClick = onClick,
        contentPadding = contentPadding
    ) { content() }
}

/**
 * The large header that leads a root screen. One thing leads each screen, and
 * on root screens that thing is this: a small eyebrow (e.g. the date), a large
 * title, an optional subtitle, and a row of trailing actions.
 */
@Composable
fun LifeOSScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Column(modifier.fillMaxWidth().padding(horizontal = LifeOSSpacing.screenPadding)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                eyebrow?.let {
                    Text(
                        it.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(LifeOSSpacing.xs))
                }
                Text(
                    title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                subtitle?.let {
                    Spacer(Modifier.height(LifeOSSpacing.xs))
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, content = actions)
        }
    }
}

/** A section label that introduces a block of content, with an optional action. */
@Composable
fun LifeOSSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            // Serif: a section title is the app speaking, so it carries the
            // display voice rather than the sans used for the content beneath.
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            // Weight, not a spacer, so a long title ellipsises instead of
            // pushing the action off the edge.
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (action != null && onAction != null) {
            Text(
                action,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .defaultMinSize(minHeight = 32.dp)
                    .clip(RoundedCornerShape(LifeOSRadius.pill))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
    }
}

/** A rounded squircle holding a single icon, tinted by [tint]. */
@Composable
fun LifeOSIconBadge(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 44.dp
) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(LifeOSRadius.medium))
            .background(
                Brush.linearGradient(
                    listOf(tint.copy(alpha = 0.22f), tint.copy(alpha = 0.10f))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(size * 0.52f))
    }
}

/** A small status pill (e.g. a streak, a category, a delta). */
@Composable
fun LifeOSPill(
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    icon: ImageVector? = null
) {
    Row(
        modifier
            .clip(RoundedCornerShape(LifeOSRadius.pill))
            .background(tint.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        icon?.let { Icon(it, null, tint = tint, modifier = Modifier.size(14.dp)) }
        Text(text, style = MaterialTheme.typography.labelSmall, color = tint, maxLines = 1)
    }
}

/**
 * A single headline number with a label and optional supporting/delta text.
 * The value is the largest thing in the tile; `emphasize` turns it into the
 * tile's hero by painting it in the brand colour.
 */
@Composable
fun LifeOSMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    delta: String? = null,
    deltaPositive: Boolean = true,
    emphasize: Boolean = false
) {
    Column(modifier) {
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            color = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val support = supporting ?: delta
        if (support != null) {
            Spacer(Modifier.height(LifeOSSpacing.xs))
            val semantic = LocalLifeOSSemanticColors.current
            Text(
                support,
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    delta == null -> MaterialTheme.colorScheme.onSurfaceVariant
                    deltaPositive -> semantic.success
                    else -> MaterialTheme.colorScheme.error
                }
            )
        }
    }
}

/** An animated, brand-gradient linear progress bar with a faint track. */
@Composable
fun LifeOSProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    height: Dp = 10.dp
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 120f, dampingRatio = 1f),
        label = "progress"
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(LifeOSRadius.pill))
            .background(trackColor)
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(height)
                .clip(RoundedCornerShape(LifeOSRadius.pill))
                .background(prismBrush(horizontal = true))
        )
    }
}

/** A circular progress ring with a large centred value — the app's hero gauge. */
@Composable
fun LifeOSProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    ringSize: Dp = 132.dp,
    strokeWidth: Dp = 12.dp,
    center: @Composable () -> Unit
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 120f, dampingRatio = 1f),
        label = "ring"
    )
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier.size(ringSize), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(ringSize)) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = track,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(PrismIndigo, PrismSky, PrismCoral, PrismIndigo)),
                startAngle = 135f,
                sweepAngle = 270f * animated,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        center()
    }
}

/** The primary action: a flat **mint** fill with dark on-accent ink. */
@Composable
fun LifeOSPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.975f else 1f, spring(stiffness = 800f), label = "btn")
    val shape = RoundedCornerShape(LifeOSRadius.medium)
    val fill = if (enabled) FigmaAccent else MaterialTheme.colorScheme.surfaceVariant
    val ink = if (enabled) FigmaOnAccent else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(fill)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            icon?.let { Icon(it, null, tint = ink, modifier = Modifier.size(19.dp)) }
            Text(text, style = MaterialTheme.typography.labelLarge, color = ink)
        }
    }
}

/** A quieter secondary action: the flat slate `#1C303A` panel. */
@Composable
fun LifeOSSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.975f else 1f, spring(stiffness = 800f), label = "btn2")
    val shape = RoundedCornerShape(LifeOSRadius.medium)
    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(FigmaElevated)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            icon?.let { Icon(it, null, tint = FigmaAccent, modifier = Modifier.size(19.dp)) }
            Text(text, style = MaterialTheme.typography.labelLarge, color = FigmaText)
        }
    }
}

/**
 * A large, tappable action tile — a big icon over a label — used by the
 * Capture hub and the Home quick actions. [accent] tints the icon badge.
 */
@Composable
fun LifeOSActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    caption: String? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, spring(stiffness = 800f), label = "tile")
    Box(
        modifier
            .scale(scale)
            .liquidGlass(
                shape = RoundedCornerShape(LifeOSRadius.large),
                tint = accent,
                interactive = true,
                pressed = pressed
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(LifeOSSpacing.lg)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)) {
            LifeOSIconBadge(icon, contentDescription = null, tint = accent, size = 48.dp)
            Column {
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                caption?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * The honest empty state: an icon, a title, a sentence of guidance and one
 * primary action. Never a fabricated counter.
 */
@Composable
fun LifeOSEmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = LifeOSSpacing.screenPadding, vertical = LifeOSSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)
    ) {
        LifeOSIconBadge(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, size = 64.dp)
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(LifeOSSpacing.xs))
            LifeOSPrimaryButton(actionLabel, onAction)
        }
    }
}

/**
 * A settings / menu row: leading icon badge, title (+ optional subtitle) and a
 * trailing slot. The whole row is the touch target.
 */
@Composable
fun LifeOSListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    tint: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LifeOSRadius.large))
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
                } else Modifier
            )
            .defaultMinSize(minHeight = LifeOSSpacing.minTouchTarget)
            .padding(horizontal = LifeOSSpacing.md, vertical = LifeOSSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)
    ) {
        if (icon != null) LifeOSIconBadge(icon, contentDescription = null, tint = tint, size = 40.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        trailing?.invoke()
    }
}

/** A minimal segmented control for switching a period (Week / Month / Year). */
@Composable
fun <T> LifeOSSegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: (T) -> String = { it.toString() }
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LifeOSRadius.pill))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val bg by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                label = "segBg"
            )
            val fg by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "segFg"
            )
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(LifeOSRadius.pill))
                    .background(bg)
                    .clickable { onSelect(option) }
                    .defaultMinSize(minHeight = 36.dp)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label(option), style = MaterialTheme.typography.labelMedium, color = fg)
            }
        }
    }
}

/** A hairline divider that adapts to the theme's border colour. */
@Composable
fun LifeOSDivider(modifier: Modifier = Modifier) {
    val border = LocalLifeOSSemanticColors.current.border
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(border)
    )
}

/** A thin vertical/horizontal spacer. */
@Composable
fun LifeOSGap(size: Dp) = Spacer(Modifier.size(size))
