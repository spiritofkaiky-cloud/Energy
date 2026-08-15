package com.energy.app.data.workout

import org.json.JSONArray
import org.json.JSONObject

/**
 * Versioned workout-metadata codec (APP_SPEC §34: schema versioning,
 * tolerant decoding). Pure JSON in/out — no Android — so malformed-input
 * behavior is covered by unit tests.
 *
 * Decoding is per-item tolerant: one corrupt workout is skipped, the rest
 * survive. Wrong schema version → null (caller falls back to backup).
 */
object WorkoutMetaCodec {

    const val VERSION = 1

    fun encode(workouts: List<SavedWorkout>): String {
        val root = JSONObject().put("v", VERSION)
        val arr = JSONArray()
        workouts.forEach { w ->
            arr.put(
                JSONObject()
                    .put("id", w.id)
                    .put("type", w.type.name)
                    .put("start", w.startMillis)
                    .put("end", w.endMillis)
                    .put("distance", w.distanceMeters)
                    .put("duration", w.durationMillis)
                    .put("calories", w.calories)
                    .put("elevation", w.elevationGainMeters)
                    .apply { w.avgHeartRateBpm?.let { put("avgHr", it) } }
                    .apply { w.maxHeartRateBpm?.let { put("maxHr", it) } }
                    .put("sync", w.syncState.name)
            )
        }
        return root.put("items", arr).toString()
    }

    /** @return null when the payload is not valid v1 metadata (corrupt). */
    fun decode(json: String): List<SavedWorkout>? {
        if (json.isBlank()) return emptyList()
        return try {
            val root = JSONObject(json)
            if (root.optInt("v", 0) != VERSION) return null
            val arr = root.getJSONArray("items")
            (0 until arr.length()).mapNotNull { i ->
                runCatching {
                    val o = arr.getJSONObject(i)
                    SavedWorkout(
                        id = o.getString("id"),
                        type = WorkoutType.valueOf(o.getString("type")),
                        startMillis = o.getLong("start"),
                        endMillis = o.getLong("end"),
                        distanceMeters = o.getDouble("distance"),
                        durationMillis = o.getLong("duration"),
                        points = emptyList(),
                        calories = o.optInt("calories", 0),
                        elevationGainMeters = o.optDouble("elevation", 0.0),
                        avgHeartRateBpm = if (o.has("avgHr")) o.getInt("avgHr") else null,
                        maxHeartRateBpm = if (o.has("maxHr")) o.getInt("maxHr") else null,
                        syncState = runCatching {
                            SyncState.valueOf(o.optString("sync", SyncState.PENDING.name))
                        }.getOrDefault(SyncState.PENDING)
                    )
                }.getOrNull()
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Points-file codec ({"v":1,"points":[...]}) — also tolerant per point. */
    fun encodePoints(points: List<WorkoutPoint>): String {
        val root = JSONObject().put("v", VERSION)
        val arr = JSONArray()
        points.forEach { p ->
            arr.put(
                JSONObject()
                    .put("lat", p.lat).put("lng", p.lng).put("t", p.timeMillis).put("s", p.speedKmh)
                    .apply { p.alt?.let { put("a", it) } }
            )
        }
        return root.put("points", arr).toString()
    }

    fun decodePoints(json: String): List<WorkoutPoint>? {
        return try {
            val root = JSONObject(json)
            if (root.optInt("v", 0) != VERSION) return null
            val arr = root.getJSONArray("points")
            (0 until arr.length()).mapNotNull { i ->
                runCatching {
                    val o = arr.getJSONObject(i)
                    WorkoutPoint(
                        lat = o.getDouble("lat"),
                        lng = o.getDouble("lng"),
                        timeMillis = o.getLong("t"),
                        speedKmh = o.getDouble("s"),
                        alt = if (o.has("a")) o.getDouble("a") else null
                    )
                }.getOrNull()
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Legacy v0.4 blob (workouts with inline points) → SavedWorkout list. */
    fun decodeLegacy(json: String): List<SavedWorkout> = try {
        val arr = JSONArray(json)
        (0 until arr.length()).mapNotNull { i ->
            runCatching {
                val o = arr.getJSONObject(i)
                val pts = o.getJSONArray("points")
                val points = (0 until pts.length()).map { j ->
                    val p = pts.getJSONObject(j)
                    WorkoutPoint(
                        p.getDouble("lat"), p.getDouble("lng"),
                        p.getLong("t"), p.getDouble("s")
                    )
                }
                val type = WorkoutType.valueOf(o.getString("type"))
                SavedWorkout(
                    id = o.getString("id"),
                    type = type,
                    startMillis = o.getLong("start"),
                    endMillis = o.getLong("end"),
                    distanceMeters = o.getDouble("distance"),
                    durationMillis = o.getLong("duration"),
                    points = points,
                    calories = WorkoutMath.calories(type, o.getLong("duration"))
                )
            }.getOrNull()
        }
    } catch (e: Exception) {
        emptyList()
    }
}
