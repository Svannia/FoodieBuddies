package com.example.foodiebuddy.ui.ingredients

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.foodiebuddy.R
import com.example.foodiebuddy.data.OwnedIngredient
import com.example.foodiebuddy.errors.handleError
import com.example.foodiebuddy.navigation.NavigationActions
import com.example.foodiebuddy.navigation.Route
import com.example.foodiebuddy.ui.LoadingPage
import com.example.foodiebuddy.ui.PrimaryScreen
import com.example.foodiebuddy.viewModels.UserViewModel

@Composable
fun GroceriesHome(userViewModel: UserViewModel, navigationActions: NavigationActions) {
    // pressing the Android back button on this screen does not change it
    BackHandler {
        navigationActions.navigateTo(Route.GROCERIES, true)
    }
    val context = LocalContext.current

    val userPersonal by userViewModel.userPersonal.collectAsState()
    val groceries = remember { mutableStateOf(userPersonal.groceryList) }

    val newItems = groceries.value.mapValues { mutableListOf<String>() }
    val removedItems = groceries.value.mapValues { mutableListOf<String>() }
    val editedCategories = mutableMapOf<String, String>()
    val removedCategories = mutableListOf<String>()

    val allUpdated = remember { mutableListOf(false) }

    val screenState = remember { mutableStateOf(ScreenState.VIEWING) }
    val loading = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        Log.d("Debug", "launching effect")
        userViewModel.fetchUserPersonal({
            if (it) { handleError(context, "Could not fetch user personal") }
        }){
            groceries.value = userPersonal.groceryList
            Log.d("Debug", "groceryList is now $groceries")
        }
    }

    LaunchedEffect(userPersonal) {
        Log.d("Debug", "effect launched")
        groceries.value = userPersonal.groceryList.toMutableMap()
        Log.d("Debug", "userPersonal launching with ${userPersonal.groceryList}")
        Log.d("Debug", "groceries launching with ${groceries.value}")
    }

    if (loading.value) {
        LoadingPage()
    } else {
        PrimaryScreen(
            navigationActions = navigationActions,
            title = stringResource(R.string.title_groceries),
            navigationIndex = 1,
            topBarIcons = {},
            userViewModel = userViewModel,
            floatingButton = { FloatingButton(screenState) {
                loading.value = true
                userViewModel.addIngredients(newItems,  {
                    if (it) handleError(context, "Could not update owned ingredients list")
                }) { loading.value = false }
                userViewModel.removeIngredients(removedItems, {
                    if (it) handleError(context, "Could not remove ingredient")
                }) { loading.value = false }
                userViewModel.updateCategories(editedCategories, {
                    if (it) handleError(context, "Could not update category names")
                }) { loading.value = false }
            }},
            content = {paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (screenState.value) {
                        ScreenState.VIEWING -> {
                            groceries.value.toSortedMap().forEach { (category, ingredients) ->
                                item {
                                    newItems.forEach { (_, value) -> value.clear() }
                                    removedItems.forEach { (_, value) -> value.clear() }
                                    editedCategories.clear()
                                    IngredientCategoryView(category, ingredients) { ingredient, isTicked ->
                                        userViewModel.updateIngredientTick(ingredient.uid, isTicked, {
                                            if (it) { handleError(context, "Could not update ingredient") }
                                        }) {}
                                    }
                                }
                            }
                        }
                        ScreenState.EDITING -> {
                            groceries.value.toSortedMap().forEach { (category, ingredients) ->
                                item {
                                    IngredientCategoryEdit(
                                        category,
                                        ingredients,
                                        newItems[category] ?: mutableListOf(),
                                        removedItems[category] ?: mutableListOf(),
                                        editedCategories,
                                        removedCategories
                                    )
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}