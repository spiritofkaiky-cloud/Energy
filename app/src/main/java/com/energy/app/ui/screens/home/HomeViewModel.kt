package com.energy.app.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.energy.app.EnergyApplication
import com.energy.app.data.health.DailyHealth
import com.energy.app.data.health.HealthRepository
import com.energy.app.data.location.DayPoint
import com.energy.app.data.location.LocationTracker
import com.energy.app.data.settings.UserPreferences
import com.energy.app.data.stats.EnergyScore
import com.energy.app.data.stats.LifetimeStats
import com.energy.app.data.stats.LifetimeStatsCalculator
import com.energy.app.data.stats.StatsRepository
import com.energy.app.data.workout.SavedWorkout
import com.energy.app.data.workout.WorkoutState
import com.energy.app.data.workout.WorkoutType
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Ring progress derived from real signals (never hardcoded). */
data class RingsData(
    val move: Float,       // active kcal vs calorie goal
    val moveDetail: String,
    val exercise: Float,   // workout minutes vs 30
    val exerciseDetail: String,
    val stand: Float,      // active hours vs 12
    val standDetail: String
)

data class TodayStats(
    val steps: Int = 0,
    val distanceKm: Double = 0.0,
    val activeCalories: Int = 0,
    val workoutMinutes: Long = 0L,
    val heartRateBpm: Int? = null
)

@OptIn(FlowPreview::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as EnergyApplication).container
    private val tracker: LocationTracker = container.locationTracker
    private val health: HealthRepository = container.healthRepository
    private val stats: StatsRepository = container.statsRepository

    val points: StateFlow<List<DayPoint>> = tracker.points
    val tracking: StateFlow<Boolean> = tracker.tracking
    val dailyHealth: StateFlow<DailyHealth?> = health.daily
    val healthAvailable: Boolean = health.available
    val score: StateFlow<EnergyScore> = stats.score
    val streak: StateFlow<Int> = stats.streak
    val activeWorkout: StateFlow<Boolean> =
        container.workoutSession.state.map { it != WorkoutState.IDLE }
            .let { flow ->
                MutableStateFlow(container.workoutSession.state.value != WorkoutState.IDLE)
                    .also { s -> viewModelScope.launch { flow.collect { s.value = it } } }
            }

    val workoutState: StateFlow<WorkoutState> = container.workoutSession.state
    val workoutType: StateFlow<WorkoutType> = container.workoutSession.type

    private val _userName = MutableStateFlow("Runner")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _workouts = MutableStateFlow<List<SavedWorkout>>(emptyList())
    val workouts: StateFlow<List<SavedWorkout>> = _workouts.asStateFlow()

    private val _prefs = MutableStateFlow(UserPreferences())
    val prefs: StateFlow<UserPreferences> = _prefs.asStateFlow()

    private val _rings = MutableStateFlow(RingsData(0f, "", 0f, "", 0f, ""))
    val rings: StateFlow<RingsData> = _rings.asStateFlow()

    private val _today = MutableStateFlow(TodayStats())
    val today: StateFlow<TodayStats> = _today.asStateFlow()

    private val _lifetime = MutableStateFlow(LifetimeStats())
    val lifetime: StateFlow<LifetimeStats> = _lifetime.asStateFlow()

    private val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    init {
        viewModelScope.launch {
            health.refreshToday()
        }
        viewModelScope.launch {
            container.authRepository.currentUser.collect { user ->
                _userName.value = user?.name?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() }
                    ?: "Runner"
            }
        }

        // Reactive refresh: recompute score/rings/stats whenever any signal
        // changes (debounced so GPS fixes don't thrash DataStore).
        viewModelScope.launch {
            var emission = 0
            combine(
                tracker.points,
                container.workoutRepository.workouts,
                health.daily,
                container.settingsRepository.preferences
            ) { pts, ws, h, p -> Quad(pts, ws, h, p) }
                .debounce {
                    val isFirst = emission == 0
                    emission++
                    if (isFirst) 0L else 15_000L
                }
                .collect { (pts, ws, h, p) ->
                    _workouts.value = ws
                    _prefs.value = p
                    val today = dayFmt.format(Date())
                    val todayWorkouts = ws.filter {
                        dayFmt.format(Date(it.startMillis)) == today
                    }
                    stats.refresh(pts, ws, h, p)
                    computeRings(todayWorkouts, pts, h, p)
                    computeToday(todayWorkouts, pts, h)
                    _lifetime.value = LifetimeStatsCalculator.compute(ws)
                }
        }

        // Refresh daily health every 10 minutes while Home is alive.
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(10 * 60_000L)
                health.refreshToday()
            }
        }
    }

    private data class Quad(
        val points: List<DayPoint>,
        val workouts: List<SavedWorkout>,
        val health: DailyHealth?,
        val prefs: UserPreferences
    )

    private fun computeRings(
        todayWorkouts: List<SavedWorkout>,
        points: List<DayPoint>,
        health: DailyHealth?,
        prefs: UserPreferences
    ) {
        val steps = health?.steps ?: 0
        val workoutCalories = todayWorkouts.sumOf { it.calories }
        // Walking estimate: ~0.04 kcal/step at moderate pace (explainable).
        val activeCalories = workoutCalories + (steps * 0.04).toInt()
        val workoutMinutes = todayWorkouts.sumOf { it.durationMillis } / 60_000L

        val activeHours = points.map { Calendar.getInstance().apply {
            timeInMillis = it.timeMillis
        }.get(Calendar.HOUR_OF_DAY) }.toSet().size

        _rings.value = RingsData(
            move = (activeCalories.toDouble() / prefs.calorieGoal).coerceIn(0.0, 1.0).toFloat(),
            moveDetail = "$activeCalories of ${prefs.calorieGoal} kcal",
            exercise = (workoutMinutes / 30.0).coerceIn(0.0, 1.0).toFloat(),
            exerciseDetail = "$workoutMinutes of 30 min",
            stand = (activeHours / 12.0).coerceIn(0.0, 1.0).toFloat(),
            standDetail = "$activeHours of 12 hours"
        )
    }

    private fun computeToday(
        todayWorkouts: List<SavedWorkout>,
        points: List<DayPoint>,
        health: DailyHealth?
    ) {
        val steps = health?.steps ?: 0
        val workoutKm = todayWorkouts.sumOf { it.distanceMeters } / 1000.0
        val pathKm = StatsRepository.dayPathDistanceKm(points)
        _today.value = TodayStats(
            steps = steps,
            distanceKm = workoutKm + pathKm,
            activeCalories = todayWorkouts.sumOf { it.calories } + (steps * 0.04).toInt(),
            workoutMinutes = todayWorkouts.sumOf { it.durationMillis } / 60_000L,
            heartRateBpm = health?.avgHeartRateBpm
        )
    }

    suspend fun refreshStats() {
        health.refreshToday()
        val ws = container.workoutRepository.workouts.first()
        stats.refresh(tracker.points.value, ws, health.daily.value, _prefs.value)
    }

    fun startTracking() = tracker.start()

    fun stopTracking() = tracker.stop()
}
