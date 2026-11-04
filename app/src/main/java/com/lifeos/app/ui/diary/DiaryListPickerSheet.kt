package com.lifeos.app.ui.diary

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
import androidx.compose.ui.unit.sp
import com.lifeos.app.ui.theme.DiaryActionViolet
import com.lifeos.app.ui.theme.DiaryInkSoft
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryLavender
import com.lifeos.app.ui.theme.LifeOSSpacing

object DiaryListTemplates {
    val ALL = listOf(
        DiaryListTemplate("bullet", "Bullet list", "•", "Lists"),
        DiaryListTemplate("numbered", "Numbered list", "1.", "Lists"),
        DiaryListTemplate("checkbox", "Checkbox list", "- [ ]", "Tasks"),
        DiaryListTemplate("dash", "Dash list", "–", "Lists")
    )

    val CATEGORIES = ALL.map { it.category }.distinct()

    fun byCategory(category: String): List<DiaryListTemplate> =
        ALL.filter { it.category == category }
}

data class DiaryListTemplate(
    val key: String,
    val name: String,
    val marker: String,
    val category: String
)

@Composable
fun DiaryListPickerSheet(
    onTemplateSelected: (DiaryListTemplate) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val initialCategory = DiaryListTemplates.CATEGORIES.first()
    var category by remember { mutableStateOf(initialCategory) }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                .clickable(onClick = onDismiss)
        )

        Column(
            Modifier
                .fillMaxWidth()
                .background(DiaryLavender)
                .padding(top = 40.dp, bottom = 16.dp)
                .align(Alignment.BottomCenter)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = LifeOSSpacing.screenPadding, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "List style",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = DiaryInkViolet
                )
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = DiaryInkSoft)
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "Category",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = DiaryInkSoft,
                modifier = Modifier.padding(start = LifeOSSpacing.screenPadding, top = 4.dp, end = LifeOSSpacing.screenPadding, bottom = 8.dp)
            )
            LazyRow(
                Modifier.fillMaxWidth().padding(horizontal = LifeOSSpacing.screenPadding),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(DiaryListTemplates.CATEGORIES) { _, cat ->
                    val isSelected = category == cat
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(if (isSelected) DiaryActionViolet else DiaryLavender.copy(alpha = 0.5f))
                            .clickable(onClick = { category = cat })
                            .size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            cat.first().toString().uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = if (isSelected) Color.White else DiaryInkViolet
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            val templatesInCategory = DiaryListTemplates.byCategory(category)
            LazyColumn(
                Modifier.fillMaxWidth().padding(horizontal = LifeOSSpacing.screenPadding)
            ) {
                itemsIndexed(templatesInCategory) { _, template ->
                    ListTemplateCard(
                        template = template,
                        onClick = { onTemplateSelected(template) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ListTemplateCard(template: DiaryListTemplate, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = onClick)
            .background(Color.Transparent)
            .padding(horizontal = 4.dp)
    ) {
        Box(
            Modifier.size(52.dp).clip(CircleShape).background(DiaryLavender.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                template.marker,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = DiaryInkViolet
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                template.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = DiaryInkViolet,
                maxLines = 1
            )
            Text(
                template.category,
                style = MaterialTheme.typography.labelSmall,
                color = DiaryInkSoft,
                maxLines = 1
            )
        }
    }
}
