package com.lifeos.app.ui.diary

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeos.app.ui.theme.DiaryInkSoft
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.DiaryMarginLine
import com.lifeos.app.ui.theme.DiaryPaperCard
import com.lifeos.app.ui.theme.DiaryRibbon
import com.lifeos.app.ui.theme.DiaryRuledLine
import com.lifeos.app.ui.theme.LifeOSSpacing

/**
 * The widest the empty state's artwork may be.
 *
 * It is a share of the height the FAB does *not* cover, capped at the design's
 * own size and floored so a short window shrinks the artwork rather than
 * reducing it to a dot. Expressed as arithmetic rather than as a measured
 * layout so the rule is decidable on the JVM — see [DiaryLayoutClearanceTest].
 */
internal fun emptyStateArtworkMax(
    availableHeight: Dp,
    maximum: Dp = LifeOSSpacing.diaryEmptyArtworkMaxSize,
    fill: Float = LifeOSSpacing.diaryEmptyArtworkFill,
    minimum: Dp = LifeOSSpacing.diaryEmptyArtworkMinSize
): Dp = minOf(maximum, availableHeight * fill).coerceAtLeast(minimum)

/**
 * Empty Diary page.
 *
 * The empty state is intentionally editorial rather than card-heavy: a quiet
 * journal illustration, a clear emotional invitation, and one obvious action.
 * The illustration is drawn locally with Canvas so the released app needs no
 * remote image, network request, or additional asset dependency.
 *
 * It is drawn underneath the FAB, so it reserves [LifeOSSpacing.diaryFabOccupiedBottom]
 * — the button's *whole* band, not just its offset. Centring the invitation in
 * the full-height box without that reservation put "YOUR STORY STARTS HERE" and
 * the line under it inside the button on any phone whose box was not much taller
 * than the artwork.
 *
 * Both halves of that reservation are structural rather than fixed. The artwork
 * is sized from the width *and* the height actually left over after the band,
 * between a floor and the design's 300dp, so it shrinks on a small phone instead
 * of pushing the text out of the box. And the column scrolls, because a large
 * system font scale still outgrows even that — a reachable last line rather than a
 * clipped one. `heightIn` sits *inside* `verticalScroll` deliberately:
 * `verticalScroll` hands its child `minHeight = 0`, so without an explicit floor
 * the column would size to its content, `Arrangement.Center` would become a
 * no-op, and the invitation would quietly stop being centred.
 */
@Composable
fun DiaryEmptyState(
    dayLabel: String,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val band = LifeOSSpacing.diaryFabOccupiedBottom
        val artworkMax = emptyStateArtworkMax(maxHeight - band)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .fadeInAsContent()
                .padding(horizontal = LifeOSSpacing.screenPadding)
                .padding(bottom = band),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // The illustration is visual only. Creating a memory is intentionally
            // handled by the persistent lower-right FAB, matching the reference
            // interaction and avoiding an invisible large tap target.
            Box(
                modifier = Modifier
                    // The width clamp comes *before* the ratio on purpose: the
                    // ratio derives the height from whatever width it is given, so
                    // clamping afterwards would leave the height matching the old,
                    // larger width.
                    .widthIn(max = artworkMax)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                EmptyDiaryIllustration(
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "YOUR STORY STARTS HERE",
                style = MaterialTheme.typography.labelSmall,
                color = DiaryInkViolet,
                letterSpacing = 2.2.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Nothing recorded on $dayLabel.",
                style = MaterialTheme.typography.bodyLarge,
                color = DiaryInkSoft,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Tap + to capture your day ✨",
                style = MaterialTheme.typography.bodyMedium,
                color = DiaryInkSoft.copy(alpha = 0.62f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Small local illustration: an open ruled notebook with a fountain pen resting
 * on it, drawn in the same warm paper-and-ink palette as the rest of the diary.
 * Entirely local — no raster asset, no network.
 */
@Composable
private fun EmptyDiaryIllustration(modifier: Modifier = Modifier) {
    val paper = DiaryPaperCard
    val line = DiaryRuledLine
    val margin = DiaryMarginLine
    val ribbon = DiaryRibbon
    val ink = DiaryInkViolet
    val desk = DiaryMarginLine.copy(alpha = 0.10f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)

        // Warm halo on the desk.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(desk, Color.Transparent),
                center = center,
                radius = w * 0.48f
            ),
            radius = w * 0.48f,
            center = center
        )

        // Open notebook: two facing pages meeting at a spine.
        val bookTop = h * 0.30f
        val bookBottom = h * 0.76f
        val bookLeft = w * 0.16f
        val bookRight = w * 0.84f
        val spine = w * 0.50f

        val leftPage = Path().apply {
            moveTo(spine, bookTop)
            cubicTo(w * 0.40f, h * 0.27f, w * 0.26f, h * 0.32f, bookLeft, h * 0.38f)
            lineTo(bookLeft, bookBottom - h * 0.04f)
            cubicTo(w * 0.28f, h * 0.72f, w * 0.42f, h * 0.74f, spine, h * 0.77f)
            close()
        }
        val rightPage = Path().apply {
            moveTo(spine, bookTop)
            cubicTo(w * 0.60f, h * 0.27f, w * 0.74f, h * 0.32f, bookRight, h * 0.38f)
            lineTo(bookRight, bookBottom - h * 0.04f)
            cubicTo(w * 0.72f, h * 0.72f, w * 0.58f, h * 0.74f, spine, h * 0.77f)
            close()
        }

        // Page drop shadow, then the paper itself.
        drawPath(leftPage, color = Color.Black.copy(alpha = 0.06f))
        drawPath(rightPage, color = Color.Black.copy(alpha = 0.06f))
        drawPath(leftPage, color = paper)
        drawPath(rightPage, color = paper)
        drawPath(leftPage, color = ink.copy(alpha = 0.18f), style = Stroke(width = 1.5.dp.toPx(), join = StrokeJoin.Round))
        drawPath(rightPage, color = ink.copy(alpha = 0.18f), style = Stroke(width = 1.5.dp.toPx(), join = StrokeJoin.Round))

        // Ruled lines on both pages, with the red margin rule on the left page.
        for (i in 0..4) {
            val y = h * (0.42f + i * 0.06f)
            drawLine(line, Offset(w * 0.30f, y), Offset(w * 0.45f, y - w * 0.004f), 1.5.dp.toPx(), cap = StrokeCap.Round)
            drawLine(line, Offset(w * 0.55f, y - w * 0.004f), Offset(w * 0.70f, y), 1.5.dp.toPx(), cap = StrokeCap.Round)
        }
        drawLine(margin, Offset(w * 0.26f, h * 0.38f), Offset(w * 0.26f, h * 0.73f), 1.5.dp.toPx(), cap = StrokeCap.Round)

        // A few ink strokes: a memory already beginning to be written.
        drawLine(ink.copy(alpha = 0.55f), Offset(w * 0.32f, h * 0.44f), Offset(w * 0.43f, h * 0.44f), 2.5.dp.toPx(), cap = StrokeCap.Round)
        drawLine(ink.copy(alpha = 0.42f), Offset(w * 0.32f, h * 0.50f), Offset(w * 0.40f, h * 0.50f), 2.5.dp.toPx(), cap = StrokeCap.Round)

        // Ribbon bookmark at the spine.
        val ribbonWidth = w * 0.05f
        val ribbonPath = Path().apply {
            moveTo(spine - ribbonWidth / 2f, bookTop)
            lineTo(spine + ribbonWidth / 2f, bookTop)
            lineTo(spine + ribbonWidth / 2f, h * 0.62f)
            lineTo(spine, h * 0.56f)
            lineTo(spine - ribbonWidth / 2f, h * 0.62f)
            close()
        }
        drawPath(ribbonPath, color = ribbon)

        // Fountain pen resting across the lower-right page.
        val penStart = Offset(w * 0.58f, h * 0.66f)
        val penEnd = Offset(w * 0.80f, h * 0.52f)
        drawLine(ink, penStart, penEnd, 6.dp.toPx(), cap = StrokeCap.Round)
        // Nib.
        drawCircle(ink.copy(alpha = 0.85f), radius = w * 0.018f, center = penEnd)
    }
}
