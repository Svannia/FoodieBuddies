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

/**
 * Defines a destination that a button could go to.
 *
 * @property route where this destination navigates to
 * @property icon the destination's icon in-app
 * @property text describes in-app where this button navigates to
 */
data class Destination(val route: String, val icon: Int = 0, val text: Int)

/**
 * All destinations contained in the burger menu (side drawer menu)
 */
val BURGER_DESTINATIONS = listOf(
    Destination(route = Route.PROFILE, icon = R.drawable.user, text = R.string.dst_account),
    Destination(route = Route.SETTINGS, icon = R.drawable.settings, text = R.string.dst_settings)
)
