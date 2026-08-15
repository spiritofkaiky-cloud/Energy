package com.energy.app.ui.screens.workout

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.energy.app.EnergyApplication
import com.energy.app.data.workout.EnergyTrackingService
import com.energy.app.data.workout.PersonalRecord
import com.energy.app.data.workout.PersonalRecords
import com.energy.app.data.workout.SaveStatus
import com.energy.app.data.workout.SavedWorkout
import com.energy.app.data.workout.WorkoutInsight
import com.energy.app.data.workout.WorkoutInsights
import com.energy.app.data.workout.WorkoutPoint
import com.energy.app.data.workout.WorkoutSession
import com.energy.app.data.settings.UserPreferences
import com.energy.app.data.workout.WorkoutState
import com.energy.app.data.workout.WorkoutType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as EnergyApplication).container
    private val session: WorkoutSession = container.workoutSession

    val state: StateFlow<WorkoutState> = session.state
    val type: StateFlow<WorkoutType> = session.type
    val points: StateFlow<List<WorkoutPoint>> = session.points
    val distanceMeters: StateFlow<Double> = session.distanceMeters
    val elapsedMillis: StateFlow<Long> = session.elapsedMillis
    val maxSpeedKmh: StateFlow<Double> = session.maxSpeedKmh
    val saveStatus: StateFlow<SaveStatus> = session.saveStatus
    val savedWorkout: StateFlow<SavedWorkout?> = session.lastSavedWorkout
    val restored: StateFlow<Boolean> = session.restored
    val lastFixMillis: StateFlow<Long> = session.lastFixMillis
    val currentSpeedKmh: Double get() = session.currentSpeedKmh

    /** Centralized preferences — the live screen reads presets from here. */
    val prefs: kotlinx.coroutines.flow.Flow<UserPreferences> =
        container.settingsRepository.preferences

    private val _newRecords = MutableStateFlow<List<PersonalRecord>>(emptyList())
    val newRecords: StateFlow<List<PersonalRecord>> = _newRecords.asStateFlow()

    private val _insights = MutableStateFlow<List<WorkoutInsight>>(emptyList())
    val insights: StateFlow<List<WorkoutInsight>> = _insights.asStateFlow()

    init {
        viewModelScope.launch {
            session.lastSavedWorkout.collect { saved ->
                if (saved != null) {
                    val all = container.workoutRepository.workouts.first()
                    _newRecords.value = PersonalRecords.newRecordsFor(all, saved)
                    _insights.value = WorkoutInsights.generate(all, saved)
                }
            }
        }
    }

    /** Start a fresh session + bring up the foreground service. */
    fun startWorkout(type: WorkoutType, context: Context) {
        if (session.state.value == WorkoutState.IDLE) session.start(type)
        startService(context, EnergyTrackingService.ACTION_START)
    }

    fun togglePause(context: Context) {
        // The toggle itself happens in the service's ACTION_PAUSE handler —
        // one single source of truth shared by the UI button and the
        // notification action (a double-toggle would cancel itself out).
        startService(context, EnergyTrackingService.ACTION_PAUSE)
    }

    /** Finish: persist workout (async — watch [saveStatus]), drop the service. */
    fun stopWorkout(context: Context) {
        session.stop()
        startService(context, EnergyTrackingService.ACTION_STOP)
    }

    fun retrySave() = session.retrySave()

    fun discardDraft() = session.discardDraft()

    private fun startService(context: Context, action: String) {
        val intent = Intent(context, EnergyTrackingService::class.java).setAction(action)
        try {
            context.startForegroundService(intent)
        } catch (e: Exception) {
            context.startService(intent)
        }
    }
}
