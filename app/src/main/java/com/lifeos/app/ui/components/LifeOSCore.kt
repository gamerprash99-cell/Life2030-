package com.lifeos.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeos.app.ui.theme.LifeOSCardFillDark
import com.lifeos.app.ui.theme.LifeOSCardFillLight
import com.lifeos.app.ui.theme.LifeOSCardStrokeDark
import com.lifeos.app.ui.theme.LifeOSCardStrokeLight
import com.lifeos.app.ui.theme.LifeOSMint
import com.lifeos.app.ui.theme.LifeOSMintInk
import com.lifeos.app.ui.theme.LifeOSNavPillSelectedDark
import com.lifeos.app.ui.theme.LifeOSNavPillSelectedLight
import com.lifeos.app.ui.theme.LifeOSRadius
import com.lifeos.app.ui.theme.LifeOSSurfaceDark
import com.lifeos.app.ui.theme.LifeOSSurfaceLight
import com.lifeos.app.ui.theme.LocalLifeOSSemanticColors
import com.lifeos.app.ui.theme.lifeOSCanvasBrush

// ===========================================================================
// The rebuilt component kit — one visual vocabulary taken from the Figma
// frames: translucent r22 cards with a hairline stroke on the gradient
// canvas, pill chips, mint primary actions, and a small uppercase eyebrow
// that prefixes every page title.
// ===========================================================================

/** True while the theme is the dark (reference) polarity. */
@Composable
fun lifeOSIsDark(): Boolean =
    MaterialTheme.colorScheme.background.luminanceSafe() < 0.5f

private fun Color.luminanceSafe(): Float =
    (0.299f * red + 0.587f * green + 0.114f * blue)

/** The page canvas: the three-stop gradient every screen sits on. */
@Composable
fun Modifier.lifeOSCanvas(): Modifier =
    background(lifeOSCanvasBrush(dark = lifeOSIsDark()))

// ---- Card ------------------------------------------------------------------

/**
 * The signature surface: a translucent sheet (Figma rgba(31,53,64,0.79)) with
 * a 1px cool hairline and a soft pooled shadow. The canvas gradient breathes
 * through it, so it never needs a heavy glass treatment.
 */
@Composable
fun Modifier.lifeOSCardShape(): Modifier {
    val dark = lifeOSIsDark()
    return this
        .shadow(
            elevation = 12.dp,
            shape = RoundedCornerShape(LifeOSRadius.medium),
            ambientColor = Color.Black.copy(alpha = 0.15f),
            spotColor = Color.Black.copy(alpha = 0.15f),
        )
        .clip(RoundedCornerShape(LifeOSRadius.medium))
        .background(if (dark) LifeOSCardFillDark else LifeOSCardFillLight)
        .border(
            width = 1.dp,
            color = if (dark) LifeOSCardStrokeDark else LifeOSCardStrokeLight,
            shape = RoundedCornerShape(LifeOSRadius.medium),
        )
}

/** One translucent card. [onClick] adds press feedback and button semantics. */
@Composable
fun LifeOSCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val base = modifier
        .lifeOSCardShape()
        .then(
            if (onClick != null) {
                Modifier
                    .semantics { role = Role.Button }
                    .clickable(onClick = onClick)
            } else {
                Modifier
            }
        )
    Column(
        modifier = base.padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

// ---- Page header -----------------------------------------------------------

/**
 * The uppercase mint eyebrow above every page title (Figma: 9sp SemiBold,
 * uppercase, letter-spaced, mint).
 */
@Composable
fun LifeOSEyebrow(text: String, modifier: Modifier = Modifier, color: Color = LifeOSMint) {
    Text(
        text.uppercase(),
        modifier = modifier,
        color = color,
        fontSize = 9.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.4.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * The 86dp page header: eyebrow + 24sp title on the leading edge, an optional
 * 42x42 r15 action (or back arrow) on the trailing edge — exactly the header
 * every Figma frame uses.
 */
@Composable
fun LifeOSPageHeader(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actionIcon: ImageVector? = null,
    actionDescription: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 86.dp)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onBack != null) {
            LifeOSHeaderButton(
                icon = Icons.Outlined.ArrowBack,
                onClick = onBack,
                contentDescription = "Back",
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            LifeOSEyebrow(eyebrow)
            Text(
                title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (actionIcon != null && onAction != null) {
            LifeOSHeaderButton(
                icon = actionIcon,
                onClick = onAction,
                contentDescription = actionDescription,
            )
        }
    }
}

/** The 42x42 r15 header action slot (Figma: white 3% fill + hairline). */
@Composable
fun LifeOSHeaderButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val alpha by animateFloatAsState(
        targetValue = if (pressed) 0.14f else 0.03f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "headerActionPress",
    )
    val dark = lifeOSIsDark()
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(Color.White.copy(alpha = alpha))
            .border(
                width = 1.dp,
                color = if (dark) LifeOSCardStrokeDark else LifeOSCardStrokeLight,
                shape = RoundedCornerShape(15.dp),
            )
            .semantics {
                role = Role.Button
                if (contentDescription != null) this.contentDescription = contentDescription
            }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(22.dp),
        )
    }
}

// ---- Section header --------------------------------------------------------

/** A card-title row with an optional trailing mint label (and action). */
@Composable
fun LifeOSSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (actionLabel != null) {
            Text(
                actionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = LifeOSMint,
                modifier = Modifier
                    .clip(RoundedCornerShape(LifeOSRadius.small))
                    .then(if (onAction != null) Modifier.clickable(onClick = onAction) else Modifier)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
    }
}

// ---- Pills -----------------------------------------------------------------

/**
 * The pill chip (Figma: r100, 8x12 padding). Selected = dark-mint fill with
 * mint text and a mint-tinted stroke; idle = surface fill with secondary text.
 */
@Composable
fun LifeOSFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val dark = lifeOSIsDark()
    val container by animateColorAsState(
        targetValue = when {
            selected -> if (dark) LifeOSNavPillSelectedDark else LifeOSNavPillSelectedLight
            pressed -> Color.White.copy(alpha = 0.06f)
            else -> if (dark) LifeOSSurfaceDark else LifeOSSurfaceLight
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "pillContainer",
    )
    val content by animateColorAsState(
        targetValue = if (selected) LifeOSMint else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "pillContent",
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(LifeOSRadius.pill))
            .background(container)
            .border(
                width = 1.dp,
                color = if (selected) LifeOSMint.copy(alpha = 0.3f)
                else if (dark) LifeOSCardStrokeDark else LifeOSCardStrokeLight,
                shape = RoundedCornerShape(LifeOSRadius.pill),
            )
            .semantics { role = Role.Tab }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = content,
            maxLines = 1,
        )
    }
}

// ---- Buttons ---------------------------------------------------------------

/**
 * The mint primary action (Figma: full-width, 52dp, r16, mint fill, dark ink).
 */
@Composable
fun LifeOSPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "primaryPress",
    )
    Row(
        modifier = modifier
            .height(52.dp)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(
                when {
                    !enabled -> MaterialTheme.colorScheme.surfaceVariant
                    else -> LifeOSMint
                }
            )
            .semantics { role = Role.Button }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) LifeOSMintInk else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp),
            )
            androidx.compose.foundation.layout.Spacer(Modifier.width(10.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) LifeOSMintInk else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A secondary text action in mint (Figma: "Ask LifeOS AI" style link rows). */
@Composable
fun LifeOSLinkText(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = LifeOSMint,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.labelLarge,
) {
    Text(
        text,
        modifier = modifier
            .clip(RoundedCornerShape(LifeOSRadius.small))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        style = style,
        color = color,
        maxLines = 1,
    )
}

// ---- Search field ----------------------------------------------------------

/** The search well (Figma: #162630, r16, 15dp padding, 18dp leading icon). */
@Composable
fun LifeOSSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    val dark = lifeOSIsDark()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (dark) LifeOSSurfaceDark else LifeOSSurfaceLight)
            .padding(horizontal = 15.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (leadingIcon != null) {
            Icon(
                leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        if (value.isEmpty()) {
            Text(
                placeholder,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(LifeOSMint),
            modifier = Modifier.weight(1f),
        )
    }
}

// ---- Progress --------------------------------------------------------------

/**
 * The slim progress track (Figma: 5dp tall, rounded, mint fill over a
 * translucent surface track).
 */
@Composable
fun LifeOSProgressTrack(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 5.dp,
) {
    val dark = lifeOSIsDark()
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "progressTrack",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(if (dark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.07f))
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(height)
                .clip(CircleShape)
                .background(LifeOSMint)
        )
    }
}

// ---- States ----------------------------------------------------------------

/** The shared empty-state layout: icon disc, title, body, optional action. */
@Composable
fun LifeOSEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (icon != null) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(LifeOSMint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = LifeOSMint,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (actionText != null && onAction != null) {
            LifeOSPrimaryButton(
                text = actionText,
                onClick = onAction,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/** A one-line error row with a retry affordance. */
@Composable
fun LifeOSErrorState(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val semantics = LocalLifeOSSemanticColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .lifeOSCardShape()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = semantics.danger,
            modifier = Modifier.weight(1f),
        )
        if (onRetry != null) {
            LifeOSLinkText(text = "Retry", onClick = onRetry, color = semantics.danger)
        }
    }
}

/** A three-dot indicator used while a screen's first page loads. */
@Composable
fun LifeOSLoadingDots(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(LifeOSMint.copy(alpha = 0.7f))
                )
            }
        }
    }
}
