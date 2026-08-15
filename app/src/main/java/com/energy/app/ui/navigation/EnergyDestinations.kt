package com.energy.app.ui.navigation

object EnergyDestinations {
    const val SPLASH = "splash"
    const val SIGN_IN = "sign_in"
    const val MAIN = "main"
    const val WORKOUT_LIVE = "workout_live/{type}"
    const val MAP_FULL = "map_full"
    const val HISTORY_DETAIL = "history_detail/{id}"
    const val CONTACT = "contact"

    fun workoutLive(type: String) = "workout_live/$type"
    fun historyDetail(id: String) = "history_detail/$id"
}
