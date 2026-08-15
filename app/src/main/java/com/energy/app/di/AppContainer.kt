package com.energy.app.di

import android.app.Application
import com.energy.app.data.alarm.WorkoutAlarmScheduler
import com.energy.app.data.auth.AuthRepository
import com.energy.app.data.auth.GuestAuthRepository
import com.energy.app.data.location.DayPathRepository
import com.energy.app.data.location.LocationTracker
import com.energy.app.data.settings.SettingsRepository

/**
 * Manual DI container for M1–M4.
 * Replaced by Hilt at M5 when cloud sync arrives (APP_SPEC §7).
 */
class AppContainer(application: Application) {

    val authRepository: AuthRepository by lazy { GuestAuthRepository() }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(application) }

    val dayPathRepository: DayPathRepository by lazy { DayPathRepository(application) }

    val locationTracker: LocationTracker by lazy {
        LocationTracker(application, dayPathRepository)
    }

    val workoutAlarmScheduler: WorkoutAlarmScheduler by lazy {
        WorkoutAlarmScheduler(application)
    }
}
