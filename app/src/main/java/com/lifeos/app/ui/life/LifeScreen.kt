package com.lifeos.app.ui.life

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lifeos.app.ui.components.LifeOSCard
import com.lifeos.app.ui.components.LifeOSPageHeader
import com.lifeos.app.ui.components.LifeOSPrimaryButton
import com.lifeos.app.ui.components.LifeOSSectionHeader
import com.lifeos.app.ui.components.lifeOSIsDark
import com.lifeos.app.ui.theme.LifeOSMint
import com.lifeos.app.ui.theme.lifeOSCanvasBrush

/**
 * Life — the connected-history hub. A calm centre on the shared canvas: one
 * search entry, the Life Timeline, then every track module (Diary, Tasks,
 * Habits, Expenses) and the memory library.
 */
@Composable
fun LifeScreen(
    onOpenTimeline: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenDiary: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenHabits: () -> Unit,
    onOpenExpenses: () -> Unit
) {
    val dark = lifeOSIsDark()

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(lifeOSCanvasBrush(dark)),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        item {
            LifeOSPageHeader(
                eyebrow = "Everything so far",
                title = "Life",
                actionIcon = Icons.Outlined.Search,
                actionDescription = "Search everything",
                onAction = onOpenSearch
            )
        }

        item {
            Column(
                Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                SearchWell(onClick = onOpenSearch)

                LifeOSCard {
                    Text(
                        "Life Timeline",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Notes, memories, tasks, habits and spending merged into one scrollable history.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LifeOSPrimaryButton(
                        text = "Open timeline",
                        onClick = onOpenTimeline,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                LifeOSSectionHeader(title = "Your modules", modifier = Modifier.padding(top = 6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ModuleTile(
                            icon = Icons.Filled.Book,
                            title = "Diary",
                            caption = "Daily memory",
                            onClick = onOpenDiary,
                            modifier = Modifier.weight(1f)
                        )
                        ModuleTile(
                            icon = Icons.Filled.CheckCircle,
                            title = "Tasks",
                            caption = "What to do",
                            onClick = onOpenTasks,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ModuleTile(
                            icon = Icons.Filled.LocalFireDepartment,
                            title = "Habits",
                            caption = "Daily rhythm",
                            onClick = onOpenHabits,
                            modifier = Modifier.weight(1f)
                        )
                        ModuleTile(
                            icon = Icons.Filled.Payments,
                            title = "Spending",
                            caption = "Money",
                            onClick = onOpenExpenses,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                LifeOSSectionHeader(title = "Memory", modifier = Modifier.padding(top = 6.dp))

                ModuleTile(
                    icon = Icons.Filled.PhotoLibrary,
                    title = "Memory library",
                    caption = "Photos, voice and everything you kept",
                    onClick = onOpenSearch,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** The search well: a card that looks like the field but opens the real search. */
@Composable
private fun SearchWell(onClick: () -> Unit) {
    LifeOSCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = 15.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                "Search everything",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ModuleTile(
    icon: ImageVector,
    title: String,
    caption: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LifeOSCard(modifier = modifier, onClick = onClick) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(LifeOSMint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = LifeOSMint, modifier = Modifier.size(20.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            caption,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
