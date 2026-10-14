package com.lifeos.app.core.reminders

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.lifeos.app.R
import com.lifeos.app.core.di.ServiceLocator
import com.lifeos.app.core.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Foreground service that delivers the audible alarm for a coalesced
 * [AlarmEvent]: a looping alarm-tone [MediaPlayer] on STREAM_ALARM plus
 * vibration, held together with a partial wakelock and one high-priority,
 * full-screen notification.
 *
 * The *service owns the sound* — the full-screen activity deliberately plays
 * nothing, so STOP/snooze from either surface can never double-ring. Started by
 * [AlarmReceiver] from an exact-alarm broadcast: starting a foreground service
 * from a `setAlarmClock` alarm is explicitly permitted by the platform (see
 * the AlarmManager docs), which is why the audible alarm works even in deep
 * Doze / locked / app-force-stopped states.
 *
 * If [ServiceCompat.startForeground] is refused (exact access denied, over-
 * aggressive battery policy), the service degrades to a single silent-of-double
 * HIGH-importance notification carrying the event's own sound/flags and stops.
 */
class AlarmPlaybackService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null

    /** Trigger time of the event currently being presented (0 = none). */
    private var activeTriggerAt: Long = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            AlarmIntents.ACTION_STOP_ALARM -> stopAlarm()
            AlarmIntents.ACTION_SNOOZE -> {
                val event = AlarmEventCodec.from(intent)
                val minutes = intent.getIntExtra(AlarmIntents.EXTRA_SNOOZE_MINUTES, 5)
                if (event != null) snoozeAndStop(event, minutes) else stopAlarm()
            }
            else -> {
                val event = AlarmEventCodec.from(intent)
                if (event == null) {
                    stopSelf()
                } else if (event.triggerAtEpochMillis != activeTriggerAt) {
                    present(event)
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopRing()
        releaseWakeLock()
        super.onDestroy()
    }

    private fun present(event: AlarmEvent) {
        val foregroundStarted = runCatching {
            ServiceCompat.startForeground(
                this,
                notificationId(event),
                buildNotification(event),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                } else {
                    0
                }
            )
        }.isSuccess
        if (!foregroundStarted) {
            // FGS refused: degrade to a plain HIGH-importance alarm notification
            // carrying the event's own sound and full-screen intent, then exit.
            notifyFallback(event)
            stopSelf()
            return
        }
        activeTriggerAt = event.triggerAtEpochMillis
        acquireWakeLock()
        startRing(event)
        launchActivity(event)
    }

    private fun buildNotification(event: AlarmEvent): Notification {
        val title = AlarmStrings.titleOf(this, event)
        val body = AlarmStrings.bodyOf(this, event)
        val style = NotificationCompat.InboxStyle()
            .setBigContentTitle(title)
        event.items.forEach { style.addLine(it.title) }

        val requestCode = event.triggerAtEpochMillis.hashCode()
        val stopPendingIntent = PendingIntent.getService(
            this,
            requestCode,
            AlarmIntents.stopServiceIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snoozePendingIntent = PendingIntent.getService(
            this,
            requestCode xor 0x1F,
            AlarmIntents.snoozeServiceIntent(this, event, SNOOZE_DEFAULT_MINUTES),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(style)
            .setContentIntent(AlarmIntents.fullScreenPendingIntent(this, event))
            .setFullScreenIntent(AlarmIntents.fullScreenPendingIntent(this, event), true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            // The service itself plays the looped tone; the notification stays
            // silent so it can never double-ring the user.
            .setSilent(true)
            .addAction(0, getString(R.string.alarm_snooze), snoozePendingIntent)
            .addAction(0, getString(R.string.alarm_stop), stopPendingIntent)
            .build()
    }

    private fun notifyFallback(event: AlarmEvent) {
        NotificationHelper.showReminder(
            context = this,
            notificationId = notificationId(event),
            title = AlarmStrings.titleOf(this, event),
            body = AlarmStrings.bodyOf(this, event),
            fullScreenIntent = AlarmIntents.fullScreenPendingIntent(this, event),
            sound = event.anySoundEnabled,
            vibration = event.anyVibrationEnabled
        )
    }

    private fun startRing(event: AlarmEvent) {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(Vibrator::class.java)
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (event.anySoundEnabled && mediaPlayer == null) {
            try {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                audioManager?.requestAudioFocus(
                    null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(this@AlarmPlaybackService, uri)
                    isLooping = true
                    prepare()
                }
                player.start()
                mediaPlayer = player
            } catch (_: Exception) {
                mediaPlayer = null
            }
        }
        if (event.anyVibrationEnabled && vibrator != null) {
            try {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 500, 700), 0))
            } catch (_: Exception) {
                // Vibrator missing on some tablet/emulator builds.
            }
        }
    }

    private fun stopRing() {
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
        runCatching { vibrator?.cancel() }
        runCatching { audioManager?.abandonAudioFocus(null) }
    }

    private fun launchActivity(event: AlarmEvent) {
        runCatching {
            startActivity(AlarmIntents.fullScreenIntent(this, event))
        }
        // Reaching this point means the FSI/notification silently granted the
        // activity; the broadcast launch above is best-effort for foreground.
    }

    private fun snoozeAndStop(event: AlarmEvent, minutes: Int) {
        val reminderIds = event.items.map { it.reminderId }
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                ServiceLocator.get(applicationContext).reminderRepository.snoozeMany(reminderIds, minutes)
            }
            stopAlarm()
        }
    }

    private fun stopAlarm() {
        stopRing()
        releaseWakeLock()
        activeTriggerAt = 0L
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, "lifeos:alarmPlayback"
            ).apply {
                setReferenceCounted(false)
                acquire(ALARM_MAX_RING_MILLIS)
            }
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    private fun notificationId(event: AlarmEvent): Int = event.id.hashCode()

    companion object {
        const val SNOOZE_DEFAULT_MINUTES = 5
        private const val ALARM_MAX_RING_MILLIS = 10 * 60_000L
    }
}