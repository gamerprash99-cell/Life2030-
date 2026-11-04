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
import com.lifeos.app.ui.diary.new.DiaryStickers
import com.lifeos.app.ui.diary.new.DiaryStickerDef
import com.lifeos.app.ui.theme.DiaryActionViolet
import com.lifeos.app.ui.theme.DiaryHairline
import com.lifeos.app.ui.theme.DiaryInkSoft
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryLavender
import com.lifeos.app.ui.theme.LifeOSSpacing

/**
 * Diary Sticker Picker — new visual design.
 *
 * Category strip, emoji/illustration grid, sticker selection.
 * Reuses the existing [DiaryStickers] definition for backward-compatible sticker keys.
 */
@Composable
fun DiaryNewStickerPickerSheet(
    onStickerSelected: (DiaryStickerDef) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var category by remember { mutableStateOf(DiaryStickers.CATEGORIES.first()) }

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
                    "Stickers",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = DiaryNewInk
                    )
                )
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = DiaryNewInkSoft)
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
                itemsIndexed<String>(DiaryStickers.CATEGORIES) { _, cat ->
                    val isSelected = category == cat
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(
                                if (isSelected) DiaryNewSave else DiaryNewLavender.copy(alpha = 0.5f)
                            )
                            .clickable(onClick = { category = cat })
                            .size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            cat.first().toString().uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = if (isSelected) Color.White else DiaryNewInk
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            val stickersInCategory = DiaryStickers.byCategory(category)
            LazyColumn(
                Modifier.fillMaxWidth().padding(horizontal = DiaryNewScreenPadding)
            ) {
                itemsIndexed(stickersInCategory) { _, sticker ->
                    DiaryNewStickerCard(
                        sticker = sticker,
                        onClick = { onStickerSelected(sticker) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DiaryNewStickerCard(
    sticker: DiaryStickerDef,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = onClick)
            .background(Color.Transparent)
            .padding(horizontal = 4.dp)
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(DiaryNewLavender.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                sticker.glyph,
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                sticker.label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = DiaryNewInk
                ),
                maxLines = 1
            )
            Text(
                sticker.category,
                style = MaterialTheme.typography.labelSmall.copy(color = DiaryNewInkSoft),
                maxLines = 1
            )
        }
    }
}


