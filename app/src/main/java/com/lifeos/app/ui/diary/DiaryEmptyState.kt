package com.lifeos.app.ui.diary

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeos.app.ui.theme.DiaryInkViolet
import com.lifeos.app.ui.theme.LifeOSSpacing

/**
 * Empty Diary page.
 *
 * The empty state is intentionally editorial rather than card-heavy: a quiet
 * journal illustration, a clear emotional invitation, and one obvious action.
 * The illustration is drawn locally with Canvas so the released app needs no
 * remote image, network request, or additional asset dependency.
 */
@Composable
fun DiaryEmptyState(
    dayLabel: String,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .fadeInAsContent()
            .padding(horizontal = LifeOSSpacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .clickable(onClick = onCreate),
            contentAlignment = Alignment.Center
        ) {
            val illustrationSize = (maxWidth * 0.88f)
                .coerceAtMost(maxHeight * 0.62f)
                .coerceIn(230.dp, 430.dp)

            Box(Modifier.size(illustrationSize)) {
                EmptyDiaryIllustration(Modifier.fillMaxSize())
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Your story starts here",
            style = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 25.sp,
                lineHeight = 31.sp,
                letterSpacing = (-0.1).sp
            ),
            color = DiaryInkViolet,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Nothing recorded on $dayLabel.",
            style = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 22.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(7.dp))

        Text(
            text = "Tap the + button to capture your day ✨",
            style = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Small local illustration: an open journal, crescent moon, leaves and stars.
 * It deliberately uses soft LifeOS lavender/violet tones and no raster asset.
 */
@Composable
private fun EmptyDiaryIllustration(modifier: Modifier = Modifier) {
    val lavender = Color(0xFFEADDFF)
    val softLavender = Color(0xFFF3EDF7)
    val violet = Color(0xFF6F45B8)
    val deepViolet = Color(0xFF21005D)
    val gold = Color(0xFFFFD66B)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)

        // Soft atmospheric halo.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(lavender.copy(alpha = 0.65f), Color.Transparent),
                center = center,
                radius = w * 0.46f
            ),
            radius = w * 0.46f,
            center = center
        )

        // Floating cloud / paper glow.
        drawOval(
            brush = Brush.linearGradient(
                colors = listOf(softLavender, lavender.copy(alpha = 0.55f))
            ),
            topLeft = Offset(w * 0.12f, h * 0.56f),
            size = Size(w * 0.76f, h * 0.25f)
        )

        // Crescent moon.
        drawCircle(
            color = violet.copy(alpha = 0.9f),
            radius = w * 0.075f,
            center = Offset(w * 0.69f, h * 0.29f)
        )
        drawCircle(
            color = softLavender,
            radius = w * 0.075f,
            center = Offset(w * 0.725f, h * 0.265f)
        )

        // Stars.
        fun star(x: Float, y: Float, r: Float) {
            val p = Path()
            p.moveTo(x, y - r)
            p.lineTo(x + r * 0.22f, y - r * 0.22f)
            p.lineTo(x + r, y)
            p.lineTo(x + r * 0.22f, y + r * 0.22f)
            p.lineTo(x, y + r)
            p.lineTo(x - r * 0.22f, y + r * 0.22f)
            p.lineTo(x - r, y)
            p.lineTo(x - r * 0.22f, y - r * 0.22f)
            p.close()
            drawPath(p, color = gold.copy(alpha = 0.9f))
        }
        star(w * 0.24f, h * 0.31f, w * 0.025f)
        star(w * 0.80f, h * 0.40f, w * 0.032f)
        star(w * 0.57f, h * 0.18f, w * 0.018f)

        // Open-book shadow.
        val bookTop = h * 0.54f
        val bookBottom = h * 0.77f
        val bookLeft = w * 0.22f
        val bookRight = w * 0.78f
        val spine = w * 0.50f

        val leftPage = Path().apply {
            moveTo(spine, bookTop)
            cubicTo(w * 0.43f, h * 0.51f, w * 0.30f, h * 0.55f, bookLeft, h * 0.61f)
            lineTo(bookLeft + w * 0.04f, bookBottom)
            cubicTo(w * 0.34f, h * 0.73f, w * 0.44f, h * 0.72f, spine, h * 0.76f)
            close()
        }
        val rightPage = Path().apply {
            moveTo(spine, bookTop)
            cubicTo(w * 0.57f, h * 0.51f, w * 0.70f, h * 0.55f, bookRight, h * 0.61f)
            lineTo(bookRight - w * 0.04f, bookBottom)
            cubicTo(w * 0.66f, h * 0.73f, w * 0.56f, h * 0.72f, spine, h * 0.76f)
            close()
        }

        drawPath(leftPage, color = Color.White.copy(alpha = 0.95f))
        drawPath(rightPage, color = Color.White.copy(alpha = 0.9f))
        drawPath(
            leftPage,
            color = violet.copy(alpha = 0.28f),
            style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round)
        )
        drawPath(
            rightPage,
            color = violet.copy(alpha = 0.28f),
            style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round)
        )

        // Page lines.
        for (i in 0..3) {
            val y = h * (0.60f + i * 0.035f)
            drawLine(
                color = violet.copy(alpha = 0.22f),
                start = Offset(w * 0.29f, y),
                end = Offset(w * 0.45f, y - w * 0.006f),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = violet.copy(alpha = 0.22f),
                start = Offset(w * 0.55f, y - w * 0.006f),
                end = Offset(w * 0.71f, y),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Small sprout growing from the book: a visual metaphor for a memory
        // beginning rather than an empty/error state.
        drawLine(
            color = deepViolet.copy(alpha = 0.82f),
            start = Offset(w * 0.49f, h * 0.55f),
            end = Offset(w * 0.43f, h * 0.39f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawOval(
            color = violet.copy(alpha = 0.8f),
            topLeft = Offset(w * 0.37f, h * 0.35f),
            size = Size(w * 0.09f, h * 0.05f)
        )
        drawOval(
            color = violet.copy(alpha = 0.65f),
            topLeft = Offset(w * 0.42f, h * 0.42f),
            size = Size(w * 0.08f, h * 0.045f)
        )
    }
}
