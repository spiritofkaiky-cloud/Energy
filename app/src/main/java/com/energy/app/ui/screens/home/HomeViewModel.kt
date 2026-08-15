package com.energy.app.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.energy.app.EnergyApplication
import com.energy.app.data.health.DailyHealth
import com.energy.app.data.health.HealthRepository
import com.energy.app.data.location.DayPoint
import com.energy.app.data.location.LocationTracker
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as EnergyApplication).container
    private val tracker: LocationTracker = container.locationTracker
    private val health: HealthRepository = container.healthRepository

    val points: StateFlow<List<DayPoint>> = tracker.points
    val tracking: StateFlow<Boolean> = tracker.tracking
    val dailyHealth: StateFlow<DailyHealth?> = health.daily
    val healthAvailable: Boolean = health.available

    init {
        viewModelScope.launch { health.refreshToday() }
    }

    fun startTracking() = tracker.start()

    fun stopTracking() = tracker.stop()
}
