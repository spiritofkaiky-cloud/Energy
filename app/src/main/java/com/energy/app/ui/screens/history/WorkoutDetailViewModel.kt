package com.energy.app.ui.screens.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.energy.app.EnergyApplication
import com.energy.app.data.workout.PersonalRecord
import com.energy.app.data.workout.PersonalRecords
import com.energy.app.data.workout.SavedWorkout
import com.energy.app.data.workout.WorkoutInsight
import com.energy.app.data.workout.WorkoutInsights
import com.energy.app.data.workout.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WorkoutDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WorkoutRepository =
        (application as EnergyApplication).container.workoutRepository

    private val _workout = MutableStateFlow<SavedWorkout?>(null)
    val workout: StateFlow<SavedWorkout?> = _workout.asStateFlow()

    private val _records = MutableStateFlow<List<PersonalRecord>>(emptyList())
    val records: StateFlow<List<PersonalRecord>> = _records.asStateFlow()

    private val _insights = MutableStateFlow<List<WorkoutInsight>>(emptyList())
    val insights: StateFlow<List<WorkoutInsight>> = _insights.asStateFlow()

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    fun load(id: String) {
        viewModelScope.launch {
            val metas = repository.workouts.first()
            val meta = metas.firstOrNull { it.id == id } ?: return@launch
            val w = meta.copy(points = repository.points(id))
            _workout.value = w
            // Load all routes (cached in the repo) so records are computed
            // against the true full history.
            val all = metas.map { m ->
                if (m.id == id) w else m.copy(points = repository.points(m.id))
            }
            _records.value = PersonalRecords.allRecords(all).filter { it.workoutId == id }
            _insights.value = WorkoutInsights.generate(all, w)
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            repository.delete(id)
            _deleted.value = true
        }
    }
}
