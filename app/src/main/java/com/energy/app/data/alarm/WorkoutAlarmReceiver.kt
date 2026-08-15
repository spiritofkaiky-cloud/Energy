package com.energy.app.data.alarm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.energy.app.MainActivity
import com.energy.app.R
import com.energy.app.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** Fired by WorkoutAlarmScheduler — shows the "time to exercise" notification. */
class WorkoutAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Quiet hours (§5): suppress non-critical reminders inside the window.
        // Run off the main thread — DataStore reads suspend.
        val result = goAsync()
        Thread {
            try {
                val settings = SettingsRepository(context)
                val prefs = runBlocking { settings.preferences.first() }
                if (prefs.quietHoursEnabled && withinQuietHours(prefs.quietStart, prefs.quietEnd)) {
                    result.finish()
                    return@Thread
                }
                if (!prefs.notifications.workoutReminder) {
                    result.finish()
                    return@Thread
                }
            } catch (_: Exception) {
                // Fall through — a broken prefs read shouldn't block the nudge.
            }
            showNotification(context)
            result.finish()
        }.start()
    }

    /** Handles windows that cross midnight (e.g. 22:00 → 07:00). */
    private fun withinQuietHours(start: Int, end: Int): Boolean {
        val now = java.util.Calendar.getInstance()
        val current = now.get(java.util.Calendar.HOUR_OF_DAY) * 100 + now.get(java.util.Calendar.MINUTE)
        return if (start <= end) current in start..end else current >= start || current <= end
    }

    private fun showNotification(context: Context) {
        val channelId = "workout_reminders"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Workout reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminds you to exercise"
                }
            )
        }

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_splash_bolt)
            .setContentTitle("Time to exercise! \uD83D\uDCAA")
            .setContentText("Your body is your best project — let's move.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()

        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

        if (allowed) {
            NotificationManagerCompat.from(context).notify(1002, notification)
        }
    }
}
