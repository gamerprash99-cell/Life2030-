package com.lifeos.app.ui.diary

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lifeos.app.ui.theme.DiaryMarginLine
import com.lifeos.app.ui.theme.DiaryPageShadow
import com.lifeos.app.ui.theme.DiaryPaperBase
import com.lifeos.app.ui.theme.DiaryPaperCard
import com.lifeos.app.ui.theme.DiaryRibbon
import com.lifeos.app.ui.theme.DiaryRuledLine
import com.lifeos.app.ui.theme.LifeOSTheme

/**
 * The diary's reading theme.
 *
 * A diary is a physical object: it stays warm paper even when the rest of
 * LifeOS is in dark mode, exactly as a paper notebook would. Pinning the diary
 * to the light scheme means the default ink colours (`onSurface`,
 * `onSurfaceVariant`) resolve to dark pen against the paper instead of the
 * light-on-dark values the rest of the app would hand them.
 */
@Composable
fun DiaryTheme(content: @Composable () -> Unit) {
    LifeOSTheme(darkTheme = false, content = content)
}

/**
 * The desk: a warm cream field with a very faint ruling and the ghost of a red
 * margin rule, so a diary screen reads as paper rather than as a themed app
 * surface. The rules run the full height and show through the gaps between the
 * page leaves laid on top.
 */
fun Modifier.diaryPaperBackground(): Modifier = this
    .background(DiaryPaperBase)
    .ruledPaper(
        lineSpacing = 34.dp,
        lineColor = DiaryRuledLine.copy(alpha = 0.30f),
        marginColor = DiaryMarginLine.copy(alpha = 0.14f),
        marginStart = 34.dp
    )

/**
 * Draws the horizontal rules of a notebook page behind whatever is placed on
 * it, plus the single red rule down the left margin.
 *
 * One canvas regardless of page length, so a long entry costs nothing extra.
 * [lineSpacing] should sit close to the text's line height so handwriting rests
 * on the rules; both rules are kept faint so they can never compete with ink.
 */
fun Modifier.ruledPaper(
    lineSpacing: Dp,
    lineColor: Color,
    marginColor: Color? = null,
    marginStart: Dp = 40.dp,
    topInset: Dp = 0.dp,
    strokeWidth: Dp = 1.dp
): Modifier = drawBehind {
    val spacing = lineSpacing.toPx()
    if (spacing <= 0f) return@drawBehind
    val stroke = strokeWidth.toPx()
    var y = topInset.toPx() + spacing
    while (y < size.height) {
        drawLine(lineColor, Offset(0f, y), Offset(size.width, y), stroke)
        y += spacing
    }
    if (marginColor != null && marginStart.toPx() in 0f..size.width) {
        val x = marginStart.toPx()
        drawLine(marginColor, Offset(x, 0f), Offset(x, size.height), stroke)
    }
}

/**
 * A single page leaf: warm paper lifted off the desk on a soft shadow, with an
 * optional ruled pattern and margin rule drawn inside it.
 *
 * The corner radius is intentionally small — paper has corners, app cards do
 * not — and the shadow is warm rather than black so the leaf reads as paper.
 */
fun Modifier.diaryPage(
    cornerRadius: Dp = 8.dp,
    ruled: Boolean = false,
    lineSpacing: Dp = 30.dp,
    marginStart: Dp = 40.dp,
    shadowElevation: Dp = 3.dp
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    return this
        .shadow(
            elevation = shadowElevation,
            shape = shape,
            clip = false,
            ambientColor = DiaryPageShadow,
            spotColor = DiaryPageShadow
        )
        .clip(shape)
        .background(DiaryPaperCard)
        .then(
            if (ruled) {
                Modifier.ruledPaper(
                    lineSpacing = lineSpacing,
                    lineColor = DiaryRuledLine.copy(alpha = 0.55f),
                    marginColor = DiaryMarginLine.copy(alpha = 0.40f),
                    marginStart = marginStart
                )
            } else {
                Modifier
            }
        )
}

/**
 * The fabric bookmark hanging at the top of the day's page: a crimson ribbon
 * with a notched tail. Purely decorative — the day it marks is announced by the
 * date beside it, so it carries no content description.
 */
@Composable
fun RibbonBookmark(
    modifier: Modifier = Modifier,
    width: Dp = 16.dp,
    height: Dp = 54.dp
) {
    Canvas(modifier = modifier.size(width, height)) {
        val w = size.width
        val h = size.height
        val notch = h * 0.24f
        val tail = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w, h)
            lineTo(w / 2f, h - notch)
            lineTo(0f, h)
            close()
        }
        drawPath(tail, color = DiaryRibbon)
    }
}
