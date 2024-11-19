package com.example.foodiebuddy.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodiebuddy.data.Recipe
import com.example.foodiebuddy.database.DatabaseConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for viewing and filtering all recipes in the Database.
 *
 */
class RecipeViewModel
@Inject
constructor(private val recipeID: String ?= null) : ViewModel() {
    private val db = DatabaseConnection()

    // allRecipes contains a list of all recipes on the app
    private val _allRecipes = MutableStateFlow(emptyList<Recipe>())
    val allRecipes: StateFlow<List<Recipe>> = _allRecipes

    fun fetchAllRecipes(isError: (Boolean) -> Unit, callBack: () -> Unit) {
        viewModelScope.launch {
            val allRecipes = db.fetchAllRecipes { isError(it) }
            _allRecipes.value = allRecipes
            callBack()
        }
    }

}