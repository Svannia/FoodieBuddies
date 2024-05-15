package com.example.foodiebuddy.navigation

import androidx.navigation.NavHostController

open class NavigationActions(private val navController: NavHostController) {
    open fun navigateTo(route: String) {
        navController.navigate(route) {
            launchSingleTop = true
            restoreState = true
        }
    }

    open fun goBack() {
        navController.popBackStack()
    }

}
