package com.dttrn.habit_tracking.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
}
