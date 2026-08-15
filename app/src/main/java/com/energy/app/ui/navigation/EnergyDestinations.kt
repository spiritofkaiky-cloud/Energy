package com.energy.app.ui.navigation

object EnergyDestinations {
    const val SPLASH = "splash"
    const val SIGN_IN = "sign_in"
    const val MAIN = "main"
    const val WORKOUT_LIVE = "workout_live/{type}"
    const val MAP_FULL = "map_full"
    const val HISTORY_DETAIL = "history_detail/{id}"
    const val CONTACT = "contact"
    const val SETTINGS_WORKOUT = "settings/workout"
    const val SETTINGS_DISPLAY = "settings/display"
    const val SETTINGS_NOTIFICATIONS = "settings/notifications"
    const val SETTINGS_MAPS = "settings/maps"
    const val SETTINGS_HEALTH = "settings/health"
    const val SETTINGS_ABOUT = "settings/about"

    fun workoutLive(type: String) = "workout_live/$type"
    fun historyDetail(id: String) = "history_detail/$id"
}
