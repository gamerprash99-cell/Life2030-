package com.lifeos.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.lifeos.app.MainActivity
import com.lifeos.app.R
import com.lifeos.app.core.di.ServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * "Today" home-screen widget: the three numbers the Home screen leads with —
 * tasks still open today, habit check-ins today, and what's been spent today.
 *
 * It reads the same local database the rest of the app reads. There is no
 * network, no cached server copy and no third-party widget library: this is a
 * classic `AppWidgetProvider` + `RemoteViews`, so it needs no new dependency and
 * keeps the app's offline-only promise.
 *
 * The refresh runs off the main thread inside `goAsync()`, which keeps the
 * broadcast alive long enough for a Room read instead of being killed the
 * moment `onUpdate` returns.
 */
class TodayWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AppWidgetManager.ACTION_APPWIDGET_UPDATE,
            AppWidgetManager.ACTION_APPWIDGET_ENABLED -> {
                val pendingResult = goAsync()
                val appContext = context.applicationContext
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        render(appContext)
                    } catch (_: Throwable) {
                        // A failed read leaves the last good content on the widget
                        // rather than blanking it.
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            else -> super.onReceive(context, intent)
        }
    }

    private suspend fun render(context: Context) {
        val locator = ServiceLocator.get(context)
        val today = DateTimeUtils.today().toEpochDay()

        val pendingTasks = runCatching {
            locator.taskRepository.observeForDay(today).first().count { !it.isCompleted }
        }.getOrNull()

        val habitCheckIns = runCatching {
            locator.habitRepository.observeAllForDay(today).first().count { it.progressCount > 0 }
        }.getOrNull()

        val spentToday = runCatching {
            locator.expenseRepository.observeInRange(today, today).first().sumOf { it.amount }
        }.getOrNull()

        val placeholder = context.getString(R.string.widget_placeholder)
        val views = RemoteViews(context.packageName, R.layout.widget_lifeos_today).apply {
            setTextViewText(
                R.id.widget_tasks,
                pendingTasks?.let { if (it == 1) "1 task left today" else "$it tasks left today" } ?: placeholder
            )
            setTextViewText(
                R.id.widget_habits,
                habitCheckIns?.let { if (it == 1) "1 habit checked in" else "$it habits checked in" } ?: placeholder
            )
            setTextViewText(
                R.id.widget_spend,
                spentToday?.let { if (it <= 0.0) "Nothing spent yet" else "${money(it)} spent today" } ?: placeholder
            )
            setOnClickPendingIntent(R.id.widget_root, openApp(context))
        }

        AppWidgetManager.getInstance(context).updateAppWidget(
            ComponentName(context, TodayWidgetProvider::class.java),
            views
        )
    }

    private fun openApp(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun money(amount: Double): String {
        val digits = if (amount % 1.0 == 0.0) 0 else 2
        return "₹" + String.format(Locale.getDefault(), "%,.${digits}f", amount)
    }
}
