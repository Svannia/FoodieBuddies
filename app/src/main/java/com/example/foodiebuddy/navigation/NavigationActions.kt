package com.example.foodiebuddy.navigation

import androidx.navigation.NavHostController
import timber.log.Timber

open class NavigationActions(private val navController: NavHostController) {

    /**
     * Navigates to a specific route and composes the corresponding screen.
     *
     * @param route strictly a constant from the object Route
     * @param clearBackStack false by default. Set to true if navigating to this route should clear the navigation history
     */
    open fun navigateTo(route: String, clearBackStack: Boolean ?= false) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        // only navigates to route if it is different from the current one to avoid stacking copies of the same navigation route
        if (currentRoute != route) {
            navController.navigate(route) {
                launchSingleTop = true
                restoreState = true
                if (clearBackStack == true) {
                    popUpTo(0)
                }
            }
            Timber.tag("NavAction").d("Navigated to route $route")
        }
    }

    /**
     * Goes back to the previous screen.
     *
     */
    open fun goBack() {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        val previousRoute = navController.previousBackStackEntry?.destination?.route
        // in case there is no previous screen recorded ->
        if (previousRoute == null) {
            // already in Login screen or in Create Account screen -> stay/go to Login screen
            if (currentRoute == Route.LOGIN || currentRoute == Route.CREATE_ACCOUNT) {
                navController.navigate(Route.LOGIN) {
                    launchSingleTop = true
                    restoreState = true
                }
                Timber.tag("NavAction").d("Navigated back to Login because of empty backStack")
                // else -> go to Home page
            } else {
                navController.navigate(Route.RECIPES_HOME) {
                    launchSingleTop = true
                    restoreState = true
                }
                Timber.tag("NavAction").d("Navigated back to RecipesHome because of empty backStack")
            }
        // base case (there is a previous route) -> pop it from the backstack
        } else if (previousRoute != currentRoute) {
            navController.popBackStack()
            Timber.tag("NavAction").d("Popped backstack")
        }
    }
}
