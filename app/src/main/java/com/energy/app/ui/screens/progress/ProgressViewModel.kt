package com.energy.app.ui.screens.progress

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.energy.app.EnergyApplication
import com.energy.app.data.stats.LifetimeStats
import com.energy.app.data.stats.LifetimeStatsCalculator
import com.energy.app.data.workout.PersonalRecord
import com.energy.app.data.workout.PersonalRecords
import com.energy.app.data.workout.WorkoutType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DayBucket(val label: String, val km: Double, val minutes: Long, val score: Int?)
data class MonthBucket(val label: String, val km: Double)

data class ProgressData(
    val days14: List<DayBucket> = emptyList(),
    val days30: List<DayBucket> = emptyList(),
    val months12: List<MonthBucket> = emptyList(),
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
 * PROGRESS (§23) — one major chart at a time (WEEK / MONTH / YEAR),
 * with the summary numbers above it and records/consistency below.
 */
class ProgressViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as EnergyApplication).container
    private val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val monthFmt = SimpleDateFormat("yyyy-MM", Locale.US)

    private val _data = MutableStateFlow(ProgressData())
    val data: StateFlow<ProgressData> = _data.asStateFlow()

    init {
        viewModelScope.launch {
            val workouts = container.workoutRepository.workouts.first()
            val scoreHistory = container.statsRepository.scoreHistory.first()
            val cal = Calendar.getInstance()

            // Last 14 days
            val buckets14 = (0 until 14).map { i ->
                val key = dayFmt.format(cal.time)
                cal.add(Calendar.DAY_OF_YEAR, -1)
                val ws = workouts.filter { dayFmt.format(it.startMillis) == key }
                DayBucket(
                    label = SimpleDateFormat("E", Locale.US).format(
                        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(key)!!
                    ),
                    km = ws.sumOf { it.distanceMeters } / 1000.0,
                    minutes = ws.sumOf { it.durationMillis } / 60_000L,
                    score = scoreHistory[key]
                )
            }.reversed()

            // Last 30 days
            val cal30 = Calendar.getInstance()
            val buckets30 = (0 until 30).map { i ->
                val key = dayFmt.format(cal30.time)
                cal30.add(Calendar.DAY_OF_YEAR, -1)
                val ws = workouts.filter { dayFmt.format(it.startMillis) == key }
                DayBucket(
                    label = "",
                    km = ws.sumOf { it.distanceMeters } / 1000.0,
                    minutes = ws.sumOf { it.durationMillis } / 60_000L,
                    score = scoreHistory[key]
                )
            }.reversed()

            // Last 12 months
            val calM = Calendar.getInstance()
            calM.set(Calendar.DAY_OF_MONTH, 1)
            val months = (0 until 12).map { i ->
                val key = monthFmt.format(calM.time)
                calM.add(Calendar.MONTH, -1)
                val ws = workouts.filter { monthFmt.format(it.startMillis) == key }
                MonthBucket(
                    label = SimpleDateFormat("MMM", Locale.US).format(
                        SimpleDateFormat("yyyy-MM", Locale.US).parse(key)!!
                    ),
                    km = ws.sumOf { it.distanceMeters } / 1000.0
                )
            }.reversed()

            val trend = buckets14.mapNotNull { b ->
                b.score?.let { b.label to it }
            }

            val cutoff14 = System.currentTimeMillis() - 14 * 86_400_000L
            val recent = workouts.filter { it.startMillis >= cutoff14 }
            val paceWorkouts = recent.filter {
                it.type == WorkoutType.RUN &&
                    it.distanceMeters > 500 &&
                    it.durationMillis > 60_000 &&
                    it.avgPaceMinPerKm in 0.5..120.0
            }

            val bestMonth = months.maxByOrNull { it.km }

            _data.value = ProgressData(
                days14 = buckets14,
                days30 = buckets30,
                months12 = months,
                scoreTrend = trend,
                lifetime = LifetimeStatsCalculator.compute(workouts),
                records = PersonalRecords.allRecords(workouts),
                activeDays14 = buckets14.count { it.km > 0 || it.minutes > 0 || it.score != null },
                workouts14 = recent.size,
                km14 = recent.sumOf { it.distanceMeters } / 1000.0,
                minutes14 = recent.sumOf { it.durationMillis } / 60_000L,
                bestMonthLabel = bestMonth?.label,
                bestMonthKm = bestMonth?.km ?: 0.0,
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
