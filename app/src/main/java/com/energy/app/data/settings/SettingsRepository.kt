package com.energy.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.energy.app.data.workout.WorkoutType
import java.io.File

enum class Units { METRIC, IMPERIAL }
enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class Accent { ORANGE, CORAL }
enum class Haptics { FULL, REDUCED, OFF }
enum class MetricPreset { RUNNER, PERFORMANCE, MINIMAL, FULL }
enum class AnnounceInterval(val label: String) {
    KM1("Every 1 km"), KM5("Every 5 km"), MIN5("Every 5 min"), MIN10("Every 10 min")
}
enum class GpsMode(val label: String, val sub: String) {
    STANDARD("Standard", "Balanced accuracy and battery"),
    BATTERY_SAVER("Battery saver", "Fewer idle updates; workouts unaffected"),
    HIGH_ACCURACY("High accuracy", "More frequent fixes, more battery")
}
enum class RoutePrivacy(val label: String, val sub: String) {
    EXACT("Exact", "Your full route is kept"),
    APPROXIMATE("Approximate", "Start and end areas are trimmed from exports"),
    PRIVATE("Private", "Routes stay on this device only")
}
enum class FitnessLevel(val label: String) {
    BEGINNER("Beginner"), INTERMEDIATE("Intermediate"), ADVANCED("Advanced")
}

/**
 * Notification categories (§3) — stored as one tolerant JSON blob so the
 * list can grow without schema migrations.
 */
data class NotificationPrefs(
    val workoutReminder: Boolean = true,
    val workoutComplete: Boolean = true,
    val goalProgress: Boolean = true,
    val goalComplete: Boolean = true,
    val streak: Boolean = true,
    val achievements: Boolean = true,
    val recovery: Boolean = true,
    val syncIssues: Boolean = true
)

/**
 * THE single preferences model (§37). Every setting lives here with a
 * sensible default; screens never define their own keys. Corrupted values
 * fall back to defaults via tolerant parsing.
 */
data class AlarmSetting(
    val enabled: Boolean = false,
    val hour: Int = 8,
    val minute: Int = 0
)

data class UserPreferences(
    // ── Profile / body ────────────────────────────────────────────────────
    val weightKg: Int = 70,
    val heightCm: Int = 170,
    val birthdayMillis: Long? = null,
    val gender: String = "",                    // "" / "female" / "male" — optional
    val fitnessLevel: FitnessLevel = FitnessLevel.BEGINNER,
    val preferredActivity: WorkoutType = WorkoutType.RUN,
    // ── Goals ─────────────────────────────────────────────────────────────
    val stepGoal: Int = 10_000,
    val calorieGoal: Int = 500,
    // ── Workout behavior ──────────────────────────────────────────────────
    val defaultWorkoutType: WorkoutType = WorkoutType.RUN,
    val autoPause: Boolean = false,
    val countdownSeconds: Int = 3,               // 0 = start instantly
    val keepScreenAwake: Boolean = true,
    val confirmFinish: Boolean = true,
    val gpsMode: GpsMode = GpsMode.STANDARD,
    // ── Live display ──────────────────────────────────────────────────────
    val metricPreset: MetricPreset = MetricPreset.RUNNER,
    // ── Audio & coaching ──────────────────────────────────────────────────
    val audioCues: Boolean = false,
    val announceInterval: AnnounceInterval = AnnounceInterval.KM1,
    // ── Notifications / quiet hours ───────────────────────────────────────
    val notifications: NotificationPrefs = NotificationPrefs(),
    val quietHoursEnabled: Boolean = false,
    val quietStart: Int = 2200,                  // 22:00
    val quietEnd: Int = 700,                     // 07:00
    // ── Units ─────────────────────────────────────────────────────────────
    val units: Units = Units.METRIC,
    // ── Appearance ────────────────────────────────────────────────────────
    val accent: Accent = Accent.ORANGE,
    val visualEffects: Boolean = true,           // aurora + entry animations
    val haptics: Haptics = Haptics.FULL,
    // ── Home personalization ──────────────────────────────────────────────
    val homeScore: Boolean = true,
    val homeInsight: Boolean = true,
    val homeRings: Boolean = true,
    val homeStats: Boolean = true,
    val homeMap: Boolean = true,
    val homeStreak: Boolean = true,
    // ── Maps / privacy ────────────────────────────────────────────────────
    val routeColorAccent: Boolean = true,        // accent vs neutral route
    val speedColorRoute: Boolean = true,
    val routePrivacy: RoutePrivacy = RoutePrivacy.EXACT
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val WEIGHT = intPreferencesKey("weight_kg")
        val HEIGHT = intPreferencesKey("height_cm")
        val BIRTHDAY = longPreferencesKey("birthday")
        val GENDER = stringPreferencesKey("gender")
        val FITNESS_LEVEL = stringPreferencesKey("fitness_level")
        val PREFERRED_ACTIVITY = stringPreferencesKey("preferred_activity")
        val STEP_GOAL = intPreferencesKey("step_goal")
        val CALORIE_GOAL = intPreferencesKey("calorie_goal")
        val DEFAULT_TYPE = stringPreferencesKey("default_workout_type")
        val AUTO_PAUSE = booleanPreferencesKey("auto_pause")
        val COUNTDOWN = intPreferencesKey("countdown_seconds")
        val KEEP_AWAKE = booleanPreferencesKey("keep_screen_awake")
        val CONFIRM_FINISH = booleanPreferencesKey("confirm_finish")
        val GPS_MODE = stringPreferencesKey("gps_mode")
        val METRIC_PRESET = stringPreferencesKey("metric_preset")
        val AUDIO_CUES = booleanPreferencesKey("audio_cues")
        val ANNOUNCE_INTERVAL = stringPreferencesKey("announce_interval")
        val NOTIFICATIONS = stringPreferencesKey("notification_prefs")
        val QUIET_ENABLED = booleanPreferencesKey("quiet_enabled")
        val QUIET_START = intPreferencesKey("quiet_start")
        val QUIET_END = intPreferencesKey("quiet_end")
        val UNITS = stringPreferencesKey("units")
        val ACCENT = stringPreferencesKey("accent")
        val VISUAL_EFFECTS = booleanPreferencesKey("visual_effects")
        val HAPTICS = stringPreferencesKey("haptics")
        val HOME_SCORE = booleanPreferencesKey("home_score")
        val HOME_INSIGHT = booleanPreferencesKey("home_insight")
        val HOME_RINGS = booleanPreferencesKey("home_rings")
        val HOME_STATS = booleanPreferencesKey("home_stats")
        val HOME_MAP = booleanPreferencesKey("home_map")
        val HOME_STREAK = booleanPreferencesKey("home_streak")
        val ROUTE_COLOR_ACCENT = booleanPreferencesKey("route_color_accent")
        val SPEED_COLOR_ROUTE = booleanPreferencesKey("speed_color_route")
        val ROUTE_PRIVACY = stringPreferencesKey("route_privacy")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ALARM_ENABLED = booleanPreferencesKey("alarm_enabled")
        val ALARM_HOUR = intPreferencesKey("alarm_hour")
        val ALARM_MINUTE = intPreferencesKey("alarm_minute")
    }

    // NOTE: store name kept from the v0.4/v0.5 era so existing user
    // settings survive updates.
    private val Context.settingsStore by preferencesDataStore(name = "energy_settings")

    val themeMode: Flow<ThemeMode> = context.settingsStore.data.map { p ->
        enumOrDefault(p[Keys.THEME_MODE], ThemeMode.SYSTEM)
    }

    suspend fun setThemeMode(mode: ThemeMode) = editString(Keys.THEME_MODE, mode.name)

    val alarm: Flow<AlarmSetting> = context.settingsStore.data.map { p ->
        AlarmSetting(
            enabled = p[Keys.ALARM_ENABLED] ?: false,
            hour = p[Keys.ALARM_HOUR] ?: 8,
            minute = p[Keys.ALARM_MINUTE] ?: 0
        )
    }

    suspend fun setAlarm(hour: Int, minute: Int) {
        context.settingsStore.edit {
            it[Keys.ALARM_HOUR] = hour.coerceIn(0, 23)
            it[Keys.ALARM_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setAlarmEnabled(enabled: Boolean) =
        editBool(Keys.ALARM_ENABLED, enabled)

    val preferences: Flow<UserPreferences> = context.settingsStore.data.map { p ->
        UserPreferences(
            weightKg = p[Keys.WEIGHT] ?: 70,
            heightCm = p[Keys.HEIGHT] ?: 170,
            birthdayMillis = p[Keys.BIRTHDAY],
            gender = p[Keys.GENDER] ?: "",
            fitnessLevel = enumOrDefault(p[Keys.FITNESS_LEVEL], FitnessLevel.BEGINNER),
            preferredActivity = enumOrDefault(p[Keys.PREFERRED_ACTIVITY], WorkoutType.RUN),
            stepGoal = (p[Keys.STEP_GOAL] ?: 10_000).coerceIn(1_000, 100_000),
            calorieGoal = (p[Keys.CALORIE_GOAL] ?: 500).coerceIn(100, 5_000),
            defaultWorkoutType = enumOrDefault(p[Keys.DEFAULT_TYPE], WorkoutType.RUN),
            autoPause = p[Keys.AUTO_PAUSE] ?: false,
            countdownSeconds = (p[Keys.COUNTDOWN] ?: 3).coerceIn(0, 10),
            keepScreenAwake = p[Keys.KEEP_AWAKE] ?: true,
            confirmFinish = p[Keys.CONFIRM_FINISH] ?: true,
            gpsMode = enumOrDefault(p[Keys.GPS_MODE], GpsMode.STANDARD),
            metricPreset = enumOrDefault(p[Keys.METRIC_PRESET], MetricPreset.RUNNER),
            audioCues = p[Keys.AUDIO_CUES] ?: false,
            announceInterval = enumOrDefault(p[Keys.ANNOUNCE_INTERVAL], AnnounceInterval.KM1),
            notifications = NotificationCodec.decode(p[Keys.NOTIFICATIONS]),
            quietHoursEnabled = p[Keys.QUIET_ENABLED] ?: false,
            quietStart = p[Keys.QUIET_START] ?: 2200,
            quietEnd = p[Keys.QUIET_END] ?: 700,
            units = enumOrDefault(p[Keys.UNITS], Units.METRIC),
            accent = enumOrDefault(p[Keys.ACCENT], Accent.ORANGE),
            visualEffects = p[Keys.VISUAL_EFFECTS] ?: true,
            haptics = enumOrDefault(p[Keys.HAPTICS], Haptics.FULL),
            homeScore = p[Keys.HOME_SCORE] ?: true,
            homeInsight = p[Keys.HOME_INSIGHT] ?: true,
            homeRings = p[Keys.HOME_RINGS] ?: true,
            homeStats = p[Keys.HOME_STATS] ?: true,
            homeMap = p[Keys.HOME_MAP] ?: true,
            homeStreak = p[Keys.HOME_STREAK] ?: true,
            routeColorAccent = p[Keys.ROUTE_COLOR_ACCENT] ?: true,
            speedColorRoute = p[Keys.SPEED_COLOR_ROUTE] ?: true,
            routePrivacy = enumOrDefault(p[Keys.ROUTE_PRIVACY], RoutePrivacy.EXACT)
        )
    }

    // ── Setters — one per user-facing control ─────────────────────────────

    suspend fun setWeightKg(v: Int) = editInt(Keys.WEIGHT, v, 30, 300)
    suspend fun setHeightCm(v: Int) = editInt(Keys.HEIGHT, v, 100, 250)
    suspend fun setBirthday(millis: Long?) {
        context.settingsStore.edit { if (millis == null) it.remove(Keys.BIRTHDAY) else it[Keys.BIRTHDAY] = millis }
    }
    suspend fun setGender(v: String) = editString(Keys.GENDER, v)
    suspend fun setFitnessLevel(v: FitnessLevel) = editString(Keys.FITNESS_LEVEL, v.name)
    suspend fun setPreferredActivity(v: WorkoutType) = editString(Keys.PREFERRED_ACTIVITY, v.name)
    suspend fun setStepGoal(v: Int) = editInt(Keys.STEP_GOAL, v, 1_000, 100_000)
    suspend fun setCalorieGoal(v: Int) = editInt(Keys.CALORIE_GOAL, v, 100, 5_000)
    suspend fun setDefaultWorkoutType(v: WorkoutType) = editString(Keys.DEFAULT_TYPE, v.name)
    suspend fun setAutoPause(v: Boolean) = editBool(Keys.AUTO_PAUSE, v)
    suspend fun setCountdownSeconds(v: Int) = editInt(Keys.COUNTDOWN, v, 0, 10)
    suspend fun setKeepScreenAwake(v: Boolean) = editBool(Keys.KEEP_AWAKE, v)
    suspend fun setConfirmFinish(v: Boolean) = editBool(Keys.CONFIRM_FINISH, v)
    suspend fun setGpsMode(v: GpsMode) = editString(Keys.GPS_MODE, v.name)
    suspend fun setMetricPreset(v: MetricPreset) = editString(Keys.METRIC_PRESET, v.name)
    suspend fun setAudioCues(v: Boolean) = editBool(Keys.AUDIO_CUES, v)
    suspend fun setAnnounceInterval(v: AnnounceInterval) = editString(Keys.ANNOUNCE_INTERVAL, v.name)
    suspend fun setNotifications(v: NotificationPrefs) = editString(Keys.NOTIFICATIONS, NotificationCodec.encode(v))
    suspend fun setQuietHours(enabled: Boolean, start: Int, end: Int) {
        context.settingsStore.edit {
            it[Keys.QUIET_ENABLED] = enabled
            it[Keys.QUIET_START] = start
            it[Keys.QUIET_END] = end
        }
    }
    suspend fun setUnits(v: Units) = editString(Keys.UNITS, v.name)
    suspend fun setAccent(v: Accent) = editString(Keys.ACCENT, v.name)
    suspend fun setVisualEffects(v: Boolean) = editBool(Keys.VISUAL_EFFECTS, v)
    suspend fun setHaptics(v: Haptics) = editString(Keys.HAPTICS, v.name)
    suspend fun setHomeScore(v: Boolean) = editBool(Keys.HOME_SCORE, v)
    suspend fun setHomeInsight(v: Boolean) = editBool(Keys.HOME_INSIGHT, v)
    suspend fun setHomeRings(v: Boolean) = editBool(Keys.HOME_RINGS, v)
    suspend fun setHomeStats(v: Boolean) = editBool(Keys.HOME_STATS, v)
    suspend fun setHomeMap(v: Boolean) = editBool(Keys.HOME_MAP, v)
    suspend fun setHomeStreak(v: Boolean) = editBool(Keys.HOME_STREAK, v)
    suspend fun setRouteColorAccent(v: Boolean) = editBool(Keys.ROUTE_COLOR_ACCENT, v)
    suspend fun setSpeedColorRoute(v: Boolean) = editBool(Keys.SPEED_COLOR_ROUTE, v)
    suspend fun setRoutePrivacy(v: RoutePrivacy) = editString(Keys.ROUTE_PRIVACY, v.name)

    suspend fun resetAll() {
        context.settingsStore.edit { it.clear() }
    }

    private suspend fun editBool(key: androidx.datastore.preferences.core.Preferences.Key<Boolean>, v: Boolean) {
        context.settingsStore.edit { it[key] = v }
    }
    private suspend fun editInt(key: androidx.datastore.preferences.core.Preferences.Key<Int>, v: Int, min: Int, max: Int) {
        context.settingsStore.edit { it[key] = v.coerceIn(min, max) }
    }
    private suspend fun editString(key: androidx.datastore.preferences.core.Preferences.Key<String>, v: String) {
        context.settingsStore.edit { it[key] = v }
    }

    companion object {
        /** Local storage used by workout route files (for the Data screen). */
        fun workoutsDirSizeBytes(context: Context): Long {
            val dir = File(context.filesDir, "workouts")
            return dir.listFiles()?.sumOf { it.length() } ?: 0L
        }

        private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, default: T): T =
            raw?.let { r -> runCatching { enumValueOf<T>(r) }.getOrDefault(default) } ?: default
    }
}

/** Tolerant JSON codec for the notification-category blob. */
private object NotificationCodec {
    fun encode(p: NotificationPrefs): String = org.json.JSONObject().apply {
        put("workoutReminder", p.workoutReminder)
        put("workoutComplete", p.workoutComplete)
        put("goalProgress", p.goalProgress)
        put("goalComplete", p.goalComplete)
        put("streak", p.streak)
        put("achievements", p.achievements)
        put("recovery", p.recovery)
        put("syncIssues", p.syncIssues)
    }.toString()

    fun decode(raw: String?): NotificationPrefs {
        if (raw == null) return NotificationPrefs()
        return runCatching {
            val o = org.json.JSONObject(raw)
            NotificationPrefs(
                workoutReminder = o.optBoolean("workoutReminder", true),
                workoutComplete = o.optBoolean("workoutComplete", true),
                goalProgress = o.optBoolean("goalProgress", true),
                goalComplete = o.optBoolean("goalComplete", true),
                streak = o.optBoolean("streak", true),
                achievements = o.optBoolean("achievements", true),
                recovery = o.optBoolean("recovery", true),
                syncIssues = o.optBoolean("syncIssues", true)
            )
        }.getOrDefault(NotificationPrefs())
    }
}
