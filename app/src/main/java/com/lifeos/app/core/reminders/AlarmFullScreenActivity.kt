package com.lifeos.app.core.reminders

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.lifeos.app.R
import com.lifeos.app.core.di.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * The full-screen alarm UI, launched by [AlarmReceiver] (or by the OS as a
 * notification full-screen intent). Surfaces above the lock screen, keeps the
 * screen on, and rings a looping alarm-tone — with vibration — until the user
 * stops it or picks a snooze duration.
 *
 * STOP only ends presentation (it never cancels the next occurrence); Snooze
 * echoes the same reminder again after the chosen minutes without moving the
 * next real occurrence.
 */
class AlarmFullScreenActivity : Activity() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null

    private val reminderId: String by lazy { intent?.getStringExtra(AlarmReceiver.EXTRA_REMINDER_ID).orEmpty() }
    private val title: String by lazy { intent?.getStringExtra(AlarmReceiver.EXTRA_TITLE) ?: "LifeOS reminder" }
    private val soundEnabled: Boolean by lazy { intent?.getBooleanExtra(AlarmReceiver.EXTRA_SOUND_ENABLED, true) ?: true }
    private val vibrationEnabled: Boolean by lazy { intent?.getBooleanExtra(AlarmReceiver.EXTRA_VIBRATION_ENABLED, true) ?: true }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (reminderId.isBlank()) {
            finish()
            return
        }

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(Vibrator::class.java)
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        setContentView(buildContentView())

        startRing()
    }

    override fun onStop() {
        super.onStop()
        stopRing()
    }

    override fun onDestroy() {
        stopRing()
        super.onDestroy()
    }

    private fun startRing() {
        if (soundEnabled && mediaPlayer == null) {
            try {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                audioManager?.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(this@AlarmFullScreenActivity, uri)
                    isLooping = true
                    prepare()
                }
                player.start()
                mediaPlayer = player
            } catch (_: Exception) {
                // No alarm tone available (or media failure) — the accompanying
                // notification's own alert sound still fires so the user is alerted.
                mediaPlayer = null
            }
        }
        if (vibrationEnabled && vibrator != null) {
            try {
                val pattern = longArrayOf(0, 700, 500, 700)
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
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

    private fun stopAndFinish() {
        stopRing()
        dismissNotification()
        finish()
    }

    private fun snoozeAndFinish(minutes: Int) {
        stopRing()
        dismissNotification()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                ServiceLocator.get(applicationContext).reminderRepository.snooze(reminderId, minutes)
            }
        }
        finish()
    }

    private fun dismissNotification() {
        runCatching {
            getSystemService(NotificationManager::class.java)
                .cancel(reminderId.hashCode())
        }
    }

    private fun buildContentView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(0xFF121026.toInt())
            setPadding(dp(28), dp(40), dp(28), dp(40))
        }

        root.addView(TextView(this).apply {
            text = "\uD83D\uDD14"
            textSize = 56f
            gravity = Gravity.CENTER
        })

        root.addView(TextView(this).apply {
            text = when (intent?.getStringExtra(AlarmReceiver.EXTRA_ENTITY_TYPE)) {
                "task" -> getString(R.string.alarm_task_reminder)
                "habit" -> getString(R.string.alarm_habit_reminder)
                else -> getString(R.string.alarm_generic_reminder)
            }
            textSize = 18f
            setTextColor(0xFFB9A8FF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, 0)
        })

        root.addView(TextView(this).apply {
            text = title
            textSize = 26f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(28))
        })

        val snoozeMinutes = intArrayOf(5, 10, 15, 30)
        val rowParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(56)
        ).apply { bottomMargin = dp(12) }

        root.addView(TextView(this).apply {
            text = getString(R.string.alarm_stop)
            textSize = 20f
            gravity = Gravity.CENTER
            setBackgroundColor(0xFFFF546E.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener { stopAndFinish() }
            layoutParams = rowParams
        })

        // A single row of four equal snooze chips.
        val snoozeBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        snoozeMinutes.forEach { minutes ->
            val chip = TextView(this).apply {
                text = getString(R.string.snooze_minutes, minutes)
                textSize = 15f
                gravity = Gravity.CENTER
                setBackgroundColor(0xFF2A2547.toInt())
                setTextColor(0xFFDDD6FF.toInt())
                setOnClickListener { snoozeAndFinish(minutes) }
                layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply { rightMargin = dp(6) }
            }
            snoozeBar.addView(chip)
        }
        root.addView(snoozeBar)

        return root
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}