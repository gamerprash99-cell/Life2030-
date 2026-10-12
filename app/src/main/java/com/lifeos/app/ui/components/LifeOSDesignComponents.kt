package com.lifeos.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * The historical card/button/header helpers, now delegating to the LifeOS
 * component kit. Keeping the names means every existing screen silently adopts
 * the redesigned surfaces, buttons and section titles without a rewrite.
 */

/** A liquid glass card with the app's standard radius and padding. */
@Composable
fun LifeOSCard(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    LifeOSSurface(modifier = modifier, tint = tint, onClick = onClick) { content() }
}

/** The primary gradient hero button. */
@Composable
fun LifeOSGradientButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    LifeOSPrimaryButton(text = text, onClick = onClick, modifier = modifier)
}

/** A section label with an optional trailing action. */
@Composable
fun LifeOSSectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    LifeOSSectionTitle(title = title, action = action, onAction = onAction)
}
