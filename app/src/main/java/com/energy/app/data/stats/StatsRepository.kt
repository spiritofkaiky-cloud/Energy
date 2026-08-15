package com.energy.app.data.stats

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.energy.app.data.health.DailyHealth
import com.energy.app.data.location.DayPoint
import com.energy.app.data.workout.SavedWorkout
import com.energy.app.data.workout.WorkoutType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class EnergyScore(val value: Int) {
    val message: String get() = when {
        value >= 85 -> "Ready to crush it"
        value >= 70 -> "Solid day in motion"
        value >= 50 -> "Keep the engine warm"
        else -> "A little movement goes far"
    }
}

/**
 * Oura-style daily Energy Score + activity streak (APP_SPEC §4.5 / M6).
 * Score = steps (0-50) + workout distance (0-30) + day-path distance (0-20),
 * capped at 100. Streak = consecutive active days (tracked points or a saved
 * workout), with milestones as achievements.
 */
class StatsRepository(private val context: Context) {

    private object Keys {
        val ACTIVE_DAYS = stringSetPreferencesKey("active_days")
    }

    private val Context.statsStore by preferencesDataStore(name = "energy_stats")

    private val _score = MutableStateFlow(EnergyScore(0))
    val score: StateFlow<EnergyScore> = _score.asStateFlow()

    private val _streak = MutableStateFlow(0)
    val streak: StateFlow<Int> = _streak.asStateFlow()

    val activeDays: Flow<Set<String>> = context.statsStore.data.map { it[Keys.ACTIVE_DAYS] ?: emptySet() }

    private val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /** Recompute score + streak from today's signals. */
    suspend fun refresh(
        dayPoints: List<DayPoint>,
        workouts: List<SavedWorkout>,
        health: DailyHealth?
    ) {
        val today = dayFmt.format(Date())
        markActive(today, dayPoints.isNotEmpty() || workouts.any {
            dayFmt.format(Date(it.startMillis)) == today
        })

        // Score components
        val stepsScore = ((health?.steps ?: 0) / 10_000.0).coerceAtMost(1.0) * 50.0
        val todayWorkoutKm = workouts
            .filter { dayFmt.format(Date(it.startMillis)) == today }
            .sumOf { it.distanceMeters } / 1000.0
        val workoutScore = (todayWorkoutKm / 10.0).coerceAtMost(1.0) * 30.0
        val dayPathKm = dayPathDistanceKm(dayPoints)
        val pathScore = (dayPathKm / 8.0).coerceAtMost(1.0) * 20.0

        _score.value = EnergyScore((stepsScore + workoutScore + pathScore).toInt().coerceIn(0, 100))

        // Streak over the last 60 days
        val active = activeDaysOnce()
        val cal = Calendar.getInstance()
        var streak = 0
        while (true) {
            if (dayFmt.format(cal.time) in active) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else break
        }
        _streak.value = streak
    }

    suspend fun markActive(date: String, active: Boolean) {
        if (!active) return
        context.statsStore.edit { prefs ->
            prefs[Keys.ACTIVE_DAYS] = (prefs[Keys.ACTIVE_DAYS] ?: emptySet()) + date
        }
    }

    suspend fun activeDaysOnce(): Set<String> = activeDays.first()

    companion object {
        /** Approximate haversine distance of a point trail (km). */
        fun dayPathDistanceKm(points: List<DayPoint>): Double {
            if (points.size < 2) return 0.0
            var km = 0.0
            for (i in 1 until points.size) {
                km += haversineKm(
                    points[i - 1].lat, points[i - 1].lng,
                    points[i].lat, points[i].lng
                )
            }
            return km
        }

        fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
            val r = 6371.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLng = Math.toRadians(lng2 - lng1)
            val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
            return 2 * r * Math.asin(Math.sqrt(a))
        }
    }
}

/** Achievement badges from streak milestones (M6). */
data class Achievement(val emoji: String, val label: String, val days: Int)

val Achievements = listOf(
    Achievement("🔥", "3-day streak", 3),
    Achievement("⚡", "Week warrior", 7),
    Achievement("💪", "Two weeks strong", 14),
    Achievement("🏆", "Monthly legend", 30)
)
