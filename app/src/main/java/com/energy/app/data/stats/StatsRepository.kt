package com.energy.app.data.stats

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.energy.app.data.health.DailyHealth
import com.energy.app.data.location.DayPoint
import com.energy.app.data.settings.UserPreferences
import com.energy.app.data.workout.SavedWorkout
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

/** One explainable ingredient of the score (APP_SPEC — no fake "AI"). */
data class ScoreFactor(val label: String, val points: Int, val maxPoints: Int, val detail: String)

/** Transparent rule-based recommendation + what it is based on. */
data class Recommendation(val text: String, val basis: String)

data class EnergyScore(
    val value: Int,
    val category: String,
    val trendVs7Day: Int?,
    val factors: List<ScoreFactor>,
    val recommendation: Recommendation
)

/**
 * Oura-inspired daily Energy Score (APP_SPEC §6) — but every point is
 * explainable, and the score is clearly an estimate, not a physiological
 * measurement.
 *
 *  Steps            → up to 40 pts (vs user step goal)
 *  Workout minutes  → up to 30 pts (45 min of workouts = full)
 *  Distance         → up to 20 pts (8 km across workouts + movement = full)
 *  Recovery adjust  → ±10 pts (recent load vs the user's own 7-day baseline)
 *
 * Trend = today vs the mean of the previous 7 recorded days.
 */
class StatsRepository(private val context: Context) {

    private object Keys {
        val ACTIVE_DAYS = stringSetPreferencesKey("active_days")
        val SCORE_HISTORY = stringSetPreferencesKey("score_history") // "yyyy-MM-dd:NN"
    }

    private val Context.statsStore by preferencesDataStore(name = "energy_stats")

    /** Destructive: clears score history + active days (data wipe flow). */
    suspend fun clearAll() {
        context.statsStore.edit { it.clear() }
    }

    private val _score = MutableStateFlow(
        EnergyScore(0, "—", null, emptyList(), Recommendation("", ""))
    )
    val score: StateFlow<EnergyScore> = _score.asStateFlow()

    private val _streak = MutableStateFlow(0)
    val streak: StateFlow<Int> = _streak.asStateFlow()

    val activeDays: Flow<Set<String>> = context.statsStore.data.map { it[Keys.ACTIVE_DAYS] ?: emptySet() }

    /** date("yyyy-MM-dd") → score, for trend charts. */
    val scoreHistory: Flow<Map<String, Int>> = context.statsStore.data.map { prefs ->
        (prefs[Keys.SCORE_HISTORY] ?: emptySet()).mapNotNull { entry ->
            val parts = entry.split(':')
            if (parts.size != 2) return@mapNotNull null
            parts[0] to (parts[1].toIntOrNull() ?: return@mapNotNull null)
        }.toMap()
    }

    private val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /** Recompute score + streak + recommendation from today's signals. */
    suspend fun refresh(
        dayPoints: List<DayPoint>,
        workouts: List<SavedWorkout>,
        health: DailyHealth?,
        prefs: UserPreferences
    ) {
        val today = dayFmt.format(Date())
        markActive(today, dayPoints.isNotEmpty() || workouts.any {
            dayFmt.format(Date(it.startMillis)) == today
        })

        val steps = health?.steps ?: 0
        val todayWorkouts = workouts.filter { dayFmt.format(Date(it.startMillis)) == today }
        val workoutMinutes = todayWorkouts.sumOf { it.durationMillis } / 60_000.0
        val workoutKm = todayWorkouts.sumOf { it.distanceMeters } / 1000.0
        val pathKm = dayPathDistanceKm(dayPoints)

        val history = scoreHistory.first()
        val recentLoad = recentLoadMinutes(workouts, 7)
        val todayLoad = workoutMinutes + pathKm * 12.0 // minutes-equivalent load

        val computed = EnergyScoreEngine.compute(
            steps = steps,
            stepGoal = prefs.stepGoal,
            workoutMinutes = workoutMinutes,
            workoutKm = workoutKm,
            pathKm = pathKm,
            history = history,
            today = today,
            todayLoadMinutes = todayLoad,
            recentLoadMinutes = recentLoad
        )
        _score.value = computed

        // Persist today's score for tomorrow's trend.
        context.statsStore.edit { prefsStore ->
            prefsStore[Keys.SCORE_HISTORY] =
                (prefsStore[Keys.SCORE_HISTORY] ?: emptySet())
                    .filter { !it.startsWith(today) }
                    .toMutableSet()
                    .apply { add("$today:${computed.value}") }
        }

        // Streak over the last 60 days
        val active = activeDays.first()
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

    private fun recentLoadMinutes(workouts: List<SavedWorkout>, days: Int): Double {
        val cutoff = System.currentTimeMillis() - days * 86_400_000L
        return workouts.filter { it.startMillis >= cutoff }
            .sumOf { it.durationMillis } / 60_000.0
    }

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
