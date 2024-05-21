package com.example.foodiebuddy.navigation

import android.util.Log
import androidx.navigation.NavHostController

open class NavigationActions(private val navController: NavHostController) {
    open fun navigateTo(route: String, clearBackStack: Boolean ?= false) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
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

    open fun goBack() {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        val previousRoute = navController.previousBackStackEntry?.destination?.route
        if (previousRoute == null) {
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
