package com.example.foodiebuddy.navigation

import android.util.Log
import androidx.navigation.NavHostController

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
            Log.d("Nav","Navigated to route $route")
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
            // being
            if (currentRoute == Route.LOGIN || currentRoute == Route.CREATE_ACCOUNT) {
                navController.navigate(Route.LOGIN) {
                    launchSingleTop = true
                    restoreState = true
                }
            } else {
                navController.navigate(Route.RECIPES_HOME) {
                    launchSingleTop = true
                    restoreState = true
                }
            }
            Log.d("Nav","Navigated back to Login because of empty backStack")
        } else if (previousRoute != currentRoute) {
            navController.popBackStack()
        }
    }
}
