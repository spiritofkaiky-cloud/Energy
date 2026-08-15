package com.energy.app.data.workout

import com.energy.app.data.stats.StatsRepository
import kotlin.math.max

/**
 * Pure workout math — unit-testable, no Android dependencies.
 * Used by the live screen, summaries and the test suite.
 */
object WorkoutMath {

    /** Instantaneous speed from two fixes (km/h). Returns 0 on bad input. */
    fun speedKmh(prevLat: Double, prevLng: Double, lat: Double, lng: Double, dtMillis: Long): Double {
        if (dtMillis <= 0) return 0.0
        val km = StatsRepository.haversineKm(prevLat, prevLng, lat, lng)
        return km / (dtMillis / 3_600_000.0)
    }

    /** Pace in seconds per km from distance (m) + duration (ms); null when too short. */
    fun paceSecondsPerKm(distanceMeters: Double, durationMillis: Long): Double? {
        if (distanceMeters < 20 || durationMillis <= 0) return null
        return durationMillis / 1000.0 / (distanceMeters / 1000.0)
    }

    fun formatDuration(ms: Long): String {
        val h = ms / 3_600_000
        val m = (ms % 3_600_000) / 60_000
        val s = (ms % 60_000) / 1_000
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
    }

    fun formatDistance(meters: Double): String =
        if (meters >= 1000) String.format("%.2f km", meters / 1000)
        else String.format("%.0f m", meters)

    fun formatPace(paceSecondsPerKm: Double?): String {
        if (paceSecondsPerKm == null) return "—"
        val m = (paceSecondsPerKm / 60).toInt()
        val s = (paceSecondsPerKm - m * 60).toInt()
        return String.format("%d:%02d /km", m, s)
    }

    /** Per-km splits for a trail of points (list of (distanceM, timeMs) cumulative). */
    fun splits(points: List<Pair<Double, Long>>): List<Double> {
        val result = mutableListOf<Double>()
        var kmStart = 0.0
        var tStart = 0L
        for ((dist, t) in points) {
            if (dist - kmStart >= 1000.0) {
                val seg = dist - kmStart
                val dt = t - tStart
                result += (dt / 1000.0) / (seg / 1000.0) // s/km
                kmStart = dist
                tStart = t
            }
        }
        return result
    }

    /** Simple calorie estimate: MET per type × weight × hours (min 60 kcal). */
    fun calories(type: WorkoutType, durationMillis: Long): Int {
        val met = when (type) {
            WorkoutType.RUN -> 9.8
            WorkoutType.WALK -> 3.5
            WorkoutType.CYCLE -> 7.5
            WorkoutType.HIKE -> 6.0
        }
        val hours = durationMillis / 3_600_000.0
        return max(1, (met * 70.0 * hours).toInt()) // 70 kg reference
    }

    /** Max speed across a workout (km/h). */
    fun maxSpeedKmh(speeds: List<Double>): Double = speeds.maxOrNull() ?: 0.0

    /** Moving time: sum of segments faster than 1.2 km/h (GPS drift threshold). */
    fun movingTimeMillis(points: List<WorkoutPoint>): Long {
        if (points.size < 2) return 0L
        var moving = 0L
        for (i in 1 until points.size) {
            val dt = points[i].timeMillis - points[i - 1].timeMillis
            val kmh = speedKmh(
                points[i - 1].lat, points[i - 1].lng,
                points[i].lat, points[i].lng, dt
            )
            if (kmh > 1.2) moving += dt
        }
        return moving
    }
}
