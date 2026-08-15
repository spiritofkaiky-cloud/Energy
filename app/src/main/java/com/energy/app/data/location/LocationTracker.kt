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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "EnergyLocation"
private const val FALLBACK_DELAY_MS = 20_000L

/**
 * Passive all-day movement tracking — the Strava-style breadcrumb trail.
 *
 * Primary: GMS fused provider (battery-friendly on real devices).
 * Fallback: framework GPS provider, engaged automatically if fused stays
 * silent (e.g. broken GMS state on fresh emulators / GMS-free devices).
 */
class LocationTracker(
    private val context: Context,
    private val dayPathRepository: DayPathRepository
) {
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _points = MutableStateFlow<List<DayPoint>>(emptyList())
    val points: StateFlow<List<DayPoint>> = _points.asStateFlow()

    private val _tracking = MutableStateFlow(false)
    val tracking: StateFlow<Boolean> = _tracking.asStateFlow()

    private var lastLat: Double? = null
    private var lastLng: Double? = null

    private val fusedCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            gotFix = true
            handleFix(loc.latitude, loc.longitude)
        }
    }

    private val gpsListener = object : LocationListener {
        override fun onLocationChanged(loc: Location) {
            gotFix = true
            handleFix(loc.latitude, loc.longitude)
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
    }

    private var gotFix = false

    fun start() {
        if (_tracking.value) return
        _tracking.value = true
        gotFix = false
        scope.launch {
            _points.value = dayPathRepository.todayPoints.first()
        }
        anchorFromLastKnown()

        val request = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            30_000
        )
            .setMinUpdateDistanceMeters(25f)
            .setMaxUpdateDelayMillis(120_000)
            .build()

        try {
            fusedClient.requestLocationUpdates(request, fusedCallback, Looper.getMainLooper())
            Log.i(TAG, "fused updates requested")
        } catch (e: Exception) {
            Log.e(TAG, "fused request failed — using framework GPS", e)
            enableGpsFallback()
        }

        // Auto-heal: if fused never delivers, fall back to framework GPS.
        scope.launch {
            delay(FALLBACK_DELAY_MS)
            if (!gotFix && _tracking.value) {
                Log.w(TAG, "fused silent for ${FALLBACK_DELAY_MS}ms — enabling framework GPS")
                runCatching { fusedClient.removeLocationUpdates(fusedCallback) }
                enableGpsFallback()
            }
        }
    }

    fun stop() {
        if (!_tracking.value) return
        _tracking.value = false
        runCatching { fusedClient.removeLocationUpdates(fusedCallback) }
        runCatching { locationManager.removeUpdates(gpsListener) }
    }

    private fun anchorFromLastKnown() {
        runCatching { locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) }
            .getOrNull()?.let { loc ->
                handleFix(loc.latitude, loc.longitude)
            }
    }

    private fun enableGpsFallback() {
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                30_000L,
                25f,
                gpsListener,
                Looper.getMainLooper()
            )
            Log.i(TAG, "framework GPS updates requested")
        } catch (e: SecurityException) {
            Log.e(TAG, "GPS fallback failed (no permission)", e)
            _tracking.value = false
        }
    }

    private fun handleFix(lat: Double, lng: Double) {
        // Dedupe identical consecutive fixes.
        if (lat == lastLat && lng == lastLng) return
        lastLat = lat
        lastLng = lng
        Log.i(TAG, "fix: $lat,$lng")
        scope.launch {
            dayPathRepository.addPoint(lat, lng)
            _points.value = dayPathRepository.todayPoints.first()
        }
    }
}
