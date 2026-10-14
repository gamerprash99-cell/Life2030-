package com.lifeos.app.core.reminders

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeos.app.R
import com.lifeos.app.core.di.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The full-screen alarm UI, launched by [AlarmPlaybackService] (or by the OS via
 * the notification's full-screen intent). Surfaces above the lock screen, keeps
 * the screen on, and shows the coalesced [AlarmEvent] grouped by TASKS/HABITS.
 *
 * The activity deliberately plays **no sound** — the audible ring and vibration
 * live in [AlarmPlaybackService], so STOP/snooze here (or from the notification)
 * can never double-ring the user. STOP only ends presentation (it never cancels
 * the next occurrence); Snooze echoes the reminders again after the chosen
 * minutes, either for all of them or — when the event mixes tasks and habits —
 * for one selected group.
 */
class AlarmFullScreenActivity : ComponentActivity() {

    private var event: AlarmEvent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val decoded = AlarmEventCodec.from(intent) ?: run {
            finish()
            return
        }
        event = decoded

        // Ensure the audible alarm is running behind this screen. The service
        // ignores a re-entry for the same trigger, so this is idempotent even
        // when the service already started the presentation.
        runCatching {
            startService(buildAlarmServiceIntent(decoded))
        }

        setContent {
            AlarmTheme {
                val activeEvent = event ?: return@AlarmTheme
                AlarmScreen(
                    context = this@AlarmFullScreenActivity,
                    event = activeEvent,
                    onStop = { stopAndFinish() },
                    onSnooze = { target, minutes -> snoozeAndFinish(target, minutes) }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        AlarmEventCodec.from(intent)?.let {
            event = it
            runCatching { startService(buildAlarmServiceIntent(it)) }
        }
    }

    private fun buildAlarmServiceIntent(event: AlarmEvent): Intent =
        Intent(this, AlarmPlaybackService::class.java).apply {
            action = AlarmIntents.ACTION_ALARM_EVENT
            AlarmEventCodec.into(this, event)
        }

    private fun stopAndFinish() {
        runCatching { startService(AlarmIntents.stopServiceIntent(this)) }
        finish()
    }

    private fun snoozeAndFinish(target: SnoozeTarget, minutes: Int) {
        val ids = when (target) {
            SnoozeTarget.TASKS -> event?.itemsOf(AlarmEvent.TYPE_TASK).orEmpty().map { it.reminderId }
            SnoozeTarget.HABITS -> event?.itemsOf(AlarmEvent.TYPE_HABIT).orEmpty().map { it.reminderId }
            SnoozeTarget.BOTH -> event?.items.orEmpty().map { it.reminderId }
        }
        // Stop the ring immediately; persist the snooze in the background.
        runCatching { startService(AlarmIntents.stopServiceIntent(this)) }
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                ServiceLocator.get(applicationContext).reminderRepository.snoozeMany(ids, minutes)
            }
        }
        finish()
    }
}

private enum class SnoozeTarget(val labelRes: Int) {
    TASKS(R.string.alarm_snooze_tasks),
    HABITS(R.string.alarm_snooze_habits),
    BOTH(R.string.alarm_snooze_both)
}

private val snoozeMinutesList = intArrayOf(5, 10, 15, 20, 60)

@Composable
private fun AlarmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFCDBDFF),
            onPrimary = Color(0xFF1A1030),
            onSecondary = Color(0xFFDDD6FF),
            background = Color(0xFF121026),
            surface = Color(0xFF2A2547),
            onSurface = Color(0xFFFFFFFF),
            onSurfaceVariant = Color(0xFF9C93C4),
            error = Color(0xFFFF546E),
            onError = Color(0xFFFFFFFF)
        ),
        content = content
    )
}

@Composable
private fun AlarmScreen(
    context: Context,
    event: AlarmEvent,
    onStop: () -> Unit,
    onSnooze: (SnoozeTarget, Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = AlarmStrings.titleOf(context, event),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatTriggerTime(event.triggerAtEpochMillis),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .weight(1.6f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (event.hasTasks) {
                    SectionHeader(text = context.getString(R.string.alarm_section_tasks))
                    event.itemsOf(AlarmEvent.TYPE_TASK).forEach { item ->
                        ItemRow(title = item.title, accent = Color(0xFFCDBDFF))
                    }
                }
                if (event.hasHabits) {
                    SectionHeader(text = context.getString(R.string.alarm_section_habits))
                    event.itemsOf(AlarmEvent.TYPE_HABIT).forEach { item ->
                        ItemRow(title = item.title, accent = Color(0xFFFFB9A8))
                    }
                }
            }

            val showTargetSelector = event.hasTasks && event.hasHabits
            var selectedTarget by remember { mutableStateOf(if (showTargetSelector) SnoozeTarget.BOTH else null) }
            var selectedMinutes by remember { mutableStateOf(5) }

            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(8.dp))
                if (showTargetSelector) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SnoozeTarget.entries.forEach { target ->
                            SnoozeChip(
                                label = context.getString(target.labelRes),
                                selected = selectedTarget == target,
                                onClick = { selectedTarget = target }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    snoozeMinutesList.forEach { minutes ->
                        SnoozeChip(
                            label = context.getString(R.string.snooze_minutes, minutes),
                            selected = selectedMinutes == minutes,
                            onClick = {
                                selectedMinutes = minutes
                                val target = selectedTarget
                                    ?: if (event.hasTasks) SnoozeTarget.TASKS else SnoozeTarget.HABITS
                                onSnooze(target, minutes)
                            }
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onStop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(
                        text = context.getString(R.string.alarm_stop),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SnoozeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSecondary
        )
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(vertical = 6.dp)
    )
}

@Composable
private fun ItemRow(title: String, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color = accent, shape = CircleShape)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun formatTriggerTime(epochMillis: Long): String =
    SimpleDateFormat("EEE, MMM d · h:mm a", Locale.getDefault()).format(Date(epochMillis))