package com.lifeos.app.ui.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Row of filled/empty dots representing the entered 4-digit PIN. */
@Composable
fun PinDots(
    length: Int,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    val dotColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier.semantics { contentDescription = "$length of 4 PIN digits entered" },
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (index < length) dotColor else MaterialTheme.colorScheme.outlineVariant)
            )
        }
    }
}

/**
 * Numeric keypad for the 4-digit App Lock PIN. Digits 1-9, then blank / 0 /
 * backspace. Every key has a large touch target and haptic feedback.
 */
@Composable
fun PinKeypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val haptics = LocalHapticFeedback.current
    fun tap(action: () -> Unit) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        action()
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9")
        )
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { digit ->
                    PinKey(digit, enabled) { tap { onDigit(digit.first()) } }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Spacer(Modifier.weight(1f))
            PinKey("0", enabled) { tap { onDigit('0') } }
            PinBackspaceKey(enabled) { tap(onBackspace) }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.PinKey(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .weight(1f)
            .size(64.dp)
            .semantics { role = Role.Button; contentDescription = label }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.PinBackspaceKey(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .weight(1f)
            .size(64.dp)
            .semantics { role = Role.Button; contentDescription = "Backspace" }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = null)
        }
    }
}

/** Themed keypad container used by both setup and unlock screens. */
@Composable
fun PinKeypadPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp
    ) {
        Box(Modifier.padding(16.dp), contentAlignment = Alignment.Center) { content() }
    }
}
