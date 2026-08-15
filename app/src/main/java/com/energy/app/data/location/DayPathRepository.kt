package com.energy.app.data.location

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DayPoint(val lat: Double, val lng: Double, val timeMillis: Long)

private val Context.dayPathStore by preferencesDataStore(name = "energy_day_path")

/**
 * Stores the day's GPS breadcrumbs as JSON in DataStore (Room arrives at M4).
 * Auto-resets when the day changes.
 */
class DayPathRepository(private val context: Context) {

    private object Keys {
        val POINTS = stringPreferencesKey("points_json")
        val DAY = stringPreferencesKey("day")
    }

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    val todayPoints: Flow<List<DayPoint>> = context.dayPathStore.data.map { prefs ->
        if (prefs[Keys.DAY] != today()) emptyList() else parse(prefs[Keys.POINTS].orEmpty())
    }

    suspend fun addPoint(lat: Double, lng: Double) {
        context.dayPathStore.edit { prefs ->
            val isNewDay = prefs[Keys.DAY] != today()
            val list = if (isNewDay) emptyList() else parse(prefs[Keys.POINTS].orEmpty())
            prefs[Keys.DAY] = today()
            prefs[Keys.POINTS] = toJson(list + DayPoint(lat, lng, System.currentTimeMillis()))
        }
    }

    suspend fun clearToday() {
        context.dayPathStore.edit {
            it[Keys.DAY] = today()
            it[Keys.POINTS] = ""
        }
    }

    private fun parse(json: String): List<DayPoint> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                DayPoint(o.getDouble("lat"), o.getDouble("lng"), o.getLong("t"))
            }
        }.getOrDefault(emptyList())
    }

    private fun toJson(points: List<DayPoint>): String {
        val arr = JSONArray()
        points.forEach { p ->
            arr.put(JSONObject().put("lat", p.lat).put("lng", p.lng).put("t", p.timeMillis))
        }
        return arr.toString()
    }
}
