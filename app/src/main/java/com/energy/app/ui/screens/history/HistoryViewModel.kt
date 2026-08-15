package com.energy.app.ui.screens.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.energy.app.EnergyApplication
import com.energy.app.data.workout.SavedWorkout
import com.energy.app.data.workout.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WorkoutRepository =
        (application as EnergyApplication).container.workoutRepository

    /** Full workouts (with route points) — null while the first load runs. */
    private val _workouts = MutableStateFlow<List<SavedWorkout>?>(null)
    val workouts: StateFlow<List<SavedWorkout>?> = _workouts.asStateFlow()

    init {
        viewModelScope.launch {
            repository.workouts.collect { metas ->
                _workouts.value = metas.map { meta ->
                    meta.copy(points = repository.points(meta.id))
                }
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }

    suspend fun workoutById(id: String): SavedWorkout? =
        _workouts.value?.firstOrNull { it.id == id }
            ?: repository.workouts.first().firstOrNull { it.id == id }
                ?.let { it.copy(points = repository.points(it.id)) }
}
