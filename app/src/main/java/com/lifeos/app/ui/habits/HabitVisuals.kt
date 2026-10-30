package com.lifeos.app.ui.habits

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeos.app.data.db.entities.HabitChecklistItemEntity
import com.lifeos.app.data.db.entities.HabitEntity
import com.lifeos.app.data.db.entities.HabitFrequency
import com.lifeos.app.data.db.entities.HabitScheduleType
import com.lifeos.app.data.db.entities.HabitTrackingMode
import com.lifeos.app.data.db.entities.HabitType
import com.lifeos.app.domain.model.HeatmapCell
import com.lifeos.app.domain.model.HeatmapIntensity
import com.lifeos.app.ui.theme.LifeOSMotion
import com.lifeos.app.ui.theme.LifeOSRadius
import com.lifeos.app.ui.theme.LifeOSSpacing
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ===========================================================================
// The permanent Habits accent. LifeOS's global theme is the warm "Lumen"
// palette (clay / sage / amber); the Habits brief asks for a calm iris-purple
// identity, and docs/HABITS_PERMANENT_REDESIGN_SPEC.md explicitly says not to
// silently re-theme the whole app. So lavender lives *here*, scoped to the
// Habits experience, while typography, spacing, radii, surfaces and motion
// remain the shared LifeOS tokens.
// ===========================================================================

val HabitIris = Color(0xFF6750A4)
val HabitIrisDeep = Color(0xFF4F378B)
val HabitIrisSoft = Color(0xFFEADDFF)
val HabitIrisWash = Color(0xFFF3EDF7)
val HabitOnIris = Color(0xFF21005D)

/** One curated choice in the habit colour picker. `argb == 0` means "default". */
data class HabitColorOption(val name: String, val argb: Long)

private const val DEFAULT_HABIT_ARGB = 0L

val habitColorOptions = listOf(
    HabitColorOption("Iris", 0xFF6750A4),
    HabitColorOption("Violet", 0xFF7D4E9E),
    HabitColorOption("Rose", 0xFFB23A55),
    HabitColorOption("Coral", 0xFFC05B3A),
    HabitColorOption("Amber", 0xFFB8860B),
    HabitColorOption("Sage", 0xFF3F7D4E),
    HabitColorOption("Teal", 0xFF2E7D7A),
    HabitColorOption("Sky", 0xFF3A6EA5),
    HabitColorOption("Slate", 0xFF5B6472),
    HabitColorOption("Plum", 0xFF8A4B6B)
)

/** A stable, recognisable emoji grid — no network icon packs. */
val habitIconOptions = listOf(
    "🔥", "💧", "🏃", "🏋️", "🧘", "🚶", "🚴", "🥗",
    "😴", "💊", "🧠", "📚", "✍️", "🎯", "🧹", "🪴",
    "💰", "📵", "🚭", "🎸", "🎨", "🗣️", "☀️", "🌙"
)

val habitCategoryOptions = listOf(
    "Health", "Fitness", "Mind", "Learning", "Work", "Finance",
    "Home", "Social", "Creativity", "Other"
)

fun habitColorOf(habit: HabitEntity): Color =
    if (habit.colorArgb == DEFAULT_HABIT_ARGB) HabitIris else Color(habit.colorArgb)

fun habitColorOf(argb: Long): Color =
    if (argb == DEFAULT_HABIT_ARGB) HabitIris else Color(argb)

fun onHabitColor(color: Color): Color =
    if (color.luminance() > 0.55f) Color(0xFF241D15) else Color.White

fun habitEmoji(habit: HabitEntity): String = habit.icon.ifBlank { "🔥" }

fun habitScheduleLabel(habit: HabitEntity): String {
    val days = habit.daysOfWeekCsv?.takeIf { it.isNotBlank() } ?: habit.customDaysCsv
    return when (habit.scheduleType) {
        HabitScheduleType.LEGACY -> when (habit.frequency) {
            HabitFrequency.DAILY -> "Every day"
            HabitFrequency.WEEKLY -> "Once a week"
            HabitFrequency.CUSTOM -> weekdayCsvLabel(days)
        }
        HabitScheduleType.EVERY_DAY -> "Every day"
        HabitScheduleType.WEEKDAYS -> weekdayCsvLabel(days)
        HabitScheduleType.MONTH_DAYS -> monthDayCsvLabel(habit.daysOfMonthCsv)
        HabitScheduleType.TIMES_PER_WEEK -> "${habit.timesPerPeriod ?: 1}× per week"
        HabitScheduleType.TIMES_PER_MONTH -> "${habit.timesPerPeriod ?: 1}× per month"
    }
}

fun weekdayCsvLabel(csv: String?): String {
    val days = com.lifeos.app.core.util.HabitStatsCalculator.parseCustomDays(csv)
    if (days.isEmpty()) return "Specific weekdays"
    val names = mapOf(
        java.time.DayOfWeek.MONDAY to "Mon",
        java.time.DayOfWeek.TUESDAY to "Tue",
        java.time.DayOfWeek.WEDNESDAY to "Wed",
        java.time.DayOfWeek.THURSDAY to "Thu",
        java.time.DayOfWeek.FRIDAY to "Fri",
        java.time.DayOfWeek.SATURDAY to "Sat",
        java.time.DayOfWeek.SUNDAY to "Sun"
    )
    return days.sortedBy { it.value }.joinToString(", ") { names[it] ?: it.name.take(3) }
}

fun monthDayCsvLabel(csv: String?): String {
    val days = com.lifeos.app.core.util.HabitStatsCalculator.parseMonthDays(csv)
    if (days.isEmpty()) return "Specific days of month"
    return days.sorted().joinToString(", ") { day -> ordinal(day) }
}

fun ordinal(value: Int): String {
    val suffix = when {
        value % 100 in 11..13 -> "th"
        value % 10 == 1 -> "st"
        value % 10 == 2 -> "nd"
        value % 10 == 3 -> "rd"
        else -> "th"
    }
    return "$value$suffix"
}

fun habitTrackingLabel(habit: HabitEntity, checklistCount: Int = 0): String = when (habit.trackingMode) {
    HabitTrackingMode.TICK -> if (habit.habitType == HabitType.QUIT) "One tap · stayed on track" else "One tap"
    HabitTrackingMode.COUNT -> "Count · ${habit.goalCount.coerceAtLeast(1)} per day"
    HabitTrackingMode.CHECKLIST -> if (checklistCount > 0) "Checklist · $checklistCount items" else "Checklist"
}

fun habitTypeLabel(habit: HabitEntity): String =
    if (habit.habitType == HabitType.QUIT) "Quit habit" else "Build habit"

fun relativeDayLabel(epochDay: Long, todayEpochDay: Long): String {
    val date = LocalDate.ofEpochDay(epochDay)
    return when (epochDay - todayEpochDay) {
        0L -> "Today"
        -1L -> "Yesterday"
        1L -> "Tomorrow"
        else -> date.format(DateTimeFormatter.ofPattern("EEE, d MMM"))
    }
}

fun fullDayLabel(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))

fun formatReminderTime(epochMillis: Long?): String? {
    if (epochMillis == null || epochMillis <= 0L) return null
    val time = java.time.Instant.ofEpochMilli(epochMillis)
        .atZone(java.time.ZoneId.systemDefault()).toLocalTime()
    return time.format(DateTimeFormatter.ofPattern("h:mm a"))
}

/** Circular score ring used by the dashboard hero. */
@Composable
fun HabitScoreRing(
    percent: Int,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    stroke: Dp = 9.dp,
    track: Color = Color.White.copy(alpha = 0.22f),
    progressColor: Color = Color.White
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokePx = stroke.toPx()
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(strokePx / 2, strokePx / 2),
                size = Size(this.size.width - strokePx, this.size.height - strokePx),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * (percent.coerceIn(0, 100) / 100f),
                useCenter = false,
                topLeft = Offset(strokePx / 2, strokePx / 2),
                size = Size(this.size.width - strokePx, this.size.height - strokePx),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$percent%",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = progressColor
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = progressColor.copy(alpha = 0.82f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/** A 28-cell recent-activity strip used inside habit cards. */
@Composable
fun HabitMiniHeatmap(
    cells: List<HeatmapCell>,
    color: Color,
    modifier: Modifier = Modifier
) {
    val neutral = MaterialTheme.colorScheme.surfaceVariant
    Row(
        modifier.fillMaxWidth().height(10.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        cells.takeLast(28).forEach { cell ->
            Box(
                Modifier
                    .weight(1f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(habitHeatColor(cell.intensity, color, neutral))
            )
        }
    }
}

internal fun habitHeatColor(intensity: HeatmapIntensity, accent: Color, neutral: Color): Color =
    when (intensity) {
        HeatmapIntensity.NO_DATA -> accent.copy(alpha = 0.08f)
        HeatmapIntensity.MISSED -> neutral
        HeatmapIntensity.PARTIAL -> accent.copy(alpha = 0.42f)
        HeatmapIntensity.COMPLETED -> accent.copy(alpha = 0.78f)
        HeatmapIntensity.EXCEPTIONAL -> accent
    }

/** Shared list of habit checklist templates grouped by habit id. */
fun Map<String, List<HabitChecklistItemEntity>>.itemsFor(habitId: String): List<HabitChecklistItemEntity> =
    this[habitId].orEmpty()

/** The completion event shown after a meaningful transition to "done". */
data class HabitCelebration(
    val habitName: String,
    val icon: String,
    val colorArgb: Long,
    val streak: Int,
    val streakUnit: String,
    val habitType: HabitType
)

@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }
}

/**
 * A celebratory but calm overlay: animated check, streak milestone, medal and
 * a little confetti. Confetti and the check animation are skipped entirely when
 * the system animator scale is off.
 */
@Composable
fun HabitCelebrationOverlay(
    celebration: HabitCelebration,
    onDismiss: () -> Unit,
    onShare: (HabitCelebration) -> Unit
) {
    val reducedMotion = rememberReducedMotion()
    val checkScale = remember { Animatable(if (reducedMotion) 1f else 0.4f) }
    val confetti = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val accent = habitColorOf(celebration.colorArgb)
    val milestone = celebration.streak >= 7

    LaunchedEffect(celebration) {
        if (!reducedMotion) {
            checkScale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 420f))
            confetti.animateTo(1f, tween(1500, easing = FastOutSlowInEasing))
        }
        delay(if (reducedMotion) 1800 else 3200)
        onDismiss()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        if (!reducedMotion) {
            Confetti(
                progress = confetti.value,
                colors = listOf(accent, HabitIris, Color(0xFFE8B44A), Color(0xFF3F7D4E), Color(0xFFB23A55))
            )
        }
        Surface(
            Modifier
                .fillMaxWidth(0.86f)
                .clip(RoundedCornerShape(28.dp))
                .clickable(enabled = false) {},
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 18.dp
        ) {
            Column(
                Modifier.padding(horizontal = LifeOSSpacing.xl, vertical = LifeOSSpacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.md)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.14f))
                    )
                    Box(
                        Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = onHabitColor(accent),
                            modifier = Modifier
                                .size(42.dp)
                                .then(
                                    if (reducedMotion) Modifier
                                    else Modifier.graphicsLayerScale(checkScale.value)
                                )
                        )
                    }
                    if (milestone) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFE8B44A),
                            modifier = Modifier.align(Alignment.BottomEnd).size(34.dp)
                        ) {
                            Icon(
                                Icons.Filled.EmojiEvents,
                                contentDescription = "Streak milestone",
                                tint = Color(0xFF3B2A05),
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }
                }

                Text(
                    if (celebration.habitType == HabitType.QUIT) "Stayed strong" else "Well done",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "${celebration.icon}  ${celebration.habitName}",
                    style = MaterialTheme.typography.titleMedium,
                    color = accent,
                    textAlign = TextAlign.Center
                )
                if (celebration.streak > 0) {
                    Text(
                        "${celebration.streak} ${celebration.streakUnit}${if (celebration.streak == 1) "" else "s"} in a row",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                if (milestone) {
                    Text(
                        when {
                            celebration.streak >= 30 -> "A month of consistency. That is a real change."
                            celebration.streak >= 14 -> "Two weeks strong. Keep the chain alive."
                            else -> "One week. The hardest part is behind you."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(LifeOSSpacing.xs))
                Row(horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.sm)) {
                    TextButton(onClick = { onShare(celebration) }) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Share")
                    }
                    TextButton(onClick = onDismiss) { Text("Done") }
                }
            }
        }
    }
}

private fun Modifier.graphicsLayerScale(scale: Float): Modifier =
    this.graphicsLayer { scaleX = scale; scaleY = scale }

@Composable
private fun Confetti(progress: Float, colors: List<Color>) {
    val particles = remember {
        val random = Random(20261031)
        List(44) {
            ConfettiParticle(
                x = random.nextFloat(),
                drift = random.nextFloat() * 0.22f - 0.11f,
                delay = random.nextFloat() * 0.3f,
                speed = 0.55f + random.nextFloat() * 0.55f,
                size = 5f + random.nextFloat() * 7f,
                rotation = random.nextFloat() * 360f,
                color = colors[random.nextInt(colors.size)]
            )
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        particles.forEach { particle ->
            val local = ((progress - particle.delay) / (1f - particle.delay)).coerceIn(0f, 1f)
            if (local <= 0f) return@forEach
            val y = -40f + local * (size.height + 80f) * particle.speed
            val x = size.width * (particle.x + particle.drift * local)
            rotate(particle.rotation + local * 240f, pivot = Offset(x, y)) {
                drawRect(
                    color = particle.color.copy(alpha = (1f - local * 0.55f).coerceIn(0f, 1f)),
                    topLeft = Offset(x, y),
                    size = Size(particle.size, particle.size * 0.55f)
                )
            }
        }
    }
}

private data class ConfettiParticle(
    val x: Float,
    val drift: Float,
    val delay: Float,
    val speed: Float,
    val size: Float,
    val rotation: Float,
    val color: Color
)

/** Builds a share message without any network call. */
fun shareHabitCelebration(context: android.content.Context, celebration: HabitCelebration) {
    val streak = if (celebration.streak > 0) {
        "\n${celebration.streak} ${celebration.streakUnit}${if (celebration.streak == 1) "" else "s"} in a row."
    } else ""
    val text = "${celebration.icon} ${celebration.habitName} — done today.$streak\nTracked privately on LifeOS."
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Share habit")) }
}

/** A quiet "today" marker for the date strip. */
@Composable
fun TodayDot(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(4.dp)
            .clip(CircleShape)
            .background(color)
    )
}

/** A tiny decorative orbit used by the wizard introduction. */
@Composable
fun HabitWizardHero(modifier: Modifier = Modifier) {
    val accent = HabitIris
    Box(modifier.fillMaxWidth().height(210.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension * 0.30f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(HabitIrisSoft, Color.Transparent),
                    center = center,
                    radius = radius * 2.2f
                ),
                radius = radius * 2f,
                center = center
            )
            drawCircle(color = accent.copy(alpha = 0.18f), radius = radius * 1.45f, center = center, style = Stroke(2f))
            drawCircle(color = accent.copy(alpha = 0.10f), radius = radius * 1.95f, center = center, style = Stroke(2f))
            repeat(7) { index ->
                val angle = (index * 360.0 / 7.0 + 18.0) * PI / 180.0
                drawCircle(
                    color = listOf(accent, Color(0xFFB8860B), Color(0xFF3F7D4E))[index % 3].copy(alpha = 0.75f),
                    radius = 5f + (index % 3) * 2f,
                    center = Offset(
                        center.x + cos(angle).toFloat() * radius * 1.7f,
                        center.y + sin(angle).toFloat() * radius * 1.7f
                    )
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🔥", fontSize = 48.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Small steps, every day",
                style = MaterialTheme.typography.titleMedium,
                color = HabitIrisDeep
            )
        }
    }
}

/** Time pill for the detail screen's reminder card. */
@Composable
fun HabitTimeBadge(time: LocalTime, modifier: Modifier = Modifier) {
    Surface(
        modifier,
        shape = RoundedCornerShape(LifeOSRadius.pill),
        color = HabitIrisSoft
    ) {
        Text(
            time.format(DateTimeFormatter.ofPattern("h:mm a")),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = HabitOnIris
        )
    }
}
