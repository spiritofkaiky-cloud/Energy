package com.energy.app.data.workout

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

private val Context.workoutStore by preferencesDataStore(name = "energy_workouts")

private const val TAG = "WorkoutRepository"

/**
 * Workout persistence — local-first, crash-tolerant (APP_SPEC §34).
 *
 * Design:
 *  - A SMALL versioned metadata array lives in DataStore (id, totals, sync
 *    state — no route points). It stays tiny no matter how long history gets.
 *  - Route points live in per-workout JSON files under filesDir/workouts/,
 *    written atomically (temp file + rename). Corruption in one workout
 *    can never damage another.
 *  - Legacy v0.4 format (one giant JSON blob incl. points in DataStore) is
 *    migrated transparently on first read.
 *  - Every metadata write also keeps a backup of the previous good state;
 *    if the primary ever fails to parse, the backup is restored.
 *  - Saves are upserts by id — replaying a save cannot duplicate a workout.
 *
 * All serialization lives in [WorkoutMetaCodec] so corruption handling is
 * unit-tested without an Android runtime.
 */
class WorkoutRepository(private val context: Context) {

    private object Keys {
        val META = stringPreferencesKey("meta_v1")
        val META_BACKUP = stringPreferencesKey("meta_v1_backup")
        val LEGACY = stringPreferencesKey("workouts") // v0.4 blob (with inline points)
    }

    private val pointsDir: File = File(context.filesDir, "workouts").apply { mkdirs() }
    private val fileMutex = Mutex()
    private val pointsCache = ConcurrentHashMap<String, List<WorkoutPoint>>()

    /** Newest first. Points are NOT included — load them with [points]. */
    val workouts: Flow<List<SavedWorkout>> = context.workoutStore.data.map { prefs ->
        val legacy = prefs[Keys.LEGACY].orEmpty()
        if (legacy.isNotBlank()) {
            migrateLegacy(legacy)
        }
        readMeta(prefs[Keys.META].orEmpty(), prefs[Keys.META_BACKUP].orEmpty())
    }

    suspend fun save(workout: SavedWorkout) {
        withContext(Dispatchers.IO) {
            fileMutex.withLock { writePointsFile(workout.id, workout.points) }
            context.workoutStore.edit { prefs ->
                val current = readMeta(prefs[Keys.META].orEmpty(), prefs[Keys.META_BACKUP].orEmpty())
                val upserted = listOf(workout.copy(points = emptyList())) +
                    current.filter { it.id != workout.id }
                prefs[Keys.META_BACKUP] = prefs[Keys.META].orEmpty()
                prefs[Keys.META] = WorkoutMetaCodec.encode(upserted)
            }
        }
        pointsCache[workout.id] = workout.points
    }

    suspend fun points(id: String): List<WorkoutPoint> = withContext(Dispatchers.IO) {
        pointsCache[id]?.let { return@withContext it }
        val file = File(pointsDir, "$id.json")
        val parsed = if (file.exists()) {
            WorkoutMetaCodec.decodePoints(file.readText()) ?: emptyList()
        } else emptyList()
        pointsCache[id] = parsed
        parsed
    }

    suspend fun delete(id: String) {
        withContext(Dispatchers.IO) {
            context.workoutStore.edit { prefs ->
                val current = readMeta(prefs[Keys.META].orEmpty(), prefs[Keys.META_BACKUP].orEmpty())
                prefs[Keys.META_BACKUP] = prefs[Keys.META].orEmpty()
                prefs[Keys.META] = WorkoutMetaCodec.encode(current.filter { it.id != id })
            }
            pointsCache.remove(id)
            File(pointsDir, "$id.json").delete()
        }
    }

    /** Workouts still awaiting cloud sync (guest sessions skip sync). */
    suspend fun pendingSync(): List<SavedWorkout> {
        val metas = workouts.first()
        return metas.filter { it.syncState == SyncState.PENDING }.map { meta ->
            meta.copy(points = points(meta.id))
        }
    }

    suspend fun markSyncState(id: String, state: SyncState) {
        context.workoutStore.edit { prefs ->
            val current = readMeta(prefs[Keys.META].orEmpty(), prefs[Keys.META_BACKUP].orEmpty())
            prefs[Keys.META_BACKUP] = prefs[Keys.META].orEmpty()
            prefs[Keys.META] = WorkoutMetaCodec.encode(
                current.map { if (it.id == id) it.copy(syncState = state) else it }
            )
        }
    }

    // ── decoding with backup fallback ─────────────────────────────────────

    private fun readMeta(primary: String, backup: String): List<SavedWorkout> {
        val fromPrimary = WorkoutMetaCodec.decode(primary)
        return when {
            fromPrimary != null -> fromPrimary
            backup.isNotBlank() -> {
                Log.w(TAG, "metadata corrupt — restoring from backup")
                WorkoutMetaCodec.decode(backup) ?: emptyList()
            }
            else -> emptyList()
        }
    }

    // ── per-workout points files (atomic tmp+rename) ───────────────────────

    private fun writePointsFile(id: String, points: List<WorkoutPoint>) {
        val file = File(pointsDir, "$id.json")
        val tmp = File(pointsDir, "$id.json.tmp")
        tmp.writeText(WorkoutMetaCodec.encodePoints(points))
        file.delete()
        if (!tmp.renameTo(file)) {
            tmp.copyTo(file, overwrite = true)
            tmp.delete()
        }
    }

    // ── v0.4 → v1 migration: split the inline blob into meta + files ───────

    private suspend fun migrateLegacy(legacyJson: String) {
        val items = WorkoutMetaCodec.decodeLegacy(legacyJson)
        if (items.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                fileMutex.withLock {
                    items.forEach { w ->
                        if (!File(pointsDir, "${w.id}.json").exists()) {
                            writePointsFile(w.id, w.points)
                        }
                    }
                }
            }
        }
        context.workoutStore.edit { prefs ->
            if (prefs[Keys.META].orEmpty().isBlank() && items.isNotEmpty()) {
                prefs[Keys.META] = WorkoutMetaCodec.encode(items)
            }
            prefs.remove(Keys.LEGACY)
        }
        Log.i(TAG, "migrated ${items.size} workouts from legacy storage")
    }

    companion object {
        fun generateId(): String = java.util.UUID.randomUUID().toString()

        /** Cloud payload for one workout (consumed by CloudRepository). */
        fun toCloudJson(workout: SavedWorkout): String {
            val arr = org.json.JSONArray()
            arr.put(org.json.JSONObject()
                .put("id", workout.id)
                .put("type", workout.type.name)
                .put("start", workout.startMillis)
                .put("end", workout.endMillis)
                .put("distance", workout.distanceMeters)
                .put("duration", workout.durationMillis)
                .put("calories", workout.calories)
                .put("elevation", workout.elevationGainMeters)
                .put("points", org.json.JSONArray().apply {
                    workout.points.forEach { p ->
                        put(org.json.JSONObject()
                            .put("lat", p.lat).put("lng", p.lng).put("t", p.timeMillis)
                            .put("s", p.speedKmh).apply { p.alt?.let { put("a", it) } })
                    }
                })
            )
            return arr.toString()
        }
    }
}
