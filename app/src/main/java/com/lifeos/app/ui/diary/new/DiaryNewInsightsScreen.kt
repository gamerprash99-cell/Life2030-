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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Fireplace
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.domain.intelligence.DiaryInsights
import com.lifeos.app.domain.model.DiaryMood
import com.lifeos.app.domain.model.DiaryMoods
import com.lifeos.app.ui.diary.new.DiaryInsightsViewModel
import com.lifeos.app.ui.theme.DiaryActionViolet
import com.lifeos.app.ui.theme.DiaryHandwriting
import com.lifeos.app.ui.theme.DiaryInkSoft
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryPaperCard
import com.lifeos.app.ui.theme.LifeOSSpacing
import kotlin.math.roundToInt

/**
 * Diary Insights — new visual design.
 *
 * Writing streak, entry statistics, mood trends, recurring words, local insights,
 * privacy explanation. Reuses [DiaryInsightsViewModel] for state.
 */
@Composable
fun DiaryNewInsightsScreen(
    onBack: () -> Unit,
    onReload: () -> Unit
) {
    val locator = LocalServiceLocator.current
    val viewModel: DiaryInsightsViewModel = viewModel(
        factory = LambdaViewModelFactory {
            DiaryInsightsViewModel(locator.getDiaryInsightsUseCase)
        }
    )

    val state by viewModel.state.collectAsStateWithLifecycle()
    val insights = state.insights
    val streakLength = state.streak.currentLength
    val totalEntries = insights?.statistics?.entryCount ?: 0
    val moodDistribution = insights?.mood?.distribution ?: emptyList()
    val recurringKeywords = insights?.patterns?.recurringKeywords ?: emptyList()

    Column(Modifier.fillMaxSize().background(DiaryNewCanvas)) {
        // Header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = DiaryNewScreenPadding, vertical = DiaryNewSectionSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(DiaryNewMinTouchTarget)) {
                Icon(
                    ArrowBack,
                    contentDescription = "Back",
                    tint = DiaryNewInk
                )
            }
            Text(
                "Insights",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = DiaryHandwriting,
                    fontWeight = FontWeight.Bold,
                    color = DiaryNewInk
                ),
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp)
            )
            IconButton(onClick = onReload) {
                Icon(
                    Refresh,
                    contentDescription = "Reload",
                    tint = DiaryNewInkSoft
                )
            }
        }

        Spacer(Modifier.height(DiaryNewSectionSpacing))

        // Insights content
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = DiaryNewScreenPadding)
                .fillMaxWidth()
        ) {
            if (state.isLoading) {
                Box(
                    Modifier.fillMaxWidth().height(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator(color = DiaryNewSave)
                }
            } else {
                // Streak card
                DiaryNewStatCard(
                    title = "Writing streak",
                    value = "${streakLength} day${if (streakLength == 1) "" else "s"}",
                    subtitle = "Consecutive days with entries",
                    icon = Fireplace,
                    iconTint = DiaryNewSave
                )
                Spacer(Modifier.height(DiaryNewSectionSpacing))

                // Entry statistics
                DiaryNewStatCard(
                    title = "Total entries",
                    value = totalEntries.toString(),
                    subtitle = "Entries in your diary",
                    icon = Book,
                    iconTint = DiaryNewInk
                )
                Spacer(Modifier.height(DiaryNewSectionSpacing))

                // Mood breakdown
                Text(
                    "Mood trends",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = DiaryNewInkSoft
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (moodDistribution.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        moodDistribution.forEach { share ->
                            val mood = DiaryMoods.fromStored(share.moodKey)
                            mood?.let { m ->
                                DiaryNewMoodStatItem(
                                    mood = m,
                                    count = share.count,
                                    total = totalEntries
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        "No moods recorded yet",
                        style = MaterialTheme.typography.bodyMedium.copy(color = DiaryNewInkSoft)
                    )
                }
                Spacer(Modifier.height(DiaryNewSectionSpacing))

                // Recurring words
                Text(
                    "Recurring words",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = DiaryNewInkSoft
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (recurringKeywords.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        recurringKeywords.take(6).forEach { keyword ->
                            Box(
                                Modifier
                                    .clip(CircleShape)
                                    .background(DiaryNewLavender.copy(alpha = 0.6f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "${keyword.word} (${keyword.occurrences})",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = DiaryNewInk,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        "Not enough data yet",
                        style = MaterialTheme.typography.bodyMedium.copy(color = DiaryNewInkSoft)
                    )
                }
                Spacer(Modifier.height(DiaryNewSectionSpacing))

                // Privacy explanation
                DiaryNewPanel(
                    modifier = Modifier.padding(vertical = DiaryNewSectionSpacing),
                    cornerRadius = 16
                ) {
                    Column(
                        Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Private by design",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = DiaryHandwriting,
                                fontWeight = FontWeight.Bold,
                                color = DiaryNewInk
                            )
                        )
                        Text(
                            "Your diary stays on your device. Nothing is uploaded, tracked, or shared. All insights are computed locally from your own entries.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = DiaryNewInkSoft)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiaryNewStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color
) {
    DiaryNewPanel(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = DiaryNewInkSoft
                    )
                )
                Text(
                    value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = DiaryHandwriting,
                        fontWeight = FontWeight.Bold,
                        color = DiaryNewInk
                    )
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(color = DiaryNewInkMuted)
                )
            }
            Spacer(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun DiaryNewMoodStatItem(
    mood: DiaryMood,
    count: Int,
    total: Int
) {
    val pct = if (total > 0) (count * 100 / total) else 0
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(DiaryNewMoodColor(mood)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                mood.emoji,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            mood.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = DiaryNewInk
            ),
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        Text(
            "$pct%",
            style = MaterialTheme.typography.labelSmall.copy(color = DiaryNewInkSoft)
        )
    }
}
