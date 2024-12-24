package com.example.foodiebuddy.navigation

import com.example.foodiebuddy.R

object Route {
    const val START = "Start"
    const val LOGIN = "Login"
    const val CREATE_ACCOUNT = "CreateAccount"
    const val PROFILE = "Profile"
    const val ACCOUNT_SETTINGS = "AccountSettings"
    const val BUDDIES = "Buddies"
    const val RECIPES_HOME = "RecipesHome"
    const val RECIPE = "Recipe"
    const val RECIPE_CREATE = "RecipeCreate"
    const val RECIPE_EDIT = "EditRecipe"
    const val GROCERIES = "Groceries"
    const val FRIDGE = "MyFridge"
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
    Destination(route = Route.SETTINGS, icon = R.drawable.settings, text = R.string.dst_settings),
    Destination(route = Route.BUDDIES, icon = R.drawable.group, text = R.string.dst_buddies)
)

/**
 * All destinations contained in the bottom navigation bar
 */
val BOTTOM_DESTINATIONS = listOf(
    Destination(route = Route.RECIPES_HOME, icon = R.drawable.recipes, text = R.string.dst_recipes),
    Destination(route = Route.GROCERIES, icon = R.drawable.list, text = R.string.dst_groceries),
    Destination(route = Route.FRIDGE, icon = R.drawable.ingredients, text = R.string.dst_fridge)
)

