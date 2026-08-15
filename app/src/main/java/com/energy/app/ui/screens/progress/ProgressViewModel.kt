package com.energy.app.ui.screens.progress

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.energy.app.EnergyApplication
import com.energy.app.data.stats.LifetimeStats
import com.energy.app.data.stats.LifetimeStatsCalculator
import com.energy.app.data.workout.PersonalRecords
import com.energy.app.data.workout.PersonalRecord
import com.energy.app.data.workout.SavedWorkout
import com.energy.app.data.workout.WorkoutMath
import com.energy.app.data.workout.WorkoutType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DayBucket(val label: String, val km: Double, val minutes: Long, val score: Int?)

data class ProgressData(
    val days: List<DayBucket> = emptyList(),
    val scoreTrend: List<Pair<String, Int>> = emptyList(),
    val lifetime: LifetimeStats = LifetimeStats(),
    val records: List<PersonalRecord> = emptyList(),
    val activeDays14: Int = 0,
    val workouts14: Int = 0,
    val km14: Double = 0.0,
    val minutes14: Long = 0L,
    val bestMonthLabel: String? = null,
    val bestMonthKm: Double = 0.0,
    val avgPace14: Double? = null
)

/**
 * Progress tab ("Am I improving?" — APP_SPEC §7): last-14-days activity,
 * Energy Score trend, consistency, personal records, lifetime totals.
 * All charts are honest aggregations of saved workouts + score history.
 */
class ProgressViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as EnergyApplication).container
    private val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val _data = MutableStateFlow(ProgressData())
    val data: StateFlow<ProgressData> = _data.asStateFlow()

    init {
        viewModelScope.launch {
            val workouts = container.workoutRepository.workouts.first()
            val scoreHistory = container.statsRepository.scoreHistory.first()
            val activeDays = container.statsRepository.activeDays.first()

            val cal = Calendar.getInstance()
            val buckets = (0 until 14).map { i ->
                val key = dayFmt.format(cal.time)
                cal.add(Calendar.DAY_OF_YEAR, -1)
                val ws = workouts.filter { dayFmt.format(Date(it.startMillis)) == key }
                DayBucket(
                    label = SimpleDateFormat("E", Locale.US).format(
                        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(key)!!
                    ),
                    km = ws.sumOf { it.distanceMeters } / 1000.0,
                    minutes = ws.sumOf { it.durationMillis } / 60_000L,
                    score = scoreHistory[key]
                )
            }.reversed()

            // Score trend (only days that have a recorded score).
            val trend = buckets.mapNotNull { b ->
                b.score?.let { b.label to it }
            }

            val cutoff14 = System.currentTimeMillis() - 14 * 86_400_000L
            val recent = workouts.filter { it.startMillis >= cutoff14 }
            // Only physiologically plausible runs count toward pace stats —
            // degenerate workouts (test artifacts, GPS-less sessions) are
            // excluded rather than averaged in.
            val paceWorkouts = recent.filter {
                it.type == WorkoutType.RUN &&
                    it.distanceMeters > 500 &&
                    it.durationMillis > 60_000 &&
                    it.avgPaceMinPerKm in 0.5..120.0
            }

            // Best month.
            val byMonth = workouts.groupBy {
                SimpleDateFormat("yyyy-MM", Locale.US).format(Date(it.startMillis))
            }.mapValues { (_, ws) -> ws.sumOf { it.distanceMeters } / 1000.0 }
            val bestMonth = byMonth.maxByOrNull { it.value }

            _data.value = ProgressData(
                days = buckets,
                scoreTrend = trend,
                lifetime = LifetimeStatsCalculator.compute(workouts),
                records = PersonalRecords.allRecords(workouts),
                activeDays14 = buckets.count { it.km > 0 || it.minutes > 0 || it.score != null },
                workouts14 = recent.size,
                km14 = recent.sumOf { it.distanceMeters } / 1000.0,
                minutes14 = recent.sumOf { it.durationMillis } / 60_000L,
                bestMonthLabel = bestMonth?.key,
                bestMonthKm = bestMonth?.value ?: 0.0,
                avgPace14 = paceWorkouts.map { it.avgPaceMinPerKm }
                    .filter { it > 0 }.average().takeIf { it.isFinite() && it > 0 }
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val workouts = container.workoutRepository.workouts.first()
            _data.value = _data.value.copy(lifetime = LifetimeStatsCalculator.compute(workouts))
        }
    }
}
