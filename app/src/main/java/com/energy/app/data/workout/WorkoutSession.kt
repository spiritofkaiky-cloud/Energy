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
import com.energy.app.data.settings.SettingsRepository
import com.energy.app.EnergyApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.Locale

private const val TAG = "EnergyWorkout"
private const val HEADER_VERSION = 1

/** Save pipeline state for the finish flow (honest "saved" UI). */
enum class SaveStatus { NONE, SAVING, SAVED, FAILED }

/**
 * Live workout session — the Strava core (APP_SPEC §5.5).
 *
 * Reliability contract:
 *  - Every accepted GPS fix is appended to an on-disk journal immediately,
 *    so a process kill at ANY moment loses at most the last fix.
 *  - A small header file tracks totals; on app start the session restores
 *    itself into a paused "draft" state that the user can resume or finish.
 *  - [stop] is idempotent and reports [saveStatus] — the UI only says
 *    "saved" once the workout is actually on disk. On failure the draft
 *    files are kept, so nothing is ever silently lost.
 *  - Fixes pass through [GpsFilter]: accuracy-gated, jump/spike-rejected,
 *    duplicate-minimized. Distance is only ever added from accepted fixes.
 */
class WorkoutSession(
    private val context: Context,
    private val repository: WorkoutRepository,
    private val settings: SettingsRepository,
    private val appScope: CoroutineScope
) {
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val activeDir = File(context.filesDir, "active_workout").apply { mkdirs() }
    private val headerFile = File(activeDir, "header.json")
    private val pointsFile = File(activeDir, "points.jsonl")

    private val filter = GpsFilter(
        maxAccuracyMeters = 100.0,
        maxSpeedKmh = 90.0,
        minDistanceMeters = 2.0,
        minTimeMillis = 1_500L
    )

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

    private val _maxSpeedKmh = MutableStateFlow(0.0)
    val maxSpeedKmh: StateFlow<Double> = _maxSpeedKmh.asStateFlow()

    /** Wall-clock time of the last accepted fix (0 = never) — for GPS-status UI. */
    private val _lastFixMillis = MutableStateFlow(0L)
    val lastFixMillis: StateFlow<Long> = _lastFixMillis.asStateFlow()

    private val _saveStatus = MutableStateFlow(SaveStatus.NONE)
    val saveStatus: StateFlow<SaveStatus> = _saveStatus.asStateFlow()

    private val _lastSavedWorkout = MutableStateFlow<SavedWorkout?>(null)
    val lastSavedWorkout: StateFlow<SavedWorkout?> = _lastSavedWorkout.asStateFlow()

    /** True when the session was rebuilt from a crash draft (show a notice). */
    private val _restored = MutableStateFlow(false)
    val restored: StateFlow<Boolean> = _restored.asStateFlow()

    private var startMillis = 0L
    private var pausedTotal = 0L
    private var lastResumeAt = 0L
    private var lastFixAt = 0L
    private var tickerJob: Job? = null
    private var fallbackJob: Job? = null
    private var gotLiveFix = false
    private var acceptedSinceHeader = 0
    private var journal: BufferedWriter? = null

    init {
        restoreDraft()
    }

    // ── lifecycle ─────────────────────────────────────────────────────────

    fun start(type: WorkoutType) {
        if (_state.value != WorkoutState.IDLE) return
        _type.value = type
        _points.value = emptyList()
        _distanceMeters.value = 0.0
        _elapsedMillis.value = 0L
        _maxSpeedKmh.value = 0.0
        _restored.value = false
        _saveStatus.value = SaveStatus.NONE
        _lastSavedWorkout.value = null
        startMillis = System.currentTimeMillis()
        pausedTotal = 0L
        lastFixAt = 0L
        gotLiveFix = false
        acceptedSinceHeader = 0
        filter.reset()
        _state.value = WorkoutState.RECORDING
        writeHeader()
        openJournal(append = false)
        startTicker()
        registerLocationSources()
    }

    fun pause() {
        if (_state.value != WorkoutState.RECORDING) return
        pausedTotal += System.currentTimeMillis() - lastResumeAt
        _state.value = WorkoutState.PAUSED
        tickerJob?.cancel()
        removeLocationSources()
        closeJournal()
        writeHeader()
    }

    fun resume() {
        if (_state.value != WorkoutState.PAUSED) return
        _state.value = WorkoutState.RECORDING
        lastResumeAt = System.currentTimeMillis()
        writeHeader()
        openJournal(append = true)
        startTicker()
        registerLocationSources()
    }

    /**
     * Finish the workout. Idempotent — a second call while IDLE returns null.
     * The save runs on the application scope; observe [saveStatus] before
     * claiming success in the UI.
     */
    fun stop(): SavedWorkout? {
        if (_state.value == WorkoutState.IDLE) return null
        val endedAt = System.currentTimeMillis()
        val runningTotal = if (_state.value == WorkoutState.RECORDING) {
            pausedTotal + (endedAt - lastResumeAt)
        } else pausedTotal
        tickerJob?.cancel()
        fallbackJob?.cancel()
        removeLocationSources()
        closeJournal()
        _state.value = WorkoutState.IDLE
        persistWorkout(endedAt, runningTotal)
        return _lastSavedWorkout.value
    }

    /** Retry a failed save (draft files are still on disk). */
    fun retrySave() {
        if (_saveStatus.value != SaveStatus.FAILED) return
        persistWorkout(lastEndedAt, lastRunningTotal)
    }

    private var lastEndedAt = 0L
    private var lastRunningTotal = 0L

    private fun persistWorkout(endedAt: Long, runningTotal: Long) {
        lastEndedAt = endedAt
        lastRunningTotal = runningTotal
        _saveStatus.value = SaveStatus.SAVING
        appScope.launch {
            try {
                val weight = runCatching { settings.preferences.first().weightKg.toDouble() }
                    .getOrDefault(70.0)
                val workout = SavedWorkout(
                    id = WorkoutRepository.generateId(),
                    type = _type.value,
                    startMillis = startMillis,
                    endMillis = endedAt,
                    distanceMeters = _distanceMeters.value,
                    durationMillis = runningTotal,
                    points = _points.value,
                    calories = WorkoutMath.calories(_type.value, runningTotal, weight),
                    elevationGainMeters = WorkoutMath.elevationGainMeters(_points.value)
                )
                repository.save(workout)
                clearDraftFiles()
                _lastSavedWorkout.value = workout
                _saveStatus.value = SaveStatus.SAVED
                syncToCloud(workout)
            } catch (e: Exception) {
                Log.e(TAG, "save failed — draft kept for recovery", e)
                _saveStatus.value = SaveStatus.FAILED
            }
        }
    }

    /** Abandon a restored draft without saving it. */
    fun discardDraft() {
        if (_state.value == WorkoutState.IDLE && !_restored.value) return
        tickerJob?.cancel()
        fallbackJob?.cancel()
        removeLocationSources()
        closeJournal()
        _state.value = WorkoutState.IDLE
        _restored.value = false
        _points.value = emptyList()
        _distanceMeters.value = 0.0
        _elapsedMillis.value = 0L
        _saveStatus.value = SaveStatus.NONE
        clearDraftFiles()
    }

    private suspend fun syncToCloud(workout: SavedWorkout) {
        val app = context.applicationContext as? EnergyApplication ?: return
        if (!app.container.authRepository.currentUserIsGuest()) {
            runCatching {
                app.container.cloudRepository.syncWorkout(
                    WorkoutRepository.toCloudJson(workout)
                ).onSuccess {
                    repository.markSyncState(workout.id, SyncState.SYNCED)
                }.onFailure {
                    repository.markSyncState(workout.id, SyncState.FAILED)
                }
            }.onFailure { Log.w(TAG, "cloud sync skipped: ${it.message}") }
        }
    }

    // ── GPS sources (fused first, framework fallback) ─────────────────────

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

    // ── fix intake (quality-gated) ────────────────────────────────────────

    private fun onFix(loc: Location) {
        if (_state.value != WorkoutState.RECORDING) return
        gotLiveFix = true
        val now = System.currentTimeMillis()
        val lat = loc.latitude
        val lng = loc.longitude
        val accuracy = if (loc.hasAccuracy()) loc.accuracy.toDouble() else null

        if (!filter.accept(lat, lng, now, accuracy)) return

        val prev = _points.value.lastOrNull()
        var segmentSpeed = if (loc.hasSpeed() && loc.speed > 0) loc.speed * 3.6 else 0.0
        if (prev != null) {
            val dt = now - prev.timeMillis
            if (dt > 0) {
                val d = GpsFilter.haversineMeters(prev.lat, prev.lng, lat, lng)
                _distanceMeters.value = _distanceMeters.value + d
                if (d > 0.5) segmentSpeed = d / (dt / 3_600_000.0)
            }
        }
        _maxSpeedKmh.value = maxOf(_maxSpeedKmh.value, segmentSpeed)
        lastFixAt = now
        _lastFixMillis.value = now

        val point = WorkoutPoint(
            lat, lng, now, segmentSpeed,
            alt = if (loc.hasAltitude()) loc.altitude else null
        )
        _points.value = _points.value + point
        appendJournalLine(point)
        acceptedSinceHeader++
        if (acceptedSinceHeader >= 10) {
            acceptedSinceHeader = 0
            writeHeader()
        }
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

    /** Current speed km/h from the last two accepted fixes. */
    val currentSpeedKmh: Double
        get() {
            val pts = _points.value
            if (pts.size >= 2) {
                val a = pts[pts.size - 2]
                val b = pts.last()
                val dt = (b.timeMillis - a.timeMillis) / 1000.0
                if (dt > 0) {
                    val d = GpsFilter.haversineMeters(a.lat, a.lng, b.lat, b.lng)
                    if (d > 1) return d / dt * 3.6
                }
            }
            return pts.lastOrNull()?.speedKmh ?: 0.0
        }

    // ── crash-safe draft persistence ──────────────────────────────────────

    private fun writeHeader() {
        try {
            val json = JSONObject()
                .put("v", HEADER_VERSION)
                .put("type", _type.value.name)
                .put("start", startMillis)
                .put("state", _state.value.name)
                .put("pausedTotal", pausedTotal)
                .put("lastResumeAt", lastResumeAt)
                .put("lastFixMillis", lastFixAt)
            headerFile.writeText(json.toString())
        } catch (e: Exception) {
            Log.e(TAG, "header write failed", e)
        }
    }

    private fun openJournal(append: Boolean) {
        closeJournal()
        try {
            journal = BufferedWriter(FileWriter(pointsFile, append))
        } catch (e: Exception) {
            Log.e(TAG, "journal open failed", e)
        }
    }

    private fun appendJournalLine(p: WorkoutPoint) {
        try {
            journal?.apply {
                write(String.format(
                    Locale.US, "%.7f,%.7f,%d,%.2f,%s%n",
                    p.lat, p.lng, p.timeMillis, p.speedKmh,
                    p.alt?.let { String.format(Locale.US, "%.1f", it) } ?: ""
                ))
                flush()
            }
        } catch (e: Exception) {
            Log.e(TAG, "journal append failed", e)
        }
    }

    private fun closeJournal() {
        try {
            journal?.close()
        } catch (e: Exception) {
            Log.w(TAG, "journal close failed", e)
        }
        journal = null
    }

    private fun clearDraftFiles() {
        headerFile.delete()
        pointsFile.delete()
    }

    /** Rebuild an interrupted session from the on-disk draft (conservative: PAUSED). */
    private fun restoreDraft() {
        appScope.launch {
            try {
                if (!headerFile.exists()) return@launch
                val header = JSONObject(headerFile.readText())
                if (header.optInt("v", 0) != HEADER_VERSION) {
                    clearDraftFiles()
                    return@launch
                }
                val type = runCatching {
                    WorkoutType.valueOf(header.getString("type"))
                }.getOrElse { WorkoutType.RUN }

                val pts = loadJournalPoints()
                var distance = 0.0
                for (i in 1 until pts.size) {
                    distance += GpsFilter.haversineMeters(
                        pts[i - 1].lat, pts[i - 1].lng, pts[i].lat, pts[i].lng
                    )
                }

                _type.value = type
                _points.value = pts
                _distanceMeters.value = distance
                _elapsedMillis.value = header.optLong("pausedTotal", 0L)
                _maxSpeedKmh.value = pts.maxOfOrNull { it.speedKmh } ?: 0.0
                startMillis = header.optLong("start", 0L)
                pausedTotal = header.optLong("pausedTotal", 0L)
                lastResumeAt = header.optLong("lastResumeAt", 0L)
                _state.value = WorkoutState.PAUSED
                _restored.value = true
                Log.i(TAG, "restored draft workout (${pts.size} points)")
            } catch (e: Exception) {
                Log.w(TAG, "draft restore failed — starting fresh", e)
                clearDraftFiles()
                _state.value = WorkoutState.IDLE
            }
        }
    }

    private fun loadJournalPoints(): List<WorkoutPoint> {
        if (!pointsFile.exists()) return emptyList()
        return pointsFile.useLines { lines ->
            lines.mapNotNull { line ->
                val parts = line.split(',')
                if (parts.size < 4) return@mapNotNull null
                runCatching {
                    WorkoutPoint(
                        lat = parts[0].toDouble(),
                        lng = parts[1].toDouble(),
                        timeMillis = parts[2].toLong(),
                        speedKmh = parts[3].toDouble(),
                        alt = parts.getOrNull(4)?.takeIf { it.isNotBlank() }?.toDouble()
                    )
                }.getOrNull()
            }.toList()
        }
    }
}
