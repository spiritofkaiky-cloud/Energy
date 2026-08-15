package com.energy.app.data.workout

import kotlin.math.abs

/**
 * Deterministic GPS quality gate (APP_SPEC §10 — "GPS reliability is
 * mission-critical"). Rejects fixes that would corrupt a route:
 *
 *  1. Out-of-world coordinates (incl. the classic (0,0) "null island" glitch)
 *  2. Fixes with unusably poor reported accuracy
 *  3. "Impossible jumps" — implied speed beyond a hard physical ceiling
 *  4. Spikes — single fixes implying a wild speed jump relative to the
 *     previous segment (GPS multipath signature); they almost always snap
 *     back, and one bad point can add dozens of fake meters.
 *  5. Duplicate / spammy fixes (too close AND too soon)
 *
 * Accuracy is favored over cosmetic smoothness: a rejected fix is simply
 * not added to the route.
 */
class GpsFilter(
    /** Fixes with worse reported accuracy (meters) are rejected. */
    val maxAccuracyMeters: Double = 100.0,
    /** Hard physical ceiling (km/h) — reject implied speeds above this. */
    val maxSpeedKmh: Double = 90.0,
    /** Reject jumps more than this many times the previous segment speed. */
    val spikeRatio: Double = 3.0,
    /** Spike rejection only applies above this speed (km/h). */
    val spikeMinSpeedKmh: Double = 45.0,
    /** Minimum movement (m) between accepted fixes… */
    val minDistanceMeters: Double = 2.0,
    /** …unless at least this much time (ms) has passed (breadcrumb anchor). */
    val minTimeMillis: Long = 1_500L,
    val anchorTimeMillis: Long = 10_000L
) {
    private var lastLat = Double.NaN
    private var lastLng = Double.NaN
    private var lastTime = 0L
    private var lastSpeedKmh = 0.0
    private var lastAcceptedTime = 0L
    private var lastAcceptedLat = Double.NaN
    private var lastAcceptedLng = Double.NaN

    /** Returns true when the candidate fix should be added to the route. */
    fun accept(lat: Double, lng: Double, timeMillis: Long, accuracyMeters: Double?): Boolean {
        // 1. Coordinates must be plausible.
        if (lat.isNaN() || lng.isNaN() || abs(lat) > 90.0 || abs(lng) > 180.0) return false
        if (abs(lat) < 0.001 && abs(lng) < 0.001) return false // null island

        // 2. Accuracy gate — unknown accuracy (0 or null) is accepted.
        if (accuracyMeters != null && accuracyMeters > 0 && accuracyMeters > maxAccuracyMeters) {
            return false
        }

        val haveLast = !lastLat.isNaN()
        if (haveLast) {
            val dt = timeMillis - lastTime
            if (dt <= 0) return false // clock went backwards — drop
            val d = haversineMeters(lastLat, lastLng, lat, lng)
            // km/h: meters → km, then divide by hours.
            val impliedKmh = d / 1000.0 / (dt / 3_600_000.0)

            // 3. Impossible jump — reject outright.
            if (impliedKmh > maxSpeedKmh) return false

            // 4. Spike relative to the previous accepted segment.
            val havePrevAccepted = !lastAcceptedLat.isNaN() &&
                (timeMillis - lastAcceptedTime) in 0..60_000L
            if (havePrevAccepted) {
                val dPrev = haversineMeters(lastAcceptedLat, lastAcceptedLng, lat, lng)
                val dtPrev = (timeMillis - lastAcceptedTime) / 3_600_000.0
                if (dtPrev > 0) {
                    val impliedFromPrevKmh = dPrev / 1000.0 / dtPrev
                    if (impliedFromPrevKmh > spikeMinSpeedKmh &&
                        lastSpeedKmh > 0 &&
                        impliedFromPrevKmh > lastSpeedKmh * spikeRatio
                    ) {
                        return false
                    }
                }
            }

            // 5. Duplicate / spam — accept tiny movements only as anchors.
            if (d < minDistanceMeters && dt < anchorTimeMillis) return false
        }

        lastLat = lat
        lastLng = lng
        lastTime = timeMillis
        val dtFromAccepted = timeMillis - lastAcceptedTime
        lastSpeedKmh = if (dtFromAccepted > 0 && !lastAcceptedLat.isNaN()) {
            haversineMeters(lastAcceptedLat, lastAcceptedLng, lat, lng) / 1000.0 /
                (dtFromAccepted / 3_600_000.0)
        } else 0.0
        lastAcceptedTime = timeMillis
        lastAcceptedLat = lat
        lastAcceptedLng = lng
        return true
    }

    fun reset() {
        lastLat = Double.NaN
        lastLng = Double.NaN
        lastTime = 0L
        lastSpeedKmh = 0.0
        lastAcceptedTime = 0L
        lastAcceptedLat = Double.NaN
        lastAcceptedLng = Double.NaN
    }

    companion object {
        fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
            val r = 6_371_000.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLng = Math.toRadians(lng2 - lng1)
            val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLng / 2) * kotlin.math.sin(dLng / 2)
            return r * 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        }
    }
}
