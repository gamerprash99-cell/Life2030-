package com.lifeos.app.ui.diary

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.lifeos.app.ui.theme.DiaryActionViolet
import com.lifeos.app.ui.theme.DiaryHairline
import com.lifeos.app.ui.theme.DiaryInkSoft
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryLavender
import com.lifeos.app.ui.theme.LifeOSSpacing

data class DiaryThemeDef(
    val key: String,
    val name: String,
    val category: String,
    val paperBase: Color,
    val paperCard: Color,
    val ruledLine: Color,
    val marginLine: Color,
    val isDark: Boolean = false
)

object DiaryThemes {
    val ORIGINAL = DiaryThemeDef(
        key = "original", name = "Original", category = "Customize",
        paperBase = Color(0xFFEFE6D2), paperCard = Color(0xFFFCF7EA),
        ruledLine = Color(0xFFD3C4A3), marginLine = Color(0xFFC4796A)
    )
    val PASTEL_PINK = DiaryThemeDef(
        key = "pastel_pink", name = "Pastel Pink", category = "Popular",
        paperBase = Color(0xFFFDF0F5), paperCard = Color(0xFFFFF5F8),
        ruledLine = Color(0xFFEACFD9), marginLine = Color(0xFFE39AA3)
    )
    val WARM_CREAM = DiaryThemeDef(
        key = "warm_cream", name = "Warm Cream", category = "Popular",
        paperBase = Color(0xFFFDF6EC), paperCard = Color(0xFFFFFBF0),
        ruledLine = Color(0xFFEED9B4), marginLine = Color(0xFFE3B56A)
    )
    val DEEP_PAPER = DiaryThemeDef(
        key = "deep_paper", name = "Deep Paper", category = "Dark",
        paperBase = Color(0xFF2A2330), paperCard = Color(0xFF3A3040),
        ruledLine = Color(0xFF4A3F50), marginLine = Color(0xFF8A7BA0), isDark = true
    )
    val ARTSY_WASH = DiaryThemeDef(
        key = "artsy_wash", name = "Artsy Wash", category = "Artsy",
        paperBase = Color(0xFFF3E5F5), paperCard = Color(0xFFF9EFFD),
        ruledLine = Color(0xFFE1BEE7), marginLine = Color(0xFFCE93D8)
    )
    val CUTE_MINT = DiaryThemeDef(
        key = "cute_mint", name = "Cute Mint", category = "Cute",
        paperBase = Color(0xFFE8F5F0), paperCard = Color(0xFFFEFFF9),
        ruledLine = Color(0xFFB2DFDB), marginLine = Color(0xFF80CBC4)
    )
    val LIFESTYLE_SAGE = DiaryThemeDef(
        key = "lifestyle_sage", name = "Lifestyle Sage", category = "Lifestyle",
        paperBase = Color(0xFFF1F4EC), paperCard = Color(0xFFF9FDF2),
        ruledLine = Color(0xFFDCEDC8), marginLine = Color(0xFF9CCC65)
    )

    val CATEGORIES = listOf("Customize", "Popular", "Dark", "Artsy", "Cute", "Lifestyle")
    val ALL = listOf(ORIGINAL, PASTEL_PINK, WARM_CREAM, DEEP_PAPER, ARTSY_WASH, CUTE_MINT, LIFESTYLE_SAGE)

    fun byKey(key: String): DiaryThemeDef = ALL.firstOrNull { it.key == key } ?: ORIGINAL
    fun byCategory(category: String): List<DiaryThemeDef> = ALL.filter { it.category == category }
}

@Composable
fun DiaryThemePickerSheet(
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
                    "Theme & background",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = DiaryInkViolet
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = DiaryInkSoft)
                    }
                    TextButton(onClick = { onApply() }, enabled = selected != currentThemeKey) {
                        Text(
                            "Apply",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = DiaryActionViolet
                        )
                    }
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
                itemsIndexed(DiaryThemes.CATEGORIES) { _, cat ->
                    val isSelected = category == cat
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) DiaryActionViolet else DiaryLavender.copy(alpha = 0.5f))
                            .clickable(onClick = { category = cat })
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            cat,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = if (isSelected) Color.White else DiaryInkViolet
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            val themesInCategory = DiaryThemes.byCategory(category)
            LazyColumn(
                Modifier.fillMaxWidth().padding(horizontal = LifeOSSpacing.screenPadding)
            ) {
                itemsIndexed(themesInCategory) { _, theme ->
                    ThemeCard(
                        theme = theme,
                        isSelected = theme.key == selected,
                        onClick = { selected = theme.key; onThemeSelected(theme) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ThemeCard(
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
                DiaryActionViolet,
                RoundedCornerShape(12.dp)
            )
            .background(if (isSelected) DiaryLavender.copy(alpha = 0.35f) else Color.Transparent)
            .padding(horizontal = 4.dp)
    ) {
        Box(
            Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)).background(theme.paperCard),
            contentAlignment = Alignment.Center
        ) {
            ThemePreview(theme = theme)
        }
        Spacer(Modifier.width(12.dp))
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                theme.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = DiaryInkViolet,
                maxLines = 1
            )
            Text(
                theme.category,
                style = MaterialTheme.typography.labelSmall,
                color = DiaryInkSoft,
                maxLines = 1
            )
        }
        if (isSelected) {
            Box(
                Modifier.size(28.dp).clip(CircleShape).background(DiaryActionViolet),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", style = MaterialTheme.typography.titleLarge, color = Color.White)
            }
        }
    }
}

@Composable
private fun ThemePreview(theme: DiaryThemeDef) {
    Canvas(Modifier.size(56.dp)) {
        val w = size.width
        val h = size.height
        val pageLeft = w * 0.18f
        val pageTop = h * 0.14f
        val pageRight = w * 0.88f
        val pageBottom = h * 0.86f
        drawRoundRect(
            color = theme.paperCard,
            topLeft = Offset(pageLeft, pageTop),
            size = Size(pageRight - pageLeft, pageBottom - pageTop),
            cornerRadius = CornerRadius(4.dp.toPx())
        )
        for (i in 0..1) {
            val y = pageTop + h * (0.30f + i * 0.22f)
            drawLine(
                color = theme.ruledLine,
                start = Offset(pageLeft + w * 0.06f, y),
                end = Offset(pageRight - w * 0.06f, y),
                strokeWidth = 1.dp.toPx()
            )
        }
        drawLine(
            color = theme.marginLine,
            start = Offset(pageLeft + w * 0.04f, pageTop),
            end = Offset(pageLeft + w * 0.04f, pageBottom),
            strokeWidth = 1.dp.toPx()
        )
    }
}
