package com.energy.app.data.workout

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
import com.energy.app.EnergyApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val TAG = "EnergyWorkout"

/**
 * Live workout session — the Strava core (APP_SPEC §5.5).
 * Records GPS points, computes distance (haversine), pace, duration.
 * Fused provider primary, framework GPS auto-fallback (same pattern as
 * LocationTracker — heals broken GMS/emulator states).
 */
class WorkoutSession(
    private val context: Context,
    private val repository: WorkoutRepository
) {
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(WorkoutState.IDLE)
    val state: StateFlow<WorkoutState> = _state.asStateFlow()

    private val _type = MutableStateFlow(WorkoutType.RUN)
    val type: StateFlow<WorkoutType> = _type.asStateFlow()

    private val _points = MutableStateFlow<List<WorkoutPoint>>(emptyList())
    val points: StateFlow<List<WorkoutPoint>> = _points.asStateFlow()

    private val _distanceMeters = MutableStateFlow(0.0)
    val distanceMeters: StateFlow<Double> = _distanceMeters.asStateFlow()

    private val _elapsedMillis = MutableStateFlow(0L)
    val elapsedMillis: StateFlow<Long> = _elapsedMillis.asStateFlow()

    private var startMillis = 0L
    private var pausedTotal = 0L
    private var lastFixMillis = 0L
    private var lastLat = Double.NaN
    private var lastLng = Double.NaN
    private var lastSpeed = 0.0
    private var tickerJob: Job? = null
    private var fallbackJob: Job? = null
    private var gotLiveFix = false

    private val fusedCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { onFix(it) }
        }
    }

    private val gpsListener = object : LocationListener {
        override fun onLocationChanged(loc: Location) = onFix(loc)
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
    }

    fun start(type: WorkoutType) {
        if (_state.value != WorkoutState.IDLE) return
        _type.value = type
        _points.value = emptyList()
        _distanceMeters.value = 0.0
        _elapsedMillis.value = 0L
        startMillis = System.currentTimeMillis()
        pausedTotal = 0L
        lastFixMillis = 0L
        lastLat = Double.NaN
        lastLng = Double.NaN
        gotLiveFix = false
        _state.value = WorkoutState.RECORDING
        startTicker()
        registerLocationSources()
    }

    fun pause() {
        if (_state.value != WorkoutState.RECORDING) return
        _state.value = WorkoutState.PAUSED
        pausedTotal += System.currentTimeMillis() - lastResumeAt
        tickerJob?.cancel()
        removeLocationSources()
    }

    fun resume() {
        if (_state.value != WorkoutState.PAUSED) return
        _state.value = WorkoutState.RECORDING
        lastResumeAt = System.currentTimeMillis()
        startTicker()
        registerLocationSources()
    }

    fun stop(): SavedWorkout? {
        if (_state.value == WorkoutState.IDLE) return null
        val endedAt = System.currentTimeMillis()
        val runningTotal = if (_state.value == WorkoutState.RECORDING) {
            pausedTotal + (endedAt - lastResumeAt)
        } else pausedTotal
        tickerJob?.cancel()
        fallbackJob?.cancel()
        removeLocationSources()
        _state.value = WorkoutState.IDLE

        val workout = SavedWorkout(
            id = WorkoutRepository.newId(),
            type = _type.value,
            startMillis = startMillis,
            endMillis = endedAt,
            distanceMeters = _distanceMeters.value,
            durationMillis = runningTotal,
            points = _points.value
        )
        scope.launch { repository.save(workout) }
        // Cloud sync (M5): best-effort — no-op until Supabase is configured
        // and the user is signed in with Google.
        scope.launch {
            val app = context.applicationContext as? EnergyApplication ?: return@launch
            runCatching {
                app.container.cloudRepository.syncWorkout(
                    WorkoutRepository.toCloudJson(workout)
                )
            }.onFailure { Log.w(TAG, "cloud sync skipped: ${it.message}") }
        }
        return workout
    }

    private var lastResumeAt = 0L

    private fun registerLocationSources() {
        gotLiveFix = false
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3_000)
            .setMinUpdateDistanceMeters(2f)
            .build()
        try {
            fusedClient.requestLocationUpdates(request, fusedCallback, Looper.getMainLooper())
        } catch (e: Exception) {
            Log.e(TAG, "fused failed — framework GPS", e)
            enableGpsFallback()
        }
        fallbackJob = scope.launch {
            delay(20_000)
            if (!gotLiveFix && _state.value == WorkoutState.RECORDING) {
                Log.w(TAG, "fused silent — framework GPS fallback")
                runCatching { fusedClient.removeLocationUpdates(fusedCallback) }
                enableGpsFallback()
            }
        }
    }

    private fun enableGpsFallback() {
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 3_000L, 2f, gpsListener, Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "GPS fallback failed", e)
        }
    }

    private fun removeLocationSources() {
        runCatching { fusedClient.removeLocationUpdates(fusedCallback) }
        runCatching { locationManager.removeUpdates(gpsListener) }
        fallbackJob?.cancel()
    }

    private fun onFix(loc: Location) {
        if (_state.value != WorkoutState.RECORDING) return
        gotLiveFix = true
        val lat = loc.latitude
        val lng = loc.longitude
        val now = System.currentTimeMillis()
        val speed = if (loc.hasSpeed() && loc.speed > 0) loc.speed * 3.6 else lastSpeed

        if (!lastLat.isNaN()) {
            val d = haversineMeters(lastLat, lastLng, lat, lng)
            if (d >= 2.0) {
                _distanceMeters.value = _distanceMeters.value + d
            }
        }
        lastLat = lat
        lastLng = lng
        lastSpeed = speed
        lastFixMillis = now
        _points.value = _points.value + WorkoutPoint(lat, lng, now, speed)
    }

    private fun startTicker() {
        lastResumeAt = System.currentTimeMillis()
        tickerJob = scope.launch {
            while (isActive && _state.value == WorkoutState.RECORDING) {
                _elapsedMillis.value = pausedTotal + (System.currentTimeMillis() - lastResumeAt)
                delay(1_000)
            }
        }
    }

    /** Current speed km/h, falling back to distance-based average. */
    val currentSpeedKmh: Double
        get() {
            val pts = _points.value
            if (pts.size >= 2) {
                val a = pts[pts.size - 2]
                val b = pts.last()
                val dt = (b.timeMillis - a.timeMillis) / 1000.0
                if (dt > 0) {
                    val d = haversineMeters(a.lat, a.lng, b.lat, b.lng)
                    if (d > 1) return d / dt * 3.6
                }
            }
            return lastSpeed
        }

    companion object {
        fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
            val r = 6_371_000.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLng = Math.toRadians(lng2 - lng1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2) * sin(dLng / 2)
            return r * 2 * atan2(sqrt(a), sqrt(1 - a))
        }
    }
}
