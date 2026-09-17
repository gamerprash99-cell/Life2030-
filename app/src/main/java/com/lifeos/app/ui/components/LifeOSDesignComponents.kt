package com.lifeos.app.ui.components

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.lifeos.app.ui.theme.LifeOSPrimary
import com.lifeos.app.ui.theme.LifeOSSpacing

@Composable
fun LifeOSCard(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(28.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.985f else 1f,
        animationSpec = spring(stiffness = 700f),
        label = "cardPress"
    )
    Box(
        modifier = modifier
            .scale(scale)
            .shadow(elevation = 3.dp, shape = shape, ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
            .clip(shape)
            .background(tint)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.07f), shape)
            .then(if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick) else Modifier)
            .padding(18.dp)
    ) { content() }
}

@Composable
fun LifeOSGradientButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.985f else 1f, spring(stiffness = 700f), label = "buttonPress")
    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(LifeOSPrimary, Color(0xFFD946EF))))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .defaultMinSize(minHeight = LifeOSSpacing.minTouchTarget)
            .padding(horizontal = 20.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = Color.White, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun LifeOSSectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.weight(1f))
        if (action != null) {
            Text(action, color = LifeOSPrimary, style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.defaultMinSize(minHeight = LifeOSSpacing.minTouchTarget).clickable { onAction?.invoke() }.padding(horizontal = 8.dp, vertical = 12.dp))
        }
    }
}

@Composable
fun LifeOSBadge(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(text, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun LifeOSIntelligenceCard(onClick: () -> Unit) {
    LifeOSCard(modifier = Modifier.fillMaxWidth(), tint = Color(0xFFF1E8FF), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.72f)).padding(10.dp)) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = "LifeOS Intelligence", tint = LifeOSPrimary)
            }
            Column(Modifier.padding(start = 12.dp)) {
                Text("LifeOS Intelligence", style = MaterialTheme.typography.titleMedium)
                Text("Runs locally on your device", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
