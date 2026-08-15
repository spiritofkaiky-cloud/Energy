package com.energy.app.data.location

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.energy.app.data.settings.SettingsRepository
import com.energy.app.data.workout.GpsFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "EnergyLocation"
private const val FALLBACK_DELAY_MS = 20_000L

/**
 * Passive all-day movement tracking — the Strava-style breadcrumb trail.
 *
 * Primary: GMS fused provider (battery-friendly on real devices).
 * Fallback: framework GPS provider, engaged automatically if fused stays
 * silent (e.g. broken GMS state on fresh emulators / GMS-free devices).
 *
 * Quality & efficiency:
 *  - fixes pass through [GpsFilter] (accuracy, jumps, dupes) so a GPS glitch
 *    cannot draw a nonsense trail;
 *  - fixes are buffered in memory and flushed to disk in batches — no more
 *    one DataStore rewrite per fix;
 *  - last-known anchors older than 10 minutes are ignored (stale dots);
 *  - battery-saver mode widens the update interval.
 */
class LocationTracker(
    private val context: Context,
    private val dayPathRepository: DayPathRepository,
    private val scope: CoroutineScope,
    private val settings: SettingsRepository
) {
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val filter = GpsFilter(
        maxAccuracyMeters = 200.0,
        maxSpeedKmh = 60.0,
        minDistanceMeters = 5.0,
        minTimeMillis = 2_000L,
        anchorTimeMillis = 30_000L
    )

    private val _points = MutableStateFlow<List<DayPoint>>(emptyList())
    val points: StateFlow<List<DayPoint>> = _points.asStateFlow()

    private val _tracking = MutableStateFlow(false)
    val tracking: StateFlow<Boolean> = _tracking.asStateFlow()

    // Buffer between GPS callbacks (main thread) and disk (IO).
    private val pending = mutableListOf<DayPoint>()
    private var flushJob: Job? = null
    private var batterySaver = false

    private var gotFix = false

    private val fusedCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            gotFix = true
            handleFix(loc)
        }
    }

    private val gpsListener = object : LocationListener {
        override fun onLocationChanged(loc: Location) {
            gotFix = true
            handleFix(loc)
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
    }

    fun start() {
        if (_tracking.value) return
        _tracking.value = true
        gotFix = false
        filter.reset()
        scope.launch {
            _points.value = dayPathRepository.todayPoints.first()
        }
        anchorFromLastKnown()

        scope.launch {
            batterySaver = runCatching { settings.preferences.first().batterySaver }
                .getOrDefault(false)
            val interval = if (batterySaver) 60_000L else 30_000L
            val request = LocationRequest.Builder(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                interval
            )
                .setMinUpdateDistanceMeters(if (batterySaver) 50f else 25f)
                .setMaxUpdateDelayMillis(120_000)
                .build()

            try {
                fusedClient.requestLocationUpdates(request, fusedCallback, Looper.getMainLooper())
                Log.i(TAG, "fused updates requested (${interval}ms)")
            } catch (e: Exception) {
                Log.e(TAG, "fused request failed — using framework GPS", e)
                enableGpsFallback(interval)
            }

            // Auto-heal: if fused never delivers, fall back to framework GPS.
            delay(FALLBACK_DELAY_MS)
            if (!gotFix && _tracking.value) {
                Log.w(TAG, "fused silent for ${FALLBACK_DELAY_MS}ms — enabling framework GPS")
                runCatching { fusedClient.removeLocationUpdates(fusedCallback) }
                enableGpsFallback(interval)
            }
        }
    }

    fun stop() {
        if (!_tracking.value) return
        _tracking.value = false
        runCatching { fusedClient.removeLocationUpdates(fusedCallback) }
        runCatching { locationManager.removeUpdates(gpsListener) }
        flushJob?.cancel()
        flushPending()
    }

    private fun anchorFromLastKnown() {
        val anchor = runCatching {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }.getOrNull() ?: return
        // Ignore stale anchors — a "today" trail must not start at
        // yesterday's position.
        if (System.currentTimeMillis() - anchor.time > 10 * 60_000L) return
        handleFix(anchor)
    }

    private fun enableGpsFallback(interval: Long) {
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                interval,
                if (batterySaver) 50f else 25f,
                gpsListener,
                Looper.getMainLooper()
            )
            Log.i(TAG, "framework GPS updates requested")
        } catch (e: SecurityException) {
            Log.e(TAG, "GPS fallback failed (no permission)", e)
            _tracking.value = false
        }
    }

    private fun handleFix(loc: Location) {
        if (!_tracking.value) return
        val lat = loc.latitude
        val lng = loc.longitude
        val now = System.currentTimeMillis()
        val accuracy = if (loc.hasAccuracy()) loc.accuracy.toDouble() else null

        if (!filter.accept(lat, lng, now, accuracy)) return

        val point = DayPoint(lat, lng, now)
        _points.value = _points.value + point
        pending += point
        Log.i(TAG, "fix: $lat,$lng")

        // Batch-flush: at least every 15 s, or immediately once 5 pile up.
        if (pending.size >= 5) {
            flushJob?.cancel()
            flushPending()
        } else if (flushJob?.isActive != true) {
            flushJob = scope.launch {
                delay(15_000)
                flushPending()
            }
        }
    }

    private fun flushPending() {
        if (pending.isEmpty()) return
        val batch = pending.toList()
        pending.clear()
        scope.launch {
            withContext(Dispatchers.IO) { dayPathRepository.addPoints(batch) }
        }
    }
}
