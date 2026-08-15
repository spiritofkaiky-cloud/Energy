package com.energy.app.data.stats

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Pure, deterministic Energy Score engine (APP_SPEC §6 / §42 — no fake
 * intelligence). Fully unit-testable: no Android dependencies.
 */
object EnergyScoreEngine {

    /** Category thresholds from the product spec. */
    fun categoryFor(value: Int): String = when {
        value >= 85 -> "Excellent"
        value >= 70 -> "Good"
        value >= 60 -> "Fair"
        else -> "Recover"
    }

    fun compute(
        steps: Int,
        stepGoal: Int,
        workoutMinutes: Double,
        workoutKm: Double,
        pathKm: Double,
        history: Map<String, Int>,
        today: String,
        todayLoadMinutes: Double,
        recentLoadMinutes: Double
    ): EnergyScore {
        val stepsFactor = ScoreFactor(
            "Steps",
            ((steps.toDouble() / stepGoal).coerceAtMost(1.0) * 40).toInt(),
            40,
            "%,d of %,d step goal".format(Locale.US, steps, stepGoal)
        )
        val minutesFactor = ScoreFactor(
            "Workout minutes",
            ((workoutMinutes / 45.0).coerceAtMost(1.0) * 30).toInt(),
            30,
            if (workoutMinutes > 0) "%.0f min of workouts today".format(Locale.US, workoutMinutes)
            else "No workouts logged yet today"
        )
        val distanceFactor = ScoreFactor(
            "Distance",
            (((workoutKm + pathKm) / 8.0).coerceAtMost(1.0) * 20).toInt(),
            20,
            "%.1f km today (workouts + movement)".format(Locale.US, workoutKm + pathKm)
        )

        val recent = lastNDays(7, today).mapNotNull { history[it] }
        val baseline = if (recent.isNotEmpty()) recent.average() else null
        var recoveryAdjust = 0
        var recoveryNote: String? = null
        if (baseline != null && recentLoadMinutes > 0) {
            recoveryAdjust = when {
                todayLoadMinutes > recentLoadMinutes * 1.4 && todayLoadMinutes > 20 ->
                    -10.also { recoveryNote = "Today's load is well above your recent baseline" }
                todayLoadMinutes < recentLoadMinutes * 0.4 ->
                    +5.also { recoveryNote = "Lighter day than usual — room to move" }
                else -> 0
            }
        }

        val raw = stepsFactor.points + minutesFactor.points + distanceFactor.points
        val value = (raw + recoveryAdjust).coerceIn(0, 100)
        val trend = baseline?.let { value - it.toInt() }

        val factors = if (recoveryAdjust != 0) {
            listOf(
                stepsFactor, minutesFactor, distanceFactor,
                ScoreFactor("Recovery", recoveryAdjust, 10, recoveryNote ?: "Recent activity load")
            )
        } else listOf(stepsFactor, minutesFactor, distanceFactor)

        return EnergyScore(
            value = value,
            category = categoryFor(value),
            trendVs7Day = trend,
            factors = factors,
            recommendation = recommend(
                steps = steps, stepGoal = stepGoal,
                workoutCount = if (workoutMinutes > 0) 1 else 0,
                todayLoad = todayLoadMinutes, recentLoad = recentLoadMinutes,
                hasMovement = pathKm > 0
            )
        )
    }

    /** Rule-based, explainable daily recommendation. */
    fun recommend(
        steps: Int,
        stepGoal: Int,
        workoutCount: Int,
        todayLoad: Double,
        recentLoad: Double,
        hasMovement: Boolean
    ): Recommendation {
        val noData = steps == 0 && workoutCount == 0 && !hasMovement
        if (noData) {
            return Recommendation(
                "You've been quiet today. A 10-minute walk is a great way to start.",
                "Based on: no steps, workouts or movement recorded today."
            )
        }
        if (workoutCount == 0) {
            return when {
                recentLoad > 60 -> Recommendation(
                    "Recovery-focused day: your recent volume is above your baseline. Rest or go easy.",
                    "Based on: high activity over the last 7 days and no workout yet today."
                )
                stepGoal > 0 && steps >= stepGoal * 0.8 -> Recommendation(
                    "You're close to your step goal — a short walk seals it.",
                    "Based on: steps at ${steps * 100 / stepGoal}% of your goal."
                )
                else -> Recommendation(
                    "Good day for a workout — you're rested relative to recent activity.",
                    "Based on: moderate recent activity and no workout yet today."
                )
            }
        }
        return when {
            todayLoad > recentLoad * 1.4 && recentLoad > 0 -> Recommendation(
                "Nice work. Your volume today is above your baseline — consider an easy day tomorrow.",
                "Based on: today's activity vs your 7-day average."
            )
            else -> Recommendation(
                "Workout logged — keep the momentum, and stay hydrated.",
                "Based on: $workoutCount workout(s) today within your usual range."
            )
        }
    }

    /** Date keys ("yyyy-MM-dd") for the N days before [today] (oldest first). */
    fun lastNDays(n: Int, today: String): List<String> {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        cal.time = fmt.parse(today)!!
        return (1..n).map {
            cal.add(Calendar.DAY_OF_YEAR, -1)
            fmt.format(cal.time)
        }.reversed()
    }
}
