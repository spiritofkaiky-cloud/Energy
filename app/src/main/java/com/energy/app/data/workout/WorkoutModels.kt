package com.energy.app.data.workout

enum class WorkoutType(val label: String, val emoji: String) {
    RUN("Run", "🏃"),
    WALK("Walk", "🚶"),
    CYCLE("Cycle", "🚴"),
    HIKE("Hike", "🥾")
}

enum class WorkoutState { IDLE, RECORDING, PAUSED }

/** Persisted cloud-sync state for a saved workout (offline-first, APP_SPEC §18). */
enum class SyncState { PENDING, SYNCED, FAILED }

data class WorkoutPoint(
    val lat: Double,
    val lng: Double,
    val timeMillis: Long,
    val speedKmh: Double,
    /** GPS altitude in meters, when the fix reported one. */
    val alt: Double? = null
)

data class SavedWorkout(
    val id: String,
    val type: WorkoutType,
    val startMillis: Long,
    val endMillis: Long,
    val distanceMeters: Double,
    val durationMillis: Long,
    val points: List<WorkoutPoint>,
    /** Estimated at save time from duration + type + user weight (WorkoutMath). */
    val calories: Int = 0,
    /** Sum of positive altitude deltas across the route (GPS-derived). */
    val elevationGainMeters: Double = 0.0,
    val avgHeartRateBpm: Int? = null,
    val maxHeartRateBpm: Int? = null,
    val syncState: SyncState = SyncState.PENDING
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

    val maxSpeedKmh: Double
        get() = points.maxOfOrNull { it.speedKmh } ?: 0.0
}
