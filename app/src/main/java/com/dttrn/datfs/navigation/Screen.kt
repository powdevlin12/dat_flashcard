package com.dttrn.datfs.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
}
