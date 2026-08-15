package com.energy.app.ui.screens.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.energy.app.EnergyApplication
import com.energy.app.data.auth.AuthUser
import com.energy.app.data.cloud.CloudState
import com.energy.app.data.settings.Accent
import com.energy.app.data.settings.AlarmSetting
import com.energy.app.data.settings.FitnessLevel
import com.energy.app.data.settings.GpsMode
import com.energy.app.data.settings.Haptics
import com.energy.app.data.settings.MetricPreset
import com.energy.app.data.settings.NotificationPrefs
import com.energy.app.data.settings.RoutePrivacy
import com.energy.app.data.settings.ThemeMode
import com.energy.app.data.settings.Units
import com.energy.app.data.settings.UserPreferences
import com.energy.app.data.stats.LifetimeStats
import com.energy.app.data.stats.LifetimeStatsCalculator
import com.energy.app.data.workout.WorkoutType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class WorkoutAlarm(val enabled: Boolean, val hour: Int, val minute: Int)

/**
 * Control-center ViewModel: exposes the centralized preferences model and
 * delegates every change to SettingsRepository (the single source of truth).
 */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as EnergyApplication).container
    private val settings = container.settingsRepository

    val preferences: StateFlow<UserPreferences> = settings.preferences.let { flow ->
        val state = MutableStateFlow(UserPreferences())
        viewModelScope.launch { flow.collect { state.value = it } }
        state
    }
    val themeMode: StateFlow<ThemeMode> = settings.themeMode.let { flow ->
        val state = MutableStateFlow(ThemeMode.SYSTEM)
        viewModelScope.launch { flow.collect { state.value = it } }
        state
    }
    val user: StateFlow<AuthUser?> = container.authRepository.currentUser.let { flow ->
        val state = MutableStateFlow<AuthUser?>(null)
        viewModelScope.launch { flow.collect { state.value = it } }
        state
    }
    val cloudState: StateFlow<CloudState> = container.cloudRepository.state

    private val _lifetime = MutableStateFlow(LifetimeStats())
    val lifetime: StateFlow<LifetimeStats> = _lifetime.asStateFlow()

    private val _streak = MutableStateFlow(0)
    val streak: StateFlow<Int> = container.statsRepository.streak

    private val _alarm = MutableStateFlow(AlarmSetting(false, 8, 0))
    val alarm: StateFlow<AlarmSetting> = settings.alarm.let { flow ->
        val state = MutableStateFlow(AlarmSetting(false, 8, 0))
        viewModelScope.launch { flow.collect { state.value = it } }
        state
    }

    private val _pendingSyncCount = MutableStateFlow(0)
    val pendingSyncCount: StateFlow<Int> = _pendingSyncCount.asStateFlow()

    private val _signedOut = MutableStateFlow(false)
    val signedOut: StateFlow<Boolean> = _signedOut.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val workouts = container.workoutRepository.workouts.first()
            _lifetime.value = LifetimeStatsCalculator.compute(workouts)
            _pendingSyncCount.value = workouts.count { it.syncState == com.energy.app.data.workout.SyncState.PENDING }
        }
    }

    // ── Delegate setters (one per control) ────────────────────────────────
    fun setThemeMode(m: ThemeMode) = launch { settings.setThemeMode(m) }
    fun setAccent(a: Accent) = launch { settings.setAccent(a) }
    fun setHaptics(h: Haptics) = launch { settings.setHaptics(h) }
    fun setVisualEffects(v: Boolean) = launch { settings.setVisualEffects(v) }
    fun setUnits(u: Units) = launch { settings.setUnits(u) }
    fun setStepGoal(v: Int) = launch { settings.setStepGoal(v) }
    fun setCalorieGoal(v: Int) = launch { settings.setCalorieGoal(v) }
    fun setWeightKg(v: Int) = launch { settings.setWeightKg(v) }
    fun setHeightCm(v: Int) = launch { settings.setHeightCm(v) }
    fun setAutoPause(v: Boolean) = launch { settings.setAutoPause(v) }
    fun setGpsMode(m: GpsMode) = launch { settings.setGpsMode(m) }
    fun setCountdownSeconds(v: Int) = launch { settings.setCountdownSeconds(v) }
    fun setKeepScreenAwake(v: Boolean) = launch { settings.setKeepScreenAwake(v) }
    fun setConfirmFinish(v: Boolean) = launch { settings.setConfirmFinish(v) }
    fun setMetricPreset(p: MetricPreset) = launch { settings.setMetricPreset(p) }
    fun setAudioCues(v: Boolean) = launch { settings.setAudioCues(v) }
    fun setAnnounceInterval(v: com.energy.app.data.settings.AnnounceInterval) =
        launch { settings.setAnnounceInterval(v) }
    fun setNotifications(n: NotificationPrefs) = launch { settings.setNotifications(n) }
    fun setQuietHours(enabled: Boolean, start: Int, end: Int) =
        launch { settings.setQuietHours(enabled, start, end) }
    fun setRoutePrivacy(p: RoutePrivacy) = launch { settings.setRoutePrivacy(p) }
    fun setRouteColorAccent(v: Boolean) = launch { settings.setRouteColorAccent(v) }
    fun setSpeedColorRoute(v: Boolean) = launch { settings.setSpeedColorRoute(v) }
    fun setDefaultWorkoutType(t: WorkoutType) = launch { settings.setDefaultWorkoutType(t) }
    fun setPreferredActivity(t: WorkoutType) = launch { settings.setPreferredActivity(t) }
    fun setFitnessLevel(l: FitnessLevel) = launch { settings.setFitnessLevel(l) }
    fun setHomeScore(v: Boolean) = launch { settings.setHomeScore(v) }
    fun setHomeInsight(v: Boolean) = launch { settings.setHomeInsight(v) }
    fun setHomeRings(v: Boolean) = launch { settings.setHomeRings(v) }
    fun setHomeStats(v: Boolean) = launch { settings.setHomeStats(v) }
    fun setHomeMap(v: Boolean) = launch { settings.setHomeMap(v) }
    fun setHomeStreak(v: Boolean) = launch { settings.setHomeStreak(v) }
    fun setAlarmEnabled(enabled: Boolean) = launch {
        settings.setAlarmEnabled(enabled)
        if (enabled) {
            val a = settings.alarm.first()
            container.workoutAlarmScheduler.schedule(a.hour, a.minute)
        } else {
            container.workoutAlarmScheduler.cancel()
        }
    }

    fun setAlarmTime(hour: Int, minute: Int) = launch {
        settings.setAlarm(hour, minute)
        container.workoutAlarmScheduler.schedule(hour, minute)
    }

    fun retryPendingSync() = launch {
        val pending = container.workoutRepository.pendingSync()
        pending.forEach { w ->
            container.cloudRepository.syncWorkout(
                com.energy.app.data.workout.WorkoutRepository.toCloudJson(w)
            ).onSuccess {
                container.workoutRepository.markSyncState(w.id, com.energy.app.data.workout.SyncState.SYNCED)
            }
        }
        refresh()
    }

    fun signOut() {
        container.authRepository.signOut()
        _signedOut.value = true
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { runCatching { block() } }
    }
}
