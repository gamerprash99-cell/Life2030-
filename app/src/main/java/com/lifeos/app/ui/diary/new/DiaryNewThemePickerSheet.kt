package com.lifeos.app.ui.diary.new

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifeos.app.domain.model.DiaryAttachment
import com.lifeos.app.ui.diary.new.DiaryThemes
import com.lifeos.app.ui.diary.new.DiaryThemeDef
import com.lifeos.app.ui.theme.DiaryActionViolet
import com.lifeos.app.ui.theme.DiaryHairline
import com.lifeos.app.ui.theme.DiaryInkSoft
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryLavender
import com.lifeos.app.ui.theme.LifeOSSpacing

/**
 * Diary Theme Picker — new visual design.
 *
 * Category tabs, scrollable thumbnail grid, selected state, Apply/Cancel.
 * Reuses the existing [DiaryThemes] definition for backward-compatible theme keys.
 */
@Composable
fun DiaryNewThemePickerSheet(
    currentThemeKey: String,
    onThemeSelected: (DiaryThemeDef) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selected by remember { mutableStateOf(currentThemeKey) }
    var category by remember { mutableStateOf(DiaryThemes.CATEGORIES.first()) }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                .clickable(onClick = onDismiss)
        )

        Column(
            Modifier
                .fillMaxWidth()
                .background(DiaryNewPanel)
                .padding(top = 40.dp, bottom = 16.dp)
                .align(Alignment.BottomCenter)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DiaryNewScreenPadding, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Theme & background",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = DiaryNewInk
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = DiaryNewInkSoft)
                    }
                    TextButton(
                        onClick = { onApply() },
                        enabled = selected != currentThemeKey
                    ) {
                        Text(
                            "Apply",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = DiaryNewSave
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "Category",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = DiaryNewInkSoft
                ),
                modifier = Modifier
                    .padding(
                        start = DiaryNewScreenPadding,
                        top = 4.dp,
                        end = DiaryNewScreenPadding,
                        bottom = 8.dp
                    )
            )
            LazyRow(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DiaryNewScreenPadding),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed<String>(DiaryThemes.CATEGORIES) { _, cat ->
                    val isSelected = category == cat
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) DiaryNewSave else DiaryNewLavender.copy(alpha = 0.5f)
                            )
                            .clickable(onClick = { category = cat })
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            cat,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = if (isSelected) Color.White else DiaryNewInk
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            val themesInCategory = DiaryThemes.byCategory(category)
            LazyColumn(
                Modifier.fillMaxWidth().padding(horizontal = DiaryNewScreenPadding)
            ) {
                itemsIndexed(themesInCategory) { _, theme ->
                    DiaryNewThemeCard(
                        theme = theme,
                        isSelected = theme.key == selected,
                        onClick = {
                            selected = theme.key
                            onThemeSelected(theme)
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DiaryNewThemeCard(
    theme: DiaryThemeDef,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = onClick)
            .border(
                if (isSelected) 2.dp else 0.dp,
                DiaryNewSave,
                RoundedCornerShape(12.dp)
            )
            .background(
                if (isSelected) DiaryNewLavender.copy(alpha = 0.35f)
                else Color.Transparent
            )
            .padding(horizontal = 4.dp)
    ) {
        Box(
            Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)).background(theme.paperCard),
            contentAlignment = Alignment.Center
        ) {
            DiaryNewThemePreview(theme = theme)
        }
        Spacer(Modifier.width(12.dp))
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                theme.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = DiaryNewInk
                ),
                maxLines = 1
            )
            Text(
                theme.category,
                style = MaterialTheme.typography.labelSmall.copy(color = DiaryNewInkSoft),
                maxLines = 1
            )
        }
        if (isSelected) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(DiaryNewSave),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", style = MaterialTheme.typography.titleLarge, color = Color.White)
            }
        }
    }
}

@Composable
private fun DiaryNewThemePreview(theme: DiaryThemeDef) {
    androidx.compose.foundation.Canvas(Modifier.size(56.dp)) {
        val w = size.width
        val h = size.height
        val pageLeft = w * 0.18f
        val pageTop = h * 0.14f
        val pageRight = w * 0.88f
        val pageBottom = h * 0.86f
        drawRoundRect(
            color = theme.paperCard,
            topLeft = androidx.compose.ui.geometry.Offset(pageLeft, pageTop),
            size = androidx.compose.ui.geometry.Size(pageRight - pageLeft, pageBottom - pageTop),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
        )
        for (i in 0..1) {
            val y = pageTop + h * (0.30f + i * 0.22f)
            drawLine(
                color = theme.ruledLine,
                start = androidx.compose.ui.geometry.Offset(pageLeft + w * 0.06f, y),
                end = androidx.compose.ui.geometry.Offset(pageRight - w * 0.06f, y),
                strokeWidth = 1.dp.toPx()
            )
        }
        drawLine(
            color = theme.marginLine,
            start = androidx.compose.ui.geometry.Offset(pageLeft + w * 0.04f, pageTop),
            end = androidx.compose.ui.geometry.Offset(pageLeft + w * 0.04f, pageBottom),
            strokeWidth = 1.dp.toPx()
        )
    }
}


