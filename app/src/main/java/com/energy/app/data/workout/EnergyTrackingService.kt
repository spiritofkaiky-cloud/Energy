package com.energy.app.data.workout

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.energy.app.EnergyApplication
import com.energy.app.MainActivity
import com.energy.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Keeps the workout alive in the background (APP_SPEC §9): notification with
 * live elapsed time + pause/stop actions. Session state lives in
 * WorkoutSession (AppContainer), so Activity and Service share it.
 */
class EnergyTrackingService : Service() {

    companion object {
        const val ACTION_START = "com.energy.app.action.START"
        const val ACTION_PAUSE = "com.energy.app.action.PAUSE"
        const val ACTION_STOP = "com.energy.app.action.STOP"
        private const val CHANNEL_ID = "workout_tracking"
        private const val NOTIFICATION_ID = 2001
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickerJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val session = (application as EnergyApplication).container.workoutSession
        // Satisfy the startForegroundService contract for EVERY action:
        // any service started via startForegroundService must call
        // startForeground() within ~5 s or the system kills the whole app
        // (Android 12+). ACTION_STOP calls it too, then immediately removes
        // itself from the foreground.
        ensureChannel()
        startForeground(NOTIFICATION_ID, buildNotification(session))
        when (intent?.action) {
            ACTION_START -> {
                tickerJob?.cancel()
                tickerJob = scope.launch {
                    session.elapsedMillis.collectLatest {
                        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                        manager.notify(NOTIFICATION_ID, buildNotification(session))
                    }
                }
            }
            ACTION_PAUSE -> {
                if (session.state.value == WorkoutState.RECORDING) session.pause()
                else session.resume()
                val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, buildNotification(session))
            }
            ACTION_STOP -> {
                session.stop()
                tickerJob?.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Workout tracking", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Shows your live workout in progress" }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(session: WorkoutSession): Notification {
        val elapsed = session.elapsedMillis.value
        val mins = elapsed / 60_000
        val secs = (elapsed % 60_000) / 1_000
        val type = session.type.value.label
        val state = if (session.state.value == WorkoutState.PAUSED) "Paused" else "Live"
        val distance = WorkoutMath.formatDistance(session.distanceMeters.value)

        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val pausePi = PendingIntent.getService(
            this, 1, Intent(this, EnergyTrackingService::class.java).setAction(ACTION_PAUSE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopPi = PendingIntent.getService(
            this, 2, Intent(this, EnergyTrackingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_splash_bolt)
            .setContentTitle("$type — $state")
            .setContentText(
                String.format(Locale.US, "%02d:%02d · %s", mins, secs, distance)
            )
            .setOngoing(true)
            .setContentIntent(openApp)
            .addAction(
                0,
                if (session.state.value == WorkoutState.PAUSED) "Resume" else "Pause",
                pausePi
            )
            .addAction(0, "Stop", stopPi)
            .build()
    }
}
