package com.energy.app.data.stats

import com.energy.app.data.workout.SavedWorkout
import com.energy.app.data.workout.WorkoutType

/** Lifetime aggregates across all saved workouts (pure — reused by Home + Profile). */
data class LifetimeStats(
    val workoutCount: Int = 0,
    val totalKm: Double = 0.0,
    val totalMinutes: Long = 0L,
    val bestPaceSecondsPerKm: Double? = null,
    val longestKm: Double = 0.0,
    val totalCalories: Int = 0
)

object LifetimeStatsCalculator {
    fun compute(workouts: List<SavedWorkout>): LifetimeStats {
        if (workouts.isEmpty()) return LifetimeStats()
        return LifetimeStats(
            workoutCount = workouts.size,
            totalKm = workouts.sumOf { it.distanceMeters } / 1000.0,
            totalMinutes = workouts.sumOf { it.durationMillis } / 60_000L,
            bestPaceSecondsPerKm = workouts
                .filter {
                    it.type == WorkoutType.RUN && it.distanceMeters > 500 &&
                        it.durationMillis > 60_000 && it.avgPaceMinPerKm in 0.5..120.0
                }
                .mapNotNull { it.avgPaceMinPerKm * 60 }
                .minOrNull(),
            longestKm = workouts.maxOfOrNull { it.distanceMeters }?.div(1000.0) ?: 0.0,
            totalCalories = workouts.sumOf { it.calories }
        )
    }
}
