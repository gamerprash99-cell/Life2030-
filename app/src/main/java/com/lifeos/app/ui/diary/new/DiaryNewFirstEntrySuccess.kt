package com.lifeos.app.ui.diary.new

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lifeos.app.ui.theme.DiaryActionViolet
import com.lifeos.app.ui.theme.DiaryHandwriting
import com.lifeos.app.ui.theme.DiaryInkSoft
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryLavender
import com.lifeos.app.ui.theme.DiaryPaperCard
import com.lifeos.app.ui.theme.LifeOSSpacing

/**
 * Diary First Entry Success — new visual design.
 *
 * First Entry Saved celebration, optional 3-Day Diary Challenge, reminder
 * selector, dismiss action. Only shown after the first durable save.
 */
@Composable
fun DiaryNewFirstEntrySuccess(
    onDismiss: () -> Unit,
    onViewEntry: () -> Unit = {},
    onAddAnother: () -> Unit = {},
    onSetReminder: ((Int) -> Unit)? = null,
    onStartChallenge: () -> Unit = {}
) {
    Column(
        Modifier.fillMaxSize().background(DiaryNewCanvas),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Celebration illustration
        Box(
            Modifier.size(140.dp).clip(RoundedCornerShape(30.dp)).background(DiaryNewPanel),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(96.dp).clip(CircleShape).background(DiaryNewSave),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.displayLarge.copy(color = Color.White),
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        // Headline
        Text(
            "First Entry Saved!",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = DiaryHandwriting,
                fontWeight = FontWeight.Bold,
                color = DiaryNewInk
            ),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))

        // Body
        Text(
            "Your first memory is safely stored on your device.",
            style = MaterialTheme.typography.bodyMedium.copy(color = DiaryNewInkSoft),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        // 3-Day Challenge (optional)
        DiaryNewChallengeCard(
            title = "3-Day Diary Challenge",
            subtitle = "Write for 3 days in a row to build your habit",
            onStart = onStartChallenge
        )
        Spacer(Modifier.height(16.dp))

        // Reminder selector (optional)
        if (onSetReminder != null) {
            DiaryNewReminderCard(
                onSetReminder = onSetReminder
            )
            Spacer(Modifier.height(16.dp))
        }

        // Actions
        Row(
            Modifier.fillMaxWidth().padding(horizontal = DiaryNewScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(
                    "Maybe later",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = DiaryNewInkSoft
                    )
                )
            }
            Spacer(Modifier.fillMaxWidth())
            DiaryNewPrimaryButton(
                label = "View entry",
                onClick = onViewEntry,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(12.dp))
        TextButton(
            onClick = onAddAnother,
        ) {
            Text(
                "Add another entry",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = DiaryNewSave
                )
            )
        }
    }
}

@Composable
private fun DiaryNewChallengeCard(
    title: String,
    subtitle: String,
    onStart: () -> Unit
) {
    DiaryNewPanel(
        modifier = Modifier.fillMaxWidth().padding(horizontal = DiaryNewScreenPadding),
        cornerRadius = 16
    ) {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = DiaryHandwriting,
                    fontWeight = FontWeight.Bold,
                    color = DiaryNewInk
                )
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(color = DiaryNewInkSoft)
            )
            Spacer(Modifier.height(12.dp))
            DiaryNewPrimaryButton(
                label = "Start challenge",
                onClick = onStart,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DiaryNewReminderCard(
    onSetReminder: (Int) -> Unit
) {
    DiaryNewPanel(
        modifier = Modifier.fillMaxWidth().padding(horizontal = DiaryNewScreenPadding),
        cornerRadius = 16
    ) {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Daily reminder",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = DiaryHandwriting,
                    fontWeight = FontWeight.Bold,
                    color = DiaryNewInk
                )
            )
            Text(
                "Get reminded to write at the same time each day.",
                style = MaterialTheme.typography.bodyMedium.copy(color = DiaryNewInkSoft)
            )
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DiaryNewReminderTimeChip(
                    label = "9:00 AM",
                    hour = 9,
                    minute = 0,
                    selected = true,
                    onSelect = onSetReminder
                )
                DiaryNewReminderTimeChip(
                    label = "6:00 PM",
                    hour = 18,
                    minute = 0,
                    selected = false,
                    onSelect = onSetReminder
                )
                DiaryNewReminderTimeChip(
                    label = "9:00 PM",
                    hour = 21,
                    minute = 0,
                    selected = false,
                    onSelect = onSetReminder
                )
            }
        }
    }
}

@Composable
private fun DiaryNewReminderTimeChip(
    label: String,
    hour: Int,
    minute: Int,
    selected: Boolean,
    onSelect: (Int) -> Unit
) {
    Box(
        Modifier
            .clip(CircleShape)
            .background(if (selected) DiaryNewSave else DiaryNewLavender.copy(alpha = 0.5f))
            .clickable(onClick = { onSelect(hour * 60 + minute) }),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (selected) Color.White else DiaryNewInk,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

/** Primary button is defined in DiaryHomeScreen.kt */
