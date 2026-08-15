package com.energy.app.data.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * Exercise reminder. Uses setAlarmClock so the alarm is exact AND
 * user-visible (status-bar alarm icon) without needing SCHEDULE_EXACT_ALARM.
 */
class WorkoutAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(hour: Int, minute: Int) {
        val triggerAt = nextTrigger(hour, minute)
        val pi = pendingIntent()
        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, pi), pi)
    }

    fun cancel() {
        alarmManager.cancel(pendingIntent())
    }

    private fun nextTrigger(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
    }

    private fun pendingIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            1001,
            Intent(context, WorkoutAlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
