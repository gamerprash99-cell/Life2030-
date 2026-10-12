package com.lifeos.app.ui.life

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lifeos.app.ui.components.LifeOSActionTile
import com.lifeos.app.ui.components.LifeOSEyebrow
import com.lifeos.app.ui.components.LifeOSHeroCard
import com.lifeos.app.ui.components.LifeOSPrimaryButton
import com.lifeos.app.ui.components.LifeOSSearchBar
import com.lifeos.app.ui.theme.LifeOSPrimary
import com.lifeos.app.ui.theme.LifeOSSecondary
import com.lifeos.app.ui.theme.LifeOSSpacing
import com.lifeos.app.ui.theme.LifeOSSuccess
import com.lifeos.app.ui.theme.LifeOSWarning

/**
 * Life — the hub for everything that has already happened. A quiet command
 * centre: search one field, jump into the Life Timeline, or step into any track
 * module (Diary, Tasks, Habits, Expenses) and the memory library.
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
    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = LifeOSSpacing.screenPadding,
            end = LifeOSSpacing.screenPadding,
            top = LifeOSSpacing.sm,
            bottom = LifeOSSpacing.sectionSpacing
        ),
        verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sectionSpacing)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.xs)) {
                Text("Life", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
                Text("Your whole story, in one place", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            LifeOSSearchBar(
                value = "",
                onValueChange = {},
                placeholder = "Search notes, diary, memories…",
                onClick = onOpenSearch
            )
        }
        item {
            LifeOSHeroCard {
                Column(verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sm)) {
                    Text("THE LIFE TIMELINE", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
                    Text("Every day, connected", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    Text(
                        "Tasks, habits, spending, diary and captures merged into one scrollable history.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(Modifier.height(LifeOSSpacing.xs))
                    LifeOSPrimaryButton(text = "Open timeline", onClick = onOpenTimeline, icon = Icons.Filled.Timeline)
                }
            }
        }
        item { LifeOSEyebrow("Your modules") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)) {
                LifeOSActionTile(Icons.Filled.Book, "Diary", onOpenDiary, Modifier.weight(1f), accent = LifeOSPrimary, caption = "Daily memory")
                LifeOSActionTile(Icons.Filled.CheckCircle, "Tasks", onOpenTasks, Modifier.weight(1f), accent = LifeOSSuccess, caption = "What to do")
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)) {
                LifeOSActionTile(Icons.Filled.LocalFireDepartment, "Habits", onOpenHabits, Modifier.weight(1f), accent = LifeOSWarning, caption = "Streaks")
                LifeOSActionTile(Icons.Filled.ReceiptLong, "Spending", onOpenExpenses, Modifier.weight(1f), accent = LifeOSSecondary, caption = "Money")
            }
        }
        item { LifeOSEyebrow("Memory") }
        item {
            LifeOSActionTile(
                Icons.Filled.PhotoLibrary,
                "Memories & search",
                onOpenSearch,
                Modifier.fillMaxWidth(),
                accent = LifeOSPrimary,
                caption = "Photos, thoughts, places and everything you captured"
            )
        }
    }
}
