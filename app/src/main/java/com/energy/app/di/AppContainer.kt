package com.energy.app.di

import android.app.Application
import com.energy.app.data.alarm.WorkoutAlarmScheduler
import com.energy.app.data.auth.AuthRepository
import com.energy.app.data.auth.GoogleSignInHelper
import com.energy.app.data.auth.GuestAuthRepository
import com.energy.app.data.cloud.CloudRepository
import com.energy.app.data.location.DayPathRepository
import com.energy.app.data.location.LocationTracker
import com.energy.app.data.settings.SettingsRepository
import com.energy.app.data.workout.WorkoutRepository
import com.energy.app.data.workout.WorkoutSession

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

    val workoutRepository: WorkoutRepository by lazy { WorkoutRepository(application) }

    val workoutSession: WorkoutSession by lazy {
        WorkoutSession(application, workoutRepository)
    }

    val healthRepository: com.energy.app.data.health.HealthRepository by lazy {
        com.energy.app.data.health.HealthRepository(application)
    }

    val cloudRepository: CloudRepository by lazy { CloudRepository() }

    val googleSignInHelper: GoogleSignInHelper by lazy { GoogleSignInHelper(application) }
}
