package com.energy.app.data.workout

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.workoutStore by preferencesDataStore(name = "energy_workouts")

/**
 * Saved workouts, JSON in DataStore (newest first). Room arrives with M5
 * cloud sync; this keeps M4 fully local-first per APP_SPEC §7.
 */
class WorkoutRepository(private val context: Context) {

    private object Keys { val WORKOUTS = stringPreferencesKey("workouts") }

    val workouts: Flow<List<SavedWorkout>> = context.workoutStore.data.map { prefs ->
        parse(prefs[Keys.WORKOUTS].orEmpty())
    }

    suspend fun save(workout: SavedWorkout) {
        context.workoutStore.edit { prefs ->
            val list = parse(prefs[Keys.WORKOUTS].orEmpty())
            prefs[Keys.WORKOUTS] = toJson(listOf(workout) + list)
        }
    }

    suspend fun delete(id: String) {
        context.workoutStore.edit { prefs ->
            prefs[Keys.WORKOUTS] = toJson(parse(prefs[Keys.WORKOUTS].orEmpty()).filter { it.id != id })
        }
    }

    private fun parse(json: String): List<SavedWorkout> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                val pts = o.getJSONArray("points")
                val points = (0 until pts.length()).map { j ->
                    val p = pts.getJSONObject(j)
                    WorkoutPoint(p.getDouble("lat"), p.getDouble("lng"), p.getLong("t"), p.getDouble("s"))
                }
                SavedWorkout(
                    id = o.getString("id"),
                    type = WorkoutType.valueOf(o.getString("type")),
                    startMillis = o.getLong("start"),
                    endMillis = o.getLong("end"),
                    distanceMeters = o.getDouble("distance"),
                    durationMillis = o.getLong("duration"),
                    points = points
                )
            }
        }.getOrDefault(emptyList())
    }

    companion object {
        fun newId() = UUID.randomUUID().toString()

        /** Cloud payload for a saved workout (consumed by CloudRepository). */
        fun toCloudJson(workout: SavedWorkout): String = toJson(listOf(workout))

        private fun toJson(list: List<SavedWorkout>): String {
            val arr = JSONArray()
            list.forEach { w ->
                val pts = JSONArray()
                w.points.forEach { p ->
                    pts.put(JSONObject().put("lat", p.lat).put("lng", p.lng).put("t", p.timeMillis).put("s", p.speedKmh))
                }
                arr.put(
                    JSONObject()
                        .put("id", w.id)
                        .put("type", w.type.name)
                        .put("start", w.startMillis)
                        .put("end", w.endMillis)
                        .put("distance", w.distanceMeters)
                        .put("duration", w.durationMillis)
                        .put("points", pts)
                )
            }
            return arr.toString()
        }
    }
}
