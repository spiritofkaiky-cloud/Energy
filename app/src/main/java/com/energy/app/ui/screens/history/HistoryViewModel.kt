package com.energy.app.ui.screens.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.energy.app.EnergyApplication
import com.energy.app.data.workout.SavedWorkout
import com.energy.app.data.workout.WorkoutRepository
import kotlinx.coroutines.flow.Flow

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WorkoutRepository =
        (application as EnergyApplication).container.workoutRepository

    val workouts: Flow<List<SavedWorkout>> = repository.workouts
}
