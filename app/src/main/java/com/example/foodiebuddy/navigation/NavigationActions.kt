package com.example.foodiebuddy.navigation

import android.util.Log
import androidx.navigation.NavHostController

open class NavigationActions(private val navController: NavHostController) {
    open fun navigateTo(route: String) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        if (currentRoute != route) {
            navController.navigate(route) {
                launchSingleTop = true
                restoreState = true
            }
            Log.d("Nav","Navigated to route $route")
        }
    }

    open fun goBack() {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        val previousRoute = navController.previousBackStackEntry?.destination?.route
        if (previousRoute == null) {
            navController.navigate(Route.LOGIN) {
                launchSingleTop = true
                restoreState = true
            }
        } else if (previousRoute != currentRoute) {
            navController.popBackStack()
        }
    }
}
