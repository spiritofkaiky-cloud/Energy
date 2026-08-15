package com.energy.app.data.workout

enum class WorkoutType(val label: String, val emoji: String) {
    RUN("Run", "🏃"),
    WALK("Walk", "🚶"),
    CYCLE("Cycle", "🚴"),
    HIKE("Hike", "🥾")
}

enum class WorkoutState { IDLE, RECORDING, PAUSED }

data class WorkoutPoint(
    val lat: Double,
    val lng: Double,
    val timeMillis: Long,
    val speedKmh: Double
)

data class SavedWorkout(
    val id: String,
    val type: WorkoutType,
    val startMillis: Long,
    val endMillis: Long,
    val distanceMeters: Double,
    val durationMillis: Long,
    val points: List<WorkoutPoint>
) {
    val durationMinutes: Long get() = durationMillis / 60_000

    val avgSpeedKmh: Double
        get() = if (durationMillis > 0) {
            distanceMeters / 1000.0 / (durationMillis / 3_600_000.0)
        } else 0.0

    /** Minutes per kilometer (running pace). */
    val avgPaceMinPerKm: Double
        get() = if (distanceMeters > 10) {
            (durationMillis / 60_000.0) / (distanceMeters / 1000.0)
        } else 0.0

    /** Rough calories estimate: ~8 kcal/min for run/hike, ~6 for cycle, ~4 for walk. */
    val calories: Int
        get() = ((durationMillis / 60_000.0) *
            when (type) { WorkoutType.RUN -> 8.0; WorkoutType.HIKE -> 7.0; WorkoutType.CYCLE -> 6.0; WorkoutType.WALK -> 4.0 })
            .toInt()
}
