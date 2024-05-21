package com.example.foodiebuddy.navigation

import com.example.foodiebuddy.R

object Route {
    const val START = "Start"
    const val LOGIN = "Login"
    const val CREATE_ACCOUNT = "CreateAccount"
    const val PROFILE = "Profile"
    const val ACCOUNT_SETTINGS = "AccountSettings"
    const val RECIPES_HOME = "RecipesHome"
    const val SETTINGS = "Settings"
}

data class Destination(val route: String, val icon: Int = 0, val text: Int)

val BURGER_DESTINATIONS = listOf(
    Destination(route = Route.PROFILE, icon = R.drawable.user, text = R.string.dst_account),
    Destination(route = Route.SETTINGS, icon = R.drawable.settings, text = R.string.dst_settings)
)
