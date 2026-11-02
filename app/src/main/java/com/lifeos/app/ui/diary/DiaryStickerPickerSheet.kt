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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeos.app.domain.model.DiaryAttachment
import com.lifeos.app.ui.theme.DiaryActionViolet
import com.lifeos.app.ui.theme.DiaryHairline
import com.lifeos.app.ui.theme.DiaryInkSoft
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryLavender
import com.lifeos.app.ui.theme.LifeOSSpacing
import kotlin.math.roundToInt

data class DiaryStickerDef(
    val assetKey: String,
    val label: String,
    val glyph: String,
    val category: String
)

object DiaryStickers {
    val ALL = listOf(
        DiaryStickerDef("nature_leaf", "Leaf", "🍃", "Nature"),
        DiaryStickerDef("nature_flower", "Flower", "🌸", "Nature"),
        DiaryStickerDef("nature_sun", "Sun", "☀️", "Nature"),
        DiaryStickerDef("nature_moon", "Moon", "🌙", "Nature"),
        DiaryStickerDef("nature_star", "Star", "⭐", "Nature"),
        DiaryStickerDef("feel_heart", "Heart", "❤️", "Feelings"),
        DiaryStickerDef("feel_sparkle", "Sparkle", "✨", "Feelings"),
        DiaryStickerDef("feel_cloud", "Cloud", "☁️", "Feelings"),
        DiaryStickerDef("feel_rainbow", "Rainbow", "🌈", "Feelings"),
        DiaryStickerDef("act_book", "Book", "📖", "Activities"),
        DiaryStickerDef("act_coffee", "Coffee", "☕", "Activities"),
        DiaryStickerDef("act_camera", "Camera", "📷", "Activities"),
        DiaryStickerDef("act_music", "Music", "🎵", "Activities"),
        DiaryStickerDef("sym_check", "Check", "✅", "Symbols"),
        DiaryStickerDef("sym_pin", "Pin", "📌", "Symbols"),
        DiaryStickerDef("sym_flag", "Flag", "🚩", "Symbols")
    )

    val CATEGORIES = ALL.map { it.category }.distinct()

    fun byCategory(category: String): List<DiaryStickerDef> =
        ALL.filter { it.category == category }

    fun byKey(key: String): DiaryStickerDef =
        ALL.firstOrNull { it.assetKey == key } ?: DiaryStickerDef("placeholder", "Sticker", "●", "Symbols")
}

@Composable
fun DiaryStickerPickerSheet(
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
                    "Stickers",
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
                itemsIndexed(DiaryStickers.CATEGORIES) { _, cat ->
                    val isSelected = category == cat
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
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

            val stickersInCategory = DiaryStickers.byCategory(category)
            LazyColumn(
                Modifier.fillMaxWidth().padding(horizontal = LifeOSSpacing.screenPadding)
            ) {
                itemsIndexed(stickersInCategory) { _, sticker ->
                    StickerCard(
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
private fun StickerCard(sticker: DiaryStickerDef, onClick: () -> Unit) {
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
                sticker.glyph,
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                sticker.label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = DiaryInkViolet,
                maxLines = 1
            )
            Text(
                sticker.category,
                style = MaterialTheme.typography.labelSmall,
                color = DiaryInkSoft,
                maxLines = 1
            )
        }
    }
}

@Composable
fun DiaryStickerLayer(
    stickers: List<DiaryAttachment.Sticker>,
    pageSize: androidx.compose.ui.geometry.Size,
    onStickerRemoved: (String, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    Box(modifier = modifier.fillMaxSize()) {
        stickers.forEach { sticker ->
            val xPx = sticker.x * pageSize.width
            val yPx = sticker.y * pageSize.height
            val stickerDef = DiaryStickers.byKey(sticker.assetKey)
            val sizeDp = (48f * sticker.scale).dp
            val sizePx = with(density) { sizeDp.toPx() }
            val halfPx = sizePx / 2f

            Box(
                Modifier
                    .size(sizeDp)
                    .clip(CircleShape)
                    .background(DiaryLavender.copy(alpha = 0.7f))
                    .border(1.dp, DiaryHairline, CircleShape)
                    .offset { IntOffset((xPx - halfPx).roundToInt(), (yPx - halfPx).roundToInt()) },
                contentAlignment = Alignment.Center
            ) {
                val glyphSizeDp = (24f * sticker.scale).dp
                Text(
                    stickerDef.glyph,
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.size(glyphSizeDp)
                )
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(DiaryActionViolet)
                        .clickable(onClick = { onStickerRemoved(sticker.assetKey, sticker.x, sticker.y) }),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "✕",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}
