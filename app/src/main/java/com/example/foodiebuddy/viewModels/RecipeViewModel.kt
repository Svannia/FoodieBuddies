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
 * @property recipeID of the recipe (can be null if the recipe has not been created yet)
 */
class RecipeViewModel
@Inject
constructor(private val recipeID: String ?= null) : ViewModel() {
    private val db = DatabaseConnection()

}