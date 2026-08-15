package com.energy.app.ui.screens.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.energy.app.EnergyApplication
import com.energy.app.data.workout.SavedWorkout
import com.energy.app.data.workout.WorkoutRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

class WorkoutDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WorkoutRepository =
        (application as EnergyApplication).container.workoutRepository

    private val _workout = MutableStateFlow<SavedWorkout?>(null)
    val workout: StateFlow<SavedWorkout?> = _workout.asStateFlow()

    fun load(id: String) {
        viewModelScope.launch {
            _workout.value = repository.workouts.first().firstOrNull { it.id == id }
        }
    }
}
