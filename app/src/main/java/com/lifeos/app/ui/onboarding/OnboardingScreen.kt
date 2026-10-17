package com.lifeos.app.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeos.app.ui.components.LifeOSPrimaryButton
import com.lifeos.app.ui.theme.FigmaAccent
import com.lifeos.app.ui.theme.FigmaElevated
import com.lifeos.app.ui.theme.FigmaMuted
import com.lifeos.app.ui.theme.FigmaOnAccent
import com.lifeos.app.ui.theme.FigmaStroke
import com.lifeos.app.ui.theme.FigmaText
import com.lifeos.app.ui.theme.FigmaTertiary
import com.lifeos.app.ui.theme.FigmaWelcomeBottom
import com.lifeos.app.ui.theme.FigmaWelcomeMid
import com.lifeos.app.ui.theme.FigmaWelcomeTop

/**
 * "Welcome to LifeOS" — the redesign's first frame.
 *
 * One hero: the LifeOS mark, an orbital "personal universe" illustration, the
 * promise ("Your personal life, under your control."), the privacy line and a
 * single mint action. It paints its **own** dark welcome gradient rather than
 * the app canvas, so the very first thing a person sees is the design exactly as
 * drawn — independent of the theme setting.
 *
 * Behaviour is preserved from the previous onboarding: [onFinish] completes
 * onboarding, and [onRestoreBackup] / [restoreStatus] still let someone restore
 * an existing LifeOS backup before they start.
 */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    onRestoreBackup: () -> Unit = {},
    restoreStatus: String? = null
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to FigmaWelcomeTop,
                        0.5f to FigmaWelcomeMid,
                        1f to FigmaWelcomeBottom
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(start = 28.dp, end = 28.dp, top = 25.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BrandRow()

            Spacer(Modifier.height(20.dp))
            WelcomeUniverse(Modifier.fillMaxWidth())

            Spacer(Modifier.height(26.dp))
            WelcomeMessage()

            Spacer(Modifier.weight(1f))

            PrivacyRow()

            Spacer(Modifier.height(20.dp))
            LifeOSPrimaryButton(
                text = "Get started",
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                modifier = Modifier.fillMaxWidth(),
                onClick = onFinish
            )

            Text(
                text = restoreStatus ?: "No account needed. Nothing leaves this device.",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
                color = if (restoreStatus?.startsWith("Restore failed") == true) Color(0xFFFFB4AB) else FigmaTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )
            // The restore affordance stays, as a quiet tertiary link.
            Text(
                text = "Restore a LifeOS backup",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = FigmaMuted,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onRestoreBackup)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            )
        }
    }
}

/** The brand lock-up: the small orbit mark beside the wordmark. */
@Composable
private fun BrandRow() {
    Row(
        Modifier.fillMaxWidth().height(30.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(28.dp)) {
                val stroke = 1.6.dp.toPx()
                drawCircle(
                    brush = Brush.linearGradient(listOf(FigmaAccent, Color(0xFF528B86))),
                    radius = size.minDimension / 2f - stroke / 2f,
                    style = Stroke(width = stroke)
                )
                drawCircle(color = FigmaAccent, radius = size.minDimension * 0.13f)
            }
        }
        Text(
            "LifeOS",
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
            color = FigmaText
        )
    }
}

/**
 * The "personal universe": a glowing core inside two elliptical orbits, with
 * note / memory / habit satellites riding the rings. Drawn entirely with Canvas
 * so it scales to any width and needs no image asset (offline-first).
 */
@Composable
private fun WelcomeUniverse(modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(288.dp),
        contentAlignment = Alignment.Center
    ) {
        val w = maxWidth
        val h = maxHeight
        Canvas(Modifier.matchParentSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val core = size.minDimension * 0.213f // 152 / 2 / 356 of the frame

            // Atmosphere: a lit sphere, the only "light" in the scene.
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFFBFEBDF),
                        0.48f to Color(0xFF528B86),
                        1f to Color(0xFF142D3F)
                    ),
                    center = Offset(cx, cy),
                    radius = core
                ),
                radius = core,
                center = Offset(cx, cy)
            )
            // The soft bloom around the core.
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to Color(0x2B8ADAC5),
                        1f to Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = core * 1.9f
                ),
                radius = core * 1.9f,
                center = Offset(cx, cy)
            )
            // Outer orbit (294 × 280).
            drawOval(
                color = Color(0x1AA8E8D3),
                topLeft = Offset(cx - size.width * 0.413f, cy - h.toPx() * 0.486f),
                size = Size(size.width * 0.826f, h.toPx() * 0.972f),
                style = Stroke(width = 1.dp.toPx())
            )
            // Inner orbit (224 × 210).
            drawOval(
                color = Color(0x2EA8E8D3),
                topLeft = Offset(cx - size.width * 0.315f, cy - h.toPx() * 0.365f),
                size = Size(size.width * 0.629f, h.toPx() * 0.729f),
                style = Stroke(width = 1.dp.toPx())
            )
            // The life mark's own orbit (56).
            drawCircle(
                color = Color(0x24A8E8D3),
                radius = core,
                center = Offset(cx, cy),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Satellites riding the rings — real icons, so the orbit has meaning.
        UniverseSatellite(
            icon = Icons.Filled.EditNote,
            size = 44.dp,
            modifier = Modifier.offset(x = w * 0.30f, y = -h * 0.02f)
        )
        UniverseSatellite(
            icon = Icons.Filled.PhotoCamera,
            size = 44.dp,
            modifier = Modifier.offset(x = -w * 0.40f, y = h * 0.16f)
        )
        UniverseSatellite(
            icon = Icons.Filled.CheckCircle,
            size = 46.dp,
            modifier = Modifier.offset(x = w * 0.26f, y = h * 0.34f)
        )
    }
}

/** A slate satellite tile on the orbit — habit uses the 46dp 14-radius plate. */
@Composable
private fun UniverseSatellite(icon: ImageVector, size: Dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(FigmaElevated)
            .border(BorderStroke(1.dp, FigmaStroke), shape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = FigmaAccent, modifier = Modifier.size(size * 0.5f))
    }
}

/** The promise: a mint kicker, the 37px statement and one supporting line. */
@Composable
private fun WelcomeMessage() {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Text(
            "YOUR LIFE. YOUR DATA.",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.4.sp
            ),
            color = FigmaAccent,
            textAlign = TextAlign.Center
        )
        Text(
            "Your personal life, under your control.",
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 37.sp,
                lineHeight = 41.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = FigmaText,
            textAlign = TextAlign.Center
        )
        Text(
            "Tasks, habits, expenses and memories in one calm space that stays on your device.",
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 24.sp),
            color = FigmaMuted,
            textAlign = TextAlign.Center
        )
    }
}

/** The privacy assurance: a shield beside a two-line promise. */
@Composable
private fun PrivacyRow() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(FigmaAccent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Shield, contentDescription = null, tint = FigmaAccent, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                "Private by design. Offline by default.",
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                color = FigmaText
            )
            Text(
                "Your information stays on this device.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = FigmaMuted
            )
        }
        Icon(Icons.Filled.Spa, contentDescription = null, tint = FigmaTertiary, modifier = Modifier.size(18.dp))
    }
}
