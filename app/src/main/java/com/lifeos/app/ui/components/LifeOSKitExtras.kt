package com.lifeos.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lifeos.app.ui.theme.LifeOSElevation
import com.lifeos.app.ui.theme.LifeOSRadius
import com.lifeos.app.ui.theme.LifeOSSpacing
import com.lifeos.app.ui.theme.LocalLifeOSSemanticColors

// ===========================================================================
// LifeOS component kit (v3 "Aurora Noir") — additive pieces
//
// The pieces that are new with the redesign live here so the original kit stays
// untouched: a search field, a filter-chip row, a stat tile, a pulsing skeleton,
// a circular glass icon button and a gradient hero surface. Nothing here reads
// a raw hex or a magic dp beyond the token scale.
// ===========================================================================

/**
 * The one search field in the app. A calm glass pill with a leading glyph and
 * an inline clear affordance. When [onClick] is set the field is a *button*
 * that opens a dedicated search screen (Home, lists); otherwise it is a live
 * input that owns the query.
 */
@Composable
fun LifeOSSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
    onClick: (() -> Unit)? = null,
    leadingIcon: ImageVector = Icons.Filled.Search
) {
    val shape = RoundedCornerShape(LifeOSRadius.pill)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && onClick != null) 0.985f else 1f, spring(stiffness = 800f), label = "searchPress")

    Row(
        modifier
            .fillMaxWidth()
            .scale(scale)
            .liquidGlass(
                shape = shape,
                tint = MaterialTheme.colorScheme.primary,
                interactive = onClick != null,
                pressed = pressed,
                elevation = 4.dp
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
                } else Modifier
            )
            .defaultMinSize(minHeight = LifeOSSpacing.minTouchTarget)
            .padding(horizontal = LifeOSSpacing.lg, vertical = LifeOSSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)
    ) {
        Icon(leadingIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onClick == null) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (onClick == null && value.isNotEmpty()) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable { onValueChange("") },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/**
 * A horizontal, scrollable row of selectable filter chips. Selection is a filled
 * brand pill; the unselected state is the bare frost, so the active filter is
 * the only loud thing on the row.
 */
@Composable
fun <T> LifeOSFilterChips(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    leading: (@Composable (T) -> Unit)? = null
) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.sm),
        contentPadding = PaddingValues(horizontal = 0.dp)
    ) {
        items(options.size) { index ->
            val option = options[index]
            val isSelected = option == selected
            val shape = RoundedCornerShape(LifeOSRadius.pill)
            val bg by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                label = "chipBg"
            )
            val fg by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "chipFg"
            )
            Row(
                Modifier
                    .clip(shape)
                    .background(bg)
                    .clickable { onSelect(option) }
                    .defaultMinSize(minHeight = 36.dp)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                leading?.let { it(option) }
                Text(label(option), style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/**
 * A compact stat tile — a big value over a label, with an optional icon badge.
 * These are the unit of an insights dashboard: honest numbers, one per square.
 */
@Composable
fun LifeOSStatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color = MaterialTheme.colorScheme.primary,
    supporting: String? = null
) {
    LifeOSSurface(modifier = modifier, contentPadding = PaddingValues(LifeOSSpacing.lg)) {
        Column(verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.sm)) {
                icon?.let { LifeOSIconBadge(it, contentDescription = null, tint = tint, size = 32.dp) }
                Text(
                    label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            supporting?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/**
 * A single skeleton line/block that breathes while data loads. Never a spinner
 * in the middle of a list — a skeleton that mirrors the shape it will become.
 */
@Composable
fun LifeOSSkeleton(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(LifeOSRadius.medium)
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(900), repeatMode = RepeatMode.Reverse),
        label = "skeletonAlpha"
    )
    Box(
        modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
    )
}

/** A stack of skeleton rows that stands in for a loading list. */
@Composable
fun LifeOSSkeletonList(
    modifier: Modifier = Modifier,
    rows: Int = 4
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)) {
        repeat(rows) {
            LifeOSSurface(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)) {
                    LifeOSSkeleton(Modifier.size(40.dp), CircleShape)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sm)) {
                        LifeOSSkeleton(Modifier.fillMaxWidth(0.6f).height(14.dp))
                        LifeOSSkeleton(Modifier.fillMaxWidth(0.35f).height(11.dp))
                    }
                }
            }
        }
    }
}

/** A circular glass icon button with spring press feedback. */
@Composable
fun LifeOSIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, spring(stiffness = 900f), label = "iconBtn")
    Box(
        modifier
            .size(size)
            .scale(scale)
            .liquidGlass(shape = CircleShape, tint = MaterialTheme.colorScheme.primary, interactive = true, pressed = pressed, elevation = 4.dp)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size * 0.48f))
    }
}

/**
 * The gradient hero surface — the single loudest card a screen can lead with.
 * A brand-gradient fill, a soft neutral shadow and the app's hero radius.
 */
@Composable
fun LifeOSHeroCard(
    modifier: Modifier = Modifier,
    brush: Brush = prismBrush(),
    contentPadding: PaddingValues = PaddingValues(LifeOSSpacing.xl),
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(LifeOSRadius.xlarge)
    // Depth comes from the shared ladder and the neutral shadow token — never
    // from a brand-tinted smear, which would stain whatever sits behind it.
    val shadowColor = LocalLifeOSSemanticColors.current.shadow
    Box(
        modifier
            .fillMaxWidth()
            .shadow(
                elevation = LifeOSElevation.hero,
                shape = shape,
                ambientColor = shadowColor.copy(alpha = LifeOSElevation.shadowAlphaRest),
                spotColor = shadowColor.copy(alpha = LifeOSElevation.shadowAlphaLift)
            )
            .clip(shape)
            .background(brush)
            .padding(contentPadding)
    ) { content() }
}

/** A small, quiet section eyebrow used above grouped content. */
@Composable
fun LifeOSEyebrow(text: String, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.primary) {
    Text(
        text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = tint,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/**
 * An honest inline banner: an icon, a message and an optional action. Used for
 * privacy notices, permission asks and post-save confirmations — the places
 * where the app must tell the truth, plainly.
 */
@Composable
fun LifeOSBanner(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = LocalLifeOSSemanticColors.current.info,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LifeOSRadius.large))
            .background(tint.copy(alpha = 0.12f))
            .padding(horizontal = LifeOSSpacing.lg, vertical = LifeOSSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                action,
                style = MaterialTheme.typography.labelMedium,
                color = tint,
                modifier = Modifier
                    .clip(RoundedCornerShape(LifeOSRadius.pill))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}
