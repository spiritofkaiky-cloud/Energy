package com.energy.app.ui.screens.workout

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import com.energy.app.EnergyApplication
import com.energy.app.data.workout.EnergyTrackingService
import com.energy.app.data.workout.SavedWorkout
import com.energy.app.data.workout.WorkoutPoint
import com.energy.app.data.workout.WorkoutSession
import com.energy.app.data.workout.WorkoutState
import com.energy.app.data.workout.WorkoutType
import kotlinx.coroutines.flow.StateFlow

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as EnergyApplication).container
    private val session: WorkoutSession = container.workoutSession

    val state: StateFlow<WorkoutState> = session.state
    val type: StateFlow<WorkoutType> = session.type
    val points: StateFlow<List<WorkoutPoint>> = session.points
    val distanceMeters: StateFlow<Double> = session.distanceMeters
    val elapsedMillis: StateFlow<Long> = session.elapsedMillis
    val currentSpeedKmh: Double get() = session.currentSpeedKmh
    val maxSpeedKmh: StateFlow<Double> = session.maxSpeedKmh

    /** Start (or resume existing) session + bring up the foreground service. */
    fun startWorkout(type: WorkoutType, context: Context) {
        if (session.state.value == WorkoutState.IDLE) session.start(type)
        context.startForegroundService(
            Intent(context, EnergyTrackingService::class.java)
                .setAction(EnergyTrackingService.ACTION_START)
        )
    }

    fun togglePause(context: Context) {
        if (session.state.value == WorkoutState.RECORDING) session.pause()
        else if (session.state.value == WorkoutState.PAUSED) session.resume()
        context.startService(
            Intent(context, EnergyTrackingService::class.java)
                .setAction(EnergyTrackingService.ACTION_PAUSE)
        )
    }

    /** Finish: persist workout, drop the service notification. */
    fun stopWorkout(context: Context): SavedWorkout? {
        val saved = session.stop()
        context.startService(
            Intent(context, EnergyTrackingService::class.java)
                .setAction(EnergyTrackingService.ACTION_STOP)
        )
        return saved
    }
}
