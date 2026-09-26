package com.lifeos.app.ui.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeos.app.ui.theme.DiaryHairline
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.LifeOSSpacing

/**
 * Empty day. No illustration and no mascot — just the spine motif the rest of
 * the screen uses, ending in one open ring where the first memory will land.
 * [dayLabel] is shown because "nothing here" is ambiguous until you know which
 * day you are looking at.
 */
@Composable
fun DiaryEmptyState(
    dayLabel: String,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .fadeInAsContent()
            .padding(LifeOSSpacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // The spine, descending to a single open node.
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.width(1.dp).height(26.dp).background(DiaryHairline))
            Box(
                Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(DiaryInkViolet.copy(alpha = 0.06f))
            )
            Box(Modifier.width(1.dp).height(26.dp).background(DiaryHairline))
        }

        Spacer(Modifier.height(28.dp))

        Text(
            "YOUR STORY STARTS HERE",
            style = MaterialTheme.typography.labelSmall,
            color = DiaryInkViolet,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Nothing recorded on $dayLabel.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CreateMemoryAction(onClick = onCreate, compact = true)
        }
    }
}
