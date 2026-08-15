package com.energy.app.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.energy.app.EnergyApplication
import com.energy.app.data.location.DayPoint
import com.energy.app.data.location.LocationTracker
import kotlinx.coroutines.flow.StateFlow

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val tracker: LocationTracker =
        (application as EnergyApplication).container.locationTracker

    val points: StateFlow<List<DayPoint>> = tracker.points
    val tracking: StateFlow<Boolean> = tracker.tracking

    fun startTracking() = tracker.start()

    fun stopTracking() = tracker.stop()
}
