package com.energy.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class Units { METRIC, IMPERIAL }

data class UserPreferences(
    val units: Units = Units.METRIC,
    val batterySaver: Boolean = false,
    val autoPause: Boolean = false,
    val calorieGoal: Int = 500,
    val weightKg: Int = 70,
    val stepGoal: Int = 10_000
)

data class AlarmSetting(
    val enabled: Boolean = false,
    val hour: Int = 7,
    val minute: Int = 30
)

private val Context.settingsStore by preferencesDataStore(name = "energy_settings")

/** Persisted user settings: theme mode + workout alarm (DataStore, no Room needed). */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val ALARM_ENABLED = booleanPreferencesKey("alarm_enabled")
        val ALARM_HOUR = intPreferencesKey("alarm_hour")
        val ALARM_MINUTE = intPreferencesKey("alarm_minute")
        val UNITS = stringPreferencesKey("units")
        val BATTERY_SAVER = booleanPreferencesKey("battery_saver")
        val AUTO_PAUSE = booleanPreferencesKey("auto_pause")
        val CALORIE_GOAL = intPreferencesKey("calorie_goal")
        val WEIGHT_KG = intPreferencesKey("weight_kg")
        val STEP_GOAL = intPreferencesKey("step_goal")
    }

    val themeMode: Flow<ThemeMode> = context.settingsStore.data.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[Keys.THEME] ?: "SYSTEM") }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    val alarm: Flow<AlarmSetting> = context.settingsStore.data.map { prefs ->
        AlarmSetting(
            enabled = prefs[Keys.ALARM_ENABLED] ?: false,
            hour = prefs[Keys.ALARM_HOUR] ?: 7,
            minute = prefs[Keys.ALARM_MINUTE] ?: 30
        )
    }

    val preferences: Flow<UserPreferences> = context.settingsStore.data.map { prefs ->
        UserPreferences(
            units = runCatching { Units.valueOf(prefs[Keys.UNITS] ?: "METRIC") }
                .getOrDefault(Units.METRIC),
            batterySaver = prefs[Keys.BATTERY_SAVER] ?: false,
            autoPause = prefs[Keys.AUTO_PAUSE] ?: false,
            calorieGoal = prefs[Keys.CALORIE_GOAL] ?: 500,
            weightKg = prefs[Keys.WEIGHT_KG] ?: 70,
            stepGoal = prefs[Keys.STEP_GOAL] ?: 10_000
        )
    }

    suspend fun setUnits(units: Units) {
        context.settingsStore.edit { it[Keys.UNITS] = units.name }
    }

    suspend fun setBatterySaver(enabled: Boolean) {
        context.settingsStore.edit { it[Keys.BATTERY_SAVER] = enabled }
    }

    suspend fun setAutoPause(enabled: Boolean) {
        context.settingsStore.edit { it[Keys.AUTO_PAUSE] = enabled }
    }

    suspend fun setCalorieGoal(goal: Int) {
        context.settingsStore.edit { it[Keys.CALORIE_GOAL] = goal.coerceIn(100, 5_000) }
    }

    suspend fun setWeightKg(weight: Int) {
        context.settingsStore.edit { it[Keys.WEIGHT_KG] = weight.coerceIn(30, 250) }
    }

    suspend fun setStepGoal(goal: Int) {
        context.settingsStore.edit { it[Keys.STEP_GOAL] = goal.coerceIn(1_000, 50_000) }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsStore.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun setAlarm(enabled: Boolean, hour: Int? = null, minute: Int? = null) {
        context.settingsStore.edit {
            it[Keys.ALARM_ENABLED] = enabled
            hour?.let { h -> it[Keys.ALARM_HOUR] = h }
            minute?.let { m -> it[Keys.ALARM_MINUTE] = m }
        }
    }
}
