package com.energy.app.data.workout

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Personal records (APP_SPEC §12) — deterministic, pure, testable.
 * All records are computed from saved workouts only; nothing is invented.
 */

data class PersonalRecord(
    val key: String,
    val label: String,
    val valueText: String,
    val workoutId: String,
    val atMillis: Long
)

/** One plain-English insight generated strictly from real data. */
data class WorkoutInsight(val emoji: String, val text: String)

object PersonalRecords {

    private const val FASTEST_1K = "fastest_1k"
    private const val FASTEST_1M = "fastest_1m"
    private const val FASTEST_5K = "fastest_5k"
    private const val LONGEST_DISTANCE = "longest_distance"
    private const val LONGEST_TIME = "longest_time"
    private const val BEST_DAY = "best_day"

    /** All records across the whole history. */
    fun allRecords(workouts: List<SavedWorkout>): List<PersonalRecord> {
        val result = mutableListOf<PersonalRecord>()
        if (workouts.isEmpty()) return result

        val runs = workouts.filter { it.type == WorkoutType.RUN }
        bestEffort(runs, 1_000.0, FASTEST_1K, "Fastest 1 km", ::formatPaceTime)?.let(result::add)
        bestEffort(runs, 1_609.344, FASTEST_1M, "Fastest 1 mile", ::formatPaceTime)?.let(result::add)
        bestEffort(runs, 5_000.0, FASTEST_5K, "Fastest 5 km", ::formatPaceTime)?.let(result::add)

        workouts.maxByOrNull { it.distanceMeters }?.let { w ->
            result += PersonalRecord(
                LONGEST_DISTANCE, "Longest workout",
                WorkoutMath.formatDistance(w.distanceMeters), w.id, w.startMillis
            )
        }
        workouts.maxByOrNull { it.durationMillis }?.let { w ->
            result += PersonalRecord(
                LONGEST_TIME, "Longest duration",
                WorkoutMath.formatDuration(w.durationMillis), w.id, w.startMillis
            )
        }

        // Most distance in one day.
        val byDay = workouts.groupBy { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it.startMillis)) }
            .mapValues { (_, ws) -> ws.sumOf { it.distanceMeters } }
        byDay.maxByOrNull { it.value }?.let { (day, meters) ->
            val dayWorkout = workouts.first { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it.startMillis)) == day }
            result += PersonalRecord(
                BEST_DAY, "Most active day",
                WorkoutMath.formatDistance(meters), dayWorkout.id, dayWorkout.startMillis
            )
        }
        return result
    }

    /** Records that THIS workout newly set (for the celebration overlay). */
    fun newRecordsFor(all: List<SavedWorkout>, workout: SavedWorkout): List<PersonalRecord> {
        val with = allRecords(all)
        val without = allRecords(all.filter { it.id != workout.id })
        val before = without.associateBy { it.key + "|" + it.valueText }
        return with.filter { r ->
            r.workoutId == workout.id && (r.key + "|" + r.valueText) !in before
        }
    }

    /**
     * Best rolling effort over [targetMeters] within a workout's trail:
     * minimizes time between two cumulative-distance points at least
     * [targetMeters] apart (two-pointer scan).
     */
    fun bestEffortSeconds(points: List<WorkoutPoint>, targetMeters: Double): Double? {
        if (points.size < 2) return null
        val cum = WorkoutMath.cumulativeDistanceTime(points)
        if (cum.last().first < targetMeters) return null
        var best: Double? = null
        var i = 0
        for (j in cum.indices) {
            while (i < j && cum[j].first - cum[i].first >= targetMeters) {
                val seconds = (cum[j].second - cum[i].second) / 1000.0
                best = minOf(best ?: Double.MAX_VALUE, seconds)
                i++
            }
        }
        return best
    }

    private fun bestEffort(
        workouts: List<SavedWorkout>,
        targetMeters: Double,
        key: String,
        label: String,
        format: (Double) -> String
    ): PersonalRecord? {
        var best: Pair<SavedWorkout, Double>? = null
        for (w in workouts) {
            val secs = bestEffortSeconds(w.points, targetMeters) ?: continue
            if (best == null || secs < best!!.second) best = w to secs
        }
        return best?.let { (w, secs) ->
            PersonalRecord(key, label, format(secs), w.id, w.startMillis)
        }
    }

    private fun formatPaceTime(seconds: Double): String {
        val m = (seconds / 60).toInt()
        val s = (seconds - m * 60).toInt()
        return String.format(Locale.US, "%d:%02d", m, s)
    }
}

object WorkoutInsights {

    /** Insights comparing one workout against the user's own recent history. */
    fun generate(all: List<SavedWorkout>, workout: SavedWorkout): List<WorkoutInsight> {
        val insights = mutableListOf<WorkoutInsight>()
        val sameType = all.filter {
            it.type == workout.type && it.id != workout.id
        }
        val recent = sameType.filter {
            it.startMillis >= workout.startMillis - 28 * 86_400_000L &&
                it.startMillis < workout.startMillis
        }

        if (recent.isNotEmpty()) {
            val avgPace = recent.filter { it.distanceMeters > 100 }
                .map { it.avgPaceMinPerKm }
                .filter { it > 0 }
                .average()
            if (avgPace > 0 && workout.distanceMeters > 100) {
                val delta = (avgPace - workout.avgPaceMinPerKm) / avgPace * 100
                if (delta >= 3) {
                    insights += WorkoutInsight(
                        "⚡",
                        "Your pace was %.0f%% faster than your recent average.".format(delta)
                    )
                } else if (delta <= -3) {
                    insights += WorkoutInsight(
                        "🌿",
                        "A steadier outing — %.0f%% slower than your recent average.".format(-delta)
                    )
                } else {
                    insights += WorkoutInsight(
                        "🎯", "Right on your recent pace — consistent training."
                    )
                }
            }
            val longest = recent.maxOfOrNull { it.distanceMeters } ?: 0.0
            if (workout.distanceMeters > longest && longest > 0) {
                insights += WorkoutInsight(
                    "📏", "Your longest ${workout.type.label.lowercase()} of the month."
                )
            }
            val monthDistance = recent.sumOf { it.distanceMeters } + workout.distanceMeters
            if (recent.size >= 3) {
                insights += WorkoutInsight(
                    "📅",
                    "That's ${recent.size + 1} ${workout.type.label.lowercase()}s — %.1f km in the last month."
                        .format(monthDistance / 1000.0)
                )
            }
        } else {
            insights += WorkoutInsight(
                "🚀", "Your first ${workout.type.label.lowercase()} on Energy — welcome to the club."
            )
        }

        // Pace stability across splits.
        if (workout.type == WorkoutType.RUN || workout.type == WorkoutType.WALK) {
            val splits = WorkoutMath.splits(WorkoutMath.cumulativeDistanceTime(workout.points))
            if (splits.size >= 2) {
                val avg = splits.average()
                val spread = splits.map { kotlin.math.abs(it - avg) }.average()
                if (spread / avg <= 0.08) {
                    insights += WorkoutInsight("🧘", "You maintained a stable pace — even splits.")
                }
            }
        }

        // Longest activity of the day.
        val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val day = dayFmt.format(Date(workout.startMillis))
        val thatDay = all.filter { dayFmt.format(Date(it.startMillis)) == day && it.id != workout.id }
        if (thatDay.isEmpty() && all.isNotEmpty()) {
            insights += WorkoutInsight("☀️", "The only workout logged today — that counts double.")
        }
        return insights.take(3)
    }

    /** Calendar day keys for the last [days] days (for charts). */
    fun lastDayKeys(days: Int): List<String> {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        return (0 until days).map { i ->
            cal.add(Calendar.DAY_OF_YEAR, -1)
            fmt.format(cal.time)
        }.reversed()
    }
}
