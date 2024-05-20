package com.example.foodiebuddy.ui.recipes

import androidx.activity.compose.BackHandler
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.foodiebuddy.navigation.NavigationActions
import com.example.foodiebuddy.navigation.Route
import com.example.foodiebuddy.viewModels.RecipeListViewModel

@Composable
fun RecipesHome(uid: String, recipesViewModel: RecipeListViewModel, navigationActions: NavigationActions) {
    BackHandler {
        navigationActions.navigateTo(Route.RECIPES_HOME, true)
    }
    Text(text = "Hello")
}
