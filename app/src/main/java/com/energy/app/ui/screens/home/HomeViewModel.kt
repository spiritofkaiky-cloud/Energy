package com.energy.app.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.energy.app.EnergyApplication
import com.energy.app.data.health.DailyHealth
import com.energy.app.data.health.HealthRepository
import com.energy.app.data.location.DayPoint
import com.energy.app.data.location.LocationTracker
import com.energy.app.data.stats.EnergyScore
import com.energy.app.data.stats.StatsRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as EnergyApplication).container
    private val tracker: LocationTracker = container.locationTracker
    private val health: HealthRepository = container.healthRepository
    private val stats: StatsRepository = container.statsRepository

    val points: StateFlow<List<DayPoint>> = tracker.points
    val tracking: StateFlow<Boolean> = tracker.tracking
    val dailyHealth: StateFlow<DailyHealth?> = health.daily
    val healthAvailable: Boolean = health.available
    val score: StateFlow<EnergyScore> = stats.score
    val streak: StateFlow<Int> = stats.streak

    init {
        viewModelScope.launch {
            health.refreshToday()
            refreshStats()
        }
    }

    /** Recompute Oura-style Energy Score + streak from all signals. */
    suspend fun refreshStats() {
        val workouts = container.workoutRepository.workouts.first()
        stats.refresh(tracker.points.value, workouts, health.daily.value)
    }

    fun startTracking() = tracker.start()

    fun stopTracking() = tracker.stop()
}
