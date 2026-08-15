package com.energy.app.ui.screens.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.energy.app.EnergyApplication
import com.energy.app.data.alarm.WorkoutAlarmScheduler
import com.energy.app.data.auth.AuthRepository
import com.energy.app.data.auth.AuthUser
import com.energy.app.data.cloud.CloudState
import com.energy.app.data.cloud.CloudStatus
import com.energy.app.data.settings.AlarmSetting
import com.energy.app.data.settings.SettingsRepository
import com.energy.app.data.settings.ThemeMode
import com.energy.app.data.settings.Units
import com.energy.app.data.settings.UserPreferences
import com.energy.app.data.stats.LifetimeStats
import com.energy.app.data.stats.LifetimeStatsCalculator
import com.energy.app.data.workout.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as EnergyApplication).container
    private val repository: AuthRepository = container.authRepository
    private val settings: SettingsRepository = container.settingsRepository
    private val alarmScheduler: WorkoutAlarmScheduler = container.workoutAlarmScheduler

    val user: StateFlow<AuthUser?> = repository.currentUser
    val streak: StateFlow<Int> = container.statsRepository.streak
    val cloudState: StateFlow<CloudState> = container.cloudRepository.state

    private val _lifetime = MutableStateFlow(LifetimeStats())
    val lifetime: StateFlow<LifetimeStats> = _lifetime.asStateFlow()

    private val _pendingSyncCount = MutableStateFlow(0)
    val pendingSyncCount: StateFlow<Int> = _pendingSyncCount.asStateFlow()

    val preferences: StateFlow<UserPreferences> = settings.preferences
        .let { flow -> MutableStateFlow(UserPreferences()).also { s ->
            viewModelScope.launch { flow.collect { s.value = it } }
        } }
    val themeMode: StateFlow<ThemeMode> = settings.themeMode
        .let { flow -> MutableStateFlow(ThemeMode.SYSTEM).also { s ->
            viewModelScope.launch { flow.collect { s.value = it } }
        } }
    val alarm: StateFlow<AlarmSetting> = settings.alarm
        .let { flow -> MutableStateFlow(AlarmSetting()).also { s ->
            viewModelScope.launch { flow.collect { s.value = it } }
        } }

    private val _signedOut = MutableStateFlow(false)
    val signedOut: StateFlow<Boolean> = _signedOut.asStateFlow()

    init {
        viewModelScope.launch {
            container.workoutRepository.workouts.collect { workouts ->
                _lifetime.value = LifetimeStatsCalculator.compute(workouts)
                _pendingSyncCount.value = workouts.count { it.syncState == SyncState.PENDING }
            }
        }
    }

    fun retryPendingSync() {
        viewModelScope.launch {
            val cloud = container.cloudRepository
            if (!cloud.isConfigured || !cloud.isSignedIn) return@launch
            container.workoutRepository.pendingSync().forEach { w ->
                cloud.syncWorkout(
                    com.energy.app.data.workout.WorkoutRepository.toCloudJson(w)
                ).onSuccess {
                    container.workoutRepository.markSyncState(w.id, SyncState.SYNCED)
                }.onFailure {
                    container.workoutRepository.markSyncState(w.id, SyncState.FAILED)
                }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun setUnits(units: Units) {
        viewModelScope.launch { settings.setUnits(units) }
    }

    fun setBatterySaver(enabled: Boolean) {
        viewModelScope.launch { settings.setBatterySaver(enabled) }
    }

    fun setAutoPause(enabled: Boolean) {
        viewModelScope.launch { settings.setAutoPause(enabled) }
    }

    fun setCalorieGoal(goal: Int) {
        viewModelScope.launch { settings.setCalorieGoal(goal) }
    }

    fun setWeightKg(weight: Int) {
        viewModelScope.launch { settings.setWeightKg(weight) }
    }

    fun setStepGoal(goal: Int) {
        viewModelScope.launch { settings.setStepGoal(goal) }
    }

    fun setAlarmEnabled(enabled: Boolean) {
        val current = alarm.value
        viewModelScope.launch {
            settings.setAlarm(enabled, current.hour, current.minute)
            if (enabled) alarmScheduler.schedule(current.hour, current.minute)
            else alarmScheduler.cancel()
        }
    }

    fun setAlarmTime(hour: Int, minute: Int) {
        val current = alarm.value
        viewModelScope.launch {
            settings.setAlarm(current.enabled, hour, minute)
            if (current.enabled) alarmScheduler.schedule(hour, minute)
        }
    }

    fun signOut() {
        repository.signOut()
        _signedOut.value = true
    }
}
