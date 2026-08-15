package com.energy.app.data.workout

import com.energy.app.data.stats.StatsRepository
import kotlin.math.max
import java.util.Locale

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

    /**
     * Calorie estimate: MET per type × weight × hours.
     * METs are the compendium values for moderate outdoor activity; clearly
     * an estimate (APP_SPEC — no fake precision).
     */
    fun calories(type: WorkoutType, durationMillis: Long, weightKg: Double = 70.0): Int {
        val met = when (type) {
            WorkoutType.RUN -> 9.8
            WorkoutType.WALK -> 3.5
            WorkoutType.CYCLE -> 7.5
            WorkoutType.HIKE -> 6.0
        }
        val hours = durationMillis / 3_600_000.0
        return max(1, (met * weightKg * hours).toInt())
    }

    /** Sum of positive altitude deltas (m) — elevation gained, GPS-derived. */
    fun elevationGainMeters(points: List<WorkoutPoint>): Double {
        var gain = 0.0
        var last: Double? = null
        for (p in points) {
            val alt = p.alt ?: continue
            val prev = last
            if (prev != null) {
                val delta = alt - prev
                // Ignore sub-meter noise and GPS altitude jumps > 30 m/segment.
                if (delta in 0.5..30.0) gain += delta
            }
            last = alt
        }
        return gain
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

    // ── shared formatters (single source of truth for all screens) ────────

    fun formatDuration(ms: Long): String {
        val h = ms / 3_600_000
        val m = (ms % 3_600_000) / 60_000
        val s = (ms % 60_000) / 1_000
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.US, "%02d:%02d", m, s)
    }

    /** "4.32 km" / "850 m" — units-aware, defaults to metric. */
    fun formatDistance(meters: Double, imperial: Boolean = false): String {
        val value = if (imperial) meters * 3.28084 else meters
        val unit = if (imperial) "mi" else "km"
        return if (imperial) {
            val miles = meters / 1609.344
            if (miles >= 10) String.format(Locale.US, "%.1f mi", miles)
            else if (miles >= 1) String.format(Locale.US, "%.2f mi", miles)
            else String.format(Locale.US, "%.0f ft", value)
        } else {
            if (meters >= 1000) String.format(Locale.US, "%.2f km", meters / 1000)
            else String.format(Locale.US, "%.0f m", meters)
        }
    }

    fun formatPace(paceSecondsPerKm: Double?, imperial: Boolean = false): String {
        if (paceSecondsPerKm == null) return "—"
        val seconds = if (imperial) paceSecondsPerKm * 1.609344 else paceSecondsPerKm
        val m = (seconds / 60).toInt()
        val s = (seconds - m * 60).toInt()
        val unit = if (imperial) "/mi" else "/km"
        return String.format(Locale.US, "%d:%02d %s", m, s, unit)
    }

    fun formatSpeed(kmh: Double, imperial: Boolean = false): String =
        if (imperial) String.format(Locale.US, "%.1f mph", kmh * 0.621371)
        else String.format(Locale.US, "%.1f km/h", kmh)

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

    /** Cumulative (distance, time) pairs across a workout trail. */
    fun cumulativeDistanceTime(points: List<WorkoutPoint>): List<Pair<Double, Long>> {
        if (points.isEmpty()) return emptyList()
        val result = ArrayList<Pair<Double, Long>>(points.size + 1)
        result += 0.0 to 0L
        var dist = 0.0
        for (i in 1 until points.size) {
            val dt = points[i].timeMillis - points[i - 1].timeMillis
            val kmh = speedKmh(
                points[i - 1].lat, points[i - 1].lng,
                points[i].lat, points[i].lng, dt
            )
            if (kmh > 1.2) dist += StatsRepository.haversineKm(
                points[i - 1].lat, points[i - 1].lng,
                points[i].lat, points[i].lng
            ) * 1000.0
            result += dist to (points[i].timeMillis - points[0].timeMillis)
        }
        return result
    }
}
