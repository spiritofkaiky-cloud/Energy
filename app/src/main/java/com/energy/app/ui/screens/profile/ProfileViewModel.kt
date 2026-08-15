package com.energy.app.ui.screens.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.energy.app.EnergyApplication
import com.energy.app.data.alarm.WorkoutAlarmScheduler
import com.energy.app.data.auth.AuthRepository
import com.energy.app.data.auth.AuthUser
import com.energy.app.data.settings.AlarmSetting
import com.energy.app.data.settings.SettingsRepository
import com.energy.app.data.settings.ThemeMode
import com.energy.app.data.settings.Units
import com.energy.app.data.settings.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as EnergyApplication).container
    private val repository: AuthRepository = container.authRepository
    private val settings: SettingsRepository = container.settingsRepository
    private val alarmScheduler: WorkoutAlarmScheduler = container.workoutAlarmScheduler

    val user: AuthUser? = repository.currentUser()
    val streak: StateFlow<Int> = container.statsRepository.streak

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
