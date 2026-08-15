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
