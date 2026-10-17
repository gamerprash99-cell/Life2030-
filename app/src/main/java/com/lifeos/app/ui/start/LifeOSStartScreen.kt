package com.lifeos.app.ui.start

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lifeos.app.ui.components.lifeOSCanvas
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * The animated start screen (the "splash" after Android's own native splash).
 *
 * Sequence: the ring traces itself in (a 270° arc), the coral bead springs into
 * its opening, then the wordmark and promise rise up while a slim progress
 * shimmer runs. Total ~1.6s, then [onFinished] hands off to the real app.
 *
 * Deliberately theme-aware: on the dark lagoon it uses the bright mint→coral
 * identity gradient; in the light "day" polarity it uses the deeper brand steps
 * so the ring stays visible on pale lagoon mist. When the system asks for
 * reduced motion (animator scale = 0) it skips straight to the end state — the
 * brand still shows, nothing animates.
 */
@Composable
fun LifeOSStartScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val reduceMotion = remember {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }

    val mint = MaterialTheme.colorScheme.secondary
    val coral = MaterialTheme.colorScheme.primary
    val ink = MaterialTheme.colorScheme.onBackground

    // The three animated quantities, driven by one effect so the timing reads
    // as a single sequence rather than three unrelated animations.
    val sweep = remember { Animatable(0f) }
    val bead = remember { Animatable(0f) }
    val rise = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (reduceMotion) {
            sweep.snapTo(1f); bead.snapTo(1f); rise.snapTo(1f)
            delay(220)
        } else {
            sweep.animateTo(1f, tween(durationMillis = 560, easing = FastOutSlowInEasing))
            bead.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMedium))
            rise.animateTo(1f, tween(durationMillis = 460, easing = FastOutSlowInEasing))
            delay(300)
        }
        onFinished()
    }

    Box(
        Modifier
            .fillMaxSize()
            .lifeOSCanvas(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Canvas(Modifier.size(132.dp)) {
                val stroke = size.minDimension * 0.086f
                val radius = size.minDimension / 2f - stroke * 0.75f
                val cx = size.width / 2f
                val cy = size.height / 2f
                val center = Offset(cx, cy)

                // A soft halo, so the mark sits in light rather than on a flat fill.
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(mint.copy(alpha = 0.22f), Color.Transparent),
                        center = center,
                        radius = radius * 2.1f
                    ),
                    radius = radius * 2.1f,
                    center = center
                )

                // The ring sweeps in from 3 o'clock, clockwise, leaving the gap
                // at the top-right for the bead.
                drawArc(
                    brush = Brush.linearGradient(listOf(mint, coral)),
                    startAngle = 0f,
                    sweepAngle = 270f * sweep.value,
                    useCenter = false,
                    topLeft = Offset(cx - radius, cy - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )

                // The captured-moment bead springs into the opening (-45°).
                val gapRadians = Math.toRadians(-45.0)
                val beadRadius = stroke * 0.82f * bead.value
                if (beadRadius > 0.1f) {
                    drawCircle(
                        color = coral,
                        radius = beadRadius,
                        center = Offset(
                            cx + (radius * cos(gapRadians)).toFloat(),
                            cy + (radius * sin(gapRadians)).toFloat()
                        )
                    )
                }
            }

            Box(Modifier.height(26.dp))
            Text(
                text = "LifeOS",
                style = MaterialTheme.typography.displaySmall,
                color = ink,
                modifier = Modifier.graphicsLayer {
                    alpha = rise.value
                    translationY = (1f - rise.value) * 40f
                }
            )
            Box(Modifier.height(8.dp))
            Text(
                text = "CAPTURE YOUR LIFE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.graphicsLayer {
                    alpha = rise.value
                    translationY = (1f - rise.value) * 30f
                }
            )
            Box(Modifier.height(34.dp))
            StartProgressShimmer(
                brush = Brush.horizontalGradient(listOf(mint, coral)),
                visible = rise.value
            )
        }
    }
}

/** A slim indeterminate bar: a gradient segment sweeping across a faint track. */
@Composable
private fun StartProgressShimmer(brush: Brush, visible: Float) {
    val transition = rememberInfiniteTransition(label = "startShimmer")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "startShimmerPhase"
    )
    Box(
        Modifier
            .graphicsLayer { alpha = visible }
            .width(132.dp)
            .height(3.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
    ) {
        Box(
            Modifier
                .graphicsLayer {
                    val seg = 0.42f
                    translationX = (size.width + size.width * seg) * phase - size.width * seg
                }
                .width(56.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(brush)
        )
    }
}
