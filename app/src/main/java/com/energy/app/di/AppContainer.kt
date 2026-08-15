package com.energy.app.di

import android.app.Application
import com.energy.app.data.alarm.WorkoutAlarmScheduler
import com.energy.app.data.auth.AuthRepository
import com.energy.app.data.auth.GoogleSignInHelper
import com.energy.app.data.auth.PersistedAuthRepository
import com.energy.app.data.cloud.CloudRepository
import com.energy.app.data.location.DayPathRepository
import com.energy.app.data.location.LocationTracker
import com.energy.app.data.settings.SettingsRepository
import com.energy.app.data.workout.WorkoutRepository
import com.energy.app.data.workout.WorkoutSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Manual DI container. Kept intentionally small: one graph, lazy singletons,
 * an application-scoped coroutine scope for work that must survive screen
 * (or even process-level component) teardown — workout saves, session
 * restore, cloud sync.
 */
class AppContainer(application: Application) {

    /** Survives UI teardown — used for workout persistence & sync. */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val authRepository: AuthRepository by lazy { PersistedAuthRepository(application) }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(application) }

    val dayPathRepository: DayPathRepository by lazy { DayPathRepository(application) }

    val locationTracker: LocationTracker by lazy {
        LocationTracker(application, dayPathRepository, applicationScope, settingsRepository)
    }

    val workoutAlarmScheduler: WorkoutAlarmScheduler by lazy {
        WorkoutAlarmScheduler(application)
    }

    val workoutRepository: WorkoutRepository by lazy { WorkoutRepository(application) }

    val workoutSession: WorkoutSession by lazy {
        WorkoutSession(
            application,
            workoutRepository,
            settingsRepository,
            applicationScope
        )
    }

    val healthRepository: com.energy.app.data.health.HealthRepository by lazy {
        com.energy.app.data.health.HealthRepository(application)
    }

    val statsRepository: com.energy.app.data.stats.StatsRepository by lazy {
        com.energy.app.data.stats.StatsRepository(application)
    }

    val cloudRepository: CloudRepository by lazy { CloudRepository() }

    val googleSignInHelper: GoogleSignInHelper by lazy { GoogleSignInHelper(application) }

    /** Destructive: wipes workouts, day path, stats and preferences. */
    suspend fun eraseAllLocalData() {
        workoutRepository.deleteAllWorkouts()
        dayPathRepository.clearToday()
        statsRepository.clearAll()
        settingsRepository.resetAll()
    }

    init {
        // Restore the signed-in user at startup so relaunches skip sign-in.
        applicationScope.launch {
            (authRepository as? PersistedAuthRepository)?.restoreSession()
        }
    }
}
